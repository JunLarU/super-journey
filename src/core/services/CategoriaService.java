package core.services;

import core.HTTPConnection;
import javafx.concurrent.Task;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoriaService {

    // ==========================================
    // CALLBACK PROPIO DEL SERVICE
    // ==========================================
    public interface ListCallback {
        void onSuccess(List<JSONObject> categorias);
        void onError(String error);
    }

    // ==========================================
    // LISTAR CATEGORÍAS DE PRODUCTOS
    // ==========================================
    public static void listCategoriasProductos(ListCallback callback) {

        HTTPConnection http = HTTPConnection.getInstance();

        Task<HttpResponse<String>> task = http.requestAsync(
                "categorias_productos/list",   // ajusta endpoint si es necesario
                Optional.of(0),                // GET
                Optional.empty(),
                Optional.empty(),
                Optional.of("Error al cargar categorías"),
                Optional.of("No se pudieron obtener las categorías: ")
        );

        task.setOnSucceeded(e -> {
            try {
                HttpResponse<String> response = task.getValue();

                if (response.statusCode() != 200) {
                    callback.onError("HTTP " + response.statusCode());
                    return;
                }

                JSONObject json = new JSONObject(response.body());
                JSONArray arr = json.getJSONArray("categorias");

                List<JSONObject> categorias = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    categorias.add(arr.getJSONObject(i));
                }

                callback.onSuccess(categorias);

            } catch (Exception ex) {
                callback.onError(ex.getMessage());
            }
        });

        task.setOnFailed(e ->
                callback.onError(task.getException().getMessage())
        );

        new Thread(task).start();
    }
}
