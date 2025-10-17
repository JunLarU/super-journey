package app.controllers.productos;

import core.SessionManager;
import core.data.Productos.AllProductos;
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

import java.util.List;

/**
 * Controlador para la gestión de productos.
 * Ahora usa AllProductos en lugar del servidor.
 */
public class ProductosController {

    @FXML private TextField txtBuscar;
    @FXML private Button btnRecargar, btnNuevo;
    @FXML private TableView<Producto> tablaProductos;
    @FXML private TableColumn<Producto, String> colID, colNombre, colCategoria, colPrecio, colDisponible;
    @FXML private TableColumn<Producto, Void> colAcciones;
    @FXML private Label lblEstado;

    private final AllProductos allProductos = AllProductos.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        // Verificar permisos de administrador
        if (!session.isAdmin()) {
            mostrarError("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        configurarTabla();
        cargarProductos();
        
        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank())
                cargarProductos();
            else
                buscarProductos(n);
        });
    }

    private void configurarTabla() {
        // Configurar columnas usando PropertyValueFactory para objetos Producto
        colID.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        
        colPrecio.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                String.format("$%.2f", data.getValue().getPrecioBase())));
        
        colDisponible.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(
                data.getValue().isDisponible() ? "Sí" : "No"));

        // Configurar columna de acciones con botones
        colAcciones.setReorderable(false);
        colAcciones.setResizable(false);
        colAcciones.setSortable(false);
        colAcciones.setMinWidth(310);
        colAcciones.setCellFactory(
            (Callback<TableColumn<Producto, Void>, TableCell<Producto, Void>>) param -> new TableCell<>() {
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
                    btnEditar.setTooltip(new Tooltip("Editar producto"));
                    btnEliminar.setTooltip(new Tooltip("Eliminar producto"));

                    // Acciones
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

                private final HBox pane = new HBox(5, btnVer, btnEditar, btnEliminar);

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : pane);
                }
            });
    }

    @FXML
    private void cargarProductos() {
        lblEstado.setText("Cargando productos...");
        tablaProductos.getItems().clear();

        new Thread(() -> {
            try {
                List<Producto> productos = allProductos.getAll();
                
                Platform.runLater(() -> {
                    tablaProductos.getItems().addAll(productos);
                    lblEstado.setText("✅ " + productos.size() + " productos cargados.");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error al cargar productos."));
            }
        }).start();
    }

    /**
     * Buscar productos por nombre, categoría o descripción
     */
    private void buscarProductos(String query) {
        lblEstado.setText("Buscando \"" + query + "\"...");
        tablaProductos.getItems().clear();

        new Thread(() -> {
            try {
                List<Producto> todos = allProductos.getAll();
                String queryLower = query.toLowerCase();
                
                List<Producto> resultados = todos.stream()
                    .filter(p -> 
                        p.getNombre().toLowerCase().contains(queryLower) ||
                        (p.getCategoria() != null && p.getCategoria().toLowerCase().contains(queryLower)) ||
                        (p.getDescripcion() != null && p.getDescripcion().toLowerCase().contains(queryLower))
                    )
                    .toList();

                Platform.runLater(() -> {
                    tablaProductos.getItems().addAll(resultados);
                    lblEstado.setText("🔍 " + resultados.size() + " resultado(s).");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error en búsqueda."));
            }
        }).start();
    }

    /**
     * Abrir formulario en modo solo lectura (visualización)
     */
    private void abrirFormularioSoloLectura(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/productos/RegistroProducto.fxml"));
            Parent root = loader.load();
            
            RegistroProductoController controller = loader.getController();
            controller.visualizarProducto(producto);

            Stage stage = new Stage();
            stage.setTitle("Visualizar producto");
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
     * Eliminar producto
     */
    private void eliminarProducto(Producto producto) {
        String nombre = producto.getNombre();
        int id = producto.getId();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Eliminar producto");
        confirm.setHeaderText("¿Eliminar \"" + nombre + "\"?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        allProductos.removeProducto(id);
                        
                        Platform.runLater(() -> {
                            lblEstado.setText("Eliminar Producto eliminado correctamente.");
                            cargarProductos();
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
     * Abrir formulario para nuevo producto
     */
    @FXML
    private void onNuevoClicked() {
        abrirFormulario(null);
    }

    /**
     * Abrir formulario para editar producto existente o crear uno nuevo
     */
    private void abrirFormulario(Producto producto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/productos/RegistroProducto.fxml"));
            Parent root = loader.load();
            
            RegistroProductoController controller = loader.getController();
            
            if (producto != null) {
                controller.cargarDatosExistentes(producto);
            }

            Stage stage = new Stage();
            stage.setTitle(producto == null ? "Nuevo producto" : "Editar producto");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            cargarProductos();
            
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