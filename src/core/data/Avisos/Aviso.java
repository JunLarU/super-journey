package core.data.Avisos;

import org.json.JSONObject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

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
    private int idUsuarioCreador; // Cambiado a int para coincidir con BD
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

    // Constructor vacío
    public Aviso() {
        this.id = 0;
        this.titulo = "";
        this.contenido = "";
        this.establecimiento = Establecimiento.Cafeteria;
        this.tipoAviso = TipoAviso.General;
        this.prioridad = Prioridad.Normal;
        this.fechaPublicacion = LocalDateTime.now();
        this.fechaInicio = LocalDateTime.now();
        this.fechaFin = LocalDateTime.now().plusDays(1);
        this.idUsuarioCreador = 0;
        this.activo = true;
    }

    // Constructor completo
    public Aviso(int id, String titulo, String contenido, Establecimiento establecimiento,
            TipoAviso tipoAviso, Prioridad prioridad, LocalDateTime fechaPublicacion,
            LocalDateTime fechaInicio, LocalDateTime fechaFin, int idUsuarioCreador, boolean activo) {
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

    // Constructor desde JSON (para respuesta del servidor)
    public Aviso(JSONObject json) {
        this.id = json.getInt("ID");
        this.titulo = json.getString("Titulo");
        this.contenido = json.getString("Contenido");

        // Parsear enums
        this.establecimiento = parseEstablecimiento(json.getString("Establecimiento"));
        this.tipoAviso = parseTipoAviso(json.getString("TipoAviso"));
        this.prioridad = parsePrioridad(json.getString("Prioridad"));

        // Formateadores
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            // Fecha de publicación
            if (json.has("FechaPublicacion") && !json.isNull("FechaPublicacion")) {
                String fechaPubStr = json.getString("FechaPublicacion");
                this.fechaPublicacion = LocalDateTime.parse(fechaPubStr, dateTimeFormatter);
            } else {
                this.fechaPublicacion = LocalDateTime.now();
            }

            // Fechas de inicio y fin
            String fechaInicioStr = json.getString("FechaInicio");
            String fechaFinStr = json.getString("FechaFin");

            // Intentar parsear como DATETIME primero
            try {
                this.fechaInicio = LocalDateTime.parse(fechaInicioStr, dateTimeFormatter);
            } catch (DateTimeParseException e1) {
                try {
                    // Intentar como solo fecha
                    this.fechaInicio = LocalDate.parse(fechaInicioStr, dateFormatter).atStartOfDay();
                } catch (DateTimeParseException e2) {
                    System.err.println("Error parsing FechaInicio: " + fechaInicioStr);
                    this.fechaInicio = LocalDateTime.now();
                }
            }

            try {
                this.fechaFin = LocalDateTime.parse(fechaFinStr, dateTimeFormatter);
            } catch (DateTimeParseException e1) {
                try {
                    this.fechaFin = LocalDate.parse(fechaFinStr, dateFormatter).atTime(23, 59, 59);
                } catch (DateTimeParseException e2) {
                    System.err.println("Error parsing FechaFin: " + fechaFinStr);
                    this.fechaFin = LocalDateTime.now().plusDays(1);
                }
            }

        } catch (Exception e) {
            System.err.println("Error parsing dates in Aviso constructor: " + e.getMessage());
            this.fechaPublicacion = LocalDateTime.now();
            this.fechaInicio = LocalDateTime.now();
            this.fechaFin = LocalDateTime.now().plusDays(1);
        }

        this.idUsuarioCreador = json.optInt("IDUsuarioCreador", 0);
        this.activo = json.optInt("Activo", 1) == 1;
    }

    // En toJson, asegúrate de enviar DATETIME completo:
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        if (id > 0) {
            obj.put("id", id);
        }

        obj.put("titulo", titulo);
        obj.put("contenido", contenido);
        obj.put("establecimiento", establecimiento.name());
        obj.put("tipoAviso", tipoAviso.name());
        obj.put("prioridad", prioridad.name());

        // Enviar DATETIME completo
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        obj.put("fechaInicio", fechaInicio.format(formatter));
        obj.put("fechaFin", fechaFin.format(formatter));

        // También enviar horas separadas por si el backend las necesita
        obj.put("horaInicio", fechaInicio.getHour());
        obj.put("minutoInicio", fechaInicio.getMinute());
        obj.put("horaFin", fechaFin.getHour());
        obj.put("minutoFin", fechaFin.getMinute());

        obj.put("idUsuarioCreador", idUsuarioCreador);
        obj.put("activo", activo);

        return obj;
    }

    // Métodos helper para parsear enums
    private Establecimiento parseEstablecimiento(String estab) {
        try {
            return Establecimiento.valueOf(estab);
        } catch (IllegalArgumentException e) {
            System.err.println("Establecimiento inválido: " + estab + ", usando Cafeteria por defecto");
            return Establecimiento.Cafeteria;
        }
    }

    private TipoAviso parseTipoAviso(String tipo) {
        try {
            return TipoAviso.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            System.err.println("TipoAviso inválido: " + tipo + ", usando General por defecto");
            return TipoAviso.General;
        }
    }

    private Prioridad parsePrioridad(String prioridad) {
        try {
            return Prioridad.valueOf(prioridad);
        } catch (IllegalArgumentException e) {
            System.err.println("Prioridad inválida: " + prioridad + ", usando Normal por defecto");
            return Prioridad.Normal;
        }
    }

    // Método para verificar si el aviso está activo para una fecha y hora
    // específica
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
    public int getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getContenido() {
        return contenido;
    }

    public Establecimiento getEstablecimiento() {
        return establecimiento;
    }

    public TipoAviso getTipoAviso() {
        return tipoAviso;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public int getIdUsuarioCreador() {
        return idUsuarioCreador;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public void setEstablecimiento(Establecimiento establecimiento) {
        this.establecimiento = establecimiento;
    }

    public void setTipoAviso(TipoAviso tipoAviso) {
        this.tipoAviso = tipoAviso;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public void setIdUsuarioCreador(int idUsuarioCreador) {
        this.idUsuarioCreador = idUsuarioCreador;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return "Aviso #" + id + " - " + titulo +
                " (" + fechaInicio.format(formatter) + " a " + fechaFin.format(formatter) + ")";
    }
}