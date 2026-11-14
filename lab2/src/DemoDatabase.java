public final class DemoDatabase {
    private DemoDatabase() {}

    public static Database create(JsonStorageManager storage) {
        Database existing = new Database(storage);
        if (!existing.getTables().isEmpty())
            throw new IllegalArgumentException("Demo data can only be loaded into an empty database");
        Database demo = new Database();
        QueryParser parser = new QueryParser();
        String[] commands = {
            "CREATE TABLE artists (id INTEGER PRIMARY KEY, name STRING NOT NULL, country STRING, active BOOLEAN);",
            "CREATE TABLE songs (id INTEGER PRIMARY KEY, artist_id INTEGER REFERENCES artists(id), " +
                "title STRING NOT NULL, collaborators STRING, popularity_rank INTEGER);",
            "INSERT INTO artists VALUES (1, 'Дора', 'Россия', TRUE), " +
                "(2, 'Тёмный принц', 'Россия', TRUE), (3, 'madk1d', 'Россия', TRUE);",
            "INSERT INTO songs VALUES " +
                "(1, 2, 'Malo 2.0', 'ЕГОР КРИД, OG Buda, Toxi$, Мэйби Бэйби, Baby Cute, Дора, madk1d', 1), " +
                "(2, 2, 'ты че обиделась', 'madk1d', 2), " +
                "(3, 2, 'Jealous', '9mice, ЕГОР КРИД, madk1d', 3), " +
                "(4, 3, 'ДИНАСТИЯ', 'VILLIAN', 1), " +
                "(5, 3, '8 миля', 'VILLIAN', 2), " +
                "(6, 3, 'так по***', NULL, 3);",
            "CREATE INDEX songs_artist ON songs(artist_id);"
        };
        for (String command : commands) parser.execute(command, demo);
        storage.save(demo);
        return new Database(storage);
    }
}
