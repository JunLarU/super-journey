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
        Producto producto = new Producto();

        // Buscar ID con diferentes nombres posibles
        if (json.has("ID")) {
            producto.setId(json.getInt("ID"));
        } else if (json.has("id")) {
            producto.setId(json.getInt("id"));
        } else if (json.has("Id")) {
            producto.setId(json.getInt("Id"));
        } else {
            producto.setId(0); // Valor por defecto
        }

        // Buscar Nombre con diferentes nombres
        if (json.has("Nombre")) {
            producto.setNombre(json.optString("Nombre", ""));
        } else if (json.has("nombre")) {
            producto.setNombre(json.optString("nombre", ""));
        }

        // Buscar Descripcion con diferentes nombres
        if (json.has("Descripcion")) {
            producto.setDescripcion(json.optString("Descripcion", ""));
        } else if (json.has("descripcion")) {
            producto.setDescripcion(json.optString("descripcion", ""));
        }

        // Buscar PrecioBase con diferentes nombres
        if (json.has("PrecioBase")) {
            producto.setPrecioBase(json.optDouble("PrecioBase", 0.0));
        } else if (json.has("precioBase")) {
            producto.setPrecioBase(json.optDouble("precioBase", 0.0));
        }

        // Buscar Categoria con diferentes nombres
        if (json.has("Categoria")) {
            producto.setCategoria(json.optString("Categoria", ""));
        } else if (json.has("categoria")) {
            producto.setCategoria(json.optString("categoria", ""));
        }

        // Buscar IDCategoria con diferentes nombres
        if (json.has("IDCategoria")) {
            producto.setIdCategoria(json.optInt("IDCategoria", 0));
        } else if (json.has("idCategoria")) {
            producto.setIdCategoria(json.optInt("idCategoria", 0));
        }

        // Buscar Gramaje con diferentes nombres
        if (json.has("Gramaje")) {
            producto.setGramaje(json.optDouble("Gramaje", 0.0));
        } else if (json.has("gramaje")) {
            producto.setGramaje(json.optDouble("gramaje", 0.0));
        }

        // Buscar Calorias con diferentes nombres
        if (json.has("Calorias")) {
            producto.setCalorias(json.optDouble("Calorias", 0.0));
        } else if (json.has("calorias")) {
            producto.setCalorias(json.optDouble("calorias", 0.0));
        }

        // Buscar URLFoto con diferentes nombres
        if (json.has("URLFoto")) {
            String url = json.optString("URLFoto", "");
            producto.setUrlFoto(url.isEmpty() ? null : url);
        } else if (json.has("urlFoto")) {
            String url = json.optString("urlFoto", "");
            producto.setUrlFoto(url.isEmpty() ? null : url);
        }

        // Buscar Disponible con diferentes nombres
        if (json.has("Disponible")) {
            int disp = json.optInt("Disponible", 1);
            producto.setDisponible(disp == 1);
        } else if (json.has("disponible")) {
            if (json.get("disponible") instanceof Boolean) {
                producto.setDisponible(json.getBoolean("disponible"));
            } else {
                int disp = json.optInt("disponible", 1);
                producto.setDisponible(disp == 1);
            }
        } else {
            producto.setDisponible(true);
        }

        // Cargar ingredientes
        if (json.has("Ingredientes")) {
            JSONArray ingredientesArray = json.getJSONArray("Ingredientes");
            List<ProductoIngrediente> ingredientes = new ArrayList<>();

            for (int i = 0; i < ingredientesArray.length(); i++) {
                JSONObject ingJson = ingredientesArray.getJSONObject(i);
                ProductoIngrediente pi = new ProductoIngrediente(ingJson);
                ingredientes.add(pi);
            }
            producto.setIngredientes(ingredientes);
        }

        // Cargar tamaños
        if (json.has("Tamanos")) {
            JSONArray tamanosArray = json.getJSONArray("Tamanos");
            List<TamanoProducto> tamanos = new ArrayList<>();

            for (int i = 0; i < tamanosArray.length(); i++) {
                JSONObject tamJson = tamanosArray.getJSONObject(i);
                TamanoProducto tam = new TamanoProducto(tamJson);
                tamanos.add(tam);
            }
            producto.setTamanos(tamanos);
        }

        return producto;
    }

    // =====================================================
    // JSON → SERVIDOR
    // =====================================================
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        // Para compatibilidad con formularios que esperan "ID" en mayúsculas
        obj.put("ID", id);
        obj.put("id", id);
        
        obj.put("nombre", nombre);
        obj.put("descripcion", descripcion);
        obj.put("precioBase", precioBase);
        obj.put("categoria", categoria);
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

    // Método específico para formularios (con campos en mayúsculas)
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();

        // Usar nombres en mayúsculas para compatibilidad con formulario
        obj.put("ID", id);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("PrecioBase", precioBase);
        obj.put("Categoria", categoria);
        obj.put("IDCategoria", idCategoria);
        obj.put("Gramaje", gramaje);
        obj.put("Calorias", calorias);
        obj.put("URLFoto", urlFoto == null ? JSONObject.NULL : urlFoto);
        obj.put("Disponible", disponible ? 1 : 0);

        JSONArray ingArr = new JSONArray();
        for (ProductoIngrediente pi : ingredientes) {
            ingArr.put(pi.toJsonForForm());
        }
        obj.put("Ingredientes", ingArr);

        JSONArray tamArr = new JSONArray();
        for (TamanoProducto t : tamanos) {
            tamArr.put(t.toJsonForForm());
        }
        obj.put("Tamanos", tamArr);

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
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public double getPrecioBase() {
        return precioBase;
    }

    public void setPrecioBase(double precioBase) {
        this.precioBase = precioBase;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(int idCategoria) {
        this.idCategoria = idCategoria;
    }

    public double getGramaje() {
        return gramaje;
    }

    public void setGramaje(double gramaje) {
        this.gramaje = gramaje;
    }

    public double getCalorias() {
        return calorias;
    }

    public void setCalorias(double calorias) {
        this.calorias = calorias;
    }

    public String getUrlFoto() {
        return urlFoto;
    }

    public void setUrlFoto(String urlFoto) {
        this.urlFoto = urlFoto;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }

    public List<ProductoIngrediente> getIngredientes() {
        return new ArrayList<>(ingredientes);
    }

    public void setIngredientes(List<ProductoIngrediente> ingredientes) {
        this.ingredientes = new ArrayList<>(ingredientes);
    }

    public List<TamanoProducto> getTamanos() {
        return new ArrayList<>(tamanos);
    }

    public void setTamanos(List<TamanoProducto> tamanos) {
        this.tamanos = new ArrayList<>(tamanos);
    }

    @Override
    public String toString() {
        return nombre + " - $" + precioBase;
    }

    // CONSTRUCTOR alternativo (mantener por compatibilidad)
    public Producto(int id, String nombre, String descripcion, double precioBase,
                    String categoria, double gramaje, double calorias,
                    String urlFoto, boolean disponible) {

        this(id, nombre, descripcion, precioBase, categoria, 0, 
             gramaje, calorias, urlFoto, disponible);
    }
}