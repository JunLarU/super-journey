// Aviso.java
package core.data.Avisos;

import org.json.JSONObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Representa un aviso con fechas/horas específicas
 * Corresponde a la tabla Avisos en la BD
 */
public class Aviso {
    private int id;
    private String titulo;
    private String contenido;
    private Establecimiento establecimiento;
    private TipoAviso tipoAviso;
    private Prioridad prioridad;
    private LocalDateTime fechaPublicacion;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private String idUsuarioCreador;
    private boolean activo;

    public enum Establecimiento {
        Cafeteria, Cafecito, Ambos
    }

    public enum TipoAviso {
        General, Horario, NoLaboral, Oferta, Evento
    }

    public enum Prioridad {
        Normal, Importante
    }

    public Aviso(int id, String titulo, String contenido, Establecimiento establecimiento, 
                 TipoAviso tipoAviso, Prioridad prioridad, LocalDateTime fechaPublicacion,
                 LocalDateTime fechaInicio, LocalDateTime fechaFin, String idUsuarioCreador, boolean activo) {
        this.id = id;
        this.titulo = titulo;
        this.contenido = contenido;
        this.establecimiento = establecimiento;
        this.tipoAviso = tipoAviso;
        this.prioridad = prioridad;
        this.fechaPublicacion = fechaPublicacion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.idUsuarioCreador = idUsuarioCreador;
        this.activo = activo;
    }

    // Constructor desde JSON
    public Aviso(JSONObject json) {
        this.id = json.getInt("ID");
        this.titulo = json.getString("Titulo");
        this.contenido = json.getString("Contenido");
        this.establecimiento = Establecimiento.valueOf(json.getString("Establecimiento"));
        this.tipoAviso = TipoAviso.valueOf(json.getString("TipoAviso"));
        this.prioridad = Prioridad.valueOf(json.getString("Prioridad"));
        this.fechaPublicacion = LocalDateTime.parse(json.getString("FechaPublicacion"));
        this.fechaInicio = LocalDateTime.parse(json.getString("FechaInicio"));
        this.fechaFin = LocalDateTime.parse(json.getString("FechaFin"));
        this.idUsuarioCreador = json.getString("IDUsuarioCreador");
        this.activo = json.optBoolean("Activo", true);
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("Titulo", titulo);
        obj.put("Contenido", contenido);
        obj.put("Establecimiento", establecimiento.name());
        obj.put("TipoAviso", tipoAviso.name());
        obj.put("Prioridad", prioridad.name());
        obj.put("FechaPublicacion", fechaPublicacion.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        obj.put("FechaInicio", fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        obj.put("FechaFin", fechaFin.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        obj.put("IDUsuarioCreador", idUsuarioCreador);
        obj.put("Activo", activo);
        return obj;
    }

    // Método para verificar si el aviso está activo para una fecha y hora específica
    public boolean estaActivoParaFechaHora(LocalDateTime fechaHora) {
        return activo && 
               !fechaHora.isBefore(fechaInicio) && 
               !fechaHora.isAfter(fechaFin);
    }

    // Método para verificar si el aviso está vigente actualmente
    public boolean estaVigente() {
        return estaActivoParaFechaHora(LocalDateTime.now());
    }

    // Getters y Setters
    public int getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getContenido() { return contenido; }
    public Establecimiento getEstablecimiento() { return establecimiento; }
    public TipoAviso getTipoAviso() { return tipoAviso; }
    public Prioridad getPrioridad() { return prioridad; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public String getIdUsuarioCreador() { return idUsuarioCreador; }
    public boolean isActivo() { return activo; }

    public void setId(int id) { this.id = id; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public void setContenido(String contenido) { this.contenido = contenido; }
    public void setEstablecimiento(Establecimiento establecimiento) { this.establecimiento = establecimiento; }
    public void setTipoAviso(TipoAviso tipoAviso) { this.tipoAviso = tipoAviso; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public void setIdUsuarioCreador(String idUsuarioCreador) { this.idUsuarioCreador = idUsuarioCreador; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return "Aviso #" + id + " - " + titulo + 
               " (" + fechaInicio.format(formatter) + " a " + fechaFin.format(formatter) + ")";
    }
}