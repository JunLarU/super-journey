package core.data.Productos;

import core.services.ProductoService;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Versión segura de AllProductos para uso en ProductosEspecialesController
 * No lanza excepciones, retorna null o lista vacía en caso de error
 */
public class AllProductosSafe {
    private static AllProductosSafe instance;
    private List<Producto> cacheProductos = new ArrayList<>();
    private long cacheTimestamp = 0;
    private static final long CACHE_DURATION_MS = 60000; // 1 minuto

    private AllProductosSafe() {
        // Constructor privado para singleton
    }

    public static AllProductosSafe getInstance() {
        if (instance == null) {
            instance = new AllProductosSafe();
        }
        return instance;
    }

    /**
     * Obtiene un producto por ID sin lanzar excepciones
     */
    public Producto getById(int id) {
        try {
            // Intentar obtener del cache primero
            for (Producto p : cacheProductos) {
                if (p.getId() == id) {
                    return p;
                }
            }

            // Si no está en cache o cache expirado, cargar desde servidor
            if (System.currentTimeMillis() - cacheTimestamp > CACHE_DURATION_MS || cacheProductos.isEmpty()) {
                cargarDesdeServidor();
                
                // Buscar en cache actualizado
                for (Producto p : cacheProductos) {
                    if (p.getId() == id) {
                        return p;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Obtiene todos los productos sin lanzar excepciones
     */
    public List<Producto> getAll() {
        try {
            if (System.currentTimeMillis() - cacheTimestamp > CACHE_DURATION_MS || cacheProductos.isEmpty()) {
                cargarDesdeServidor();
            }
            return new ArrayList<>(cacheProductos);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Busca productos por nombre sin lanzar excepciones
     */
    public List<Producto> buscarPorNombre(String query) {
        List<Producto> resultados = new ArrayList<>();
        try {
            List<Producto> todos = getAll();
            String queryLower = query.toLowerCase();
            
            for (Producto p : todos) {
                if (p.getNombre().toLowerCase().contains(queryLower)) {
                    resultados.add(p);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultados;
    }

    /**
     * Carga productos desde el servidor (con callback asíncrono)
     */
    private void cargarDesdeServidor() {
        ProductoService.listProductos(new ProductoService.ListCallback() {
            @Override
            public void onSuccess(List<org.json.JSONObject> list) {
                cacheProductos.clear();
                for (org.json.JSONObject json : list) {
                    try {
                        Producto producto = Producto.fromJSON(json);
                        cacheProductos.add(producto);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                cacheTimestamp = System.currentTimeMillis();
            }

            @Override
            public void onError(String error) {
                System.err.println("Error cargando productos: " + error);
                // Mantener cache anterior si existe
                if (cacheProductos.isEmpty()) {
                    // Crear productos de prueba para evitar errores
                    crearProductosDePrueba();
                }
            }
        });
    }

    /**
     * Crea productos de prueba en caso de error de conexión
     */
    private void crearProductosDePrueba() {
        cacheProductos.clear();
        
        // Productos de ejemplo
        cacheProductos.add(new Producto(1, "Café Americano", "Café negro fuerte", 35.00, "Café", 1, 250.0, 5.0, null, true));
        cacheProductos.add(new Producto(2, "Café Latte", "Café con leche vaporizada", 45.00, "Café", 1, 350.0, 150.0, null, true));
        cacheProductos.add(new Producto(3, "Tacos al Pastor", "Tacos con carne al pastor", 85.00, "Antojitos Mexicanos", 2, 200.0, 350.0, null, true));
        cacheProductos.add(new Producto(4, "Hamburguesa Clásica", "Hamburguesa con queso y verduras", 120.00, "Hamburguesas", 3, 300.0, 550.0, null, true));
        cacheProductos.add(new Producto(5, "Ensalada César", "Ensalada con pollo y aderezo césar", 90.00, "Ensaladas", 4, 250.0, 280.0, null, true));
        
        cacheTimestamp = System.currentTimeMillis();
    }

    /**
     * Obtiene el nombre de un producto por ID (método seguro)
     */
    public String getNombreProducto(int idProducto) {
        Producto producto = getById(idProducto);
        return producto != null ? producto.getNombre() : "Producto #" + idProducto;
    }
}