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
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private TableColumn<ProductoEspecial, String> colHorario; // Esta ya no se usará
    @FXML
    private TableColumn<ProductoEspecial, String> colFechaHora; // Nueva columna
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

    @FXML
    public void initialize() {
        // Verificar permisos
        if (!SessionManager.getInstance().isAdmin()) {
            mostrarError("Acceso denegado", "Solo administradores");
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
        colID.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(String.valueOf(data.getValue().getId())));

        colProducto.setCellValueFactory(data -> {
            Producto producto = allProductos.getById(data.getValue().getIdProducto());
            String nombre = producto != null ? producto.getNombre() : "Producto #" + data.getValue().getIdProducto();
            return new javafx.beans.property.SimpleStringProperty(nombre);
        });

        // NUEVA COLUMNA: Fecha y hora combinadas
        colFechaHora.setCellValueFactory(data -> {
            ProductoEspecial especial = data.getValue();
            DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter horaFormatter = DateTimeFormatter.ofPattern("HH:mm");

            String fechaInicio = especial.getFechaInicio().format(fechaFormatter);
            String fechaFin = especial.getFechaFin().format(fechaFormatter);
            String horaInicio = especial.getFechaInicio().format(horaFormatter);
            String horaFin = especial.getFechaFin().format(horaFormatter);

            // Formato: "dd/MM/yyyy HH:mm - dd/MM/yyyy HH:mm"
            return new javafx.beans.property.SimpleStringProperty(
                    fechaInicio + " " + horaInicio + " - " + fechaFin + " " + horaFin);
        });

        // Eliminamos las columnas individuales de fecha y horario
        // colFechas y colHorario ya no se configuran

        colPrecioEspecial.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("$%.2f", data.getValue().getPrecioEspecial())));

        colEstado.setCellValueFactory(data -> {
            ProductoEspecial especial = data.getValue();
            String estado;
            if (!especial.isActivo()) {
                estado = "❌ Inactivo";
            } else if (especial.estaVigente()) {
                estado = "✅ Vigente";
            } else if (LocalDateTime.now().isBefore(especial.getFechaInicio())) {
                estado = "⏳ Próximo";
            } else {
                estado = "📅 Expirado";
            }
            return new javafx.beans.property.SimpleStringProperty(estado);
        });

        colAcciones.setCellFactory(param -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnEditar.setMinWidth(100);
                btnEliminar.setMinWidth(100);
                btnEditar.setStyle(
                        "-fx-background-color: #f1c40f; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5;");
                btnEliminar.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5;");

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

        allEspeciales.getAllAsync(new AllProductosEspeciales.ProductosEspecialesCallback() {
            @Override
            public void onSuccess(List<ProductoEspecial> productos) {
                Platform.runLater(() -> {
                    tablaEspeciales.getItems().addAll(productos);
                    actualizarEstadisticas(productos);
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblEstado.setText("❌ Error al cargar: " + error);
                    mostrarError("Error", error);
                });
            }
        });
    }

    private void buscarEspeciales(String query) {
        String queryLower = query.toLowerCase();
        List<ProductoEspecial> todos = allEspeciales.getAll();

        List<ProductoEspecial> filtrados = todos.stream()
                .filter(especial -> {
                    Producto producto = allProductos.getById(especial.getIdProducto());
                    if (producto != null && producto.getNombre().toLowerCase().contains(queryLower)) {
                        return true;
                    }
                    if (especial.getDescripcion() != null &&
                            especial.getDescripcion().toLowerCase().contains(queryLower)) {
                        return true;
                    }
                    // Actualizamos el formato de búsqueda para coincidir con la nueva columna
                    DateTimeFormatter fechaFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    String fechaHora = especial.getFechaInicio().format(fechaFormatter) +
                            " - " +
                            especial.getFechaFin().format(fechaFormatter);
                    return fechaHora.toLowerCase().contains(queryLower);
                })
                .toList();

        tablaEspeciales.getItems().clear();
        tablaEspeciales.getItems().addAll(filtrados);
        lblEstado.setText("🔍 " + filtrados.size() + " resultado(s) encontrado(s).");
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
                allEspeciales.eliminarDelServidor(
                        especial.getId(),
                        () -> {
                            lblEstado.setText("✅ Producto especial eliminado");
                            cargarEspeciales();
                        },
                        error -> {
                            lblEstado.setText("❌ Error al eliminar");
                            mostrarError("Error", error);
                        });
            }
        });
    }

    private String getNombreProducto(int idProducto) {
        Producto producto = allProductos.getById(idProducto);
        return producto != null ? producto.getNombre() : "Producto #" + idProducto;
    }

    private void abrirFormulario(ProductoEspecial especial) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/app/views/productos/RegistroProductoEspecial.fxml"));
            Parent root = loader.load();

            RegistroProductoEspecialController controller = loader.getController();

            if (especial != null) {
                controller.cargarDatosExistentes(especial);
            } else {
                // controller.setModoNuevo(true);
            }

            Stage stage = new Stage();
            stage.setTitle(especial == null ? "Nuevo Producto Especial" : "Editar Producto Especial");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.setOnHidden(e -> cargarEspeciales());
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el formulario: " + e.getMessage());
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}