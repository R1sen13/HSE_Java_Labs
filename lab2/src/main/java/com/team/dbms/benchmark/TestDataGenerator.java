package com.team.dbms.benchmark;

import com.team.dbms.core.*;
import java.util.*;

public final class TestDataGenerator {
    private TestDataGenerator() {}

    public static Database generate(int size) {
        if (size < 0) throw new IllegalArgumentException("Negative dataset size");
        Database database = new Database();
        Table table = new Table(new TableSchema("artists", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("name", DataType.STRING, false, false),
            new Column("country", DataType.STRING, false, true)
        )));
        Random random = new Random(42);
        // Fixture preparation only: known valid types and unique keys.
        // Avoid quadratic PK validation in Table.insert when building 100,000 rows.
        for (int i = 0; i < size; i++)
            table.getRows().add(new Row().set("id", i).set("name", "Artist " + i)
                .set("country", "country_" + random.nextInt(1000)));
        database.addTable(table);
        return database;
    }
}
