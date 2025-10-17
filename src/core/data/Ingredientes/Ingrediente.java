package core.data.Ingredientes;

import org.json.JSONObject;

public class Ingrediente {
    private int id; // ID autoincremental simulado
    private String nombre;
    private String categoria; // puede ser null
    private String descripcion;
    private double calorias;
    private boolean alergenico;

    // Constructor principal
    public Ingrediente(int id, String nombre, String categoria, String descripcion, double calorias, boolean alergenico) {
        this.id = id;
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.calorias = calorias;
        this.alergenico = alergenico;
    }

    // Constructor desde JSON
    public Ingrediente(JSONObject json) {
        this.id = json.getInt("ID");
        this.nombre = json.getString("Nombre");
        this.categoria = json.getString("categoria");
        this.descripcion = json.optString("Descripcion", "");
        this.calorias = json.optDouble("Calorias", 0.0);
        this.alergenico = json.optBoolean("Alergeno", false);
    }

    // Convertir a JSONObject
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("Nombre", nombre);
        obj.put("categoria", categoria == null ? JSONObject.NULL : categoria);
        obj.put("Descripcion", descripcion);
        obj.put("Calorias", calorias);
        obj.put("Alergeno", alergenico);
        return obj;
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getcategoria() { return categoria; }
    public String getDescripcion() { return descripcion; }
    public double getCalorias() { return calorias; }
    public boolean isAlergenico() { return alergenico; }

    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setcategoria(String categoria) { this.categoria = categoria; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCalorias(double calorias) { this.calorias = calorias; }
    public void setAlergenico(boolean alergenico) { this.alergenico = alergenico; }

    @Override
    public String toString() {
        return nombre + " (" + (alergenico ? "⚠️ Alergeno" : "Seguro") + ")";
    }
}
