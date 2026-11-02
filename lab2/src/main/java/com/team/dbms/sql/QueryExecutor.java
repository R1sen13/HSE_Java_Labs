package com.team.dbms.sql;

import com.team.dbms.core.*;
import java.util.*;

public class QueryExecutor {
    private final DatabaseEngine engine;
    private final SqlParser parser = new SqlParser();

    public QueryExecutor(DatabaseEngine engine) { this.engine = engine; }

    /** SELECT returns List<Row>, CRUD returns a count, CREATE INDEX returns null. */
    public Object execute(String sql) { return execute(parser.parse(sql)); }

    public Object execute(SqlCommand command) {
        TableSchema schema = engine.getSchema(command.table);
        Condition condition = command.condition;
        if (condition != null) {
            Column column = schema.getColumn(condition.getColumn());
            if (column == null) throw new IllegalArgumentException("Unknown column: " + condition.getColumn());
            condition = new Condition(column.getName(), condition.getOperator(),
                column.getType().convert(condition.getValue()));
        }
        if (command.columns != null)
            for (String column : command.columns)
                if (schema.getColumn(column) == null) throw new IllegalArgumentException("Unknown column: " + column);
        switch (command.type) {
            case CREATE_INDEX:
                engine.createIndex(command.index, command.table, command.column);
                return null;
            case SELECT: return engine.select(command.table, command.columns, condition);
            case INSERT:
                engine.insert(command.table, command.rows);
                return command.rows.size();
            case UPDATE: return engine.update(command.table, command.values, condition);
            case DELETE: return engine.delete(command.table, condition);
            default: throw new IllegalArgumentException("Unsupported SQL command");
        }
    }
}
