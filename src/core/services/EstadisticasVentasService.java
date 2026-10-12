package core.services;

import java.net.http.HttpResponse;
import java.util.Optional;

import org.json.JSONObject;

import core.HTTPConnection;
import javafx.concurrent.Task;

public final class EstadisticasVentasService {
    private EstadisticasVentasService() {}

    private static final int GET = 0;

    public interface Callback {
        void onSuccess(JSONObject json);
        void onError(String error);
    }

    private static void getUrl(String endpoint, Callback callback) {
        Task<HttpResponse<String>> task =
                HTTPConnection.getInstance().requestAsync(
                        endpoint,
                        Optional.of(GET),
                        Optional.empty(),
                        Optional.of(0),
                        Optional.of("Error"),
                        Optional.of("No se pudieron cargar las estadísticas.")
                );

        task.setOnSucceeded(e -> {
            try {
                HttpResponse<String> response = task.getValue();
                JSONObject json = new JSONObject(response.body());

                if (response.statusCode() >= 200
                        && response.statusCode() < 300
                        && json.optBoolean("success", false)) {
                    callback.onSuccess(json);
                } else {
                    callback.onError(
                            json.optString(
                                    "error",
                                    "HTTP " + response.statusCode()
                            )
                    );
                }
            } catch (Exception ex) {
                callback.onError(ex.getMessage());
            }
        });

        task.setOnFailed(e ->
                callback.onError(
                        task.getException() == null
                                ? "Error de red"
                                : task.getException().getMessage()
                )
        );

        Thread t = new Thread(task, "cafi-stats-http");
        t.setDaemon(true);
        t.start();
    }

    private static String range(String resource, String desde, String hasta) {
        return "api/estadisticas/" + resource
                + "?desde=" + desde
                + "&hasta=" + hasta;
    }

    public static void resumen(String d, String h, Callback cb) {
        getUrl(range("resumen", d, h), cb);
    }

    public static void serieDiaria(String d, String h, Callback cb) {
        getUrl(range("serie-diaria", d, h), cb);
    }

    public static void productos(String d, String h, int limit, Callback cb) {
        getUrl(
                range("productos", d, h) + "&limit=" + limit,
                cb
        );
    }

    public static void categorias(String d, String h, Callback cb) {
        getUrl(range("categorias", d, h), cb);
    }

    public static void pagos(String d, String h, Callback cb) {
        getUrl(range("pagos", d, h), cb);
    }

    public static void vendedores(String d, String h, Callback cb) {
        getUrl(range("vendedores", d, h), cb);
    }

    public static void tamanos(String d, String h, Callback cb) {
        getUrl(range("tamanos", d, h), cb);
    }

    public static void modificaciones(String d, String h, Callback cb) {
        getUrl(range("modificaciones", d, h), cb);
    }
}
