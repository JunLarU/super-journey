package app.controllers.ventas;

import java.time.LocalDate;

import org.json.JSONArray;
import org.json.JSONObject;

import core.services.VentaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

public class HistorialVentasController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TableView<JSONObject> tablaVentas;
    @FXML private TableColumn<JSONObject,String> colFolio;
    @FXML private TableColumn<JSONObject,String> colFecha;
    @FXML private TableColumn<JSONObject,String> colVendedor;
    @FXML private TableColumn<JSONObject,String> colEstado;
    @FXML private TableColumn<JSONObject,String> colDescuento;
    @FXML private TableColumn<JSONObject,String> colTotal;
    @FXML private Label lblStatus;
    @FXML private Button btnVerDetalle;
    @FXML private Button btnCancelarVenta;

    private final ObservableList<JSONObject> ventas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        dpHasta.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().minusDays(30));
        tablaVentas.setItems(ventas);
        tablaVentas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        colFolio.setCellValueFactory(d -> prop(d.getValue().optString("Folio")));
        colFecha.setCellValueFactory(d -> prop(d.getValue().optString("FechaHora")));
        colVendedor.setCellValueFactory(d -> {
            String nombre = (d.getValue().optString("Nombre") + " "
                    + d.getValue().optString("ApellidoPaterno")).trim();
            return prop(nombre.isEmpty() ? "—" : nombre);
        });
        colEstado.setCellValueFactory(d -> prop(d.getValue().optString("Estado")));
        colDescuento.setCellValueFactory(d -> prop(money(
                d.getValue().optDouble("DescuentoProductos", 0)
                        + d.getValue().optDouble("DescuentoVenta", 0))));
        colTotal.setCellValueFactory(d -> prop(money(d.getValue().optDouble("Total", 0))));

        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle("Cancelada".equalsIgnoreCase(item)
                        ? "-fx-text-fill:#e74c3c; -fx-font-weight:bold;"
                        : "-fx-text-fill:#27ae60; -fx-font-weight:bold;");
            }
        });

        tablaVentas.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> actualizarBotones());
        tablaVentas.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                    && tablaVentas.getSelectionModel().getSelectedItem() != null) {
                onVerDetalle();
            }
        });

        actualizarBotones();
        cargar();
    }

    @FXML
    private void onBuscar() {
        cargar();
    }

    @FXML
    private void onVerDetalle() {
        JSONObject venta = tablaVentas.getSelectionModel().getSelectedItem();
        if (venta == null) {
            warn("Selecciona una venta.");
            return;
        }

        btnVerDetalle.setDisable(true);
        lblStatus.setText("Cargando detalle de " + venta.optString("Folio") + "...");

        VentaService.obtener(venta.optInt("ID"), new VentaService.JsonCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                Platform.runLater(() -> {
                    actualizarBotones();
                    mostrarDetalle(data);
                    lblStatus.setText(ventas.size() + " venta(s) en el periodo.");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    actualizarBotones();
                    lblStatus.setText("Error: " + error);
                    warn(error);
                });
            }
        });
    }

    @FXML
    private void onCancelarVenta() {
        JSONObject venta = tablaVentas.getSelectionModel().getSelectedItem();
        if (venta == null) {
            warn("Selecciona una venta.");
            return;
        }
        if ("Cancelada".equalsIgnoreCase(venta.optString("Estado"))) {
            warn("La venta ya está cancelada.");
            return;
        }

        TextInputDialog reasonDialog = new TextInputDialog();
        reasonDialog.setTitle("Cancelar venta");
        reasonDialog.setHeaderText("Cancelar " + venta.optString("Folio"));
        reasonDialog.setContentText("Motivo:");

        String motivo = reasonDialog.showAndWait().orElse("").trim();
        if (motivo.isEmpty()) return;

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "La venta quedará marcada como cancelada y se conservará en el historial.",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Confirmar cancelación");
        confirm.setHeaderText("¿Cancelar esta venta?");

        if (confirm.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        btnCancelarVenta.setDisable(true);
        lblStatus.setText("Cancelando venta...");

        VentaService.cancelar(venta.optInt("ID"), motivo, "", new VentaService.JsonCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                Platform.runLater(() -> {
                    info("Venta cancelada correctamente.");
                    cargar();
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    actualizarBotones();
                    lblStatus.setText("Error: " + error);
                    warn(error);
                });
            }
        });
    }

    private void cargar() {
        if (dpDesde.getValue() == null || dpHasta.getValue() == null) {
            warn("Selecciona ambas fechas.");
            return;
        }
        if (dpDesde.getValue().isAfter(dpHasta.getValue())) {
            warn("La fecha inicial no puede ser posterior a la fecha final.");
            return;
        }

        lblStatus.setText("Cargando ventas...");

        VentaService.listar(
                dpDesde.getValue().toString(),
                dpHasta.getValue().toString(),
                new VentaService.JsonCallback() {
                    @Override
                    public void onSuccess(JSONObject data) {
                        Platform.runLater(() -> {
                            ventas.clear();
                            JSONArray a = data.optJSONArray("ventas");
                            if (a != null) {
                                for (int i = 0; i < a.length(); i++) ventas.add(a.getJSONObject(i));
                            }
                            tablaVentas.getSelectionModel().clearSelection();
                            lblStatus.setText(ventas.size() + " venta(s) en el periodo.");
                            actualizarBotones();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            lblStatus.setText("Error al cargar ventas: " + error);
                            actualizarBotones();
                        });
                    }
                }
        );
    }

    private void mostrarDetalle(JSONObject data) {
        JSONObject venta = data.optJSONObject("venta");
        JSONArray detalles = data.optJSONArray("detalles");
        JSONArray pagos = data.optJSONArray("pagos");

        StringBuilder sb = new StringBuilder();
        if (venta != null) {
            sb.append("Folio: ").append(venta.optString("Folio", "—")).append('\n');
            sb.append("Fecha: ").append(venta.optString("FechaHora", "—")).append('\n');
            sb.append("Estado: ").append(venta.optString("Estado", "—")).append('\n');
            sb.append("Subtotal: ").append(money(venta.optDouble("Subtotal", 0))).append('\n');
            sb.append("Descuentos: ").append(money(
                    venta.optDouble("DescuentoProductos", 0) + venta.optDouble("DescuentoVenta", 0))).append('\n');
            sb.append("TOTAL: ").append(money(venta.optDouble("Total", 0))).append("\n\n");
        }

        sb.append("PRODUCTOS\n");
        sb.append("----------------------------------------\n");
        if (detalles == null || detalles.length() == 0) {
            sb.append("Sin detalles.\n");
        } else {
            for (int i = 0; i < detalles.length(); i++) {
                JSONObject d = detalles.getJSONObject(i);
                sb.append(d.optInt("Cantidad", 1)).append(" x ")
                        .append(d.optString("NombreProducto", "Producto"));
                String size = d.optString("NombreTamano", "").trim();
                if (!size.isEmpty()) sb.append(" (").append(size).append(')');
                sb.append("  ").append(money(d.optDouble("TotalLineaFinal", 0))).append('\n');

                JSONArray mods = d.optJSONArray("Modificaciones");
                if (mods != null) {
                    for (int j = 0; j < mods.length(); j++) {
                        JSONObject m = mods.getJSONObject(j);
                        sb.append("   • ").append(m.optString("Descripcion", m.optString("Tipo", "Modificación")));
                        double extra = m.optDouble("CostoExtraTotal", 0);
                        if (extra > 0) sb.append("  +").append(money(extra));
                        sb.append('\n');
                    }
                }
            }
        }

        sb.append("\nPAGOS\n");
        sb.append("----------------------------------------\n");
        if (pagos == null || pagos.length() == 0) {
            sb.append("Sin pagos registrados.\n");
        } else {
            for (int i = 0; i < pagos.length(); i++) {
                JSONObject p = pagos.getJSONObject(i);
                sb.append(p.optString("Metodo", "Pago"))
                        .append(": ").append(money(p.optDouble("Monto", 0)));
                String ref = p.optString("Referencia", "").trim();
                if (!ref.isEmpty()) sb.append("  [").append(ref).append(']');
                if (p.optDouble("Cambio", 0) > 0) sb.append("  Cambio: ").append(money(p.optDouble("Cambio", 0)));
                sb.append('\n');
            }
        }

        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(false);
        area.setPrefColumnCount(55);
        area.setPrefRowCount(24);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Detalle de venta");
        dialog.setHeaderText(venta == null ? "Detalle" : venta.optString("Folio", "Detalle de venta"));
        dialog.getDialogPane().setContent(area);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void actualizarBotones() {
        JSONObject selected = tablaVentas.getSelectionModel().getSelectedItem();
        boolean has = selected != null;
        if (btnVerDetalle != null) btnVerDetalle.setDisable(!has);
        if (btnCancelarVenta != null) btnCancelarVenta.setDisable(!has || "Cancelada".equalsIgnoreCase(selected.optString("Estado")));
    }

    private String money(double value) {
        return String.format("$%.2f", value);
    }

    private SimpleStringProperty prop(String s) {
        return new SimpleStringProperty(s);
    }

    private void warn(String s) {
        Alert a = new Alert(Alert.AlertType.WARNING, s, ButtonType.OK);
        a.setTitle("Historial de ventas");
        a.setHeaderText("Revisa la selección");
        a.showAndWait();
    }

    private void info(String s) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, s, ButtonType.OK);
        a.setTitle("Historial de ventas");
        a.setHeaderText(null);
        a.showAndWait();
    }
}
