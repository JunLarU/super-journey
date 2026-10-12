package app.controllers.ventas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import core.data.Ventas.CarritoVenta;
import core.data.Ventas.Descuento;
import core.data.Ventas.LineaVenta;
import core.services.VentaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class VentasController {

    @FXML private TextField txtBuscar;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private FlowPane flowProductos;

    @FXML private TableView<LineaVenta> tablaCarrito;
    @FXML private TableColumn<LineaVenta,String> colProducto;
    @FXML private TableColumn<LineaVenta,String> colConfiguracion;
    @FXML private TableColumn<LineaVenta,String> colCantidad;
    @FXML private TableColumn<LineaVenta,String> colPrecio;
    @FXML private TableColumn<LineaVenta,String> colExtras;
    @FXML private TableColumn<LineaVenta,String> colDescuento;
    @FXML private TableColumn<LineaVenta,String> colTotal;

    @FXML private Label lblSubtotalBruto;
    @FXML private Label lblExtras;
    @FXML private Label lblDescuentosProductos;
    @FXML private Label lblDescuentoVenta;
    @FXML private Label lblTotal;
    @FXML private Label lblStatus;
    @FXML private Label lblCantidadLineas;

    @FXML private Button btnOtraConfiguracion;
    @FXML private Button btnDescuentoLinea;
    @FXML private Button btnEliminarLinea;
    @FXML private Button btnCobrar;

    private final CarritoVenta carrito = new CarritoVenta();
    private final List<JSONObject> catalogo = new ArrayList<>();

    private long quoteVersion = 0;
    private boolean quotePending = false;

    @FXML
    public void initialize() {
        tablaCarrito.setItems(carrito.getItems());
        tablaCarrito.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tablaCarrito.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        colProducto.setCellValueFactory(d -> prop(d.getValue().getNombreProducto()));
        colConfiguracion.setCellValueFactory(d -> prop(d.getValue().getDescripcionConfiguracion()));
        colCantidad.setCellValueFactory(d -> prop(String.valueOf(d.getValue().getCantidad())));
        colPrecio.setCellValueFactory(d -> prop(money(d.getValue().getPrecioListaUnitario())));
        colExtras.setCellValueFactory(d -> prop(money(d.getValue().getTotalExtrasUnitario() * d.getValue().getCantidad())));
        colDescuento.setCellValueFactory(d -> prop(money(
                d.getValue().getDescuentoLinea() + d.getValue().getDescuentoVentaAsignado())));
        colTotal.setCellValueFactory(d -> prop(money(d.getValue().getTotalLineaFinal())));

        txtBuscar.textProperty().addListener((obs, old, value) -> renderCatalogo());
        cmbCategoria.valueProperty().addListener((obs, old, value) -> renderCatalogo());
        tablaCarrito.getSelectionModel().selectedItemProperty().addListener((obs, old, value) -> actualizarBotones());

        cargarCatalogo();
        refreshTotals();
        actualizarBotones();
    }

    private void cargarCatalogo() {
        lblStatus.setText("Cargando catálogo...");

        VentaService.catalogo(new VentaService.JsonCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                Platform.runLater(() -> {
                    catalogo.clear();
                    JSONArray a = data.optJSONArray("productos");
                    if (a != null) {
                        for (int i = 0; i < a.length(); i++) catalogo.add(a.getJSONObject(i));
                    }

                    cargarCategorias();
                    renderCatalogo();
                    lblStatus.setText("Catálogo cargado: " + catalogo.size() + " productos.");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error al cargar catálogo: " + error);
                    warn(error);
                });
            }
        });
    }

    private void cargarCategorias() {
        String old = cmbCategoria.getValue();
        List<String> cats = catalogo.stream()
                .map(p -> p.optString("Categoria", "Sin categoría"))
                .distinct()
                .sorted()
                .toList();

        cmbCategoria.getItems().setAll("Todas");
        cmbCategoria.getItems().addAll(cats);
        cmbCategoria.setValue(old != null && cmbCategoria.getItems().contains(old) ? old : "Todas");
    }

    private void renderCatalogo() {
        flowProductos.getChildren().clear();

        String search = txtBuscar.getText() == null ? "" : txtBuscar.getText().trim().toLowerCase();
        String cat = cmbCategoria.getValue();

        for (JSONObject p : catalogo) {
            String name = p.optString("Nombre", "Producto");
            String desc = p.optString("Descripcion", "");
            String category = p.optString("Categoria", "Sin categoría");

            if (!search.isEmpty()
                    && !name.toLowerCase().contains(search)
                    && !desc.toLowerCase().contains(search)
                    && !category.toLowerCase().contains(search)) {
                continue;
            }

            if (cat != null && !"Todas".equals(cat) && !cat.equals(category)) continue;

            VBox card = new VBox(7);
            card.getStyleClass().add("product-card");
            card.setPrefWidth(205);
            card.setMinHeight(150);

            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("product-name");
            nameLabel.setWrapText(true);
            nameLabel.setMaxWidth(Double.MAX_VALUE);

            Label categoryLabel = new Label(category);
            categoryLabel.getStyleClass().add("product-category");

            double price = calcularPrecioDesde(p);
            boolean conTamanos = p.optJSONArray("Tamanos") != null && p.optJSONArray("Tamanos").length() > 0;
            Label priceLabel = new Label((conTamanos ? "Desde " : "") + money(price));
            priceLabel.getStyleClass().add("product-price");

            JSONObject special = p.optJSONObject("Especial");
            if (special != null) {
                Label badge = new Label("Precio especial vigente");
                badge.getStyleClass().add("special-badge");
                card.getChildren().addAll(nameLabel, categoryLabel, priceLabel, badge);
            } else {
                card.getChildren().addAll(nameLabel, categoryLabel, priceLabel);
            }

            Button add = new Button("Agregar a la venta");
            add.getStyleClass().add("product-add-button");
            add.setMaxWidth(Double.MAX_VALUE);
            add.setOnAction(e -> abrirPersonalizacion(p.optInt("ID")));
            card.getChildren().add(add);

            if (!desc.isBlank()) Tooltip.install(card, new Tooltip(desc));
            flowProductos.getChildren().add(card);
        }

        if (flowProductos.getChildren().isEmpty()) {
            Label empty = new Label("No se encontraron productos con esos filtros.");
            empty.getStyleClass().add("empty-hint");
            flowProductos.getChildren().add(empty);
        }
    }

    private double calcularPrecioDesde(JSONObject producto) {
        double min = producto.optDouble("PrecioBase", 0);
        JSONArray sizes = producto.optJSONArray("Tamanos");

        if (sizes != null && sizes.length() > 0) {
            min = Double.MAX_VALUE;
            for (int i = 0; i < sizes.length(); i++) {
                double p = sizes.getJSONObject(i).optDouble("Precio", 0);
                if (p > 0) min = Math.min(min, p);
            }
            if (min == Double.MAX_VALUE) min = producto.optDouble("PrecioBase", 0);
        }

        JSONObject special = producto.optJSONObject("Especial");
        if (special != null) {
            double general = special.optDouble("PrecioEspecial", 0);
            if (general > 0) min = min > 0 ? Math.min(min, general) : general;

            JSONArray specialSizes = special.optJSONArray("Tamanos");
            if (specialSizes != null) {
                for (int i = 0; i < specialSizes.length(); i++) {
                    double p = specialSizes.getJSONObject(i).optDouble("PrecioEspecial", 0);
                    if (p > 0) min = min > 0 ? Math.min(min, p) : p;
                }
            }
        }

        return Math.max(0, min);
    }

    private void abrirPersonalizacion(int productId) {
        lblStatus.setText("Cargando opciones del producto...");

        VentaService.configuracionProducto(productId, new VentaService.JsonCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                Platform.runLater(() -> showCustomizationDialog(data));
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("Error: " + error);
                    warn(error);
                });
            }
        });
    }

    private void showCustomizationDialog(JSONObject config) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/ventas/PersonalizarProducto.fxml"));
            Parent root = loader.load();

            PersonalizarProductoController controller = loader.getController();
            controller.setConfiguracion(config);

            Stage stage = new Stage();
            stage.initOwner(txtBuscar.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("CAFI - Personalizar producto");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            LineaVenta line = controller.getResult();
            if (line != null) {
                carrito.getItems().add(line);
                tablaCarrito.getSelectionModel().select(line);
                tablaCarrito.scrollTo(line);
                cotizar(null);
            } else {
                lblStatus.setText("Operación cancelada.");
            }

        } catch (IOException e) {
            warn("No se pudo abrir la personalización: " + e.getMessage());
        }
    }

    @FXML
    private void onEliminarLinea() {
        LineaVenta line = tablaCarrito.getSelectionModel().getSelectedItem();
        if (line == null) {
            warn("Selecciona un producto de la venta.");
            return;
        }

        carrito.getItems().remove(line);
        cotizar(null);
    }

    @FXML
    private void onDuplicarLinea() {
        // En esta versión la acción abre la configuración del mismo producto
        // para permitir otra unidad con tamaño/ingredientes/descuento distintos.
        LineaVenta line = tablaCarrito.getSelectionModel().getSelectedItem();
        if (line == null) {
            warn("Selecciona un producto de la venta.");
            return;
        }
        abrirPersonalizacion(line.getIdProducto());
    }

    @FXML
    private void onVaciarVenta() {
        if (carrito.getItems().isEmpty()) return;

        Alert confirm = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Se eliminarán todos los productos de la venta actual.",
                ButtonType.YES,
                ButtonType.NO
        );
        confirm.setTitle("Vaciar venta");
        confirm.setHeaderText("¿Vaciar la venta actual?");

        if (confirm.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            carrito.limpiar();
            tablaCarrito.refresh();
            refreshTotals();
            actualizarBotones();
            lblStatus.setText("Venta vaciada.");
        }
    }

    @FXML
    private void onDescuentoLinea() {
        LineaVenta line = tablaCarrito.getSelectionModel().getSelectedItem();
        if (line == null) {
            warn("Selecciona un producto de la venta.");
            return;
        }

        Descuento d = pedirDescuento("Descuento del producto", line.getDescuento());
        if (d != null) {
            line.setDescuento(d);
            cotizar(null);
        }
    }

    @FXML
    private void onDescuentoVenta() {
        if (carrito.getItems().isEmpty()) {
            warn("Agrega al menos un producto antes de aplicar un descuento a la venta.");
            return;
        }

        Descuento d = pedirDescuento("Descuento de toda la venta", carrito.getDescuentoVenta());
        if (d != null) {
            carrito.setDescuentoVenta(d);
            cotizar(null);
        }
    }

    private Descuento pedirDescuento(String title, Descuento current) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(title);

        ComboBox<String> type = new ComboBox<>();
        type.getItems().setAll("Ninguno", "Porcentaje", "Monto");
        type.setValue(current.getTipo().name());
        type.setMaxWidth(Double.MAX_VALUE);

        TextField value = new TextField(String.valueOf(current.getValor()));
        value.setPromptText("0.00");
        value.setTextFormatter(new TextFormatter<>(change -> {
            String text = change.getControlNewText();
            return text.matches("\\d{0,8}([\\.,]\\d{0,2})?") ? change : null;
        }));

        TextField reason = new TextField(current.getMotivo());
        reason.setPromptText("Motivo (opcional)");

        Label help = new Label("Porcentaje: 0 a 100. Monto: importe fijo a descontar.");
        help.getStyleClass().add("empty-hint");
        help.setWrapText(true);

        VBox box = new VBox(8,
                new Label("Tipo"), type,
                new Label("Valor"), value,
                new Label("Motivo"), reason,
                help
        );
        box.setPrefWidth(360);

        Runnable update = () -> {
            boolean enabled = !"Ninguno".equals(type.getValue());
            value.setDisable(!enabled);
            reason.setDisable(!enabled);
            if (!enabled) value.setText("0");
        };
        type.valueProperty().addListener((obs, old, v) -> update.run());
        update.run();

        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return null;

        double v;
        try {
            v = value.getText() == null || value.getText().isBlank()
                    ? 0
                    : Double.parseDouble(value.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            warn("El valor del descuento no es válido.");
            return null;
        }

        if (v < 0) {
            warn("El descuento no puede ser negativo.");
            return null;
        }
        if ("Porcentaje".equals(type.getValue()) && v > 100) {
            warn("El porcentaje de descuento no puede superar 100%.");
            return null;
        }

        Descuento.Tipo t = switch (type.getValue()) {
            case "Porcentaje" -> Descuento.Tipo.Porcentaje;
            case "Monto" -> Descuento.Tipo.Monto;
            default -> Descuento.Tipo.Ninguno;
        };

        return new Descuento(t, t == Descuento.Tipo.Ninguno ? 0 : v, reason.getText());
    }

    private void cotizar(Runnable afterSuccess) {
        if (carrito.getItems().isEmpty()) {
            quoteVersion++;
            quotePending = false;
            carrito.applyQuote(new JSONObject());
            tablaCarrito.refresh();
            refreshTotals();
            actualizarBotones();
            lblStatus.setText("Agrega productos para iniciar una venta.");
            return;
        }

        long version = ++quoteVersion;
        quotePending = true;
        actualizarBotones();
        lblStatus.setText("Calculando total...");

        VentaService.cotizar(carrito.toJson(), new VentaService.JsonCallback() {
            @Override
            public void onSuccess(JSONObject data) {
                Platform.runLater(() -> {
                    if (version != quoteVersion) return;

                    quotePending = false;
                    carrito.applyQuote(data);
                    tablaCarrito.refresh();
                    refreshTotals();
                    actualizarBotones();
                    lblStatus.setText("Venta lista.");

                    if (afterSuccess != null) afterSuccess.run();
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    if (version != quoteVersion) return;
                    quotePending = false;
                    actualizarBotones();
                    lblStatus.setText("Error al calcular: " + error);
                    warn(error);
                });
            }
        });
    }

    @FXML
    private void onCobrar() {
        if (carrito.getItems().isEmpty()) {
            warn("El carrito está vacío.");
            return;
        }

        cotizar(this::abrirCobro);
    }

    private void abrirCobro() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/ventas/Cobro.fxml"));
            Parent root = loader.load();

            CobroController controller = loader.getController();
            controller.setTotal(carrito.getTotal());

            Stage stage = new Stage();
            stage.initOwner(txtBuscar.getScene().getWindow());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("CAFI - Cobro");
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            if (!controller.isConfirmed()) {
                lblStatus.setText("Cobro cancelado. La venta sigue abierta.");
                return;
            }

            JSONObject body = carrito.toJson(true, controller.getPagosJson());
            lblStatus.setText("Registrando venta...");
            btnCobrar.setDisable(true);

            VentaService.registrar(body, new VentaService.JsonCallback() {
                @Override
                public void onSuccess(JSONObject data) {
                    Platform.runLater(() -> {
                        JSONObject v = data.getJSONObject("venta");
                        info("Venta registrada correctamente\n\nFolio: "
                                + v.optString("Folio")
                                + "\nTotal: "
                                + money(v.optDouble("Total", 0)));

                        carrito.limpiar();
                        tablaCarrito.getSelectionModel().clearSelection();
                        tablaCarrito.refresh();
                        refreshTotals();
                        actualizarBotones();
                        lblStatus.setText("Venta completada. Puedes iniciar una nueva venta.");
                    });
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        actualizarBotones();
                        lblStatus.setText("No se pudo registrar la venta: " + error);
                        warn(error);
                    });
                }
            });

        } catch (IOException e) {
            warn("No se pudo abrir el cobro: " + e.getMessage());
        }
    }

    private void refreshTotals() {
        lblSubtotalBruto.setText(money(carrito.getSubtotalBruto()));
        lblExtras.setText(money(carrito.getTotalExtras()));
        lblDescuentosProductos.setText("-" + money(carrito.getDescuentoProductos()));
        lblDescuentoVenta.setText("-" + money(carrito.getDescuentoVentaCalculado()));
        lblTotal.setText(money(carrito.getTotal()));
        lblCantidadLineas.setText(carrito.getItems().size() + (carrito.getItems().size() == 1 ? " línea" : " líneas"));
    }

    private void actualizarBotones() {
        boolean hasSelection = tablaCarrito.getSelectionModel().getSelectedItem() != null;
        boolean hasItems = !carrito.getItems().isEmpty();

        if (btnOtraConfiguracion != null) btnOtraConfiguracion.setDisable(!hasSelection || quotePending);
        if (btnDescuentoLinea != null) btnDescuentoLinea.setDisable(!hasSelection || quotePending);
        if (btnEliminarLinea != null) btnEliminarLinea.setDisable(!hasSelection || quotePending);
        if (btnCobrar != null) btnCobrar.setDisable(!hasItems || quotePending);
    }

    private String money(double value) {
        return String.format("$%.2f", value);
    }

    private SimpleStringProperty prop(String value) {
        return new SimpleStringProperty(value);
    }

    private void warn(String message) {
        Alert a = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        a.setTitle("CAFI - Ventas");
        a.setHeaderText("Revisa la venta");
        a.showAndWait();
    }

    private void info(String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setTitle("CAFI - Ventas");
        a.setHeaderText("Venta completada");
        a.showAndWait();
    }
}
