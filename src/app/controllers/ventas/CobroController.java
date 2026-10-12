package app.controllers.ventas;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

public class CobroController {

    @FXML private Label lblTotal;
    @FXML private Label lblPagado;
    @FXML private Label lblRestante;
    @FXML private Label lblCambio;
    @FXML private Label lblStatus;

    @FXML private ComboBox<String> cmbMetodo;
    @FXML private TextField txtMonto;
    @FXML private TextField txtRecibido;
    @FXML private TextField txtReferencia;

    @FXML private TableView<JSONObject> tablaPagos;
    @FXML private TableColumn<JSONObject,String> colMetodo;
    @FXML private TableColumn<JSONObject,String> colMonto;
    @FXML private TableColumn<JSONObject,String> colReferencia;
    @FXML private TableColumn<JSONObject,String> colRecibido;
    @FXML private TableColumn<JSONObject,String> colCambio;

    @FXML private Button btnAgregarPago;
    @FXML private Button btnEliminarPago;
    @FXML private Button btnLimpiarPagos;
    @FXML private Button btnConfirmar;

    private final ObservableList<JSONObject> pagos = FXCollections.observableArrayList();

    private double total;
    private boolean confirmed;

    @FXML
    public void initialize() {
        cmbMetodo.getItems().setAll("Efectivo", "Tarjeta", "Transferencia", "Otro");
        cmbMetodo.setValue("Efectivo");

        tablaPagos.setItems(pagos);
        tablaPagos.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tablaPagos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colMetodo.setCellValueFactory(d -> prop(d.getValue().optString("metodo", "")));
        colMonto.setCellValueFactory(d -> prop(money(d.getValue().optDouble("monto", 0))));
        colReferencia.setCellValueFactory(d -> {
            String ref = d.getValue().optString("referencia", "").trim();
            return prop(ref.isEmpty() ? "—" : ref);
        });
        colRecibido.setCellValueFactory(d -> {
            JSONObject p = d.getValue();
            return prop(p.has("montoRecibido") ? money(p.optDouble("montoRecibido", 0)) : "—");
        });
        colCambio.setCellValueFactory(d -> {
            double cambio = d.getValue().optDouble("cambio", 0);
            return prop(cambio > 0.0001 ? money(cambio) : "—");
        });

        txtMonto.setTextFormatter(moneyFormatter());
        txtRecibido.setTextFormatter(moneyFormatter());

        cmbMetodo.valueProperty().addListener((obs, old, value) -> actualizarCamposMetodo());
        tablaPagos.getSelectionModel().selectedItemProperty().addListener((obs, old, value) ->
                btnEliminarPago.setDisable(value == null));

        tablaPagos.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) onEliminar();
        });
        tablaPagos.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && tablaPagos.getSelectionModel().getSelectedItem() != null) {
                onEliminar();
            }
        });

        txtMonto.setOnAction(e -> onAgregar());
        txtRecibido.setOnAction(e -> onAgregar());
        txtReferencia.setOnAction(e -> onAgregar());

        btnEliminarPago.setDisable(true);
        btnLimpiarPagos.setDisable(true);
        actualizarCamposMetodo();
        refresh();
    }

    public void setTotal(double total) {
        this.total = round2(Math.max(0, total));
        lblTotal.setText(money(this.total));
        txtMonto.setText(formatNumber(this.total));
        refresh();
    }

    @FXML
    private void onAgregar() {
        agregarPagoDesdeFormulario(true);
    }

    private boolean agregarPagoDesdeFormulario(boolean mostrarAvisos) {
        double restanteAntes = remaining();

        if (restanteAntes <= 0.01) {
            if (mostrarAvisos) warn("La venta ya está cubierta por completo.");
            return false;
        }

        String metodo = cmbMetodo.getValue();
        if (metodo == null || metodo.isBlank()) {
            if (mostrarAvisos) warn("Selecciona un método de pago.");
            return false;
        }

        final double amount;
        try {
            amount = round2(parseMoney(txtMonto.getText()));
        } catch (NumberFormatException e) {
            if (mostrarAvisos) warn("El monto aplicado no es válido.");
            return false;
        }

        if (amount <= 0) {
            if (mostrarAvisos) warn("El monto aplicado debe ser mayor que cero.");
            return false;
        }

        if (amount - restanteAntes > 0.01) {
            if (mostrarAvisos) {
                warn("El monto aplicado no puede superar lo que falta por pagar (" + money(restanteAntes) + ").\n"
                        + "Si es efectivo y el cliente entrega más dinero, captura ese importe en ‘Efectivo recibido’.");
            }
            return false;
        }

        JSONObject pago = new JSONObject()
                .put("metodo", metodo)
                .put("monto", amount)
                .put("referencia", txtReferencia.getText() == null ? "" : txtReferencia.getText().trim());

        if ("Efectivo".equals(metodo)) {
            final double recibido;
            try {
                recibido = txtRecibido.getText() == null || txtRecibido.getText().isBlank()
                        ? amount
                        : round2(parseMoney(txtRecibido.getText()));
            } catch (NumberFormatException e) {
                if (mostrarAvisos) warn("El efectivo recibido no es válido.");
                return false;
            }

            if (recibido + 0.001 < amount) {
                if (mostrarAvisos) warn("El efectivo recibido no puede ser menor que el monto aplicado.");
                return false;
            }

            pago.put("montoRecibido", recibido);
            pago.put("cambio", round2(recibido - amount));
        } else {
            pago.put("cambio", 0.0);
        }

        pagos.add(pago);
        tablaPagos.getSelectionModel().select(pago);
        tablaPagos.scrollTo(pago);

        txtRecibido.clear();
        txtReferencia.clear();

        refresh();
        actualizarMontoSugerido();
        return true;
    }

    @FXML
    private void onEliminar() {
        JSONObject seleccionado = tablaPagos.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            warn("Selecciona un pago de la tabla para eliminarlo.");
            return;
        }

        pagos.remove(seleccionado);
        tablaPagos.getSelectionModel().clearSelection();
        refresh();
        actualizarMontoSugerido();
    }

    @FXML
    private void onLimpiar() {
        if (pagos.isEmpty()) return;

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "¿Eliminar todos los pagos capturados?",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Limpiar pagos");
        confirm.setHeaderText("Limpiar pagos de la venta");

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            pagos.clear();
            tablaPagos.getSelectionModel().clearSelection();
            refresh();
            actualizarMontoSugerido();
        }
    }

    @FXML
    private void onConfirmar() {
        // UX: si el usuario capturó un pago pero olvidó presionar “Agregar pago”,
        // lo agregamos automáticamente antes de validar.
        if (remaining() > 0.01) {
            double pendienteFormulario = 0;
            try {
                pendienteFormulario = parseMoney(txtMonto.getText());
            } catch (Exception ignored) {
            }

            if (pendienteFormulario > 0.001) {
                if (!agregarPagoDesdeFormulario(true)) return;
            }
        }

        if (pagos.isEmpty()) {
            warn("Agrega al menos un pago.");
            return;
        }

        double restante = remaining();
        if (Math.abs(restante) > 0.01) {
            warn("Faltan " + money(Math.max(0, restante)) + " por registrar.");
            return;
        }

        confirmed = true;
        close();
    }

    @FXML
    private void onCancelar() {
        confirmed = false;
        close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public JSONArray getPagosJson() {
        JSONArray array = new JSONArray();
        pagos.forEach(array::put);
        return array;
    }

    private void actualizarCamposMetodo() {
        String metodo = cmbMetodo.getValue();
        boolean efectivo = "Efectivo".equals(metodo);

        txtRecibido.setDisable(!efectivo);
        txtRecibido.setPromptText(efectivo ? "Ej. 100.00" : "Solo para efectivo");

        boolean usaReferencia = !efectivo;
        txtReferencia.setDisable(!usaReferencia);
        txtReferencia.setPromptText(usaReferencia ? "Folio, autorización o referencia (opcional)" : "No aplica");
    }

    private void actualizarMontoSugerido() {
        double r = remaining();
        txtMonto.setText(formatNumber(Math.max(0, r)));
    }

    private double paid() {
        return round2(pagos.stream()
                .mapToDouble(p -> p.optDouble("monto", 0))
                .sum());
    }

    private double totalChange() {
        return round2(pagos.stream()
                .mapToDouble(p -> p.optDouble("cambio", 0))
                .sum());
    }

    private double remaining() {
        return round2(total - paid());
    }

    private void refresh() {
        double pagado = paid();
        double restante = remaining();
        double cambio = totalChange();

        if (lblTotal != null) lblTotal.setText(money(total));
        if (lblPagado != null) lblPagado.setText(money(pagado));
        if (lblRestante != null) lblRestante.setText(money(Math.max(0, restante)));
        if (lblCambio != null) lblCambio.setText(money(cambio));

        if (lblStatus != null) {
            if (total <= 0) {
                lblStatus.setText("Sin total de venta.");
            } else if (Math.abs(restante) <= 0.01 && !pagos.isEmpty()) {
                lblStatus.setText("Pago completo. Puedes confirmar el cobro.");
            } else if (pagos.isEmpty()) {
                lblStatus.setText("Captura el método y el monto. Puedes confirmar directamente o agregar varios pagos.");
            } else {
                lblStatus.setText("Falta por pagar: " + money(Math.max(0, restante)));
            }
        }

        if (btnLimpiarPagos != null) btnLimpiarPagos.setDisable(pagos.isEmpty());
        if (btnEliminarPago != null && pagos.isEmpty()) btnEliminarPago.setDisable(true);
    }

    private TextFormatter<String> moneyFormatter() {
        return new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("\\d{0,9}([\\.,]\\d{0,2})?") ? change : null;
        });
    }

    private double parseMoney(String raw) {
        if (raw == null || raw.isBlank()) return 0;
        return Double.parseDouble(raw.trim().replace(',', '.'));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String money(double value) {
        return String.format("$%.2f", value);
    }

    private String formatNumber(double value) {
        return String.format("%.2f", value);
    }

    private SimpleStringProperty prop(String value) {
        return new SimpleStringProperty(value);
    }

    private void close() {
        ((Stage) btnConfirmar.getScene().getWindow()).close();
    }

    private void warn(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.setTitle("Cobro");
        a.setHeaderText("Revisa la información del pago");
        a.showAndWait();
    }
}
