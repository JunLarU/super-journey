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
    private String color; // Color hex para identificación visual
    private boolean activo;
    private String fechaCreacion; // 🔹 NUEVO - Para auditoría
    private List<SeccionProducto> productos;

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

    // Constructor desde JSON
    public SeccionMenu(JSONObject json) {
        this.id = json.getInt("ID");
        this.nombre = json.getString("Nombre");
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
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("URLFoto", urlFoto);
        obj.put("Color", color);
        obj.put("Activo", activo);
        obj.put("FechaCreacion", fechaCreacion);
        
        // Guardar productos
        JSONArray productosArray = new JSONArray();
        for (SeccionProducto producto : productos) {
            productosArray.put(producto.toJson());
        }
        obj.put("Productos", productosArray);
        
        return obj;
    }

    // Métodos para manejar productos
    public void agregarProducto(SeccionProducto producto) {
        productos.add(producto);
    }

    public void eliminarProducto(int idProducto) {
        productos.removeIf(p -> p.getIdProducto() == idProducto);
    }

    public SeccionProducto getProducto(int idProducto) {
        return productos.stream()
                .filter(p -> p.getIdProducto() == idProducto)
                .findFirst()
                .orElse(null);
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getUrlFoto() { return urlFoto; }
    public String getColor() { return color; }
    public boolean isActivo() { return activo; }
    public String getFechaCreacion() { return fechaCreacion; }
    public List<SeccionProducto> getProductos() { return new ArrayList<>(productos); }

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

    @Override
    public String toString() {
        return nombre + (activo ? " (Activo)" : " (Inactivo)");
    }
}