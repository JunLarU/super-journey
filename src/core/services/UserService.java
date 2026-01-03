package core.services;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import core.HTTPConnection;
import org.json.JSONObject;

public class UserService {

    public interface SignupCallback {
        void onSuccess(JSONObject response);

        void onError(String error);
    }

    public static void signup(
            String expediente,
            String nombre,
            String apellidoPaterno,
            String apellidoMaterno,
            String correo,
            String telefono,
            String nip,
            String tipo,
            SignupCallback callback) {
        System.out.println("[UserService] Iniciando signup...");

        JSONObject body = new JSONObject();
        body.put("expediente", expediente);
        body.put("nombre", nombre);
        body.put("apellidoPaterno", apellidoPaterno);
        body.put("apellidoMaterno", apellidoMaterno);
        body.put("correo", correo);
        body.put("telefono", telefono);
        body.put("nip", nip);
        body.put("tipo", tipo);

        // 💡 GUARDA EL TASK
        var task = HTTPConnection.getInstance().requestAsync(
                "api/users/signup",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al registrar usuario"),
                Optional.of("No se pudo conectar al servidor.\n"));

        System.out.println("[UserService] Task creado, registrando listeners...");

        task.setOnSucceeded(evt -> {
            System.out.println("[UserService] onSucceeded");

            HttpResponse<String> resp = task.getValue(); // correct way
            System.out.println("[UserService] HTTP status: " + resp.statusCode());
            System.out.println("[UserService] Body: " + resp.body());

            if (resp.statusCode() == 201) {
                callback.onSuccess(new JSONObject(resp.body()));
            } else {
                try {
                    String err = new JSONObject(resp.body()).optString("error", "Error desconocido");
                    System.out.println("[UserService] Error extraído: " + err);
                    callback.onError(err);
                } catch (Exception ex) {
                    System.out.println("[UserService] Excepción al parsear error -> " + ex.getMessage());
                    callback.onError("Error inesperado");
                }
            }
        });

        task.setOnFailed(evt -> {
            System.out.println("[UserService] onFailed");
            Throwable t = task.getException();
            if (t != null) {
                System.out.println("[UserService] Exception: " + t.getMessage());
            }
            callback.onError("Falló la petición HTTP");
        });

        // 🟢 Finalmente lanza el task en un thread
        new Thread(task).start();
    }

    public interface LoginCallback {
        void onSuccess(org.json.JSONObject userJson);

        void onError(String error);
    }

    public static void login(
            String expediente,
            String nip,
            LoginCallback callback) {
        System.out.println("[UserService] Iniciando login...");

        JSONObject body = new JSONObject();
        body.put("expediente", expediente);
        body.put("nip", nip);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/users/login",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al iniciar sesión"),
                Optional.of("No se pudo conectar al servidor.\n"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();
            System.out.println("[UserService] HTTP status (login): " + resp.statusCode());
            System.out.println("[UserService] Body (login): " + resp.body());

            if (resp.statusCode() == 200) {
                org.json.JSONObject json = new org.json.JSONObject(resp.body());
                org.json.JSONObject userData = json.getJSONObject("user");
                callback.onSuccess(userData);
            } else {
                try {
                    String err = new org.json.JSONObject(resp.body()).optString("error", "Error desconocido");
                    callback.onError(err);
                } catch (Exception e) {
                    callback.onError("Error inesperado");
                }
            }
        });

        task.setOnFailed(evt -> {
            Throwable t = task.getException();
            String msg = (t != null ? t.getMessage() : "Error de red");
            callback.onError("Login fallido: " + msg);
        });

        new Thread(task).start();
    }

    public interface GetAdminsCallback {
        void onSuccess(List<org.json.JSONObject> admins);

        void onError(String error);
    }

    public static void getAdmins(GetAdminsCallback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/users?role=admin",
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener administradores"),
                Optional.of("No se pudo conectar al servidor.\n"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();
            if (resp.statusCode() == 200) {
                org.json.JSONObject json = new org.json.JSONObject(resp.body());
                org.json.JSONArray arr = json.getJSONArray("admins");
                List<org.json.JSONObject> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getJSONObject(i));
                }
                callback.onSuccess(list);
            } else {
                try {
                    String err = new org.json.JSONObject(resp.body()).optString("error", "Error desconocido");
                    callback.onError(err);
                } catch (Exception e) {
                    callback.onError("Error inesperado");
                }
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud"));

        new Thread(task).start();
    }

    public interface DeleteCallback {
        void onSuccess();

        void onError(String error);
    }

    public static void deleteAdmin(String expediente, DeleteCallback callback) {
        JSONObject body = new JSONObject();
        body.put("expediente", expediente);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/users/delete",
                Optional.of(1),
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al eliminar administrador"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();
            if (resp.statusCode() == 200) {
                callback.onSuccess();
            } else {
                try {
                    String error = new JSONObject(resp.body()).optString("error", "Error desconocido");
                    callback.onError(error);
                } catch (Exception e) {
                    callback.onError("Error inesperado");
                }
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud"));

        new Thread(task).start();
    }

    public interface UpdateCallback {
        void onSuccess();

        void onError(String error);
    }

    public static void updateUser(JSONObject body, UpdateCallback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/users/update",
                Optional.of(1),
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al actualizar usuario"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();
            if (resp.statusCode() == 200) {
                callback.onSuccess();
            } else {
                try {
                    String error = new JSONObject(resp.body()).optString("error", "Error desconocido");
                    callback.onError(error);
                } catch (Exception e) {
                    callback.onError("Error inesperado");
                }
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud"));

        new Thread(task).start();
    }

}
