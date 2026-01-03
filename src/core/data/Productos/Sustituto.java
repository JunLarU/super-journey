package core.data.Productos;

import org.json.JSONObject;

public class Sustituto {
    private int id;
    private int idIngredienteSustituto;
    private String nombreIngrediente;
    private double costoExtra;
    private boolean disponible;

    public Sustituto() {}

    public Sustituto(int idIngredienteSustituto, String nombreIngrediente, double costoExtra, boolean disponible) {
        this.idIngredienteSustituto = idIngredienteSustituto;
        this.nombreIngrediente = nombreIngrediente;
        this.costoExtra = costoExtra;
        this.disponible = disponible;
    }

    public Sustituto(JSONObject json) {
        // Buscar campos con diferentes nombres
        if (json.has("ID")) {
            this.id = json.optInt("ID", 0);
        } else if (json.has("id")) {
            this.id = json.optInt("id", 0);
        }
        
        if (json.has("IDIngredienteSustituto")) {
            this.idIngredienteSustituto = json.optInt("IDIngredienteSustituto", 0);
        } else if (json.has("idIngredienteSustituto")) {
            this.idIngredienteSustituto = json.optInt("idIngredienteSustituto", 0);
        } else if (json.has("idIngrediente")) {
            this.idIngredienteSustituto = json.optInt("idIngrediente", 0);
        }
        
        if (json.has("Nombre")) {
            this.nombreIngrediente = json.optString("Nombre", "");
        } else if (json.has("nombre")) {
            this.nombreIngrediente = json.optString("nombre", "");
        }
        
        if (json.has("CostoExtra")) {
            this.costoExtra = json.optDouble("CostoExtra", 0.0);
        } else if (json.has("costoExtra")) {
            this.costoExtra = json.optDouble("costoExtra", 0.0);
        }
        
        if (json.has("Disponible")) {
            this.disponible = json.optInt("Disponible", 1) == 1;
        } else if (json.has("disponible")) {
            if (json.get("disponible") instanceof Boolean) {
                this.disponible = json.getBoolean("disponible");
            } else {
                this.disponible = json.optInt("disponible", 1) == 1;
            }
        }
    }

    // Para enviar al servidor
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("idIngrediente", idIngredienteSustituto);
        obj.put("nombre", nombreIngrediente);
        obj.put("costoExtra", costoExtra);
        obj.put("disponible", disponible ? 1 : 0);
        return obj;
    }

    // Para formularios
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("IDIngredienteSustituto", idIngredienteSustituto);
        obj.put("Nombre", nombreIngrediente);
        obj.put("CostoExtra", costoExtra);
        obj.put("Disponible", disponible ? 1 : 0);
        return obj;
    }

    // Getters y Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIdIngredienteSustituto() { return idIngredienteSustituto; }
    public void setIdIngredienteSustituto(int idIngredienteSustituto) { 
        this.idIngredienteSustituto = idIngredienteSustituto; 
    }

    public String getNombreIngrediente() { return nombreIngrediente; }
    public void setNombreIngrediente(String nombreIngrediente) { 
        this.nombreIngrediente = nombreIngrediente; 
    }

    public double getCostoExtra() { return costoExtra; }
    public void setCostoExtra(double costoExtra) { this.costoExtra = costoExtra; }

    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
}