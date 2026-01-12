package core.services;

import core.HTTPConnection;
import org.json.JSONObject;
import javafx.concurrent.Task;

import java.net.http.HttpResponse;
import java.util.Optional;

public class EstadisticasService {
    
    private static EstadisticasService instance;
    private final HTTPConnection httpConnection;
    
    private EstadisticasService() {
        this.httpConnection = HTTPConnection.getInstance();
    }
    
    public static EstadisticasService getInstance() {
        if (instance == null) {
            instance = new EstadisticasService();
        }
        return instance;
    }
    
    public Task<JSONObject> getEstadisticasPeriodo(String periodo) {
        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                try {
                    // Validar período
                    if (!periodo.matches("hoy|semana|mes|seis_meses")) {
                        throw new Exception("Período no válido. Use: hoy, semana, mes, seis_meses");
                    }
                    
                    // Enviar el período como parámetro GET
                    String endpoint = "api/estadisticas?periodo=" + periodo;
                    System.out.println("Solicitando estadísticas: " + endpoint);
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        endpoint,
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Respuesta recibida. Status: " + response.statusCode());
                    System.out.println("Body: " + response.body().substring(0, Math.min(200, response.body().length())));
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (json.has("success") && json.getBoolean("success")) {
                            return json;
                        } else {
                            String error = json.optString("error", "Error desconocido del servidor");
                            throw new Exception("Error del servidor: " + error);
                        }
                    } else {
                        throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getEstadisticasPeriodo: " + e.getMessage());
                    e.printStackTrace();
                    throw new Exception("Error al obtener estadísticas: " + e.getMessage());
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
}