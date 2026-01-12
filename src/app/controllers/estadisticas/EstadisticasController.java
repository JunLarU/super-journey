package app.controllers.estadisticas;

import core.SessionManager;
import core.services.EstadisticasService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EstadisticasController {
    @FXML
    private Label lblFechaActual;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private VBox contentBox;
    @FXML
    private TabPane tabPane;

    // Referencias a las pestañas
    @FXML
    private Tab tabSemana;
    @FXML
    private Tab tabMes;
    @FXML
    private Tab tabSeisMeses;

    // VBox para el contenido de cada tab
    @FXML
    private VBox hoyContent;
    @FXML
    private VBox semanaContent;
    @FXML
    private VBox mesContent;
    @FXML
    private VBox seisMesesContent;

    // Controladores
    private EstadisticasHoyController hoyController;
    private EstadisticasSemanaController semanaController;
    private EstadisticasMesController mesController;
    private EstadisticasSeisMesesController seisMesesController;

    private final EstadisticasService service = EstadisticasService.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private String periodoActual = "semana";

    @FXML
    public void initialize() {
        System.out.println("Controlador de estadísticas inicializado");
        
        // Verificar que todos los componentes FXML se inyectaron correctamente
        System.out.println("\n=== VERIFICACIÓN DE INYECCIÓN FXML ===");
        System.out.println("Labels y controles principales:");
        System.out.println("  - lblFechaActual: " + (lblFechaActual != null));
        System.out.println("  - progressIndicator: " + (progressIndicator != null));
        System.out.println("  - contentBox: " + (contentBox != null));
        System.out.println("  - tabPane: " + (tabPane != null));
        
        System.out.println("\nTabs:");
        System.out.println("  - tabSemana: " + (tabSemana != null));
        System.out.println("  - tabMes: " + (tabMes != null));
        System.out.println("  - tabSeisMeses: " + (tabSeisMeses != null));
        
        System.out.println("\nVBox para contenido:");
        System.out.println("  - hoyContent: " + (hoyContent != null));
        System.out.println("  - semanaContent: " + (semanaContent != null));
        System.out.println("  - mesContent: " + (mesContent != null));
        System.out.println("  - seisMesesContent: " + (seisMesesContent != null));

        if (!session.isAdmin()) {
            mostrarError("Acceso denegado. Solo administradores pueden ver estadísticas.");
            return;
        }

        // Cargar los FXMLs manualmente
        cargarControladores();

        // Configurar listener para cambios de pestaña
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                if (newTab == tabSemana) {
                    periodoActual = "semana";
                } else if (newTab == tabMes) {
                    periodoActual = "mes";
                } else if (newTab == tabSeisMeses) {
                    periodoActual = "seis_meses";
                }

                System.out.println("Cambiando a pestaña: " + periodoActual);
                cargarEstadisticasPeriodo(periodoActual);
            }
        });

        // Cargar estadísticas iniciales
        cargarEstadisticasPeriodo(periodoActual);
    }

    private void cargarEstadisticasPeriodo(String periodo) {
        System.out.println("\n=== Cargando estadísticas para período: " + periodo + " ===");
        
        // Verificar controlador correspondiente
        boolean controladorOk = false;
        switch (periodo) {
            case "hoy":
                controladorOk = (hoyController != null);
                if (!controladorOk) {
                    System.err.println("ERROR: Controlador Hoy es null");
                }
                break;
            case "semana":
                controladorOk = (semanaController != null);
                if (!controladorOk) {
                    System.err.println("ERROR: Controlador Semana es null");
                }
                break;
            case "mes":
                controladorOk = (mesController != null);
                if (!controladorOk) {
                    System.err.println("ERROR: Controlador Mes es null");
                }
                break;
            case "seis_meses":
                controladorOk = (seisMesesController != null);
                if (!controladorOk) {
                    System.err.println("ERROR: Controlador 6 Meses es null");
                }
                break;
        }
        
        if (!controladorOk) {
            System.err.println("No se cargarán estadísticas porque el controlador es null");
            mostrarError("Error interno: La interfaz no se cargó correctamente");
            return;
        }

        // Mostrar loading
        progressIndicator.setVisible(true);
        contentBox.setVisible(false);

        Task<JSONObject> task = service.getEstadisticasPeriodo(periodo);

        task.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                try {
                    JSONObject est = task.getValue();
                    if (est != null && est.has("success") && est.getBoolean("success")) {
                        System.out.println("Estadísticas recibidas correctamente para " + periodo);
                        mostrar(est, periodo);
                    } else {
                        mostrarError("Error en la respuesta del servidor: " +
                                (est != null ? est.toString() : "Respuesta vacía"));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    mostrarError("Error al procesar estadísticas: " + ex.getMessage());
                } finally {
                    progressIndicator.setVisible(false);
                    contentBox.setVisible(true);
                }
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                progressIndicator.setVisible(false);
                contentBox.setVisible(true);
                mostrarError("No se pudieron cargar las estadísticas: " +
                        task.getException().getMessage());
            });
        });

        new Thread(task).start();
    }

    private void cargarControladores() {
        System.out.println("\n=== CARGANDO CONTROLADORES ===");
        
        try {
            // Cargar controlador para Hoy
            System.out.println("Cargando controlador Hoy...");
            FXMLLoader hoyLoader = new FXMLLoader(getClass().getResource("/app/views/estadisticas/EstadisticasHoy.fxml"));
            Parent hoyRoot = hoyLoader.load();
            hoyController = hoyLoader.getController();
            if (hoyContent != null) {
                hoyContent.getChildren().setAll(hoyRoot);
                System.out.println("✓ Controlador Hoy cargado y contenido establecido");
            } else {
                System.err.println("✗ hoyContent es null - no se puede establecer contenido");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error cargando controlador Hoy: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            // Cargar controlador para Semana
            System.out.println("\nCargando controlador Semana...");
            FXMLLoader semanaLoader = new FXMLLoader(getClass().getResource("/app/views/estadisticas/EstadisticasSemana.fxml"));
            Parent semanaRoot = semanaLoader.load();
            semanaController = semanaLoader.getController();
            if (semanaContent != null) {
                semanaContent.getChildren().setAll(semanaRoot);
                System.out.println("✓ Controlador Semana cargado y contenido establecido");
            } else {
                System.err.println("✗ semanaContent es null - no se puede establecer contenido");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error cargando controlador Semana: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            // Cargar controlador para Mes
            System.out.println("\nCargando controlador Mes...");
            FXMLLoader mesLoader = new FXMLLoader(getClass().getResource("/app/views/estadisticas/EstadisticasMes.fxml"));
            Parent mesRoot = mesLoader.load();
            mesController = mesLoader.getController();
            if (mesContent != null) {
                mesContent.getChildren().setAll(mesRoot);
                System.out.println("✓ Controlador Mes cargado y contenido establecido");
            } else {
                System.err.println("✗ mesContent es null - no se puede establecer contenido");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error cargando controlador Mes: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            // Cargar controlador para 6 Meses
            System.out.println("\nCargando controlador 6 Meses...");
            FXMLLoader seisMesesLoader = new FXMLLoader(getClass().getResource("/app/views/estadisticas/EstadisticasSeisMeses.fxml"));
            Parent seisMesesRoot = seisMesesLoader.load();
            seisMesesController = seisMesesLoader.getController();
            if (seisMesesContent != null) {
                seisMesesContent.getChildren().setAll(seisMesesRoot);
                System.out.println("✓ Controlador 6 Meses cargado y contenido establecido");
            } else {
                System.err.println("✗ seisMesesContent es null - no se puede establecer contenido");
            }
            
        } catch (Exception e) {
            System.err.println("✗ Error cargando controlador 6 Meses: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== RESUMEN CONTROLADORES CARGADOS ===");
        System.out.println("  - Hoy: " + (hoyController != null));
        System.out.println("  - Semana: " + (semanaController != null));
        System.out.println("  - Mes: " + (mesController != null));
        System.out.println("  - 6 Meses: " + (seisMesesController != null));
    }

    private void mostrar(JSONObject est, String periodo) {
        // Actualizar fecha
        lblFechaActual.setText("Última actualización: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        if (!est.has("estadisticas")) {
            System.err.println("JSON no tiene clave 'estadisticas': " + est.toString());
            mostrarError("Datos de estadísticas incompletos");
            return;
        }

        JSONObject periodoData = est.getJSONObject("estadisticas");
        System.out.println("Datos recibidos para " + periodo + ": " + periodoData.toString(2));

        // Pasar datos al controlador correspondiente
        try {
            switch (periodo) {
                case "hoy":
                    if (hoyController != null) {
                        hoyController.actualizarDatos(periodoData);
                        System.out.println("✓ Datos enviados a controlador Hoy");
                    } else {
                        System.err.println("✗ Controlador de Hoy es null");
                    }
                    break;
                case "semana":
                    if (semanaController != null) {
                        semanaController.actualizarDatos(periodoData);
                        System.out.println("✓ Datos enviados a controlador Semana");
                    } else {
                        System.err.println("✗ Controlador de Semana es null");
                    }
                    break;
                case "mes":
                    if (mesController != null) {
                        mesController.actualizarDatos(periodoData);
                        System.out.println("✓ Datos enviados a controlador Mes");
                    } else {
                        System.err.println("✗ Controlador de Mes es null");
                    }
                    break;
                case "seis_meses":
                    if (seisMesesController != null) {
                        seisMesesController.actualizarDatos(periodoData);
                        System.out.println("✓ Datos enviados a controlador 6 Meses");
                    } else {
                        System.err.println("✗ Controlador de 6 Meses es null");
                    }
                    break;
            }
        } catch (Exception e) {
            System.err.println("Error al actualizar controladores: " + e.getMessage());
            e.printStackTrace();
            mostrarError("Error al mostrar estadísticas: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error en estadísticas");
            alert.setContentText(mensaje);
            alert.show();
        });
    }

    @FXML
    private void onRecargarClicked() {
        System.out.println("Recargando estadísticas para: " + periodoActual);
        cargarEstadisticasPeriodo(periodoActual);
    }
}