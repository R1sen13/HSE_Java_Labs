package com.team.dbms.benchmark;

import com.team.dbms.core.*;
import com.team.dbms.storage.StorageManager;
import java.util.*;

/** Standalone timing tool, deliberately separate from JUnit. */
public final class QueryBenchmark {
    private static final int WARMUP = 200;
    private static final int ROUNDS = 9;
    private static final int QUERIES_PER_ROUND = 40;
    private static volatile long consumed;

    private QueryBenchmark() {}

    public static void main(String[] args) {
        int[] sizes = {1000, 10000, 100000};
        if (args.length > 0) {
            sizes = new int[args.length];
            for (int i = 0; i < args.length; i++) sizes[i] = Integer.parseInt(args[i]);
        }
        System.out.println("Java " + System.getProperty("java.version") + ", " +
            System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        System.out.println("rows | scan us/query | index us/query | speedup | prepare ms | build ms");
        for (int size : sizes) run(size);
    }

    private static void run(int size) {
        long start = System.nanoTime();
        final Database database = TestDataGenerator.generate(size);
        DatabaseEngine engine = new DefaultDatabaseEngine(new StorageManager() {
            public Database load() { return database; }
            public void save(Database ignored) { /* In-memory benchmark, no disk I/O. */ }
        });
        double preparationMs = (System.nanoTime() - start) / 1_000_000.0;
        Condition[] conditions = new Condition[8];
        for (int i = 0; i < conditions.length; i++)
            conditions[i] = new Condition("country", Condition.Operator.EQ,
                i == 7 ? "missing" : "country_" + (i * 137));

        List<List<Map<String, Object>>> expected = results(engine, conditions);
        double scan = measure(engine, conditions);
        start = System.nanoTime();
        engine.createIndex("idx_country", "artists", "country");
        double buildMs = (System.nanoTime() - start) / 1_000_000.0;
        if (!expected.equals(results(engine, conditions)))
            throw new IllegalStateException("Indexed results differ from full scan");
        double indexed = measure(engine, conditions);
        System.out.printf(Locale.ROOT, "%d | %.3f | %.3f | %.2fx | %.3f | %.3f%n",
            size, scan / 1000, indexed / 1000, scan / indexed, preparationMs, buildMs);
    }

    private static List<List<Map<String, Object>>> results(DatabaseEngine engine, Condition[] conditions) {
        List<List<Map<String, Object>>> result = new ArrayList<>();
        for (Condition condition : conditions) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Row row : engine.select("artists", null, condition)) rows.add(row.getValues());
            result.add(rows);
        }
        return result;
    }

    private static double measure(DatabaseEngine engine, Condition[] conditions) {
        for (int i = 0; i < WARMUP; i++) query(engine, conditions[i % conditions.length]);
        long[] times = new long[ROUNDS];
        for (int round = 0; round < ROUNDS; round++) {
            long start = System.nanoTime();
            for (int i = 0; i < QUERIES_PER_ROUND; i++) query(engine, conditions[i % conditions.length]);
            times[round] = System.nanoTime() - start;
        }
        Arrays.sort(times);
        return times[ROUNDS / 2] / (double) QUERIES_PER_ROUND;
    }

    private static void query(DatabaseEngine engine, Condition condition) {
        consumed += engine.select("artists", null, condition).size();
    }
}
