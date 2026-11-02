# API ядра для команды

```java
public interface DatabaseEngine {
    void createTable(TableSchema schema);
    void dropTable(String tableName);
    void insert(String tableName, List<Row> rows);
    List<Row> select(String tableName, List<String> columns, Condition condition);
    int update(String tableName, Map<String, Object> values, Condition condition);
    int delete(String tableName, Condition condition);
}
```

- `columns == null` или пустой список означает SELECT *.
- `condition == null` означает отсутствие WHERE.
- Даты передаются как LocalDate.
- Имена таблиц и столбцов нечувствительны к регистру.
