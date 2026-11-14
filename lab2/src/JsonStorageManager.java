import com.google.gson.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDate;

public class JsonStorageManager {
    private final Path file;
    private final Gson gson;

    public JsonStorageManager(String filename) {
        this.file = Paths.get(filename);
        this.gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new JsonSerializer<LocalDate>() {
                public JsonElement serialize(LocalDate src, Type t, JsonSerializationContext c) {
                    return new JsonPrimitive(src.toString());
                }
            })
            .registerTypeAdapter(LocalDate.class, new JsonDeserializer<LocalDate>() {
                public LocalDate deserialize(JsonElement json, Type t, JsonDeserializationContext c) {
                    return LocalDate.parse(json.getAsString());
                }
            })
            .setPrettyPrinting()
            .create();
    }

    public Database load() {
        if (!Files.exists(file)) return new Database();
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);
            if (json == null) throw new IllegalArgumentException("Empty database file");
            for (java.util.Map.Entry<String, JsonElement> entry : json.getAsJsonObject("tables").entrySet()) {
                JsonObject table = entry.getValue().getAsJsonObject();
                if (table.has("schema")) {
                    JsonObject schema = table.remove("schema").getAsJsonObject();
                    table.add("name", schema.get("name"));
                    table.add("columns", schema.get("columns"));
                }
            }
            Database db = gson.fromJson(json, Database.class);
            for (Table table : db.getTables().values()) table.restoreTypes();
            db.validateReferences();
            return db;
        } catch (IOException e) {
            throw new RuntimeException("Cannot load database", e);
        }
    }

    public void save(Database database) {
        Path absolute = file.toAbsolutePath();
        Path parent = absolute.getParent();

        try {
            if (parent != null) Files.createDirectories(parent);
            Path temp = absolute.resolveSibling(absolute.getFileName() + ".tmp");

            try (Writer writer = Files.newBufferedWriter(temp)) {
                gson.toJson(database, writer);
            }

            try {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING,
                           StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot save database", e);
        }
    }
}
