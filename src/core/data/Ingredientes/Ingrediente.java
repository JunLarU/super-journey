package core.data.Ingredientes;

import org.json.JSONObject;

public class Ingrediente {

    private int id;
    private String nombre;
    private String categoria;
    private String descripcion;
    private double calorias;
    private boolean alergenico;

    // ✅ Constructor vacío (OBLIGATORIO)
    public Ingrediente() {}

    // Constructor completo
    public Ingrediente(int id, String nombre, String categoria,
                       String descripcion, double calorias, boolean alergenico) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.calorias = calorias;
        this.alergenico = alergenico;
    }

    // ✅ Constructor desde API (PHP)
    public Ingrediente(JSONObject json) {
        this.id = json.getInt("ID");
        this.nombre = json.getString("Nombre");
        this.categoria = json.optString("Categoria", "");
        this.descripcion = json.optString("Descripcion", "");
        this.calorias = json.optDouble("Calorias", 0);
        this.alergenico = json.optInt("Alergeno", 0) == 1;
    }

    // =====================
    // Getters / Setters
    // =====================

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public double getCalorias() { return calorias; }
    public boolean isAlergenico() { return alergenico; }

    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCalorias(double calorias) { this.calorias = calorias; }
    public void setAlergenico(boolean alergenico) { this.alergenico = alergenico; }

    // =====================
    // JSON para API
    // =====================
    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("nombre", nombre);
        o.put("descripcion", descripcion);
        o.put("calorias", calorias);
        o.put("alergeno", alergenico ? 1 : 0);
        // idCategoria se envía desde el formulario
        return o;
    }

    @Override
    public String toString() {
        return nombre + (alergenico ? " ⚠️" : "");
    }
}
