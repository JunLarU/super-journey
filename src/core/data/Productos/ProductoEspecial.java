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

    // Constructor vacío para compatibilidad
    public ProductoEspecial() {
    }

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

    // Constructor desde JSON - VERSIÓN CORREGIDA
    public ProductoEspecial(JSONObject json) {
        try {
            // Obtener ID de diferentes formas posibles
            if (json.has("ID")) {
                this.id = json.getInt("ID");
            } else if (json.has("id")) {
                this.id = json.getInt("id");
            } else {
                this.id = 0;
            }

            // Obtener IDProducto de diferentes formas posibles
            if (json.has("IDProducto")) {
                this.idProducto = json.getInt("IDProducto");
            } else if (json.has("idProducto")) {
                this.idProducto = json.getInt("idProducto");
            } else {
                this.idProducto = 0;
            }

            // Manejo seguro de fechas - NO AGREGAR HORAS AUTOMÁTICAMENTE
            String fechaInicioStr = json.optString("FechaInicio", "");
            String fechaFinStr = json.optString("FechaFin", "");

            // Manejar diferentes formatos de fecha
            try {
                if (!fechaInicioStr.isEmpty()) {
                    // Intentar diferentes formatos
                    this.fechaInicio = parseDateTime(fechaInicioStr);
                } else {
                    this.fechaInicio = LocalDateTime.now();
                }
            } catch (Exception e) {
                System.err.println("Error parseando fechaInicio: " + fechaInicioStr + " - " + e.getMessage());
                this.fechaInicio = LocalDateTime.now();
            }

            try {
                if (!fechaFinStr.isEmpty()) {
                    this.fechaFin = parseDateTime(fechaFinStr);
                } else {
                    this.fechaFin = LocalDateTime.now().plusDays(1);
                }
            } catch (Exception e) {
                System.err.println("Error parseando fechaFin: " + fechaFinStr + " - " + e.getMessage());
                this.fechaFin = LocalDateTime.now().plusDays(1);
            }

            this.descripcion = json.optString("Descripcion", "");

            // Precio especial
            if (json.has("PrecioEspecial")) {
                this.precioEspecial = json.getDouble("PrecioEspecial");
            } else if (json.has("precioEspecial")) {
                this.precioEspecial = json.getDouble("precioEspecial");
            } else {
                this.precioEspecial = 0.0;
            }

            // Manejo seguro de activo
            if (json.has("Activo")) {
                Object activoObj = json.get("Activo");
                this.activo = parseBoolean(activoObj);
            } else if (json.has("activo")) {
                Object activoObj = json.get("activo");
                this.activo = parseBoolean(activoObj);
            } else {
                this.activo = true;
            }

        } catch (Exception e) {
            System.err.println("Error creando ProductoEspecial desde JSON: " + e.getMessage());
            // Valores por defecto
            this.id = 0;
            this.idProducto = 0;
            this.fechaInicio = LocalDateTime.now();
            this.fechaFin = LocalDateTime.now().plusDays(1);
            this.descripcion = "";
            this.precioEspecial = 0.0;
            this.activo = true;
        }
    }

    // Método auxiliar para parsear fechas con diferentes formatos
    private LocalDateTime parseDateTime(String dateTimeStr) throws Exception {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return LocalDateTime.now();
        }

        // Lista de formatos a intentar
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                if (formatter.toString().contains("HH") || dateTimeStr.contains(":") || dateTimeStr.contains("T")) {
                    // Tiene hora
                    return LocalDateTime.parse(
                            dateTimeStr.replace(" ", "T").replace("T", "T"),
                            formatter);
                } else {
                    // Solo fecha - usar medianoche como hora por defecto
                    return LocalDateTime.parse(dateTimeStr + " 00:00:00",
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (Exception e) {
                // Continuar con el siguiente formato
            }
        }

        throw new Exception("No se pudo parsear la fecha: " + dateTimeStr);
    }

    // Método auxiliar para parsear boolean
    private boolean parseBoolean(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        } else if (obj instanceof Integer) {
            return ((Integer) obj) == 1;
        } else if (obj instanceof String) {
            String str = (String) obj;
            return "1".equals(str) || "true".equalsIgnoreCase(str);
        } else {
            return true;
        }
    }

    // Convertir a JSON para enviar al servidor - AHORA CON HORA
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("idProducto", idProducto);
        // Enviar fecha y hora completa
        obj.put("fechaInicio", fechaInicio.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        obj.put("fechaFin", fechaFin.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        obj.put("descripcion", descripcion);
        obj.put("precioEspecial", precioEspecial);
        obj.put("activo", activo);
        return obj;
    }

    // Método para verificar si el especial está activo para una fecha y hora
    // específica
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
    public int getId() {
        return id;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getPrecioEspecial() {
        return precioEspecial;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setPrecioEspecial(double precioEspecial) {
        this.precioEspecial = precioEspecial;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "ProductoEspecial #" + id + " (Producto: " + idProducto + ") - $" + precioEspecial +
                " (" + fechaInicio.format(formatter) + " a " + fechaFin.format(formatter) + ")";
    }
}