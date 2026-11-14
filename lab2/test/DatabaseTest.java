import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {
    @TempDir Path tempDir;

    private Database database(String filename) {
        return new Database(
            new JsonStorageManager(tempDir.resolve(filename).toString())
        );
    }

    private Table artistsTable() {
        return new Table("artists", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("name", DataType.STRING, false, false),
            new Column("active", DataType.BOOLEAN, false, false)
        ));
    }

    @Test
    void insertAndSelect() {
        Database db = database("test.json");
        db.createTable(artistsTable());
        db.insert("artists", Collections.singletonList(
            new Row().set("id", 1).set("name", "Dora").set("active", true)
        ));

        List<Row> result = db.select("artists", null,
            new Condition("id", Condition.Operator.EQ, 1));

        assertEquals(1, result.size());
        assertEquals("Dora", result.get(0).get("name"));
    }

    @Test
    void duplicatePrimaryKeyIsRejected() {
        Database db = database("duplicate.json");
        db.createTable(artistsTable());

        Row row = new Row().set("id", 1).set("name", "A").set("active", true);
        db.insert("artists", Collections.singletonList(row));

        assertThrows(IllegalArgumentException.class,
            () -> db.insert("artists", Collections.singletonList(row)));
    }

    @Test
    void dataSurvivesReload() {
        Database first = database("persist.json");
        first.createTable(artistsTable());
        first.insert("artists", Collections.singletonList(
            new Row().set("id", 1).set("name", "Test").set("active", true)
        ));

        Database second = database("persist.json");
        assertEquals(1, second.select("artists", null, null).size());
    }

    @Test
    void updateAndDelete() {
        Database db = database("crud.json");
        db.createTable(artistsTable());
        db.insert("artists", Collections.singletonList(
            new Row().set("id", 1).set("name", "Old").set("active", true)
        ));

        Map<String, Object> values = new HashMap<>();
        values.put("name", "New");

        assertEquals(1, db.update("artists", values, null));
        assertEquals("New", db.select("artists", null, null).get(0).get("name"));
        assertEquals(1, db.delete("artists", null));
    }

    @Test
    void sqlCommandsRoundTripWithAllTypes() {
        Database db = database("sql.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE artists (id INTEGER PRIMARY KEY, name STRING NOT NULL, " +
            "active BOOLEAN, score DOUBLE, born DATE);", db);
        assertEquals(2, parser.execute("INSERT INTO artists (id, name, active, score, born) VALUES " +
            "(1, 'O''Brien, Jr.', true, 2.5, '2000-01-02'), (2, 'Другой', false, NULL, NULL);", db));
        parser.execute("UPDATE artists SET name = 'Новое имя' WHERE ID = 2;", db);
        Database loaded = database("sql.json");
        List<Row> rows = loaded.select("ARTISTS", Arrays.asList("name", "born", "score", "active"),
            new Condition("id", Condition.Operator.GE, "1"));
        assertEquals(2, rows.size());
        assertEquals("O'Brien, Jr.", rows.get(0).get("name"));
        assertEquals(LocalDate.of(2000, 1, 2), rows.get(0).get("born"));
        assertEquals(2.5, rows.get(0).get("score"));
        assertEquals(true, rows.get(0).get("active"));
        assertEquals("Новое имя", rows.get(1).get("name"));
        assertNull(rows.get(1).get("score"));
        assertEquals(1, parser.execute("DELETE FROM artists WHERE id <> 1;", loaded));
        parser.execute("DROP TABLE artists;", loaded);
        assertTrue(database("sql.json").getTables().isEmpty());
    }

    @Test
    void invalidStatementsDoNotPartiallyChangeData() {
        Database db = database("invalid.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE items (id INTEGER PRIMARY KEY, name STRING NOT NULL);", db);
        String[] invalid = {
            "INSERT INTO items (id, name) VALUES (1, 'A'); DELETE FROM items;",
            "INSERT INTO items (id, name) VALUES (1, 'A'), (1, 'B');",
            "INSERT INTO items (id, name) VALUES (1, NULL);",
            "INSERT INTO items (id, name) VALUES (NULL, 'A');",
            "INSERT INTO items (id, name) VALUES (1.5, 'A');",
            "INSERT INTO items (id, name) VALUES (2147483648, 'A');",
            "INSERT INTO items (id, unknown) VALUES (1, 'A');",
            "INSERT INTO items (id, ID) VALUES (1, 2);",
            "SELECT unknown FROM items;",
            "SELECT * FROM items WHERE unknown = 1;",
            "UPDATE items SET unknown = 1;",
            "DELETE FROM items WHERE unknown = 1;"
        };
        for (String sql : invalid) {
            assertThrows(IllegalArgumentException.class, () -> parser.execute(sql, db), sql);
            assertTrue(db.select("items", null, null).isEmpty(), sql);
        }
        parser.execute("INSERT INTO items (id, name) VALUES (1, 'A'), (2, 'B');", db);
        assertThrows(IllegalArgumentException.class,
            () -> parser.execute("UPDATE items SET id = 3;", db));
        assertEquals(1, database("invalid.json").select("items", null, null).get(0).get("id"));
        assertEquals(2, db.select("items", null, null).get(1).get("id"));
    }

    @Test
    void foreignKeysProtectReferencedRowsAndSurviveReload() {
        Database db = database("foreign.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE artists (id INTEGER PRIMARY KEY);", db);
        parser.execute("CREATE TABLE albums (id INTEGER PRIMARY KEY, artist_id INTEGER REFERENCES artists(id));", db);
        assertThrows(IllegalArgumentException.class,
            () -> parser.execute("INSERT INTO albums (id, artist_id) VALUES (1, 99);", db));
        parser.execute("INSERT INTO artists (id) VALUES (1);", db);
        parser.execute("INSERT INTO albums (id, artist_id) VALUES (1, 1);", db);
        Database loaded = database("foreign.json");
        assertThrows(IllegalArgumentException.class,
            () -> parser.execute("DELETE FROM artists WHERE id = 1;", loaded));
        assertThrows(IllegalArgumentException.class,
            () -> parser.execute("UPDATE artists SET id = 2 WHERE id = 1;", loaded));
        assertThrows(IllegalArgumentException.class, () -> parser.execute("DROP TABLE artists;", loaded));
        assertEquals(1, loaded.select("artists", null, null).get(0).get("id"));
        parser.execute("DELETE FROM albums;", loaded);
        assertEquals(1, parser.execute("DELETE FROM artists;", loaded));
    }

    @Test
    void failedSaveRollsBackCrudAndSchemaChanges() throws Exception {
        Path file = tempDir.resolve("blocked.json");
        Database db = database("blocked.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE items (id INTEGER PRIMARY KEY);", db);
        parser.execute("INSERT INTO items (id) VALUES (1);", db);
        Files.delete(file);
        Files.createDirectory(file);
        Files.write(file.resolve("blocker"), new byte[] {1});
        String[] commands = {
            "INSERT INTO items (id) VALUES (2);",
            "UPDATE items SET id = 2;",
            "DELETE FROM items;",
            "DROP TABLE items;",
            "CREATE TABLE other (id INTEGER PRIMARY KEY);"
        };
        for (String sql : commands) {
            assertThrows(RuntimeException.class, () -> parser.execute(sql, db));
            assertEquals(1, db.getTables().size());
            assertEquals(1, db.select("items", null, null).get(0).get("id"));
        }
    }

    @Test
    void previousJsonLayoutLoadsWithoutLosingRows() throws Exception {
        String json = "{\"tables\":{\"artists\":{\"schema\":{\"name\":\"artists\",\"columns\":[" +
            "{\"name\":\"id\",\"type\":\"INTEGER\",\"primaryKey\":true,\"nullable\":false}]}," +
            "\"rows\":[{\"values\":{\"id\":1}}]}},\"indexes\":[]}";
        Files.write(tempDir.resolve("old.json"), json.getBytes(StandardCharsets.UTF_8));
        Database db = database("old.json");
        assertEquals(1, db.select("artists", null, null).get(0).get("id"));
        db.insert("artists", Collections.singletonList(new Row().set("id", 2)));
        assertEquals(2, database("old.json").select("artists", null, null).size());
    }

    @Test
    void insertWithoutColumnListUsesTableOrder() {
        Database db = database("insert-without-columns.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE cars (id INTEGER PRIMARY KEY, brand STRING, price DOUBLE, active BOOLEAN);", db);

        assertEquals(1, parser.execute(
            "INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE);", db));

        Row row = db.select("cars", null, null).get(0);
        assertEquals(1, row.get("id"));
        assertEquals("Toyota", row.get("brand"));
        assertEquals(25000.0, row.get("price"));
        assertEquals(true, row.get("active"));
    }

    @Test
    void indexFindsRowsByEqualityAndSurvivesReload() {
        Database db = database("index.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE songs (id INTEGER PRIMARY KEY, artist STRING);", db);
        parser.execute("INSERT INTO songs (id, artist) VALUES (1, 'A'), (2, 'B'), (3, 'A');", db);
        assertEquals("Index created", parser.execute("CREATE INDEX songs_artist ON songs(artist);", db));
        assertEquals(2, db.select("songs", null,
            new Condition("artist", Condition.Operator.EQ, "A")).size());

        Database loaded = database("index.json");
        assertEquals(Arrays.asList("artist"), loaded.getTable("songs").getIndexedColumns());
        assertEquals(2, loaded.select("songs", null,
            new Condition("artist", Condition.Operator.EQ, "A")).size());
    }

    @Test
    void indexIsUpdatedAfterInsertUpdateAndDelete() {
        Database db = database("index-crud.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE songs (id INTEGER PRIMARY KEY, artist STRING);", db);
        parser.execute("CREATE INDEX songs_artist ON songs(artist);", db);
        parser.execute("INSERT INTO songs (id, artist) VALUES (1, 'A');", db);
        assertEquals(1, db.select("songs", null,
            new Condition("artist", Condition.Operator.EQ, "A")).size());
        parser.execute("UPDATE songs SET artist = 'B' WHERE id = 1;", db);
        assertEquals(0, db.select("songs", null,
            new Condition("artist", Condition.Operator.EQ, "A")).size());
        assertEquals(1, db.select("songs", null,
            new Condition("artist", Condition.Operator.EQ, "B")).size());
        parser.execute("DELETE FROM songs WHERE id = 1;", db);
        assertEquals(0, db.select("songs", null,
            new Condition("artist", Condition.Operator.EQ, "B")).size());
    }

    @Test
    void duplicateIndexIsRejected() {
        Database db = database("duplicate-index.json");
        QueryParser parser = new QueryParser();
        parser.execute("CREATE TABLE songs (id INTEGER PRIMARY KEY, title STRING);", db);
        parser.execute("CREATE INDEX songs_title ON songs(title);", db);
        assertThrows(IllegalArgumentException.class,
            () -> parser.execute("CREATE INDEX another_name ON songs(title);", db));
    }
}
