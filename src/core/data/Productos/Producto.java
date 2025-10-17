package core.data.Productos;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un producto de la cafetería con ingredientes y tamaños
 */
public class Producto {
    private int id;
    private String nombre;
    private String descripcion;
    private double precioBase;
    private String categoria;
    private double gramaje;
    private double calorias;
    private String urlFoto;
    private boolean disponible;
    private List<ProductoIngrediente> ingredientes;
    private List<TamanoProducto> tamanos; // ✅ NUEVO: Lista de tamaños

    // Constructor principal
    public Producto(int id, String nombre, String descripcion, double precioBase, 
                   String categoria, double gramaje, double calorias, String urlFoto, boolean disponible) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioBase = precioBase;
        this.categoria = categoria;
        this.gramaje = gramaje;
        this.calorias = calorias;
        this.urlFoto = urlFoto;
        this.disponible = disponible;
        this.ingredientes = new ArrayList<>();
        this.tamanos = new ArrayList<>(); // ✅ Inicializar lista de tamaños
    }

    // Constructor desde JSON
    public Producto(JSONObject json) {
        this.id = json.getInt("ID");
        this.nombre = json.getString("Nombre");
        this.descripcion = json.optString("Descripcion", "");
        this.precioBase = json.getDouble("PrecioBase");
        this.categoria = json.optString("Categoria", "");
        this.gramaje = json.optDouble("Gramaje", 0.0);
        this.calorias = json.optDouble("Calorias", 0.0);
        this.urlFoto = json.optString("URLFoto", "");
        this.disponible = json.optBoolean("Disponible", true);
        
        // Cargar ingredientes
        this.ingredientes = new ArrayList<>();
        if (json.has("Ingredientes")) {
            JSONArray ingArray = json.getJSONArray("Ingredientes");
            for (int i = 0; i < ingArray.length(); i++) {
                ingredientes.add(new ProductoIngrediente(ingArray.getJSONObject(i)));
            }
        }
        
        // ✅ NUEVO: Cargar tamaños
        this.tamanos = new ArrayList<>();
        if (json.has("Tamanos")) {
            JSONArray tamArray = json.getJSONArray("Tamanos");
            for (int i = 0; i < tamArray.length(); i++) {
                tamanos.add(new TamanoProducto(tamArray.getJSONObject(i)));
            }
        }
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("PrecioBase", precioBase);
        obj.put("Categoria", categoria == null ? JSONObject.NULL : categoria);
        obj.put("Gramaje", gramaje);
        obj.put("Calorias", calorias);
        obj.put("URLFoto", urlFoto == null ? JSONObject.NULL : urlFoto);
        obj.put("Disponible", disponible);
        
        // Guardar ingredientes
        JSONArray ingArray = new JSONArray();
        for (ProductoIngrediente pi : ingredientes) {
            ingArray.put(pi.toJson());
        }
        obj.put("Ingredientes", ingArray);
        
        // ✅ NUEVO: Guardar tamaños
        JSONArray tamArray = new JSONArray();
        for (TamanoProducto tam : tamanos) {
            tamArray.put(tam.toJson());
        }
        obj.put("Tamanos", tamArray);
        
        return obj;
    }

    // Métodos para manejar ingredientes
    public void agregarIngrediente(ProductoIngrediente ingrediente) {
        ingredientes.add(ingrediente);
    }

    public void eliminarIngrediente(int idIngrediente) {
        ingredientes.removeIf(pi -> pi.getIdIngrediente() == idIngrediente);
    }

    public ProductoIngrediente getIngrediente(int idIngrediente) {
        return ingredientes.stream()
                .filter(pi -> pi.getIdIngrediente() == idIngrediente)
                .findFirst()
                .orElse(null);
    }

    // ✅ NUEVO: Métodos para manejar tamaños
    public void agregarTamano(TamanoProducto tamano) {
        tamanos.add(tamano);
    }

    public void eliminarTamano(int id) {
        tamanos.removeIf(t -> t.getId() == id);
    }

    public TamanoProducto getTamano(int id) {
        return tamanos.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getPrecioBase() { return precioBase; }
    public String getCategoria() { return categoria; }
    public double getGramaje() { return gramaje; }
    public double getCalorias() { return calorias; }
    public String getUrlFoto() { return urlFoto; }
    public boolean isDisponible() { return disponible; }
    public List<ProductoIngrediente> getIngredientes() { return new ArrayList<>(ingredientes); }
    public List<TamanoProducto> getTamanos() { return new ArrayList<>(tamanos); } // ✅ NUEVO

    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setGramaje(double gramaje) { this.gramaje = gramaje; }
    public void setCalorias(double calorias) { this.calorias = calorias; }
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public void setIngredientes(List<ProductoIngrediente> ingredientes) { 
        this.ingredientes = new ArrayList<>(ingredientes); 
    }
    public void setTamanos(List<TamanoProducto> tamanos) { // ✅ NUEVO
        this.tamanos = new ArrayList<>(tamanos); 
    }

    @Override
    public String toString() {
        return nombre + " - $" + precioBase + " (" + (disponible ? "Disponible" : "No disponible") + ")";
    }
}