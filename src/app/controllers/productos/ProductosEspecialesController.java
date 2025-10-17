// ProductosEspecialesController.java
package app.controllers.productos;

import core.SessionManager;
import core.data.Productos.AllProductosEspeciales;
import core.data.Productos.ProductoEspecial;
import core.data.Productos.AllProductos;
import core.data.Productos.Producto;
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
 * Controlador para gestión de productos especiales
 */
public class ProductosEspecialesController {

    @FXML
    private TextField txtBuscar;
    @FXML
    private Button btnRecargar;
    @FXML
    private Button btnNuevo;
    @FXML
    private TableView<ProductoEspecial> tablaEspeciales;
    @FXML
    private TableColumn<ProductoEspecial, String> colID;
    @FXML
    private TableColumn<ProductoEspecial, String> colProducto;
    @FXML
    private TableColumn<ProductoEspecial, String> colFechas;
    @FXML
    private TableColumn<ProductoEspecial, String> colHorario;
    @FXML
    private TableColumn<ProductoEspecial, String> colPrecioEspecial;
    @FXML
    private TableColumn<ProductoEspecial, String> colEstado;
    @FXML
    private TableColumn<ProductoEspecial, Void> colAcciones;
    @FXML
    private Label lblEstado;

    private final AllProductosEspeciales allEspeciales = AllProductosEspeciales.getInstance();
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
        cargarEspeciales();

        txtBuscar.textProperty().addListener((obs, o, n) -> {
            if (n.isBlank())
                cargarEspeciales();
            else
                buscarEspeciales(n);
        });
    }

    private void configurarTabla() {
        // Configurar columnas
        colID.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));
        
        colProducto.setCellValueFactory(data -> {
            Producto producto = allProductos.getById(data.getValue().getIdProducto());
            String nombreProducto = producto != null ? producto.getNombre() : "Producto #" + data.getValue().getIdProducto();
            return new javafx.beans.property.SimpleStringProperty(nombreProducto);
        });
        
        colFechas.setCellValueFactory(data -> {
            ProductoEspecial especial = data.getValue();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String fechaInicio = especial.getFechaInicio().format(dateFormatter);
            String fechaFin = especial.getFechaFin().format(dateFormatter);
            return new javafx.beans.property.SimpleStringProperty(fechaInicio + " - " + fechaFin);
        });
        
        colHorario.setCellValueFactory(data -> {
            ProductoEspecial especial = data.getValue();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            String horaInicio = especial.getFechaInicio().format(timeFormatter);
            String horaFin = especial.getFechaFin().format(timeFormatter);
            return new javafx.beans.property.SimpleStringProperty(horaInicio + " - " + horaFin);
        });
        
        colPrecioEspecial.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(String.format("$%.2f", data.getValue().getPrecioEspecial())));
        
        colEstado.setCellValueFactory(data -> {
            ProductoEspecial especial = data.getValue();
            String estado;
            if (!especial.isActivo()) {
                estado = "❌ Inactivo";
            } else if (especial.estaVigente()) {
                estado = "✅ Vigente";
            } else if (LocalDateTime.now().isBefore(especial.getFechaInicio())) {
                estado = "Próximo";
            } else {
                estado = "📅 Expirado";
            }
            return new javafx.beans.property.SimpleStringProperty(estado);
        });

        // Columna de acciones
        colAcciones.setReorderable(false);
        colAcciones.setResizable(false);
        colAcciones.setSortable(false);
        colAcciones.setMinWidth(210);
        colAcciones.setCellFactory(
            (Callback<TableColumn<ProductoEspecial, Void>, TableCell<ProductoEspecial, Void>>) param -> new TableCell<>() {
                private final Button btnEditar = new Button("Editar");
                private final Button btnEliminar = new Button("Eliminar");

                {

                btnEditar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                btnEditar.setMinWidth(100);
                btnEliminar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                btnEliminar.setMinWidth(100);
                    // Estilos de botones
                    btnEditar.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");
                    btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;");

                    // Tooltips
                    btnEditar.setTooltip(new Tooltip("Editar producto especial"));
                    btnEliminar.setTooltip(new Tooltip("Eliminar producto especial"));

                    // Acciones
                    btnEditar.setOnAction(e -> {
                        ProductoEspecial especial = getTableView().getItems().get(getIndex());
                        editarEspecial(especial);
                    });
                    
                    btnEliminar.setOnAction(e -> {
                        ProductoEspecial especial = getTableView().getItems().get(getIndex());
                        eliminarEspecial(especial);
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
    private void cargarEspeciales() {
        lblEstado.setText("Cargando productos especiales...");
        tablaEspeciales.getItems().clear();

        new Thread(() -> {
            try {
                List<ProductoEspecial> especiales = allEspeciales.getAll();
                
                Platform.runLater(() -> {
                    tablaEspeciales.getItems().addAll(especiales);
                    actualizarEstadisticas(especiales);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error al cargar productos especiales."));
            }
        }).start();
    }

    private void buscarEspeciales(String query) {
        lblEstado.setText("Buscando \"" + query + "\"...");
        tablaEspeciales.getItems().clear();

        new Thread(() -> {
            try {
                List<ProductoEspecial> todos = allEspeciales.getAll();
                String queryLower = query.toLowerCase();
                
                List<ProductoEspecial> resultados = todos.stream()
                    .filter(especial -> {
                        // Buscar por nombre de producto
                        Producto producto = allProductos.getById(especial.getIdProducto());
                        if (producto != null && producto.getNombre().toLowerCase().contains(queryLower)) {
                            return true;
                        }
                        // Buscar por descripción
                        if (especial.getDescripcion() != null && 
                            especial.getDescripcion().toLowerCase().contains(queryLower)) {
                            return true;
                        }
                        // Buscar por rango de fechas y horas
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                        String fechasHoras = especial.getFechaInicio().format(formatter) + " " + especial.getFechaFin().format(formatter);
                        return fechasHoras.toLowerCase().contains(queryLower);
                    })
                    .toList();

                Platform.runLater(() -> {
                    tablaEspeciales.getItems().addAll(resultados);
                    lblEstado.setText("🔍 " + resultados.size() + " resultado(s) encontrado(s).");
                });
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> lblEstado.setText("❌ Error en búsqueda."));
            }
        }).start();
    }

    private void actualizarEstadisticas(List<ProductoEspecial> especiales) {
        int total = especiales.size();
        int vigentes = (int) especiales.stream().filter(ProductoEspecial::estaVigente).count();
        int activos = (int) especiales.stream().filter(ProductoEspecial::isActivo).count();
        
        lblEstado.setText(String.format("📊 Total: %d | ✅ Vigentes: %d | 🔄 Activos: %d", total, vigentes, activos));
    }

    @FXML
    private void onRecargarClicked() {
        txtBuscar.clear();
        cargarEspeciales();
    }

    @FXML
    private void onNuevoClicked() {
        abrirFormulario(null);
    }

    private void editarEspecial(ProductoEspecial especial) {
        abrirFormulario(especial);
    }

    private void eliminarEspecial(ProductoEspecial especial) {
        String nombreProducto = getNombreProducto(especial.getIdProducto());
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eliminar producto especial");
        alert.setHeaderText("¿Eliminar el producto especial de \"" + nombreProducto + "\"?");
        alert.setContentText("Esta acción no se puede deshacer.");
        
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                new Thread(() -> {
                    try {
                        allEspeciales.removeProductoEspecial(especial.getId());
                        Platform.runLater(() -> {
                            lblEstado.setText("Eliminar Producto especial eliminado correctamente.");
                            cargarEspeciales();
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Platform.runLater(() -> lblEstado.setText("❌ Error al eliminar."));
                    }
                }).start();
            }
        });
    }

    private String getNombreProducto(int idProducto) {
        Producto producto = allProductos.getById(idProducto);
        return producto != null ? producto.getNombre() : "Producto #" + idProducto;
    }

    private void abrirFormulario(ProductoEspecial especial) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/productos/RegistroProductoEspecial.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del formulario
            RegistroProductoEspecialController controller = loader.getController();
            
            if (especial != null) {
                controller.cargarDatosExistentes(especial);
            }

            Stage stage = new Stage();
            stage.setTitle(especial == null ? "Nuevo Producto Especial" : "Editar Producto Especial");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            
            // Recargar cuando se cierre el formulario
            stage.setOnHidden(e -> cargarEspeciales());
            
            stage.showAndWait();

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