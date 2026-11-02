package com.team.dbms.core;

import java.util.*;

public class TableSchema {
    private String name;
    private List<Column> columns;

    public TableSchema() {}

    public TableSchema(String name, List<Column> columns) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Empty table name");
        if (columns == null || columns.isEmpty()) throw new IllegalArgumentException("Table must have columns");

        Set<String> names = new HashSet<>();
        int keys = 0;
        for (Column c : columns) {
            if (!names.add(c.getName().toLowerCase())) throw new IllegalArgumentException("Duplicate column");
            if (c.isPrimaryKey()) keys++;
        }
        if (keys != 1) throw new IllegalArgumentException("Exactly one primary key is required");

        this.name = name;
        this.columns = new ArrayList<>(columns);
    }

    public String getName() { return name; }
    public List<Column> getColumns() { return columns; }

    public Column getColumn(String name) {
        for (Column c : columns)
            if (c.getName().equalsIgnoreCase(name)) return c;
        return null;
    }

    public Column getPrimaryKey() {
        for (Column c : columns) if (c.isPrimaryKey()) return c;
        return null;
    }
}
