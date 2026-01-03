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
                        btnEditar.setOnAction(e -> abrirFormulario(getTableView().getItems().get(getIndex())));

                        btnEliminar.setOnAction(e -> eliminarIngrediente(getTableView().getItems().get(getIndex())));
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
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
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar \"" + ing.getNombre() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);

        a.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {
                IngredienteService.deleteIngrediente(ing.getId(), new CrudCallback() {
                    @Override
                    public void onSuccess() {
                        Platform.runLater(() -> {
                            lblEstado.setText("🗑️ Eliminado correctamente");
                            cargarIngredientes();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> lblEstado.setText("❌ " + error));
                    }
                });
            }
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

    private void mostrarError(String t, String m) {
        new Alert(Alert.AlertType.ERROR, m).showAndWait();
    }

    @FXML
    private void onRecargarClicked() {
        txtBuscar.clear();
        cargarIngredientes();
    }

}
