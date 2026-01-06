package core.data.Users;

import org.json.JSONObject;

public class User {
    private int id; // Nuevo campo para el ID
    private String clave;
    private String username;
    private String password;
    private boolean isAdmin;
    private String name;
    private String email;
    private String phone;
    private String apellidoPaterno;
    private String apellidoMaterno;

    // Constructor principal
    public User(String username, String password, String name, String apellidoPaterno, String apellidoMaterno, String email, String phone) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.email = email;
        this.phone = phone;
        this.isAdmin = false;
    }

    // Constructor con ID
    public User(int id, String username, String password, String name, String apellidoPaterno, String apellidoMaterno, String email, String phone) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.email = email;
        this.phone = phone;
        this.isAdmin = false;
    }

    // Constructor vacío (requerido para carga desde JSON)
    public User() {}

    // Getters y setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClave() {
        return clave;
    }
    
    public void setClave(String clave) {
        this.clave = clave;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getApellidoPaterno() {
        return apellidoPaterno;
    }
    public void setApellidoPaterno(String apellidoPaterno) {
        this.apellidoPaterno = apellidoPaterno;
    }

    public String getApellidoMaterno() {
        return apellidoMaterno;
    }
    public void setApellidoMaterno(String apellidoMaterno) {
        this.apellidoMaterno = apellidoMaterno;
    }

    // Conversión a JSONObject (para guardar en JSON)
    public JSONObject toJSON() {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("clave", clave != null ? clave : "");
        obj.put("username", username != null ? username : "");
        obj.put("password", password != null ? password : "");
        obj.put("isAdmin", isAdmin);
        obj.put("name", name != null ? name : "");
        obj.put("email", email != null ? email : "");
        obj.put("phone", phone != null ? phone : "");
        obj.put("apellidoPaterno", apellidoPaterno != null ? apellidoPaterno : "");
        obj.put("apellidoMaterno", apellidoMaterno != null ? apellidoMaterno : "");
        return obj;
    }

    // Crea un objeto User desde un JSONObject (para leer desde JSON)
    public static User fromJSON(JSONObject obj) {
        User u = new User();
        u.setId(obj.optInt("id", 0));
        u.setClave(obj.optString("clave", ""));
        u.username = obj.optString("username", "");
        u.password = obj.optString("password", "");
        u.isAdmin = obj.optBoolean("isAdmin", false);
        u.name = obj.optString("name", "");
        u.email = obj.optString("email", "");
        u.phone = obj.optString("phone", "");
        u.apellidoPaterno = obj.optString("apellidoPaterno", "");
        u.apellidoMaterno = obj.optString("apellidoMaterno", "");
        return u;
    }

    @Override
    public String toString() {
        return String.format("User[ID=%d, %s %s %s | user=%s | admin=%s]",
                id, name, apellidoPaterno, apellidoMaterno, username, isAdmin);
    }
}