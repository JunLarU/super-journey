package app.controllers.estadisticas;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class EstadisticasHoyController {

    @FXML
    private Label lblTotalUsuariosHoy, lblTotalProductosHoy, lblTotalAvisosHoy,
            lblTotalMenusHoy, lblTotalCategoriasHoy, lblTotalIngredientesHoy;
    @FXML
    private PieChart chartUsuariosTipoHoy;
    @FXML
    private BarChart<String, Number> chartProductosCategoriaHoy;
    @FXML
    private PieChart chartAvisosEstablecimientoHoy;
    @FXML
    private BarChart<String, Number> chartMenusDiaHoy;
    @FXML
    private PieChart chartProductosEstadoHoy;

    public void actualizarDatos(JSONObject data) {
        try {
            System.out.println("Actualizando datos para " + getClass().getSimpleName());
            System.out.println("Datos recibidos: " + data.toString(2));
            actualizarLabels(data);
            actualizarCharts(data);
        } catch (Exception e) {
            System.err.println("Error en actualizarDatos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarLabels(JSONObject data) {
        try {
            if (data.has("usuarios")) {
                JSONObject usuarios = data.getJSONObject("usuarios");
                int valor = usuarios.optInt("total_hoy", 0);
                lblTotalUsuariosHoy.setText(String.valueOf(valor));
            }

            if (data.has("productos")) {
                JSONObject productos = data.getJSONObject("productos");
                int valor = productos.optInt("total_hoy", 0);
                lblTotalProductosHoy.setText(String.valueOf(valor));
            }

            if (data.has("avisos")) {
                JSONObject avisos = data.getJSONObject("avisos");
                int valor = avisos.optInt("total_hoy", 0);
                lblTotalAvisosHoy.setText(String.valueOf(valor));
            }

            if (data.has("menus")) {
                JSONObject menus = data.getJSONObject("menus");
                int valor = menus.optInt("total_hoy", 0);
                lblTotalMenusHoy.setText(String.valueOf(valor));
            }

            if (data.has("categorias")) {
                JSONObject categorias = data.getJSONObject("categorias");
                int valor = categorias.optInt("total_hoy", 0);
                lblTotalCategoriasHoy.setText(String.valueOf(valor));
            }

            if (data.has("ingredientes")) {
                JSONObject ingredientes = data.getJSONObject("ingredientes");
                int valor = ingredientes.optInt("total_hoy", 0);
                lblTotalIngredientesHoy.setText(String.valueOf(valor));
            }

        } catch (Exception e) {
            System.err.println("Error actualizando labels hoy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarCharts(JSONObject data) {
        try {
            // Usuarios por tipo
            if (chartUsuariosTipoHoy != null && data.has("usuarios") &&
                    data.getJSONObject("usuarios").has("por_tipo_hoy")) {
                JSONArray array = data.getJSONObject("usuarios").getJSONArray("por_tipo_hoy");
                cargarPie(chartUsuariosTipoHoy, array, "Tipo", "cantidad");
            }

            // Productos por categoría
            if (chartProductosCategoriaHoy != null && data.has("categorias") &&
                    data.getJSONObject("categorias").has("productos_por_categoria_hoy")) {
                JSONArray array = data.getJSONObject("categorias").getJSONArray("productos_por_categoria_hoy");
                cargarBar(chartProductosCategoriaHoy, array, "categoria", "cantidad_productos");
            }

            // Avisos por establecimiento
            if (chartAvisosEstablecimientoHoy != null && data.has("avisos") &&
                    data.getJSONObject("avisos").has("por_establecimiento_hoy")) {
                JSONArray array = data.getJSONObject("avisos").getJSONArray("por_establecimiento_hoy");
                cargarPie(chartAvisosEstablecimientoHoy, array, "Establecimiento", "cantidad");
            }

            // Menús por día
            if (chartMenusDiaHoy != null && data.has("menus") &&
                    data.getJSONObject("menus").has("por_dia_semana_hoy")) {
                JSONArray array = data.getJSONObject("menus").getJSONArray("por_dia_semana_hoy");
                cargarBar(chartMenusDiaHoy, array, "DiaSemana", "cantidad");
            }

            // Productos por estado
            if (chartProductosEstadoHoy != null && data.has("productos") &&
                    data.getJSONObject("productos").has("por_disponibilidad_hoy")) {
                JSONArray array = data.getJSONObject("productos").getJSONArray("por_disponibilidad_hoy");
                cargarPie(chartProductosEstadoHoy, array, "Disponible", "cantidad");
            }

        } catch (Exception e) {
            System.err.println("Error actualizando charts hoy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cargarPie(PieChart chart, JSONArray arr, String labelKey, String valueKey) {
        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String label = o.has(labelKey) ? o.getString(labelKey) : "Sin etiqueta";
            int value = o.has(valueKey) ? o.getInt(valueKey) : 0;
            if (value > 0) {
                data.add(new PieChart.Data(label, value));
            }
        }

        if (data.isEmpty()) {
            data.add(new PieChart.Data("Sin datos", 1));
        }

        chart.setData(data);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        chart.setAnimated(false);
    }

    private void cargarBar(BarChart<String, Number> chart, JSONArray arr, String xKey, String yKey) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.getJSONObject(i);
            String xValue = o.has(xKey) ? o.getString(xKey) : "Sin etiqueta";
            int yValue = o.has(yKey) ? o.getInt(yKey) : 0;
            if (yValue > 0) {
                series.getData().add(new XYChart.Data<>(xValue, yValue));
            }
        }

        if (series.getData().isEmpty()) {
            series.getData().add(new XYChart.Data<>("Sin datos", 1));
        }

        chart.getData().setAll(series);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
    }
}