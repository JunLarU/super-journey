package app.controllers.productos;

import core.SessionManager;
import core.services.ProductoService;
import core.data.Productos.Producto;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.json.JSONObject;

import java.util.List;

/**
 * Controlador para la gestión de productos.
 * Ahora usa ProductoService (servidor) en lugar de AllProductos.
 */
public class ProductosController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRecargar, btnNuevo;
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colID, colNombre, colCategoria, colPrecio, colDisponible;
    @FXML private TableColumn<Producto, Void> colAcciones;
    @FXML private Label lblEstado;

    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {

        // 🔐 Validar permisos
        if (!session.isAdmin()) {
            mostrarError("Acceso denegado", "Solo administradores pueden acceder.");
            return;
        }

        configurarTabla();
        cargarProductos();

        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                cargarProductos();
            } else {
                buscarProductos(n);
            }
        });
    }

    // ==========================================================
    // CONFIGURACIÓN DE TABLA
    // ==========================================================
    private void configurarTabla() {

        colID.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().getId())
            )
        );

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));

        colPrecio.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                String.format("$%.2f", d.getValue().getPrecioBase())
            )
        );

        colDisponible.setCellValueFactory(d ->
            new javafx.beans.property.SimpleStringProperty(
                d.getValue().isDisponible() ? "Sí" : "No"
            )
        );

        colAcciones.setCellFactory(
            (Callback<TableColumn<Producto, Void>, TableCell<Producto, Void>>) param ->
                new TableCell<>() {

                    private final Button btnVer = new Button("Ver");
                    private final Button btnEditar = new Button("Editar");
                    private final Button btnEliminar = new Button("Eliminar");

                    {
                        btnVer.setMinWidth(90);
                        btnEditar.setMinWidth(90);
                        btnEliminar.setMinWidth(90);

                        btnVer.setStyle("-fx-background-color:#3498db;-fx-text-fill:white;");
                        btnEditar.setStyle("-fx-background-color:#f1c40f;");
                        btnEliminar.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;");

                        btnVer.setOnAction(e ->
                            abrirFormularioSoloLectura(getTableView().getItems().get(getIndex()))
                        );

                        btnEditar.setOnAction(e ->
                            abrirFormulario(getTableView().getItems().get(getIndex()))
                        );

                        btnEliminar.setOnAction(e ->
                            eliminarProducto(getTableView().getItems().get(getIndex()))
                        );
                    }

                    private final HBox box = new HBox(5, btnVer, btnEditar, btnEliminar);

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : box);
                    }
                }
        );
    }

    // ==========================================================
    // CARGAR PRODUCTOS DESDE SERVIDOR
    // ==========================================================
    @FXML
private void cargarProductos() {
    lblEstado.setText("Cargando productos...");
    tablaProductos.getItems().clear();

    ProductoService.listProductos(new ProductoService.ListCallback() {

        @Override
        public void onSuccess(List<org.json.JSONObject> list) {
            Platform.runLater(() -> {
                tablaProductos.getItems().setAll(
                    list.stream()
                        .map(Producto::fromJSON)
                        .toList()
                );
                lblEstado.setText("✅ " + list.size() + " productos cargados");
            });
        }

        @Override
        public void onError(String error) {
            Platform.runLater(() ->
                lblEstado.setText("❌ " + error)
            );
        }
    });
}


    // ==========================================================
    // BÚSQUEDA LOCAL (sobre datos ya cargados)
    // ==========================================================
    private void buscarProductos(String q) {
        String query = q.toLowerCase();

        List<Producto> filtrados = tablaProductos.getItems().stream()
            .filter(p ->
                p.getNombre().toLowerCase().contains(query) ||
                (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(query)) ||
                (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(query))
            )
            .toList();

        tablaProductos.getItems().setAll(filtrados);
        lblEstado.setText("🔍 " + filtrados.size() + " resultado(s)");
    }

    // ==========================================================
    // ELIMINAR PRODUCTO
    // ==========================================================
    private void eliminarProducto(Producto p) {

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText("¿Eliminar \"" + p.getNombre() + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");

        confirm.showAndWait().ifPresent(b -> {
            if (b == ButtonType.OK) {

                ProductoService.deleteProducto(
                    p.getId(),
                    () -> Platform.runLater(this::cargarProductos),
                    err -> Platform.runLater(() ->
                        lblEstado.setText("❌ " + err)
                    )
                );
            }
        });
    }

    // ==========================================================
    // FORMULARIOS
    // ==========================================================
    @FXML
    private void onNuevoClicked() {
        abrirFormulario(null);
    }

    private void abrirFormulario(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/app/views/productos/RegistroProducto.fxml")
            );
            Parent root = loader.load();

            RegistroProductoController ctrl = loader.getController();
            if (producto != null) {
                ctrl.cargarDatosExistentes(producto);
            }

            Stage s = new Stage();
            s.setTitle(producto == null ? "Nuevo producto" : "Editar producto");
            s.setScene(new Scene(root));
            s.initModality(Modality.APPLICATION_MODAL);
            s.setResizable(false);
            s.showAndWait();

            cargarProductos();

        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("❌ Error al abrir formulario");
        }
    }

    private void abrirFormularioSoloLectura(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/app/views/productos/RegistroProducto.fxml")
            );
            Parent root = loader.load();

            RegistroProductoController ctrl = loader.getController();
            ctrl.visualizarProducto(producto);

            Stage s = new Stage();
            s.setTitle("Visualizar producto");
            s.setScene(new Scene(root));
            s.initModality(Modality.APPLICATION_MODAL);
            s.setResizable(false);
            s.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==========================================================
    // UTILIDAD
    // ==========================================================
    private void mostrarError(String t, String m) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(t);
        a.setContentText(m);
        a.showAndWait();
    }
}
