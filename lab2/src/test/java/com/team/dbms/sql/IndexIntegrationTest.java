package com.team.dbms.sql;

import com.google.gson.*;
import com.team.dbms.core.*;
import com.team.dbms.index.*;
import com.team.dbms.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IndexIntegrationTest {
    @TempDir Path directory;
    private Path file;
    private DatabaseEngine engine;
    private QueryExecutor sql;

    @BeforeEach
    void prepare() {
        file = directory.resolve("music.json");
        engine = new DefaultDatabaseEngine(new JsonStorageManager(file.toString()));
        MusicDatabaseSchema.createTables(engine);
        sql = new QueryExecutor(engine);
        sql.execute("INSERT INTO artists (id, name, country, is_active) VALUES " +
            "(1, 'One', 'USA', true), (2, 'Two', 'UK', false), (3, 'Three', 'USA', true)");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> select(String query) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Row row : (List<Row>) sql.execute(query)) result.add(row.getValues());
        return result;
    }

    private List<Integer> ids(String where) {
        List<Integer> result = new ArrayList<>();
        for (Map<String, Object> row : select("SELECT * FROM artists " + where))
            result.add((Integer) row.get("id"));
        return result;
    }

    @Test
    void selectSameWithAndWithoutIndexIncludingProjectionAndRanges() {
        String[] queries = {
            "SELECT * FROM artists WHERE country = 'USA'",
            "SELECT name FROM ARTISTS WHERE COUNTRY = 'USA'",
            "SELECT * FROM artists WHERE country = 'missing'",
            "SELECT * FROM artists WHERE country = 'usa'",
            "SELECT * FROM artists WHERE country != 'UK'",
            "SELECT * FROM artists WHERE id >= 2",
            "SELECT * FROM artists"
        };
        List<List<Map<String, Object>>> expected = new ArrayList<>();
        for (String query : queries) expected.add(select(query));
        sql.execute("CREATE INDEX idx_country ON artists (country);");
        sql.execute("CREATE INDEX idx_id ON artists (id)");
        for (int i = 0; i < queries.length; i++) assertEquals(expected.get(i), select(queries[i]));
        assertEquals(Arrays.asList(1, 3), ids("WHERE country = 'USA'"));
        Database restored = new JsonStorageManager(file.toString()).load();
        assertEquals(2, restored.getIndexManager().getIndex("ARTISTS", "COUNTRY").find("USA").size());
    }

    @Test
    void indexCreationValidationAndEmptyTable() {
        sql.execute("CREATE INDEX idx_country ON artists (country)");
        assertThrows(IllegalArgumentException.class, () -> sql.execute("CREATE INDEX IDX_COUNTRY ON artists (name)"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("CREATE INDEX other ON artists (COUNTRY)"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("CREATE INDEX other ON absent (country)"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("CREATE INDEX other ON artists (absent)"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("CREATE INDEX 1bad ON artists (id)"));
        assertThrows(IllegalArgumentException.class, () -> engine.createIndex("bad-name", "artists", "id"));
        sql.execute("CREATE INDEX idx_album ON albums (title)");
        assertTrue(select("SELECT * FROM albums WHERE title = 'missing'").isEmpty());
    }

    @Test
    void sequentialInsertUpdateDeleteAndReload() {
        sql.execute("CREATE INDEX idx_country ON artists (country)");
        sql.execute("CREATE INDEX idx_name ON artists (name)");
        assertEquals(1, sql.execute("INSERT INTO artists (id, name, country, is_active) VALUES (4, 'Four', 'USA', true)"));
        assertEquals(Arrays.asList(1, 3, 4), ids("WHERE country = 'USA'"));
        assertEquals(1, sql.execute("UPDATE artists SET COUNTRY = 'USA', name = 'Changed' WHERE id = 2"));
        assertEquals(Arrays.asList(1, 2, 3, 4), ids("WHERE country = 'USA'"));
        assertEquals(Collections.singletonList(2), ids("WHERE name = 'Changed'"));
        assertTrue(ids("WHERE country = 'UK'").isEmpty());
        assertEquals(3, sql.execute("UPDATE artists SET country = 'Canada' WHERE is_active = true"));
        assertEquals(Arrays.asList(1, 3, 4), ids("WHERE country = 'Canada'"));
        assertEquals(Collections.singletonList(2), ids("WHERE country = 'USA'"));
        assertEquals(3, sql.execute("DELETE FROM artists WHERE country = 'Canada'"));
        assertTrue(ids("WHERE country = 'Canada'").isEmpty());
        assertEquals(Collections.singletonList(2), ids("WHERE country = 'USA'"));
        sql = new QueryExecutor(new DefaultDatabaseEngine(new JsonStorageManager(file.toString())));
        assertEquals(Collections.singletonList(2), ids("WHERE country = 'USA'"));
        assertEquals(1, sql.execute("DELETE FROM artists"));
        assertTrue(ids("WHERE country = 'USA'").isEmpty());
    }

    @Test
    void metadataOnlyAndRebuildUsesLoadedRowObjectsWithoutDuplicates() throws Exception {
        sql.execute("CREATE INDEX idx_country ON artists (country)");
        sql.execute("CREATE INDEX idx_id ON artists (id)");
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        JsonObject object = JsonParser.parseString(json).getAsJsonObject();
        JsonArray definitions = object.getAsJsonArray("indexes");
        assertEquals(2, definitions.size());
        assertEquals(3, definitions.get(0).getAsJsonObject().size());
        assertFalse(json.contains("groups"));
        assertFalse(json.contains("nextOrder"));
        for (int i = 0; i < 3; i++) {
            JsonStorageManager storage = new JsonStorageManager(file.toString());
            Database loaded = storage.load();
            IndexManager manager = loaded.getIndexManager();
            manager.rebuild();
            manager.rebuild();
            assertEquals(2, loaded.getIndexDefinitions().size());
            HashIndex index = manager.getIndex("artists", "country");
            assertEquals(2, index.find("USA").size());
            assertSame(loaded.getTable("artists").getRows().get(0), index.find("USA").get(0));
            assertEquals(Integer.class, manager.getIndex("artists", "id").find(1).get(0).get("id").getClass());
            loaded.getTable("artists").delete(new Condition("id", Condition.Operator.EQ, 1));
            assertEquals(1, index.find("USA").size());
            assertEquals(2, storage.load().getIndexManager().getIndex("artists", "country").find("USA").size());
        }
    }

    @Test
    void failedBatchAndFailedUpdateLeaveIndexesConsistent() {
        sql.execute("CREATE INDEX idx_country ON artists (country)");
        assertThrows(IllegalArgumentException.class, () -> sql.execute(
            "INSERT INTO artists (id, name, country, is_active) VALUES " +
            "(4, 'Four', 'UK', true), (1, 'Duplicate', 'UK', true)"));
        assertEquals(Collections.singletonList(2), ids("WHERE country = 'UK'"));
        assertEquals(Arrays.asList(1, 3), ids("WHERE country = 'USA'"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("UPDATE artists SET country = 'UK', id = 1"));
        assertEquals(Arrays.asList(1, 3), ids("WHERE country = 'USA'"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("UPDATE artists SET name = NULL WHERE id = 1"));
        assertEquals(Arrays.asList(1, 3), ids("WHERE country = 'USA'"));
    }

    @Test
    void foreignKeyRejectionRestoresIndexedValues() {
        sql.execute("INSERT INTO albums (id, artist_id, title, release_date, is_single) VALUES (1, 1, 'Album', '2024-01-02', false)");
        sql.execute("CREATE INDEX idx_artist ON albums (artist_id)");
        sql.execute("CREATE INDEX idx_date ON albums (release_date)");
        List<Map<String, Object>> expected = select("SELECT * FROM albums WHERE artist_id = 1");
        assertThrows(IllegalArgumentException.class, () -> sql.execute("UPDATE albums SET artist_id = 999 WHERE id = 1"));
        assertEquals(expected, select("SELECT * FROM albums WHERE artist_id = 1"));
        assertTrue(select("SELECT * FROM albums WHERE artist_id = 999").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> sql.execute("DELETE FROM artists WHERE id = 1"));
        sql = new QueryExecutor(new DefaultDatabaseEngine(new JsonStorageManager(file.toString())));
        assertEquals(expected, select("SELECT * FROM albums WHERE release_date = '2024-01-02'"));
    }

    @Test
    void allTypesNullAndEscapedStringsSurviveReload() {
        sql.execute("INSERT INTO artists (id, name, country, is_active) VALUES (4, 'O''Brien; Jr', NULL, false)");
        String[] columns = {"country", "name", "is_active", "id"};
        String[] queries = {
            "SELECT * FROM artists WHERE country = NULL",
            "SELECT * FROM artists WHERE name = 'O''Brien; Jr'",
            "SELECT * FROM artists WHERE is_active = false",
            "SELECT * FROM artists WHERE id = '2'"
        };
        List<List<Map<String, Object>>> expected = new ArrayList<>();
        for (String query : queries) expected.add(select(query));
        for (String column : columns) sql.execute("CREATE INDEX idx_" + column + " ON artists (" + column + ")");
        for (int i = 0; i < queries.length; i++) assertEquals(expected.get(i), select(queries[i]));
        sql = new QueryExecutor(new DefaultDatabaseEngine(new JsonStorageManager(file.toString())));
        for (int i = 0; i < queries.length; i++) assertEquals(expected.get(i), select(queries[i]));
        sql.execute("INSERT INTO albums (id, artist_id, title, is_single) VALUES (1, 1, 'Album', true)");
        sql.execute("INSERT INTO songs (id, album_id, title, duration, plays, likes, is_explicit) VALUES (1, 1, 'Song', 3.5, 10, 2, false)");
        sql.execute("CREATE INDEX idx_duration ON songs (duration)");
        assertEquals(1, select("SELECT * FROM songs WHERE duration = 3.5").size());
        sql = new QueryExecutor(new DefaultDatabaseEngine(new JsonStorageManager(file.toString())));
        assertEquals(1, select("SELECT * FROM songs WHERE duration = 3.5").size());
    }

    @Test
    void legacyFileWithoutMetadataAndDropTable() throws Exception {
        JsonObject json = JsonParser.parseString(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)).getAsJsonObject();
        json.remove("indexes");
        Files.write(file, json.toString().getBytes(StandardCharsets.UTF_8));
        engine = new DefaultDatabaseEngine(new JsonStorageManager(file.toString()));
        sql = new QueryExecutor(engine);
        assertEquals(Arrays.asList(1, 3), ids("WHERE country = 'USA'"));
        sql.execute("CREATE INDEX idx_duration ON songs (duration)");
        engine.dropTable("songs");
        Database loaded = new JsonStorageManager(file.toString()).load();
        assertTrue(loaded.getIndexDefinitions().isEmpty());
        assertNull(loaded.getTable("songs"));
    }

    @Test
    void malformedSqlIsRejectedWithoutChangingData() {
        String[] invalid = {"", "SELECT * FROM artists; DELETE FROM artists", "CREATE INDEX idx ON artists (name, country)",
            "SELECT * FROM artists WHERE id = 1 AND country = 'USA'", "INSERT INTO artists (id, ID) VALUES (1, 2)",
            "UPDATE artists SET name = 'A', NAME = 'B'", "CREATE INDEX idx ON artists (name", "SELECT * FROM artists WHERE id ~~ 1"};
        for (String query : invalid) assertThrows(IllegalArgumentException.class, () -> sql.execute(query), query);
        assertThrows(IllegalArgumentException.class, () -> sql.execute("SELECT absent FROM artists WHERE id = 999"));
        assertThrows(IllegalArgumentException.class, () -> sql.execute("SELECT * FROM artists WHERE absent = 'x'"));
        assertEquals(3, ids("").size());
    }
}
