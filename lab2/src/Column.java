/** Описание столбца таблицы и его ограничений. */
public class Column {
    private String name;
    private DataType type;
    private boolean primaryKey;
    private boolean nullable;
    private String foreignTable;
    private String foreignColumn;

    public Column() {}

    public Column(String name, DataType type, boolean primaryKey, boolean nullable) {
        this(name, type, primaryKey, nullable, null, null);
    }

    public Column(String name, DataType type, boolean primaryKey, boolean nullable,
                  String foreignTable, String foreignColumn) {
        this.name = name;
        this.type = type;
        this.primaryKey = primaryKey;
        this.nullable = nullable;
        this.foreignTable = foreignTable;
        this.foreignColumn = foreignColumn;
    }

    public String getName() { return name; }
    public DataType getType() { return type; }
    public boolean isPrimaryKey() { return primaryKey; }
    public boolean isNullable() { return nullable; }
    public String getForeignTable() { return foreignTable; }
    public String getForeignColumn() { return foreignColumn; }
}
