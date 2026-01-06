package core.data.Menus;

import org.json.JSONObject;

/**
 * Representa un producto dentro de una sección del menú
 * Corresponde a la tabla SeccionesMenuProductos en la BD
 */
public class SeccionProducto {
    private int id;
    private int idSeccion;
    private int idProducto;
    private String nombre;
    private String descripcion;
    private double precio;
    private String categoria;
    private int orden;
    private boolean destacado;

    // Constructor básico
    public SeccionProducto() {
        this.id = 0;
        this.idSeccion = 0;
        this.idProducto = 0;
        this.nombre = "";
        this.descripcion = "";
        this.precio = 0.0;
        this.categoria = "";
        this.orden = 0;
        this.destacado = false;
    }

    // Constructor con parámetros mínimos
    public SeccionProducto(int id, int idSeccion, int idProducto, String nombre, 
                          int orden, boolean destacado) {
        this.id = id;
        this.idSeccion = idSeccion;
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.descripcion = "";
        this.precio = 0.0;
        this.categoria = "";
        this.orden = orden;
        this.destacado = destacado;
    }

    // Constructor completo
    public SeccionProducto(int id, int idSeccion, int idProducto, String nombre,
                          String descripcion, double precio, String categoria,
                          int orden, boolean destacado) {
        this.id = id;
        this.idSeccion = idSeccion;
        this.idProducto = idProducto;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.categoria = categoria;
        this.orden = orden;
        this.destacado = destacado;
    }

    // Constructor desde JSON (compatible con backend)
    public SeccionProducto(JSONObject json) {
        this.id = json.optInt("ID", 0);
        this.idSeccion = json.optInt("IDSeccion", 0);
        this.idProducto = json.optInt("IDProducto", 0);
        this.nombre = json.optString("Nombre", "");
        this.descripcion = json.optString("Descripcion", "");
        this.precio = json.optDouble("PrecioBase", 0.0);
        this.categoria = json.optString("Categoria", "");
        this.orden = json.optInt("Orden", 0);
        this.destacado = json.optBoolean("Destacado", false);
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("IDSeccion", idSeccion);
        obj.put("IDProducto", idProducto);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("PrecioBase", precio);
        obj.put("Categoria", categoria);
        obj.put("Orden", orden);
        obj.put("Destacado", destacado);
        return obj;
    }

    // Convertir para formulario
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("idSeccion", idSeccion);
        obj.put("idProducto", idProducto);
        obj.put("nombre", nombre);
        obj.put("precio", precio);
        obj.put("categoria", categoria);
        obj.put("orden", orden);
        obj.put("destacado", destacado);
        return obj;
    }

    // Getters y Setters
    public int getId() { return id; }
    public int getIdSeccion() { return idSeccion; }
    public int getIdProducto() { return idProducto; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getPrecio() { return precio; }
    public String getCategoria() { return categoria; }
    public int getOrden() { return orden; }
    public boolean isDestacado() { return destacado; }

    public void setId(int id) { this.id = id; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setOrden(int orden) { this.orden = orden; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }

    // Alias para compatibilidad
    public String getNombreProducto() { return nombre; }
    public void setNombreProducto(String nombreProducto) { this.nombre = nombreProducto; }

    // Método estático
    public static SeccionProducto fromJSON(JSONObject json) {
        return new SeccionProducto(json);
    }

    @Override
    public String toString() {
        return nombre + " ($" + precio + ")" + (destacado ? " ⭐" : "") + 
               " [" + categoria + "]";
    }
}