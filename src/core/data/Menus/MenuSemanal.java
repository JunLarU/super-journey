package core.data.Menus;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Representa un menú semanal completo (Lunes a Viernes, Desayuno y Comida)
 */
public class MenuSemanal {
    private int semana;
    private int anio;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Map<String, Menu> menusPorDiaHorario; // Clave: "Lunes-Desayuno", "Martes-Comida", etc.
    private List<SeccionMenu> seccionesDisponibles;
    private String fechaGeneracion;
    private int idUsuarioGenerador;

    public MenuSemanal(int semana, int anio) {
        this.semana = semana;
        this.anio = anio;
        this.menusPorDiaHorario = new HashMap<>();
        this.seccionesDisponibles = new ArrayList<>();
        this.fechaGeneracion = LocalDate.now().toString();
        calcularFechas();
    }

    public MenuSemanal(int semana, int anio, LocalDate fechaInicio) {
        this.semana = semana;
        this.anio = anio;
        this.fechaInicio = fechaInicio;
        this.menusPorDiaHorario = new HashMap<>();
        this.seccionesDisponibles = new ArrayList<>();
        this.fechaGeneracion = LocalDate.now().toString();
        calcularFechasDesdeInicio();
    }

    // En MenuSemanal.java, agrega este constructor
    public MenuSemanal() {
        this.menusPorDiaHorario = new HashMap<>();
        this.seccionesDisponibles = new ArrayList<>();
        this.fechaGeneracion = LocalDate.now().toString();
    }

    // Y este método setter

    private void calcularFechas() {
        // Calcular fecha de inicio (lunes) de la semana ISO
        LocalDate fecha = LocalDate.now()
                .withYear(anio)
                .with(java.time.temporal.WeekFields.ISO.weekOfYear(), semana)
                .with(java.time.temporal.WeekFields.ISO.dayOfWeek(), 1); // Lunes

        this.fechaInicio = fecha;
        this.fechaFin = fecha.plusDays(4); // Viernes
    }

    private void calcularFechasDesdeInicio() {
        this.fechaFin = fechaInicio.plusDays(4); // Lunes a Viernes
    }

    public void agregarMenu(Menu menu) {
        String clave = menu.getDiaSemana() + "-" + menu.getHorario();
        menusPorDiaHorario.put(clave, menu);
    }

    public Menu getMenu(String dia, String horario) {
        String clave = dia + "-" + horario;
        return menusPorDiaHorario.get(clave);
    }

    public List<Menu> getMenus() {
        return new ArrayList<>(menusPorDiaHorario.values());
    }

    public List<Menu> getMenusPorDia(String dia) {
        List<Menu> menusDia = new ArrayList<>();
        for (Map.Entry<String, Menu> entry : menusPorDiaHorario.entrySet()) {
            if (entry.getKey().startsWith(dia)) {
                menusDia.add(entry.getValue());
            }
        }
        return menusDia;
    }

    public List<Menu> getMenusPorHorario(String horario) {
        List<Menu> menusHorario = new ArrayList<>();
        for (Map.Entry<String, Menu> entry : menusPorDiaHorario.entrySet()) {
            if (entry.getKey().endsWith(horario)) {
                menusHorario.add(entry.getValue());
            }
        }
        return menusHorario;
    }

    public void agregarSeccionDisponible(SeccionMenu seccion) {
        if (!seccionesDisponibles.contains(seccion)) {
            seccionesDisponibles.add(seccion);
        }
    }

    public void asignarSeccionATodos(String dia, String horario, SeccionMenu seccion) {
        Menu menu = getMenu(dia, horario);
        if (menu != null) {
            MenuSeccion menuSeccion = new MenuSeccion();
            menuSeccion.setIdSeccion(seccion.getId());
            menuSeccion.setNombre(seccion.getNombre());
            menuSeccion.setColor(seccion.getColor());
            menu.agregarSeccion(menuSeccion);
        }
    }

    public boolean estaCompleto() {
        // Verificar que tengamos todos los slots (5 días × 2 horarios = 10)
        return menusPorDiaHorario.size() == 10;
    }

    // Getters y Setters
    public int getSemana() {
        return semana;
    }

    public int getAnio() {
        return anio;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public String getFechaGeneracion() {
        return fechaGeneracion;
    }

    public int getIdUsuarioGenerador() {
        return idUsuarioGenerador;
    }

    public List<SeccionMenu> getSeccionesDisponibles() {
        return new ArrayList<>(seccionesDisponibles);
    }

    public void setSemana(int semana) {
        this.semana = semana;
    }

    public void setAnio(int anio) {
        this.anio = anio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public void setFechaGeneracion(String fechaGeneracion) {
        this.fechaGeneracion = fechaGeneracion;
    }

    public void setIdUsuarioGenerador(int idUsuarioGenerador) {
        this.idUsuarioGenerador = idUsuarioGenerador;
    }

    public void setSeccionesDisponibles(List<SeccionMenu> seccionesDisponibles) {
        this.seccionesDisponibles = new ArrayList<>(seccionesDisponibles);
    }

    @Override
    public String toString() {
        return "Menú Semana " + semana + "/" + anio +
                " (" + fechaInicio.format(DateTimeFormatter.ISO_LOCAL_DATE) +
                " al " + fechaFin.format(DateTimeFormatter.ISO_LOCAL_DATE) + ") - " +
                menusPorDiaHorario.size() + " slots";
    }
}