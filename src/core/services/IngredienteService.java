package core.services;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import core.HTTPConnection;
import org.json.JSONArray;
import org.json.JSONObject;

public class IngredienteService {

    public interface ListCallback {
        void onSuccess(List<JSONObject> list);

        void onError(String error);
    }

    public interface CrudCallback {
        void onSuccess();

        void onError(String error);
    }

    // 🔄 Listar ingredientes
    public static void listIngredientes(ListCallback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/ingredientes",
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener ingredientes"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();

            try {
                String body = resp.body()
                        .replace("\uFEFF", "") // 🔥 BOM FIX
                        .trim();

                if (resp.statusCode() == 200) {
                    JSONObject json = new JSONObject(body);
                    JSONArray arr = json.getJSONArray("ingredientes");

                    List<JSONObject> out = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject ing = arr.getJSONObject(i);

                        // 🔧 Normalizar: siempre tener idCategoria
                        if (!ing.has("idCategoria") || ing.isNull("idCategoria")) {
                            ing.put("idCategoria", JSONObject.NULL);
                        }

                        out.add(ing);
                    }

                    callback.onSuccess(out);
                } else {
                    JSONObject errorJson = new JSONObject(body);
                    String errorMsg = errorJson.optString("error", "Error HTTP " + resp.statusCode());
                    callback.onError(errorMsg);
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar ingredientes: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> {
            Throwable cause = evt.getSource().getException();
            callback.onError("Error en solicitud: " + (cause != null ? cause.getMessage() : "Desconocido"));
        });

        new Thread(task).start();
    }

    // 🗑️ Eliminar ingrediente
    // 🗑️ Eliminar ingrediente - CORREGIDO: Sin body, ID en URL
    public static void deleteIngrediente(int id, CrudCallback callback) {
        // Usar parámetro en la URL: api/ingredientes?id=X
        String url = "api/ingredientes?id=" + id;

        System.out.println("DELETE ingrediente to: " + url);

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(3), // DELETE
                Optional.empty(), // NO BODY
                Optional.of(0),
                Optional.of("Error al eliminar ingrediente"),
                Optional.empty()); // No content-type para DELETE sin body

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();
                System.out.println("DELETE ingrediente response (status " + r.statusCode() + "): " + responseBody);

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

        task.setOnFailed(evt -> {
            Throwable cause = evt.getSource().getException();
            callback.onError("Error en solicitud: " + (cause != null ? cause.getMessage() : "Desconocido"));
        });

        new Thread(task).start();
    }

    // 💾 Guardar ingrediente (crear o actualizar)
    public static void saveIngrediente(JSONObject body, CrudCallback callback) {
        // 🔧 Validar categoría
        if (!body.has("idCategoria") || body.isNull("idCategoria")) {
            callback.onError("Categoría inválida");
            return;
        }

        // Determinar si es actualización o creación
        boolean isUpdate = body.has("id") && body.getInt("id") > 0;
        int method = isUpdate ? 2 : 1; // 2 = PUT, 1 = POST

        var task = HTTPConnection.getInstance().requestAsync(
                "api/ingredientes",
                Optional.of(method),
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al guardar ingrediente"),
                Optional.of("application/json"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();

                if (r.statusCode() == 200 || r.statusCode() == 201) {
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

        task.setOnFailed(evt -> {
            Throwable cause = evt.getSource().getException();
            callback.onError("Error en solicitud: " + (cause != null ? cause.getMessage() : "Desconocido"));
        });

        new Thread(task).start();
    }

    // 🔍 Buscar ingrediente por nombre
    // Buscar ingrediente por nombre
    public static void buscarIngredientePorNombre(String nombre, ListCallback callback) {
        // Primero obtener todos los ingredientes y filtrar localmente
        listIngredientes(new ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> ingredients) {
                String nombreLower = nombre.toLowerCase();
                List<JSONObject> resultados = ingredients.stream()
                        .filter(i -> i.optString("Nombre", "").toLowerCase().contains(nombreLower))
                        .collect(Collectors.toList());
                callback.onSuccess(resultados);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // 🔍 Obtener ingrediente por ID
    public static void obtenerIngredientePorId(int id, CrudCallbackWithData callback) {
        listIngredientes(new ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> ingredientes) {
                JSONObject ingrediente = ingredientes.stream()
                        .filter(ing -> ing.getInt("id") == id)
                        .findFirst()
                        .orElse(null);

                if (ingrediente != null) {
                    callback.onSuccess(ingrediente);
                } else {
                    callback.onError("Ingrediente no encontrado");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    // Interfaz para callback con datos
    public interface CrudCallbackWithData {
        void onSuccess(JSONObject data);

        void onError(String error);
    }
}