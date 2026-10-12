package core.data.Ventas;

import org.json.JSONObject;

public class Descuento {
    public enum Tipo { Ninguno, Porcentaje, Monto }

    private Tipo tipo = Tipo.Ninguno;
    private double valor = 0.0;
    private String motivo = "";

    public Descuento() {}

    public Descuento(Tipo tipo, double valor, String motivo) {
        this.tipo = tipo == null ? Tipo.Ninguno : tipo;
        this.valor = Math.max(0, valor);
        this.motivo = motivo == null ? "" : motivo;
    }

    public JSONObject toJson() {
        return new JSONObject()
                .put("tipo", tipo.name())
                .put("valor", valor)
                .put("motivo", motivo);
    }

    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = Math.max(0, valor); }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo == null ? "" : motivo; }
}
