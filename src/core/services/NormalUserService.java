package core.services;

import core.HTTPConnection;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.concurrent.Task;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Servicio para obtener datos del dashboard de usuarios normales
 */
public class NormalUserService {
    
    private static NormalUserService instance;
    private final HTTPConnection httpConnection;
    
    private NormalUserService() {
        this.httpConnection = HTTPConnection.getInstance();
    }
    
    public static NormalUserService getInstance() {
        if (instance == null) {
            instance = new NormalUserService();
        }
        return instance;
    }
    
    /**
     * Obtener menú semanal
     */
    public Task<JSONObject> getMenuSemanal(int semana, int anio, String horario) {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                try {
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/normaluser/menu?semana=" + semana + "&anio=" + anio + "&horario=" + horario,
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Response status: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (!json.getBoolean("success")) {
                            String errorMsg = json.optString("error", "Error desconocido");
                            throw new Exception("Error del servidor: " + errorMsg);
                        }
                        
                        return json;
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getMenuSemanal: " + e.getMessage());
                    throw new Exception("Error al obtener menú semanal: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Task failed: " + ex.getMessage());
            ex.printStackTrace();
        });
        
        return task;
    }
    
    /**
     * Obtener menú de la semana actual
     */
    public Task<JSONObject> getMenuActual(String horario) {
        LocalDate hoy = LocalDate.now();
        int semanaActual = hoy.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        int anioActual = hoy.getYear();
        
        return getMenuSemanal(semanaActual, anioActual, horario);
    }
    
    /**
     * Obtener avisos con filtros
     */
    public Task<JSONObject> getAvisos(String establecimiento, String tipo, String prioridad) {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                try {
                    String endpoint = "api/normaluser/avisos";
                    boolean hasParams = false;
                    
                    StringBuilder params = new StringBuilder();
                    if (establecimiento != null && !establecimiento.equals("Todos")) {
                        params.append("establecimiento=").append(establecimiento);
                        hasParams = true;
                    }
                    
                    if (tipo != null && !tipo.equals("Todos")) {
                        if (hasParams) params.append("&");
                        params.append("tipo=").append(tipo);
                        hasParams = true;
                    }
                    
                    if (prioridad != null && !prioridad.equals("Todos")) {
                        if (hasParams) params.append("&");
                        params.append("prioridad=").append(prioridad);
                        hasParams = true;
                    }
                    
                    if (hasParams) {
                        endpoint += "?" + params.toString();
                    }
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        endpoint,
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Response status: " + response.statusCode());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (!json.getBoolean("success")) {
                            String errorMsg = json.optString("error", "Error desconocido");
                            throw new Exception("Error del servidor: " + errorMsg);
                        }
                        
                        return json;
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getAvisos: " + e.getMessage());
                    throw new Exception("Error al obtener avisos: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Task failed: " + ex.getMessage());
            ex.printStackTrace();
        });
        
        return task;
    }
    
    /**
     * Obtener todos los avisos sin filtros
     */
    public Task<JSONObject> getAllAvisos() {
        return getAvisos("Todos", "Todos", "Todos");
    }
    
    /**
     * Obtener productos especiales vigentes
     */
    public Task<JSONObject> getProductosEspeciales() {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                try {
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/normaluser/especiales",
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Response status: " + response.statusCode());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (!json.getBoolean("success")) {
                            String errorMsg = json.optString("error", "Error desconocido");
                            throw new Exception("Error del servidor: " + errorMsg);
                        }
                        
                        return json;
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getProductosEspeciales: " + e.getMessage());
                    throw new Exception("Error al obtener productos especiales: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Task failed: " + ex.getMessage());
            ex.printStackTrace();
        });
        
        return task;
    }
    
    /**
     * Obtener semanas disponibles con menú
     */
    public Task<JSONObject> getSemanasDisponibles() {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                try {
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/normaluser/semanas",
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Response status: " + response.statusCode());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (!json.getBoolean("success")) {
                            String errorMsg = json.optString("error", "Error desconocido");
                            throw new Exception("Error del servidor: " + errorMsg);
                        }
                        
                        return json;
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getSemanasDisponibles: " + e.getMessage());
                    throw new Exception("Error al obtener semanas disponibles: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Task failed: " + ex.getMessage());
            ex.printStackTrace();
        });
        
        return task;
    }
    
    /**
     * Obtener menú de hoy
     */
    public Task<JSONObject> getMenuHoy() {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                try {
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/normaluser/menu/hoy",
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Response status: " + response.statusCode());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (!json.getBoolean("success")) {
                            String errorMsg = json.optString("error", "Error desconocido");
                            throw new Exception("Error del servidor: " + errorMsg);
                        }
                        
                        return json;
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getMenuHoy: " + e.getMessage());
                    throw new Exception("Error al obtener menú de hoy: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Task failed: " + ex.getMessage());
            ex.printStackTrace();
        });
        
        return task;
    }
    
    /**
     * Convertir JSON a lista de objetos (si necesitas convertir a tus clases de datos)
     */
    public List<JSONObject> jsonToMenusList(JSONObject jsonResponse) {
        List<JSONObject> menus = new ArrayList<>();
        
        if (jsonResponse.has("menus")) {
            JSONArray menusArray = jsonResponse.getJSONArray("menus");
            
            for (int i = 0; i < menusArray.length(); i++) {
                JSONObject menuJson = menusArray.getJSONObject(i);
                menus.add(menuJson);
            }
        }
        
        return menus;
    }
    
    /**
     * Convertir JSON a lista de avisos
     */
    public List<JSONObject> jsonToAvisosList(JSONObject jsonResponse) {
        List<JSONObject> avisos = new ArrayList<>();
        
        if (jsonResponse.has("avisos")) {
            JSONArray avisosArray = jsonResponse.getJSONArray("avisos");
            
            for (int i = 0; i < avisosArray.length(); i++) {
                JSONObject avisoJson = avisosArray.getJSONObject(i);
                avisos.add(avisoJson);
            }
        }
        
        return avisos;
    }
    
    /**
     * Convertir JSON a lista de productos especiales
     */
    public List<JSONObject> jsonToEspecialesList(JSONObject jsonResponse) {
        List<JSONObject> especiales = new ArrayList<>();
        
        if (jsonResponse.has("productos_especiales")) {
            JSONArray especialesArray = jsonResponse.getJSONArray("productos_especiales");
            
            for (int i = 0; i < especialesArray.length(); i++) {
                JSONObject especialJson = especialesArray.getJSONObject(i);
                especiales.add(especialJson);
            }
        }
        
        return especiales;
    }
    
    /**
     * Obtener información de semana actual desde JSON
     */
    public int getSemanaActual(JSONObject jsonResponse) {
        return jsonResponse.optInt("semana", 0);
    }
    
    /**
     * Obtener año actual desde JSON
     */
    public int getAnioActual(JSONObject jsonResponse) {
        return jsonResponse.optInt("anio", 0);
    }
}