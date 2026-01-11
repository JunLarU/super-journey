package core.data.Productos;

import core.services.ProductosEspecialesService;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Singleton para gestionar todos los productos especiales
 */
public class AllProductosEspeciales {
    private static AllProductosEspeciales instance;
    private final List<ProductoEspecial> productosEspeciales = new ArrayList<>();
    private final ProductosEspecialesService service;

    // Callback interface
    public interface ProductosEspecialesCallback {
        void onSuccess(List<ProductoEspecial> productos);

        void onError(String error);
    }

    private AllProductosEspeciales() {
        this.service = ProductosEspecialesService.getInstance();
    }

    public static AllProductosEspeciales getInstance() {
        if (instance == null) {
            instance = new AllProductosEspeciales();
        }
        return instance;
    }

    /**
     * Obtener todos los productos especiales (sincrónico - para compatibilidad)
     */
    public List<ProductoEspecial> getAll() {
        return new ArrayList<>(productosEspeciales);
    }

    /**
     * Obtener todos los productos especiales (asincrónico con callback)
     */
    public void getAllAsync(ProductosEspecialesCallback callback) {
        Task<List<ProductoEspecial>> task = service.getAll();

        task.setOnSucceeded(event -> {
            try {
                List<ProductoEspecial> resultado = task.getValue();
                productosEspeciales.clear();
                productosEspeciales.addAll(resultado);
                callback.onSuccess(new ArrayList<>(productosEspeciales));
            } catch (Exception e) {
                callback.onError("Error procesando datos: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            callback.onError("Error al cargar: " + (ex != null ? ex.getMessage() : "Error desconocido"));
        });

        new Thread(task).start();
    }

    /**
     * Métodos de compatibilidad (mantienen la misma interfaz)
     */
    public void addProductoEspecial(ProductoEspecial productoEspecial) {
        productosEspeciales.add(productoEspecial);
    }

    public void updateProductoEspecial(ProductoEspecial nuevo) {
        for (int i = 0; i < productosEspeciales.size(); i++) {
            if (productosEspeciales.get(i).getId() == nuevo.getId()) {
                productosEspeciales.set(i, nuevo);
                return;
            }
        }
    }

    public void removeProductoEspecial(int id) {
        productosEspeciales.removeIf(pe -> pe.getId() == id);
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

    // Método para verificar si un producto tiene precio especial en una fecha y
    // hora
    public boolean tienePrecioEspecial(int idProducto, LocalDateTime fechaHora) {
        return getEspecialParaProductoYFecha(idProducto, fechaHora) != null;
    }

    // Método para obtener el precio especial de un producto en una fecha y hora
    public Double getPrecioEspecial(int idProducto, LocalDateTime fechaHora) {
        ProductoEspecial especial = getEspecialParaProductoYFecha(idProducto, fechaHora);
        return especial != null ? especial.getPrecioEspecial() : null;
    }

    // Método para obtener estadísticas
    public String getEstadisticas() {
        int total = productosEspeciales.size();
        int activos = (int) productosEspeciales.stream().filter(ProductoEspecial::isActivo).count();
        int vigentes = getEspecialesVigentes().size();

        return String.format("Total: %d especiales | Activos: %d | Vigentes: %d",
                total, activos, vigentes);
    }

    /**
     * Métodos para operaciones con servidor
     */
    public Task<Boolean> guardarEnServidor(ProductoEspecial producto) {
        if (producto.getId() == 0) {
            return service.crear(producto);
        } else {
            return service.actualizar(producto);
        }
    }

    public void eliminarDelServidor(int id, Runnable onSuccess, Consumer<String> onError) {
        Task<Boolean> task = service.eliminar(id);

        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                removeProductoEspecial(id);
                Platform.runLater(onSuccess);
            } else {
                Platform.runLater(() -> onError.accept("No se pudo eliminar del servidor"));
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> onError.accept("Error: " +
                    (ex != null ? ex.getMessage() : "Error desconocido")));
        });

        new Thread(task).start();
    }

    /**
     * Método para cargar desde servidor y actualizar cache
     */
    public void cargarDesdeServidor() {
        getAllAsync(new ProductosEspecialesCallback() {
            @Override
            public void onSuccess(List<ProductoEspecial> productos) {
                System.out.println("Cargados " + productos.size() + " productos especiales desde servidor");
            }

            @Override
            public void onError(String error) {
                System.err.println("Error al cargar desde servidor: " + error);
            }
        });
    }
}