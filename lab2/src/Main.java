import java.util.*;

/** Консольная точка входа MusicDB. */
public class Main {
    public static void main(String[] args) {
        String filename = args.length == 0 ? "data/music.json" : args[0];
        Database database;
        try {
            database = new Database(new JsonStorageManager(filename));
        } catch (RuntimeException e) {
            System.err.println("Cannot open database: " + e.getMessage());
            return;
        }
        QueryParser parser = new QueryParser();
        System.out.println("MusicDB. One command per line. HELP for examples, EXIT to quit.");
        try (Scanner input = new Scanner(System.in, "UTF-8")) {
            while (true) {
                if (System.console() != null) System.out.print("db> ");
                if (!input.hasNextLine()) break;
                String command = input.nextLine().trim();
                if (command.isEmpty()) continue;
                if (command.equalsIgnoreCase("EXIT") || command.equalsIgnoreCase("EXIT;")) break;
                try {
                    if (command.equalsIgnoreCase("HELP")) {
                        printHelp();
                    } else if (command.equalsIgnoreCase("SHOW TABLES") || command.equalsIgnoreCase("SHOW TABLES;")) {
                        for (String name : database.getTables().keySet()) System.out.println(name);
                        System.out.println(database.getTables().size() + " table(s)");
                    } else {
                        printResult(parser.execute(command, database));
                    }
                } catch (RuntimeException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        }
    }

    private static void printHelp() {
        System.out.println("CREATE TABLE artists (id INTEGER PRIMARY KEY, name STRING NOT NULL);");
        System.out.println("INSERT INTO artists (id, name) VALUES (1, 'Dora');");
        System.out.println("SELECT * FROM artists WHERE id = 1;");
        System.out.println("UPDATE artists SET name = 'New' WHERE id = 1;");
        System.out.println("DELETE FROM artists WHERE id = 1;");
        System.out.println("DROP TABLE artists;");
        System.out.println("SHOW TABLES;");
        System.out.println("EXIT");
    }

    @SuppressWarnings("unchecked")
    private static void printResult(Object result) {
        if (!(result instanceof List)) {
            if (result instanceof Integer) System.out.println(result + " row(s) affected");
            else System.out.println(result);
            return;
        }
        List<Row> rows = (List<Row>) result;
        if (rows.isEmpty()) {
            System.out.println("0 rows");
            return;
        }
        List<String> columns = new ArrayList<>(rows.get(0).getValues().keySet());
        int[] widths = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            widths[i] = columns.get(i).length();
            for (Row row : rows)
                widths[i] = Math.max(widths[i], String.valueOf(row.get(columns.get(i))).length());
        }
        printLine(columns, widths);
        for (Row row : rows) {
            List<String> values = new ArrayList<>();
            for (String column : columns) values.add(String.valueOf(row.get(column)));
            printLine(values, widths);
        }
        System.out.println(rows.size() + " row(s)");
    }

    private static void printLine(List<String> values, int[] widths) {
        for (int i = 0; i < values.size(); i++) System.out.printf("| %-" + widths[i] + "s ", values.get(i));
        System.out.println("|");
    }
}
