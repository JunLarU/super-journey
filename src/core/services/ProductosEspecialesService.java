package core.services;

import core.HTTPConnection;
import core.data.Productos.ProductoEspecial;
import org.json.JSONArray;
import org.json.JSONObject;
import javafx.concurrent.Task;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductosEspecialesService {
    
    private static ProductosEspecialesService instance;
    private final HTTPConnection httpConnection;
    
    private ProductosEspecialesService() {
        this.httpConnection = HTTPConnection.getInstance();
    }
    
    public static ProductosEspecialesService getInstance() {
        if (instance == null) {
            instance = new ProductosEspecialesService();
        }
        return instance;
    }
    
    /**
     * Obtener todos los productos especiales del servidor
     */
    public Task<List<ProductoEspecial>> getAll() {
        Task<List<ProductoEspecial>> task = new Task<>() {
            @Override
            protected List<ProductoEspecial> call() throws Exception {
                try {
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/productos-especiales",
                        Optional.of(0), // GET
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("Response status: " + response.statusCode());
                    System.out.println("Response body: " + response.body());
                    
                    if (response.statusCode() == 200) {
                        try {
                            JSONObject json = new JSONObject(response.body());
                            
                            // Verificar si la respuesta tiene el formato esperado
                            if (!json.has("productos_especiales")) {
                                throw new Exception("Formato de respuesta inválido. No se encontró 'productos_especiales'");
                            }
                            
                            JSONArray data = json.getJSONArray("productos_especiales");
                            List<ProductoEspecial> productos = new ArrayList<>();
                            
                            for (int i = 0; i < data.length(); i++) {
                                try {
                                    JSONObject item = data.getJSONObject(i);
                                    ProductoEspecial producto = new ProductoEspecial(item);
                                    productos.add(producto);
                                } catch (Exception e) {
                                    System.err.println("Error procesando item " + i + ": " + e.getMessage());
                                    // Continuar con el siguiente
                                }
                            }
                            
                            return productos;
                            
                        } catch (Exception e) {
                            throw new Exception("Error parseando JSON: " + e.getMessage());
                        }
                    } else {
                        // Intentar obtener mensaje de error del JSON
                        try {
                            JSONObject errorJson = new JSONObject(response.body());
                            String errorMsg = errorJson.optString("error", "Error desconocido");
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + errorMsg);
                        } catch (Exception e) {
                            throw new Exception("Error HTTP " + response.statusCode() + ": " + response.body());
                        }
                    }
                    
                } catch (Exception e) {
                    System.err.println("Error en getAll: " + e.getMessage());
                    throw new Exception("Error al obtener productos especiales: " + e.getMessage());
                }
            }
        };
        
        // Manejo de excepciones
        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            System.err.println("Task failed: " + ex.getMessage());
            ex.printStackTrace();
        });
        
        return task;
    }
    
    /**
     * Crear nuevo producto especial
     */
    public Task<Boolean> crear(ProductoEspecial producto) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    JSONObject json = producto.toJson();
                    
                    System.out.println("Enviando POST con datos: " + json.toString());
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/productos-especiales",
                        Optional.of(1), // POST
                        Optional.of(json.toString()),
                        Optional.of(0)
                    );
                    
                    System.out.println("POST Response: " + response.statusCode() + " - " + response.body());
                    
                    if (response.statusCode() == 201) {
                        // Intentar obtener el ID asignado
                        try {
                            JSONObject respJson = new JSONObject(response.body());
                            if (respJson.has("id")) {
                                producto.setId(respJson.getInt("id"));
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
                            throw new Exception("Error HTTP " + response.statusCode());
                        }
                    }
                    
                } catch (Exception e) {
                    throw new Exception("Error al crear producto especial: " + e.getMessage());
                }
            }
        };
        
        task.setOnFailed(event -> {
            System.err.println("Error en crear: " + task.getException().getMessage());
        });
        
        return task;
    }
    
    /**
     * Actualizar producto especial existente
     */
    public Task<Boolean> actualizar(ProductoEspecial producto) {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    JSONObject json = producto.toJson();
                    
                    System.out.println("Enviando PUT con datos: " + json.toString());
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/productos-especiales",
                        Optional.of(2), // PUT
                        Optional.of(json.toString()),
                        Optional.of(0)
                    );
                    
                    System.out.println("PUT Response: " + response.statusCode() + " - " + response.body());
                    
                    return response.statusCode() == 200;
                    
                } catch (Exception e) {
                    throw new Exception("Error al actualizar producto especial: " + e.getMessage());
                }
            }
        };
    }
    
    /**
     * Eliminar producto especial
     */
    public Task<Boolean> eliminar(int id) {
        return new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                try {
                    System.out.println("Enviando DELETE para ID: " + id);
                    
                    HttpResponse<String> response = httpConnection.sendRequest(
                        "api/productos-especiales?id=" + id,
                        Optional.of(3), // DELETE
                        Optional.empty(),
                        Optional.of(0)
                    );
                    
                    System.out.println("DELETE Response: " + response.statusCode() + " - " + response.body());
                    
                    return response.statusCode() == 200;
                    
                } catch (Exception e) {
                    throw new Exception("Error al eliminar producto especial: " + e.getMessage());
                }
            }
        };
    }
}