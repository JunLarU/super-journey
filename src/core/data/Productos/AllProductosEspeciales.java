// AllProductosEspeciales.java
package core.data.Productos;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton para gestionar todos los productos especiales
 */
public class AllProductosEspeciales {
    private static AllProductosEspeciales instance;
    private final List<ProductoEspecial> productosEspeciales = new ArrayList<>();
    private final String FILE_PATH = "data/productos_especiales.json";
    private int nextId = 1;

    private AllProductosEspeciales() {
        loadFromFile();
    }

    public static AllProductosEspeciales getInstance() {
        if (instance == null) {
            instance = new AllProductosEspeciales();
        }
        return instance;
    }

    public void addProductoEspecial(ProductoEspecial productoEspecial) {
        if (productoEspecial.getId() == 0) {
            productoEspecial.setId(nextId++);
        }
        productosEspeciales.add(productoEspecial);
        saveToFile();
    }

    public void updateProductoEspecial(ProductoEspecial nuevo) {
        for (int idx = 0; idx < productosEspeciales.size(); idx++) {
            if (productosEspeciales.get(idx).getId() == nuevo.getId()) {
                productosEspeciales.set(idx, nuevo);
                saveToFile();
                return;
            }
        }
    }

    public void removeProductoEspecial(int id) {
        productosEspeciales.removeIf(pe -> pe.getId() == id);
        saveToFile();
    }

    public ProductoEspecial getById(int id) {
        return productosEspeciales.stream()
                .filter(pe -> pe.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<ProductoEspecial> getByProducto(int idProducto) {
        return productosEspeciales.stream()
                .filter(pe -> pe.getIdProducto() == idProducto)
                .collect(Collectors.toList());
    }

    public List<ProductoEspecial> getEspecialesParaFecha(LocalDateTime fechaHora) {
        return productosEspeciales.stream()
                .filter(pe -> pe.estaActivoParaFechaHora(fechaHora))
                .collect(Collectors.toList());
    }

    public List<ProductoEspecial> getEspecialesVigentes() {
        return getEspecialesParaFecha(LocalDateTime.now());
    }

    public ProductoEspecial getEspecialParaProductoYFecha(int idProducto, LocalDateTime fechaHora) {
        return productosEspeciales.stream()
                .filter(pe -> pe.getIdProducto() == idProducto && pe.estaActivoParaFechaHora(fechaHora))
                .findFirst()
                .orElse(null);
    }

    public List<ProductoEspecial> getEspecialesActivos() {
        return productosEspeciales.stream()
                .filter(ProductoEspecial::isActivo)
                .collect(Collectors.toList());
    }

    public List<ProductoEspecial> getAll() {
        return new ArrayList<>(productosEspeciales);
    }

    // Método para verificar si un producto tiene precio especial en una fecha y hora
    public boolean tienePrecioEspecial(int idProducto, LocalDateTime fechaHora) {
        return getEspecialParaProductoYFecha(idProducto, fechaHora) != null;
    }

    // Método para obtener el precio especial de un producto en una fecha y hora
    public Double getPrecioEspecial(int idProducto, LocalDateTime fechaHora) {
        ProductoEspecial especial = getEspecialParaProductoYFecha(idProducto, fechaHora);
        return especial != null ? especial.getPrecioEspecial() : null;
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
            productosEspeciales.clear();

            int maxId = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ProductoEspecial pe = new ProductoEspecial(obj);
                productosEspeciales.add(pe);
                if (pe.getId() > maxId) {
                    maxId = pe.getId();
                }
            }
            nextId = maxId + 1;

        } catch (Exception e) {
            //System.err.println("Error cargando productos especiales desde archivo:");
            e.printStackTrace();
        }
    }

    public void saveToFile() {
        try {
            JSONArray array = new JSONArray();
            for (ProductoEspecial pe : productosEspeciales) {
                array.put(pe.toJson());
            }

            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(array.toString(4)); // JSON legible
            writer.close();

        } catch (Exception e) {
            //System.err.println("Error guardando productos especiales en archivo:");
            e.printStackTrace();
        }
    }

    // Método para obtener estadísticas
    public String getEstadisticas() {
        int total = productosEspeciales.size();
        int activos = (int) productosEspeciales.stream().filter(ProductoEspecial::isActivo).count();
        int vigentes = getEspecialesVigentes().size();
        
        return String.format("Total: %d especiales | Activos: %d | Vigentes: %d", 
                           total, activos, vigentes);
    }

    // Método para limpiar especiales expirados (más de 30 días)
    public void limpiarExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(30);
        int removidos = (int) productosEspeciales.stream()
                .filter(pe -> pe.getFechaFin().isBefore(limite))
                .count();
        
        productosEspeciales.removeIf(pe -> pe.getFechaFin().isBefore(limite));
        
        if (removidos > 0) {
            saveToFile();
            //System.out.println("Se removieron " + removidos + " productos especiales expirados");
        }
    }
}