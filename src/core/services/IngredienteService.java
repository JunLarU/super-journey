package core.services;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import core.HTTPConnection;
import org.json.JSONArray;
import org.json.JSONObject;

public class IngredienteService {

    public interface ListCallback {
        void onSuccess(List<JSONObject> list);

        void onError(String error);
    }

    public static void listIngredientes(ListCallback callback) {

        var task = HTTPConnection.getInstance().requestAsync(
                "api/ingredientes",
                Optional.of(0),
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

                JSONObject json = new JSONObject(body);
                JSONArray arr = json.getJSONArray("ingredientes");

                List<JSONObject> out = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject ing = arr.getJSONObject(i);

                    // 🔧 Normalizar: siempre tener idCategoria
                    if (!ing.has("idCategoria")) {
                        ing.put("idCategoria", JSONObject.NULL);
                    }

                    out.add(ing);
                }

                callback.onSuccess(out);

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar ingredientes");
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud"));
        new Thread(task).start();
    }

    public interface CrudCallback {
        void onSuccess();

        void onError(String error);
    }

    public static void deleteIngrediente(int id, CrudCallback callback) {

        JSONObject body = new JSONObject();
        body.put("id", id);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/ingredientes",
                Optional.of(1), // ✅ DELETE
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al eliminar ingrediente"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            if (r.statusCode() == 200) {
                callback.onSuccess();
            } else {
                try {
                    String err = new JSONObject(
                            r.body().replace("\uFEFF", "")).optString("error", "Error desconocido");

                    callback.onError(err);
                } catch (Exception e) {
                    callback.onError("Error HTTP " + r.statusCode());
                }
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud"));
        new Thread(task).start();
    }

    public static void saveIngrediente(JSONObject body, CrudCallback callback) {

        // 🔧 Validar categoría
        if (!body.has("idCategoria") || body.isNull("idCategoria")) {
            callback.onError("Categoría inválida");
            return;
        }

        // POST para crear, PUT para actualizar
        boolean isUpdate = body.has("id");

        var task = HTTPConnection.getInstance().requestAsync(
                "api/ingredientes",
                Optional.of(isUpdate ? 2 : 1),
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al guardar ingrediente"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            if (r.statusCode() == 200 || r.statusCode() == 201) {
                callback.onSuccess();
            } else {
                try {
                    String err = new JSONObject(
                            r.body().replace("\uFEFF", "")).optString("error", "Error desconocido");

                    callback.onError(err);

                } catch (Exception e) {
                    callback.onError("Error HTTP " + r.statusCode());
                }
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud"));
        new Thread(task).start();
    }

}
