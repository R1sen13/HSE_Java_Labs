package com.team.dbms.index;

import com.team.dbms.core.*;
import com.team.dbms.benchmark.TestDataGenerator;
import com.team.dbms.storage.StorageManager;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class IndexConsistencyTest {
    @Test
    void directApiRetainsCrossTypeConditionSemantics() {
        Table table = new Table(new TableSchema("typed", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("date", DataType.DATE, false, true),
            new Column("active", DataType.BOOLEAN, false, true)
        )));
        table.insert(new Row().set("id", 1).set("date", LocalDate.of(2024, 1, 2)).set("active", true));
        table.insert(new Row().set("id", 2));
        Condition[] conditions = {
            new Condition("id", Condition.Operator.EQ, 1.0),
            new Condition("id", Condition.Operator.EQ, 1.5),
            new Condition("id", Condition.Operator.EQ, "1"),
            new Condition("id", Condition.Operator.EQ, "invalid"),
            new Condition("date", Condition.Operator.EQ, "2024-01-02"),
            new Condition("date", Condition.Operator.EQ, "invalid"),
            new Condition("active", Condition.Operator.EQ, "true"),
            new Condition("active", Condition.Operator.EQ, "TRUE"),
            new Condition("active", Condition.Operator.EQ, null)
        };
        List<List<Map<String, Object>>> expected = snapshots(table, conditions);
        Database database = new Database();
        database.addTable(table);
        for (Column column : table.getSchema().getColumns())
            database.getIndexManager().create("idx_" + column.getName(), "typed", column.getName());
        assertEquals(expected, snapshots(table, conditions));
    }

    private List<List<Map<String, Object>>> snapshots(Table table, Condition[] conditions) {
        List<List<Map<String, Object>>> result = new ArrayList<>();
        for (Condition condition : conditions) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Row row : table.select(null, condition)) rows.add(row.getValues());
            result.add(rows);
        }
        return result;
    }

    @Test
    void failedStorageSaveRollsBackRowsAndIndexes() {
        final Database database = TestDataGenerator.generate(3);
        StorageManager storage = new StorageManager() {
            public Database load() { return database; }
            public void save(Database ignored) { throw new IllegalStateException("Disk failure"); }
        };
        DatabaseEngine engine = new DefaultDatabaseEngine(storage);
        IndexManager manager = database.getIndexManager();
        manager.create("idx_name", "artists", "name");
        Table table = database.getTable("artists");
        Row first = table.getRows().get(0);
        assertThrows(IllegalStateException.class, () -> engine.insert("artists", Collections.singletonList(
            new Row().set("id", 10).set("name", "New").set("country", "USA"))));
        assertEquals(3, table.getRows().size());
        assertTrue(manager.getIndex("artists", "name").find("New").isEmpty());
        assertThrows(IllegalStateException.class, () -> engine.update("artists", Collections.singletonMap("name", "Changed"), null));
        assertEquals(Collections.singletonList(first), manager.getIndex("artists", "name").find("Artist 0"));
        assertTrue(manager.getIndex("artists", "name").find("Changed").isEmpty());
        assertThrows(IllegalStateException.class, () -> engine.delete("artists", null));
        assertEquals(Collections.singletonList(first), manager.getIndex("artists", "name").find("Artist 0"));
        assertThrows(IllegalStateException.class, () -> engine.createIndex("idx_country", "artists", "country"));
        assertNull(manager.getIndex("artists", "country"));
        assertEquals(1, database.getIndexDefinitions().size());
    }

    @Test
    void selectUsesRegisteredIndexAndStillCopiesRows() {
        Database database = TestDataGenerator.generate(5);
        Table table = database.getTable("artists");
        final List<Object> lookups = new ArrayList<>();
        HashIndex index = new HashIndex(table.getSchema().getColumn("id")) {
            @Override public List<Row> find(Object value) {
                lookups.add(value);
                return super.find(value);
            }
        };
        for (Row row : table.getRows()) index.add(row);
        table.registerIndex(index);
        List<Row> result = table.select(null, new Condition("id", Condition.Operator.EQ, 2));
        assertEquals(Collections.singletonList(2), lookups);
        assertEquals(1, result.size());
        assertEquals(2, result.get(0).get("id"));
        result.get(0).set("name", "External change");
        assertEquals("Artist 2", table.getRows().get(2).get("name"));
        table.select(null, new Condition("id", Condition.Operator.GT, 2));
        assertEquals(1, lookups.size());
    }
}
