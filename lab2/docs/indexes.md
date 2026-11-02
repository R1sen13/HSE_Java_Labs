# Индексы: диаграммы фактической реализации

Определения хранятся в Database, менеджер создаёт и регистрирует HashIndex на Table.
Поиск и обновление ссылок выполняет Table, поэтому SQL не дублирует CRUD ядра.

## Классы

```mermaid
classDiagram
    class SqlParser {
        +parse(sql) SqlCommand
    }
    class SqlCommand
    class QueryExecutor {
        +execute(sql) Object
        +execute(command) Object
    }
    class DatabaseEngine {
        <<interface>>
        +getSchema(table) TableSchema
        +createIndex(name, table, column)
        +select(table, columns, condition) List
        +insert(table, rows)
        +update(table, values, condition) int
        +delete(table, condition) int
    }
    class DefaultDatabaseEngine
    class Database {
        +getIndexManager() IndexManager
        +getIndexDefinitions() List
    }
    class Definition {
        -String name
        -String table
        -String column
    }
    class IndexManager {
        +create(name, table, column)
        +getIndex(table, column) HashIndex
        +rebuild()
        +removeForTable(name)
    }
    class HashIndex {
        -HashMap groups
        -IdentityHashMap order
        +add(row)
        +replace(oldRow, newRow)
        +remove(row)
        +find(value) List
    }
    class Table {
        +registerIndex(index)
        +select(columns, condition) List
        +insert(row)
        +update(values, condition) int
        +delete(condition) int
        +restoreRows(backup)
        +restoreTypes()
    }
    class Row {
        +get(column) Object
        +set(column, value) Row
    }
    class StorageManager {
        <<interface>>
        +load() Database
        +save(database)
    }
    class JsonStorageManager
    QueryExecutor --> SqlParser
    SqlParser ..> SqlCommand : creates
    QueryExecutor ..> SqlCommand : executes
    QueryExecutor --> DatabaseEngine
    DefaultDatabaseEngine ..|> DatabaseEngine
    DefaultDatabaseEngine --> Database
    DefaultDatabaseEngine --> StorageManager
    JsonStorageManager ..|> StorageManager
    JsonStorageManager ..> Database : JSON
    JsonStorageManager ..> Table : restoreTypes
    Database "1" *-- "*" Table
    Database "1" *-- "*" Definition : persisted indexes
    Database --> IndexManager : transient
    IndexManager --> Database
    IndexManager ..> HashIndex : creates
    IndexManager ..> Table : registers and reads
    Table "1" *-- "*" Row
    Table "1" o-- "*" HashIndex : transient
    HashIndex --> Row : live references
```

## Создание индекса

```mermaid
sequenceDiagram
    actor User as Пользователь
    participant Q as QueryExecutor
    participant P as SqlParser
    participant E as DefaultDatabaseEngine
    participant D as Database
    participant M as IndexManager
    participant T as Table
    participant H as HashIndex
    participant S as StorageManager
    User->>Q: execute(CREATE INDEX ...)
    Q->>P: parse(sql)
    P-->>Q: SqlCommand
    Q->>E: getSchema(table), createIndex(name, table, column)
    E->>D: getIndexManager()
    D-->>E: manager
    E->>M: create(name, table, column)
    M->>D: проверка имён, getTable(table)
    M->>T: getSchema(), getIndex(column)
    M->>H: new HashIndex(column)
    M->>T: getRows()
    loop существующие строки
        M->>H: add(row)
    end
    M->>T: registerIndex(index)
    M->>D: добавить Definition(name, table, column)
    E->>S: save(database)
    Q-->>User: null
```

## SELECT с индексом

```mermaid
sequenceDiagram
    actor User as Пользователь
    participant Q as QueryExecutor
    participant P as SqlParser
    participant E as DefaultDatabaseEngine
    participant T as Table
    participant H as HashIndex
    User->>Q: execute(SELECT ... WHERE country = 'USA')
    Q->>P: parse(sql)
    P-->>Q: SqlCommand
    Q->>E: getSchema(table)
    Note over Q: DataType.convert литерала условия
    Q->>E: select(table, columns, condition)
    E->>T: select(columns, condition)
    Note over T: getIndex(column): индекс уже зарегистрирован IndexManager
    alt EQ и совместимый тип ключа
        T->>H: find(value)
        H-->>T: подходящие Row в порядке таблицы
    else нет подходящего индекса
        Note over T: полный перебор getRows()
    end
    Note over T: Condition.matches и копии/проекция строк
    T-->>E: List of Row
    E-->>Q: List of Row
    Q-->>User: результат
```

## INSERT, UPDATE и DELETE

```mermaid
sequenceDiagram
    actor User as Пользователь
    participant Q as QueryExecutor
    participant P as SqlParser
    participant E as DefaultDatabaseEngine
    participant T as Table
    participant H as HashIndex
    participant S as StorageManager
    User->>Q: execute(INSERT / UPDATE / DELETE)
    Q->>P: parse(sql)
    P-->>Q: SqlCommand
    Q->>E: CRUD(table, ...)
    Note over E: резервный список прежних Row
    alt INSERT
        E->>E: преобразование типов, проверка FK
        E->>T: insert(row)
        Note over T: нормализация, проверка PK, добавление Row
        T->>H: add(сохранённый Row) для каждого индекса
    else UPDATE
        E->>T: update(values, condition)
        Note over T: старые Row сохранены, новые Row нормализованы, PK проверен
        loop изменённые строки и все индексы
            T->>H: replace(oldRow, newRow)
            Note over H: прежний ключ — замена ссылки; новый — перенос группы
        end
        E->>E: проверка FK
    else DELETE
        E->>E: проверка ссылок других таблиц
        E->>T: delete(condition)
        loop удаляемые строки и все индексы
            T->>H: remove(row)
            Note over H: удалить пустую группу
        end
    end
    E->>S: save(database)
    alt ошибка изменения / сохранения
        E->>T: restoreRows(backup)
        Note over T,H: восстановление строк и перестройка индексов
        E-->>Q: исключение
    else успех
        E-->>Q: результат CRUD
        Q-->>User: число строк
    end
```

## Загрузка и восстановление

```mermaid
sequenceDiagram
    actor User as Приложение
    participant E as DefaultDatabaseEngine
    participant S as JsonStorageManager
    participant D as Database
    participant T as Table
    participant M as IndexManager
    participant H as HashIndex
    User->>E: new DefaultDatabaseEngine(storage)
    E->>S: load()
    S->>D: Gson.fromJson: таблицы, строки, определения
    loop все таблицы
        S->>T: restoreTypes()
        Note over T: преобразование значений согласно DataType
    end
    S->>D: getIndexManager()
    D->>M: new IndexManager(database), rebuild()
    loop все таблицы
        M->>T: clearIndexes()
    end
    loop определения индексов
        M->>D: getTable(definition.table)
        M->>H: new HashIndex(column)
        M->>T: getRows()
        loop загруженные строки
            M->>H: add(row)
        end
        M->>T: registerIndex(index)
    end
    S-->>E: Database
    E->>D: getIndexManager() — уже создан
    E-->>User: движок готов
```
