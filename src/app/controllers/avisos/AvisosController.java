package app.controllers.avisos;
import core.SessionManager;
import core.data.Avisos.AllAvisos;
import core.data.Avisos.Aviso;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador para la gestión de avisos
 */
public class AvisosController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRecargar, btnNuevo;
    @FXML private TableView<Aviso> tablaAvisos;
    @FXML private TableColumn<Aviso, String> colID, colTitulo, colEstablecimiento, colTipo, colPrioridad;
    @FXML private TableColumn<Aviso, String> colFechas, colEstado;
    @FXML private TableColumn<Aviso, Void> colAcciones;
    @FXML private Label lblEstado;

    private final AllAvisos allAvisos = AllAvisos.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        // Verificar permisos de administrador
        if (!session.isAdmin()) {
            mostrarError("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        configurarTabla();
        cargarAvisos();
        
        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank())
                cargarAvisos();
            else
                buscarAvisos(n);
        });
    }

    private void configurarTabla() {
        // Configurar columnas
        colID.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        
        colEstablecimiento.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getEstablecimiento().name()));
        
        colTipo.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getTipoAviso().name()));
        
        colPrioridad.setCellValueFactory(data -> {
            Aviso.Prioridad prioridad = data.getValue().getPrioridad();
            String texto = prioridad == Aviso.Prioridad.Importante ? "⭐ Importante" : "Normal";
            return new javafx.beans.property.SimpleStringProperty(texto);
        });
        
        colFechas.setCellValueFactory(data -> {
            Aviso aviso = data.getValue();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            String fechaInicio = aviso.getFechaInicio().format(dateFormatter);
            String fechaFin = aviso.getFechaFin().format(dateFormatter);
            String horaInicio = aviso.getFechaInicio().format(timeFormatter);
            String horaFin = aviso.getFechaFin().format(timeFormatter);
            
            return new javafx.beans.property.SimpleStringProperty(
                fechaInicio + " - " + fechaFin + "\n" + horaInicio + " - " + horaFin);
        });
        
        colEstado.setCellValueFactory(data -> {
            Aviso aviso = data.getValue();
            String estado;
            if (!aviso.isActivo()) {
                estado = "❌ Inactivo";
            } else if (aviso.estaVigente()) {
                estado = "✅ Vigente";
            } else if (LocalDateTime.now().isBefore(aviso.getFechaInicio())) {
                estado = "Próximo";
            } else {
                estado = "📅 Expirado";
            }
            return new javafx.beans.property.SimpleStringProperty(estado);
        });

        // Configurar columna de acciones con botones
        colAcciones.setReorderable(false);
        colAcciones.setResizable(false);
        colAcciones.setSortable(false);
        colAcciones.setMinWidth(310);
        colAcciones.setCellFactory(
            (Callback<TableColumn<Aviso, Void>, TableCell<Aviso, Void>>) param -> new TableCell<>() {
                private final Button btnVer = new Button("Ver");
                private final Button btnEditar = new Button("Editar");
                private final Button btnEliminar = new Button("Eliminar");

                {
                    btnVer.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    btnVer.setMinWidth(100);
                    btnEditar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    btnEditar.setMinWidth(100);
                    btnEliminar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                    btnEliminar.setMinWidth(100);
                    // Estilos de botones
                    btnVer.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnEditar.setStyle("-fx-background-color: #f1c40f; -fx-cursor: hand; -fx-background-radius: 5;");
                    btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                    // Tooltips
                    btnVer.setTooltip(new Tooltip("Ver detalles"));
                    btnEditar.setTooltip(new Tooltip("Editar aviso"));
                    btnEliminar.setTooltip(new Tooltip("Eliminar aviso"));

                    // Acciones
                    btnVer.setOnAction(e -> {
                        Aviso aviso = getTableView().getItems().get(getIndex());
                        abrirFormularioSoloLectura(aviso);
                    });
                    
                    btnEditar.setOnAction(e -> {
                        Aviso aviso = getTableView().getItems().get(getIndex());
                        abrirFormulario(aviso);
                    });
                    
                    btnEliminar.setOnAction(e -> {
                        Aviso aviso = getTableView().getItems().get(getIndex());
                        eliminarAviso(aviso);
                    });
                }

                private final HBox pane = new HBox(5, btnVer, btnEditar, btnEliminar);

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : pane);
                }
            });
    }

    @FXML
    private void cargarAvisos() {
        lblEstado.setText("Cargando avisos...");
        tablaAvisos.getItems().clear();

        new Thread(() -> {
            try {
                List<Aviso> avisos = allAvisos.getAll();
                
                Platform.runLater(() -> {
                    tablaAvisos.getItems().addAll(avisos);
                    actualizarEstadisticas(avisos);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error al cargar avisos."));
            }
        }).start();
    }

    /**
     * Buscar avisos por título, contenido o establecimiento
     */
    private void buscarAvisos(String query) {
        lblEstado.setText("Buscando \"" + query + "\"...");
        tablaAvisos.getItems().clear();

        new Thread(() -> {
            try {
                List<Aviso> todos = allAvisos.getAll();
                String queryLower = query.toLowerCase();
                
                List<Aviso> resultados = todos.stream()
                    .filter(aviso -> 
                        aviso.getTitulo().toLowerCase().contains(queryLower) ||
                        aviso.getContenido().toLowerCase().contains(queryLower) ||
                        aviso.getEstablecimiento().name().toLowerCase().contains(queryLower) ||
                        aviso.getTipoAviso().name().toLowerCase().contains(queryLower)
                    )
                    .toList();

                Platform.runLater(() -> {
                    tablaAvisos.getItems().addAll(resultados);
                    lblEstado.setText("🔍 " + resultados.size() + " resultado(s) encontrado(s).");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error en búsqueda."));
            }
        }).start();
    }

    private void actualizarEstadisticas(List<Aviso> avisos) {
        int total = avisos.size();
        int vigentes = (int) avisos.stream().filter(Aviso::estaVigente).count();
        int activos = (int) avisos.stream().filter(Aviso::isActivo).count();
        int importantes = (int) avisos.stream()
            .filter(a -> a.getPrioridad() == Aviso.Prioridad.Importante && a.isActivo())
            .count();
        
        lblEstado.setText(String.format("📊 Total: %d | ✅ Vigentes: %d | 🔄 Activos: %d | ⭐ Importantes: %d", 
                                      total, vigentes, activos, importantes));
    }

    @FXML
    private void onRecargarClicked() {
        txtBuscar.clear();
        cargarAvisos();
    }

    @FXML
    private void onNuevoClicked() {
        abrirFormulario(null);
    }

    /**
     * Abrir formulario en modo solo lectura (visualización)
     */
    private void abrirFormularioSoloLectura(Aviso aviso) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/avisos/RegistroAviso.fxml"));
            Parent root = loader.load();
            
            RegistroAvisoController controller = loader.getController();
            controller.visualizarAviso(aviso);

            Stage stage = new Stage();
            stage.setTitle("Visualizar aviso: " + aviso.getTitulo());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
            
        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("❌ Error al abrir vista: " + e.getMessage());
        }
    }

    /**
     * Eliminar aviso
     */
    private void eliminarAviso(Aviso aviso) {
        String titulo = aviso.getTitulo();
        int id = aviso.getId();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar aviso");
        confirm.setHeaderText("¿Eliminar \"" + titulo + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        allAvisos.removeAviso(id);
                        
                        Platform.runLater(() -> {
                            lblEstado.setText("Eliminar Aviso eliminado correctamente.");
                            cargarAvisos();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> lblEstado.setText("❌ Error al eliminar."));
                    }
                }).start();
            }
        });
    }

    /**
     * Abrir formulario para editar aviso existente o crear uno nuevo
     */
    private void abrirFormulario(Aviso aviso) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/avisos/RegistroAviso.fxml"));
            Parent root = loader.load();
            
            RegistroAvisoController controller = loader.getController();
            
            if (aviso != null) {
                controller.cargarDatosExistentes(aviso);
            }

            Stage stage = new Stage();
            stage.setTitle(aviso == null ? "Nuevo Aviso" : "Editar Aviso: " + aviso.getTitulo());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Recargar cuando se cierre el formulario
            stage.setOnHidden(e -> cargarAvisos());
            
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("❌ Error al abrir formulario: " + e.getMessage());
        }
    }

    /**
     * Mostrar mensaje de error
     */
    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}