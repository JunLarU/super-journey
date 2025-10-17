// RegistroProductoEspecialController.java
package app.controllers.productos;

import core.SessionManager;
import core.data.Productos.AllProductosEspeciales;
import core.data.Productos.ProductoEspecial;
import core.data.Productos.AllProductos;
import core.data.Productos.Producto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para el formulario de registro/edición de productos especiales
 */
public class RegistroProductoEspecialController {

    
    // COMPONENTES FXML
    
    @FXML private Label lblTitulo;
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtDescripcion;
    @FXML private TextField txtPrecioEspecial;
    @FXML private DatePicker dtpFechaInicio;
    @FXML private DatePicker dtpFechaFin;
    @FXML private Spinner<Integer> spnHoraInicio;
    @FXML private Spinner<Integer> spnMinutoInicio;
    @FXML private Spinner<Integer> spnHoraFin;
    @FXML private Spinner<Integer> spnMinutoFin;
    @FXML private CheckBox chkActivo;
    @FXML private Button btnGuardar;
    @FXML private Button btnCancelar;
    @FXML private Label lblStatus;

    
    // MODELOS Y DATOS
    
    private final AllProductosEspeciales allEspeciales = AllProductosEspeciales.getInstance();
    private final AllProductos allProductos = AllProductos.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private boolean modoEdicion = false;
    private ProductoEspecial productoEspecialEditando = null;

    private final ObservableList<Producto> productosDisponibles = FXCollections.observableArrayList();

    
    // INICIALIZACIÓN
    
    @FXML
    public void initialize() {
        // Verificar permisos de administrador
        if (!session.isAdmin()) {
            mostrarAlerta("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        configurarControles();
        cargarProductos();
        configurarValoresPorDefecto();
    }

    private void configurarControles() {
        // Configurar spinners de hora y minuto
        spnHoraInicio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        spnMinutoInicio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        spnHoraFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        spnMinutoFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Configurar DatePickers con valores por defecto
        dtpFechaInicio.setValue(LocalDate.now());
        dtpFechaFin.setValue(LocalDate.now().plusDays(7));

        // Configurar ComboBox de productos
        cmbProducto.setConverter(new StringConverter<Producto>() {
            @Override
            public String toString(Producto producto) {
                return producto != null ? 
                    String.format("%s - $%.2f", producto.getNombre(), producto.getPrecioBase()) : 
                    "";
            }

            @Override
            public Producto fromString(String string) {
                return null; // No necesario para display
            }
        });

        // Configurar validación de precio
        txtPrecioEspecial.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                txtPrecioEspecial.setText(oldVal);
            }
        });

        // Establecer tooltips
        txtDescripcion.setTooltip(new Tooltip("Descripción opcional de la promoción"));
        txtPrecioEspecial.setTooltip(new Tooltip("Precio especial durante el periodo seleccionado"));
    }

    private void configurarValoresPorDefecto() {
        chkActivo.setSelected(true);
        lblStatus.setText("Editar Completa los campos para crear un producto especial");
    }

    private void cargarProductos() {
        lblStatus.setText("Cargando productos...");
        
        new Thread(() -> {
            try {
                List<Producto> productos = allProductos.getAll().stream()
                    .filter(Producto::isDisponible)
                    .collect(Collectors.toList());
                
                Platform.runLater(() -> {
                    productosDisponibles.clear();
                    productosDisponibles.addAll(productos);
                    cmbProducto.setItems(productosDisponibles);
                    lblStatus.setText("✅ " + productos.size() + " productos cargados");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar productos: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    
    // MÉTODOS DE ACCIÓN
    
    @FXML
    private void onGuardarClicked() {
        if (!validarFormulario()) return;

        btnGuardar.setDisable(true);
        lblStatus.setText("Guardando producto especial...");

        new Thread(() -> {
            try {
                Producto producto = cmbProducto.getValue();
                String descripcion = txtDescripcion.getText().trim();
                double precioEspecial = Double.parseDouble(txtPrecioEspecial.getText().trim());
                boolean activo = chkActivo.isSelected();

                // Crear LocalDateTime para inicio y fin
                LocalDateTime fechaInicio = LocalDateTime.of(
                    dtpFechaInicio.getValue(),
                    LocalTime.of(spnHoraInicio.getValue(), spnMinutoInicio.getValue())
                );
                
                LocalDateTime fechaFin = LocalDateTime.of(
                    dtpFechaFin.getValue(),
                    LocalTime.of(spnHoraFin.getValue(), spnMinutoFin.getValue())
                );

                // Validar que la fecha de fin sea posterior a la de inicio
                if (fechaFin.isBefore(fechaInicio)) {
                    Platform.runLater(() -> {
                        lblStatus.setText("❌ La fecha/hora de fin debe ser posterior a la de inicio");
                        btnGuardar.setDisable(false);
                    });
                    return;
                }

                if (modoEdicion && productoEspecialEditando != null) {
                    // Modo edición
                    productoEspecialEditando.setIdProducto(producto.getId());
                    productoEspecialEditando.setDescripcion(descripcion);
                    productoEspecialEditando.setPrecioEspecial(precioEspecial);
                    productoEspecialEditando.setFechaInicio(fechaInicio);
                    productoEspecialEditando.setFechaFin(fechaFin);
                    productoEspecialEditando.setActivo(activo);

                    allEspeciales.updateProductoEspecial(productoEspecialEditando);

                    Platform.runLater(() -> {
                        lblStatus.setText("✅ Producto especial actualizado correctamente");
                        cerrarVentana();
                    });
                } else {
                    // Modo nuevo - verificar que no exista conflicto
                    ProductoEspecial existente = allEspeciales.getEspecialParaProductoYFecha(
                        producto.getId(), fechaInicio);
                    
                    if (existente != null && existente.estaActivoParaFechaHora(fechaInicio)) {
                        Platform.runLater(() -> {
                            lblStatus.setText("⚠️ Ya existe un producto especial para este producto en las fechas seleccionadas");
                            btnGuardar.setDisable(false);
                        });
                        return;
                    }

                    // Crear nuevo producto especial
                    ProductoEspecial nuevoEspecial = new ProductoEspecial(
                        0, // ID se asignará automáticamente
                        producto.getId(),
                        fechaInicio,
                        fechaFin,
                        descripcion,
                        precioEspecial,
                        activo
                    );

                    allEspeciales.addProductoEspecial(nuevoEspecial);

                    Platform.runLater(() -> {
                        lblStatus.setText("✅ Producto especial creado correctamente");
                        limpiarCampos();
                        btnGuardar.setDisable(false);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al guardar: " + e.getMessage());
                    btnGuardar.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onCancelarClicked() {
        cerrarVentana();
    }

    
    // VALIDACIÓN
    
    private boolean validarFormulario() {
        // Validar producto seleccionado
        if (cmbProducto.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona un producto");
            cmbProducto.requestFocus();
            return false;
        }

        // Validar precio especial
        if (txtPrecioEspecial.getText().trim().isEmpty()) {
            mostrarAlerta("⚠️ Campo requerido", "Ingresa el precio especial");
            txtPrecioEspecial.requestFocus();
            return false;
        }

        try {
            double precio = Double.parseDouble(txtPrecioEspecial.getText().trim());
            if (precio <= 0) {
                mostrarAlerta("⚠️ Precio inválido", "El precio debe ser mayor a cero");
                txtPrecioEspecial.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("⚠️ Formato inválido", "El precio debe ser un número válido");
            txtPrecioEspecial.requestFocus();
            return false;
        }

        // Validar fechas
        if (dtpFechaInicio.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona la fecha de inicio");
            dtpFechaInicio.requestFocus();
            return false;
        }

        if (dtpFechaFin.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona la fecha de fin");
            dtpFechaFin.requestFocus();
            return false;
        }

        return true;
    }

    
    // CARGAR DATOS EXISTENTES
    
    public void cargarDatosExistentes(ProductoEspecial productoEspecial) {
        if (productoEspecial == null) return;

        modoEdicion = true;
        productoEspecialEditando = productoEspecial;

        // Configurar título
        lblTitulo.setText("Editar Producto Especial");

        // Buscar y seleccionar el producto
        Producto producto = allProductos.getById(productoEspecial.getIdProducto());
        if (producto != null) {
            cmbProducto.setValue(producto);
        }

        // Cargar datos existentes
        txtDescripcion.setText(productoEspecial.getDescripcion());
        txtPrecioEspecial.setText(String.valueOf(productoEspecial.getPrecioEspecial()));
        chkActivo.setSelected(productoEspecial.isActivo());

        // Cargar fechas y horas
        dtpFechaInicio.setValue(productoEspecial.getFechaInicio().toLocalDate());
        dtpFechaFin.setValue(productoEspecial.getFechaFin().toLocalDate());
        
        spnHoraInicio.getValueFactory().setValue(productoEspecial.getFechaInicio().getHour());
        spnMinutoInicio.getValueFactory().setValue(productoEspecial.getFechaInicio().getMinute());
        spnHoraFin.getValueFactory().setValue(productoEspecial.getFechaFin().getHour());
        spnMinutoFin.getValueFactory().setValue(productoEspecial.getFechaFin().getMinute());

        // Actualizar UI
        btnGuardar.setText("💾 Actualizar");
        lblStatus.setText("📝 Editando producto especial #" + productoEspecial.getId());
    }

    
    // MODO VISUALIZACIÓN
    
    public void visualizarProductoEspecial(ProductoEspecial productoEspecial) {
        cargarDatosExistentes(productoEspecial);
        
        // Deshabilitar todos los controles
        cmbProducto.setDisable(true);
        txtDescripcion.setDisable(true);
        txtPrecioEspecial.setDisable(true);
        dtpFechaInicio.setDisable(true);
        dtpFechaFin.setDisable(true);
        spnHoraInicio.setDisable(true);
        spnMinutoInicio.setDisable(true);
        spnHoraFin.setDisable(true);
        spnMinutoFin.setDisable(true);
        chkActivo.setDisable(true);
        
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        lblTitulo.setText("visualizar Producto Especial");
        lblStatus.setText("visualizando producto especial #" + productoEspecial.getId());
    }

    
    // UTILIDADES
    
    private void limpiarCampos() {
        cmbProducto.setValue(null);
        txtDescripcion.clear();
        txtPrecioEspecial.clear();
        dtpFechaInicio.setValue(LocalDate.now());
        dtpFechaFin.setValue(LocalDate.now().plusDays(7));
        spnHoraInicio.getValueFactory().setValue(8);
        spnMinutoInicio.getValueFactory().setValue(0);
        spnHoraFin.getValueFactory().setValue(20);
        spnMinutoFin.getValueFactory().setValue(0);
        chkActivo.setSelected(true);
        
        modoEdicion = false;
        productoEspecialEditando = null;
        btnGuardar.setText("💾 Guardar");
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}