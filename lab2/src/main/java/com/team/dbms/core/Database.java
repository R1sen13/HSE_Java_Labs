package com.team.dbms.core;

import java.util.*;

public class Database {
    private Map<String, Table> tables = new LinkedHashMap<>();

    public Database() {}

    public Map<String, Table> getTables() { return tables; }

    public Table getTable(String name) {
        for (Map.Entry<String, Table> e : tables.entrySet())
            if (e.getKey().equalsIgnoreCase(name)) return e.getValue();
        return null;
    }

    public void addTable(Table table) {
        String name = table.getSchema().getName();
        if (getTable(name) != null) throw new IllegalArgumentException("Table already exists: " + name);
        tables.put(name, table);
    }

    public void removeTable(String name) {
        String actual = null;
        for (String key : tables.keySet())
            if (key.equalsIgnoreCase(name)) actual = key;
        if (actual == null) throw new IllegalArgumentException("Unknown table: " + name);
        tables.remove(actual);
    }
}
