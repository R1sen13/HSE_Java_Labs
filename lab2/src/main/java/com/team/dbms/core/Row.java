package com.team.dbms.core;

import java.util.*;

public class Row {
    private Map<String, Object> values = new LinkedHashMap<>();

    public Row() {}

    public Row(Map<String, Object> values) {
        if (values != null) this.values.putAll(values);
    }

    public Row set(String column, Object value) {
        values.put(column, value);
        return this;
    }

    public Object get(String column) {
        for (Map.Entry<String, Object> e : values.entrySet())
            if (e.getKey().equalsIgnoreCase(column)) return e.getValue();
        return null;
    }

    public Map<String, Object> getValues() { return values; }
}
