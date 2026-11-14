import java.util.*;

/** Простейшее сравнение выборки с индексом и без него. */
public class Benchmark {
    public static void main(String[] args) {
        int[] sizes = {1000, 10000, 100000};
        for (int size : sizes) {
            Database plain = new Database();
            Database indexed = new Database();
            plain.createTable(songsTable());
            indexed.createTable(songsTable());
            indexed.createIndex("songs", "artist");
            List<Row> rows = new ArrayList<>();
            for (int i = 0; i < size; i++)
                rows.add(new Row().set("id", i).set("artist", "artist-" + (i % 100)));
            plain.insert("songs", rows);
            indexed.insert("songs", rows);
            System.out.println(size + " rows: without index=" + measure(plain, size) +
                " ns, with index=" + measure(indexed, size) + " ns");
        }
    }

    private static Table songsTable() {
        return new Table("songs", Arrays.asList(
            new Column("id", DataType.INTEGER, true, false),
            new Column("artist", DataType.STRING, false, false)
        ));
    }

    private static long measure(Database database, int size) {
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++)
            database.select("songs", null,
                new Condition("artist", Condition.Operator.EQ, "artist-" + (i % 100)));
        return System.nanoTime() - start;
    }
}
