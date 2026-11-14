import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DemoDatabaseTest {
    @TempDir Path tempDir;

    @Test
    void demoContainsArtistsSongsAndPersistentIndex() {
        JsonStorageManager storage = new JsonStorageManager(tempDir.resolve("demo.json").toString());
        Database db = DemoDatabase.create(storage);
        assertEquals(4, db.select("artists", null, null).size());
        assertEquals("Россия", db.select("artists", null,
            new Condition("name", Condition.Operator.EQ, "Дора")).get(0).get("country"));
        assertEquals(3, db.select("songs", null, new Condition("artist_id", Condition.Operator.EQ, 2)).size());
        assertEquals(2, db.select("songs", null, new Condition("artist_id", Condition.Operator.EQ, 3)).size());
        assertEquals("Серёга Пират", db.select("artists", null,
            new Condition("id", Condition.Operator.EQ, 4)).get(0).get("name"));
        Database loaded = new Database(storage);
        List<String> pirateTitles = new ArrayList<>();
        for (Row row : loaded.select("songs", null, new Condition("artist_id", Condition.Operator.EQ, 4)))
            pirateTitles.add((String) row.get("title"));
        assertEquals(Arrays.asList("Мой байк", "Тп на аме", "Гимн Дахака", "Маша",
            "Я не пойду с тобой гулять"), pirateTitles);
        assertTrue(loaded.select("songs", null, new Condition("id", Condition.Operator.EQ, 6)).isEmpty());
        assertEquals(Arrays.asList("artist_id"), loaded.getTable("songs").getIndexedColumns());
        assertEquals(10, loaded.select("songs", null, null).size());
        assertThrows(IllegalArgumentException.class,
            () -> loaded.delete("artists", new Condition("id", Condition.Operator.EQ, 2)));
        loaded.update("artists", Collections.singletonMap("name", "Новое имя"),
            new Condition("id", Condition.Operator.EQ, 1));
        assertEquals("Новое имя", new Database(storage).select("artists", null,
            new Condition("id", Condition.Operator.EQ, 1)).get(0).get("name"));
        assertThrows(IllegalArgumentException.class, () -> DemoDatabase.create(storage));
        assertEquals("Новое имя", new Database(storage).select("artists", null,
            new Condition("id", Condition.Operator.EQ, 1)).get(0).get("name"));
    }

    @Test
    void startupChoiceLoadsDemoAndRepeatedStartupPreservesChanges() throws Exception {
        String filename = tempDir.resolve("chosen.json").toString();
        String output = runMain("wrong\n1\nDELETE FROM songs WHERE id = 1;\nEXIT\n", filename);
        assertTrue(output.contains("Enter 1 or 2."));
        assertTrue(output.contains("Demo database loaded: 4 artists and 10 songs."));
        assertEquals(9, new Database(new JsonStorageManager(filename)).select("songs", null, null).size());
        output = runMain("SELECT * FROM songs;\nEXIT\n", filename, "--demo");
        assertTrue(output.contains("9 row(s)"));
        assertFalse(output.contains("Choose database:"));
        assertEquals(9, new Database(new JsonStorageManager(filename)).select("songs", null, null).size());
    }

    @Test
    void emptyModeSupportsChoiceAndScriptedInput() throws Exception {
        String chosen = tempDir.resolve("empty-choice.json").toString();
        runMain("2\nEXIT\n", chosen);
        assertTrue(new Database(new JsonStorageManager(chosen)).getTables().isEmpty());
        String scripted = tempDir.resolve("empty-script.json").toString();
        String output = runMain("CREATE TABLE songs (id INTEGER PRIMARY KEY);\nEXIT\n", scripted, "--empty");
        assertFalse(output.contains("Choose database:"));
        assertEquals(1, new Database(new JsonStorageManager(scripted)).getTables().size());
    }

    @Test
    void demoFlagSkipsPrompt() throws Exception {
        String filename = tempDir.resolve("flag.json").toString();
        String output = runMain("SELECT name FROM artists;\nEXIT\n", "--demo", filename);
        assertFalse(output.contains("Choose database:"));
        assertTrue(output.contains("Дора"));
        assertTrue(output.contains("Серёга Пират"));
        assertTrue(output.contains("4 row(s)"));
    }

    private String runMain(String input, String... args) throws Exception {
        InputStream previousIn = System.in;
        PrintStream previousOut = System.out;
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes("UTF-8")));
            System.setOut(new PrintStream(output, true, "UTF-8"));
            Main.main(args);
            return output.toString("UTF-8");
        } finally {
            System.setIn(previousIn);
            System.setOut(previousOut);
        }
    }
}
