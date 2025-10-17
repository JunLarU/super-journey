package core.data.Productos;

import org.json.JSONObject;

/**
 * Representa un tamaño específico de un producto
 * Ejemplo: Chico, Mediano, Grande, Media Orden, etc.
 */
public class TamanoProducto {
    private int id;
    private String nombre; // Chico, Mediano, Grande, Orden Completa, etc.
    private String descripcion; // Descripción adicional
    private double capacidad; // en ml (para bebidas)
    private double gramaje; // en gramos (para comida)
    private int piezas; // número de piezas (para tacos, nuggets, etc.)
    private double precio;
    private int orden; // Para ordenar (1=más pequeño, 2=mediano, 3=más grande)
    private boolean disponible;

    public TamanoProducto(int id, String nombre, String descripcion, double capacidad, 
                         double gramaje, int piezas, double precio, int orden, boolean disponible) {
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

    // Constructor desde JSON
    public TamanoProducto(JSONObject json) {
        this.id = json.optInt("ID", 0);
        this.nombre = json.getString("Nombre");
        this.descripcion = json.optString("Descripcion", "");
        this.capacidad = json.optDouble("Capacidad", 0.0);
        this.gramaje = json.optDouble("Gramaje", 0.0);
        this.piezas = json.optInt("Piezas", 0);
        this.precio = json.getDouble("Precio");
        this.orden = json.optInt("Orden", 1);
        this.disponible = json.optBoolean("Disponible", true);
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("Capacidad", capacidad);
        obj.put("Gramaje", gramaje);
        obj.put("Piezas", piezas);
        obj.put("Precio", precio);
        obj.put("Orden", orden);
        obj.put("Disponible", disponible);
        return obj;
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getCapacidad() { return capacidad; }
    public double getGramaje() { return gramaje; }
    public int getPiezas() { return piezas; }
    public double getPrecio() { return precio; }
    public int getOrden() { return orden; }
    public boolean isDisponible() { return disponible; }

    public void setId(int id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setCapacidad(double capacidad) { this.capacidad = capacidad; }
    public void setGramaje(double gramaje) { this.gramaje = gramaje; }
    public void setPiezas(int piezas) { this.piezas = piezas; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setOrden(int orden) { this.orden = orden; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nombre);
        if (capacidad > 0) sb.append(" (").append(capacidad).append("ml)");
        if (gramaje > 0) sb.append(" (").append(gramaje).append("g)");
        if (piezas > 0) sb.append(" (").append(piezas).append(" pzas)");
        sb.append(" - $").append(precio);
        return sb.toString();
    }
}