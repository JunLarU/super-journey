package core.services;

import java.net.http.HttpResponse;
import java.util.Optional;

import org.json.JSONObject;

import core.HTTPConnection;
import core.SessionManager;
import javafx.concurrent.Task;

public final class VentaService {

    private VentaService() {}

    private static final int GET = 0;
    private static final int POST = 1;
    private static final int BODY_JSON = 0;

    public interface JsonCallback {
        void onSuccess(JSONObject data);
        void onError(String error);
    }

    private static int getCurrentUserId() {
        SessionManager session = SessionManager.getInstance();
        if (session.isAuthenticated() && session.getCurrentUser() != null) {
            return session.getCurrentUser().getId();
        }
        return 1;
    }

    private static void request(String endpoint, int method, JSONObject body, JsonCallback callback) {
        Task<HttpResponse<String>> task = HTTPConnection.getInstance().requestAsync(
                endpoint,
                Optional.of(method),
                body == null ? Optional.empty() : Optional.of(body.toString()),
                Optional.of(BODY_JSON),
                Optional.of("Error de conexión"),
                Optional.of("No se pudo comunicar con el servidor. ")
        );

        task.setOnSucceeded(e -> {
            try {
                HttpResponse<String> response = task.getValue();
                if (response == null) {
                    callback.onError("Respuesta vacía del servidor.");
                    return;
                }

                String raw = response.body() == null ? "" : response.body().replace("\uFEFF", "").trim();
                JSONObject json;
                try {
                    json = raw.isEmpty() ? new JSONObject() : new JSONObject(raw);
                } catch (Exception parse) {
                    callback.onError("Respuesta no válida del servidor (HTTP " + response.statusCode() + "): " + raw);
                    return;
                }

                if (response.statusCode() >= 200 && response.statusCode() < 300
                        && json.optBoolean("success", false)) {
                    callback.onSuccess(json);
                } else {
                    callback.onError(json.optString("error", "HTTP " + response.statusCode()));
                }
            } catch (Exception ex) {
                callback.onError(ex.getMessage() == null ? "Error procesando respuesta" : ex.getMessage());
            }
        });

        task.setOnFailed(e -> callback.onError(
                task.getException() == null ? "Error de red" : task.getException().getMessage()
        ));

        Thread t = new Thread(task, "cafi-ventas-http");
        t.setDaemon(true);
        t.start();
    }

    public static void catalogo(JsonCallback cb) {
        request("api/ventas/catalogo", GET, null, cb);
    }

    public static void configuracionProducto(int idProducto, JsonCallback cb) {
        request("api/ventas/productos/" + idProducto + "/configuracion", GET, null, cb);
    }

    public static void cotizar(JSONObject venta, JsonCallback cb) {
        venta.put("idUsuario", getCurrentUserId());
        request("api/ventas/cotizar", POST, venta, cb);
    }

    public static void registrar(JSONObject venta, JsonCallback cb) {
        venta.put("idUsuario", getCurrentUserId());
        request("api/ventas", POST, venta, cb);
    }

    public static void listar(String desde, String hasta, JsonCallback cb) {
        request("api/ventas?desde=" + desde + "&hasta=" + hasta, GET, null, cb);
    }

    public static void obtener(int id, JsonCallback cb) {
        request("api/ventas/" + id, GET, null, cb);
    }

    public static void cancelar(int id, String motivo, String descripcion, JsonCallback cb) {
        JSONObject body = new JSONObject()
                .put("idUsuario", getCurrentUserId())
                .put("motivo", motivo == null ? "" : motivo)
                .put("descripcion", descripcion == null ? "" : descripcion);

        request("api/ventas/" + id + "/cancelar", POST, body, cb);
    }
}
