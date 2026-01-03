package core.services;

import core.HTTPConnection;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductoService {

    public interface ListCallback {
        void onSuccess(List<JSONObject> productos);
        void onError(String error);
    }

    

    public static void listProductos(ListCallback cb) {

        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(0),
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al cargar productos"),
                Optional.of("")
        );

        task.setOnSucceeded(e -> {
            try {
                JSONObject json = new JSONObject(task.getValue().body().trim());
                JSONArray arr = json.getJSONArray("productos");

                List<JSONObject> out = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++)
                    out.add(arr.getJSONObject(i));

                cb.onSuccess(out);

            } catch (Exception ex) {
                cb.onError("Error al procesar productos");
            }
        });

        task.setOnFailed(e -> cb.onError("Error HTTP"));
        new Thread(task).start();
    }

    public static void saveProducto(JSONObject body, Runnable ok, java.util.function.Consumer<String> err) {

        boolean update = body.has("id");

        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(update ? 2 : 1),
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al guardar producto"),
                Optional.of("")
        );

        task.setOnSucceeded(e -> {
            HttpResponse<String> r = task.getValue();
            if (r.statusCode() == 200 || r.statusCode() == 201)
                ok.run();
            else
                err.accept("Error HTTP " + r.statusCode());
        });

        task.setOnFailed(e -> err.accept("Error de red"));
        new Thread(task).start();
    }

    public static void deleteProducto(int id, Runnable ok, java.util.function.Consumer<String> err) {

        JSONObject body = new JSONObject().put("id", id);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/productos",
                Optional.of(1),
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al eliminar producto"),
                Optional.of("")
        );

        task.setOnSucceeded(e -> {
            if (task.getValue().statusCode() == 200)
                ok.run();
            else
                err.accept("No se pudo eliminar");
        });

        task.setOnFailed(e -> err.accept("Error de red"));
        new Thread(task).start();
    }
}
