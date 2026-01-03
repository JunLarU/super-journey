package core.services;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import core.HTTPConnection;
import core.data.Productos.Producto;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProductoService {

    public interface ListCallback {
        void onSuccess(List<JSONObject> list);

        void onError(String error);
    }

    public interface CrudCallback {
        void onSuccess();

        void onError(String error);
    }

    // 🔄 Listar productos
    public static void listProductos(ListCallback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener productos"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();

            try {
                String body = resp.body().replace("\uFEFF", "").trim();

                if (resp.statusCode() == 200) {
                    JSONObject json = new JSONObject(body);
                    JSONArray arr = json.getJSONArray("productos");

                    List<JSONObject> out = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        out.add(arr.getJSONObject(i));
                    }

                    callback.onSuccess(out);
                } else {
                    JSONObject errorJson = new JSONObject(body);
                    callback.onError(errorJson.optString("error", "Error HTTP " + resp.statusCode()));
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar productos: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud: " + evt.getSource().getException().getMessage()));
        new Thread(task).start();
    }

    // 🔄 Listar categorías de productos
    public static void listCategoriasProductos(ListCallback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/categorias-productos",
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener categorías"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();

            try {
                String body = resp.body().replace("\uFEFF", "").trim();

                if (resp.statusCode() == 200) {
                    JSONObject json = new JSONObject(body);
                    JSONArray arr = json.getJSONArray("categorias");

                    List<JSONObject> out = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        out.add(arr.getJSONObject(i));
                    }

                    callback.onSuccess(out);
                } else {
                    JSONObject errorJson = new JSONObject(body);
                    callback.onError(errorJson.optString("error", "Error HTTP " + resp.statusCode()));
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar categorías: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud: " + evt.getSource().getException().getMessage()));
        new Thread(task).start();
    }

    // ➕ Crear producto
    public static void createProducto(Producto producto, CrudCallback callback) {
        JSONObject body = producto.toJson();

        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0), // application/json
                Optional.of("Error al crear producto"),
                Optional.of("application/json"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();
                System.out.println("Respuesta del servidor (status " + r.statusCode() + "): " + responseBody);

                if (r.statusCode() == 200 || r.statusCode() == 201) {
                    // Intentar parsear la respuesta JSON
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);

                        // Verificar diferentes formatos de respuesta exitosa
                        if (responseJson.has("success") && responseJson.getBoolean("success")) {
                            // Formato: {"success": true, "id": 123, "message": "..."}
                            callback.onSuccess();
                        } else if (responseJson.has("id")) {
                            // Formato: {"id": 123, "message": "..."}
                            callback.onSuccess();
                        } else if (responseJson.has("message")) {
                            // Formato: {"message": "Producto creado exitosamente"}
                            callback.onSuccess();
                        } else {
                            // Formato desconocido pero status 200
                            callback.onSuccess();
                        }
                    } catch (Exception e) {
                        // Si no es JSON válido pero el status es 200, asumimos éxito
                        if (r.statusCode() == 200 || r.statusCode() == 201) {
                            callback.onSuccess();
                        } else {
                            callback.onError("Respuesta no es JSON válido: " + responseBody);
                        }
                    }
                } else {
                    // Intentar parsear error
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        String errorMsg = errorJson.optString("error", "Error HTTP " + r.statusCode());
                        callback.onError(errorMsg);
                    } catch (Exception e) {
                        callback.onError("Error HTTP " + r.statusCode() + ": " + responseBody);
                    }
                }
            } catch (Exception e) {
                callback.onError("Error al procesar respuesta: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> {
            Throwable ex = evt.getSource().getException();
            callback.onError("Error en solicitud: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    // ✏️ Actualizar producto
    public static void updateProducto(Producto producto, CrudCallback callback) {
        JSONObject body = producto.toJson();

        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(2), // PUT
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al actualizar producto"),
                Optional.of("application/json"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();
                System.out.println("Respuesta de actualización (status " + r.statusCode() + "): " + responseBody);

                if (r.statusCode() == 200) {
                    // Intentar parsear la respuesta
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);

                        if (responseJson.has("error")) {
                            callback.onError(responseJson.getString("error"));
                        } else {
                            callback.onSuccess();
                        }
                    } catch (Exception e) {
                        // Si no es JSON válido pero status 200, asumimos éxito
                        callback.onSuccess();
                    }
                } else {
                    // Intentar parsear error
                    try {
                        JSONObject errorJson = new JSONObject(responseBody);
                        String errorMsg = errorJson.optString("error", "Error HTTP " + r.statusCode());
                        callback.onError(errorMsg);
                    } catch (Exception e) {
                        callback.onError("Error HTTP " + r.statusCode() + ": " + responseBody);
                    }
                }
            } catch (Exception e) {
                callback.onError("Error al procesar respuesta: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud: " + evt.getSource().getException().getMessage()));
        new Thread(task).start();
    }

    // 🗑️ Eliminar producto
    public static void deleteProducto(int id, CrudCallback callback) {
        JSONObject body = new JSONObject();
        body.put("id", id);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(3), // DELETE
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al eliminar producto"),
                Optional.of("application/json"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();

                if (r.statusCode() == 200) {
                    callback.onSuccess();
                } else {
                    JSONObject errorJson = new JSONObject(responseBody);
                    String errorMsg = errorJson.optString("error", "Error HTTP " + r.statusCode());
                    callback.onError(errorMsg);
                }
            } catch (Exception e) {
                callback.onError("Error al procesar respuesta: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud: " + evt.getSource().getException().getMessage()));
        new Thread(task).start();
    }
}