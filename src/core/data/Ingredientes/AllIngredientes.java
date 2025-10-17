package core.data.Ingredientes;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class AllIngredientes {
    private static AllIngredientes instance;
    private final List<Ingrediente> ingredientes = new ArrayList<>();
    private final String FILE_PATH = "data/ingredientes.json";
    private int nextId = 1; // simulación de AUTO_INCREMENT

    private AllIngredientes() {
        loadFromFile();
    }

    public static AllIngredientes getInstance() {
        if (instance == null) {
            instance = new AllIngredientes();
        }
        return instance;
    }

    public void addIngrediente(Ingrediente i) {
        // Si no tiene ID (nuevo registro), asignamos el siguiente disponible
        if (i.getId() == 0) {
            i.setId(nextId++);
        }
        ingredientes.add(i);
        saveToFile();
    }

    public void updateIngrediente(Ingrediente nuevo) {
        for (int idx = 0; idx < ingredientes.size(); idx++) {
            if (ingredientes.get(idx).getId() == nuevo.getId()) {
                ingredientes.set(idx, nuevo);
                saveToFile();
                return;
            }
        }
    }

    public void removeIngrediente(int id) {
        ingredientes.removeIf(i -> i.getId() == id);
        saveToFile();
    }

    public Ingrediente getById(int id) {
        return ingredientes.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
    }

    public Ingrediente getByNombre(String nombre) {
        return ingredientes.stream()
                .filter(i -> i.getNombre().equalsIgnoreCase(nombre))
                .findFirst().orElse(null);
    }

    public List<Ingrediente> getAll() {
        return new ArrayList<>(ingredientes);
    }

    private void loadFromFile() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                saveToFile(); // crea archivo vacío
                return;
            }

            String content = Files.readString(file.toPath());
            if (content.isBlank()) return;

            JSONArray array = new JSONArray(content);
            ingredientes.clear();

            int maxId = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Ingrediente ing = new Ingrediente(obj);
                ingredientes.add(ing);
                if (ing.getId() > maxId) {
                    maxId = ing.getId();
                }
            }
            nextId = maxId + 1;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveToFile() {
        try {
            JSONArray array = new JSONArray();
            for (Ingrediente i : ingredientes) {
                array.put(i.toJson());
            }

            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(array.toString(4)); // JSON legible
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}