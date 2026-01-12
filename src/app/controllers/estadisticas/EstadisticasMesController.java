package app.controllers.estadisticas;

import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class EstadisticasMesController {

    @FXML
    private Label lblTotalUsuariosMes, lblTotalProductosMes, lblTotalAvisosMes,
            lblTotalMenusMes, lblTotalCategoriasMes, lblTotalIngredientesMes;
    @FXML
    private PieChart chartUsuariosTipoMes;
    @FXML
    private BarChart<String, Number> chartProductosCategoriaMes;
    @FXML
    private PieChart chartAvisosEstablecimientoMes;
    @FXML
    private BarChart<String, Number> chartMenusDiaMes;
    @FXML
    private PieChart chartProductosEstadoMes;

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
                int valor = usuarios.optInt("total_mes", 0);
                lblTotalUsuariosMes.setText(String.valueOf(valor));
            }

            if (data.has("productos")) {
                JSONObject productos = data.getJSONObject("productos");
                int valor = productos.optInt("total_mes", 0);
                lblTotalProductosMes.setText(String.valueOf(valor));
            }

            if (data.has("avisos")) {
                JSONObject avisos = data.getJSONObject("avisos");
                int valor = avisos.optInt("total_mes", 0);
                lblTotalAvisosMes.setText(String.valueOf(valor));
            }

            if (data.has("menus")) {
                JSONObject menus = data.getJSONObject("menus");
                int valor = menus.optInt("total_mes", 0);
                lblTotalMenusMes.setText(String.valueOf(valor));
            }

            if (data.has("categorias")) {
                JSONObject categorias = data.getJSONObject("categorias");
                int valor = categorias.optInt("total_mes", 0);
                lblTotalCategoriasMes.setText(String.valueOf(valor));
            }

            if (data.has("ingredientes")) {
                JSONObject ingredientes = data.getJSONObject("ingredientes");
                int valor = ingredientes.optInt("total_mes", 0);
                lblTotalIngredientesMes.setText(String.valueOf(valor));
            }

        } catch (Exception e) {
            System.err.println("Error actualizando labels Mes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void actualizarCharts(JSONObject data) {
        try {
            // Usuarios por tipo
            if (chartUsuariosTipoMes != null && data.has("usuarios") &&
                    data.getJSONObject("usuarios").has("por_tipo_mes")) {
                JSONArray array = data.getJSONObject("usuarios").getJSONArray("por_tipo_mes");
                cargarPieSeguro(chartUsuariosTipoMes, array, "Tipo", "cantidad");
            } else if (chartUsuariosTipoMes != null) {
                chartUsuariosTipoMes.setData(FXCollections.observableArrayList());
            }

            // Productos por categoría
            if (chartProductosCategoriaMes != null && data.has("categorias") &&
                    data.getJSONObject("categorias").has("productos_por_categoria_mes")) {
                JSONArray array = data.getJSONObject("categorias").getJSONArray("productos_por_categoria_mes");
                cargarBarSeguro(chartProductosCategoriaMes, array, "categoria", "cantidad_productos");
            } else if (chartProductosCategoriaMes != null) {
                chartProductosCategoriaMes.getData().clear();
            }

            // Avisos por establecimiento
            if (chartAvisosEstablecimientoMes != null && data.has("avisos") &&
                    data.getJSONObject("avisos").has("por_establecimiento_mes")) {
                JSONArray array = data.getJSONObject("avisos").getJSONArray("por_establecimiento_mes");
                cargarPieSeguro(chartAvisosEstablecimientoMes, array, "Establecimiento", "cantidad");
            } else if (chartAvisosEstablecimientoMes != null) {
                chartAvisosEstablecimientoMes.setData(FXCollections.observableArrayList());
            }

            // Menús por día - CORREGIDO: usar "DiaSemana" en lugar de "DiaMes"
            if (chartMenusDiaMes != null && data.has("menus") &&
                    data.getJSONObject("menus").has("por_dia_semana_mes")) {
                JSONArray array = data.getJSONObject("menus").getJSONArray("por_dia_semana_mes");
                cargarBarSeguro(chartMenusDiaMes, array, "DiaSemana", "cantidad");
            } else if (chartMenusDiaMes != null) {
                chartMenusDiaMes.getData().clear();
            }

            // Productos por estado
            if (chartProductosEstadoMes != null && data.has("productos") &&
                    data.getJSONObject("productos").has("por_disponibilidad_mes")) {
                JSONArray array = data.getJSONObject("productos").getJSONArray("por_disponibilidad_mes");
                cargarPieSeguro(chartProductosEstadoMes, array, "Disponible", "cantidad");
            } else if (chartProductosEstadoMes != null) {
                chartProductosEstadoMes.setData(FXCollections.observableArrayList());
            }

        } catch (Exception e) {
            System.err.println("Error actualizando charts Mes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método seguro para cargar datos en PieChart que maneja diferentes tipos de
     * datos
     */
    private void cargarPieSeguro(PieChart chart, JSONArray arr, String labelKey, String valueKey) {
        try {
            ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject o = arr.getJSONObject(i);

                    // Obtener etiqueta de forma segura
                    String label = obtenerValorComoString(o, labelKey, "Sin etiqueta");

                    // Obtener valor numérico
                    int value = obtenerValorComoInt(o, valueKey, 0);

                    if (value > 0) {
                        // Para el campo "Disponible", convertir 0/1 a texto significativo
                        if ("Disponible".equals(labelKey) && (label.equals("1") || label.equals("0"))) {
                            label = label.equals("1") ? "Disponible" : "No Disponible";
                        }

                        data.add(new PieChart.Data(label, value));
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando elemento " + i + " en PieChart: " + e.getMessage());
                }
            }

            if (data.isEmpty()) {
                data.add(new PieChart.Data("Sin datos", 1));
            }

            chart.setData(data);
            chart.setLabelsVisible(true);
            chart.setLegendVisible(true);
            chart.setAnimated(false);

        } catch (Exception e) {
            System.err.println("Error en cargarPieSeguro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método seguro para cargar datos en BarChart que maneja diferentes tipos de
     * datos
     */
    private void cargarBarSeguro(BarChart<String, Number> chart, JSONArray arr, String xKey, String yKey) {
        try {
            XYChart.Series<String, Number> series = new XYChart.Series<>();

            for (int i = 0; i < arr.length(); i++) {
                try {
                    JSONObject o = arr.getJSONObject(i);

                    // Obtener valor X como string
                    String xValue = obtenerValorComoString(o, xKey, "Sin etiqueta");

                    // Obtener valor Y como número
                    int yValue = obtenerValorComoInt(o, yKey, 0);

                    if (yValue > 0) {
                        series.getData().add(new XYChart.Data<>(xValue, yValue));
                    }
                } catch (Exception e) {
                    System.err.println("Error procesando elemento " + i + " en BarChart: " + e.getMessage());
                }
            }

            if (series.getData().isEmpty()) {
                series.getData().add(new XYChart.Data<>("Sin datos", 1));
            }

            chart.getData().setAll(series);
            chart.setAnimated(false);
            chart.setLegendVisible(false);

        } catch (Exception e) {
            System.err.println("Error en cargarBarSeguro: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar para obtener un valor como String de forma segura
     */
    private String obtenerValorComoString(JSONObject obj, String key, String defaultValue) {
        try {
            if (obj.has(key)) {
                Object value = obj.get(key);
                if (value == null) {
                    return defaultValue;
                }
                return value.toString();
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo string para clave '" + key + "': " + e.getMessage());
        }
        return defaultValue;
    }

    /**
     * Método auxiliar para obtener un valor como int de forma segura
     */
    private int obtenerValorComoInt(JSONObject obj, String key, int defaultValue) {
        try {
            if (obj.has(key)) {
                Object value = obj.get(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value instanceof String) {
                    try {
                        return Integer.parseInt((String) value);
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error obteniendo int para clave '" + key + "': " + e.getMessage());
        }
        return defaultValue;
    }
}