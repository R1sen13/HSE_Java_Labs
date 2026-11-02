package com.team.dbms.index;

import com.team.dbms.core.*;
import java.util.*;

/** Non-unique equality index. Row identities and order are local to this instance. */
public class HashIndex {
    private final Column column;
    private final HashMap<Object, List<Row>> groups = new HashMap<>();
    private final Map<Row, Long> order = new IdentityHashMap<>();
    private long nextOrder;

    public HashIndex(Column column) { this.column = column; }
    public String getColumn() { return column.getName(); }

    public void add(Row row) {
        if (order.containsKey(row)) return;
        order.put(row, nextOrder++);
        addToGroup(row);
    }

    private void addToGroup(Row row) {
        Object key = column.getType().convert(row.get(getColumn()));
        groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
    }

    public void remove(Row row) {
        removeFromGroup(row);
        order.remove(row);
    }

    private void removeFromGroup(Row row) {
        Object key = column.getType().convert(row.get(getColumn()));
        List<Row> group = groups.get(key);
        if (group == null) return;
        group.remove(row);
        if (group.isEmpty()) groups.remove(key);
    }

    /** Table.update replaces Row objects; retain the original table order. */
    public void replace(Row oldRow, Row newRow) {
        Long position = order.remove(oldRow);
        Object oldKey = column.getType().convert(oldRow.get(getColumn()));
        Object newKey = column.getType().convert(newRow.get(getColumn()));
        if (position != null && Objects.equals(oldKey, newKey)) {
            List<Row> group = groups.get(oldKey);
            group.set(group.indexOf(oldRow), newRow);
        } else {
            removeFromGroup(oldRow);
            addToGroup(newRow);
        }
        order.put(newRow, position == null ? nextOrder++ : position);
    }

    public List<Row> find(Object value) {
        List<Row> group = groups.get(column.getType().convert(value));
        if (group == null) return new ArrayList<>();
        List<Row> result = new ArrayList<>(group);
        result.sort(Comparator.comparingLong(row -> order.get(row)));
        return result;
    }
}
