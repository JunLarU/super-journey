package app.controllers.menus;

import core.data.Menus.AllMenus;
import core.data.Menus.Menu;
import core.data.Menus.MenuSeccion;
import core.data.Menus.SeccionMenu;
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
import java.util.stream.Collectors;

/**
 * Controlador para el formulario de registro/edición de menús semanales
 * Ahora usando archivos JSON en lugar de base de datos
 */
public class RegistroMenuController {

    // =========================
    // 🔹 COMPONENTES FXML
    // =========================
    @FXML private Label lblTitulo;
    @FXML private Spinner<Integer> spinAnio;
    @FXML private Spinner<Integer> spinSemana;
    @FXML private Button btnCalcularFechas;
    @FXML private Label lblRangoFechas;

    // Labels de fechas por día
    @FXML private Label lblFechaLunesD, lblFechaMartesD, lblFechaMiercolesD, lblFechaJuevesD, lblFechaViernesD;
    @FXML private Label lblFechaLunesC, lblFechaMartesC, lblFechaMiercolesC, lblFechaJuevesC, lblFechaViernesC;

    // ListViews para cada día/horario
    @FXML private ListView<String> listLunesDesayuno, listMartesDesayuno, listMiercolesDesayuno;
    @FXML private ListView<String> listJuevesDesayuno, listViernesDesayuno;
    @FXML private ListView<String> listLunesComida, listMartesComida, listMiercolesComida;
    @FXML private ListView<String> listJuevesComida, listViernesComida;

    @FXML private Button btnGuardar, btnCancelar;
    @FXML private Label lblStatus;

    // =========================
    // 🔹 MODELOS Y DATOS
    // =========================
    private final AllMenus allMenus = AllMenus.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private boolean modoEdicion = false;
    private boolean modoVisualizacion = false;
    private int numeroSemana = 0;
    private int anio = 0;

    // Mapa de fechas calculadas
    private Map<String, LocalDate> mapaFechas = new HashMap<>();
    
    // Mapa de listas para acceso fácil
    private Map<String, ListView<String>> mapaListas = new HashMap<>();
    
    // Almacenar secciones seleccionadas: clave = "Lunes-Desayuno", valor = lista de IDs de secciones
    private Map<String, List<Integer>> seccionesSeleccionadas = new HashMap<>();
    
    // Catálogo de secciones disponibles
    private List<SeccionMenu> seccionesDisponibles = new ArrayList<>();

    // =========================
    // 🔹 INICIALIZACIÓN
    // =========================
    @FXML
    public void initialize() {
        configurarSpinners();
        configurarMapaListas();
        cargarSeccionesDisponibles();
        
        // Inicializar estructuras de datos
        inicializarEstructuras();
        
        lblStatus.setText("Editar Configura el menú para cada día y horario");
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
        // Mapear todas las listas para acceso fácil
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

        // Configurar doble click para eliminar (solo si no es modo visualización)
        for (ListView<String> lista : mapaListas.values()) {
            lista.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !modoVisualizacion) {
                    eliminarSeccionSeleccionada(lista);
                }
            });
        }
    }

    private void inicializarEstructuras() {
        String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        String[] horarios = {"Desayuno", "Comida"};
        
        for (String dia : dias) {
            for (String horario : horarios) {
                String clave = dia + "-" + horario;
                seccionesSeleccionadas.put(clave, new ArrayList<>());
            }
        }
    }

    private void cargarSeccionesDisponibles() {
        new Thread(() -> {
            try {
                List<SeccionMenu> secciones = allMenus.getAllSecciones();
                
                Platform.runLater(() -> {
                    seccionesDisponibles.clear();
                    seccionesDisponibles.addAll(secciones);
                    lblStatus.setText("✅ " + seccionesDisponibles.size() + " secciones cargadas");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar secciones: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    // =========================
    // 🔹 CALCULAR FECHAS
    // =========================
    @FXML
    private void onCalcularFechasClicked() {
        calcularFechasSemana();
    }

    private void calcularFechasSemana() {
        numeroSemana = spinSemana.getValue();
        anio = spinAnio.getValue();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        
        // Obtener el primer día de la semana
        LocalDate primerDiaAnio = LocalDate.of(anio, 1, 1);
        LocalDate primerLunes = primerDiaAnio.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        
        // Si el primer lunes está en el año anterior, ajustar
        if (primerLunes.getYear() < anio) {
            primerLunes = primerLunes.plusWeeks(1);
        }
        
        // Calcular el lunes de la semana deseada
        LocalDate lunesObjetivo = primerLunes.plusWeeks(numeroSemana - 1);
        
        // Guardar fechas en el mapa
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
        
        lblStatus.setText("✅ Fechas calculadas correctamente");
        
        // Si estamos en modo edición, cargar los menús existentes para esta semana
        if (modoEdicion) {
            cargarMenusExistentes();
        }
    }

    // =========================
    // 🔹 MÉTODOS PARA AGREGAR SECCIONES
    // =========================
    
    @FXML private void onAgregarLunesDesayuno() { abrirDialogoSeleccion("Lunes", "Desayuno"); }
    @FXML private void onAgregarMartesDesayuno() { abrirDialogoSeleccion("Martes", "Desayuno"); }
    @FXML private void onAgregarMiercolesDesayuno() { abrirDialogoSeleccion("Miércoles", "Desayuno"); }
    @FXML private void onAgregarJuevesDesayuno() { abrirDialogoSeleccion("Jueves", "Desayuno"); }
    @FXML private void onAgregarViernesDesayuno() { abrirDialogoSeleccion("Viernes", "Desayuno"); }
    
    @FXML private void onAgregarLunesComida() { abrirDialogoSeleccion("Lunes", "Comida"); }
    @FXML private void onAgregarMartesComida() { abrirDialogoSeleccion("Martes", "Comida"); }
    @FXML private void onAgregarMiercolesComida() { abrirDialogoSeleccion("Miércoles", "Comida"); }
    @FXML private void onAgregarJuevesComida() { abrirDialogoSeleccion("Jueves", "Comida"); }
    @FXML private void onAgregarViernesComida() { abrirDialogoSeleccion("Viernes", "Comida"); }

    private void abrirDialogoSeleccion(String dia, String horario) {
        if (seccionesDisponibles.isEmpty()) {
            mostrarAlerta("⚠️ Sin secciones", "No hay secciones disponibles. Crea secciones primero.");
            return;
        }

        if (mapaFechas.isEmpty()) {
            mostrarAlerta("⚠️ Faltan fechas", "Calcula las fechas de la semana primero.");
            return;
        }

        String clave = dia + "-" + horario;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Seleccionar Secciones - " + dia + " " + horario);
        dialog.setHeaderText("Selecciona las secciones del menú");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(400);

        Label lblInfo = new Label("Selecciona una o más secciones:");
        
        // ListView con checkboxes
        ListView<CheckBox> listaSecciones = new ListView<>();
        ObservableList<CheckBox> items = FXCollections.observableArrayList();
        
        List<Integer> yaSeleccionados = seccionesSeleccionadas.get(clave);
        
        for (SeccionMenu seccion : seccionesDisponibles) {
            int id = seccion.getId();
            String nombre = seccion.getNombre();
            String color = seccion.getColor();
            
            CheckBox cb = new CheckBox(nombre);
            cb.setUserData(id);
            cb.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
            
            // Marcar si ya está seleccionado
            if (yaSeleccionados.contains(id)) {
                cb.setSelected(true);
            }
            
            items.add(cb);
        }
        
        listaSecciones.setItems(items);
        listaSecciones.setPrefHeight(300);

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
        if (lista == null) return;
        
        lista.getItems().clear();
        
        List<Integer> idsSeleccionados = seccionesSeleccionadas.get(clave);
        if (idsSeleccionados == null || idsSeleccionados.isEmpty()) {
            lista.getItems().add("(Sin secciones asignadas)");
            return;
        }
        
        for (Integer id : idsSeleccionados) {
            // Buscar el nombre y color de la sección
            for (SeccionMenu seccion : seccionesDisponibles) {
                if (seccion.getId() == id) {
                    String nombre = seccion.getNombre();
                    String color = seccion.getColor();
                    lista.getItems().add("📦 " + nombre);
                    break;
                }
            }
        }
    }

    private void eliminarSeccionSeleccionada(ListView<String> lista) {
        String seleccionado = lista.getSelectionModel().getSelectedItem();
        if (seleccionado == null || seleccionado.equals("(Sin secciones asignadas)")) return;

        // Buscar la clave correspondiente
        String clave = null;
        for (Map.Entry<String, ListView<String>> entry : mapaListas.entrySet()) {
            if (entry.getValue() == lista) {
                clave = entry.getKey();
                break;
            }
        }
        
        if (clave == null) return;

        int index = lista.getSelectionModel().getSelectedIndex();
        List<Integer> ids = seccionesSeleccionadas.get(clave);
        if (ids != null && index >= 0 && index < ids.size()) {
            ids.remove(index);
            actualizarListaVisual(clave);
        }
    }

    // =========================
    // 🔹 GUARDAR MENÚ
    // =========================
    @FXML
    private void onGuardarClicked() {
        if (!validarFormulario()) return;

        btnGuardar.setDisable(true);
        lblStatus.setText("Guardando menú...");

        new Thread(() -> {
            try {
                if (modoEdicion) {
                    // Eliminar menús existentes de esta semana
                    List<Menu> menusExistentes = allMenus.getMenusBySemana(numeroSemana, anio);
                    for (Menu menu : menusExistentes) {
                        allMenus.removeMenu(menu.getId());
                    }
                }

                // Crear nuevos menús
                crearMenusSemana();
                
                Platform.runLater(() -> {
                    lblStatus.setText("✅ Menú guardado correctamente");
                    Alert alert = new Alert(Alert.AlertType.INFORMATION,
                            "Menú guardado correctamente",
                            ButtonType.OK);
                    alert.showAndWait();
                    cerrarVentana();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                    btnGuardar.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    private boolean validarFormulario() {
        if (mapaFechas.isEmpty()) {
            mostrarAlerta("⚠️ Faltan datos", "Calcula las fechas de la semana primero");
            return false;
        }

        // Verificar que al menos una celda tenga secciones
        boolean hayAlgunaSeccion = false;
        for (List<Integer> lista : seccionesSeleccionadas.values()) {
            if (!lista.isEmpty() && !lista.get(0).equals(-1)) {
                hayAlgunaSeccion = true;
                break;
            }
        }

        if (!hayAlgunaSeccion) {
            mostrarAlerta("⚠️ Sin secciones", "Agrega al menos una sección a algún día/horario");
            return false;
        }

        return true;
    }

    private void crearMenusSemana() {
        String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes"};
        String[] horarios = {"Desayuno", "Comida"};
        String fechaCreacion = LocalDate.now().toString();
        int idUsuario = 1; // Por defecto, podrías obtenerlo de la sesión

        for (String dia : dias) {
            for (String horario : horarios) {
                String clave = dia + "-" + horario;
                LocalDate fecha = mapaFechas.get(dia);
                List<Integer> seccionesIds = seccionesSeleccionadas.get(clave);

                if (fecha != null) {
                    // Crear el menú
                    Menu menu = new Menu(
                        0, // ID se asignará automáticamente
                        fecha,
                        dia,
                        horario,
                        numeroSemana,
                        anio,
                        fechaCreacion,
                        true,
                        idUsuario,
                        0,
                        null
                    );

                    // Agregar secciones al menú
                    if (seccionesIds != null && !seccionesIds.isEmpty()) {
                        int orden = 1;
                        for (Integer idSeccion : seccionesIds) {
                            // Buscar la sección para obtener su nombre
                            SeccionMenu seccion = allMenus.getSeccionById(idSeccion);
                            if (seccion != null) {
                                MenuSeccion menuSeccion = new MenuSeccion(
                                    0, // ID se asignará automáticamente
                                    menu.getId(), // Se actualizará después
                                    idSeccion,
                                    seccion.getNombre(),
                                    orden,
                                    idUsuario,
                                    fechaCreacion
                                );
                                menu.agregarSeccion(menuSeccion);
                                orden++;
                            }
                        }
                    }

                    // Guardar el menú
                    allMenus.addMenu(menu);
                }
            }
        }
    }

    // =========================
    // 🔹 CARGAR DATOS EXISTENTES
    // =========================
    public void cargarDatosMenu(int semana, int anio) {
        modoEdicion = true;
        lblTitulo.setText("Editar Editar Menú Semanal");

        this.numeroSemana = semana;
        this.anio = anio;

        spinAnio.getValueFactory().setValue(anio);
        spinSemana.getValueFactory().setValue(semana);

        // Calcular fechas
        calcularFechasSemana();
        
        lblStatus.setText("📝 Editando menú de la semana " + numeroSemana + "/" + anio);
    }

    private void cargarMenusExistentes() {
        new Thread(() -> {
            try {
                List<Menu> menus = allMenus.getMenusBySemana(numeroSemana, anio);
                
                Platform.runLater(() -> {
                    // Limpiar selecciones anteriores
                    for (String clave : seccionesSeleccionadas.keySet()) {
                        seccionesSeleccionadas.put(clave, new ArrayList<>());
                    }
                    
                    // Cargar secciones de cada menú existente
                    for (Menu menu : menus) {
                        String dia = menu.getDiaSemana();
                        String horario = menu.getHorario();
                        String clave = dia + "-" + horario;
                        
                        List<MenuSeccion> seccionesMenu = menu.getSecciones();
                        List<Integer> idsSecciones = new ArrayList<>();
                        
                        for (MenuSeccion menuSeccion : seccionesMenu) {
                            idsSecciones.add(menuSeccion.getIdSeccion());
                        }
                        
                        seccionesSeleccionadas.put(clave, idsSecciones);
                        actualizarListaVisual(clave);
                    }
                    
                    lblStatus.setText("✅ Menú existente cargado para edición");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar menú existente: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    // =========================
    // 🔹 MODO VISUALIZACIÓN
    // =========================
    public void visualizarMenu(int semana, int anio) {
        cargarDatosMenu(semana, anio);
        
        modoVisualizacion = true;
        lblTitulo.setText("visualizar Menú Semanal");

        // Deshabilitar controles de edición
        spinAnio.setDisable(true);
        spinSemana.setDisable(true);
        btnCalcularFechas.setDisable(true);
        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        // Deshabilitar todos los botones de agregar y listas
        for (ListView<String> lista : mapaListas.values()) {
            lista.setMouseTransparent(true);
            lista.setFocusTraversable(false);
            lista.setStyle(lista.getStyle() + "; -fx-opacity: 0.8;");
        }

        lblStatus.setText("visualizando menú de la semana " + numeroSemana + "/" + anio);
    }

    // =========================
    // 🔹 UTILIDADES
    // =========================
    @FXML
    private void onCancelarClicked() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}