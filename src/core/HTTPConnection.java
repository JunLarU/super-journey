package core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.ContentHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.time.Duration;
import java.util.Optional;

import app.controllers.FXDialogs;
import javafx.concurrent.Task;

public class HTTPConnection {
    //Esta clase maneja las conexiones HTTP hacia el servidor
    //Vamos a hacer que sea singleton para evitar crear muchas instancias innecesarias
    private static HTTPConnection instance = null;
    private HttpClient client;

    private String baseURL = "http://localhost/";
    private int timeoutSeconds = 10;


    private HTTPConnection(){
        rebuildClient();
    }
    private void rebuildClient() {
        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    public static HTTPConnection getInstance(){
        if(instance == null){
            instance = new HTTPConnection();
        }
        return instance;
    }

    public HttpResponse<String> sendRequest(String endpoint, Optional<Integer> method,  Optional<String> body,Optional<Integer> bodyOption) throws Exception{
        if(!method.isPresent()){
            method = Optional.of(0); //Por defecto GET
        }
        if(!bodyOption.isPresent()){
            bodyOption = Optional.of(0); //Por defecto sin body
        }
        String contentType=null;
        switch (bodyOption.get()){
            case 0: //Application/JSON
                contentType = "application/json";
                break;
            case 1: //Multipart/Form-Data
                contentType = "multipart/form-data";
                break;
            default:
                contentType = "application/json";
                break;
        }

        HttpRequest request = null;
        switch (Integer.valueOf(method.get())){
            case 0: //GET
                request = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + endpoint))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .GET()
                        .build();
                break;
            case 1: //POST
                request = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + endpoint))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body.orElse("")))
                        .build();
                        break;
            case 2: //PUT
                request = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + endpoint))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(body.orElse("")))
                        .build();
                break;

            case 3: //DELETE
                request = HttpRequest.newBuilder()
                        .uri(URI.create(baseURL + endpoint))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .header("Content-Type", "application/json")
                        .DELETE()
                        .build();
        
            default:
                break;
        }


        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public Task<HttpResponse<String>> requestAsync(
        String endpoint,
        Optional<Integer> method,
        Optional<String> body,
        Optional<Integer> bodyOption,
        Optional<String> errorMessageHeader,
        Optional<String> errorMessageContent
) {
    return new Task<>() {
        @Override
        protected HttpResponse<String> call() throws Exception {
            try {
                return HTTPConnection.this.sendRequest(endpoint, method, body, bodyOption);

            } catch (Exception ex) {

                // Mostrar diálogo en el hilo de JavaFX
                javafx.application.Platform.runLater(() -> {
                    FXDialogs.error(
                            "Conexión fallida",
                            errorMessageHeader.orElse("No se pudo conectar al servidor"),
                            (errorMessageContent.orElse("") +
                            ex.getMessage() != null ? ex.getMessage() : "Error desconocido")
                    );
                });

                // IMPORTANTE: relanzar la excepción
                throw ex;
            }
        }
    };
}


    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }


}
