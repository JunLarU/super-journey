package core.data.Menus;

import org.json.JSONObject;

/**
 * Representa un producto dentro de una sección del menú
 */
public class SeccionProducto {
    private int id;
    private int idSeccion;
    private int idProducto;
    private String nombreProducto; // Para mostrar en UI
    private int orden;
    private boolean destacado;

    public SeccionProducto(int id, int idSeccion, int idProducto, String nombreProducto, 
                          int orden, boolean destacado) {
        this.id = id;
        this.idSeccion = idSeccion;
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.orden = orden;
        this.destacado = destacado;
    }

    // Constructor desde JSON
    public SeccionProducto(JSONObject json) {
        this.id = json.getInt("ID");
        this.idSeccion = json.getInt("IDSeccion");
        this.idProducto = json.getInt("IDProducto");
        this.nombreProducto = json.getString("NombreProducto");
        this.orden = json.optInt("Orden", 0);
        this.destacado = json.optBoolean("Destacado", false);
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("IDSeccion", idSeccion);
        obj.put("IDProducto", idProducto);
        obj.put("NombreProducto", nombreProducto);
        obj.put("Orden", orden);
        obj.put("Destacado", destacado);
        return obj;
    }

    // Getters y Setters
    public int getId() { return id; }
    public int getIdSeccion() { return idSeccion; }
    public int getIdProducto() { return idProducto; }
    public String getNombreProducto() { return nombreProducto; }
    public int getOrden() { return orden; }
    public boolean isDestacado() { return destacado; }

    public void setId(int id) { this.id = id; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public void setNombreProducto(String nombreProducto) { this.nombreProducto = nombreProducto; }
    public void setOrden(int orden) { this.orden = orden; }
    public void setDestacado(boolean destacado) { this.destacado = destacado; }

    @Override
    public String toString() {
        return nombreProducto + (destacado ? " ⭐" : "") + " (Orden: " + orden + ")";
    }
}