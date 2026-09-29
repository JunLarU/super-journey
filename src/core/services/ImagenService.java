package core.services;

import core.HTTPConnection;
import javafx.concurrent.Task;
import org.json.JSONObject;

import java.io.File;
import java.net.http.HttpResponse;

public class ImagenService {

    public interface UploadCallback {

        void onSuccess(String url);

        void onError(String error);
    }

    public static void subirImagenProducto(
            File archivo,
            UploadCallback callback) {

        Task<String> task = new Task<>() {

            @Override
            protected String call()
                    throws Exception {

                HttpResponse<String> response = HTTPConnection
                        .getInstance()
                        .uploadFile(
                                "api/productos/imagen",
                                archivo,
                                "imagen");

                String body = response.body().trim();

                if (response.statusCode() != 200
                        && response.statusCode() != 201) {

                    throw new Exception(
                            "HTTP "
                                    + response.statusCode()
                                    + ": "
                                    + body);
                }

                JSONObject json = new JSONObject(body);

                if (!json.optBoolean(
                        "success",
                        false)) {

                    throw new Exception(
                            json.optString(
                                    "error",
                                    "No se pudo subir la imagen"));
                }

                return json.getString("url");
            }
        };

        task.setOnSucceeded(event -> {

            callback.onSuccess(
                    task.getValue());
        });

        task.setOnFailed(event -> {

            Throwable error = task.getException();

            callback.onError(
                    error != null
                            ? error.getMessage()
                            : "Error desconocido");
        });

        new Thread(task).start();
    }
}