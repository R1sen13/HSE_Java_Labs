package com.team.dbms.sql;

import com.team.dbms.core.*;
import java.util.*;
import java.util.regex.*;

/** Small SQL subset: one statement and at most one WHERE comparison. */
public class SqlParser {
    private static final Pattern TOKEN = Pattern.compile(
        "\\s*('(?:[^']|'')*'|[a-zA-Z_][a-zA-Z0-9_]*|[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?|>=|<=|<>|!=|[=<>(),*;])");

    public SqlCommand parse(String sql) {
        Tokens tokens = new Tokens(sql);
        SqlCommand command;
        if (tokens.take("CREATE")) {
            tokens.expect("INDEX");
            command = new SqlCommand(SqlCommand.Type.CREATE_INDEX);
            command.index = tokens.identifier();
            tokens.expect("ON");
            command.table = tokens.identifier();
            tokens.expect("(");
            command.column = tokens.identifier();
            tokens.expect(")");
        } else if (tokens.take("SELECT")) {
            command = new SqlCommand(SqlCommand.Type.SELECT);
            if (!tokens.take("*")) command.columns = columns(tokens);
            tokens.expect("FROM");
            command.table = tokens.identifier();
            command.condition = condition(tokens);
        } else if (tokens.take("INSERT")) {
            tokens.expect("INTO");
            command = new SqlCommand(SqlCommand.Type.INSERT);
            command.table = tokens.identifier();
            tokens.expect("(");
            command.columns = columns(tokens);
            tokens.expect(")");
            tokens.expect("VALUES");
            command.rows = new ArrayList<>();
            do {
                tokens.expect("(");
                Row row = new Row();
                for (int i = 0; i < command.columns.size(); i++) {
                    if (i > 0) tokens.expect(",");
                    row.set(command.columns.get(i), tokens.value());
                }
                tokens.expect(")");
                command.rows.add(row);
            } while (tokens.take(","));
        } else if (tokens.take("UPDATE")) {
            command = new SqlCommand(SqlCommand.Type.UPDATE);
            command.table = tokens.identifier();
            tokens.expect("SET");
            command.values = new LinkedHashMap<>();
            do {
                String column = tokens.identifier();
                tokens.expect("=");
                for (String existing : command.values.keySet())
                    if (existing.equalsIgnoreCase(column)) throw tokens.error("Duplicate column");
                command.values.put(column, tokens.value());
            } while (tokens.take(","));
            command.condition = condition(tokens);
        } else if (tokens.take("DELETE")) {
            tokens.expect("FROM");
            command = new SqlCommand(SqlCommand.Type.DELETE);
            command.table = tokens.identifier();
            command.condition = condition(tokens);
        } else {
            throw tokens.error("Unsupported SQL command");
        }
        tokens.take(";");
        if (tokens.hasNext()) throw tokens.error("Unexpected token");
        return command;
    }

    private List<String> columns(Tokens tokens) {
        List<String> result = new ArrayList<>();
        do {
            String column = tokens.identifier();
            for (String existing : result)
                if (existing.equalsIgnoreCase(column)) throw tokens.error("Duplicate column");
            result.add(column);
        } while (tokens.take(","));
        return result;
    }

    private Condition condition(Tokens tokens) {
        if (!tokens.take("WHERE")) return null;
        String column = tokens.identifier();
        String operator = tokens.next();
        Condition.Operator comparison;
        switch (operator) {
            case "=": comparison = Condition.Operator.EQ; break;
            case "!=": case "<>": comparison = Condition.Operator.NE; break;
            case ">": comparison = Condition.Operator.GT; break;
            case "<": comparison = Condition.Operator.LT; break;
            case ">=": comparison = Condition.Operator.GE; break;
            case "<=": comparison = Condition.Operator.LE; break;
            default: throw tokens.error("Unknown comparison: " + operator);
        }
        return new Condition(column, comparison, tokens.value());
    }

    private static class Tokens {
        private final List<String> items = new ArrayList<>();
        private int position;

        Tokens(String sql) {
            if (sql == null) throw error("SQL cannot be null");
            Matcher matcher = TOKEN.matcher(sql);
            int offset = 0;
            while (offset < sql.length()) {
                if (sql.substring(offset).trim().isEmpty()) break;
                matcher.region(offset, sql.length());
                if (!matcher.lookingAt()) throw error("Invalid SQL at character " + offset);
                items.add(matcher.group(1));
                offset = matcher.end();
            }
        }

        boolean hasNext() { return position < items.size(); }
        String next() {
            if (!hasNext()) throw error("Unexpected end of SQL");
            return items.get(position++);
        }
        boolean take(String token) {
            if (hasNext() && items.get(position).equalsIgnoreCase(token)) {
                position++;
                return true;
            }
            return false;
        }
        void expect(String token) {
            if (!take(token)) throw error("Expected " + token);
        }
        String identifier() {
            String token = next();
            if (!token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) throw error("Invalid identifier: " + token);
            return token;
        }
        Object value() {
            String token = next();
            if (token.startsWith("'")) return token.substring(1, token.length() - 1).replace("''", "'");
            if (token.equalsIgnoreCase("NULL")) return null;
            if (token.equalsIgnoreCase("TRUE")) return true;
            if (token.equalsIgnoreCase("FALSE")) return false;
            try {
                if (token.matches("[+-]?[0-9]+")) return Integer.valueOf(token);
                return Double.valueOf(token);
            } catch (NumberFormatException e) {
                throw error("Invalid literal: " + token);
            }
        }
        IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " (token " + position + ")");
        }
    }
}
