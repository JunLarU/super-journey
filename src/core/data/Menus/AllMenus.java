package core.data.Menus;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Singleton para gestionar todos los menús de la cafetería
 */
public class AllMenus {
    private static AllMenus instance;
    private final List<Menu> menus = new ArrayList<>();
    private final List<SeccionMenu> secciones = new ArrayList<>();
    private final String MENUS_FILE_PATH = "data/menus.json";
    private final String SECCIONES_FILE_PATH = "data/secciones_menu.json";
    private int nextMenuId = 1;
    private int nextSeccionId = 1;

    private AllMenus() {
        loadFromFiles();
    }

    public static AllMenus getInstance() {
        if (instance == null) {
            instance = new AllMenus();
        }
        return instance;
    }

    
    // MÉTODOS PARA MENÚS
    

    public void addMenu(Menu menu) {
        if (menu.getId() == 0) {
            menu.setId(nextMenuId++);
        }
        menus.add(menu);
        saveToFile();
    }

    public void updateMenu(Menu nuevo) {
        for (int idx = 0; idx < menus.size(); idx++) {
            if (menus.get(idx).getId() == nuevo.getId()) {
                menus.set(idx, nuevo);
                saveToFile();
                return;
            }
        }
    }

    public void removeMenu(int id) {
        menus.removeIf(m -> m.getId() == id);
        saveToFile();
    }

    public Menu getMenuById(int id) {
        return menus.stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Menu> getMenusByFecha(LocalDate fecha) {
        return menus.stream()
                .filter(m -> m.getFecha().equals(fecha))
                .collect(Collectors.toList());
    }

    public List<Menu> getMenusBySemana(int numeroSemana, int anio) {
        return menus.stream()
                .filter(m -> m.getNumeroSemana() == numeroSemana && m.getAnio() == anio)
                .collect(Collectors.toList());
    }

    public List<Menu> getMenusActivos() {
        return menus.stream()
                .filter(Menu::isActivo)
                .collect(Collectors.toList());
    }

    public List<Menu> getAllMenus() {
        return new ArrayList<>(menus);
    }

    public Menu getMenuByFechaYHorario(LocalDate fecha, String horario) {
        return menus.stream()
                .filter(m -> m.getFecha().equals(fecha) && m.getHorario().equalsIgnoreCase(horario))
                .findFirst()
                .orElse(null);
    }

    
    // MÉTODOS PARA SECCIONES
    

    public void addSeccion(SeccionMenu seccion) {
        if (seccion.getId() == 0) {
            seccion.setId(nextSeccionId++);
        }
        secciones.add(seccion);
        saveToFile();
    }

    public void updateSeccion(SeccionMenu nueva) {
        for (int idx = 0; idx < secciones.size(); idx++) {
            if (secciones.get(idx).getId() == nueva.getId()) {
                secciones.set(idx, nueva);
                saveToFile();
                return;
            }
        }
    }

    public void removeSeccion(int id) {
        secciones.removeIf(s -> s.getId() == id);
        saveToFile();
    }

    public SeccionMenu getSeccionById(int id) {
        return secciones.stream()
                .filter(s -> s.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public SeccionMenu getSeccionByNombre(String nombre) {
        return secciones.stream()
                .filter(s -> s.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .orElse(null);
    }

    public List<SeccionMenu> getSeccionesActivas() {
        return secciones.stream()
                .filter(SeccionMenu::isActivo)
                .collect(Collectors.toList());
    }

    public List<SeccionMenu> getAllSecciones() {
        return new ArrayList<>(secciones);
    }

    
    // MÉTODOS UTILITARIOS
    

    /**
     * Genera menús para una semana específica
     */
    public void generarMenusSemana(LocalDate fechaInicio, int idUsuarioCreador) {
        LocalDate fecha = fechaInicio;
        String fechaCreacion = LocalDate.now().toString();
        
        for (int i = 0; i < 7; i++) {
            // Menú de desayuno
            Menu menuDesayuno = new Menu(
                0,
                fecha,
                getDiaSemanaEspanol(fecha.getDayOfWeek()),
                "Desayuno",
                fecha.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()),
                fecha.getYear(),
                fechaCreacion,
                true,
                idUsuarioCreador,
                0,
                null
            );
            addMenu(menuDesayuno);

            // Menú de comida
            Menu menuComida = new Menu(
                0,
                fecha,
                getDiaSemanaEspanol(fecha.getDayOfWeek()),
                "Comida",
                fecha.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()),
                fecha.getYear(),
                fechaCreacion,
                true,
                idUsuarioCreador,
                0,
                null
            );
            addMenu(menuComida);

            fecha = fecha.plusDays(1);
        }
    }

    private String getDiaSemanaEspanol(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Lunes";
            case TUESDAY: return "Martes";
            case WEDNESDAY: return "Miércoles";
            case THURSDAY: return "Jueves";
            case FRIDAY: return "Viernes";
            case SATURDAY: return "Sábado";
            case SUNDAY: return "Domingo";
            default: return "";
        }
    }

    
    // PERSISTENCIA
    

    private void loadFromFiles() {
        loadMenusFromFile();
        loadSeccionesFromFile();
    }

    private void loadMenusFromFile() {
        try {
            File file = new File(MENUS_FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                saveMenusToFile();
                return;
            }

            String content = Files.readString(file.toPath());
            if (content.isBlank()) return;

            JSONArray array = new JSONArray(content);
            menus.clear();

            int maxId = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Menu menu = new Menu(obj);
                menus.add(menu);
                if (menu.getId() > maxId) {
                    maxId = menu.getId();
                }
            }
            nextMenuId = maxId + 1;

        } catch (Exception e) {
            //System.err.println("Error cargando menús desde archivo:");
            e.printStackTrace();
        }
    }

    private void loadSeccionesFromFile() {
        try {
            File file = new File(SECCIONES_FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                saveSeccionesToFile();
                return;
            }

            String content = Files.readString(file.toPath());
            if (content.isBlank()) return;

            JSONArray array = new JSONArray(content);
            secciones.clear();

            int maxId = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                SeccionMenu seccion = new SeccionMenu(obj);
                secciones.add(seccion);
                if (seccion.getId() > maxId) {
                    maxId = seccion.getId();
                }
            }
            nextSeccionId = maxId + 1;

        } catch (Exception e) {
            //System.err.println("Error cargando secciones desde archivo:");
            e.printStackTrace();
        }
    }

    private void saveMenusToFile() {
        try {
            JSONArray array = new JSONArray();
            for (Menu m : menus) {
                array.put(m.toJson());
            }

            File file = new File(MENUS_FILE_PATH);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(array.toString(4));
            writer.close();

        } catch (Exception e) {
            //System.err.println("Error guardando menús en archivo:");
            e.printStackTrace();
        }
    }

    private void saveSeccionesToFile() {
        try {
            JSONArray array = new JSONArray();
            for (SeccionMenu s : secciones) {
                array.put(s.toJson());
            }

            File file = new File(SECCIONES_FILE_PATH);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(array.toString(4));
            writer.close();

        } catch (Exception e) {
            //System.err.println("Error guardando secciones en archivo:");
            e.printStackTrace();
        }
    }

    /**
     * Método unificado para guardar ambos archivos JSON
     * Similar al saveToFile() de AllProductos
     */
    public void saveToFile() {
        saveMenusToFile();
        saveSeccionesToFile();
    }

    
    // ESTADÍSTICAS
    

    public String getEstadisticas() {
        int totalMenus = menus.size();
        int menusActivos = (int) menus.stream().filter(Menu::isActivo).count();
        int totalSecciones = secciones.size();
        int seccionesActivas = (int) secciones.stream().filter(SeccionMenu::isActivo).count();
        
        return String.format("Menús: %d total (%d activos) | Secciones: %d total (%d activas)", 
                           totalMenus, menusActivos, totalSecciones, seccionesActivas);
    }
}