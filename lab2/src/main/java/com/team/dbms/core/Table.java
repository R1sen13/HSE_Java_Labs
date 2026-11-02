package com.team.dbms.core;

import java.util.*;
import com.team.dbms.index.HashIndex;

public class Table {
    private TableSchema schema;
    private List<Row> rows = new ArrayList<>();

    private transient Map<String, HashIndex> indexes = new LinkedHashMap<>();

    public Table() {}

    public void registerIndex(HashIndex index) {
        indexes.put(index.getColumn().toLowerCase(Locale.ROOT), index);
    }

    public HashIndex getIndex(String column) {
        return indexes.get(column.toLowerCase(Locale.ROOT));
    }

    public void clearIndexes() { indexes.clear(); }

    public void restoreRows(List<Row> backup) {
        rows = new ArrayList<>(backup);
        List<String> columns = new ArrayList<>(indexes.keySet());
        indexes.clear();
        for (String name : columns) {
            HashIndex index = new HashIndex(schema.getColumn(name));
            for (Row row : rows) index.add(row);
            registerIndex(index);
        }
    }

    /** Gson deserializes Object numbers as Double; restore schema types first. */
    public void restoreTypes() {
        List<Row> normalized = new ArrayList<>();
        for (Row row : rows) normalized.add(normalize(row));
        rows = normalized;
    }

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
        for (HashIndex index : indexes.values()) index.add(normalized);
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
        Iterable<Row> candidates = rows;
        if (condition != null && condition.getOperator() == Condition.Operator.EQ) {
            Column column = schema.getColumn(condition.getColumn());
            HashIndex index = getIndex(condition.getColumn());
            // Only use keys with exactly the same semantics as Condition.matches.
            if (index != null && canUseIndex(column, condition.getValue()))
                candidates = index.find(condition.getValue());
        }
        for (Row row : candidates) {
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

    private boolean canUseIndex(Column column, Object value) {
        if (value == null) return true;
        try {
            return column.getType().convert(value).equals(value);
        } catch (IllegalArgumentException e) {
            // Direct API conditions historically allow cross-type comparisons.
            return false;
        }
    }

    public int update(Map<String, Object> values, Condition condition) {
        for (String key : values.keySet())
            if (schema.getColumn(key) == null)
                throw new IllegalArgumentException("Unknown column: " + key);

        List<Row> backup = new ArrayList<>();
        backup.addAll(rows);

        try {
            int count = 0;
            for (int i = 0; i < rows.size(); i++) {
                Row row = rows.get(i);
                if (condition != null && !condition.matches(row)) continue;

                Row changed = new Row(row.getValues());
                for (Map.Entry<String, Object> e : values.entrySet())
                    changed.set(schema.getColumn(e.getKey()).getName(), e.getValue());

                rows.set(i, normalize(changed));
                count++;
            }

            Set<Object> keys = new HashSet<>();
            for (Row row : rows)
                if (!keys.add(row.get(schema.getPrimaryKey().getName())))
                    throw new IllegalArgumentException("Duplicate primary key");

            for (int i = 0; i < rows.size(); i++) {
                Row oldRow = backup.get(i);
                Row newRow = rows.get(i);
                // Unchanged rows retain their original objects.
                if (oldRow != newRow)
                    for (HashIndex index : indexes.values()) index.replace(oldRow, newRow);
            }
            return count;
        } catch (RuntimeException e) {
            restoreRows(backup);
            throw e;
        }
    }

    public int delete(Condition condition) {
        int before = rows.size();
        Iterator<Row> iterator = rows.iterator();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            if (condition == null || condition.matches(row)) {
                iterator.remove();
                for (HashIndex index : indexes.values()) index.remove(row);
            }
        }
        return before - rows.size();
    }
}
