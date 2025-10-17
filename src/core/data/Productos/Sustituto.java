package core.data.Productos;

import org.json.JSONObject;

/**
 * Representa un sustituto para un ingrediente específico de un producto
 */
public class Sustituto {
    private int idIngrediente; // ID del ingrediente sustituto
    private String nombreIngrediente; // Nombre para mostrar
    private double costoExtra; // Costo adicional por la sustitución
    private boolean disponible;

    public Sustituto(int idIngrediente, String nombreIngrediente, double costoExtra, boolean disponible) {
        this.idIngrediente = idIngrediente;
        this.nombreIngrediente = nombreIngrediente;
        this.costoExtra = costoExtra;
        this.disponible = disponible;
    }

    // Constructor desde JSON
    public Sustituto(JSONObject json) {
        this.idIngrediente = json.getInt("IDIngrediente");
        this.nombreIngrediente = json.getString("NombreIngrediente");
        this.costoExtra = json.optDouble("CostoExtra", 0.0);
        this.disponible = json.optBoolean("Disponible", true);
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("IDIngrediente", idIngrediente);
        obj.put("NombreIngrediente", nombreIngrediente);
        obj.put("CostoExtra", costoExtra);
        obj.put("Disponible", disponible);
        return obj;
    }

    // Getters y Setters
    public int getIdIngrediente() { return idIngrediente; }
    public String getNombreIngrediente() { return nombreIngrediente; }
    public double getCostoExtra() { return costoExtra; }
    public boolean isDisponible() { return disponible; }

    public void setIdIngrediente(int idIngrediente) { this.idIngrediente = idIngrediente; }
    public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }
    public void setCostoExtra(double costoExtra) { this.costoExtra = costoExtra; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    @Override
    public String toString() {
        String extra = costoExtra > 0 ? " (+$" + costoExtra + ")" : "";
        return nombreIngrediente + extra;
    }
}