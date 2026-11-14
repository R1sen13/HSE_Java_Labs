import java.util.*;
import java.util.*;
import java.util.regex.*;

/** Разбирает и выполняет поддерживаемое SQL-подобное подмножество. */

public class QueryParser {
    private static final Pattern TOKEN = Pattern.compile(
        "\\s*('(?:[^']|'')*'|[a-zA-Z_][a-zA-Z0-9_]*|[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?|>=|<=|<>|!=|[=<>(),*;])");
    private final List<String> tokens = new ArrayList<>();
    private int position;

    public Object execute(String sql, Database database) {
        tokenize(sql);
        String command = next().toUpperCase(Locale.ROOT);
        switch (command) {
            case "CREATE": return create(database);
            case "DROP":
                expect("TABLE");
                String name = identifier();
                end();
                database.dropTable(name);
                return "Table dropped";
            case "SELECT": return select(database);
            case "INSERT": return insert(database);
            case "UPDATE": return update(database);
            case "DELETE":
                expect("FROM");
                String table = identifier();
                Condition condition = condition();
                end();
                return database.delete(table, condition);
            default: throw error("Unsupported command: " + command);
        }
    }

    private Object create(Database database) {
        if (take("INDEX")) return createIndex(database);
        expect("TABLE");
        String table = identifier();
        expect("(");
        List<Column> columns = new ArrayList<>();
        do {
            String name = identifier();
            DataType type = DataType.valueOf(identifier().toUpperCase(Locale.ROOT));
            boolean primaryKey = false;
            boolean nullable = true;
            String foreignTable = null;
            String foreignColumn = null;
            boolean hasNullability = false;
            while (!peek(",") && !peek(")")) {
                if (take("PRIMARY")) {
                    if (primaryKey) throw error("Duplicate PRIMARY KEY");
                    expect("KEY");
                    primaryKey = true;
                } else if (take("NOT")) {
                    if (hasNullability) throw error("Duplicate nullability constraint");
                    expect("NULL");
                    nullable = false;
                    hasNullability = true;
                } else if (take("NULL")) {
                    if (hasNullability) throw error("Duplicate nullability constraint");
                    hasNullability = true;
                } else if (take("REFERENCES")) {
                    if (foreignTable != null) throw error("Duplicate REFERENCES");
                    foreignTable = identifier();
                    expect("(");
                    foreignColumn = identifier();
                    expect(")");
                } else {
                    throw error("Expected PRIMARY KEY, NOT NULL or REFERENCES");
                }
            }
            columns.add(new Column(name, type, primaryKey, nullable, foreignTable, foreignColumn));
        } while (take(","));
        expect(")");
        end();
        database.createTable(new Table(table, columns));
        return "Table created";
    }

    private Object createIndex(Database database) {
        identifier();
        expect("ON");
        String table = identifier();
        expect("(");
        String column = identifier();
        expect(")");
        end();
        database.createIndex(table, column);
        return "Index created";
    }

    private Object select(Database database) {
        List<String> columns = null;
        if (!take("*")) columns = columns();
        expect("FROM");
        String table = identifier();
        Condition condition = condition();
        end();
        return database.select(table, columns, condition);
    }

    private Object insert(Database database) {
        expect("INTO");
        String table = identifier();
        List<String> columns;
        if (take("(")) {
            columns = columns();
            expect(")");
        } else {
            columns = new ArrayList<>();
            for (Column column : database.getTable(table).getColumns())
                columns.add(column.getName());
        }
        expect("VALUES");
        List<Row> rows = new ArrayList<>();
        do {
            expect("(");
            Row row = new Row();
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) expect(",");
                row.set(columns.get(i), value());
            }
            expect(")");
            rows.add(row);
        } while (take(","));
        end();
        database.insert(table, rows);
        return rows.size();
    }

    private Object update(Database database) {
        String table = identifier();
        expect("SET");
        Map<String, Object> values = new LinkedHashMap<>();
        do {
            String column = identifier();
            for (String existing : values.keySet())
                if (existing.equalsIgnoreCase(column)) throw error("Duplicate column");
            expect("=");
            values.put(column, value());
        } while (take(","));
        Condition condition = condition();
        end();
        return database.update(table, values, condition);
    }

    private List<String> columns() {
        List<String> result = new ArrayList<>();
        do {
            String column = identifier();
            for (String existing : result)
                if (existing.equalsIgnoreCase(column)) throw error("Duplicate column");
            result.add(column);
        } while (take(","));
        return result;
    }

    private Condition condition() {
        if (!take("WHERE")) return null;
        String column = identifier();
        String operator = next();
        Condition.Operator comparison;
        switch (operator) {
            case "=": comparison = Condition.Operator.EQ; break;
            case "!=": case "<>": comparison = Condition.Operator.NE; break;
            case ">": comparison = Condition.Operator.GT; break;
            case "<": comparison = Condition.Operator.LT; break;
            case ">=": comparison = Condition.Operator.GE; break;
            case "<=": comparison = Condition.Operator.LE; break;
            default: throw error("Unknown comparison: " + operator);
        }
        return new Condition(column, comparison, value());
    }

    private void tokenize(String sql) {
        tokens.clear();
        position = 0;
        if (sql == null) throw error("SQL cannot be null");
        Matcher matcher = TOKEN.matcher(sql);
        int offset = 0;
        while (offset < sql.length()) {
            if (sql.substring(offset).trim().isEmpty()) break;
            matcher.region(offset, sql.length());
            if (!matcher.lookingAt()) throw error("Invalid SQL at character " + offset);
            tokens.add(matcher.group(1));
            offset = matcher.end();
        }
    }

    private boolean peek(String token) {
        return position < tokens.size() && tokens.get(position).equalsIgnoreCase(token);
    }

    private String next() {
        if (position == tokens.size()) throw error("Unexpected end of SQL");
        return tokens.get(position++);
    }

    private boolean take(String token) {
        if (!peek(token)) return false;
        position++;
        return true;
    }

    private void expect(String token) {
        if (!take(token)) throw error("Expected " + token);
    }

    private void end() {
        take(";");
        if (position != tokens.size()) throw error("Unexpected token");
    }

    private String identifier() {
        String token = next();
        if (!token.matches("[a-zA-Z_][a-zA-Z0-9_]*")) throw error("Invalid identifier: " + token);
        return token;
    }

    private Object value() {
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

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(message + " (token " + position + ")");
    }
}
