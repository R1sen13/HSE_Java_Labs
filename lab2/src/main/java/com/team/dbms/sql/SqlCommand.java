package com.team.dbms.sql;

import com.team.dbms.core.*;
import java.util.*;

/** Parsed data only; execution stays in QueryExecutor and DatabaseEngine. */
public class SqlCommand {
    public enum Type { CREATE_INDEX, SELECT, INSERT, UPDATE, DELETE }

    final Type type;
    String table;
    String index;
    String column;
    List<String> columns;
    List<Row> rows;
    Map<String, Object> values;
    Condition condition;

    SqlCommand(Type type) { this.type = type; }
    public Type getType() { return type; }
}
