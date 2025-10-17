package app.controllers.ingredientes;

import core.SessionManager;
import core.data.Ingredientes.AllIngredientes;
import core.data.Ingredientes.Ingrediente;
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

/**
 * Controlador para gestión de ingredientes: listar, buscar, agregar, editar,
 * eliminar.
 * Ahora usando AllIngredientes en lugar de servidor.
 */
public class IngredientesController {

    @FXML
    private TextField txtBuscar;
    @FXML
    private Button btnRecargar;
    @FXML
    private Button btnNuevo;
    @FXML
    private TableView<Ingrediente> tablaIngredientes;
    @FXML
    private TableColumn<Ingrediente, Integer> colId;
    @FXML
    private TableColumn<Ingrediente, String> colNombre;
    @FXML
    private TableColumn<Ingrediente, String> colCategoria;
    @FXML
    private TableColumn<Ingrediente, String> colDescripcion;
    @FXML
    private TableColumn<Ingrediente, Double> colCalorias;
    @FXML
    private TableColumn<Ingrediente, String> colAlergeno;
    @FXML
    private TableColumn<Ingrediente, Void> colAcciones;
    @FXML
    private Label lblEstado;

    private final AllIngredientes allIngredientes = AllIngredientes.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        // Verificar permisos de administrador
        if (!session.isAdmin()) {
            mostrarError("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        configurarTabla();
        cargarIngredientes();

        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank())
                cargarIngredientes();
            else
                buscarIngredientes(n);
        });
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCalorias.setCellValueFactory(new PropertyValueFactory<>("calorias"));
        colAlergeno.setCellValueFactory(data -> {
            Ingrediente ing = data.getValue();
            return new javafx.beans.property.SimpleStringProperty(ing.isAlergenico() ? "Sí" : "No");
        });

        // 🔧 Columna de botones de acción
        colAcciones.setCellFactory(
                (Callback<TableColumn<Ingrediente, Void>, TableCell<Ingrediente, Void>>) param -> new TableCell<>() {
                    private final Button btnEditar = new Button("✏️");
                    private final Button btnEliminar = new Button("🗑️");

                    {
                        btnEditar.setStyle(
                                "-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5;");
                        btnEliminar.setStyle(
                                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");

                        btnEditar.setOnAction(e -> {
                            Ingrediente ingrediente = getTableView().getItems().get(getIndex());
                            editarIngrediente(ingrediente);
                        });
                        btnEliminar.setOnAction(e -> {
                            Ingrediente ingrediente = getTableView().getItems().get(getIndex());
                            eliminarIngrediente(ingrediente);
                        });
                    }

                    private final HBox pane = new HBox(5, btnEditar, btnEliminar);

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(pane);
                        }
                    }
                });
    }

    @FXML
    private void cargarIngredientes() {
        lblEstado.setText("Cargando ingredientes...");
        tablaIngredientes.getItems().clear();

        new Thread(() -> {
            try {
                List<Ingrediente> ingredientes = allIngredientes.getAll();
                Platform.runLater(() -> {
                    tablaIngredientes.getItems().addAll(ingredientes);
                    lblEstado.setText("✅ Se cargaron " + ingredientes.size() + " ingredientes.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error al cargar ingredientes."));
            }
        }).start();
    }

    private void buscarIngredientes(String query) {
        lblEstado.setText("Buscando \"" + query + "\"...");
        tablaIngredientes.getItems().clear();

        new Thread(() -> {
            try {
                List<Ingrediente> todos = allIngredientes.getAll();
                List<Ingrediente> resultados = todos.stream()
                        .filter(ing -> ing.getNombre().toLowerCase().contains(query.toLowerCase()) ||
                                (ing.getcategoria() != null
                                        && ing.getcategoria().toLowerCase().contains(query.toLowerCase()))
                                ||
                                ing.getDescripcion().toLowerCase().contains(query.toLowerCase()))
                        .toList();

                Platform.runLater(() -> {
                    tablaIngredientes.getItems().addAll(resultados);
                    lblEstado.setText("🔍 " + resultados.size() + " resultado(s).");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error en búsqueda."));
            }
        }).start();
    }

    @FXML
    private void onRecargarClicked() {
        txtBuscar.clear();
        cargarIngredientes();
    }

    @FXML
    private void onNuevoIngredienteClicked() {
        abrirFormulario(null); // null → modo nuevo
    }

    // ✏️ Editar
    private void editarIngrediente(Ingrediente ingrediente) {
        abrirFormulario(ingrediente); // modo edición
    }

    // 🗑️ Eliminar
    private void eliminarIngrediente(Ingrediente ingrediente) {
        String nombre = ingrediente.getNombre();
        int id = ingrediente.getId();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar ingrediente");
        alert.setHeaderText("¿Eliminar \"" + nombre + "\"?");
        alert.setContentText("Esta acción no se puede deshacer.");
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        allIngredientes.removeIngrediente(id);
                        Platform.runLater(() -> {
                            lblEstado.setText("🗑️ " + nombre + " eliminado correctamente.");
                            cargarIngredientes();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> lblEstado.setText("❌ Error al eliminar."));
                    }
                }).start();
            }
        });
    }

    // Abre el formulario (nuevo o editar)
    private void abrirFormulario(Ingrediente ingrediente) {
        try {
            // Ruta corregida para ser consistente con la ubicación del controlador
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/views/ingredientes/RegistroIngrediente.fxml"));
            Parent root = loader.load();

            // Obtenemos el controlador del formulario
            RegistroIngredienteController formController = loader.getController();

            if (ingrediente != null) {
                formController.cargarDatosExistentes(ingrediente);
            }

            Stage stage = new Stage();
            stage.setTitle(ingrediente == null ? "Registrar nuevo ingrediente" : "Editar ingrediente");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            cargarIngredientes();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}