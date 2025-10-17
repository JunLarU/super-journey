package app.controllers.ingredientes;

import core.data.Ingredientes.AllIngredientes;
import core.data.Ingredientes.Ingrediente;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controlador del formulario de registro de ingredientes.
 * Trabaja con el sistema local AllIngredientes/Ingrediente.
 */
public class RegistroIngredienteController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<String> cbCategoria;
    @FXML private TextField txtCalorias;
    @FXML private CheckBox chkAlergeno;
    @FXML private Button btnRegistrar;
    @FXML private Label lblStatus;
    @FXML private Label lblTitulo;

    private final AllIngredientes allIngredientes = AllIngredientes.getInstance();
    private Integer ingredienteIdEnEdicion = null; // Para modo edición

    @FXML
    public void initialize() {
        // Cargar categorías
        cbCategoria.getItems().addAll(
                "Lácteos",
                "Proteínas",
                "Vegetales",
                "Panes",
                "Aderezos",
                "Endulzantes",
                "Lácteos Vegetales"
        );
        lblStatus.setText("");
    }

    @FXML
    private void onRegistrarClicked() {
        // Validaciones
        if (txtNombre.getText().isBlank()) {
            lblStatus.setText("⚠️ El nombre es requerido.");
            return;
        }

        if (cbCategoria.getValue() == null) {
            lblStatus.setText("⚠️ Selecciona una categoría.");
            return;
        }

        if (txtCalorias.getText().isBlank()) {
            lblStatus.setText("⚠️ Las calorías son requeridas.");
            return;
        }

        // Validar que calorías sea un número válido
        double calorias;
        try {
            calorias = Double.parseDouble(txtCalorias.getText().trim());
            if (calorias < 0) {
                lblStatus.setText("⚠️ Las calorías no pueden ser negativas.");
                return;
            }
        } catch (NumberFormatException e) {
            lblStatus.setText("⚠️ Ingresa un valor numérico válido para calorías.");
            return;
        }

        btnRegistrar.setDisable(true);
        lblStatus.setText("Guardando...");

        // Procesar en segundo plano para mantener UI responsive
        new Thread(() -> {
            try {
                String nombre = txtNombre.getText().trim();
                String descripcion = txtDescripcion.getText().trim();
                String categoria = cbCategoria.getValue();
                boolean esAlergeno = chkAlergeno.isSelected();

                if (ingredienteIdEnEdicion != null) {
                    // 🔧 Modo edición
                    Ingrediente ingrediente = new Ingrediente(
                            ingredienteIdEnEdicion,
                            nombre,
                            categoria,
                            descripcion,
                            calorias,
                            esAlergeno
                    );
                    allIngredientes.updateIngrediente(ingrediente);

                    Platform.runLater(() -> {
                        lblStatus.setText("✅ Ingrediente actualizado correctamente.");
                        limpiarCampos();
                        btnRegistrar.setDisable(false);
                    });

                } else {
                    // ➕ Modo nuevo
                    // Verificar que no exista un ingrediente con el mismo nombre
                    Ingrediente existente = allIngredientes.getByNombre(nombre);
                    if (existente != null) {
                        Platform.runLater(() -> {
                            lblStatus.setText("⚠️ Ya existe un ingrediente con ese nombre.");
                            btnRegistrar.setDisable(false);
                        });
                        return;
                    }

                    Ingrediente nuevoIngrediente = new Ingrediente(
                            0, // AllIngredientes asignará el ID automáticamente
                            nombre,
                            categoria,
                            descripcion,
                            calorias,
                            esAlergeno
                    );
                    allIngredientes.addIngrediente(nuevoIngrediente);

                    Platform.runLater(() -> {
                        lblStatus.setText("✅ Ingrediente registrado correctamente.");
                        limpiarCampos();
                        btnRegistrar.setDisable(false);
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al guardar el ingrediente: " + e.getMessage());
                    btnRegistrar.setDisable(false);
                });
            }
        }).start();
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtDescripcion.clear();
        cbCategoria.setValue(null);
        txtCalorias.clear();
        chkAlergeno.setSelected(false);
        ingredienteIdEnEdicion = null;
        btnRegistrar.setText("Registrar Ingrediente");
        lblStatus.setText("");
    }

    /**
     * ✅ Modo edición: carga los datos de un ingrediente existente
     * @param ingrediente El ingrediente a editar
     */
    public void cargarDatosExistentes(Ingrediente ingrediente) {
        txtNombre.setText(ingrediente.getNombre());
        txtDescripcion.setText(ingrediente.getDescripcion());
        cbCategoria.setValue(ingrediente.getcategoria());
        txtCalorias.setText(String.valueOf(ingrediente.getCalorias()));
        chkAlergeno.setSelected(ingrediente.isAlergenico());
        lblTitulo.setText("Edición de Ingrediente");
        btnRegistrar.setText("Guardar Cambios");
        lblStatus.setText("");

        // Guardar ID para saber que estamos en modo edición
        ingredienteIdEnEdicion = ingrediente.getId();
    }
}