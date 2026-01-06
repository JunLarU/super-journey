package core.data.Menus;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una sección del menú que agrupa productos
 * Corresponde a la tabla SeccionesMenu en la BD
 */
public class SeccionMenu {
    private int id;
    private String nombre;
    private String descripcion;
    private String urlFoto;
    private String color;
    private boolean activo;
    private String fechaCreacion;
    private List<SeccionProducto> productos;

    // Constructor básico
    public SeccionMenu() {
        this.id = 0;
        this.nombre = "";
        this.descripcion = "";
        this.urlFoto = "";
        this.color = "#3498db";
        this.activo = true;
        this.fechaCreacion = LocalDate.now().toString();
        this.productos = new ArrayList<>();
    }

    // Constructor con parámetros básicos
    public SeccionMenu(int id, String nombre, String descripcion, String urlFoto, 
                      String color, boolean activo, String fechaCreacion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.urlFoto = urlFoto;
        this.color = color;
        this.activo = activo;
        this.fechaCreacion = fechaCreacion;
        this.productos = new ArrayList<>();
    }

    // Constructor desde JSON (compatible con backend)
    public SeccionMenu(JSONObject json) {
        this.id = json.optInt("ID", 0);
        this.nombre = json.optString("Nombre", "");
        this.descripcion = json.optString("Descripcion", "");
        this.urlFoto = json.optString("URLFoto", "");
        this.color = json.optString("Color", "#3498db");
        this.activo = json.optBoolean("Activo", true);
        this.fechaCreacion = json.optString("FechaCreacion", LocalDate.now().toString());
        
        // Cargar productos
        this.productos = new ArrayList<>();
        if (json.has("Productos")) {
            JSONArray productosArray = json.getJSONArray("Productos");
            for (int i = 0; i < productosArray.length(); i++) {
                productos.add(new SeccionProducto(productosArray.getJSONObject(i)));
            }
        }
        
        // Compatibilidad con campo "CantidadProductos"
        if (json.has("CantidadProductos")) {
            int cantidad = json.getInt("CantidadProductos");
            // Podemos inicializar productos vacíos o con marcadores
        }
    }

    // Convertir a JSON para enviar al servidor
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        
        if (id > 0) {
            obj.put("ID", id);
        }
        
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        
        if (urlFoto != null && !urlFoto.isEmpty()) {
            obj.put("URLFoto", urlFoto);
        }
        
        obj.put("Color", color);
        obj.put("Activo", activo);
        obj.put("FechaCreacion", fechaCreacion);
        
        // Guardar productos
        if (productos != null && !productos.isEmpty()) {
            JSONArray productosArray = new JSONArray();
            for (SeccionProducto producto : productos) {
                productosArray.put(producto.toJson());
            }
            obj.put("Productos", productosArray);
        }
        
        return obj;
    }

    // Convertir para formulario
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("nombre", nombre);
        obj.put("descripcion", descripcion);
        obj.put("urlFoto", urlFoto);
        obj.put("color", color);
        obj.put("activo", activo);
        
        // Incluir IDs de productos para formulario
        if (productos != null && !productos.isEmpty()) {
            JSONArray productosArray = new JSONArray();
            for (SeccionProducto producto : productos) {
                JSONObject pObj = new JSONObject();
                pObj.put("idProducto", producto.getIdProducto());
                pObj.put("destacado", producto.isDestacado());
                productosArray.put(pObj);
            }
            obj.put("productos", productosArray);
        }
        
        return obj;
    }

    // Métodos para manejar productos
    public void agregarProducto(SeccionProducto producto) {
        if (productos == null) {
            productos = new ArrayList<>();
        }
        productos.add(producto);
    }

    public void eliminarProducto(int idProducto) {
        if (productos != null) {
            productos.removeIf(p -> p.getIdProducto() == idProducto);
        }
    }

    public SeccionProducto getProducto(int idProducto) {
        if (productos == null) return null;
        
        return productos.stream()
                .filter(p -> p.getIdProducto() == idProducto)
                .findFirst()
                .orElse(null);
    }

    // Obtener cantidad de productos
    public int getCantidadProductos() {
        return productos != null ? productos.size() : 0;
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getUrlFoto() { return urlFoto; }
    public String getColor() { return color; }
    public boolean isActivo() { return activo; }
    public String getFechaCreacion() { return fechaCreacion; }
    public List<SeccionProducto> getProductos() { 
        if (productos == null) return new ArrayList<>();
        return new ArrayList<>(productos); 
    }

    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }
    public void setColor(String color) { this.color = color; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setProductos(List<SeccionProducto> productos) { 
        this.productos = new ArrayList<>(productos); 
    }

    // Método estático
    public static SeccionMenu fromJSON(JSONObject json) {
        return new SeccionMenu(json);
    }

    @Override
    public String toString() {
        return nombre + (activo ? " (Activo)" : " (Inactivo)") + 
               " - " + getCantidadProductos() + " producto(s)";
    }
}