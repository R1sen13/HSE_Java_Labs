import java.util.*;

/** Типизированная таблица со строками, первичным ключом и индексами. */

public class Table {
    private String name;
    private List<Column> columns;
    private List<Row> rows = new ArrayList<>();
    private List<String> indexedColumns = new ArrayList<>();
    private transient Map<String, Map<Object, List<Row>>> indexes = new HashMap<>();

    public Table(String name, List<Column> columns) {
        this.name = name;
        this.columns = new ArrayList<>(columns);
        validateSchema();
    }

    private void validateSchema() {
        if (name == null || !name.matches("[a-zA-Z_][a-zA-Z0-9_]*"))
            throw new IllegalArgumentException("Invalid table name: " + name);
        Set<String> names = new HashSet<>();
        int keys = 0;
        for (Column column : columns) {
            if (column.getName() == null || !column.getName().matches("[a-zA-Z_][a-zA-Z0-9_]*"))
                throw new IllegalArgumentException("Invalid column name");
            if (!names.add(column.getName().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException("Duplicate column: " + column.getName());
            if (column.getType() == null) throw new IllegalArgumentException("Missing column type");
            if (column.isPrimaryKey()) keys++;
        }
        if (keys != 1) throw new IllegalArgumentException("Exactly one primary key is required");
    }

    public String getName() { return name; }
    public List<Column> getColumns() { return Collections.unmodifiableList(columns); }
    public List<Row> getRows() { return Collections.unmodifiableList(rows); }
    public List<String> getIndexedColumns() { return Collections.unmodifiableList(indexedColumns); }

    public Column getColumn(String name) {
        for (Column column : columns)
            if (column.getName().equalsIgnoreCase(name)) return column;
        throw new IllegalArgumentException("Unknown column: " + name);
    }

    public Column getPrimaryKey() {
        for (Column column : columns)
            if (column.isPrimaryKey()) return column;
        throw new IllegalArgumentException("Missing primary key");
    }

    public void restoreRows(List<Row> backup) {
        rows = new ArrayList<>(backup);
        rebuildIndexes();
    }

    public void restoreTypes() {
        validateSchema();
        if (indexedColumns == null) indexedColumns = new ArrayList<>();
        indexes = new HashMap<>();
        List<Row> saved = new ArrayList<>(rows);
        rows.clear();
        for (Row row : saved) insert(row);
        rebuildIndexes();
    }

    public void createIndex(String columnName) {
        Column column = getColumn(columnName);
        String name = column.getName();
        for (String existing : indexedColumns)
            if (existing.equalsIgnoreCase(name))
                throw new IllegalArgumentException("Index already exists: " + name);
        indexedColumns.add(name);
        rebuildIndexes();
    }

    public void restoreIndexes(List<String> columns) {
        indexedColumns = new ArrayList<>(columns);
        rebuildIndexes();
    }

    private void rebuildIndexes() {
        if (indexes == null) indexes = new HashMap<>();
        indexes.clear();
        for (String column : indexedColumns) indexes.put(column.toLowerCase(Locale.ROOT), new HashMap<>());
        for (Row row : rows) addToIndexes(row);
    }

    private void addToIndexes(Row row) {
        for (String column : indexedColumns) {
            Object value = row.get(column);
            if (value == null) continue;
            Map<Object, List<Row>> index = indexes.get(column.toLowerCase(Locale.ROOT));
            index.computeIfAbsent(value, key -> new ArrayList<>()).add(row);
        }
    }

    private Row normalize(Row input) {
        for (String key : input.getValues().keySet()) getColumn(key);
        Row result = new Row();
        for (Column column : columns) {
            Object value = input.get(column.getName());
            if (value == null && (column.isPrimaryKey() || !column.isNullable()))
                throw new IllegalArgumentException("Column cannot be null: " + column.getName());
            result.set(column.getName(), column.getType().convert(value));
        }
        return result;
    }

    private Condition normalizeCondition(Condition condition) {
        if (condition == null) return null;
        Column column = getColumn(condition.getColumn());
        return new Condition(column.getName(), condition.getOperator(),
            column.getType().convert(condition.getValue()));
    }

    public void insert(Row input) {
        Row normalized = normalize(input);
        String key = getPrimaryKey().getName();
        for (Row row : rows)
            if (Objects.equals(row.get(key), normalized.get(key)))
                throw new IllegalArgumentException("Duplicate primary key: " + normalized.get(key));
        rows.add(normalized);
        addToIndexes(normalized);
    }

    public List<Row> select(List<String> selected, Condition condition) {
        condition = normalizeCondition(condition);
        if (selected != null)
            for (String column : selected) getColumn(column);
        Collection<Row> source = rows;
        if (condition != null && condition.getOperator() == Condition.Operator.EQ && indexes != null) {
            Map<Object, List<Row>> index = indexes.get(condition.getColumn().toLowerCase(Locale.ROOT));
            if (index != null) source = index.getOrDefault(condition.getValue(), Collections.emptyList());
        }
        List<Row> result = new ArrayList<>();
        for (Row row : source) {
            if (condition != null && !condition.matches(row)) continue;
            Row copy = new Row();
            if (selected == null || selected.isEmpty()) {
                copy = new Row(row.getValues());
            } else {
                for (String column : selected) copy.set(column, row.get(column));
            }
            result.add(copy);
        }
        return result;
    }

    public int update(Map<String, Object> values, Condition condition) {
        condition = normalizeCondition(condition);
        for (String column : values.keySet()) getColumn(column);
        List<Row> changedRows = new ArrayList<>();
        Set<Object> keys = new HashSet<>();
        String primaryKey = getPrimaryKey().getName();
        int count = 0;
        for (Row row : rows) {
            Row changed = row;
            if (condition == null || condition.matches(row)) {
                changed = new Row(row.getValues());
                for (Map.Entry<String, Object> entry : values.entrySet())
                    changed.set(getColumn(entry.getKey()).getName(), entry.getValue());
                changed = normalize(changed);
                count++;
            }
            if (!keys.add(changed.get(primaryKey)))
                throw new IllegalArgumentException("Duplicate primary key");
            changedRows.add(changed);
        }
        rows = changedRows;
        rebuildIndexes();
        return count;
    }

    public int delete(Condition condition) {
        condition = normalizeCondition(condition);
        int before = rows.size();
        Iterator<Row> iterator = rows.iterator();
        while (iterator.hasNext()) {
            Row row = iterator.next();
            if (condition == null || condition.matches(row)) iterator.remove();
        }
        rebuildIndexes();
        return before - rows.size();
    }
}
