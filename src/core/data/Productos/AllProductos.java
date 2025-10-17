package core.data.Productos;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton para gestionar todos los productos de la cafetería
 */
public class AllProductos {
    private static AllProductos instance;
    private final List<Producto> productos = new ArrayList<>();
    private final String FILE_PATH = "data/productos.json";
    private int nextId = 1; // simulación de AUTO_INCREMENT

    private AllProductos() {
        loadFromFile();
    }

    public static AllProductos getInstance() {
        if (instance == null) {
            instance = new AllProductos();
        }
        return instance;
    }

    public void addProducto(Producto p) {
        // Si no tiene ID (nuevo registro), asignamos el siguiente disponible
        if (p.getId() == 0) {
            p.setId(nextId++);
        }
        productos.add(p);
        saveToFile();
    }

    public void updateProducto(Producto nuevo) {
        for (int idx = 0; idx < productos.size(); idx++) {
            if (productos.get(idx).getId() == nuevo.getId()) {
                productos.set(idx, nuevo);
                saveToFile();
                return;
            }
        }
    }

    public void removeProducto(int id) {
        productos.removeIf(p -> p.getId() == id);
        saveToFile();
    }

    public Producto getById(int id) {
        return productos.stream()
                .filter(p -> p.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Producto getByNombre(String nombre) {
        return productos.stream()
                .filter(p -> p.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);
    }

    public List<Producto> getAll() {
        return new ArrayList<>(productos);
    }

    public List<Producto> getByCategoria(String categoria) {
        return productos.stream()
                .filter(p -> p.getCategoria() != null && 
                            p.getCategoria().equalsIgnoreCase(categoria))
                .collect(Collectors.toList());
    }

    public List<Producto> getDisponibles() {
        return productos.stream()
                .filter(Producto::isDisponible)
                .collect(Collectors.toList());
    }

    public List<String> getAllCategorias() {
        return productos.stream()
                .map(Producto::getCategoria)
                .filter(cat -> cat != null && !cat.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
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
            productos.clear();

            int maxId = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Producto prod = new Producto(obj);
                productos.add(prod);
                if (prod.getId() > maxId) {
                    maxId = prod.getId();
                }
            }
            nextId = maxId + 1;

        } catch (Exception e) {
            //System.err.println("Error cargando productos desde archivo:");
            e.printStackTrace();
        }
    }

    public void saveToFile() {
        try {
            JSONArray array = new JSONArray();
            for (Producto p : productos) {
                array.put(p.toJson());
            }

            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(array.toString(4)); // JSON legible
            writer.close();

        } catch (Exception e) {
            //System.err.println("Error guardando productos en archivo:");
            e.printStackTrace();
        }
    }

    // Método para obtener estadísticas
    public String getEstadisticas() {
        int total = productos.size();
        int disponibles = (int) productos.stream().filter(Producto::isDisponible).count();
        int categorias = getAllCategorias().size();
        
        return String.format("Total: %d productos | Disponibles: %d | Categorías: %d", 
                           total, disponibles, categorias);
    }
}
