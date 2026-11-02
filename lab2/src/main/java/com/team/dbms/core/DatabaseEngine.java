package com.team.dbms.core;

import java.util.*;

public interface DatabaseEngine {
    TableSchema getSchema(String tableName);
    void createIndex(String name, String table, String column);
    void createTable(TableSchema schema);
    void dropTable(String tableName);
    void insert(String tableName, List<Row> rows);
    List<Row> select(String tableName, List<String> columns, Condition condition);
    int update(String tableName, Map<String, Object> values, Condition condition);
    int delete(String tableName, Condition condition);
}
