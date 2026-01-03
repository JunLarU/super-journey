package core.data.Productos;

import org.json.JSONObject;

public class CategoriaProducto {
    private int id;
    private String nombre;
    private String descripcion;
    private int orden;
    private boolean activo;

    public CategoriaProducto() {}
    
    public CategoriaProducto(int id, String nombre, String descripcion) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = 1;
        this.activo = true;
    }
    
    public CategoriaProducto(JSONObject json) {
        this.id = json.optInt("ID", 0);
        this.nombre = json.optString("Nombre", "");
        this.descripcion = json.optString("Descripcion", "");
        this.orden = json.optInt("Orden", 1);
        this.activo = json.optInt("Activo", 1) == 1;
    }
    
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ID", id);
            obj.put("Nombre", nombre);
            obj.put("Descripcion", descripcion);
            obj.put("Orden", orden);
            obj.put("Activo", activo ? 1 : 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }
    
    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    
    @Override
    public String toString() {
        return nombre; // lo que se muestra en el ComboBox
    }
}