package core.data.Ventas;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

public class LineaVenta {
    private final String uid = UUID.randomUUID().toString();
    private int idProducto;
    private String nombreProducto;
    private Integer idTamanoProducto;
    private String nombreTamano;
    private int cantidad = 1;
    private final List<ModificacionVenta> modificaciones = new ArrayList<>();
    private Descuento descuento = new Descuento();
    private String observaciones = "";

    private double precioListaUnitario;
    private double totalExtrasUnitario;
    private double subtotalLineaBruto;
    private double descuentoLinea;
    private double descuentoVentaAsignado;
    private double totalLineaFinal;

    public JSONObject toJson() {
        JSONArray mods = new JSONArray();
        modificaciones.forEach(m -> mods.put(m.toJson()));

        JSONObject o = new JSONObject()
                .put("idProducto", idProducto)
                .put("cantidad", cantidad)
                .put("modificaciones", mods)
                .put("descuento", descuento.toJson())
                .put("observaciones", observaciones);

        o.put(
            "idTamanoProducto",
            idTamanoProducto == null ? JSONObject.NULL : idTamanoProducto
        );
        return o;
    }

    public void applyQuote(JSONObject q) {
        nombreProducto = q.optString("nombreProducto", nombreProducto);
        nombreTamano = q.isNull("nombreTamano")
                ? null : q.optString("nombreTamano", nombreTamano);
        precioListaUnitario = q.optDouble("precioListaUnitario",0);
        totalExtrasUnitario = q.optDouble("totalExtrasUnitario",0);
        subtotalLineaBruto = q.optDouble("subtotalLineaBruto",0);
        descuentoLinea = q.optDouble("descuentoLinea",0);
        descuentoVentaAsignado = q.optDouble("descuentoVentaAsignado",0);
        totalLineaFinal = q.optDouble("totalLineaFinal",0);
    }

    public String getDescripcionConfiguracion() {
        List<String> parts = new ArrayList<>();
        if (nombreTamano != null && !nombreTamano.isBlank()) parts.add(nombreTamano);
        for (ModificacionVenta m : modificaciones)
            if (!m.getDescripcion().isBlank()) parts.add(m.getDescripcion());
        return parts.isEmpty() ? "Estándar" : String.join(", ",parts);
    }

    public String getUid(){return uid;}
    public int getIdProducto(){return idProducto;}
    public void setIdProducto(int v){idProducto=v;}
    public String getNombreProducto(){return nombreProducto;}
    public void setNombreProducto(String v){nombreProducto=v;}
    public Integer getIdTamanoProducto(){return idTamanoProducto;}
    public void setIdTamanoProducto(Integer v){idTamanoProducto=v;}
    public String getNombreTamano(){return nombreTamano;}
    public void setNombreTamano(String v){nombreTamano=v;}
    public int getCantidad(){return cantidad;}
    public void setCantidad(int v){cantidad=Math.max(1,v);}
    public List<ModificacionVenta> getModificaciones(){return modificaciones;}
    public Descuento getDescuento(){return descuento;}
    public void setDescuento(Descuento v){descuento=v==null?new Descuento():v;}
    public String getObservaciones(){return observaciones;}
    public void setObservaciones(String v){observaciones=v==null?"":v;}
    public double getPrecioListaUnitario(){return precioListaUnitario;}
    public double getTotalExtrasUnitario(){return totalExtrasUnitario;}
    public double getSubtotalLineaBruto(){return subtotalLineaBruto;}
    public double getDescuentoLinea(){return descuentoLinea;}
    public double getDescuentoVentaAsignado(){return descuentoVentaAsignado;}
    public double getTotalLineaFinal(){return totalLineaFinal;}
}
