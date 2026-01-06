package app.controllers.ingredientes;

import core.SessionManager;
import core.services.IngredienteService;
import core.services.IngredienteService.CrudCallback;
import core.services.IngredienteService.ListCallback;
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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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

    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        if (!session.isAdmin()) {
            mostrarError("Acceso denegado", "Solo administradores.");
            return;
        }

        configurarTabla();
        cargarIngredientes();

        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank())
                cargarIngredientes();
            else
                filtrarLocal(n);
        });
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colCalorias.setCellValueFactory(new PropertyValueFactory<>("calorias"));
        colAlergeno.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().isAlergenico() ? "Sí" : "No"));

        colAcciones.setCellFactory(
                (Callback<TableColumn<Ingrediente, Void>, TableCell<Ingrediente, Void>>) p -> new TableCell<>() {

                    private final Button btnEditar = new Button("Editar");
                    private final Button btnEliminar = new Button("Eliminar");
                    private final HBox box = new HBox(5, btnEditar, btnEliminar);

                    {
                        // Configurar tamaño y estilos de los botones
                        btnEditar.setMinWidth(70);
                        btnEliminar.setMinWidth(70);

                        // Estilos para los botones
                        btnEditar.setStyle(
                                "-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;");
                        btnEliminar.setStyle(
                                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");

                        // Agregar hover effects
                        btnEditar.setOnMouseEntered(e -> btnEditar.setStyle(
                                "-fx-background-color: #f39c12; -fx-text-fill: black; -fx-font-weight: bold;"));
                        btnEditar.setOnMouseExited(e -> btnEditar.setStyle(
                                "-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold;"));

                        btnEliminar.setOnMouseEntered(e -> btnEliminar.setStyle(
                                "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-weight: bold;"));
                        btnEliminar.setOnMouseExited(e -> btnEliminar.setStyle(
                                "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;"));

                        btnEditar.setOnAction(e -> {
                            Ingrediente ingrediente = getTableView().getItems().get(getIndex());
                            if (ingrediente != null) {
                                abrirFormulario(ingrediente);
                            }
                        });

                        btnEliminar.setOnAction(e -> {
                            Ingrediente ingrediente = getTableView().getItems().get(getIndex());
                            if (ingrediente != null) {
                                eliminarIngrediente(ingrediente);
                            }
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(box);
                        }
                    }
                });
    }

    /*
     * =========================
     * === CARGA DESDE API ===
     * =========================
     */

    private void cargarIngredientes() {
        lblEstado.setText("Cargando ingredientes...");
        tablaIngredientes.getItems().clear();

        IngredienteService.listIngredientes(new ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> list) {
                List<Ingrediente> ingredientes = new ArrayList<>();
                for (JSONObject o : list) {
                    ingredientes.add(jsonToIngrediente(o));
                }

                Platform.runLater(() -> {
                    tablaIngredientes.getItems().addAll(ingredientes);
                    lblEstado.setText("✅ " + ingredientes.size() + " ingredientes cargados");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> lblEstado.setText("❌ " + error));
            }
        });
    }

    /*
     * =========================
     * === FILTRO LOCAL ===
     * =========================
     */

    private void filtrarLocal(String q) {
        final String query = q.toLowerCase();

        tablaIngredientes.getItems().removeIf(i -> !i.getNombre().toLowerCase().contains(query) &&
                (i.getCategoria() == null || !i.getCategoria().toLowerCase().contains(query)) &&
                !i.getDescripcion().toLowerCase().contains(query));
    }

    /*
     * =========================
     * === ELIMINAR ===
     * =========================
     */

    private void eliminarIngrediente(Ingrediente ing) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Confirmar eliminación");
        a.setHeaderText("¿Eliminar ingrediente \"" + ing.getNombre() + "\"?");
        a.setContentText("Esta acción no se puede deshacer. El ingrediente será eliminado permanentemente.");
        a.getDialogPane().setMinWidth(400);

        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                lblEstado.setText("Eliminando ingrediente...");
                IngredienteService.deleteIngrediente(ing.getId(), new CrudCallback() {
                    @Override
                    public void onSuccess() {
                        Platform.runLater(() -> {
                            mostrarExito("Éxito", "Ingrediente \"" + ing.getNombre() + "\" eliminado correctamente");
                            cargarIngredientes();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            mostrarError("Error al eliminar", error);
                            lblEstado.setText("❌ " + error);
                        });
                    }
                });
            }
        });
    }

    // Métodos de utilidad para mostrar alertas
    private void mostrarError(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.getDialogPane().setMinWidth(400);
            alert.showAndWait();
        });
    }

    private void mostrarExito(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.getDialogPane().setMinWidth(400);
            alert.showAndWait();
        });
    }

    /*
     * =========================
     * === FORMULARIO ===
     * =========================
     */

    @FXML
    private void onNuevoIngredienteClicked() {
        abrirFormulario(null);
    }

    private void abrirFormulario(Ingrediente ingrediente) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/views/ingredientes/RegistroIngrediente.fxml"));
            Parent root = loader.load();

            RegistroIngredienteController c = loader.getController();
            if (ingrediente != null)
                c.cargarDatosExistentes(ingrediente);

            Stage s = new Stage();
            s.initModality(Modality.APPLICATION_MODAL);
            s.setScene(new Scene(root));
            s.showAndWait();

            cargarIngredientes();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", e.getMessage());
        }
    }

    /*
     * =========================
     * === JSON → MODELO ===
     * =========================
     */

    private Ingrediente jsonToIngrediente(JSONObject o) {
        Ingrediente i = new Ingrediente();
        i.setId(o.getInt("ID"));
        i.setNombre(o.getString("Nombre"));
        i.setCategoria(o.optString("Categoria", ""));
        i.setDescripcion(o.optString("Descripcion", ""));
        i.setCalorias(o.optDouble("Calorias", 0));
        i.setAlergenico(o.optInt("Alergeno", 0) == 1);
        return i;
    }

    @FXML
    private void onRecargarClicked() {
        txtBuscar.clear();
        cargarIngredientes();
    }

}
