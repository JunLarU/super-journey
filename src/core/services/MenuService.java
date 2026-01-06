package core.services;

import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import core.HTTPConnection;
import core.data.Menus.MenuSemanal;
import core.data.Menus.Menu;
import core.data.Menus.MenuSeccion;
import core.data.Menus.SeccionMenu;
import core.data.Menus.SeccionProducto;
import org.json.JSONArray;
import org.json.JSONObject;

public class MenuService {
    public interface Callback {
        void onSuccess(JSONObject response);

        void onError(String error);
    }

    public interface ListCallback {
        void onSuccess(List<JSONObject> list);

        void onError(String error);
    }

    public interface CrudCallback {
        void onSuccess();

        void onError(String error);
    }

    public interface MenuSemanalCallback {
        void onSuccess(MenuSemanal menuSemanal);

        void onError(String error);
    }

    public interface SeccionesCallback {
        void onSuccess(List<SeccionMenu> secciones);

        void onError(String error);
    }

    // ============================================
    // SECCIONES DEL MENÚ
    // ============================================

    /**
     * Listar todas las secciones del menú
     */
    public static void listSecciones(SeccionesCallback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/secciones",
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener secciones"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();

            try {
                String body = resp.body().replace("\uFEFF", "").trim();

                // DEBUG: Ver qué está devolviendo el servidor
                System.out.println("[DEBUG] Response body (first 500 chars): " +
                        (body.length() > 500 ? body.substring(0, 500) + "..." : body));

                if (resp.statusCode() == 200) {
                    // Verificar si el body es JSON válido
                    if (!body.startsWith("{") && !body.startsWith("[")) {
                        // El servidor podría estar devolviendo HTML o texto de error
                        if (body.contains("<html") || body.contains("<!DOCTYPE")) {
                            callback.onError(
                                    "El servidor devolvió HTML en lugar de JSON. Verifique la configuración del servidor.");
                        } else {
                            callback.onError("Respuesta del servidor no es JSON válido: " +
                                    (body.length() > 200 ? body.substring(0, 200) + "..." : body));
                        }
                        return;
                    }

                    JSONObject json = new JSONObject(body);

                    if (!json.has("secciones")) {
                        callback.onError("Respuesta JSON no contiene campo 'secciones'");
                        return;
                    }

                    JSONArray arr = json.getJSONArray("secciones");
                    List<SeccionMenu> secciones = new ArrayList<>();

                    for (int i = 0; i < arr.length(); i++) {
                        try {
                            SeccionMenu seccion = new SeccionMenu(arr.getJSONObject(i));
                            secciones.add(seccion);
                        } catch (Exception e) {
                            System.err.println("Error procesando sección " + i + ": " + e.getMessage());
                        }
                    }

                    callback.onSuccess(secciones);
                } else {
                    // Intentar parsear error como JSON
                    try {
                        JSONObject errorJson = new JSONObject(body);
                        callback.onError(errorJson.optString("error", "Error HTTP " + resp.statusCode()));
                    } catch (Exception e) {
                        callback.onError("Error HTTP " + resp.statusCode() + ": " +
                                (body.length() > 200 ? body.substring(0, 200) + "..." : body));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar secciones: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> {
            Throwable ex = evt.getSource().getException();
            callback.onError("Error en solicitud: " + ex.getMessage());
        });
        new Thread(task).start();
    }

    /**
     * Verificar si un menú ya existe para una fecha y horario específicos
     */
    public static void verificarMenuExistente(String fecha, String horario, Callback callback) {
        String url = String.format("api/menus/verificar?fecha=%s&horario=%s", fecha, horario);

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al verificar menú"),
                Optional.of(""));

        handleCallback(task, callback);
    }

    /**
     * Obtener una sección por ID
     */
    public static void getSeccion(int id, Callback callback) {
        String url = "api/menus/secciones?id=" + id;

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener sección"),
                Optional.of(""));

        handleCallback(task, callback);
    }

    /**
     * Crear una nueva sección
     */
    public static void createSeccion(SeccionMenu seccion, CrudCallback callback) {
        JSONObject body = seccionToJson(seccion);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/secciones",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al crear sección"),
                Optional.of("application/json"));

        handleCrudResponse(task, callback, "Sección creada exitosamente");
    }

    public static void crearMenuIndividual(String fecha, String diaSemana, String horario,
            int numeroSemana, int anio, int idUsuario,
            Callback callback) {

        try {
            JSONObject body = new JSONObject();
            body.put("fecha", fecha);
            body.put("diaSemana", diaSemana);
            body.put("horario", horario);
            body.put("numeroSemana", numeroSemana);
            body.put("anio", anio);
            body.put("idUsuario", idUsuario);

            var task = HTTPConnection.getInstance().requestAsync(
                    "api/menus/semanal?action=crearIndividual",
                    Optional.of(1), // POST
                    Optional.of(body.toString()),
                    Optional.of(0),
                    Optional.of("Error al crear menú individual"),
                    Optional.of("application/json"));

            handleActionResponse(task, callback);

        } catch (Exception e) {
            callback.onError("Error al crear menú individual: " + e.getMessage());
        }
    }

    /**
     * Actualizar una sección existente
     */
    public static void updateSeccion(SeccionMenu seccion, CrudCallback callback) {
        JSONObject body = seccionToJson(seccion);
        body.put("id", seccion.getId());

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/secciones",
                Optional.of(2), // PUT
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al actualizar sección"),
                Optional.of("application/json"));

        handleCrudResponse(task, callback, "Sección actualizada exitosamente");
    }

    /**
     * Eliminar una sección
     */
    public static void deleteSeccion(int id, CrudCallback callback) {
        String url = "api/menus/secciones?id=" + id;

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(3), // DELETE
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al eliminar sección"),
                Optional.empty());

        handleCrudResponse(task, callback, "Sección eliminada exitosamente");
    }

    // ============================================
    // MENÚ SEMANAL
    // ============================================

    /**
     * Obtener menú de una semana específica
     */
    public static void getMenuSemanal(int semana, int anio, MenuSemanalCallback callback) {
        String url = String.format("api/menus/semanal?semana=%d&anio=%d", semana, anio);

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener menú semanal"),
                Optional.of(""));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();

            try {
                String body = resp.body().replace("\uFEFF", "").trim();
                System.out.println("[DEBUG] Response status: " + resp.statusCode());
                System.out.println("[DEBUG] Response body (first 500 chars): " +
                        (body.length() > 500 ? body.substring(0, 500) + "..." : body));

                if (resp.statusCode() == 200) {
                    JSONObject json = new JSONObject(body);

                    // Verificar si hay error en la respuesta
                    if (json.has("error")) {
                        callback.onError(json.getString("error"));
                        return;
                    }

                    // Verificar si hay menús
                    if (!json.has("menus")) {
                        // No hay menús para esta semana, retornar objeto vacío
                        MenuSemanal menuSemanal = new MenuSemanal(semana, anio);
                        callback.onSuccess(menuSemanal);
                        return;
                    }

                    JSONArray menusArray = json.getJSONArray("menus");

                    if (menusArray.length() == 0) {
                        // Array vacío, retornar objeto vacío
                        MenuSemanal menuSemanal = new MenuSemanal(semana, anio);
                        callback.onSuccess(menuSemanal);
                        return;
                    }

                    // Crear objeto MenuSemanal
                    MenuSemanal menuSemanal = new MenuSemanal(semana, anio);

                    // Parsear menús diarios
                    for (int i = 0; i < menusArray.length(); i++) {
                        try {
                            JSONObject menuJson = menusArray.getJSONObject(i);
                            Menu menu = parseMenuFromJSON(menuJson);

                            // Cargar secciones del menú
                            if (menuJson.has("Secciones")) {
                                JSONArray seccionesArray = menuJson.getJSONArray("Secciones");
                                List<MenuSeccion> secciones = new ArrayList<>();

                                for (int j = 0; j < seccionesArray.length(); j++) {
                                    JSONObject seccionJson = seccionesArray.getJSONObject(j);
                                    MenuSeccion menuSeccion = MenuSeccion.fromJSON(seccionJson);
                                    secciones.add(menuSeccion);
                                }
                                menu.setSecciones(secciones);
                            }

                            menuSemanal.agregarMenu(menu);

                        } catch (Exception e) {
                            System.err.println("Error procesando menú " + i + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }

                    callback.onSuccess(menuSemanal);
                } else {
                    // Intentar parsear error
                    try {
                        JSONObject errorJson = new JSONObject(body);
                        callback.onError(errorJson.optString("error", "Error HTTP " + resp.statusCode()));
                    } catch (Exception e) {
                        callback.onError("Error HTTP " + resp.statusCode() + ": " +
                                (body.length() > 200 ? body.substring(0, 200) + "..." : body));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar menú: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> {
            Throwable ex = task.getException();
            callback.onError("Error en solicitud: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    /**
     * Generar menú semanal a partir de una fecha
     */
    public static void generarMenuSemanal(String fechaInicio, int idUsuario, Callback callback) {
        JSONObject body = new JSONObject();
        body.put("fechaInicio", fechaInicio);
        body.put("idUsuario", idUsuario);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/semanal?action=generar",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al generar menú semanal"),
                Optional.of("application/json"));

        handleActionResponse(task, callback);
    }

    /**
     * Asignar una sección a un menú específico
     */
    public static void asignarSeccionMenu(int idMenu, int idSeccion, int idUsuario, Callback callback) {
        JSONObject body = new JSONObject();
        body.put("idMenu", idMenu);
        body.put("idSeccion", idSeccion);
        body.put("idUsuario", idUsuario);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/semanal?action=asignar",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al asignar sección"),
                Optional.of("application/json"));

        handleActionResponse(task, callback);
    }

    /**
     * Remover una sección de un menú
     */
    public static void removerSeccionMenu(int idAsignacion, Callback callback) {
        JSONObject body = new JSONObject();
        body.put("id", idAsignacion);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/semanal?action=remover",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al remover sección"),
                Optional.of("application/json"));

        handleActionResponse(task, callback);
    }

    /**
     * Actualizar orden de las secciones en un menú
     */
    public static void updateOrdenSecciones(int idMenu, List<Integer> idsSecciones, Callback callback) {
        JSONObject body = new JSONObject();
        body.put("idMenu", idMenu);

        JSONArray seccionesArray = new JSONArray();
        for (Integer id : idsSecciones) {
            seccionesArray.put(id);
        }
        body.put("secciones", seccionesArray);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/semanal?action=orden",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al actualizar orden"),
                Optional.of("application/json"));

        handleActionResponse(task, callback);
    }

    /**
     * Eliminar menú completo de una semana
     */
    /**
     * Eliminar menú completo de una semana
     */
    public static void eliminarMenuCompleto(int semana, int anio, Callback callback) {
        // Usar GET con parámetros en URL
        String url = String.format("api/menus/semanal?action=eliminar&semana=%d&anio=%d", semana, anio);

        System.out.println("[DEBUG] Eliminar menú - URL: " + url);

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(1), // POST
                Optional.of(""), // Body vacío
                Optional.of(0),
                Optional.of("Error al eliminar menú"),
                Optional.of("application/json"));

        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();
                System.out.println("[DEBUG] Eliminar respuesta: " + responseBody);

                if (r.statusCode() == 200) {
                    JSONObject responseJson = new JSONObject(responseBody);
                    callback.onSuccess(responseJson);
                } else {
                    callback.onError("Error HTTP " + r.statusCode() + ": " + responseBody);
                }
            } catch (Exception e) {
                callback.onError("Error procesando respuesta: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> {
            callback.onError("Error en solicitud: " + evt.getSource().getException().getMessage());
        });

        new Thread(task).start();
    }

    // ============================================
    // MENÚ DEL DÍA
    // ============================================

    /**
     * Obtener menú del día actual
     */
    public static void getMenuHoy(Callback callback) {
        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/hoy",
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener menú del día"),
                Optional.of(""));

        handleCallback(task, callback);
    }

    /**
     * Obtener menú por fecha específica
     */
    public static void getMenuPorFecha(String fecha, Callback callback) {
        String url = "api/menus/fecha?fecha=" + fecha;

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(0), // GET
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al obtener menú por fecha"),
                Optional.of(""));

        handleCallback(task, callback);
    }

    // ============================================
    // PRODUCTOS DE SECCIONES
    // ============================================

    /**
     * Agregar producto a una sección
     */
    public static void agregarProductoSeccion(int idSeccion, int idProducto, boolean destacado, CrudCallback callback) {
        JSONObject body = new JSONObject();
        body.put("idSeccion", idSeccion);
        body.put("idProducto", idProducto);
        body.put("destacado", destacado);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/secciones/productos",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al agregar producto"),
                Optional.of("application/json"));

        handleCrudResponse(task, callback, "Producto agregado exitosamente");
    }

    /**
     * Remover producto de una sección
     */
    public static void removerProductoSeccion(int idSeccion, int idProducto, CrudCallback callback) {
        String url = String.format("api/menus/secciones/productos?idSeccion=%d&idProducto=%d",
                idSeccion, idProducto);

        var task = HTTPConnection.getInstance().requestAsync(
                url,
                Optional.of(3), // DELETE
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error al remover producto"),
                Optional.empty());

        handleCrudResponse(task, callback, "Producto removido exitosamente");
    }

    /**
     * Actualizar orden de productos en sección
     */
    public static void updateOrdenProductos(int idSeccion, List<Integer> idsProductos, Callback callback) {
        JSONObject body = new JSONObject();
        body.put("idSeccion", idSeccion);

        JSONArray productosArray = new JSONArray();
        for (Integer id : idsProductos) {
            productosArray.put(id);
        }
        body.put("productos", productosArray);

        var task = HTTPConnection.getInstance().requestAsync(
                "api/menus/secciones/productos/orden",
                Optional.of(1), // POST
                Optional.of(body.toString()),
                Optional.of(0),
                Optional.of("Error al actualizar orden de productos"),
                Optional.of("application/json"));

        handleActionResponse(task, callback);
    }

    // ============================================
    // MÉTODOS AUXILIARES
    // ============================================

    /**
     * Convertir SeccionMenu a JSON
     */
    private static JSONObject seccionToJson(SeccionMenu seccion) {
        JSONObject json = new JSONObject();

        if (seccion.getId() > 0) {
            json.put("id", seccion.getId());
        }

        json.put("nombre", seccion.getNombre());
        json.put("descripcion", seccion.getDescripcion());
        json.put("color", seccion.getColor());
        json.put("activo", seccion.isActivo());

        if (seccion.getUrlFoto() != null && !seccion.getUrlFoto().isEmpty()) {
            json.put("urlFoto", seccion.getUrlFoto());
        }

        // Agregar productos si existen
        if (!seccion.getProductos().isEmpty()) {
            JSONArray productosArray = new JSONArray();

            for (SeccionProducto producto : seccion.getProductos()) {
                JSONObject productoJson = new JSONObject();
                productoJson.put("idProducto", producto.getIdProducto());
                productoJson.put("destacado", producto.isDestacado());
                productoJson.put("orden", producto.getOrden());
                productosArray.put(productoJson);
            }

            json.put("productos", productosArray);
        }

        return json;
    }

    /**
     * Manejar respuesta de operaciones CRUD
     */
    private static void handleCrudResponse(javafx.concurrent.Task<HttpResponse<String>> task, CrudCallback callback,
            String successMessage) {
        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();

                // DEBUG
                System.out.println("[DEBUG] CRUD Response status: " + r.statusCode());
                System.out.println("[DEBUG] CRUD Response body: " +
                        (responseBody.length() > 300 ? responseBody.substring(0, 300) + "..." : responseBody));

                if (r.statusCode() == 200 || r.statusCode() == 201) {
                    try {
                        // Primero verificar si es JSON
                        if (responseBody.startsWith("{") || responseBody.startsWith("[")) {
                            JSONObject responseJson = new JSONObject(responseBody);

                            if (responseJson.has("success") && responseJson.getBoolean("success")) {
                                callback.onSuccess();
                            } else if (responseJson.has("error")) {
                                callback.onError(responseJson.getString("error"));
                            } else {
                                callback.onSuccess();
                            }
                        } else {
                            // Si no es JSON pero el código es exitoso
                            callback.onSuccess();
                        }
                    } catch (Exception e) {
                        if (r.statusCode() == 200 || r.statusCode() == 201) {
                            callback.onSuccess();
                        } else {
                            callback.onError("Respuesta no es JSON válido: " + responseBody);
                        }
                    }
                } else {
                    try {
                        if (responseBody.startsWith("{") || responseBody.startsWith("[")) {
                            JSONObject errorJson = new JSONObject(responseBody);
                            String errorMsg = errorJson.optString("error", "Error HTTP " + r.statusCode());
                            callback.onError(errorMsg);
                        } else {
                            callback.onError("Error HTTP " + r.statusCode() + ": " + responseBody);
                        }
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

    /**
     * Manejar respuesta de acciones específicas
     */
    private static void handleActionResponse(javafx.concurrent.Task<HttpResponse<String>> task, Callback callback) {
        task.setOnSucceeded(evt -> {
            HttpResponse<String> r = task.getValue();

            try {
                String responseBody = r.body().replace("\uFEFF", "").trim();

                if (r.statusCode() == 200 || r.statusCode() == 201) {
                    try {
                        JSONObject responseJson = new JSONObject(responseBody);

                        if (responseJson.has("error")) {
                            callback.onError(responseJson.getString("error"));
                        } else {
                            callback.onSuccess(responseJson);
                        }
                    } catch (Exception e) {
                        JSONObject response = new JSONObject();
                        response.put("success", true);
                        response.put("message", "Operación completada");
                        callback.onSuccess(response);
                    }
                } else {
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

    /**
     * Manejar callback genérico
     */
    private static void handleCallback(javafx.concurrent.Task<HttpResponse<String>> task, Callback callback) {
        task.setOnSucceeded(evt -> {
            HttpResponse<String> resp = task.getValue();

            try {
                String body = resp.body().replace("\uFEFF", "").trim();

                if (resp.statusCode() == 200) {
                    JSONObject json = new JSONObject(body);
                    callback.onSuccess(json);
                } else {
                    JSONObject errorJson = new JSONObject(body);
                    callback.onError(errorJson.optString("error", "Error HTTP " + resp.statusCode()));
                }

            } catch (Exception e) {
                e.printStackTrace();
                callback.onError("Error al procesar respuesta: " + e.getMessage());
            }
        });

        task.setOnFailed(evt -> callback.onError("Error en solicitud: " + evt.getSource().getException().getMessage()));
        new Thread(task).start();
    }

    /**
     * Método para parsear JSON a objeto Menu
     */
    public static Menu parseMenuFromJSON(JSONObject json) {
        try {
            Menu menu = new Menu();

            // Usar los nombres exactos de los campos del JSON
            if (json.has("ID")) {
                menu.setId(json.getInt("ID"));
            }

            if (json.has("Fecha")) {
                menu.setFecha(json.getString("Fecha"));
            }

            if (json.has("DiaSemana")) {
                menu.setDiaSemana(json.getString("DiaSemana"));
            }

            if (json.has("Horario")) {
                menu.setHorario(json.getString("Horario"));
            }

            if (json.has("NumeroSemana")) {
                menu.setNumeroSemana(json.getInt("NumeroSemana"));
            }

            if (json.has("Anio")) {
                menu.setAnio(json.getInt("Anio"));
            }

            if (json.has("Activo")) {
                // CORRECCIÓN: Manejar tanto string como integer
                Object activoObj = json.get("Activo");
                if (activoObj instanceof Integer) {
                    menu.setActivo(((Integer) activoObj) == 1);
                } else if (activoObj instanceof String) {
                    String activoStr = (String) activoObj;
                    menu.setActivo(activoStr.equals("1") || activoStr.equalsIgnoreCase("true"));
                } else if (activoObj instanceof Boolean) {
                    menu.setActivo((Boolean) activoObj);
                } else {
                    menu.setActivo(true); // Por defecto
                }
            } else {
                menu.setActivo(true); // Por defecto si no viene el campo
            }

            if (json.has("FechaCreacion")) {
                menu.setFechaCreacion(json.getString("FechaCreacion"));
            }

            // Parsear secciones si existen
            if (json.has("Secciones")) {
                JSONArray seccionesArray = json.getJSONArray("Secciones");
                List<MenuSeccion> secciones = new ArrayList<>();

                for (int i = 0; i < seccionesArray.length(); i++) {
                    try {
                        JSONObject seccionJson = seccionesArray.getJSONObject(i);
                        MenuSeccion menuSeccion = MenuSeccion.fromJSON(seccionJson);
                        secciones.add(menuSeccion);
                    } catch (Exception e) {
                        System.err.println("Error procesando sección " + i + ": " + e.getMessage());
                    }
                }

                menu.setSecciones(secciones);
            }

            return menu;

        } catch (Exception e) {
            System.err.println("Error en parseMenuFromJSON: " + e.getMessage());
            e.printStackTrace();
            return new Menu(); // Retornar menú vacío en caso de error
        }
    }

    /**
     * Método para parsear JSON a objeto MenuSemanal
     */
    public static MenuSemanal parseMenuSemanalFromJSON(JSONObject json) {
        if (!json.has("semana") || !json.has("anio")) {
            return null;
        }

        int semana = json.getInt("semana");
        int anio = json.getInt("anio");

        MenuSemanal menuSemanal = new MenuSemanal(semana, anio);

        if (json.has("menus")) {
            JSONArray menusArray = json.getJSONArray("menus");

            for (int i = 0; i < menusArray.length(); i++) {
                JSONObject menuJson = menusArray.getJSONObject(i);
                Menu menu = parseMenuFromJSON(menuJson);

                // Crear clave para el mapa
                String clave = menu.getDiaSemana() + "-" + menu.getHorario();

                // Usar reflexión para acceder al mapa privado
                try {
                    var field = MenuSemanal.class.getDeclaredField("menusPorDiaHorario");
                    field.setAccessible(true);
                    Map<String, Menu> map = (Map<String, Menu>) field.get(menuSemanal);
                    map.put(clave, menu);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return menuSemanal;
    }
}