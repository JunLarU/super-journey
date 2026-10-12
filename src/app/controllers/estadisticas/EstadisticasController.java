package app.controllers.estadisticas;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.JSONObject;

import core.SessionManager;
import core.services.EstadisticasService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;

public class EstadisticasController {

    @FXML private Label lblFechaActual;
    @FXML private Label lblPeriodoActual;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox contentBox;
    @FXML private TabPane tabPane;

    @FXML private Tab tabHoy;
    @FXML private Tab tabSemana;
    @FXML private Tab tabMes;
    @FXML private Tab tabSeisMeses;

    @FXML private VBox hoyContent;
    @FXML private VBox semanaContent;
    @FXML private VBox mesContent;
    @FXML private VBox seisMesesContent;

    private EstadisticasHoyController hoyController;
    private EstadisticasSemanaController semanaController;
    private EstadisticasMesController mesController;
    private EstadisticasSeisMesesController seisMesesController;

    private final EstadisticasService service = EstadisticasService.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private String periodoActual = "hoy";

    @FXML
    public void initialize() {
        if (!session.isAdmin()) {
            mostrarError(
                    "Acceso denegado. Solo administradores pueden ver estadísticas."
            );
            return;
        }

        cargarControladores();

        tabPane.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldTab, newTab) -> {
                    if (newTab == null) return;

                    if (newTab == tabHoy) {
                        periodoActual = "hoy";
                    } else if (newTab == tabSemana) {
                        periodoActual = "semana";
                    } else if (newTab == tabMes) {
                        periodoActual = "mes";
                    } else if (newTab == tabSeisMeses) {
                        periodoActual = "seis_meses";
                    }

                    cargarEstadisticasPeriodo(periodoActual);
                });

        tabPane.getSelectionModel().select(tabHoy);
        cargarEstadisticasPeriodo(periodoActual);
    }

    private void cargarControladores() {
        hoyController = cargarVista(
                "/app/views/estadisticas/EstadisticasHoy.fxml",
                hoyContent,
                EstadisticasHoyController.class
        );

        semanaController = cargarVista(
                "/app/views/estadisticas/EstadisticasSemana.fxml",
                semanaContent,
                EstadisticasSemanaController.class
        );

        mesController = cargarVista(
                "/app/views/estadisticas/EstadisticasMes.fxml",
                mesContent,
                EstadisticasMesController.class
        );

        seisMesesController = cargarVista(
                "/app/views/estadisticas/EstadisticasSeisMeses.fxml",
                seisMesesContent,
                EstadisticasSeisMesesController.class
        );
    }

    private <T> T cargarVista(
            String ruta,
            VBox contenedor,
            Class<T> controllerClass
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            Parent root = loader.load();

            contenedor.getChildren().setAll(root);

            Object controller = loader.getController();
            return controllerClass.cast(controller);

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError(
                    "No se pudo cargar "
                            + ruta
                            + "\n"
                            + e.getMessage()
            );
            return null;
        }
    }

    private void cargarEstadisticasPeriodo(String periodo) {
        if (!controladorDisponible(periodo)) {
            mostrarError(
                    "No se pudo inicializar el panel del período seleccionado."
            );
            return;
        }

        mostrarCargando(true);
        lblPeriodoActual.setText(textoPeriodo(periodo));

        Task<JSONObject> task =
                service.getEstadisticasPeriodo(periodo);

        task.setOnSucceeded(e ->
                Platform.runLater(() -> {
                    try {
                        JSONObject respuesta = task.getValue();

                        if (
                            respuesta == null
                            || !respuesta.optBoolean("success", false)
                        ) {
                            mostrarError(
                                    respuesta == null
                                            ? "Respuesta vacía del servidor."
                                            : respuesta.optString(
                                                    "error",
                                                    "No se pudieron cargar las estadísticas."
                                            )
                            );
                            return;
                        }

                        JSONObject data =
                                respuesta.optJSONObject("estadisticas");

                        if (data == null) {
                            mostrarError(
                                    "El servidor no devolvió el bloque 'estadisticas'."
                            );
                            return;
                        }

                        mostrar(data, periodo);

                        lblFechaActual.setText(
                                "Actualizado "
                                        + LocalDateTime.now().format(
                                                DateTimeFormatter.ofPattern(
                                                        "dd/MM/yyyy HH:mm"
                                                )
                                        )
                        );

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        mostrarError(
                                "Error al procesar estadísticas: "
                                        + ex.getMessage()
                        );
                    } finally {
                        mostrarCargando(false);
                    }
                })
        );

        task.setOnFailed(e ->
                Platform.runLater(() -> {
                    mostrarCargando(false);

                    String message =
                            task.getException() == null
                                    ? "Error de comunicación con el servidor."
                                    : task.getException().getMessage();

                    mostrarError(
                            "No se pudieron cargar las estadísticas: "
                                    + message
                    );
                })
        );

        Thread thread =
                new Thread(
                        task,
                        "cafi-estadisticas-" + periodo
                );

        thread.setDaemon(true);
        thread.start();
    }

    private boolean controladorDisponible(String periodo) {
        return switch (periodo) {
            case "hoy" -> hoyController != null;
            case "semana" -> semanaController != null;
            case "mes" -> mesController != null;
            case "seis_meses" -> seisMesesController != null;
            default -> false;
        };
    }

    private void mostrar(JSONObject data, String periodo) {
        switch (periodo) {
            case "hoy" -> hoyController.actualizarDatos(data);
            case "semana" -> semanaController.actualizarDatos(data);
            case "mes" -> mesController.actualizarDatos(data);
            case "seis_meses" -> seisMesesController.actualizarDatos(data);
        }
    }

    private void mostrarCargando(boolean cargando) {
        progressIndicator.setVisible(cargando);
        progressIndicator.setManaged(cargando);
        contentBox.setVisible(cargando);
        contentBox.setManaged(cargando);
    }

    private String textoPeriodo(String periodo) {
        return switch (periodo) {
            case "hoy" -> "Hoy";
            case "semana" -> "Últimos 7 días";
            case "mes" -> "Mes actual";
            case "seis_meses" -> "Últimos 6 meses";
            default -> "";
        };
    }

    @FXML
    private void onRecargarClicked() {
        cargarEstadisticasPeriodo(periodoActual);
    }

    private void mostrarError(String mensaje) {
        Platform.runLater(() -> {
            Alert alert =
                    new Alert(Alert.AlertType.ERROR);

            alert.setTitle("Estadísticas");
            alert.setHeaderText(
                    "No se pudieron mostrar las estadísticas"
            );
            alert.setContentText(mensaje);
            alert.show();
        });
    }
}
