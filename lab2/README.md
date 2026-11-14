# MusicDB

Простая учебная СУБД на Java 8+ и Maven. Команды вводятся в консоль,
данные автоматически сохраняются в JSON после успешного изменения.

## Структура

Все девять основных классов находятся в `src/`.

| Файл | Назначение |
|---|---|
| `Main.java` | Консоль, вывод таблиц и ошибок |
| `Database.java` | Таблицы, операции, внешние ключи, вызов сохранения |
| `Table.java` | Схема, строки, CRUD, проверка типов и первичного ключа |
| `Row.java` | Значения одной записи |
| `Column.java` | Описание столбца и ограничений |
| `DataType.java` | Типы данных и преобразование значений |
| `Condition.java` | Одно сравнение WHERE |
| `QueryParser.java` | Разбор и выполнение команд через Database |
| `JsonStorageManager.java` | Чтение и запись JSON |

## Сборка и запуск

Из каталога `lab2`, при установленных JDK 8+ и Maven:

```bash
mvn clean test
mvn compile
java -cp "target/classes:$HOME/.m2/repository/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar" Main
```

Команда запуска приведена для macOS/Linux и стандартного локального репозитория
Maven. В Windows разделитель элементов classpath — `;`.
Также можно запустить `Main.main` из IDE после импорта Maven-проекта.

По умолчанию используется `data/music.json`. Другой путь можно передать первым
аргументом программы. Папка `data/` и результаты сборки исключены из Git.

## Команды

Одна команда на строку; завершающая `;` необязательна. Имена таблиц и столбцов,
ключевые слова и типы нечувствительны к регистру. Идентификаторы состоят из
латинских букв, цифр и `_`, не начинаются с цифры.

```sql
CREATE TABLE artists (id INTEGER PRIMARY KEY, name STRING NOT NULL, country STRING, active BOOLEAN);
CREATE TABLE albums (id INTEGER PRIMARY KEY, artist_id INTEGER REFERENCES artists(id), title STRING NOT NULL, released DATE);
INSERT INTO artists (id, name, country, active) VALUES (1, 'Dora', 'USA', true), (2, 'O''Brien', NULL, false);
INSERT INTO albums (id, artist_id, title, released) VALUES (1, 1, 'First', '2026-01-01');
SELECT * FROM artists;
SELECT name, country FROM artists WHERE id >= 1;
UPDATE artists SET country = 'UK' WHERE id = 2;
DELETE FROM albums WHERE id = 1;
DROP TABLE albums;
SHOW TABLES;
HELP
EXIT
```

Типы: `INTEGER`, `DOUBLE`, `STRING`, `DATE`, `BOOLEAN`. Даты задаются строками
`YYYY-MM-DD`. Строки заключаются в одинарные кавычки, кавычка внутри — `''`.
`NULL` обозначает отсутствие значения. В каждой таблице обязателен ровно один
первичный ключ; он уникален и не допускает NULL. Остальные столбцы допускают
NULL, если не указано `NOT NULL`. Внешний ключ ссылается на первичный ключ того
же типа; нарушающие ссылку изменения отклоняются.

WHERE поддерживает одно сравнение: `=`, `!=`, `<>`, `>`, `<`, `>=`, `<=`.
Без WHERE операции SELECT, UPDATE и DELETE применяются ко всем строкам.
Сохранено прежнее поведение: `column = NULL` проверяет отсутствие значения.
JOIN, AND/OR, ORDER BY и индексы в минимальную версию не входят.

Изменения выполняются через Database. При ошибке проверки или сохранения
предыдущие строки восстанавливаются. JSON записывается через временный файл.
Загружается также прежний формат со вложенным полем `schema`; старые определения
индексов игнорируются. База рассчитана на один процесс и один поток.

Тесты находятся в `test/DatabaseTest.java`: CRUD,
ограничения, команды SQL, JSON, внешние ключи и восстановление после ошибок.

Индексы создаются командой `CREATE INDEX name ON table(column)`. Индекс
использует `HashMap` и применяется для условий `WHERE column = value`. После
`INSERT`, `UPDATE` и `DELETE` индекс обновляется, а список индексированных
столбцов сохраняется в JSON и восстанавливается при запуске.

Для простого сравнения поиска без индекса и с индексом есть `Benchmark`. Он
сравнивает 1 000, 10 000 и 100 000 строк:

```bash
java -cp "target/classes;путь/к/gson-2.10.1.jar" Benchmark
```
