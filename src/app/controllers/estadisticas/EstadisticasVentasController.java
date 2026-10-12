package app.controllers.estadisticas;

import java.time.LocalDate;

import org.json.JSONArray;
import org.json.JSONObject;

import core.services.EstadisticasVentasService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

public class EstadisticasVentasController {
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    @FXML private Label lblVentas;
    @FXML private Label lblIngresos;
    @FXML private Label lblTicketPromedio;
    @FXML private Label lblProductos;
    @FXML private Label lblDescuentos;
    @FXML private Label lblCanceladas;

    @FXML private LineChart<String,Number> chartIngresos;
    @FXML private BarChart<String,Number> chartProductos;
    @FXML private PieChart chartPagos;

    @FXML
    public void initialize() {
        dpHasta.setValue(LocalDate.now());
        dpDesde.setValue(LocalDate.now().minusDays(30));
        cargar();
    }

    @FXML
    private void onActualizar(){cargar();}

    private void cargar() {
        String d=dpDesde.getValue().toString();
        String h=dpHasta.getValue().toString();

        EstadisticasVentasService.resumen(
                d,h,
                new EstadisticasVentasService.Callback() {
                    public void onSuccess(JSONObject j) {
                        Platform.runLater(() ->
                                renderResumen(j.getJSONObject("resumen")));
                    }
                    public void onError(String e) {System.err.println(e);}
                }
        );

        EstadisticasVentasService.serieDiaria(
                d,h,
                new EstadisticasVentasService.Callback() {
                    public void onSuccess(JSONObject j) {
                        Platform.runLater(() ->
                                renderSerie(j.optJSONArray("datos")));
                    }
                    public void onError(String e) {System.err.println(e);}
                }
        );

        EstadisticasVentasService.productos(
                d,h,10,
                new EstadisticasVentasService.Callback() {
                    public void onSuccess(JSONObject j) {
                        Platform.runLater(() ->
                                renderProductos(j.optJSONArray("datos")));
                    }
                    public void onError(String e) {System.err.println(e);}
                }
        );

        EstadisticasVentasService.pagos(
                d,h,
                new EstadisticasVentasService.Callback() {
                    public void onSuccess(JSONObject j) {
                        Platform.runLater(() ->
                                renderPagos(j.optJSONArray("datos")));
                    }
                    public void onError(String e) {System.err.println(e);}
                }
        );
    }

    private void renderResumen(JSONObject r) {
        lblVentas.setText(String.valueOf(r.optInt("TotalVentas",0)));
        lblIngresos.setText(String.format("$%.2f",r.optDouble("IngresoNeto",0)));
        lblTicketPromedio.setText(
                String.format("$%.2f",r.optDouble("TicketPromedio",0))
        );
        lblProductos.setText(
                String.valueOf(r.optInt("TotalProductosVendidos",0))
        );

        double discounts=
                r.optDouble("DescuentoProductos",0)
                +r.optDouble("DescuentoVenta",0);

        lblDescuentos.setText(String.format("$%.2f",discounts));
        lblCanceladas.setText(
                String.valueOf(r.optInt("VentasCanceladas",0))
        );
    }

    private void renderSerie(JSONArray data) {
        chartIngresos.getData().clear();
        XYChart.Series<String,Number> s=new XYChart.Series<>();
        s.setName("Ingresos");

        if (data!=null) {
            for (int i=0;i<data.length();i++) {
                JSONObject r=data.getJSONObject(i);
                s.getData().add(
                        new XYChart.Data<>(
                                r.optString("Fecha"),
                                r.optDouble("Ingresos",0)
                        )
                );
            }
        }
        chartIngresos.getData().add(s);
    }

    private void renderProductos(JSONArray data) {
        chartProductos.getData().clear();
        XYChart.Series<String,Number> s=new XYChart.Series<>();
        s.setName("Unidades");

        if (data!=null) {
            for (int i=0;i<data.length();i++) {
                JSONObject r=data.getJSONObject(i);
                s.getData().add(
                        new XYChart.Data<>(
                                r.optString("NombreProducto"),
                                r.optInt("Cantidad",0)
                        )
                );
            }
        }
        chartProductos.getData().add(s);
    }

    private void renderPagos(JSONArray data) {
        chartPagos.getData().clear();
        if (data==null) return;

        for (int i=0;i<data.length();i++) {
            JSONObject r=data.getJSONObject(i);
            chartPagos.getData().add(
                    new PieChart.Data(
                            r.optString("Metodo"),
                            r.optDouble("MontoTotal",0)
                    )
            );
        }
    }
}
