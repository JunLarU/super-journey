package core.data.Productos;

import org.json.JSONObject;

public class TamanoProducto {

    private int id;
    private String nombre;
    private String descripcion;
    private Double capacidad;
    private Double gramaje;
    private Integer piezas;
    private double precio;
    private int orden;
    private boolean disponible;

    public TamanoProducto() {
    }

    public TamanoProducto(JSONObject json) {
        // Buscar campos con diferentes nombres (mayúsculas/minúsculas)
        if (json.has("ID")) {
            this.id = json.optInt("ID", 0);
        } else if (json.has("id")) {
            this.id = json.optInt("id", 0);
        }

        if (json.has("Nombre")) {
            this.nombre = json.optString("Nombre", "");
        } else if (json.has("nombre")) {
            this.nombre = json.optString("nombre", "");
        }

        if (json.has("Descripcion")) {
            this.descripcion = json.optString("Descripcion", "");
        } else if (json.has("descripcion")) {
            this.descripcion = json.optString("descripcion", "");
        }

        // Capacidad
        if (json.has("Capacidad") && !json.isNull("Capacidad")) {
            this.capacidad = json.optDouble("Capacidad");
        } else if (json.has("capacidad") && !json.isNull("capacidad")) {
            this.capacidad = json.optDouble("capacidad");
        }

        // Gramaje
        if (json.has("Gramaje") && !json.isNull("Gramaje")) {
            this.gramaje = json.optDouble("Gramaje");
        } else if (json.has("gramaje") && !json.isNull("gramaje")) {
            this.gramaje = json.optDouble("gramaje");
        }

        // Piezas
        if (json.has("Piezas") && !json.isNull("Piezas")) {
            this.piezas = json.optInt("Piezas");
        } else if (json.has("piezas") && !json.isNull("piezas")) {
            this.piezas = json.optInt("piezas");
        }

        // Precio
        if (json.has("Precio")) {
            this.precio = json.optDouble("Precio", 0.0);
        } else if (json.has("precio")) {
            this.precio = json.optDouble("precio", 0.0);
        }

        // Orden
        if (json.has("Orden")) {
            this.orden = json.optInt("Orden", 1);
        } else if (json.has("orden")) {
            this.orden = json.optInt("orden", 1);
        }

        // Disponible
        if (json.has("Disponible")) {
            int disp = json.optInt("Disponible", 1);
            this.disponible = disp == 1;
        } else if (json.has("disponible")) {
            if (json.get("disponible") instanceof Boolean) {
                this.disponible = json.getBoolean("disponible");
            } else {
                int disp = json.optInt("disponible", 1);
                this.disponible = disp == 1;
            }
        }
    }

    // Constructor con parámetros individuales
    public TamanoProducto(int id, String nombre, String descripcion,
            Double capacidad, Double gramaje, Integer piezas,
            double precio, int orden, boolean disponible) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.capacidad = capacidad;
        this.gramaje = gramaje;
        this.piezas = piezas;
        this.precio = precio;
        this.orden = orden;
        this.disponible = disponible;
    }

    // Para enviar al servidor (usando minúsculas)
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        obj.put("id", id);
        obj.put("nombre", nombre);
        obj.put("descripcion", descripcion);
        obj.put("capacidad", capacidad != null ? capacidad : JSONObject.NULL);
        obj.put("gramaje", gramaje != null ? gramaje : JSONObject.NULL);
        obj.put("piezas", piezas != null ? piezas : JSONObject.NULL);
        obj.put("precio", precio);
        obj.put("orden", orden);
        obj.put("disponible", disponible ? 1 : 0);

        return obj;
    }

    // Para formularios (usando mayúsculas)
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();

        obj.put("ID", id);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("Capacidad", capacidad != null ? capacidad : JSONObject.NULL);
        obj.put("Gramaje", gramaje != null ? gramaje : JSONObject.NULL);
        obj.put("Piezas", piezas != null ? piezas : JSONObject.NULL);
        obj.put("Precio", precio);
        obj.put("Orden", orden);
        obj.put("Disponible", disponible ? 1 : 0);

        return obj;
    }

    // Getters y Setters
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

    public Double getCapacidad() {
        return capacidad;
    }

    public void setCapacidad(Double capacidad) {
        this.capacidad = capacidad;
    }

    public Double getGramaje() {
        return gramaje;
    }

    public void setGramaje(Double gramaje) {
        this.gramaje = gramaje;
    }

    public Integer getPiezas() {
        return piezas;
    }

    public void setPiezas(Integer piezas) {
        this.piezas = piezas;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}