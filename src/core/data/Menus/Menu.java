package core.data.Menus;

import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa un día/horario específico del menú semanal
 * Corresponde a la tabla MenuSemanal en la BD
 */
public class Menu {
    private int id;
    private LocalDate fecha;
    private String diaSemana; // Lunes, Martes, Miércoles, etc.
    private String horario; // Desayuno, Comida
    private int numeroSemana;
    private int anio;
    private String fechaCreacion;
    private boolean activo;
    private int idUsuarioCreador; // 🔹 NUEVO - Para auditoría
    private int idUsuarioModificador; // 🔹 NUEVO - Para auditoría
    private String fechaModificacion; // 🔹 NUEVO - Para auditoría
    private List<MenuSeccion> secciones;

    public Menu(int id, LocalDate fecha, String diaSemana, String horario, 
                int numeroSemana, int anio, String fechaCreacion, boolean activo,
                int idUsuarioCreador, int idUsuarioModificador, String fechaModificacion) {
        this.id = id;
        this.fecha = fecha;
        this.diaSemana = diaSemana;
        this.horario = horario;
        this.numeroSemana = numeroSemana;
        this.anio = anio;
        this.fechaCreacion = fechaCreacion;
        this.activo = activo;
        this.idUsuarioCreador = idUsuarioCreador;
        this.idUsuarioModificador = idUsuarioModificador;
        this.fechaModificacion = fechaModificacion;
        this.secciones = new ArrayList<>();
    }

    // Constructor desde JSON
    public Menu(JSONObject json) {
        this.id = json.getInt("ID");
        this.fecha = LocalDate.parse(json.getString("Fecha"));
        this.diaSemana = json.getString("DiaSemana");
        this.horario = json.getString("Horario");
        this.numeroSemana = json.getInt("NumeroSemana");
        this.anio = json.getInt("Anio");
        this.fechaCreacion = json.optString("FechaCreacion", LocalDate.now().toString());
        this.activo = json.optBoolean("Activo", true);
        this.idUsuarioCreador = json.optInt("IDUsuarioCreador", 0);
        this.idUsuarioModificador = json.optInt("IDUsuarioModificador", 0);
        this.fechaModificacion = json.optString("FechaModificacion", null);
        
        // Cargar secciones
        this.secciones = new ArrayList<>();
        if (json.has("Secciones")) {
            JSONArray seccionesArray = json.getJSONArray("Secciones");
            for (int i = 0; i < seccionesArray.length(); i++) {
                secciones.add(new MenuSeccion(seccionesArray.getJSONObject(i)));
            }
        }
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("Fecha", fecha.format(DateTimeFormatter.ISO_LOCAL_DATE));
        obj.put("DiaSemana", diaSemana);
        obj.put("Horario", horario);
        obj.put("NumeroSemana", numeroSemana);
        obj.put("Anio", anio);
        obj.put("FechaCreacion", fechaCreacion);
        obj.put("Activo", activo);
        obj.put("IDUsuarioCreador", idUsuarioCreador);
        obj.put("IDUsuarioModificador", idUsuarioModificador);
        if (fechaModificacion != null) {
            obj.put("FechaModificacion", fechaModificacion);
        }
        
        // Guardar secciones
        JSONArray seccionesArray = new JSONArray();
        for (MenuSeccion seccion : secciones) {
            seccionesArray.put(seccion.toJson());
        }
        obj.put("Secciones", seccionesArray);
        
        return obj;
    }

    // Métodos para manejar secciones
    public void agregarSeccion(MenuSeccion seccion) {
        secciones.add(seccion);
    }

    public void eliminarSeccion(int idSeccion) {
        secciones.removeIf(s -> s.getIdSeccion() == idSeccion);
    }

    public MenuSeccion getSeccion(int idSeccion) {
        return secciones.stream()
                .filter(s -> s.getIdSeccion() == idSeccion)
                .findFirst()
                .orElse(null);
    }

    // Método para actualizar fecha de modificación
    public void registrarModificacion(int idUsuario) {
        this.idUsuarioModificador = idUsuario;
        this.fechaModificacion = LocalDate.now().toString();
    }

    // Getters y Setters
    public int getId() { return id; }
    public LocalDate getFecha() { return fecha; }
    public String getDiaSemana() { return diaSemana; }
    public String getHorario() { return horario; }
    public int getNumeroSemana() { return numeroSemana; }
    public int getAnio() { return anio; }
    public String getFechaCreacion() { return fechaCreacion; }
    public boolean isActivo() { return activo; }
    public int getIdUsuarioCreador() { return idUsuarioCreador; }
    public int getIdUsuarioModificador() { return idUsuarioModificador; }
    public String getFechaModificacion() { return fechaModificacion; }
    public List<MenuSeccion> getSecciones() { return new ArrayList<>(secciones); }

    public void setId(int id) { this.id = id; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }
    public void setHorario(String horario) { this.horario = horario; }
    public void setNumeroSemana(int numeroSemana) { this.numeroSemana = numeroSemana; }
    public void setAnio(int anio) { this.anio = anio; }
    public void setFechaCreacion(String fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setIdUsuarioCreador(int idUsuarioCreador) { this.idUsuarioCreador = idUsuarioCreador; }
    public void setIdUsuarioModificador(int idUsuarioModificador) { this.idUsuarioModificador = idUsuarioModificador; }
    public void setFechaModificacion(String fechaModificacion) { this.fechaModificacion = fechaModificacion; }
    public void setSecciones(List<MenuSeccion> secciones) { 
        this.secciones = new ArrayList<>(secciones); 
    }

    @Override
    public String toString() {
        return fecha + " (" + diaSemana + ") - " + horario + " - Semana " + numeroSemana;
    }
}