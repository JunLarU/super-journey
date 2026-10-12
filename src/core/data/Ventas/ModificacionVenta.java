package core.data.Ventas;

import org.json.JSONObject;

public class ModificacionVenta {
    private String tipo;
    private Integer idProductoIngrediente;
    private Integer idIngredienteNuevo;
    private Integer idExtraProductoIngrediente;
    private int cantidad = 1;
    private String descripcion = "";
    private double costoEstimado = 0;

    public JSONObject toJson() {
        JSONObject o = new JSONObject()
                .put("tipo", tipo)
                .put("cantidad", cantidad);

        if (idProductoIngrediente != null)
            o.put("idProductoIngrediente", idProductoIngrediente);
        if (idIngredienteNuevo != null)
            o.put("idIngredienteNuevo", idIngredienteNuevo);
        if (idExtraProductoIngrediente != null)
            o.put("idExtraProductoIngrediente", idExtraProductoIngrediente);

        return o;
    }

    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v; }
    public Integer getIdProductoIngrediente() { return idProductoIngrediente; }
    public void setIdProductoIngrediente(Integer v) { idProductoIngrediente = v; }
    public Integer getIdIngredienteNuevo() { return idIngredienteNuevo; }
    public void setIdIngredienteNuevo(Integer v) { idIngredienteNuevo = v; }
    public Integer getIdExtraProductoIngrediente() { return idExtraProductoIngrediente; }
    public void setIdExtraProductoIngrediente(Integer v) { idExtraProductoIngrediente = v; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int v) { cantidad = Math.max(1,v); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v == null ? "" : v; }
    public double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(double v) { costoEstimado = v; }
}
