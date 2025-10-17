package app.controllers.administradores;

import core.SessionManager;
import core.data.Users.AllUsers;
import core.data.Users.User;
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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para la gestión de administradores.
 */
public class AdministradoresController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRecargar, btnNuevo;
    @FXML private TableView<User> tablaAdministradores;
    @FXML private TableColumn<User, String> colExpediente, colNombre, colApellidos, colCorreo, colTelefono, colEstado;
    @FXML private TableColumn<User, Void> colAcciones;
    @FXML private Label lblEstado;

    private final AllUsers allUsers = AllUsers.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    public void initialize() {
        // Verificar permisos de administrador
        if (!sessionManager.isAdmin()) {
            mostrarError("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        configurarTabla();
        cargarAdministradores();
        
        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank())
                cargarAdministradores();
            else
                buscarAdministradores(n);
        });
    }

    private void configurarTabla() {
        // Configurar columnas
        colExpediente.setCellValueFactory(new PropertyValueFactory<>("clave"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("name"));
        
        colApellidos.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().getApellidoPaterno() + " " + 
                (data.getValue().getApellidoMaterno() != null ? data.getValue().getApellidoMaterno() : "")
            ));
        
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("phone"));
        
        colEstado.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isAdmin() ? "👑 Administrador" : "👤 Usuario"
            ));

        // Configurar columna de acciones con botones
        colAcciones.setCellFactory(
            (Callback<TableColumn<User, Void>, TableCell<User, Void>>) param -> new TableCell<>() {
                private final Button btnEditar = new Button("✏️");
                private final Button btnEliminar = new Button("🗑️");

                {
                    // Estilos de botones
                    btnEditar.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                    btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");

                    // Tooltips
                    btnEditar.setTooltip(new Tooltip("Editar administrador"));
                    btnEliminar.setTooltip(new Tooltip("Eliminar administrador"));

                    // Acciones
                    btnEditar.setOnAction(e -> {
                        User administrador = getTableView().getItems().get(getIndex());
                        editarAdministrador(administrador);
                    });
                    
                    btnEliminar.setOnAction(e -> {
                        User administrador = getTableView().getItems().get(getIndex());
                        eliminarAdministrador(administrador);
                    });
                }

                private final HBox pane = new HBox(5, btnEditar, btnEliminar);

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        User administrador = getTableView().getItems().get(getIndex());
                        
                        // No mostrar botones para el usuario actual
                        if (esUsuarioActual(administrador)) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                }
            });
    }

    @FXML
    private void cargarAdministradores() {
        lblEstado.setText("Cargando administradores...");
        tablaAdministradores.getItems().clear();

        new Thread(() -> {
            try {
                List<User> administradores = allUsers.getUsers().stream()
                    .filter(User::isAdmin)
                    .collect(Collectors.toList());
                
                Platform.runLater(() -> {
                    tablaAdministradores.getItems().addAll(administradores);
                    actualizarEstadisticas(administradores);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error al cargar administradores."));
            }
        }).start();
    }

    /**
     * Buscar administradores por nombre, apellidos, expediente o correo
     */
    private void buscarAdministradores(String query) {
        lblEstado.setText("Buscando \"" + query + "\"...");
        tablaAdministradores.getItems().clear();

        new Thread(() -> {
            try {
                List<User> todosAdmins = allUsers.getUsers().stream()
                    .filter(User::isAdmin)
                    .collect(Collectors.toList());
                
                String queryLower = query.toLowerCase();
                
                List<User> resultados = todosAdmins.stream()
                    .filter(admin -> 
                        admin.getClave().toLowerCase().contains(queryLower) ||
                        admin.getName().toLowerCase().contains(queryLower) ||
                        admin.getApellidoPaterno().toLowerCase().contains(queryLower) ||
                        (admin.getApellidoMaterno() != null && admin.getApellidoMaterno().toLowerCase().contains(queryLower)) ||
                        admin.getEmail().toLowerCase().contains(queryLower)
                    )
                    .collect(Collectors.toList());

                Platform.runLater(() -> {
                    tablaAdministradores.getItems().addAll(resultados);
                    lblEstado.setText("🔍 " + resultados.size() + " resultado(s) encontrado(s).");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error en búsqueda."));
            }
        }).start();
    }

    private void actualizarEstadisticas(List<User> administradores) {
        int total = administradores.size();
        int usuarioActual = esUsuarioActualEnLista(administradores) ? 1 : 0;
        
        lblEstado.setText(String.format("📊 Total: %d administrador(es) | 👤 Tú: %s", 
            total, usuarioActual > 0 ? "Sí" : "No"));
    }

    @FXML
    private void onRecargarClicked() {
        txtBuscar.clear();
        cargarAdministradores();
    }

    @FXML
    private void onNuevoClicked() {
        abrirFormulario(null);
    }

    private void editarAdministrador(User administrador) {
        abrirFormulario(administrador);
    }

    private void eliminarAdministrador(User administrador) {
        // No permitir eliminar al usuario actual
        if (esUsuarioActual(administrador)) {
            mostrarError("Acción no permitida", "No puedes eliminar tu propia cuenta.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar administrador");
        alert.setHeaderText("¿Eliminar al administrador \"" + administrador.getName() + " " + administrador.getApellidoPaterno() + "\"?");
        alert.setContentText("Expediente: " + administrador.getClave() + "\n\nEsta acción no se puede deshacer.");
        
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        allUsers.getUsers().remove(administrador);
                        allUsers.saveUsers();
                        
                        Platform.runLater(() -> {
                            lblEstado.setText("🗑️ Administrador eliminado correctamente.");
                            cargarAdministradores();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> lblEstado.setText("❌ Error al eliminar."));
                    }
                }).start();
            }
        });
    }

    private void abrirFormulario(User administrador) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/administradores/RegistroAdministrador.fxml"));
            Parent root = loader.load();

            RegisterAdministradorController controller = loader.getController();
            
            if (administrador != null) {
                controller.cargarDatosExistentes(administrador);
            }

            Stage stage = new Stage();
            stage.setTitle(administrador == null ? "👑 Nuevo Administrador" : "✏️ Editar Administrador");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Recargar cuando se cierre el formulario
            stage.setOnHidden(e -> cargarAdministradores());
            
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    /**
     * Verificar si un usuario es el usuario actual
     */
    private boolean esUsuarioActual(User usuario) {
        User currentUser = sessionManager.getCurrentUser();
        return currentUser != null && currentUser.getClave().equals(usuario.getClave());
    }

    /**
     * Verificar si el usuario actual está en la lista
     */
    private boolean esUsuarioActualEnLista(List<User> administradores) {
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null) return false;
        
        return administradores.stream()
            .anyMatch(admin -> admin.getClave().equals(currentUser.getClave()));
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}