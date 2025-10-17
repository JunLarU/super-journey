package core.data.Menus;

import org.json.JSONObject;

/**
 * Representa una sección asignada a un menú específico
 */
public class MenuSeccion {
    private int id;
    private int idMenu;
    private int idSeccion;
    private String nombreSeccion; // Para mostrar en UI
    private int orden;
    private int idUsuarioAsigno;
    private String fechaAsignacion;

    public MenuSeccion(int id, int idMenu, int idSeccion, String nombreSeccion, 
                      int orden, int idUsuarioAsigno, String fechaAsignacion) {
        this.id = id;
        this.idMenu = idMenu;
        this.idSeccion = idSeccion;
        this.nombreSeccion = nombreSeccion;
        this.orden = orden;
        this.idUsuarioAsigno = idUsuarioAsigno;
        this.fechaAsignacion = fechaAsignacion;
    }

    // Constructor desde JSON
    public MenuSeccion(JSONObject json) {
        this.id = json.getInt("ID");
        this.idMenu = json.getInt("IDMenu");
        this.idSeccion = json.getInt("IDSeccion");
        this.nombreSeccion = json.getString("NombreSeccion");
        this.orden = json.optInt("Orden", 0);
        this.idUsuarioAsigno = json.optInt("IDUsuarioAsigno", 0);
        this.fechaAsignacion = json.getString("FechaAsignacion");
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("IDMenu", idMenu);
        obj.put("IDSeccion", idSeccion);
        obj.put("NombreSeccion", nombreSeccion);
        obj.put("Orden", orden);
        obj.put("IDUsuarioAsigno", idUsuarioAsigno);
        obj.put("FechaAsignacion", fechaAsignacion);
        return obj;
    }

    // Getters y Setters
    public int getId() { return id; }
    public int getIdMenu() { return idMenu; }
    public int getIdSeccion() { return idSeccion; }
    public String getNombreSeccion() { return nombreSeccion; }
    public int getOrden() { return orden; }
    public int getIdUsuarioAsigno() { return idUsuarioAsigno; }
    public String getFechaAsignacion() { return fechaAsignacion; }

    public void setId(int id) { this.id = id; }
    public void setIdMenu(int idMenu) { this.idMenu = idMenu; }
    public void setIdSeccion(int idSeccion) { this.idSeccion = idSeccion; }
    public void setNombreSeccion(String nombreSeccion) { this.nombreSeccion = nombreSeccion; }
    public void setOrden(int orden) { this.orden = orden; }
    public void setIdUsuarioAsigno(int idUsuarioAsigno) { this.idUsuarioAsigno = idUsuarioAsigno; }
    public void setFechaAsignacion(String fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    @Override
    public String toString() {
        return nombreSeccion + " (Orden: " + orden + ")";
    }
}