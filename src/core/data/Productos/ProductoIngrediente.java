package core.data.Productos;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ProductoIngrediente {

    private int id;
    private int idIngrediente;
    private String nombre;
    private double cantidad;
    private boolean eliminable;
    private boolean sustituible;
    private int orden;
    private List<Sustituto> sustitutos;

    public ProductoIngrediente() {
        this.sustitutos = new ArrayList<>();
    }

    // Constructor actualizado con todos los parámetros
    public ProductoIngrediente(int idIngrediente, String nombre, double cantidad,
            boolean eliminable, boolean sustituible, int orden) {
        this.idIngrediente = idIngrediente;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.eliminable = eliminable;
        this.sustituible = sustituible;
        this.orden = orden;
        this.sustitutos = new ArrayList<>();
    }

    public ProductoIngrediente(JSONObject json) {
        this.sustitutos = new ArrayList<>();
        
        // Buscar ID con diferentes nombres
        if (json.has("ID")) {
            this.id = json.optInt("ID", 0);
        } else if (json.has("id")) {
            this.id = json.optInt("id", 0);
        }
        
        // Buscar IDIngrediente con diferentes nombres
        if (json.has("IDIngrediente")) {
            this.idIngrediente = json.optInt("IDIngrediente", 0);
        } else if (json.has("idIngrediente")) {
            this.idIngrediente = json.optInt("idIngrediente", 0);
        }
        
        // Buscar Nombre con diferentes nombres
        if (json.has("Nombre")) {
            this.nombre = json.optString("Nombre", "");
        } else if (json.has("nombre")) {
            this.nombre = json.optString("nombre", "");
        }
        
        // Buscar Cantidad con diferentes nombres
        if (json.has("Cantidad")) {
            this.cantidad = json.optDouble("Cantidad", 0.0);
        } else if (json.has("cantidad")) {
            this.cantidad = json.optDouble("cantidad", 0.0);
        }
        
        // Buscar Eliminable con diferentes nombres
        if (json.has("Eliminable")) {
            this.eliminable = json.optInt("Eliminable", 0) == 1;
        } else if (json.has("eliminable")) {
            if (json.get("eliminable") instanceof Boolean) {
                this.eliminable = json.getBoolean("eliminable");
            } else {
                this.eliminable = json.optInt("eliminable", 0) == 1;
            }
        }
        
        // Buscar Sustituible con diferentes nombres
        if (json.has("Sustituible")) {
            this.sustituible = json.optInt("Sustituible", 0) == 1;
        } else if (json.has("sustituible")) {
            if (json.get("sustituible") instanceof Boolean) {
                this.sustituible = json.getBoolean("sustituible");
            } else {
                this.sustituible = json.optInt("sustituible", 0) == 1;
            }
        }
        
        // Buscar Orden con diferentes nombres
        if (json.has("Orden")) {
            this.orden = json.optInt("Orden", 1);
        } else if (json.has("orden")) {
            this.orden = json.optInt("orden", 1);
        }

        // Cargar sustitutos si existen
        if (json.has("Sustitutos") && json.get("Sustitutos") instanceof JSONArray) {
            JSONArray sustitutosArray = json.getJSONArray("Sustitutos");
            for (int i = 0; i < sustitutosArray.length(); i++) {
                JSONObject sustJson = sustitutosArray.getJSONObject(i);
                Sustituto sust = new Sustituto(sustJson);
                this.sustitutos.add(sust);
            }
        }
    }

    // Para enviar al servidor
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        
        obj.put("idIngrediente", idIngrediente);
        obj.put("nombre", nombre);
        obj.put("cantidad", cantidad);
        obj.put("eliminable", eliminable ? 1 : 0);
        obj.put("sustituible", sustituible ? 1 : 0);
        obj.put("orden", orden);

        // Agregar sustitutos si existen
        if (sustitutos != null && !sustitutos.isEmpty()) {
            JSONArray sustArr = new JSONArray();
            for (Sustituto sust : sustitutos) {
                sustArr.put(sust.toJson());
            }
            obj.put("sustitutos", sustArr);
        }

        return obj;
    }

    // Para formularios
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();
        
        obj.put("ID", id);
        obj.put("IDIngrediente", idIngrediente);
        obj.put("Nombre", nombre);
        obj.put("Cantidad", cantidad);
        obj.put("Eliminable", eliminable ? 1 : 0);
        obj.put("Sustituible", sustituible ? 1 : 0);
        obj.put("Orden", orden);

        // Agregar sustitutos si existen
        if (sustitutos != null && !sustitutos.isEmpty()) {
            JSONArray sustArr = new JSONArray();
            for (Sustituto sust : sustitutos) {
                sustArr.put(sust.toJsonForForm());
            }
            obj.put("Sustitutos", sustArr);
        }

        return obj;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdIngrediente() {
        return idIngrediente;
    }

    public void setIdIngrediente(int idIngrediente) {
        this.idIngrediente = idIngrediente;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreIngrediente() {
        return nombre;
    }

    public double getCantidad() {
        return cantidad;
    }

    public void setCantidad(double cantidad) {
        this.cantidad = cantidad;
    }

    public boolean isEliminable() {
        return eliminable;
    }

    public void setEliminable(boolean eliminable) {
        this.eliminable = eliminable;
    }

    public boolean isSustituible() {
        return sustituible;
    }

    public void setSustituible(boolean sustituible) {
        this.sustituible = sustituible;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public List<Sustituto> getSustitutos() {
        return sustitutos;
    }

    public void setSustitutos(List<Sustituto> sustitutos) {
        this.sustitutos = sustitutos;
    }
}