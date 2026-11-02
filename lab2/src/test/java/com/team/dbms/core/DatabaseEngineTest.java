package com.team.dbms.core;

import com.team.dbms.storage.JsonStorageManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseEngineTest {
    @TempDir Path tempDir;

    private DatabaseEngine engine(String filename) {
        return new DefaultDatabaseEngine(
            new JsonStorageManager(tempDir.resolve(filename).toString())
        );
    }

    private TableSchema schema() {
        return new TableSchema("artists", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("name", DataType.STRING, false, false),
            new Column("active", DataType.BOOLEAN, false, false)
        ));
    }

    @Test
    void insertAndSelect() {
        DatabaseEngine db = engine("test.json");
        db.createTable(schema());
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
        DatabaseEngine db = engine("duplicate.json");
        db.createTable(schema());

        Row row = new Row().set("id", 1).set("name", "A").set("active", true);
        db.insert("artists", Collections.singletonList(row));

        assertThrows(IllegalArgumentException.class,
            () -> db.insert("artists", Collections.singletonList(row)));
    }

    @Test
    void dataSurvivesReload() {
        DatabaseEngine first = engine("persist.json");
        first.createTable(schema());
        first.insert("artists", Collections.singletonList(
            new Row().set("id", 1).set("name", "Test").set("active", true)
        ));

        DatabaseEngine second = engine("persist.json");
        assertEquals(1, second.select("artists", null, null).size());
    }

    @Test
    void updateAndDelete() {
        DatabaseEngine db = engine("crud.json");
        db.createTable(schema());
        db.insert("artists", Collections.singletonList(
            new Row().set("id", 1).set("name", "Old").set("active", true)
        ));

        Map<String, Object> values = new HashMap<>();
        values.put("name", "New");

        assertEquals(1, db.update("artists", values, null));
        assertEquals("New", db.select("artists", null, null).get(0).get("name"));
        assertEquals(1, db.delete("artists", null));
    }
}
