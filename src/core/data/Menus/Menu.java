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
    private String fecha; // Cambiado a String para compatibilidad
    private String diaSemana; // Lunes, Martes, Miércoles, etc.
    private String horario; // Desayuno, Comida
    private int numeroSemana;
    private int anio;
    private String fechaCreacion;
    private boolean activo;
    private List<MenuSeccion> secciones;
    private int idUsuarioCreador;
    private int idUsuarioModificador;
    private String fechaModificacion;

    // Constructor para nuevo menú
    public Menu() {
        this.id = 0;
        this.fecha = LocalDate.now().toString();
        this.diaSemana = "";
        this.horario = "";
        this.numeroSemana = 0;
        this.anio = 0;
        this.fechaCreacion = LocalDate.now().toString();
        this.activo = true;
        this.secciones = new ArrayList<>();
    }

    // Constructor con parámetros básicos
    public Menu(int id, String fecha, String diaSemana, String horario,
            int numeroSemana, int anio) {
        this.id = id;
        this.fecha = fecha;
        this.diaSemana = diaSemana;
        this.horario = horario;
        this.numeroSemana = numeroSemana;
        this.anio = anio;
        this.fechaCreacion = LocalDate.now().toString();
        this.activo = true;
        this.secciones = new ArrayList<>();
    }

    // Constructor completo
    public Menu(int id, String fecha, String diaSemana, String horario,
            int numeroSemana, int anio, String fechaCreacion, boolean activo) {
        this.id = id;
        this.fecha = fecha;
        this.diaSemana = diaSemana;
        this.horario = horario;
        this.numeroSemana = numeroSemana;
        this.anio = anio;
        this.fechaCreacion = fechaCreacion;
        this.activo = activo;
        this.secciones = new ArrayList<>();
    }

    // Constructor desde JSON (compatible con backend)
    // Constructor desde JSON (compatible con backend)
    // Constructor desde JSON (compatible con backend)
    public Menu(JSONObject json) {
        try {
            this.id = json.optInt("ID", 0);
            this.fecha = json.optString("Fecha", LocalDate.now().toString());
            this.diaSemana = json.optString("DiaSemana", "");
            this.horario = json.optString("Horario", "");
            this.numeroSemana = json.optInt("NumeroSemana", 0);
            this.anio = json.optInt("Anio", 0);

            // CORRECCIÓN: Manejar tanto string como integer para Activo
            if (json.has("Activo")) {
                Object activoObj = json.get("Activo");
                if (activoObj instanceof Integer) {
                    this.activo = ((Integer) activoObj) == 1;
                } else if (activoObj instanceof String) {
                    String activoStr = (String) activoObj;
                    this.activo = activoStr.equals("1") || activoStr.equalsIgnoreCase("true");
                } else if (activoObj instanceof Boolean) {
                    this.activo = (Boolean) activoObj;
                } else {
                    this.activo = true;
                }
            } else {
                this.activo = true;
            }

            this.fechaCreacion = json.optString("FechaCreacion", LocalDate.now().toString());

            // Cargar secciones
            this.secciones = new ArrayList<>();
            if (json.has("Secciones")) {
                JSONArray seccionesArray = json.getJSONArray("Secciones");
                for (int i = 0; i < seccionesArray.length(); i++) {
                    try {
                        secciones.add(new MenuSeccion(seccionesArray.getJSONObject(i)));
                    } catch (Exception e) {
                        System.err.println("Error cargando sección " + i + ": " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error en constructor Menu(JSONObject): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Convertir a JSON para enviar al servidor
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();

        if (id > 0) {
            obj.put("ID", id);
        }

        obj.put("Fecha", fecha);
        obj.put("DiaSemana", diaSemana);
        obj.put("Horario", horario);
        obj.put("NumeroSemana", numeroSemana);
        obj.put("Anio", anio);
        obj.put("FechaCreacion", fechaCreacion);
        obj.put("Activo", activo);

        // Guardar secciones
        if (secciones != null && !secciones.isEmpty()) {
            JSONArray seccionesArray = new JSONArray();
            for (MenuSeccion seccion : secciones) {
                seccionesArray.put(seccion.toJson());
            }
            obj.put("Secciones", seccionesArray);
        }

        return obj;
    }

    // Convertir a JSON para formulario/UI
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("fecha", fecha);
        obj.put("diaSemana", diaSemana);
        obj.put("horario", horario);
        obj.put("numeroSemana", numeroSemana);
        obj.put("anio", anio);
        obj.put("activo", activo);
        return obj;
    }

    // Métodos para manejar secciones
    public void agregarSeccion(MenuSeccion seccion) {
        if (secciones == null) {
            secciones = new ArrayList<>();
        }
        secciones.add(seccion);
    }

    public void eliminarSeccion(int idSeccion) {
        if (secciones != null) {
            secciones.removeIf(s -> s.getIdSeccion() == idSeccion);
        }
    }

    public MenuSeccion getSeccion(int idSeccion) {
        if (secciones == null)
            return null;

        return secciones.stream()
                .filter(s -> s.getIdSeccion() == idSeccion)
                .findFirst()
                .orElse(null);
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public String getFecha() {
        return fecha;
    }

    public LocalDate getFechaAsLocalDate() {
        try {
            return LocalDate.parse(fecha);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public String getHorario() {
        return horario;
    }

    public int getNumeroSemana() {
        return numeroSemana;
    }

    public int getAnio() {
        return anio;
    }

    public String getFechaCreacion() {
        return fechaCreacion;
    }

    public boolean isActivo() {
        return activo;
    }

    public List<MenuSeccion> getSecciones() {
        if (secciones == null)
            return new ArrayList<>();
        return new ArrayList<>(secciones);
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha.toString();
    }

    public void setDiaSemana(String diaSemana) {
        this.diaSemana = diaSemana;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public void setNumeroSemana(int numeroSemana) {
        this.numeroSemana = numeroSemana;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public void setFechaCreacion(String fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setSecciones(List<MenuSeccion> secciones) {
        this.secciones = new ArrayList<>(secciones);
    }

    // Método estático para crear desde JSON
    public static Menu fromJSON(JSONObject json) {
        return new Menu(json);
    }

    @Override
    public String toString() {
        return fecha + " (" + diaSemana + ") - " + horario + " - Semana " + numeroSemana;
    }

    // Getters y setters
    public int getIdUsuarioCreador() {
        return idUsuarioCreador;
    }

    public void setIdUsuarioCreador(int idUsuarioCreador) {
        this.idUsuarioCreador = idUsuarioCreador;
    }

    public int getIdUsuarioModificador() {
        return idUsuarioModificador;
    }

    public void setIdUsuarioModificador(int idUsuarioModificador) {
        this.idUsuarioModificador = idUsuarioModificador;
    }

    public String getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(String fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}