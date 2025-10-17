// AllAvisos.java
package core.data.Avisos;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Singleton para gestionar todos los avisos
 */
public class AllAvisos {
    private static AllAvisos instance;
    private final List<Aviso> avisos = new ArrayList<>();
    private final String FILE_PATH = "data/avisos.json";
    private int nextId = 1;

    private AllAvisos() {
        loadFromFile();
    }

    public static AllAvisos getInstance() {
        if (instance == null) {
            instance = new AllAvisos();
        }
        return instance;
    }

    public void addAviso(Aviso aviso) {
        if (aviso.getId() == 0) {
            aviso.setId(nextId++);
        }
        avisos.add(aviso);
        saveToFile();
    }

    public void updateAviso(Aviso nuevo) {
        for (int idx = 0; idx < avisos.size(); idx++) {
            if (avisos.get(idx).getId() == nuevo.getId()) {
                avisos.set(idx, nuevo);
                saveToFile();
                return;
            }
        }
    }

    public void removeAviso(int id) {
        avisos.removeIf(aviso -> aviso.getId() == id);
        saveToFile();
    }

    public Aviso getById(int id) {
        return avisos.stream()
                .filter(aviso -> aviso.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public List<Aviso> getByEstablecimiento(Aviso.Establecimiento establecimiento) {
        return avisos.stream()
                .filter(aviso -> aviso.getEstablecimiento() == establecimiento || 
                                aviso.getEstablecimiento() == Aviso.Establecimiento.Ambos)
                .collect(Collectors.toList());
    }

    public List<Aviso> getByTipo(Aviso.TipoAviso tipoAviso) {
        return avisos.stream()
                .filter(aviso -> aviso.getTipoAviso() == tipoAviso)
                .collect(Collectors.toList());
    }

    public List<Aviso> getAvisosParaFecha(LocalDateTime fechaHora) {
        return avisos.stream()
                .filter(aviso -> aviso.estaActivoParaFechaHora(fechaHora))
                .collect(Collectors.toList());
    }

    public List<Aviso> getAvisosVigentes() {
        return getAvisosParaFecha(LocalDateTime.now());
    }

    public List<Aviso> getAvisosActivos() {
        return avisos.stream()
                .filter(Aviso::isActivo)
                .collect(Collectors.toList());
    }

    public List<Aviso> getAvisosImportantes() {
        return avisos.stream()
                .filter(aviso -> aviso.getPrioridad() == Aviso.Prioridad.Importante && aviso.isActivo())
                .collect(Collectors.toList());
    }

    public List<Aviso> getAll() {
        return new ArrayList<>(avisos);
    }

    // Método para obtener avisos vigentes por establecimiento
    public List<Aviso> getAvisosVigentesPorEstablecimiento(Aviso.Establecimiento establecimiento) {
        return avisos.stream()
                .filter(aviso -> aviso.estaVigente() && 
                                (aviso.getEstablecimiento() == establecimiento || 
                                 aviso.getEstablecimiento() == Aviso.Establecimiento.Ambos))
                .collect(Collectors.toList());
    }

    private void loadFromFile() {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                saveToFile(); // crea archivo vacío
                return;
            }

            String content = Files.readString(file.toPath());
            if (content.isBlank()) return;

            JSONArray array = new JSONArray(content);
            avisos.clear();

            int maxId = 0;
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                Aviso aviso = new Aviso(obj);
                avisos.add(aviso);
                if (aviso.getId() > maxId) {
                    maxId = aviso.getId();
                }
            }
            nextId = maxId + 1;

        } catch (Exception e) {
            System.err.println("Error cargando avisos desde archivo:");
            e.printStackTrace();
        }
    }

    public void saveToFile() {
        try {
            JSONArray array = new JSONArray();
            for (Aviso aviso : avisos) {
                array.put(aviso.toJson());
            }

            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();

            FileWriter writer = new FileWriter(file);
            writer.write(array.toString(4)); // JSON legible
            writer.close();

        } catch (Exception e) {
            System.err.println("Error guardando avisos en archivo:");
            e.printStackTrace();
        }
    }

    // Método para obtener estadísticas
    public String getEstadisticas() {
        int total = avisos.size();
        int activos = (int) avisos.stream().filter(Aviso::isActivo).count();
        int vigentes = getAvisosVigentes().size();
        int importantes = getAvisosImportantes().size();
        
        return String.format("Total: %d avisos | Activos: %d | Vigentes: %d | Importantes: %d", 
                           total, activos, vigentes, importantes);
    }

    // Método para limpiar avisos expirados (más de 30 días)
    public void limpiarExpirados() {
        LocalDateTime limite = LocalDateTime.now().minusDays(30);
        int removidos = (int) avisos.stream()
                .filter(aviso -> aviso.getFechaFin().isBefore(limite))
                .count();
        
        avisos.removeIf(aviso -> aviso.getFechaFin().isBefore(limite));
        
        if (removidos > 0) {
            saveToFile();
            System.out.println("Se removieron " + removidos + " avisos expirados");
        }
    }

    // Método para obtener avisos por rango de fechas
    public List<Aviso> getAvisosPorRango(LocalDateTime inicio, LocalDateTime fin) {
        return avisos.stream()
                .filter(aviso -> !aviso.getFechaInicio().isAfter(fin) && 
                                !aviso.getFechaFin().isBefore(inicio))
                .collect(Collectors.toList());
    }
}