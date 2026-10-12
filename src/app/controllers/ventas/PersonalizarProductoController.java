package app.controllers.ventas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import core.data.Ventas.Descuento;
import core.data.Ventas.LineaVenta;
import core.data.Ventas.ModificacionVenta;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class PersonalizarProductoController {

    @FXML private Label lblProducto;
    @FXML private Label lblDescripcion;
    @FXML private Label lblPrecioBase;
    @FXML private ComboBox<String> cmbTamano;
    @FXML private VBox boxIngredientes;
    @FXML private VBox boxExtras;
    @FXML private Spinner<Integer> spinCantidad;
    @FXML private ComboBox<String> cmbTipoDescuento;
    @FXML private TextField txtDescuento;
    @FXML private TextField txtMotivoDescuento;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnAceptar;

    private JSONObject config;
    private JSONObject producto;
    private LineaVenta result;

    private final Map<String, JSONObject> sizesByLabel = new HashMap<>();
    private final List<ExtraRow> extraRows = new ArrayList<>();

    private static final class ExtraRow {
        final JSONObject extra;
        final CheckBox check;
        final Spinner<Integer> cantidad;

        ExtraRow(JSONObject extra, CheckBox check, Spinner<Integer> cantidad) {
            this.extra = extra;
            this.check = check;
            this.cantidad = cantidad;
        }
    }

    @FXML
    public void initialize() {
        spinCantidad.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 1));
        spinCantidad.setEditable(true);

        cmbTipoDescuento.getItems().setAll("Ninguno", "Porcentaje", "Monto");
        cmbTipoDescuento.setValue("Ninguno");

        txtDescuento.setText("0");
        txtDescuento.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("\\d{0,7}([\\.,]\\d{0,2})?") ? change : null;
        }));

        cmbTipoDescuento.valueProperty().addListener((obs, old, value) -> actualizarDescuentoUI());
        actualizarDescuentoUI();
    }

    public void setConfiguracion(JSONObject config) {
        this.config = config;
        this.producto = config.getJSONObject("producto");

        lblProducto.setText(producto.optString("Nombre", "Producto"));
        lblDescripcion.setText(producto.optString("Descripcion", ""));
        lblPrecioBase.setText("Precio base: " + String.format("$%.2f", producto.optDouble("PrecioBase", 0)));

        cargarTamanos();
        cargarIngredientes();
        cargarExtras();
    }

    private void cargarTamanos() {
        cmbTamano.getItems().clear();
        sizesByLabel.clear();

        JSONArray sizes = config.optJSONArray("tamanos");

        if (sizes == null || sizes.length() == 0) {
            cmbTamano.setDisable(true);
            cmbTamano.setPromptText("Tamaño estándar");
            return;
        }

        cmbTamano.setDisable(false);

        for (int i = 0; i < sizes.length(); i++) {
            JSONObject size = sizes.getJSONObject(i);
            String label = size.optString("Nombre", "Tamaño")
                    + String.format("  —  $%.2f", size.optDouble("Precio", 0));
            sizesByLabel.put(label, size);
            cmbTamano.getItems().add(label);
        }

        cmbTamano.getSelectionModel().selectFirst();
    }

    private void cargarIngredientes() {
        boxIngredientes.getChildren().clear();
        JSONArray ingredients = config.optJSONArray("ingredientes");

        if (ingredients == null || ingredients.length() == 0) {
            Label empty = new Label("Este producto no tiene ingredientes configurables.");
            empty.getStyleClass().add("empty-hint");
            boxIngredientes.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < ingredients.length(); i++) {
            JSONObject ing = ingredients.getJSONObject(i);
            String name = ing.optString("Nombre", "Ingrediente");
            int piId = ing.optInt("IDProductoIngrediente");

            VBox group = new VBox(7);
            group.getStyleClass().add("option-card");

            Label title = new Label(name);
            title.getStyleClass().add("option-title");
            group.getChildren().add(title);

            ToggleGroup options = new ToggleGroup();

            RadioButton normal = new RadioButton("Normal");
            normal.setToggleGroup(options);
            normal.setSelected(true);
            normal.setUserData(null);
            group.getChildren().add(normal);

            if (ing.optInt("Eliminable", 0) == 1) {
                RadioButton remove = new RadioButton("Sin " + name);
                remove.setToggleGroup(options);

                ModificacionVenta m = new ModificacionVenta();
                m.setTipo("Eliminacion");
                m.setIdProductoIngrediente(piId);
                m.setDescripcion("Sin " + name);

                remove.setUserData(m);
                group.getChildren().add(remove);
            }

            if (ing.optInt("Sustituible", 0) == 1) {
                JSONArray subs = ing.optJSONArray("Sustituciones");

                if (subs != null) {
                    for (int j = 0; j < subs.length(); j++) {
                        JSONObject sub = subs.getJSONObject(j);
                        double extra = sub.optDouble("CostoExtra", 0);
                        String subName = sub.optString("Nombre", "Sustituto");

                        RadioButton rb = new RadioButton(
                                subName + (extra > 0 ? String.format("  (+$%.2f)", extra) : "")
                        );
                        rb.setToggleGroup(options);

                        ModificacionVenta m = new ModificacionVenta();
                        m.setTipo("Sustitucion");
                        m.setIdProductoIngrediente(piId);
                        m.setIdIngredienteNuevo(sub.optInt("IDIngredienteSustituto"));
                        m.setDescripcion(name + " → " + subName);
                        m.setCostoEstimado(extra);

                        rb.setUserData(m);
                        group.getChildren().add(rb);
                    }
                }
            }

            group.setUserData(options);
            boxIngredientes.getChildren().add(group);
        }
    }

    private void cargarExtras() {
        boxExtras.getChildren().clear();
        extraRows.clear();

        JSONArray extras = config.optJSONArray("extras");
        if (extras == null || extras.length() == 0) {
            Label empty = new Label("No hay extras configurados para este producto.");
            empty.getStyleClass().add("empty-hint");
            boxExtras.getChildren().add(empty);
            return;
        }

        for (int i = 0; i < extras.length(); i++) {
            JSONObject extra = extras.getJSONObject(i);

            CheckBox cb = new CheckBox(
                    extra.optString("Nombre", "Extra")
                            + String.format("  (+$%.2f c/u)", extra.optDouble("CostoExtra", 0))
            );

            Spinner<Integer> qty = new Spinner<>(1, Math.max(1, extra.optInt("MaxCantidad", 1)), 1);
            qty.setEditable(true);
            qty.setPrefWidth(90);
            qty.setDisable(true);
            cb.selectedProperty().addListener((obs, old, value) -> qty.setDisable(!value));

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox row = new HBox(10, cb, spacer, new Label("Cantidad:"), qty);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("option-card");

            extraRows.add(new ExtraRow(extra, cb, qty));
            boxExtras.getChildren().add(row);
        }
    }

    @FXML
    private void onAceptar() {
        LineaVenta line = new LineaVenta();
        line.setIdProducto(producto.optInt("ID"));
        line.setNombreProducto(producto.optString("Nombre"));
        line.setCantidad(spinCantidad.getValue());
        line.setObservaciones(txtObservaciones.getText());

        if (!cmbTamano.isDisabled()) {
            JSONObject size = sizesByLabel.get(cmbTamano.getValue());
            if (size == null) {
                alert("Selecciona un tamaño.");
                return;
            }
            line.setIdTamanoProducto(size.optInt("ID"));
            line.setNombreTamano(size.optString("Nombre"));
        }

        for (var node : boxIngredientes.getChildren()) {
            if (!(node instanceof VBox group)) continue;
            Object ud = group.getUserData();
            if (!(ud instanceof ToggleGroup tg) || tg.getSelectedToggle() == null) continue;

            Object data = tg.getSelectedToggle().getUserData();
            if (data instanceof ModificacionVenta m) line.getModificaciones().add(m);
        }

        for (ExtraRow row : extraRows) {
            if (!row.check.isSelected()) continue;

            ModificacionVenta m = new ModificacionVenta();
            m.setTipo("Extra");
            m.setIdExtraProductoIngrediente(row.extra.optInt("ID"));
            m.setCantidad(row.cantidad.getValue());
            m.setDescripcion("Extra " + row.extra.optString("Nombre", ""));
            m.setCostoEstimado(row.extra.optDouble("CostoExtra", 0) * row.cantidad.getValue());
            line.getModificaciones().add(m);
        }

        double discountValue;
        try {
            discountValue = parseNumber(txtDescuento.getText());
        } catch (NumberFormatException e) {
            alert("El descuento no es válido.");
            return;
        }

        Descuento.Tipo type = switch (cmbTipoDescuento.getValue()) {
            case "Porcentaje" -> Descuento.Tipo.Porcentaje;
            case "Monto" -> Descuento.Tipo.Monto;
            default -> Descuento.Tipo.Ninguno;
        };

        if (discountValue < 0) {
            alert("El descuento no puede ser negativo.");
            return;
        }
        if (type == Descuento.Tipo.Porcentaje && discountValue > 100) {
            alert("El porcentaje de descuento no puede ser mayor a 100%.");
            return;
        }

        if (type == Descuento.Tipo.Ninguno) discountValue = 0;

        line.setDescuento(new Descuento(type, discountValue, txtMotivoDescuento.getText()));

        result = line;
        close();
    }

    @FXML
    private void onCancelar() {
        result = null;
        close();
    }

    public LineaVenta getResult() {
        return result;
    }

    private void actualizarDescuentoUI() {
        boolean enabled = !"Ninguno".equals(cmbTipoDescuento.getValue());
        txtDescuento.setDisable(!enabled);
        txtMotivoDescuento.setDisable(!enabled);
        if (!enabled) {
            txtDescuento.setText("0");
            txtMotivoDescuento.clear();
        }
    }

    private double parseNumber(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        return Double.parseDouble(raw.trim().replace(',', '.'));
    }

    private void close() {
        ((Stage) btnAceptar.getScene().getWindow()).close();
    }

    private void alert(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.setTitle("Personalizar producto");
        a.setHeaderText("Revisa la configuración");
        a.showAndWait();
    }
}
