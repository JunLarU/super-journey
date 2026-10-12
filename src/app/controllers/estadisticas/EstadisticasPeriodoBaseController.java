package app.controllers.estadisticas;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public abstract class EstadisticasPeriodoBaseController {

    private static final String[] PALETTE = {
            "#3498DB", "#2ECC71", "#9B59B6", "#F39C12",
            "#E74C3C", "#1ABC9C", "#34495E", "#E67E22",
            "#16A085", "#8E44AD"
    };

    @FXML protected Label lblIngresos;
    @FXML protected Label lblVentas;
    @FXML protected Label lblTicket;
    @FXML protected Label lblProductos;
    @FXML protected Label lblDescuentos;
    @FXML protected Label lblCancelaciones;

    @FXML protected Label lblIngresosDetalle;
    @FXML protected Label lblVentasDetalle;
    @FXML protected Label lblTicketDetalle;
    @FXML protected Label lblProductosDetalle;
    @FXML protected Label lblDescuentosDetalle;
    @FXML protected Label lblCancelacionesDetalle;

    @FXML protected Label lblMediana;
    @FXML protected Label lblDesviacion;
    @FXML protected Label lblRangoTicket;
    @FXML protected Label lblExtras;
    @FXML protected Label lblEspeciales;
    @FXML protected Label lblVariabilidad;

    @FXML protected Label lblMejorProducto;
    @FXML protected Label lblMejorVendedor;
    @FXML protected Label lblHoraPico;
    @FXML protected Label lblCategoriaLider;
    @FXML protected Label lblMetodoPago;
    @FXML protected Label lblConcentracion;

    @FXML protected LineChart<String, Number> chartIngresos;
    @FXML protected BarChart<String, Number> chartProductos;
    @FXML protected BarChart<String, Number> chartVendedores;
    @FXML protected PieChart chartCategorias;
    @FXML protected PieChart chartPagos;
    @FXML protected BarChart<String, Number> chartHorarios;
    @FXML protected BarChart<String, Number> chartModificaciones;
    @FXML protected BarChart<String, Number> chartTamanos;

    @FXML
    public void initialize() {
        chartIngresos.setAnimated(false);
        chartProductos.setAnimated(false);
        chartVendedores.setAnimated(false);
        chartCategorias.setAnimated(false);
        chartPagos.setAnimated(false);
        chartHorarios.setAnimated(false);
        chartModificaciones.setAnimated(false);
        chartTamanos.setAnimated(false);

        chartIngresos.setCreateSymbols(true);
        chartProductos.setLegendVisible(false);
        chartVendedores.setLegendVisible(false);
        chartHorarios.setLegendVisible(false);
        chartModificaciones.setLegendVisible(false);
        chartTamanos.setLegendVisible(false);
    }

    public void actualizarDatos(JSONObject data) {
        JSONObject resumen = data.optJSONObject("resumen");
        JSONObject comparacion = data.optJSONObject("comparacion");
        JSONObject insights = data.optJSONObject("insights");

        if (resumen == null) resumen = new JSONObject();
        if (comparacion == null) comparacion = new JSONObject();
        if (insights == null) insights = new JSONObject();

        renderResumen(resumen, comparacion);
        renderInsights(insights);

        renderSerie(data.optJSONArray("serie"));
        renderBar(
                chartProductos,
                data.optJSONArray("productos"),
                "NombreProducto",
                "Cantidad",
                "Unidades vendidas"
        );
        renderBar(
                chartVendedores,
                data.optJSONArray("vendedores"),
                "Vendedor",
                "IngresoTotal",
                "Ingresos"
        );
        renderPie(
                chartCategorias,
                data.optJSONArray("categorias"),
                "Categoria",
                "IngresoNeto"
        );
        renderPie(
                chartPagos,
                data.optJSONArray("pagos"),
                "Metodo",
                "MontoTotal"
        );
        renderBar(
                chartHorarios,
                data.optJSONArray("horarios"),
                "Hora",
                "Ventas",
                "Ventas"
        );
        renderBar(
                chartModificaciones,
                data.optJSONArray("modificaciones"),
                "Descripcion",
                "Cantidad",
                "Solicitudes"
        );
        renderSizes(data.optJSONArray("tamanos"));
    }

    private void renderResumen(JSONObject r, JSONObject c) {
        lblIngresos.setText(money(r.optDouble("IngresoNeto", 0)));
        lblVentas.setText(integer(r.optDouble("TotalVentas", 0)));
        lblTicket.setText(money(r.optDouble("TicketPromedio", 0)));
        lblProductos.setText(integer(r.optDouble("TotalProductosVendidos", 0)));

        double discounts =
                r.optDouble("DescuentoProductos", 0)
                        + r.optDouble("DescuentoVenta", 0);

        lblDescuentos.setText(money(discounts));
        lblCancelaciones.setText(integer(r.optDouble("VentasCanceladas", 0)));

        lblIngresosDetalle.setText(changeText(c, "IngresosPct"));
        lblVentasDetalle.setText(changeText(c, "VentasPct"));

        Double ticketChange = nullableDouble(c, "TicketPct");
        lblTicketDetalle.setText(
                "Mediana " + money(r.optDouble("MedianaTicket", 0))
                        + (ticketChange == null ? "" : " · " + signedPercent(ticketChange))
        );

        lblProductosDetalle.setText(
                String.format(
                        Locale.US,
                        "%.2f por venta",
                        r.optDouble("PromedioProductosPorVenta", 0)
                )
        );

        lblDescuentosDetalle.setText(
                String.format(
                        Locale.US,
                        "%.2f%% del bruto",
                        r.optDouble("TasaDescuento", 0)
                )
        );

        lblCancelacionesDetalle.setText(
                String.format(
                        Locale.US,
                        "%.2f%% de operaciones",
                        r.optDouble("TasaCancelacion", 0)
                )
        );

        lblMediana.setText(money(r.optDouble("MedianaTicket", 0)));
        lblDesviacion.setText(money(r.optDouble("DesviacionTicket", 0)));
        lblRangoTicket.setText(
                money(r.optDouble("TicketMinimo", 0))
                        + " – "
                        + money(r.optDouble("TicketMaximo", 0))
        );
        lblExtras.setText(money(r.optDouble("TotalExtras", 0)));
        lblEspeciales.setText(
                integer(r.optDouble("ProductosEspecialesVendidos", 0))
                        + " ("
                        + format2(r.optDouble("PorcentajeEspeciales", 0))
                        + "%)"
        );
        lblVariabilidad.setText(
                format2(r.optDouble("VariabilidadIngresosPct", 0)) + "%"
        );
    }

    private void renderInsights(JSONObject i) {
        lblMejorProducto.setText(
                i.optString("MejorProducto", "Sin datos")
                        + " · "
                        + integer(i.optDouble("MejorProductoCantidad", 0))
                        + " u."
        );

        lblMejorVendedor.setText(
                i.optString("MejorVendedor", "Sin datos")
                        + " · "
                        + money(i.optDouble("MejorVendedorIngreso", 0))
        );

        lblHoraPico.setText(
                i.optString("HoraPico", "Sin datos")
                        + " · "
                        + integer(i.optDouble("HoraPicoVentas", 0))
                        + " ventas"
        );

        lblCategoriaLider.setText(
                i.optString("CategoriaLider", "Sin datos")
        );

        lblMetodoPago.setText(
                i.optString("MetodoPagoPreferido", "Sin datos")
        );

        lblConcentracion.setText(
                format2(i.optDouble("ConcentracionTop3Pct", 0))
                        + "% de unidades en Top 3"
        );
    }

    private void renderSerie(JSONArray arr) {
        chartIngresos.getData().clear();

        XYChart.Series<String, Number> ingresos = new XYChart.Series<>();
        ingresos.setName("Ingresos");

        if (arr != null) {
            for (int x = 0; x < arr.length(); x++) {
                JSONObject o = arr.optJSONObject(x);
                if (o == null) continue;

                ingresos.getData().add(
                        new XYChart.Data<>(
                                o.optString("Etiqueta", ""),
                                o.optDouble("Ingresos", 0)
                        )
                );
            }
        }

        chartIngresos.getData().add(ingresos);

        Platform.runLater(() -> {
            if (ingresos.getNode() != null) {
                Node line = ingresos.getNode().lookup(".chart-series-line");
                if (line != null) {
                    line.setStyle("-fx-stroke: #3498DB; -fx-stroke-width: 3px;");
                }
            }

            for (XYChart.Data<String, Number> point : ingresos.getData()) {
                if (point.getNode() != null) {
                    point.getNode().setStyle(
                            "-fx-background-color: #3498DB, white;"
                    );
                    Tooltip.install(
                            point.getNode(),
                            new Tooltip(
                                    point.getXValue()
                                            + "\nIngresos: "
                                            + money(point.getYValue().doubleValue())
                            )
                    );
                }
            }
        });
    }

    private void renderBar(
            BarChart<String, Number> chart,
            JSONArray arr,
            String labelKey,
            String valueKey,
            String seriesName
    ) {
        chart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(seriesName);

        if (arr != null) {
            for (int x = 0; x < arr.length(); x++) {
                JSONObject o = arr.optJSONObject(x);
                if (o == null) continue;

                String label = cleanLabel(
                        o.optString(labelKey, "Sin dato"),
                        24
                );
                double value = o.optDouble(valueKey, 0);

                if (value > 0) {
                    series.getData().add(
                            new XYChart.Data<>(label, value)
                    );
                }
            }
        }

        chart.getData().add(series);

        Platform.runLater(() -> colorBars(chart, series, valueKey));
    }

    private void renderSizes(JSONArray arr) {
        chartTamanos.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Unidades");

        if (arr != null) {
            for (int x = 0; x < arr.length(); x++) {
                JSONObject o = arr.optJSONObject(x);
                if (o == null) continue;

                String label =
                        o.optString("NombreProducto", "Producto")
                                + " · "
                                + o.optString("NombreTamano", "Estándar");

                double value = o.optDouble("Cantidad", 0);

                if (value > 0) {
                    series.getData().add(
                            new XYChart.Data<>(
                                    cleanLabel(label, 24),
                                    value
                            )
                    );
                }
            }
        }

        chartTamanos.getData().add(series);
        Platform.runLater(() -> colorBars(chartTamanos, series, "Cantidad"));
    }

    private void renderPie(
            PieChart chart,
            JSONArray arr,
            String labelKey,
            String valueKey
    ) {
        chart.getData().clear();

        if (arr != null) {
            for (int x = 0; x < arr.length(); x++) {
                JSONObject o = arr.optJSONObject(x);
                if (o == null) continue;

                double value = o.optDouble(valueKey, 0);
                if (value <= 0) continue;

                chart.getData().add(
                        new PieChart.Data(
                                cleanLabel(o.optString(labelKey, "Sin dato"), 28),
                                value
                        )
                );
            }
        }

        if (chart.getData().isEmpty()) {
            chart.setData(FXCollections.observableArrayList());
            return;
        }

        Platform.runLater(() -> {
            for (int x = 0; x < chart.getData().size(); x++) {
                PieChart.Data d = chart.getData().get(x);
                if (d.getNode() != null) {
                    d.getNode().setStyle(
                            "-fx-pie-color: "
                                    + PALETTE[x % PALETTE.length]
                                    + ";"
                    );
                    Tooltip.install(
                            d.getNode(),
                            new Tooltip(
                                    d.getName()
                                            + "\n"
                                            + money(d.getPieValue())
                            )
                    );
                }
            }
        });
    }

    private void colorBars(
            BarChart<String, Number> chart,
            XYChart.Series<String, Number> series,
            String valueKey
    ) {
        int x = 0;
        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                String color = PALETTE[x % PALETTE.length];
                d.getNode().setStyle(
                        "-fx-bar-fill: " + color + ";"
                );

                String value = d.getYValue().doubleValue() % 1 == 0
                        ? integer(d.getYValue().doubleValue())
                        : format2(d.getYValue().doubleValue());

                Tooltip.install(
                        d.getNode(),
                        new Tooltip(
                                d.getXValue()
                                        + "\n"
                                        + series.getName()
                                        + ": "
                                        + value
                        )
                );
            }
            x++;
        }
    }

    private String changeText(JSONObject c, String key) {
        Double value = nullableDouble(c, key);

        if (value == null) {
            return "Sin base comparable";
        }

        if (Math.abs(value) < 0.005) {
            return "Sin cambio vs. período anterior";
        }

        return signedPercent(value) + " vs. período anterior";
    }

    private String signedPercent(double value) {
        String sign = value > 0 ? "+" : "";
        return sign + format2(value) + "%";
    }

    private Double nullableDouble(JSONObject object, String key) {
        Object raw = object.opt(key);

        if (raw == null || raw == JSONObject.NULL) {
            return null;
        }

        if (raw instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String money(double value) {
        return String.format(Locale.US, "$%,.2f", value);
    }

    private String integer(double value) {
        return String.format(Locale.US, "%,.0f", value);
    }

    private String format2(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private String cleanLabel(String value, int max) {
        if (value == null || value.isBlank()) {
            return "Sin dato";
        }

        String clean = value.trim();
        if (clean.length() <= max) {
            return clean;
        }

        return clean.substring(0, Math.max(1, max - 1)) + "…";
    }
}
