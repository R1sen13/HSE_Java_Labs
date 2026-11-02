package com.team.dbms.core;

import java.time.LocalDate;
import java.util.*;

public final class MusicDatabaseSchema {
    private MusicDatabaseSchema() {}

    public static void createTables(DatabaseEngine db) {
        db.createTable(new TableSchema("artists", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("name", DataType.STRING, false, false),
            new Column("country", DataType.STRING, false, true),
            new Column("is_active", DataType.BOOLEAN, false, false)
        )));

        db.createTable(new TableSchema("albums", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("artist_id", DataType.INTEGER, false, false, "artists", "id"),
            new Column("title", DataType.STRING, false, false),
            new Column("release_date", DataType.DATE, false, true),
            new Column("is_single", DataType.BOOLEAN, false, false)
        )));

        db.createTable(new TableSchema("songs", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("album_id", DataType.INTEGER, false, false, "albums", "id"),
            new Column("title", DataType.STRING, false, false),
            new Column("duration", DataType.DOUBLE, false, false),
            new Column("plays", DataType.INTEGER, false, false),
            new Column("likes", DataType.INTEGER, false, false),
            new Column("is_explicit", DataType.BOOLEAN, false, false)
        )));
    }
}
