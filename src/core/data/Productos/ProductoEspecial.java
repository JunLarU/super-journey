// ProductoEspecial.java
package core.data.Productos;

import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un producto especial con precio y fechas/horas específicas
 * Corresponde a la tabla ProductosEspeciales en la BD
 */
public class ProductoEspecial {
    private int id;
    private int idProducto;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String descripcion;
    private double precioEspecial;
    private boolean activo;

    public ProductoEspecial(int id, int idProducto, LocalDateTime fechaInicio, LocalDateTime fechaFin, 
                           String descripcion, double precioEspecial, boolean activo) {
        this.id = id;
        this.idProducto = idProducto;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.descripcion = descripcion;
        this.precioEspecial = precioEspecial;
        this.activo = activo;
    }

    // Constructor desde JSON
    public ProductoEspecial(JSONObject json) {
        this.id = json.getInt("ID");
        this.idProducto = json.getInt("IDProducto");
        this.fechaInicio = LocalDateTime.parse(json.getString("FechaInicio"));
        this.fechaFin = LocalDateTime.parse(json.getString("FechaFin"));
        this.descripcion = json.optString("Descripcion", "");
        this.precioEspecial = json.getDouble("PrecioEspecial");
        this.activo = json.optBoolean("Activo", true);
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("IDProducto", idProducto);
        obj.put("FechaInicio", fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        obj.put("FechaFin", fechaFin.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        obj.put("Descripcion", descripcion);
        obj.put("PrecioEspecial", precioEspecial);
        obj.put("Activo", activo);
        return obj;
    }

    // Método para verificar si el especial está activo para una fecha y hora específica
    public boolean estaActivoParaFechaHora(LocalDateTime fechaHora) {
        return activo && 
               !fechaHora.isBefore(fechaInicio) && 
               !fechaHora.isAfter(fechaFin);
    }

    // Método para verificar si el especial está vigente actualmente
    public boolean estaVigente() {
        return estaActivoParaFechaHora(LocalDateTime.now());
    }

    // Getters y Setters
    public int getId() { return id; }
    public int getIdProducto() { return idProducto; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public String getDescripcion() { return descripcion; }
    public double getPrecioEspecial() { return precioEspecial; }
    public boolean isActivo() { return activo; }

    public void setId(int id) { this.id = id; }
    public void setIdProducto(int idProducto) { this.idProducto = idProducto; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecioEspecial(double precioEspecial) { this.precioEspecial = precioEspecial; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "ProductoEspecial #" + id + " (Producto: " + idProducto + ") - $" + precioEspecial + 
               " (" + fechaInicio.format(formatter) + " a " + fechaFin.format(formatter) + ")";
    }
}