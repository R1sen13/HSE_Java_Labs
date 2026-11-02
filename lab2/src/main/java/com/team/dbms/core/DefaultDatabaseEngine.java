package com.team.dbms.core;

import com.team.dbms.storage.StorageManager;
import java.util.*;

public class DefaultDatabaseEngine implements DatabaseEngine {
    private Database database;
    private StorageManager storage;

    public DefaultDatabaseEngine(StorageManager storage) {
        this.storage = storage;
        this.database = storage.load();
    }

    private Table table(String name) {
        Table table = database.getTable(name);
        if (table == null) throw new IllegalArgumentException("Unknown table: " + name);
        return table;
    }

    private void validateForeignKeys(Table target, Row row) {
        for (Column column : target.getSchema().getColumns()) {
            if (column.getForeignTable() == null) continue;
            Object value = row.get(column.getName());
            if (value == null) continue;

            Table referenced = table(column.getForeignTable());
            boolean found = false;
            for (Row candidate : referenced.getRows()) {
                if (Objects.equals(candidate.get(column.getForeignColumn()), value)) {
                    found = true;
                    break;
                }
            }
            if (!found) throw new IllegalArgumentException("Foreign key violation: " + column.getName());
        }
    }

    public void createTable(TableSchema schema) {
        for (Column column : schema.getColumns()) {
            if (column.getForeignTable() != null) {
                Table referenced = table(column.getForeignTable());
                if (referenced.getSchema().getColumn(column.getForeignColumn()) == null)
                    throw new IllegalArgumentException("Unknown referenced column");
            }
        }
        database.addTable(new Table(schema));
        storage.save(database);
    }

    public void dropTable(String name) {
        for (Table t : database.getTables().values()) {
            for (Column c : t.getSchema().getColumns())
                if (c.getForeignTable() != null && c.getForeignTable().equalsIgnoreCase(name))
                    throw new IllegalArgumentException("Table is referenced by " + t.getSchema().getName());
        }
        database.removeTable(name);
        storage.save(database);
    }

    public void insert(String name, List<Row> rows) {
        Table target = table(name);
        List<Row> backup = new ArrayList<>();
        for (Row r : target.getRows()) backup.add(new Row(r.getValues()));

        try {
            for (Row row : rows) {
                Row normalized = new Row();
                for (Column c : target.getSchema().getColumns()) {
                    Object value = row.get(c.getName());
                    normalized.set(c.getName(), value == null ? null : c.getType().convert(value));
                }
                validateForeignKeys(target, normalized);
                target.insert(normalized);
            }
            storage.save(database);
        } catch (RuntimeException e) {
            target.getRows().clear();
            target.getRows().addAll(backup);
            throw e;
        }
    }

    public List<Row> select(String name, List<String> columns, Condition condition) {
        return table(name).select(columns, condition);
    }

    public int update(String name, Map<String, Object> values, Condition condition) {
        Table target = table(name);
        int count = target.update(values, condition);
        for (Row row : target.getRows()) validateForeignKeys(target, row);
        storage.save(database);
        return count;
    }

    public int delete(String name, Condition condition) {
        Table target = table(name);
        Column pk = target.getSchema().getPrimaryKey();
        List<Object> deletedKeys = new ArrayList<>();

        for (Row row : target.getRows())
            if (condition == null || condition.matches(row))
                deletedKeys.add(row.get(pk.getName()));

        for (Table other : database.getTables().values()) {
            for (Column column : other.getSchema().getColumns()) {
                if (column.getForeignTable() != null &&
                    column.getForeignTable().equalsIgnoreCase(name)) {
                    for (Row row : other.getRows())
                        if (deletedKeys.contains(row.get(column.getForeignColumn())))
                            throw new IllegalArgumentException("Record is referenced by another table");
                }
            }
        }

        int count = target.delete(condition);
        storage.save(database);
        return count;
    }
}
