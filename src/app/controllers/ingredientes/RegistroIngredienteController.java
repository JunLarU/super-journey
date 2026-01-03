package app.controllers.ingredientes;

import core.data.Ingredientes.CategoriaIngrediente;
import core.data.Ingredientes.Ingrediente;
import core.services.IngredienteService;
import core.services.IngredienteService.CrudCallback;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import org.json.JSONObject;

public class RegistroIngredienteController {

    @FXML
    private TextField txtNombre;
    @FXML
    private TextField txtDescripcion;
    @FXML
    private ComboBox<CategoriaIngrediente> cbCategoria;
    @FXML
    private TextField txtCalorias;
    @FXML
    private CheckBox chkAlergeno;
    @FXML
    private Button btnRegistrar;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblTitulo;

    private Integer ingredienteIdEnEdicion = null;

    @FXML
    public void initialize() {

        cbCategoria.getItems().addAll(
                new CategoriaIngrediente(1, "Lácteos"),
                new CategoriaIngrediente(2, "Proteínas"),
                new CategoriaIngrediente(3, "Vegetales"),
                new CategoriaIngrediente(4, "Panes"),
                new CategoriaIngrediente(5, "Aderezos"),
                new CategoriaIngrediente(6, "Endulzantes"),
                new CategoriaIngrediente(7, "Lácteos Vegetales"));

        lblStatus.setText("");
    }

    /*
     * =========================
     * === GUARDAR ===
     * =========================
     */

    @FXML
    private void onRegistrarClicked() {

        // ---------- Validaciones ----------
        if (txtNombre.getText().isBlank()) {
            lblStatus.setText("⚠️ El nombre es requerido.");
            return;
        }

        if (cbCategoria.getValue() == null) {
            lblStatus.setText("⚠️ Selecciona una categoría.");
            return;
        }

        double calorias;
        try {
            calorias = Double.parseDouble(txtCalorias.getText().trim());
            if (calorias < 0) {
                lblStatus.setText("⚠️ Las calorías no pueden ser negativas.");
                return;
            }
        } catch (NumberFormatException e) {
            lblStatus.setText("⚠️ Calorías inválidas.");
            return;
        }

        // ---------- Construir JSON ----------
        JSONObject body = new JSONObject();
        body.put("nombre", txtNombre.getText().trim());
        body.put("descripcion", txtDescripcion.getText().trim());
        body.put("calorias", calorias);
        body.put("alergeno", chkAlergeno.isSelected() ? 1 : 0);

        // ✅ ID REAL DE CATEGORÍA
        CategoriaIngrediente cat = cbCategoria.getValue();
        body.put("idCategoria", cat.getId());

        if (ingredienteIdEnEdicion != null) {
            body.put("id", ingredienteIdEnEdicion);
        }

        btnRegistrar.setDisable(true);
        lblStatus.setText("Guardando...");

        IngredienteService.saveIngrediente(body, new CrudCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    lblStatus.setText(
                            ingredienteIdEnEdicion == null
                                    ? "✅ Ingrediente registrado."
                                    : "✅ Ingrediente actualizado.");
                    limpiarCampos();
                    btnRegistrar.setDisable(false);
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ " + error);
                    btnRegistrar.setDisable(false);
                });
            }
        });
    }

    /*
     * =========================
     * === LIMPIAR ===
     * =========================
     */

    private void limpiarCampos() {
        txtNombre.clear();
        txtDescripcion.clear();
        cbCategoria.setValue(null);
        txtCalorias.clear();
        chkAlergeno.setSelected(false);
        ingredienteIdEnEdicion = null;
        btnRegistrar.setText("Registrar Ingrediente");
        lblTitulo.setText("Registro de Ingrediente");
    }

    /*
     * =========================
     * === MODO EDICIÓN ===
     * =========================
     */

    public void cargarDatosExistentes(Ingrediente ingrediente) {
        txtNombre.setText(ingrediente.getNombre());
        txtDescripcion.setText(ingrediente.getDescripcion());
        for (CategoriaIngrediente c : cbCategoria.getItems()) {
            if (c.getNombre().equalsIgnoreCase(ingrediente.getCategoria())) {
                cbCategoria.setValue(c);
                break;
            }
        }

        txtCalorias.setText(String.valueOf(ingrediente.getCalorias()));
        chkAlergeno.setSelected(ingrediente.isAlergenico());

        ingredienteIdEnEdicion = ingrediente.getId();
        lblTitulo.setText("Edición de Ingrediente");
        btnRegistrar.setText("Guardar Cambios");
        lblStatus.setText("");
    }
}
