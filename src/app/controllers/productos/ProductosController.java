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
 * Usa ProductoService para comunicación con el servidor.
 */
public class ProductosController {

    @FXML
    private TextField txtBuscar;
    @FXML
    private Button btnRecargar, btnNuevo;
    @FXML
    private TableView<Producto> tablaProductos;
    @FXML
    private TableColumn<Producto, String> colID, colNombre, colCategoria, colPrecio, colDisponible;
    @FXML
    private TableColumn<Producto, Void> colAcciones;
    @FXML
    private Label lblEstado;

    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {

        // 🔐 Validar permisos
        if (!session.isAdmin()) {
            mostrarError("Acceso denegado", "Solo administradores pueden acceder.");
            Platform.runLater(() -> {
                Stage stage = (Stage) tablaProductos.getScene().getWindow();
                stage.close();
            });
            return;
        }

        configurarTabla();
        configurarEventos();
        cargarProductos();
    }

    // ==========================================================
    // CONFIGURACIÓN DE TABLA
    // ==========================================================
    private void configurarTabla() {

        colID.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().getId())));

        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));

        colPrecio.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.format("$%.2f", d.getValue().getPrecioBase())));

        colDisponible.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().isDisponible() ? "Sí" : "No"));

        colAcciones.setCellFactory(
                (Callback<TableColumn<Producto, Void>, TableCell<Producto, Void>>) param -> new TableCell<Producto, Void>() {

                    private final Button btnVer = new Button("Ver");
                    private final Button btnEditar = new Button("Editar");
                    private final Button btnEliminar = new Button("Eliminar");

                    {
                        btnVer.setMinWidth(70);
                        btnEditar.setMinWidth(70);
                        btnEliminar.setMinWidth(70);

                        btnVer.setStyle("-fx-background-color:#3498db;-fx-text-fill:white;");
                        btnEditar.setStyle("-fx-background-color:#f1c40f;");
                        btnEliminar.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;");

                        btnVer.setOnAction(e -> {
                            Producto producto = getTableView().getItems().get(getIndex());
                            abrirFormularioSoloLectura(producto);
                        });

                        btnEditar.setOnAction(e -> {
                            Producto producto = getTableView().getItems().get(getIndex());
                            abrirFormulario(producto);
                        });

                        btnEliminar.setOnAction(e -> {
                            Producto producto = getTableView().getItems().get(getIndex());
                            eliminarProducto(producto);
                        });
                    }

                    private final HBox box = new HBox(5, btnVer, btnEditar, btnEliminar);

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

    private void configurarEventos() {
        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n == null || n.isBlank()) {
                cargarProductos();
            } else {
                buscarProductos(n);
            }
        });

        btnRecargar.setOnAction(e -> cargarProductos());
        btnNuevo.setOnAction(e -> onNuevoClicked());
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
            public void onSuccess(List<JSONObject> list) {
                Platform.runLater(() -> {
                    try {
                        tablaProductos.getItems().setAll(
                                list.stream()
                                        .map(json -> {
                                            try {
                                                return Producto.fromJSON(json);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                return null;
                                            }
                                        })
                                        .filter(p -> p != null)
                                        .toList());
                        lblEstado.setText("✅ " + list.size() + " productos cargados");
                    } catch (Exception e) {
                        e.printStackTrace();
                        lblEstado.setText("❌ Error al procesar datos");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblEstado.setText("❌ " + error);
                    mostrarError("Error", "No se pudieron cargar los productos: " + error);
                });
            }
        });
    }

    // ==========================================================
    // BÚSQUEDA LOCAL (sobre datos ya cargados)
    // ==========================================================
    private void buscarProductos(String q) {
        String query = q.toLowerCase();

        List<Producto> filtrados = tablaProductos.getItems().stream()
                .filter(p -> p.getNombre().toLowerCase().contains(query) ||
                        (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(query)) ||
                        (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(query)))
                .toList();

        tablaProductos.getItems().setAll(filtrados);
        lblEstado.setText("🔍 " + filtrados.size() + " resultado(s)");
    }

    // ==========================================================
    // ELIMINAR PRODUCTO
    // ==========================================================
    private void eliminarProducto(Producto producto) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText("¿Eliminar \"" + producto.getNombre() + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        confirm.getDialogPane().setMinWidth(400);

        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                ProductoService.deleteProducto(
                        producto.getId(),
                        new ProductoService.CrudCallback() {
                            @Override
                            public void onSuccess() {
                                Platform.runLater(() -> {
                                    cargarProductos();
                                    mostrarExito("Éxito", "Producto eliminado correctamente");
                                });
                            }

                            @Override
                            public void onError(String error) {
                                Platform.runLater(() -> {
                                    lblEstado.setText("❌ " + error);
                                    mostrarError("Error al eliminar", error);
                                });
                            }
                        });
            }
        });
    }

    // ==========================================================
    // FORMULARIOS - CORRECIONES PRINCIPALES
    // ==========================================================
    @FXML
    private void onNuevoClicked() {
        abrirFormulario(null);
    }

    private void abrirFormulario(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/views/productos/RegistroProducto.fxml"));
            Parent root = loader.load();

            RegistroProductoController ctrl = loader.getController();
            if (producto != null) {
                // Usar el método específico para formularios
                JSONObject productoJson = producto.toJsonForForm();
                ctrl.cargarDatosExistentes(productoJson);
            }

            Stage stage = new Stage();
            stage.setTitle(producto == null ? "Nuevo producto" : "Editar producto: " + producto.getNombre());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.setOnHidden(e -> cargarProductos());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            lblEstado.setText("❌ Error al abrir formulario");
            mostrarError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void abrirFormularioSoloLectura(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/views/productos/RegistroProducto.fxml"));
            Parent root = loader.load();

            RegistroProductoController ctrl = loader.getController();

            // Usar el método específico para formularios
            JSONObject productoJson = producto.toJsonForForm();

            // Debug: ver qué se está enviando
            System.out.println("JSON enviado al formulario: " + productoJson.toString());

            ctrl.visualizarProducto(productoJson);

            Stage stage = new Stage();
            stage.setTitle("Visualizar producto: " + producto.getNombre());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir la visualización: " + e.getMessage());
        }
    }

    // ==========================================================
    // UTILIDAD
    // ==========================================================
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
}