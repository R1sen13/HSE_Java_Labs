import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filename = "data/music.json";
        String mode = null;
        boolean hasFilename = false;
        for (String arg : args) {
            if (arg.equals("--demo") || arg.equals("--empty")) {
                if (mode != null) {
                    System.err.println("Choose only one mode: --demo or --empty");
                    return;
                }
                mode = arg;
            } else if (arg.startsWith("--") || hasFilename) {
                System.err.println("Usage: Main [database.json] [--demo | --empty]");
                return;
            } else {
                filename = arg;
                hasFilename = true;
            }
        }
        JsonStorageManager storage = new JsonStorageManager(filename);
        Database database;
        try {
            database = new Database(storage);
        } catch (RuntimeException e) {
            System.err.println("Cannot open database: " + e.getMessage());
            return;
        }
        QueryParser parser = new QueryParser();
        System.out.println("MusicDB. One command per line. HELP for examples, EXIT to quit.");
        try (Scanner input = new Scanner(System.in, "UTF-8")) {
            if (database.getTables().isEmpty()) {
                if (mode == null) mode = chooseMode(input);
                if ("--demo".equals(mode)) {
                    try {
                        database = DemoDatabase.create(storage);
                        System.out.println("Demo database loaded: 3 artists and 6 songs.");
                    } catch (RuntimeException e) {
                        System.err.println("Cannot create demo database: " + e.getMessage());
                        return;
                    }
                }
            } else if ("--demo".equals(mode)) {
                System.out.println("Existing database opened; demo data was not added.");
            }
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

    private static String chooseMode(Scanner input) {
        while (true) {
            System.out.println("Choose database: 1 - ready-made music demo, 2 - empty database (default).");
            if (!input.hasNextLine()) return "--empty";
            String choice = input.nextLine().trim();
            if (choice.equals("1")) return "--demo";
            if (choice.isEmpty() || choice.equals("2")) return "--empty";
            System.out.println("Enter 1 or 2.");
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
