package core.services;

import core.HTTPConnection;
import core.data.Avisos.Aviso;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.concurrent.Task;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AvisosService {
    
    private static AvisosService instance;
    private final HTTPConnection httpConnection;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    private AvisosService() {
        this.httpConnection = HTTPConnection.getInstance();
    }
    
    public static AvisosService getInstance() {
        if (instance == null) {
            instance = new AvisosService();
        }
        return instance;
    }
    
    public Task<List<Aviso>> getAll() {
        Task<List<Aviso>> task = new Task<>() {
            @Override
            protected List<Aviso> call() throws Exception {
                try {
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/avisos",
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Avisos Response status: " + response.statusCode());
                    System.out.println("Avisos Response body: " + response.body());
                    
                    if (response.statusCode() == 200) {
                        JSONObject json = new JSONObject(response.body());
                        
                        if (!json.has("avisos")) {
                            throw new Exception("Formato de respuesta inválido. No se encontró 'avisos'");
                        }
                        
                        JSONArray data = json.getJSONArray("avisos");
                        List<Aviso> avisos = new ArrayList<>();
                        
                        for (int i = 0; i < data.length(); i++) {
                            try {
                                JSONObject item = data.getJSONObject(i);
                                
                                Aviso aviso = new Aviso();
                                aviso.setId(item.getInt("ID"));
                                aviso.setTitulo(item.getString("Titulo"));
                                aviso.setContenido(item.getString("Contenido"));
                                aviso.setEstablecimiento(Aviso.Establecimiento.valueOf(item.getString("Establecimiento")));
                                aviso.setTipoAviso(Aviso.TipoAviso.valueOf(item.getString("TipoAviso")));
                                aviso.setPrioridad(Aviso.Prioridad.valueOf(item.getString("Prioridad")));
                                
                                // Parsear fechas (manejar tanto fecha como fecha/hora)
                                String fechaInicioStr = item.getString("FechaInicio");
                                String fechaFinStr = item.getString("FechaFin");
                                
                                // Intentar parsear como fecha/hora primero, luego como solo fecha
                                try {
                                    aviso.setFechaInicio(LocalDateTime.parse(fechaInicioStr, dateTimeFormatter));
                                } catch (Exception e) {
                                    // Si falla, intentar como solo fecha
                                    aviso.setFechaInicio(LocalDate.parse(fechaInicioStr, dateFormatter).atStartOfDay());
                                }
                                
                                try {
                                    aviso.setFechaFin(LocalDateTime.parse(fechaFinStr, dateTimeFormatter));
                                } catch (Exception e) {
                                    // Si falla, intentar como solo fecha
                                    aviso.setFechaFin(LocalDate.parse(fechaFinStr, dateFormatter).atTime(23, 59, 59));
                                }
                                
                                aviso.setActivo(item.getInt("Activo") == 1);
                                
                                if (item.has("FechaPublicacion") && !item.isNull("FechaPublicacion")) {
                                    try {
                                        String fechaPubStr = item.getString("FechaPublicacion");
                                        aviso.setFechaPublicacion(LocalDateTime.parse(fechaPubStr, dateTimeFormatter));
                                    } catch (Exception e) {
                                        System.err.println("Error parsing FechaPublicacion: " + e.getMessage());
                                        aviso.setFechaPublicacion(LocalDateTime.now());
                                    }
                                } else {
                                    aviso.setFechaPublicacion(LocalDateTime.now());
                                }
                                
                                // ID Usuario Creador
                                aviso.setIdUsuarioCreador(item.optInt("IDUsuarioCreador", 0));
                                
                                avisos.add(aviso);
                                
                            } catch (Exception e) {
                                System.err.println("Error procesando aviso " + i + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                        
                        return avisos;
                        
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
                    System.err.println("Error en getAll avisos: " + e.getMessage());
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
    
    public Task<Boolean> crear(Aviso aviso) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // Crear JSON manualmente para asegurar formato correcto
                    JSONObject json = new JSONObject();
                    
                    json.put("titulo", aviso.getTitulo());
                    json.put("contenido", aviso.getContenido());
                    json.put("establecimiento", aviso.getEstablecimiento().name());
                    json.put("tipoAviso", aviso.getTipoAviso().name());
                    json.put("prioridad", aviso.getPrioridad().name());
                    
                    // Formatear fechas como DATETIME completo
                    json.put("fechaInicio", aviso.getFechaInicio().format(dateTimeFormatter));
                    json.put("fechaFin", aviso.getFechaFin().format(dateTimeFormatter));
                    
                    // Agregar también horas separadas por si el backend las necesita
                    json.put("horaInicio", aviso.getFechaInicio().getHour());
                    json.put("minutoInicio", aviso.getFechaInicio().getMinute());
                    json.put("horaFin", aviso.getFechaFin().getHour());
                    json.put("minutoFin", aviso.getFechaFin().getMinute());
                    
                    json.put("idUsuarioCreador", aviso.getIdUsuarioCreador());
                    json.put("activo", aviso.isActivo());
                    
                    System.out.println("Enviando POST aviso con datos: " + json.toString());
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/avisos",
                        Optional.of(1), // POST
                        Optional.of(json.toString()),
                        Optional.of(0)
                    );
                    
                    System.out.println("POST Aviso Response: " + response.statusCode() + " - " + response.body());
                    
                    if (response.statusCode() == 201) {
                        // Intentar obtener el ID asignado
                        try {
                            JSONObject respJson = new JSONObject(response.body());
                            if (respJson.has("id")) {
                                aviso.setId(respJson.getInt("id"));
                            }
                        } catch (Exception e) {
                            System.err.println("No se pudo obtener ID de respuesta: " + e.getMessage());
                        }
                        return true;
                    } else {
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error del servidor: " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    throw new Exception("Error al crear aviso: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            System.err.println("Error en crear aviso: " + task.getException().getMessage());
        });
        
        return task;
    }
    
    public Task<Boolean> actualizar(Aviso aviso) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    // Crear JSON manualmente
                    JSONObject json = new JSONObject();
                    
                    json.put("id", aviso.getId());
                    json.put("titulo", aviso.getTitulo());
                    json.put("contenido", aviso.getContenido());
                    json.put("establecimiento", aviso.getEstablecimiento().name());
                    json.put("tipoAviso", aviso.getTipoAviso().name());
                    json.put("prioridad", aviso.getPrioridad().name());
                    
                    // Formatear fechas como DATETIME completo
                    json.put("fechaInicio", aviso.getFechaInicio().format(dateTimeFormatter));
                    json.put("fechaFin", aviso.getFechaFin().format(dateTimeFormatter));
                    
                    // Agregar horas separadas por si el backend las necesita
                    json.put("horaInicio", aviso.getFechaInicio().getHour());
                    json.put("minutoInicio", aviso.getFechaInicio().getMinute());
                    json.put("horaFin", aviso.getFechaFin().getHour());
                    json.put("minutoFin", aviso.getFechaFin().getMinute());
                    
                    json.put("idUsuarioCreador", aviso.getIdUsuarioCreador());
                    json.put("activo", aviso.isActivo());
                    
                    System.out.println("Enviando PUT aviso con datos: " + json.toString());
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/avisos",
                        Optional.of(2), // PUT
                        Optional.of(json.toString()),
                        Optional.of(0)
                    );
                    
                    System.out.println("PUT Aviso Response: " + response.statusCode() + " - " + response.body());
                    
                    return response.statusCode() == 200;
                    
                } catch (Exception e) {
                    throw new Exception("Error al actualizar aviso: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            System.err.println("Error en actualizar aviso: " + task.getException().getMessage());
        });
        
        return task;
    }
    
    public Task<Boolean> eliminar(int id) {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    System.out.println("Enviando DELETE aviso para ID: " + id);
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/avisos?id=" + id,
                        Optional.of(3), // DELETE
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("DELETE Aviso Response: " + response.statusCode() + " - " + response.body());
                    
                    return response.statusCode() == 200;
                    
                } catch (Exception e) {
                    throw new Exception("Error al eliminar aviso: " + e.getMessage());
                }
            }
        };
    }
}