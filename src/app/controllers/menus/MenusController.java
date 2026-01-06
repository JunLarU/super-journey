package app.controllers.menus;

import core.data.Menus.MenuSemanal;
import core.data.Menus.Menu;
import core.data.Menus.MenuSeccion;
import core.data.Menus.SeccionMenu;
import core.data.Menus.SeccionProducto;
import core.data.Productos.Producto;
import core.services.MenuService;
import core.services.ProductoService;
import core.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import org.json.JSONObject;

/**
 * Controlador para gestión de menús semanales y secciones con productos
 * Funciona con servidor y base de datos MySQL
 */
public class MenusController {

    // 📌 COMPONENTES MENÚS
    @FXML
    private TabPane tabPane;
    @FXML
    private Spinner<Integer> spinAnio;
    @FXML
    private Spinner<Integer> spinSemana;
    @FXML
    private Button btnCargarSemana;
    @FXML
    private Label lblRangoFechas;
    @FXML
    private Label lblStatus;
    @FXML
    private GridPane gridCalendario;

    // VBoxes para cada día/horario
    @FXML
    private VBox vboxLunesDesayuno, vboxMartesDesayuno, vboxMiercolesDesayuno;
    @FXML
    private VBox vboxJuevesDesayuno, vboxViernesDesayuno;
    @FXML
    private VBox vboxLunesComida, vboxMartesComida, vboxMiercolesComida;
    @FXML
    private VBox vboxJuevesComida, vboxViernesComida;

    @FXML
    private Button btnNuevoMenu, btnEditarMenu, btnEliminarMenu, btnGenerarMenu, btnVisualizarMenu;

    // 📌 COMPONENTES SECCIONES
    @FXML
    private TableView<SeccionMenu> tablaSecciones;
    @FXML
    private TableColumn<SeccionMenu, String> colSecID, colSecNombre, colSecDescripcion;
    @FXML
    private TableColumn<SeccionMenu, String> colSecColor, colSecProductos;
    @FXML
    private TableColumn<SeccionMenu, Void> colSecAcciones;
    @FXML
    private Button btnNuevaSeccion, btnActualizarSecciones;

    // 📌 MODELOS Y DATOS
    private final SessionManager session = SessionManager.getInstance();
    private final ObservableList<SeccionMenu> seccionesData = FXCollections.observableArrayList();
    private final ObservableList<Producto> productosData = FXCollections.observableArrayList();

    private Map<String, VBox> mapaCeldas = new HashMap<>();
    private MenuSemanal menuSemanalActual = null;
    private int semanaActual = 0;
    private int anioActual = 0;
    private LocalDate fechaInicioSemanaActual = null;

    // 📌 INICIALIZACIÓN
    @FXML
    public void initialize() {
        configurarSpinners();
        configurarMapa();
        configurarTablaSecciones();
        cargarDatosIniciales();
        lblStatus.setText("✅ Sistema listo. Conectado a base de datos.");
    }

    private void configurarSpinners() {
        LocalDate hoy = LocalDate.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        int anioHoy = hoy.getYear();
        int semanaHoy = hoy.get(weekFields.weekOfWeekBasedYear());

        SpinnerValueFactory<Integer> factoryAnio = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                anioHoy - 2, anioHoy + 2, anioHoy);
        spinAnio.setValueFactory(factoryAnio);

        SpinnerValueFactory<Integer> factorySemana = new SpinnerValueFactory.IntegerSpinnerValueFactory(
                1, 52, semanaHoy);
        spinSemana.setValueFactory(factorySemana);
    }

    private void configurarMapa() {
        mapaCeldas.put("Lunes-Desayuno", vboxLunesDesayuno);
        mapaCeldas.put("Martes-Desayuno", vboxMartesDesayuno);
        mapaCeldas.put("Miércoles-Desayuno", vboxMiercolesDesayuno);
        mapaCeldas.put("Jueves-Desayuno", vboxJuevesDesayuno);
        mapaCeldas.put("Viernes-Desayuno", vboxViernesDesayuno);

        mapaCeldas.put("Lunes-Comida", vboxLunesComida);
        mapaCeldas.put("Martes-Comida", vboxMartesComida);
        mapaCeldas.put("Miércoles-Comida", vboxMiercolesComida);
        mapaCeldas.put("Jueves-Comida", vboxJuevesComida);
        mapaCeldas.put("Viernes-Comida", vboxViernesComida);
    }

    private void cargarDatosIniciales() {
        cargarProductosDisponibles();
        cargarSecciones();
    }

    // In the cargarProductosDisponibles() method, fix the Producto creation:
    private void cargarProductosDisponibles() {
        ProductoService.listProductos(new ProductoService.ListCallback() {
            @Override
            public void onSuccess(List<org.json.JSONObject> list) {
                productosData.clear();
                for (org.json.JSONObject json : list) {
                    Producto producto = Producto.fromJSON(json); // Fixed: Use static method
                    if (producto.isDisponible()) {
                        productosData.add(producto);
                    }
                }
                Platform.runLater(() -> {
                    lblStatus.setText("✅ " + productosData.size() + " productos disponibles cargados");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error cargando productos: " + error);
                });
            }
        });
    }

    // En el método onCargarSemanaClicked, verifica si los botones existen antes de
    // usarlos:
    private void habilitarDeshabilitarBotones(boolean tieneMenu) {
        Platform.runLater(() -> {
            if (btnEditarMenu != null) {
                btnEditarMenu.setDisable(!tieneMenu);
            }
            if (btnEliminarMenu != null) {
                btnEliminarMenu.setDisable(!tieneMenu);
            }
            if (btnVisualizarMenu != null) {
                btnVisualizarMenu.setDisable(!tieneMenu);
            }
        });
    }

    // 📅 GESTIÓN DE MENÚS SEMANALES
    @FXML
    private void onCargarSemanaClicked() {
        semanaActual = spinSemana.getValue();
        anioActual = spinAnio.getValue();

        lblStatus.setText("Cargando menú de la semana " + semanaActual + "/" + anioActual + "...");
        limpiarCalendario();

        // Calcular fechas primero
        calcularFechasSemana(semanaActual, anioActual);

        MenuService.getMenuSemanal(semanaActual, anioActual, new MenuService.MenuSemanalCallback() {
            @Override
            public void onSuccess(MenuSemanal menuSemanal) {
                Platform.runLater(() -> {
                    menuSemanalActual = menuSemanal;

                    if (menuSemanal != null) {
                        // Mostrar mensaje de depuración
                        System.out.println("[DEBUG] Menú cargado: " +
                                (menuSemanal.getMenus() != null ? menuSemanal.getMenus().size() : 0) + " menús");

                        if (menuSemanal.getMenus() != null && !menuSemanal.getMenus().isEmpty()) {
                            mostrarMenuEnCalendario(menuSemanal);
                            lblStatus.setText("✅ Menú cargado correctamente (" +
                                    menuSemanal.getMenus().size() + " menús encontrados)");
                        } else {
                            mostrarCalendarioVacio();
                            lblStatus.setText("⚠️ No hay menús asignados para esta semana");
                        }
                    } else {
                        mostrarCalendarioVacio();
                        lblStatus.setText("⚠️ No se encontró menú para esta semana");
                    }

                    // Habilitar botones de edición/eliminación si hay menú
                    boolean tieneMenu = menuSemanal != null &&
                            menuSemanal.getMenus() != null &&
                            !menuSemanal.getMenus().isEmpty();
                    habilitarDeshabilitarBotones(tieneMenu);
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar menú: " + error);
                    menuSemanalActual = null;
                    mostrarCalendarioVacio();
                    btnEditarMenu.setDisable(true);
                    btnEliminarMenu.setDisable(true);
                    btnVisualizarMenu.setDisable(true);
                });
            }
        });
    }

    private void mostrarCalendarioVacio() {
        for (VBox celda : mapaCeldas.values()) {
            celda.getChildren().clear();
            Label lblVacio = new Label("(Sin asignar)");
            lblVacio.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            celda.getChildren().add(lblVacio);
        }
    }

    private void mostrarMenuEnCalendario(MenuSemanal menuSemanal) {
        List<Menu> menus = menuSemanal.getMenus();

        for (Menu menu : menus) {
            String dia = menu.getDiaSemana();
            String horario = menu.getHorario();
            String clave = dia + "-" + horario;

            VBox celda = mapaCeldas.get(clave);
            if (celda != null) {
                celda.getChildren().clear();

                List<MenuSeccion> secciones = menu.getSecciones();
                if (!secciones.isEmpty()) {
                    for (MenuSeccion menuSeccion : secciones) {
                        agregarSeccionACelda(celda, menuSeccion);
                    }
                } else {
                    Label lblVacio = new Label("(Sin asignar)");
                    lblVacio.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
                    celda.getChildren().add(lblVacio);
                }
            }
        }
    }

    private void agregarSeccionACelda(VBox celda, MenuSeccion menuSeccion) {
        if (menuSeccion == null) {
            return;
        }

        String nombre = menuSeccion.getNombre() != null ? menuSeccion.getNombre() : "Sin nombre";
        String color = menuSeccion.getColor() != null ? menuSeccion.getColor() : "#3498db";

        HBox seccionContainer = new HBox();
        seccionContainer.setAlignment(Pos.CENTER_LEFT);
        seccionContainer.setSpacing(5);
        seccionContainer.setStyle("-fx-padding: 2 0;");

        // Color indicator
        Region colorIndicator = new Region();
        colorIndicator.setPrefSize(10, 10);
        colorIndicator.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

        Label lblSeccion = new Label(nombre);
        lblSeccion.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        // Tooltip con más información
        StringBuilder tooltipText = new StringBuilder(nombre);

        if (menuSeccion.getDescripcion() != null && !menuSeccion.getDescripcion().isEmpty()) {
            tooltipText.append("\n\n").append(menuSeccion.getDescripcion());
        }

        if (menuSeccion.getFechaAsignacion() != null && !menuSeccion.getFechaAsignacion().isEmpty()) {
            tooltipText.append("\n\nAsignado: ").append(menuSeccion.getFechaAsignacion());
        }

        Tooltip tooltip = new Tooltip(tooltipText.toString());
        Tooltip.install(seccionContainer, tooltip);

        seccionContainer.getChildren().addAll(colorIndicator, lblSeccion);
        celda.getChildren().add(seccionContainer);
    }

    private void calcularFechasSemana(int semana, int anio) {
        LocalDate fecha = LocalDate.now()
                .withYear(anio)
                .with(WeekFields.ISO.weekOfYear(), semana)
                .with(WeekFields.ISO.dayOfWeek(), 1);

        fechaInicioSemanaActual = fecha;
        LocalDate fechaFin = fecha.plusDays(4);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        lblRangoFechas.setText("📆 " + fecha.format(formatter) + " al " + fechaFin.format(formatter));
    }

    private void limpiarCalendario() {
        for (VBox celda : mapaCeldas.values()) {
            celda.getChildren().clear();
        }
    }

    @FXML
    private void onNuevoMenuClicked() {
        abrirRegistroMenu(false, 0, 0);
    }

    @FXML
    private void onEditarMenuClicked() {
        if (semanaActual == 0 || anioActual == 0 || menuSemanalActual == null) {
            mostrarAlerta("⚠️ Sin menú cargado", "Primero carga un menú existente.");
            return;
        }
        abrirRegistroMenu(true, semanaActual, anioActual);
    }

    @FXML
    private void onVisualizarMenuClicked() {
        if (semanaActual == 0 || anioActual == 0 || menuSemanalActual == null) {
            mostrarAlerta("⚠️ Sin menú cargado", "Primero carga un menú existente.");
            return;
        }
        abrirRegistroMenu(true, semanaActual, anioActual, true);
    }

    @FXML
    private void onGenerarMenuClicked() {
        if (fechaInicioSemanaActual == null) {
            mostrarAlerta("⚠️ Error", "No se pudo determinar la fecha de inicio de la semana.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Generar Menú Semanal");
        confirm.setHeaderText("¿Generar nuevo menú semanal?");
        confirm.setContentText("Se generará un menú para la semana " + semanaActual + "/" + anioActual +
                "\n\nEsta acción creará slots vacíos para Lunes a Viernes, Desayuno y Comida.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            generarMenuSemana();
        }
    }

    private void generarMenuSemana() {
        if (semanaActual == 0 || anioActual == 0) {
            mostrarAlerta("⚠️ Error", "Selecciona una semana válida primero.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Generar Menú Semanal");
        confirm.setHeaderText("¿Generar nuevo menú semanal?");
        confirm.setContentText("Se generará un menú para la semana " + semanaActual + "/" + anioActual +
                "\n\nEsta acción creará slots vacíos para Lunes a Viernes, Desayuno y Comida.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            lblStatus.setText("Generando menú semanal...");

            // Obtener el ID del usuario actual
            int userId = session.isAuthenticated() && session.getCurrentUser() != null
                    ? session.getCurrentUser().getId()
                    : 1;

            // Calcular la fecha de inicio (lunes de la semana)
            LocalDate fechaInicio = calcularFechaInicioSemana(semanaActual, anioActual);

            MenuService.generarMenuSemanal(fechaInicio.toString(), userId, new MenuService.Callback() {
                @Override
                public void onSuccess(org.json.JSONObject response) {
                    Platform.runLater(() -> {
                        if (response.optBoolean("success", false)) {
                            lblStatus.setText("✅ Menú semanal generado correctamente");
                            onCargarSemanaClicked(); // Recargar
                        } else {
                            String error = response.optString("error", "Error desconocido");
                            lblStatus.setText("❌ Error: " + error);
                            mostrarAlerta("Error", error);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> {
                        lblStatus.setText("❌ Error al generar menú: " + error);
                        mostrarAlerta("Error", error);
                    });
                }
            });
        }
    }

    private LocalDate calcularFechaInicioSemana(int semana, int anio) {
        try {
            return LocalDate.now()
                    .withYear(anio)
                    .with(WeekFields.ISO.weekOfYear(), semana)
                    .with(WeekFields.ISO.dayOfWeek(), 1); // Lunes
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private void abrirRegistroMenu(boolean esEdicion, int semana, int anio) {
        abrirRegistroMenu(esEdicion, semana, anio, false);
    }

    private void abrirRegistroMenu(boolean esEdicion, int semana, int anio, boolean soloLectura) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/menus/RegistroMenu.fxml"));
            Parent root = loader.load();

            RegistroMenuController controller = loader.getController();

            if (soloLectura) {
                controller.visualizarMenu(semana, anio);
            } else if (esEdicion) {
                controller.cargarDatosMenu(semana, anio);
            } else {
                // Modo creación nueva
                controller.modoEdicion(true);
                controller.calcularFechasSemana();
            }

            Stage stage = new Stage();
            stage.setTitle(esEdicion ? (soloLectura ? "Visualizar Menú Semanal" : "Editar Menú Semanal")
                    : "Nuevo Menú Semanal");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);

            stage.setOnHidden(e -> {
                onCargarSemanaClicked(); // Recargar la vista actual
            });

            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ Error", "No se pudo abrir el formulario de menú: " + e.getMessage());
        }
    }

    @FXML
    private void onEliminarMenuClicked() {
        if (semanaActual == 0 || anioActual == 0 || menuSemanalActual == null) {
            mostrarAlerta("⚠️ Sin menú cargado", "Primero carga un menú existente.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar el menú de la semana " + semanaActual + "/" + anioActual + "?");
        confirm.setContentText("Esta acción eliminará:\n" +
                "• Todos los slots del menú (Lunes-Viernes, Desayuno-Comida)\n" +
                "• Todas las asignaciones de secciones\n" +
                "• No afecta a las secciones ni productos\n\n" +
                "⚠️ Esta acción no se puede deshacer.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                eliminarMenu();
            }
        });
    }

    private void eliminarMenu() {
        if (semanaActual == 0 || anioActual == 0) {
            mostrarAlerta("⚠️ Error", "No hay un menú seleccionado para eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar el menú de la semana " + semanaActual + "/" + anioActual + "?");
        confirm.setContentText("Esta acción eliminará:\n" +
                "• Todos los slots del menú (Lunes-Viernes, Desayuno-Comida)\n" +
                "• Todas las asignaciones de secciones\n" +
                "• No afecta a las secciones ni productos\n\n" +
                "⚠️ Esta acción no se puede deshacer.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                lblStatus.setText("Eliminando menú semana " + semanaActual + "/" + anioActual + "...");

                // DEBUG
                System.out.println("[DEBUG] Intentando eliminar semana: " + semanaActual + ", año: " + anioActual);

                MenuService.eliminarMenuCompleto(semanaActual, anioActual, new MenuService.Callback() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Platform.runLater(() -> {
                            System.out.println("[DEBUG] Respuesta eliminación: " + response.toString());

                            if (response.optBoolean("success", false)) {
                                lblStatus.setText("✅ Menú eliminado correctamente");
                                limpiarCalendario();
                                menuSemanalActual = null;
                                lblRangoFechas.setText("Menú eliminado - Semana " + semanaActual + "/" + anioActual);

                                // Deshabilitar botones
                                btnEditarMenu.setDisable(true);
                                btnEliminarMenu.setDisable(true);
                                btnVisualizarMenu.setDisable(true);

                                mostrarAlerta("✅ Éxito", "Menú eliminado correctamente.");
                            } else {
                                String error = response.optString("error", "Error desconocido");
                                String debug = response.optString("debug", "");
                                lblStatus.setText("❌ Error: " + error);
                                mostrarAlerta("Error", error + (debug.isEmpty() ? "" : "\n\nDebug: " + debug));
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            System.err.println("[ERROR] Error eliminando menú: " + error);
                            lblStatus.setText("❌ Error al eliminar menú: " + error);
                            mostrarAlerta("Error", "Error al eliminar menú: " + error);
                        });
                    }
                });
            }
        });
    }

    // 📦 GESTIÓN DE SECCIONES CON PRODUCTOS
    private void configurarTablaSecciones() {
        colSecID.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colSecNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colSecDescripcion.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getDescripcion() != null ? data.getValue().getDescripcion() : ""));

        colSecColor.setCellFactory(tc -> new TableCell<SeccionMenu, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    SeccionMenu seccion = getTableRow().getItem();
                    String color = seccion.getColor() != null ? seccion.getColor() : "#3498db";

                    Label circulo = new Label("●");
                    circulo.setStyle("-fx-font-size: 20px; -fx-text-fill: " + color + ";");
                    setGraphic(circulo);
                }
            }
        });

        colSecProductos.setCellValueFactory(data -> {
            int cantidad = data.getValue().getProductos().size();
            return new SimpleStringProperty(cantidad + " producto(s)");
        });

        colSecAcciones.setReorderable(false);
        colSecAcciones.setResizable(false);
        colSecAcciones.setSortable(false);
        colSecAcciones.setMinWidth(310);
        colSecAcciones.setCellFactory(tc -> new TableCell<SeccionMenu, Void>() {
            private final Button btnVer = new Button("Ver");
            private final Button btnEditar = new Button("Editar");
            private final Button btnEliminar = new Button("Eliminar");

            {
                btnVer.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                btnVer.setMinWidth(100);
                btnEditar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                btnEditar.setMinWidth(100);
                btnEliminar.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
                btnEliminar.setMinWidth(100);

                btnVer.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                btnEditar.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");

                btnVer.setTooltip(new Tooltip("Ver productos"));
                btnEditar.setTooltip(new Tooltip("Editar sección"));
                btnEliminar.setTooltip(new Tooltip("Eliminar sección"));

                btnVer.setOnAction(e -> {
                    SeccionMenu seccion = getTableRow().getItem();
                    if (seccion != null) {
                        verProductosSeccion(seccion);
                    }
                });

                btnEditar.setOnAction(e -> {
                    SeccionMenu seccion = getTableRow().getItem();
                    if (seccion != null) {
                        abrirDialogoSeccion(false, seccion);
                    }
                });

                btnEliminar.setOnAction(e -> {
                    SeccionMenu seccion = getTableRow().getItem();
                    if (seccion != null) {
                        confirmarEliminarSeccion(seccion);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5, btnVer, btnEditar, btnEliminar);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });

        tablaSecciones.setItems(seccionesData);
    }

    private void cargarSecciones() {
        lblStatus.setText("Cargando secciones...");
        seccionesData.clear();

        MenuService.listSecciones(new MenuService.SeccionesCallback() {
            @Override
            public void onSuccess(List<SeccionMenu> secciones) {
                Platform.runLater(() -> {
                    seccionesData.addAll(secciones);
                    lblStatus.setText("✅ " + seccionesData.size() + " secciones cargadas desde base de datos");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar secciones: " + error);
                });
            }
        });
    }

    @FXML
    private void onNuevaSeccionClicked() {
        abrirDialogoSeccion(true, null);
    }

    @FXML
    private void onActualizarSeccionesClicked() {
        cargarSecciones();
    }

    /**
     * Diálogo para crear/editar secciones con selector de productos
     */
    private void abrirDialogoSeccion(boolean esNueva, SeccionMenu seccionExistente) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(esNueva ? "Nueva Sección" : "Editar Sección");
        dialog.initModality(Modality.APPLICATION_MODAL);

        // Crear formulario
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(550);

        // === DATOS BÁSICOS ===
        Label lblNombre = new Label("* Nombre:");
        TextField txtNombre = new TextField();
        txtNombre.setPromptText("Ej: Hamburguesas");

        Label lblDescripcion = new Label("Descripción:");
        TextArea txtDescripcion = new TextArea();
        txtDescripcion.setPrefRowCount(2);
        txtDescripcion.setPromptText("Descripción de la sección");

        Label lblColor = new Label("Color:");
        ColorPicker colorPicker = new ColorPicker(Color.web("#3498db"));

        // === SELECTOR DE PRODUCTOS ===
        Label lblProductos = new Label("Productos de esta sección:");
        lblProductos.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        // ListView con CheckBoxes para productos
        ListView<CheckBox> listaProductos = new ListView<>();
        ObservableList<CheckBox> itemsProductos = FXCollections.observableArrayList();

        // Cargar productos actuales si es edición
        final Set<Integer> productosActuales = new HashSet<>();
        if (!esNueva && seccionExistente != null) {
            productosActuales.addAll(seccionExistente.getProductos().stream()
                    .map(SeccionProducto::getIdProducto)
                    .collect(Collectors.toSet()));
        }

        // Llenar lista de productos disponibles
        for (Producto producto : productosData) {
            int id = producto.getId();
            String nombre = producto.getNombre();
            String categoria = producto.getCategoria();

            if (categoria == null || categoria.isEmpty()) {
                categoria = "Sin categoría";
            }

            CheckBox cb = new CheckBox(String.format("%s (%s) - $%.2f",
                    nombre, categoria, producto.getPrecioBase()));
            cb.setUserData(id);
            cb.setStyle("-fx-font-size: 12px;");

            // Marcar si está en la sección actual
            if (productosActuales.contains(id)) {
                cb.setSelected(true);
            }

            itemsProductos.add(cb);
        }

        listaProductos.setItems(itemsProductos);
        listaProductos.setPrefHeight(250);

        // Botones de selección rápida
        HBox botonesSeleccion = new HBox(10);
        Button btnSeleccionarTodos = new Button("✓ Todos");
        Button btnLimpiarSeleccion = new Button("✗ Ninguno");

        btnSeleccionarTodos.setOnAction(e -> {
            for (CheckBox cb : itemsProductos) {
                cb.setSelected(true);
            }
        });

        btnLimpiarSeleccion.setOnAction(e -> {
            for (CheckBox cb : itemsProductos) {
                cb.setSelected(false);
            }
        });

        botonesSeleccion.getChildren().addAll(btnSeleccionarTodos, btnLimpiarSeleccion);

        // Cargar datos si es edición
        if (!esNueva && seccionExistente != null) {
            txtNombre.setText(seccionExistente.getNombre());
            txtDescripcion.setText(seccionExistente.getDescripcion() != null ? seccionExistente.getDescripcion() : "");
            String colorHex = seccionExistente.getColor();
            if (colorHex != null && !colorHex.isEmpty()) {
                try {
                    colorPicker.setValue(Color.web(colorHex));
                } catch (Exception e) {
                    colorPicker.setValue(Color.web("#3498db"));
                }
            }
        }

        // Campo de búsqueda
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("🔍 Buscar producto...");
        txtBuscar.textProperty().addListener((obs, old, nuevo) -> {
            String busqueda = nuevo.toLowerCase();
            for (CheckBox cb : itemsProductos) {
                boolean visible = cb.getText().toLowerCase().contains(busqueda);
                cb.setVisible(visible);
                cb.setManaged(visible);
            }
        });

        content.getChildren().addAll(
                lblNombre, txtNombre,
                lblDescripcion, txtDescripcion,
                lblColor, colorPicker,
                new Separator(),
                lblProductos,
                txtBuscar,
                listaProductos,
                botonesSeleccion);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                String nombre = txtNombre.getText().trim();
                String descripcion = txtDescripcion.getText().trim();
                String color = String.format("#%02X%02X%02X",
                        (int) (colorPicker.getValue().getRed() * 255),
                        (int) (colorPicker.getValue().getGreen() * 255),
                        (int) (colorPicker.getValue().getBlue() * 255));

                if (nombre.isEmpty()) {
                    mostrarAlerta("⚠️ Campo requerido", "El nombre es obligatorio");
                    return;
                }

                // Recopilar productos seleccionados
                List<Integer> productosSeleccionados = new ArrayList<>();
                for (CheckBox cb : itemsProductos) {
                    if (cb.isSelected()) {
                        productosSeleccionados.add((Integer) cb.getUserData());
                    }
                }

                if (esNueva) {
                    crearSeccionConProductos(nombre, descripcion, color, productosSeleccionados);
                } else {
                    actualizarSeccionConProductos(
                            seccionExistente.getId(),
                            nombre,
                            descripcion,
                            color,
                            productosActuales,
                            productosSeleccionados);
                }
            }
        });
    }

    /**
     * Ver productos de una sección
     */
    private void verProductosSeccion(SeccionMenu seccion) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Productos de: " + seccion.getNombre());

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        List<SeccionProducto> productos = seccion.getProductos();

        if (!productos.isEmpty()) {
            ListView<String> lista = new ListView<>();
            ObservableList<String> items = FXCollections.observableArrayList();

            for (SeccionProducto seccionProducto : productos) {
                // Buscar el producto en los datos cargados
                Producto producto = productosData.stream()
                        .filter(p -> p.getId() == seccionProducto.getIdProducto())
                        .findFirst()
                        .orElse(null);

                if (producto != null) {
                    String categoria = producto.getCategoria();
                    if (categoria == null || categoria.isEmpty()) {
                        categoria = "Sin categoría";
                    }

                    String item = String.format("%s (%s) - $%.2f",
                            producto.getNombre(), categoria, producto.getPrecioBase());

                    if (seccionProducto.isDestacado()) {
                        item += " ⭐";
                    }

                    items.add(item);
                } else {
                    items.add("Producto ID: " + seccionProducto.getIdProducto() + " (no encontrado)");
                }
            }

            lista.setItems(items);
            lista.setPrefHeight(300);

            Label lblTotal = new Label("Total: " + productos.size() + " productos");
            lblTotal.setStyle("-fx-font-weight: bold;");

            content.getChildren().addAll(lblTotal, lista);
        } else {
            Label lblVacio = new Label("Esta sección no tiene productos asignados");
            lblVacio.setStyle("-fx-text-fill: #95a5a6; -fx-font-style: italic;");
            content.getChildren().add(lblVacio);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.show();
    }

    /**
     * Crear sección con productos
     */
    private void crearSeccionConProductos(String nombre, String descripcion, String color,
            List<Integer> productosSeleccionados) {
        lblStatus.setText("Creando sección...");

        // Crear objeto SeccionMenu
        SeccionMenu nuevaSeccion = new SeccionMenu();
        nuevaSeccion.setNombre(nombre);
        nuevaSeccion.setDescripcion(descripcion);
        nuevaSeccion.setColor(color);
        nuevaSeccion.setActivo(true);
        nuevaSeccion.setFechaCreacion(LocalDate.now().toString());

        // Agregar productos
        List<SeccionProducto> productos = new ArrayList<>();
        int orden = 1;
        for (Integer idProducto : productosSeleccionados) {
            SeccionProducto seccionProducto = new SeccionProducto();
            seccionProducto.setIdProducto(idProducto);
            seccionProducto.setOrden(orden);
            seccionProducto.setDestacado(false);
            productos.add(seccionProducto);
            orden++;
        }
        nuevaSeccion.setProductos(productos);

        MenuService.createSeccion(nuevaSeccion, new MenuService.CrudCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    lblStatus.setText("✅ Sección creada con " +
                            productosSeleccionados.size() + " productos");
                    cargarSecciones();
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al crear sección: " + error);
                });
            }
        });
    }

    /**
     * Actualizar sección con productos
     */
    private void actualizarSeccionConProductos(int id, String nombre, String descripcion,
            String color, Set<Integer> productosAnteriores,
            List<Integer> productosNuevos) {
        lblStatus.setText("Actualizando sección...");

        // Crear objeto SeccionMenu actualizado
        SeccionMenu seccionActualizada = new SeccionMenu();
        seccionActualizada.setId(id);
        seccionActualizada.setNombre(nombre);
        seccionActualizada.setDescripcion(descripcion);
        seccionActualizada.setColor(color);
        seccionActualizada.setActivo(true);

        // Agregar productos actualizados
        List<SeccionProducto> productos = new ArrayList<>();
        int orden = 1;
        for (Integer idProducto : productosNuevos) {
            SeccionProducto seccionProducto = new SeccionProducto();
            seccionProducto.setIdProducto(idProducto);
            seccionProducto.setOrden(orden);
            seccionProducto.setDestacado(false);
            productos.add(seccionProducto);
            orden++;
        }
        seccionActualizada.setProductos(productos);

        MenuService.updateSeccion(seccionActualizada, new MenuService.CrudCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    lblStatus.setText("✅ Sección actualizada correctamente");
                    cargarSecciones();
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al actualizar sección: " + error);
                });
            }
        });
    }

    private void confirmarEliminarSeccion(SeccionMenu seccion) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar la sección \"" + seccion.getNombre() + "\"?");
        confirm.setContentText("Esta acción eliminará:\n" +
                "• La sección del menú\n" +
                "• " + seccion.getProductos().size() + " productos asociados\n" +
                "• Todas las asignaciones a menús\n\n" +
                "⚠️ Esta acción no se puede deshacer.");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                eliminarSeccion(seccion.getId());
            }
        });
    }

    private void eliminarSeccion(int id) {
        lblStatus.setText("Eliminando sección...");

        MenuService.deleteSeccion(id, new MenuService.CrudCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    lblStatus.setText("✅ Sección eliminada correctamente");
                    cargarSecciones();
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al eliminar sección: " + error);
                });
            }
        });
    }

    // 🔧 UTILIDADES
    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }
}