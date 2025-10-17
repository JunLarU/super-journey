package core.data.Productos;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un ingrediente dentro de un producto
 * Incluye sus sustitutos disponibles
 */
public class ProductoIngrediente {
    private int idIngrediente;
    private String nombreIngrediente; // Para mostrar en UI
    private double cantidad; // en gramos/ml
    private boolean eliminable; // Si el cliente puede eliminarlo
    private boolean sustituible; // Si el cliente puede sustituirlo
    private int orden; // Para ordenar en la lista
    private List<Sustituto> sustitutos; // Lista de sustitutos disponibles

    public ProductoIngrediente(int idIngrediente, String nombreIngrediente, double cantidad, 
                              boolean eliminable, boolean sustituible, int orden) {
        this.idIngrediente = idIngrediente;
        this.nombreIngrediente = nombreIngrediente;
        this.cantidad = cantidad;
        this.eliminable = eliminable;
        this.sustituible = sustituible;
        this.orden = orden;
        this.sustitutos = new ArrayList<>();
    }

    // Constructor desde JSON
    public ProductoIngrediente(JSONObject json) {
        this.idIngrediente = json.getInt("IDIngrediente");
        this.nombreIngrediente = json.getString("NombreIngrediente");
        this.cantidad = json.optDouble("Cantidad", 0.0);
        this.eliminable = json.optBoolean("Eliminable", false);
        this.sustituible = json.optBoolean("Sustituible", false);
        this.orden = json.optInt("Orden", 0);
        
        // Cargar sustitutos
        this.sustitutos = new ArrayList<>();
        if (json.has("Sustitutos")) {
            JSONArray sustArray = json.getJSONArray("Sustitutos");
            for (int i = 0; i < sustArray.length(); i++) {
                sustitutos.add(new Sustituto(sustArray.getJSONObject(i)));
            }
        }
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("IDIngrediente", idIngrediente);
        obj.put("NombreIngrediente", nombreIngrediente);
        obj.put("Cantidad", cantidad);
        obj.put("Eliminable", eliminable);
        obj.put("Sustituible", sustituible);
        obj.put("Orden", orden);
        
        // Guardar sustitutos
        JSONArray sustArray = new JSONArray();
        for (Sustituto s : sustitutos) {
            sustArray.put(s.toJson());
        }
        obj.put("Sustitutos", sustArray);
        
        return obj;
    }

    // Métodos para manejar sustitutos
    public void agregarSustituto(Sustituto sustituto) {
        sustitutos.add(sustituto);
    }

    public void eliminarSustituto(int idIngrediente) {
        sustitutos.removeIf(s -> s.getIdIngrediente() == idIngrediente);
    }

    // Getters y Setters
    public int getIdIngrediente() { return idIngrediente; }
    public String getNombreIngrediente() { return nombreIngrediente; }
    public double getCantidad() { return cantidad; }
    public boolean isEliminable() { return eliminable; }
    public boolean isSustituible() { return sustituible; }
    public int getOrden() { return orden; }
    public List<Sustituto> getSustitutos() { return new ArrayList<>(sustitutos); }

    public void setIdIngrediente(int idIngrediente) { this.idIngrediente = idIngrediente; }
    public void setNombreIngrediente(String nombreIngrediente) { this.nombreIngrediente = nombreIngrediente; }
    public void setCantidad(double cantidad) { this.cantidad = cantidad; }
    public void setEliminable(boolean eliminable) { this.eliminable = eliminable; }
    public void setSustituible(boolean sustituible) { this.sustituible = sustituible; }
    public void setOrden(int orden) { this.orden = orden; }
    public void setSustitutos(List<Sustituto> sustitutos) { this.sustitutos = new ArrayList<>(sustitutos); }

    @Override
    public String toString() {
        String opciones = "";
        if (eliminable) opciones += " [Eliminable]";
        if (sustituible) opciones += " [Sustituible]";
        return nombreIngrediente + " (" + cantidad + "g)" + opciones;
    }
}