package com.team.dbms.index;

import com.team.dbms.core.*;
import java.util.*;

public class IndexManager {
    /** Only these definitions are persisted, never HashIndex or Row references. */
    public static class Definition {
        private String name;
        private String table;
        private String column;

        public Definition(String name, String table, String column) {
            this.name = name;
            this.table = table;
            this.column = column;
        }
        public String getName() { return name; }
        public String getTable() { return table; }
        public String getColumn() { return column; }
    }

    private final Database database;

    public IndexManager(Database database) { this.database = database; }

    public void create(String name, String tableName, String columnName) {
        if (name == null || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*"))
            throw new IllegalArgumentException("Invalid index name: " + name);
        for (Definition definition : database.getIndexDefinitions())
            if (definition.name.equalsIgnoreCase(name))
                throw new IllegalArgumentException("Index already exists: " + name);
        Table table = requireTable(tableName);
        Column column = table.getSchema().getColumn(columnName);
        if (column == null) throw new IllegalArgumentException("Unknown column: " + columnName);
        if (getIndex(tableName, columnName) != null)
            throw new IllegalArgumentException("Column already indexed: " + columnName);
        register(table, column);
        database.getIndexDefinitions().add(new Definition(name, table.getSchema().getName(), column.getName()));
    }

    private Table requireTable(String name) {
        Table table = database.getTable(name);
        if (table == null) throw new IllegalArgumentException("Unknown table: " + name);
        return table;
    }

    private void register(Table table, Column column) {
        HashIndex index = new HashIndex(column);
        for (Row row : table.getRows()) index.add(row);
        table.registerIndex(index);
    }

    public HashIndex getIndex(String table, String column) {
        return requireTable(table).getIndex(column);
    }

    public void rebuild() {
        for (Table table : database.getTables().values()) table.clearIndexes();
        for (Definition definition : database.getIndexDefinitions()) {
            Table table = requireTable(definition.table);
            Column column = table.getSchema().getColumn(definition.column);
            if (column == null) throw new IllegalArgumentException("Unknown indexed column: " + definition.column);
            register(table, column);
        }
    }

    public void removeForTable(String name) {
        database.getIndexDefinitions().removeIf(d -> d.table.equalsIgnoreCase(name));
    }
}
