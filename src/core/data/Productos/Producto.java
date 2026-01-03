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
    private int idCategoria;
    private double gramaje;
    private double calorias;
    private String urlFoto;
    private boolean disponible;

    private List<ProductoIngrediente> ingredientes;
    private List<TamanoProducto> tamanos;

    // =====================================================
    // CONSTRUCTOR VACÍO (NECESARIO)
    // =====================================================
    public Producto() {
        this.ingredientes = new ArrayList<>();
        this.tamanos = new ArrayList<>();
    }

    // =====================================================
    // CONSTRUCTOR PRINCIPAL
    // =====================================================
    public Producto(int id, String nombre, String descripcion, double precioBase,
                    String categoria, int idCategoria,
                    double gramaje, double calorias,
                    String urlFoto, boolean disponible) {

        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioBase = precioBase;
        this.categoria = categoria;
        this.idCategoria = idCategoria;
        this.gramaje = gramaje;
        this.calorias = calorias;
        this.urlFoto = urlFoto;
        this.disponible = disponible;
        this.ingredientes = new ArrayList<>();
        this.tamanos = new ArrayList<>();
    }

    // =====================================================
    // FACTORY: JSON → PRODUCTO (USADO POR CONTROLLER)
    // =====================================================
    public static Producto fromJSON(JSONObject json) {

        Producto p = new Producto();

        p.id = json.getInt("ID");
        p.nombre = json.getString("Nombre");
        p.descripcion = json.optString("Descripcion", "");
        p.precioBase = json.getDouble("PrecioBase");
        p.categoria = json.optString("Categoria", "");
        p.idCategoria = json.optInt("IDCategoria", 0);
        p.gramaje = json.optDouble("Gramaje", 0.0);
        p.calorias = json.optDouble("Calorias", 0.0);
        p.urlFoto = json.optString("URLFoto", "");
        p.disponible = json.optInt("Disponible", 1) == 1;

        // Ingredientes (si vienen)
        if (json.has("Ingredientes")) {
            JSONArray arr = json.getJSONArray("Ingredientes");
            for (int i = 0; i < arr.length(); i++) {
                p.ingredientes.add(
                    new ProductoIngrediente(arr.getJSONObject(i))
                );
            }
        }

        // Tamaños (si vienen)
        if (json.has("Tamanos")) {
            JSONArray arr = json.getJSONArray("Tamanos");
            for (int i = 0; i < arr.length(); i++) {
                p.tamanos.add(
                    new TamanoProducto(arr.getJSONObject(i))
                );
            }
        }

        return p;
    }

    // =====================================================
    // JSON → SERVIDOR
    // =====================================================
    public JSONObject toJson() {

        JSONObject obj = new JSONObject();

        obj.put("id", id);
        obj.put("nombre", nombre);
        obj.put("descripcion", descripcion);
        obj.put("precioBase", precioBase);
        obj.put("idCategoria", idCategoria);
        obj.put("gramaje", gramaje);
        obj.put("calorias", calorias);
        obj.put("urlFoto", urlFoto == null ? JSONObject.NULL : urlFoto);
        obj.put("disponible", disponible ? 1 : 0);

        JSONArray ingArr = new JSONArray();
        for (ProductoIngrediente pi : ingredientes) {
            ingArr.put(pi.toJson());
        }
        obj.put("ingredientes", ingArr);

        JSONArray tamArr = new JSONArray();
        for (TamanoProducto t : tamanos) {
            tamArr.put(t.toJson());
        }
        obj.put("tamanos", tamArr);

        return obj;
    }

    // =====================================================
    // MANEJO DE INGREDIENTES
    // =====================================================
    public void agregarIngrediente(ProductoIngrediente ingrediente) {
        ingredientes.add(ingrediente);
    }

    public void eliminarIngrediente(int idIngrediente) {
        ingredientes.removeIf(i -> i.getIdIngrediente() == idIngrediente);
    }

    public ProductoIngrediente getIngrediente(int idIngrediente) {
        return ingredientes.stream()
                .filter(i -> i.getIdIngrediente() == idIngrediente)
                .findFirst()
                .orElse(null);
    }

    // =====================================================
    // MANEJO DE TAMAÑOS
    // =====================================================
    public void agregarTamano(TamanoProducto t) {
        tamanos.add(t);
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

    // =====================================================
    // GETTERS / SETTERS
    // =====================================================
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getPrecioBase() { return precioBase; }
    public String getCategoria() { return categoria; }
    public int getIdCategoria() { return idCategoria; }
    public double getGramaje() { return gramaje; }
    public double getCalorias() { return calorias; }
    public String getUrlFoto() { return urlFoto; }
    public boolean isDisponible() { return disponible; }
    public List<ProductoIngrediente> getIngredientes() { return new ArrayList<>(ingredientes); }
    public List<TamanoProducto> getTamanos() { return new ArrayList<>(tamanos); }

    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecioBase(double precioBase) { this.precioBase = precioBase; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public void setIdCategoria(int idCategoria) { this.idCategoria = idCategoria; }
    public void setGramaje(double gramaje) { this.gramaje = gramaje; }
    public void setCalorias(double calorias) { this.calorias = calorias; }
    public void setUrlFoto(String urlFoto) { this.urlFoto = urlFoto; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public void setIngredientes(List<ProductoIngrediente> ingredientes) {
        this.ingredientes = new ArrayList<>(ingredientes);
    }
    public void setTamanos(List<TamanoProducto> tamanos) {
        this.tamanos = new ArrayList<>(tamanos);
    }

    @Override
    public String toString() {
        return nombre + " - $" + precioBase;
    }
}
