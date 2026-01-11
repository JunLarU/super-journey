package core.data.Avisos;

import core.services.AvisosService;
import javafx.application.Platform;
import javafx.concurrent.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AllAvisos {
    private static AllAvisos instance;
    private final List<Aviso> avisos = new ArrayList<>();
    private final AvisosService service;

    // Callback interface
    public interface AvisosCallback {
        void onSuccess(List<Aviso> avisos);
        void onError(String error);
    }

    private AllAvisos() {
        this.service = AvisosService.getInstance();
    }

    public static AllAvisos getInstance() {
        if (instance == null) {
            instance = new AllAvisos();
        }
        return instance;
    }

    /**
     * Obtener todos los avisos (sincrónico - para compatibilidad)
     */
    public List<Aviso> getAll() {
        return new ArrayList<>(avisos);
    }

    /**
     * Obtener todos los avisos (asincrónico con callback)
     */
    public void getAllAsync(AvisosCallback callback) {
        Task<List<Aviso>> task = service.getAll();

        task.setOnSucceeded(event -> {
            try {
                List<Aviso> resultado = task.getValue();
                avisos.clear();
                avisos.addAll(resultado);
                callback.onSuccess(new ArrayList<>(avisos));
            } catch (Exception e) {
                callback.onError("Error procesando datos: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            callback.onError("Error al cargar: " + (ex != null ? ex.getMessage() : "Error desconocido"));
        });

        new Thread(task).start();
    }

    /**
     * Métodos de compatibilidad
     */
    public void addAviso(Aviso aviso) {
        avisos.add(aviso);
    }

    public void updateAviso(Aviso nuevo) {
        for (int i = 0; i < avisos.size(); i++) {
            if (avisos.get(i).getId() == nuevo.getId()) {
                avisos.set(i, nuevo);
                return;
            }
        }
        // Si no existe, agregarlo
        avisos.add(nuevo);
    }

    public void removeAviso(int id) {
        avisos.removeIf(a -> a.getId() == id);
    }

    public Aviso getById(int id) {
        return avisos.stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     * Filtrar avisos vigentes (activos y dentro del rango de fechas)
     */
    public List<Aviso> getAvisosVigentes() {
        LocalDateTime ahora = LocalDateTime.now();
        return avisos.stream()
                .filter(Aviso::isActivo)
                .filter(a -> a.getFechaInicio().isBefore(ahora) && a.getFechaFin().isAfter(ahora))
                .toList();
    }

    /**
     * Filtrar avisos por establecimiento
     */
    public List<Aviso> getByEstablecimiento(Aviso.Establecimiento establecimiento) {
        return avisos.stream()
                .filter(a -> a.getEstablecimiento() == establecimiento || 
                           a.getEstablecimiento() == Aviso.Establecimiento.Ambos)
                .toList();
    }

    /**
     * Filtrar avisos por tipo
     */
    public List<Aviso> getByTipo(Aviso.TipoAviso tipo) {
        return avisos.stream()
                .filter(a -> a.getTipoAviso() == tipo)
                .toList();
    }

    /**
     * Métodos para operaciones con servidor
     */
    public Task<Boolean> guardarEnServidor(Aviso aviso) {
        if (aviso.getId() == 0) {
            return service.crear(aviso);
        } else {
            return service.actualizar(aviso);
        }
    }

    public void eliminarDelServidor(int id, Runnable onSuccess, Consumer<String> onError) {
        Task<Boolean> task = service.eliminar(id);

        task.setOnSucceeded(event -> {
            if (task.getValue()) {
                removeAviso(id);
                Platform.runLater(onSuccess);
            } else {
                Platform.runLater(() -> onError.accept("No se pudo eliminar del servidor"));
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            Platform.runLater(() -> onError.accept("Error: " +
                    (ex != null ? ex.getMessage() : "Error desconocido")));
        });

        new Thread(task).start();
    }

    /**
     * Método para cargar desde servidor y actualizar cache
     */
    public void cargarDesdeServidor() {
        getAllAsync(new AvisosCallback() {
            @Override
            public void onSuccess(List<Aviso> listaAvisos) {
                System.out.println("Cargados " + listaAvisos.size() + " avisos desde servidor");
            }

            @Override
            public void onError(String error) {
                System.err.println("Error al cargar avisos desde servidor: " + error);
            }
        });
    }
}