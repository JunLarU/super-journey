package core.data.Menus;

import org.json.JSONObject;

/**
 * Representa una sección asignada a un menú específico
 * Corresponde a la tabla MenuSemanalSecciones en la BD
 */
public class MenuSeccion {
    private int id;
    private int idMenu;
    private int idSeccion;
    private String nombre;
    private String descripcion;
    private String color;
    private int orden;
    private String fechaAsignacion;
    private String usuarioAsigno;

    // Constructor básico
    public MenuSeccion() {
        this.id = 0;
        this.idMenu = 0;
        this.idSeccion = 0;
        this.nombre = "";
        this.descripcion = "";
        this.color = "#3498db";
        this.orden = 0;
        this.fechaAsignacion = "";
        this.usuarioAsigno = "";
    }

    // Constructor con parámetros
    public MenuSeccion(int id, int idMenu, int idSeccion, String nombre,
            String color, int orden) {
        this.id = id;
        this.idMenu = idMenu;
        this.idSeccion = idSeccion;
        this.nombre = nombre;
        this.descripcion = "";
        this.color = color;
        this.orden = orden;
        this.fechaAsignacion = "";
        this.usuarioAsigno = "";
    }

    // Constructor completo
    public MenuSeccion(int id, int idMenu, int idSeccion, String nombre,
            String descripcion, String color, int orden,
            String fechaAsignacion, String usuarioAsigno) {
        this.id = id;
        this.idMenu = idMenu;
        this.idSeccion = idSeccion;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.color = color;
        this.orden = orden;
        this.fechaAsignacion = fechaAsignacion;
        this.usuarioAsigno = usuarioAsigno;
    }

    // Constructor desde JSON (compatible con backend)
    // Constructor desde JSON (compatible con backend)
    // Constructor desde JSON (compatible con backend)
    public MenuSeccion(JSONObject json) {
        try {
            this.id = json.optInt("ID", 0);

            // El menú puede venir como IDMenuSemanal o IDMenu
            if (json.has("IDMenuSemanal")) {
                this.idMenu = json.getInt("IDMenuSemanal");
            } else if (json.has("IDMenu")) {
                this.idMenu = json.getInt("IDMenu");
            } else {
                this.idMenu = 0;
            }

            this.idSeccion = json.optInt("IDSeccion", 0);
            this.nombre = json.optString("Nombre", "");
            this.descripcion = json.optString("Descripcion", "");
            this.color = json.optString("Color", "#3498db");
            this.orden = json.optInt("Orden", 0);
            this.fechaAsignacion = json.optString("FechaAsignacion", "");
            this.usuarioAsigno = json.optString("UsuarioAsigno", "");

        } catch (Exception e) {
            System.err.println("Error en constructor MenuSeccion(JSONObject): " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Convertir a JSON
    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        obj.put("ID", id);
        obj.put("IDMenuSemanal", idMenu);
        obj.put("IDSeccion", idSeccion);
        obj.put("Nombre", nombre);
        obj.put("Descripcion", descripcion);
        obj.put("Color", color);
        obj.put("Orden", orden);
        obj.put("FechaAsignacion", fechaAsignacion);
        obj.put("UsuarioAsigno", usuarioAsigno);
        return obj;
    }

    // Convertir para formulario
    public JSONObject toJsonForForm() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("idMenu", idMenu);
        obj.put("idSeccion", idSeccion);
        obj.put("nombre", nombre);
        obj.put("descripcion", descripcion);
        obj.put("color", color);
        obj.put("orden", orden);
        return obj;
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public int getIdMenu() {
        return idMenu;
    }

    public int getIdSeccion() {
        return idSeccion;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getColor() {
        return color;
    }

    public int getOrden() {
        return orden;
    }

    public String getFechaAsignacion() {
        return fechaAsignacion;
    }

    public String getUsuarioAsigno() {
        return usuarioAsigno;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setIdMenu(int idMenu) {
        this.idMenu = idMenu;
    }

    public void setIdSeccion(int idSeccion) {
        this.idSeccion = idSeccion;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }

    public void setFechaAsignacion(String fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    public void setUsuarioAsigno(String usuarioAsigno) {
        this.usuarioAsigno = usuarioAsigno;
    }

    // Alias para compatibilidad
    public String getNombreSeccion() {
        return nombre;
    }

    public void setNombreSeccion(String nombreSeccion) {
        this.nombre = nombreSeccion;
    }

    // Método estático
    public static MenuSeccion fromJSON(JSONObject json) {
        return new MenuSeccion(json);
    }

    @Override
    public String toString() {
        return nombre + (color != null && !color.isEmpty() ? " [" + color + "]" : "") +
                " (Orden: " + orden + ")";
    }
}