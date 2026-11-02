package com.team.dbms.core;

import java.util.*;

public class Table {
    private TableSchema schema;
    private List<Row> rows = new ArrayList<>();

    public Table() {}

    public Table(TableSchema schema) {
        this.schema = schema;
    }

    public TableSchema getSchema() { return schema; }
    public List<Row> getRows() { return rows; }

    public void insert(Row input) {
        Row normalized = normalize(input);
        Object key = normalized.get(schema.getPrimaryKey().getName());

        for (Row row : rows)
            if (Objects.equals(row.get(schema.getPrimaryKey().getName()), key))
                throw new IllegalArgumentException("Duplicate primary key: " + key);

        rows.add(normalized);
    }

    private Row normalize(Row input) {
        Row result = new Row();

        for (String key : input.getValues().keySet())
            if (schema.getColumn(key) == null)
                throw new IllegalArgumentException("Unknown column: " + key);

        for (Column column : schema.getColumns()) {
            Object value = input.get(column.getName());

            if (value == null && !column.isNullable() && !column.isPrimaryKey())
                throw new IllegalArgumentException("Column cannot be null: " + column.getName());

            if (value == null && column.isPrimaryKey())
                throw new IllegalArgumentException("Primary key cannot be null");

            result.set(column.getName(), value == null ? null : column.getType().convert(value));
        }
        return result;
    }

    public List<Row> select(List<String> columns, Condition condition) {
        List<Row> result = new ArrayList<>();
        for (Row row : rows) {
            if (condition != null && !condition.matches(row)) continue;

            if (columns == null || columns.isEmpty()) {
                result.add(new Row(row.getValues()));
            } else {
                Row projected = new Row();
                for (String column : columns) {
                    if (schema.getColumn(column) == null)
                        throw new IllegalArgumentException("Unknown column: " + column);
                    projected.set(column, row.get(column));
                }
                result.add(projected);
            }
        }
        return result;
    }

    public int update(Map<String, Object> values, Condition condition) {
        for (String key : values.keySet())
            if (schema.getColumn(key) == null)
                throw new IllegalArgumentException("Unknown column: " + key);

        List<Row> backup = new ArrayList<>();
        for (Row row : rows) backup.add(new Row(row.getValues()));

        try {
            int count = 0;
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                if (condition != null && !condition.matches(row)) continue;

                Row changed = new Row(row.getValues());
                for (Map.Entry<String, Object> e : values.entrySet())
                    changed.set(e.getKey(), e.getValue());

                rows.set(i, normalize(changed));
                count++;
            }

            Set<Object> keys = new HashSet<>();
            for (Row row : rows)
                if (!keys.add(row.get(schema.getPrimaryKey().getName())))
                    throw new IllegalArgumentException("Duplicate primary key");

            return count;
        } catch (RuntimeException e) {
            rows = backup;
            throw e;
        }
    }

    public int delete(Condition condition) {
        int before = rows.size();
        rows.removeIf(row -> condition == null || condition.matches(row));
        return before - rows.size();
    }
}
