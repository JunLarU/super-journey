package core.data.Ventas;

import java.util.UUID;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONObject;

public class CarritoVenta {
    private final ObservableList<LineaVenta> items =
            FXCollections.observableArrayList();

    private String claveOperacion = UUID.randomUUID().toString();
    private Descuento descuentoVenta = new Descuento();
    private Integer idTurnoCaja;
    private String observaciones = "";

    private double subtotalBruto;
    private double totalExtras;
    private double descuentoProductos;
    private double subtotal;
    private double descuentoVentaCalculado;
    private double total;

    public JSONObject toJson() {
        return toJson(false,null);
    }

    public JSONObject toJson(boolean includePayments, JSONArray pagos) {
        JSONArray a = new JSONArray();
        items.forEach(i -> a.put(i.toJson()));

        JSONObject o = new JSONObject()
                .put("claveOperacion",claveOperacion)
                .put("items",a)
                .put("descuentoVenta",descuentoVenta.toJson())
                .put("observaciones",observaciones);

        if (idTurnoCaja != null) o.put("idTurnoCaja",idTurnoCaja);
        if (includePayments) o.put("pagos",pagos == null ? new JSONArray() : pagos);
        return o;
    }

    public void applyQuote(JSONObject q) {
        JSONArray arr = q.optJSONArray("items");
        if (arr != null) {
            for (int i=0;i<arr.length() && i<items.size();i++)
                items.get(i).applyQuote(arr.getJSONObject(i));
        }

        subtotalBruto=q.optDouble("subtotalBruto",0);
        totalExtras=q.optDouble("totalExtras",0);
        descuentoProductos=q.optDouble("descuentoProductos",0);
        subtotal=q.optDouble("subtotal",0);
        descuentoVentaCalculado=q.optDouble("descuentoVenta",0);
        total=q.optDouble("total",0);
    }

    public void limpiar() {
        items.clear();
        descuentoVenta=new Descuento();
        observaciones="";
        subtotalBruto=totalExtras=descuentoProductos=subtotal=
            descuentoVentaCalculado=total=0;
        claveOperacion=UUID.randomUUID().toString();
    }

    public ObservableList<LineaVenta> getItems(){return items;}
    public String getClaveOperacion(){return claveOperacion;}
    public Descuento getDescuentoVenta(){return descuentoVenta;}
    public void setDescuentoVenta(Descuento v){descuentoVenta=v==null?new Descuento():v;}
    public Integer getIdTurnoCaja(){return idTurnoCaja;}
    public void setIdTurnoCaja(Integer v){idTurnoCaja=v;}
    public String getObservaciones(){return observaciones;}
    public void setObservaciones(String v){observaciones=v==null?"":v;}
    public double getSubtotalBruto(){return subtotalBruto;}
    public double getTotalExtras(){return totalExtras;}
    public double getDescuentoProductos(){return descuentoProductos;}
    public double getSubtotal(){return subtotal;}
    public double getDescuentoVentaCalculado(){return descuentoVentaCalculado;}
    public double getTotal(){return total;}
}
