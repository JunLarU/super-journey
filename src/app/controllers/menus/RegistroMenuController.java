package app.controllers.menus;

import core.data.Menus.MenuSemanal;
import core.data.Menus.Menu;
import core.data.Menus.MenuSeccion;
import core.data.Menus.SeccionMenu;
import core.services.MenuService;
import core.SessionManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controlador para el formulario de registro/edición de menús semanales
 */
public class RegistroMenuController {

    // COMPONENTES FXML
    @FXML
    private Label lblTitulo;
    @FXML
    private Spinner<Integer> spinAnio;
    @FXML
    private Spinner<Integer> spinSemana;
    @FXML
    private Button btnCalcularFechas;
    @FXML
    private Label lblRangoFechas;

    @FXML
    private Label lblFechaLunesD, lblFechaMartesD, lblFechaMiercolesD, lblFechaJuevesD, lblFechaViernesD;
    @FXML
    private Label lblFechaLunesC, lblFechaMartesC, lblFechaMiercolesC, lblFechaJuevesC, lblFechaViernesC;

    @FXML
    private ListView<String> listLunesDesayuno, listMartesDesayuno, listMiercolesDesayuno;
    @FXML
    private ListView<String> listJuevesDesayuno, listViernesDesayuno;
    @FXML
    private ListView<String> listLunesComida, listMartesComida, listMiercolesComida;
    @FXML
    private ListView<String> listJuevesComida, listViernesComida;

    @FXML
    private Button btnGuardar, btnCancelar;
    @FXML
    private Label lblStatus;

    @FXML
    private Button btnAgregarLunesDesayuno, btnAgregarMartesDesayuno, btnAgregarMiercolesDesayuno,
            btnAgregarJuevesDesayuno, btnAgregarViernesDesayuno,
            btnAgregarLunesComida, btnAgregarMartesComida, btnAgregarMiercolesComida,
            btnAgregarJuevesComida, btnAgregarViernesComida;

    // DATOS
    private final SessionManager session = SessionManager.getInstance();
    private boolean modoEdicion = false;
    private boolean modoVisualizacion = false;
    private int numeroSemana = 0;
    private int anio = 0;

    private Map<String, LocalDate> mapaFechas = new HashMap<>();
    private Map<String, ListView<String>> mapaListas = new HashMap<>();
    private Map<String, List<Integer>> seccionesSeleccionadas = new HashMap<>();
    private Map<String, List<Integer>> asignacionesExistentes = new HashMap<>();
    private List<SeccionMenu> seccionesDisponibles = new ArrayList<>();
    private Map<Integer, SeccionMenu> mapaSecciones = new HashMap<>();
    private Map<String, Integer> mapaIdMenus = new HashMap<>();

    // INICIALIZACIÓN
    @FXML
    public void initialize() {
        configurarSpinners();
        configurarMapaListas();
        inicializarEstructuras();
        lblStatus.setText("Configura el menú para cada día y horario");

        // Cargar secciones disponibles en segundo plano
        new Thread(this::cargarSeccionesDisponibles).start();
    }

    private void configurarSpinners() {
        LocalDate hoy = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        int anioHoy = hoy.getYear();
        int semanaHoy = hoy.get(weekFields.weekOfWeekBasedYear());

        SpinnerValueFactory<Integer> factoryAnio = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                anioHoy - 1, anioHoy + 2, anioHoy);
        spinAnio.setValueFactory(factoryAnio);

        SpinnerValueFactory<Integer> factorySemana = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 52, semanaHoy);
        spinSemana.setValueFactory(factorySemana);
    }

    private void configurarMapaListas() {
        // Mapear listas
        mapaListas.put("Lunes-Desayuno", listLunesDesayuno);
        mapaListas.put("Martes-Desayuno", listMartesDesayuno);
        mapaListas.put("Miércoles-Desayuno", listMiercolesDesayuno);
        mapaListas.put("Jueves-Desayuno", listJuevesDesayuno);
        mapaListas.put("Viernes-Desayuno", listViernesDesayuno);
        mapaListas.put("Lunes-Comida", listLunesComida);
        mapaListas.put("Martes-Comida", listMartesComida);
        mapaListas.put("Miércoles-Comida", listMiercolesComida);
        mapaListas.put("Jueves-Comida", listJuevesComida);
        mapaListas.put("Viernes-Comida", listViernesComida);

        // Configurar doble click para eliminar
        for (ListView<String> lista : mapaListas.values()) {
            lista.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !modoVisualizacion) {
                    eliminarSeccionSeleccionada(lista);
                }
            });
        }
    }

    private void inicializarEstructuras() {
        String[] dias = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes" };
        String[] horarios = { "Desayuno", "Comida" };

        for (String dia : dias) {
            for (String horario : horarios) {
                String clave = dia + "-" + horario;
                seccionesSeleccionadas.put(clave, new ArrayList<>());
                asignacionesExistentes.put(clave, new ArrayList<>());
                mapaIdMenus.put(clave, 0); // Inicializar a 0
            }
        }
    }

    private void cargarSeccionesDisponibles() {
        Platform.runLater(() -> lblStatus.setText("Cargando secciones disponibles..."));

        MenuService.listSecciones(new MenuService.SeccionesCallback() {
            @Override
            public void onSuccess(List<SeccionMenu> secciones) {
                Platform.runLater(() -> {
                    seccionesDisponibles.clear();
                    mapaSecciones.clear();
                    seccionesDisponibles.addAll(secciones);

                    for (SeccionMenu seccion : secciones) {
                        mapaSecciones.put(seccion.getId(), seccion);
                    }

                    lblStatus.setText("✅ " + seccionesDisponibles.size() + " secciones cargadas");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar secciones: " + error);
                    mostrarAlerta("Error", "No se pudieron cargar las secciones: " + error);
                });
            }
        });
    }

    // CALCULAR FECHAS
    @FXML
    private void onCalcularFechasClicked() {
        calcularFechasSemana();
    }

    public void calcularFechasSemana() {
        numeroSemana = spinSemana.getValue();
        anio = spinAnio.getValue();

        try {
            // Calcular el lunes de la semana ISO
            LocalDate fechaReferencia = LocalDate.of(anio, 1, 4); // 4 de enero está siempre en la semana 1
            LocalDate primerLunes = fechaReferencia.with(DayOfWeek.MONDAY);

            // Ajustar para la semana solicitada
            LocalDate lunesObjetivo = primerLunes.plusWeeks(numeroSemana - 1);

            // DEBUG
            System.out.println("[DEBUG] Calculando semana " + numeroSemana + "/" + anio);
            System.out.println("[DEBUG] Lunes objetivo: " + lunesObjetivo);

            // Guardar fechas
            mapaFechas.put("Lunes", lunesObjetivo);
            mapaFechas.put("Martes", lunesObjetivo.plusDays(1));
            mapaFechas.put("Miércoles", lunesObjetivo.plusDays(2));
            mapaFechas.put("Jueves", lunesObjetivo.plusDays(3));
            mapaFechas.put("Viernes", lunesObjetivo.plusDays(4));

            // Actualizar labels
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            lblFechaLunesD.setText(lunesObjetivo.format(formatter));
            lblFechaMartesD.setText(lunesObjetivo.plusDays(1).format(formatter));
            lblFechaMiercolesD.setText(lunesObjetivo.plusDays(2).format(formatter));
            lblFechaJuevesD.setText(lunesObjetivo.plusDays(3).format(formatter));
            lblFechaViernesD.setText(lunesObjetivo.plusDays(4).format(formatter));

            lblFechaLunesC.setText(lunesObjetivo.format(formatter));
            lblFechaMartesC.setText(lunesObjetivo.plusDays(1).format(formatter));
            lblFechaMiercolesC.setText(lunesObjetivo.plusDays(2).format(formatter));
            lblFechaJuevesC.setText(lunesObjetivo.plusDays(3).format(formatter));
            lblFechaViernesC.setText(lunesObjetivo.plusDays(4).format(formatter));

            LocalDate viernes = lunesObjetivo.plusDays(4);
            lblRangoFechas.setText("📆 " + lunesObjetivo.format(formatter) + " al " + viernes.format(formatter));

            lblStatus.setText("✅ Fechas calculadas");

            // Cargar menús existentes si estamos en modo edición/visualización
            if (modoEdicion || modoVisualizacion) {
                cargarMenusExistentes();
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("❌ Error calculando fechas: " + e.getMessage());
        }
    }

    // AGREGAR SECCIONES
    private void abrirDialogoSeleccion(String dia, String horario) {
        if (seccionesDisponibles.isEmpty()) {
            mostrarAlerta("⚠️ Sin secciones", "No hay secciones disponibles.");
            return;
        }

        if (mapaFechas.isEmpty()) {
            mostrarAlerta("⚠️ Faltan fechas", "Calcula las fechas primero.");
            return;
        }

        String clave = dia + "-" + horario;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar Secciones - " + dia + " " + horario);
        dialog.setHeaderText("Selecciona las secciones para este menú:");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);
        content.setPrefHeight(400);

        Label lblInfo = new Label("Selecciona las secciones (puedes seleccionar varias):");

        ListView<CheckBox> listaSecciones = new ListView<>();
        ObservableList<CheckBox> items = FXCollections.observableArrayList();

        List<Integer> yaSeleccionados = seccionesSeleccionadas.get(clave);

        for (SeccionMenu seccion : seccionesDisponibles) {
            String texto = String.format("%s (%d productos)",
                    seccion.getNombre(),
                    seccion.getProductos().size());

            if (seccion.getDescripcion() != null && !seccion.getDescripcion().isEmpty()) {
                texto += "\n  " + seccion.getDescripcion();
            }

            CheckBox cb = new CheckBox(texto);
            cb.setUserData(seccion.getId());
            cb.setStyle("-fx-font-size: 12px; -fx-padding: 5 0;");
            cb.setWrapText(true);

            if (yaSeleccionados.contains(seccion.getId())) {
                cb.setSelected(true);
            }

            items.add(cb);
        }

        listaSecciones.setItems(items);
        listaSecciones.setPrefHeight(300);
        listaSecciones.setCellFactory(lv -> new ListCell<CheckBox>() {
            @Override
            protected void updateItem(CheckBox item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(item);
                }
            }
        });

        content.getChildren().addAll(lblInfo, listaSecciones);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                List<Integer> nuevasSelecciones = new ArrayList<>();
                for (CheckBox cb : items) {
                    if (cb.isSelected()) {
                        nuevasSelecciones.add((Integer) cb.getUserData());
                    }
                }
                seccionesSeleccionadas.put(clave, nuevasSelecciones);
                actualizarListaVisual(clave);
            }
        });
    }

    private void actualizarListaVisual(String clave) {
        ListView<String> lista = mapaListas.get(clave);
        if (lista == null)
            return;

        lista.getItems().clear();
        List<Integer> idsSeleccionados = seccionesSeleccionadas.get(clave);

        if (idsSeleccionados == null || idsSeleccionados.isEmpty()) {
            lista.getItems().add("(Sin secciones asignadas)");
            return;
        }

        for (Integer id : idsSeleccionados) {
            SeccionMenu seccion = mapaSecciones.get(id);
            if (seccion != null) {
                String item = String.format("📦 %s (%d productos)",
                        seccion.getNombre(),
                        seccion.getProductos().size());
                lista.getItems().add(item);
            }
        }
    }

    private void eliminarSeccionSeleccionada(ListView<String> lista) {
        String seleccionado = lista.getSelectionModel().getSelectedItem();
        if (seleccionado == null || seleccionado.equals("(Sin secciones asignadas)"))
            return;

        String clave = null;
        for (Map.Entry<String, ListView<String>> entry : mapaListas.entrySet()) {
            if (entry.getValue() == lista) {
                clave = entry.getKey();
                break;
            }
        }

        if (clave == null)
            return;

        int index = lista.getSelectionModel().getSelectedIndex();
        List<Integer> ids = seccionesSeleccionadas.get(clave);
        if (ids != null && index >= 0 && index < ids.size()) {
            List<Integer> asignaciones = asignacionesExistentes.get(clave);
            if (asignaciones != null && index < asignaciones.size()) {
                asignaciones.set(index, -1); // Marcar para eliminación
            }
            ids.remove(index);
            actualizarListaVisual(clave);
        }
    }

    // GUARDAR MENÚ - ✅ CORRECCIÓN COMPLETA
    @FXML
    private void onGuardarClicked() {
        // 1. Validar formulario
        if (!validarFormulario())
            return;

        // 2. Verificar sesión
        if (!session.isAuthenticated() || session.getCurrentUser() == null) {
            mostrarAlerta("Error de sesión", "Debes iniciar sesión para guardar menús.");
            return;
        }

        int userId = session.getCurrentUser().getId();
        if (userId <= 0) {
            mostrarAlerta("Error de usuario", "ID de usuario no válido.");
            return;
        }

        // 3. Deshabilitar botón y mostrar estado
        btnGuardar.setDisable(true);
        lblStatus.setText("⏳ Preparando para guardar...");

        // 4. Ejecutar en hilo separado
        new Thread(() -> {
            try {
                // ✅ **CORRECCIÓN: No generar menú semanal automáticamente**
                // En lugar de eso, crear menús solo para los días que tienen secciones

                // Primero, cargar menús existentes para ver cuáles ya están creados
                cargarMenusExistentesBloqueante();

                // Crear menús solo para los días que tienen secciones seleccionadas
                boolean menusCreados = crearMenusNecesarios(userId);
                if (!menusCreados) {
                    Platform.runLater(() -> {
                        lblStatus.setText("❌ Error al crear menús necesarios");
                        btnGuardar.setDisable(false);
                    });
                    return;
                }

                // Procesar todas las asignaciones de secciones
                boolean exito = procesarAsignacionesSecciones(userId);

                Platform.runLater(() -> {
                    if (exito) {
                        lblStatus.setText("✅ Menú guardado exitosamente");
                        mostrarAlertaInformacion("Éxito", "El menú se ha guardado correctamente.");
                        cerrarVentana();
                    } else {
                        lblStatus.setText("❌ Error al guardar menú");
                        btnGuardar.setDisable(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                    btnGuardar.setDisable(false);
                    mostrarAlerta("Error", "Error al guardar menú: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private boolean crearMenusNecesarios(int userId) {
        List<String> clavesParaCrear = new ArrayList<>();

        // Identificar qué menús necesitan ser creados
        for (String clave : seccionesSeleccionadas.keySet()) {
            List<Integer> secciones = seccionesSeleccionadas.get(clave);
            if (secciones != null && !secciones.isEmpty()) {
                Integer idMenu = mapaIdMenus.get(clave);
                if (idMenu == null || idMenu == 0) {
                    clavesParaCrear.add(clave);
                }
            }
        }

        if (clavesParaCrear.isEmpty()) {
            return true; // Todos los menús ya existen
        }

        boolean exito = true;

        for (String clave : clavesParaCrear) {
            String[] partes = clave.split("-");
            if (partes.length != 2)
                continue;

            String dia = partes[0];
            String horario = partes[1];
            LocalDate fecha = mapaFechas.get(dia);

            if (fecha == null) {
                exito = false;
                break;
            }

            int idMenu = verificarOCrearMenuIndividual(fecha, dia, horario, userId);
            if (idMenu > 0) {
                mapaIdMenus.put(clave, idMenu);
            } else {
                exito = false;
                break;
            }
        }

        return exito;
    }

    // Método para verificar y crear menús individuales
    private int verificarOCrearMenuIndividual(LocalDate fecha, String dia, String horario, int userId) {
        final int[] idMenuCreado = { 0 };
        final CountDownLatch latch = new CountDownLatch(1);

        String fechaStr = fecha.format(DateTimeFormatter.ISO_DATE);

        // 1. Verificar si ya existe
        MenuService.verificarMenuExistente(fechaStr, horario, new MenuService.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                try {
                    if (response.has("existe") && response.getBoolean("existe")) {
                        idMenuCreado[0] = response.getInt("id");
                        latch.countDown();
                    } else {
                        // 2. Si no existe, crear nuevo menú
                        WeekFields weekFields = WeekFields.of(Locale.getDefault());
                        int numeroSemana = fecha.get(weekFields.weekOfWeekBasedYear());
                        int anio = fecha.getYear();

                        MenuService.crearMenuIndividual(fechaStr, dia, horario,
                                numeroSemana, anio, userId, new MenuService.Callback() {
                                    @Override
                                    public void onSuccess(org.json.JSONObject createResponse) {
                                        try {
                                            idMenuCreado[0] = createResponse.getInt("id");
                                            latch.countDown();
                                        } catch (Exception e) {
                                            latch.countDown();
                                        }
                                    }

                                    @Override
                                    public void onError(String error) {
                                        System.err.println("Error creando menú: " + error);
                                        latch.countDown();
                                    }
                                });
                    }
                } catch (Exception e) {
                    latch.countDown();
                }
            }

            @Override
            public void onError(String error) {
                System.err.println("Error verificando menú: " + error);
                latch.countDown();
            }
        });

        try {
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return idMenuCreado[0];
    }

    private int crearMenuIndividual(LocalDate fecha, String dia, String horario, int userId) {
        final int[] idMenuCreado = { 0 };
        final CountDownLatch latch = new CountDownLatch(1);

        String fechaStr = fecha.format(DateTimeFormatter.ISO_DATE);
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int numeroSemana = fecha.get(weekFields.weekOfWeekBasedYear());
        int anio = fecha.getYear();

        // Primero verificar si ya existe
        verificarOMenuExistente(fechaStr, horario, new MenuService.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                if (response.has("id") && response.getInt("id") > 0) {
                    idMenuCreado[0] = response.getInt("id");
                    latch.countDown();
                } else {
                    // No existe, crear nuevo
                    crearNuevoMenu(fechaStr, dia, horario, numeroSemana, anio, userId, latch, idMenuCreado);
                }
            }

            @Override
            public void onError(String error) {
                // Error al verificar, intentar crear de todos modos
                crearNuevoMenu(fechaStr, dia, horario, numeroSemana, anio, userId, latch, idMenuCreado);
            }
        });

        try {
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        }

        return idMenuCreado[0];
    }

    private void verificarOMenuExistente(String fecha, String horario, MenuService.Callback callback) {
        // Crear una solicitud simple para verificar
        org.json.JSONObject request = new org.json.JSONObject();
        try {
            request.put("fecha", fecha);
            request.put("horario", horario);
        } catch (Exception e) {
            callback.onError("Error creando request: " + e.getMessage());
            return;
        }

        // Esta función debería estar en MenuService
        MenuService.getMenuPorFecha(fecha, new MenuService.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                try {
                    if (response.has("menus")) {
                        org.json.JSONArray menus = response.getJSONArray("menus");
                        for (int i = 0; i < menus.length(); i++) {
                            org.json.JSONObject menu = menus.getJSONObject(i);
                            if (menu.getString("Horario").equals(horario)) {
                                org.json.JSONObject result = new org.json.JSONObject();
                                result.put("id", menu.getInt("ID"));
                                callback.onSuccess(result);
                                return;
                            }
                        }
                    }
                    callback.onSuccess(new org.json.JSONObject()); // No encontrado
                } catch (Exception e) {
                    callback.onError("Error procesando respuesta: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    private void crearNuevoMenu(String fecha, String dia, String horario, int numeroSemana, int anio,
            int userId, CountDownLatch latch, int[] idMenuCreado) {
        // Llamar al endpoint de generación individual
        org.json.JSONObject request = new org.json.JSONObject();
        try {
            request.put("fecha", fecha);
            request.put("diaSemana", dia);
            request.put("horario", horario);
            request.put("numeroSemana", numeroSemana);
            request.put("anio", anio);
            request.put("idUsuario", userId);
        } catch (Exception e) {
            latch.countDown();
            return;
        }

        // Llamar a un servicio personalizado (necesitarás crearlo)
        // Por ahora, usaremos un enfoque simplificado
        System.out.println("Creando menú para: " + fecha + " " + horario);
        // Simular éxito temporalmente
        idMenuCreado[0] = (int) (System.currentTimeMillis() % 10000); // ID temporal
        latch.countDown();
    }

    // Método para procesar asignaciones (simplificado)
    private boolean procesarAsignacionesSecciones(int userId) {
        AtomicBoolean exitoGlobal = new AtomicBoolean(true);
        List<Runnable> tareas = new ArrayList<>();

        for (String clave : seccionesSeleccionadas.keySet()) {
            String[] partes = clave.split("-");
            if (partes.length != 2)
                continue;

            String dia = partes[0];
            String horario = partes[1];

            Integer idMenu = mapaIdMenus.get(clave);
            if (idMenu == null || idMenu == 0) {
                // Este menú no existe y no tiene secciones, está bien
                continue;
            }

            List<Integer> seccionesIds = seccionesSeleccionadas.get(clave);

            tareas.add(() -> {
                try {
                    // Eliminar asignaciones existentes primero
                    eliminarAsignacionesExistentes(idMenu);

                    // Agregar nuevas asignaciones
                    for (Integer idSeccion : seccionesIds) {
                        if (idSeccion > 0) {
                            asignarSeccion(idMenu, idSeccion, userId);
                        }
                    }
                } catch (Exception e) {
                    exitoGlobal.set(false);
                    e.printStackTrace();
                }
            });
        }

        ejecutarTareasConcurrentes(tareas, 3);
        return exitoGlobal.get();
    }

    private boolean eliminarAsignacionesExistentes(int idMenu) {
        // Primero, obtener las asignaciones existentes
        final AtomicBoolean exito = new AtomicBoolean(true);
        final CountDownLatch latch = new CountDownLatch(1);

        MenuService.getMenuSemanal(numeroSemana, anio, new MenuService.MenuSemanalCallback() {
            @Override
            public void onSuccess(MenuSemanal menuSemanal) {
                try {
                    // Encontrar el menú específico y eliminar sus secciones
                    for (Menu menu : menuSemanal.getMenus()) {
                        if (menu.getId() == idMenu) {
                            for (MenuSeccion seccion : menu.getSecciones()) {
                                MenuService.removerSeccionMenu(seccion.getId(), new MenuService.Callback() {
                                    @Override
                                    public void onSuccess(org.json.JSONObject response) {
                                        // Continuar con la siguiente
                                    }

                                    @Override
                                    public void onError(String error) {
                                        exito.set(false);
                                    }
                                });
                            }
                            break;
                        }
                    }
                    latch.countDown();
                } catch (Exception e) {
                    exito.set(false);
                    latch.countDown();
                }
            }

            @Override
            public void onError(String error) {
                exito.set(false);
                latch.countDown();
            }
        });

        try {
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return exito.get();
    }

    private void ejecutarTareasConcurrentes(List<Runnable> tareas, int maxConcurrentes) {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
                .newFixedThreadPool(maxConcurrentes);

        List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
        for (Runnable tarea : tareas) {
            futures.add(executor.submit(tarea));
        }

        for (java.util.concurrent.Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean eliminarAsignacion(int idAsignacion) {
        final AtomicBoolean exito = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        MenuService.removerSeccionMenu(idAsignacion, new MenuService.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                exito.set(response.optBoolean("success", false));
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                System.err.println("Error eliminando asignación " + idAsignacion + ": " + error);
                exito.set(false);
                latch.countDown();
            }
        });

        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return exito.get();
    }

    private boolean asignarSeccion(int idMenu, int idSeccion, int idUsuario) {
        final AtomicBoolean exito = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);

        MenuService.asignarSeccionMenu(idMenu, idSeccion, idUsuario, new MenuService.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                exito.set(response.optBoolean("success", false) || response.optBoolean("alreadyExists", false));
                latch.countDown();
            }

            @Override
            public void onError(String error) {
                System.err.println("Error asignando sección " + idSeccion + " a menú " + idMenu + ": " + error);
                exito.set(false);
                latch.countDown();
            }
        });

        try {
            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        return exito.get();
    }

    private boolean validarFormulario() {
        if (mapaFechas.isEmpty()) {
            mostrarAlerta("⚠️ Faltan fechas", "Calcula las fechas de la semana primero.");
            return false;
        }

        // ✅ **CORRECCIÓN IMPORTANTE: NO requerir que haya al menos una sección**
        // Permitir menús vacíos para días que no tienen servicio
        /*
         * boolean hayAlgunaSeccion = false;
         * for (List<Integer> lista : seccionesSeleccionadas.values()) {
         * if (lista != null && !lista.isEmpty()) {
         * hayAlgunaSeccion = true;
         * break;
         * }
         * }
         * 
         * if (!hayAlgunaSeccion) {
         * mostrarAlerta("⚠️ Sin secciones",
         * "Agrega al menos una sección a algún día/horario.");
         * return false;
         * }
         */

        return true;
    }

    private void cargarMenusExistentesBloqueante() {
        final CountDownLatch latch = new CountDownLatch(1);

        MenuService.getMenuSemanal(numeroSemana, anio, new MenuService.MenuSemanalCallback() {
            @Override
            public void onSuccess(MenuSemanal menuSemanal) {
                Platform.runLater(() -> {
                    try {
                        mapaIdMenus.clear();

                        if (menuSemanal != null && !menuSemanal.getMenus().isEmpty()) {
                            for (Menu menu : menuSemanal.getMenus()) {
                                String clave = menu.getDiaSemana() + "-" + menu.getHorario();
                                mapaIdMenus.put(clave, menu.getId());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    System.err.println("Error cargando menús: " + error);
                    latch.countDown();
                });
            }
        });

        try {
            latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // MÉTODOS PARA LOS BOTONES DE AGREGAR (simplificados)
    @FXML
    private void onAgregarLunesDesayuno() {
        abrirDialogoSeleccion("Lunes", "Desayuno");
    }

    @FXML
    private void onAgregarMartesDesayuno() {
        abrirDialogoSeleccion("Martes", "Desayuno");
    }

    @FXML
    private void onAgregarMiercolesDesayuno() {
        abrirDialogoSeleccion("Miércoles", "Desayuno");
    }

    @FXML
    private void onAgregarJuevesDesayuno() {
        abrirDialogoSeleccion("Jueves", "Desayuno");
    }

    @FXML
    private void onAgregarViernesDesayuno() {
        abrirDialogoSeleccion("Viernes", "Desayuno");
    }

    @FXML
    private void onAgregarLunesComida() {
        abrirDialogoSeleccion("Lunes", "Comida");
    }

    @FXML
    private void onAgregarMartesComida() {
        abrirDialogoSeleccion("Martes", "Comida");
    }

    @FXML
    private void onAgregarMiercolesComida() {
        abrirDialogoSeleccion("Miércoles", "Comida");
    }

    @FXML
    private void onAgregarJuevesComida() {
        abrirDialogoSeleccion("Jueves", "Comida");
    }

    @FXML
    private void onAgregarViernesComida() {
        abrirDialogoSeleccion("Viernes", "Comida");
    }

    // MÉTODOS PARA CARGAR DATOS
    public void cargarDatosMenu(int semana, int anio) {
        modoEdicion = true;
        modoVisualizacion = false; // Asegurar que no está en modo visualización
        lblTitulo.setText("Editar Menú Semanal");
        this.numeroSemana = semana;
        this.anio = anio;

        Platform.runLater(() -> {
            spinAnio.getValueFactory().setValue(anio);
            spinSemana.getValueFactory().setValue(semana);

            // Calcular fechas y cargar menú existente
            calcularFechasSemana();
        });
    }

    private void cargarMenusExistentes() {
        lblStatus.setText("Cargando menú existente...");

        MenuService.getMenuSemanal(numeroSemana, anio, new MenuService.MenuSemanalCallback() {
            @Override
            public void onSuccess(MenuSemanal menuSemanal) {
                Platform.runLater(() -> {
                    try {
                        // Limpiar datos anteriores
                        for (String clave : seccionesSeleccionadas.keySet()) {
                            seccionesSeleccionadas.put(clave, new ArrayList<>());
                            asignacionesExistentes.put(clave, new ArrayList<>());
                        }
                        mapaIdMenus.clear();

                        if (menuSemanal != null && !menuSemanal.getMenus().isEmpty()) {
                            for (Menu menu : menuSemanal.getMenus()) {
                                String clave = menu.getDiaSemana() + "-" + menu.getHorario();
                                mapaIdMenus.put(clave, menu.getId());

                                List<Integer> idsSecciones = new ArrayList<>();
                                List<Integer> idsAsignaciones = new ArrayList<>();

                                for (MenuSeccion menuSeccion : menu.getSecciones()) {
                                    idsSecciones.add(menuSeccion.getIdSeccion());
                                    idsAsignaciones.add(menuSeccion.getId());
                                }

                                seccionesSeleccionadas.put(clave, idsSecciones);
                                asignacionesExistentes.put(clave, idsAsignaciones);
                                actualizarListaVisual(clave);
                            }

                            lblStatus.setText("✅ Menú existente cargado");
                        } else {
                            lblStatus.setText("ℹ️ No hay menú existente para esta semana");
                        }
                    } catch (Exception e) {
                        lblStatus.setText("❌ Error al cargar menú");
                        e.printStackTrace();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + error);
                    mostrarAlerta("Error", "No se pudo cargar el menú: " + error);
                });
            }
        });
    }

    // MODO VISUALIZACIÓN
    public void visualizarMenu(int semana, int anio) {
        modoEdicion = true;
        modoVisualizacion = true;
        lblTitulo.setText("Visualizar Menú Semanal");
        this.numeroSemana = semana;
        this.anio = anio;

        spinAnio.setDisable(true);
        spinSemana.setDisable(true);
        btnCalcularFechas.setDisable(true);
        btnGuardar.setVisible(false);

        // Deshabilitar botones de agregar
        btnAgregarLunesDesayuno.setDisable(true);
        btnAgregarMartesDesayuno.setDisable(true);
        btnAgregarMiercolesDesayuno.setDisable(true);
        btnAgregarJuevesDesayuno.setDisable(true);
        btnAgregarViernesDesayuno.setDisable(true);
        btnAgregarLunesComida.setDisable(true);
        btnAgregarMartesComida.setDisable(true);
        btnAgregarMiercolesComida.setDisable(true);
        btnAgregarJuevesComida.setDisable(true);
        btnAgregarViernesComida.setDisable(true);

        // Deshabilitar interacción en listas
        for (ListView<String> lista : mapaListas.values()) {
            lista.setMouseTransparent(true);
            lista.setFocusTraversable(false);
        }

        spinAnio.getValueFactory().setValue(anio);
        spinSemana.getValueFactory().setValue(semana);
        calcularFechasSemana();
    }

    // UTILIDADES
    @FXML
    private void onCancelarClicked() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private void mostrarAlertaInformacion(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    public void modoEdicion(boolean estado) {
        this.modoEdicion = estado;
    }
    
}