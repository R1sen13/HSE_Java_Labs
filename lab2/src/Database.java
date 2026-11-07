import java.util.*;

public class Database {
    private Map<String, Table> tables = new LinkedHashMap<>();
    private transient JsonStorageManager storage;

    public Database() {}

    public Database(JsonStorageManager storage) {
        this.tables = storage.load().tables;
        this.storage = storage;
    }

    public Map<String, Table> getTables() { return Collections.unmodifiableMap(tables); }

    public Table getTable(String name) {
        for (Table table : tables.values())
            if (table.getName().equalsIgnoreCase(name)) return table;
        throw new IllegalArgumentException("Unknown table: " + name);
    }

    private void save() {
        if (storage != null) storage.save(this);
    }

    public void createTable(Table table) {
        for (Table existing : tables.values())
            if (existing.getName().equalsIgnoreCase(table.getName()))
                throw new IllegalArgumentException("Table already exists: " + table.getName());
        tables.put(table.getName(), table);
        try {
            validateReferences();
            save();
        } catch (RuntimeException e) {
            tables.remove(table.getName());
            throw e;
        }
    }

    public void dropTable(String name) {
        Table target = getTable(name);
        for (Table table : tables.values()) {
            if (table == target) continue;
            for (Column column : table.getColumns())
                if (name.equalsIgnoreCase(column.getForeignTable()))
                    throw new IllegalArgumentException("Table is referenced by " + table.getName());
        }
        Map<String, Table> backup = new LinkedHashMap<>(tables);
        tables.remove(target.getName());
        try {
            save();
        } catch (RuntimeException e) {
            tables = backup;
            throw e;
        }
    }

    public void validateReferences() {
        for (Table table : tables.values()) {
            for (Column column : table.getColumns()) {
                if (column.getForeignTable() == null) continue;
                Table referenced = getTable(column.getForeignTable());
                Column key = referenced.getColumn(column.getForeignColumn());
                if (!key.isPrimaryKey() || key.getType() != column.getType())
                    throw new IllegalArgumentException("Foreign key must reference a primary key of the same type");
                for (Row row : table.getRows()) {
                    Object value = row.get(column.getName());
                    if (value == null) continue;
                    boolean found = false;
                    for (Row candidate : referenced.getRows()) {
                        if (Objects.equals(candidate.get(key.getName()), value)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) throw new IllegalArgumentException("Foreign key violation: " + column.getName());
                }
            }
        }
    }

    public void insert(String name, List<Row> rows) {
        Table table = getTable(name);
        List<Row> backup = new ArrayList<>(table.getRows());
        try {
            for (Row row : rows) table.insert(row);
            validateReferences();
            save();
        } catch (RuntimeException e) {
            table.restoreRows(backup);
            throw e;
        }
    }

    public List<Row> select(String name, List<String> columns, Condition condition) {
        return getTable(name).select(columns, condition);
    }

    public int update(String name, Map<String, Object> values, Condition condition) {
        Table table = getTable(name);
        List<Row> backup = new ArrayList<>(table.getRows());
        try {
            int count = table.update(values, condition);
            validateReferences();
            save();
            return count;
        } catch (RuntimeException e) {
            table.restoreRows(backup);
            throw e;
        }
    }

    public int delete(String name, Condition condition) {
        Table table = getTable(name);
        List<Row> backup = new ArrayList<>(table.getRows());
        try {
            int count = table.delete(condition);
            validateReferences();
            save();
            return count;
        } catch (RuntimeException e) {
            table.restoreRows(backup);
            throw e;
        }
    }
}
