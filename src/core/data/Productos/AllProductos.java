package core.data.Productos;

import core.services.ProductoService;
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
 * Ahora conectado al servidor
 */
public class AllProductos {
    private static AllProductos instance;
    private final List<Producto> productos = new ArrayList<>();
    private final String FILE_PATH = "data/productos.json";

    private AllProductos() {
        loadFromServer();
        // También cargar desde archivo local como respaldo
        loadFromFile();
    }

    public static AllProductos getInstance() {
        if (instance == null) {
            instance = new AllProductos();
        }
        return instance;
    }

    /**
     * Cargar productos desde el servidor
     */
    private void loadFromServer() {
        ProductoService.listProductos(new ProductoService.ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> productosJson) {
                productos.clear();
                
                for (JSONObject json : productosJson) {
                    try {
                        Producto producto = Producto.fromJSON(json);
                        productos.add(producto);
                    } catch (Exception e) {
                        System.err.println("Error al convertir JSON a Producto: " + e.getMessage());
                    }
                }
                
                System.out.println("Cargados " + productos.size() + " productos desde servidor");
                
                // Guardar en archivo local como cache
                saveToFile();
            }

            @Override
            public void onError(String error) {
                System.err.println("Error al cargar productos desde servidor: " + error);
                // Si falla el servidor, usar el archivo local
                loadFromFile();
            }
        });
    }

    /**
     * Cargar productos desde archivo local (como respaldo)
     */
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
            
            // Si ya tenemos productos del servidor, no sobreescribir
            if (productos.isEmpty()) {
                productos.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    Producto prod = Producto.fromJSON(obj);
                    productos.add(prod);
                }
                System.out.println("Cargados " + productos.size() + " productos desde archivo local");
            }

        } catch (Exception e) {
            System.err.println("Error cargando productos desde archivo:");
            e.printStackTrace();
        }
    }

    /**
     * Guardar productos en archivo local como cache
     */
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
            System.err.println("Error guardando productos en archivo:");
            e.printStackTrace();
        }
    }

    /**
     * Recargar productos desde servidor
     */
    public void recargarDesdeServidor() {
        loadFromServer();
    }

    /**
     * Agregar producto (tanto en servidor como local)
     */
    public void addProducto(Producto p, Runnable onSuccess, Runnable onError) {
        ProductoService.createProducto(p, new ProductoService.CrudCallback() {
            @Override
            public void onSuccess() {
                // Después de guardar en servidor, recargar
                loadFromServer();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("Error al agregar producto: " + error);
                if (onError != null) {
                    onError.run();
                }
            }
        });
    }

    /**
     * Actualizar producto en servidor
     */
    public void updateProducto(Producto nuevo, Runnable onSuccess, Runnable onError) {
        ProductoService.updateProducto(nuevo, new ProductoService.CrudCallback() {
            @Override
            public void onSuccess() {
                // Actualizar en lista local
                for (int idx = 0; idx < productos.size(); idx++) {
                    if (productos.get(idx).getId() == nuevo.getId()) {
                        productos.set(idx, nuevo);
                        break;
                    }
                }
                saveToFile();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("Error al actualizar producto: " + error);
                if (onError != null) {
                    onError.run();
                }
            }
        });
    }

    /**
     * Eliminar producto del servidor
     */
    public void removeProducto(int id, Runnable onSuccess, Runnable onError) {
        ProductoService.deleteProducto(id, new ProductoService.CrudCallback() {
            @Override
            public void onSuccess() {
                productos.removeIf(p -> p.getId() == id);
                saveToFile();
                if (onSuccess != null) {
                    onSuccess.run();
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("Error al eliminar producto: " + error);
                if (onError != null) {
                    onError.run();
                }
            }
        });
    }

    /**
     * Obtener producto por ID
     */
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

    /**
     * Método para obtener estadísticas
     */
    public String getEstadisticas() {
        int total = productos.size();
        int disponibles = (int) productos.stream().filter(Producto::isDisponible).count();
        int categorias = getAllCategorias().size();
        
        return String.format("Total: %d productos | Disponibles: %d | Categorías: %d", 
                           total, disponibles, categorias);
    }
}