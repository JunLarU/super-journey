package app.controllers.menus;

import core.data.Menus.AllMenus;
import core.data.Menus.Menu;
import core.data.Menus.MenuSeccion;
import core.data.Menus.SeccionMenu;
import core.data.Menus.SeccionProducto;
import core.data.Productos.AllProductos;
import core.data.Productos.Producto;
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
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador mejorado para gestión de menús semanales y secciones con
 * productos
 * Ahora usando archivos JSON en lugar de base de datos
 */
public class MenusController {

    // =========================
    // 📌 COMPONENTES MENÚS
    // =========================
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
    private Button btnNuevoMenu, btnEditarMenu, btnEliminarMenu;

    // =========================
    // 📌 COMPONENTES SECCIONES
    // =========================
    @FXML
    private TableView<SeccionMenu> tablaSecciones;
    @FXML
    private TableColumn<SeccionMenu, String> colSecID, colSecNombre, colSecDescripcion;
    @FXML
    private TableColumn<SeccionMenu, String> colSecColor, colSecProductos;
    @FXML
    private TableColumn<SeccionMenu, Void> colSecAcciones;
    @FXML
    private Button btnNuevaSeccion;

    // =========================
    // 📌 MODELOS Y DATOS
    // =========================
    private final AllMenus allMenus = AllMenus.getInstance();
    private final AllProductos allProductos = AllProductos.getInstance();
    private final SessionManager session = SessionManager.getInstance();
    private final ObservableList<SeccionMenu> seccionesData = FXCollections.observableArrayList();
    private List<Producto> productosDisponibles = new ArrayList<>();

    private Map<String, VBox> mapaCeldas = new HashMap<>();
    private Menu menuSemanalActual = null;
    private int semanaActual = 0;
    private int anioActual = 0;

    // =========================
    // 📌 INICIALIZACIÓN
    // =========================
    @FXML
    public void initialize() {
        configurarSpinners();
        configurarMapa();
        configurarTablaSecciones();
        cargarDatosIniciales();
        lblStatus.setText("✅ Sistema listo");
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

    private void cargarProductosDisponibles() {
        productosDisponibles = allProductos.getAll().stream()
                .filter(Producto::isDisponible)
                .collect(Collectors.toList());
        //System.out.println("✅ " + productosDisponibles.size() + " productos disponibles cargados");
    }

    // =========================
    // 📅 GESTIÓN DE MENÚS
    // =========================

    @FXML
    private void onCargarSemanaClicked() {
        semanaActual = spinSemana.getValue();
        anioActual = spinAnio.getValue();

        lblStatus.setText("Cargando menú de la semana " + semanaActual + "/" + anioActual + "...");
        limpiarCalendario();

        new Thread(() -> {
            try {
                List<Menu> menus = allMenus.getMenusBySemana(semanaActual, anioActual);

                Platform.runLater(() -> {
                    if (!menus.isEmpty()) {
                        mostrarMenuEnCalendario(menus);
                        lblStatus.setText("✅ Menú cargado correctamente");
                    } else {
                        lblStatus.setText("⚠️ No hay menú para esta semana");
                        menuSemanalActual = null;
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar menú: " + e.getMessage());
                    menuSemanalActual = null;
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void mostrarMenuEnCalendario(List<Menu> menus) {
        if (menus.isEmpty()) {
            lblRangoFechas.setText("No hay menú para esta semana");
            return;
        }

        // Calcular rango de fechas
        LocalDate primeraFecha = menus.get(0).getFecha();
        LocalDate ultimaFecha = menus.get(menus.size() - 1).getFecha();
        lblRangoFechas.setText("📆 " + primeraFecha + " al " + ultimaFecha);

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
        String nombre = menuSeccion.getNombreSeccion();
        String color = "#3498db"; // Color por defecto

        // Buscar la sección para obtener su color
        SeccionMenu seccion = allMenus.getSeccionById(menuSeccion.getIdSeccion());
        if (seccion != null) {
            color = seccion.getColor();
        }

        Label lblSeccion = new Label("📦 " + nombre);
        lblSeccion.setStyle(
                "-fx-background-color: " + color + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 5 10; " +
                        "-fx-background-radius: 5; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold;");
        lblSeccion.setMaxWidth(Double.MAX_VALUE);

        celda.getChildren().add(lblSeccion);
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
        if (semanaActual == 0 || anioActual == 0) {
            mostrarAlerta("⚠️ Sin menú cargado", "Primero carga un menú existente.");
            return;
        }
        abrirRegistroMenu(true, semanaActual, anioActual);
    }

    @FXML
    private void onVisualizarMenuClicked() {
        if (semanaActual == 0 || anioActual == 0) {
            mostrarAlerta("⚠️ Sin menú cargado", "Primero carga un menú existente.");
            return;
        }
        abrirRegistroMenu(true, semanaActual, anioActual, true);
    }

    /**
     * Abre la ventana de registro/edición de menú
     */
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
            }
            // Si no es edición ni visualización, se queda en modo nuevo menú

            Stage stage = new Stage();
            stage.setTitle(esEdicion ? "Editar Menú Semanal" : "Nuevo Menú Semanal");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(true);
            
            // Cuando se cierra la ventana, recargar los datos
            stage.setOnHidden(e -> {
                onCargarSemanaClicked(); // Recargar la vista actual
            });
            
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta("❌ Error", "No se pudo abrir el formulario de menú: " + e.getMessage());
        }
    }

    private void generarMenuSemana() {
        LocalDate fechaInicio = LocalDate.now();
        int idUsuario = 1; // Por defecto, podrías obtenerlo de la sesión

        allMenus.generarMenusSemana(fechaInicio, idUsuario);

        semanaActual = fechaInicio.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
        anioActual = fechaInicio.getYear();

        spinSemana.getValueFactory().setValue(semanaActual);
        spinAnio.getValueFactory().setValue(anioActual);

        onCargarSemanaClicked();
        lblStatus.setText("✅ Menú semanal generado correctamente");
    }

    @FXML
    private void onEliminarMenuClicked() {
        if (semanaActual == 0 || anioActual == 0) {
            mostrarAlerta("⚠️ Sin menú cargado", "Primero carga un menú existente.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar el menú de la semana " + semanaActual + "/" + anioActual + "?\n\n" +
                        "Esta acción no se puede deshacer.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                eliminarMenu();
            }
        });
    }

    private void eliminarMenu() {
        lblStatus.setText("Eliminando menú...");

        new Thread(() -> {
            try {
                List<Menu> menus = allMenus.getMenusBySemana(semanaActual, anioActual);
                for (Menu menu : menus) {
                    allMenus.removeMenu(menu.getId());
                }

                Platform.runLater(() -> {
                    lblStatus.setText("✅ Menú eliminado correctamente");
                    limpiarCalendario();
                    menuSemanalActual = null;
                    lblRangoFechas.setText("Selecciona una semana");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    // =========================
    // 📦 GESTIÓN DE SECCIONES CON PRODUCTOS
    // =========================

    private void configurarTablaSecciones() {
        colSecID.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));
        colSecNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colSecDescripcion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescripcion()));

        colSecColor.setCellFactory(tc -> new TableCell<SeccionMenu, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    SeccionMenu seccion = getTableRow().getItem();
                    String color = seccion.getColor();

                    Label circulo = new Label("●");
                    circulo.setStyle("-fx-font-size: 20px; -fx-text-fill: " + color + ";");
                    setGraphic(circulo);
                }
            }
        });

        // Mostrar cantidad de productos
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

        new Thread(() -> {
            try {
                List<SeccionMenu> secciones = allMenus.getAllSecciones();

                Platform.runLater(() -> {
                    seccionesData.addAll(secciones);
                    lblStatus.setText("✅ " + seccionesData.size() + " secciones cargadas");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error al cargar secciones: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void onNuevaSeccionClicked() {
        abrirDialogoSeccion(true, null);
    }

    /**
     * Diálogo mejorado para crear/editar secciones CON selector de productos
     */
    private void abrirDialogoSeccion(boolean esNueva, SeccionMenu seccionExistente) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(esNueva ? "Nueva Sección" : "Editar Editar Sección");

        // Crear formulario
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);
        content.setPrefHeight(500);

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

        // Cargar productos actuales si es edición - CORREGIDO
        final Set<Integer> productosActuales = new HashSet<>();
        if (!esNueva && seccionExistente != null) {
            // En lugar de reasignar, agregamos elementos al Set existente
            productosActuales.addAll(seccionExistente.getProductos().stream()
                    .map(SeccionProducto::getIdProducto)
                    .collect(Collectors.toSet()));
        }

        // Llenar lista de productos disponibles
        for (Producto producto : productosDisponibles) {
            int id = producto.getId();
            String nombre = producto.getNombre();
            String categoria = producto.getCategoria();

            if (categoria == null || categoria.isEmpty()) {
                categoria = "Sin categoría";
            }

            CheckBox cb = new CheckBox(nombre + " (" + categoria + ") - $" + producto.getPrecioBase());
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
            txtDescripcion.setText(seccionExistente.getDescripcion());
            String colorHex = seccionExistente.getColor();
            colorPicker.setValue(Color.web(colorHex));
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
                            productosActuales, // ✅ Ahora funciona porque no se reasigna
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
        dialog.setTitle("Ver Productos de: " + seccion.getNombre());

        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.setPrefWidth(500);

        List<SeccionProducto> productos = seccion.getProductos();

        if (!productos.isEmpty()) {
            ListView<String> lista = new ListView<>();
            ObservableList<String> items = FXCollections.observableArrayList();

            for (SeccionProducto seccionProducto : productos) {
                Producto producto = allProductos.getById(seccionProducto.getIdProducto());
                if (producto != null) {
                    String categoria = producto.getCategoria();
                    if (categoria == null || categoria.isEmpty()) {
                        categoria = "Sin categoría";
                    }

                    items.add(String.format("%s (%s) - $%.2f",
                            producto.getNombre(), categoria, producto.getPrecioBase()));
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

        new Thread(() -> {
            try {
                // 1. Crear la sección
                SeccionMenu nuevaSeccion = new SeccionMenu(
                        0, nombre, descripcion, "", color, true, LocalDate.now().toString());

                // 2. Agregar productos a la sección
                int orden = 1;
                for (Integer idProducto : productosSeleccionados) {
                    Producto producto = allProductos.getById(idProducto);
                    String nombreProducto = producto != null ? producto.getNombre() : "Producto";

                    SeccionProducto seccionProducto = new SeccionProducto(
                            0, 0, idProducto, nombreProducto, orden, false);
                    nuevaSeccion.agregarProducto(seccionProducto);
                    orden++;
                }

                allMenus.addSeccion(nuevaSeccion);

                Platform.runLater(() -> {
                    lblStatus.setText("✅ Sección creada con " +
                            productosSeleccionados.size() + " productos");
                    cargarSecciones();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Actualizar sección con productos
     */
    private void actualizarSeccionConProductos(int id, String nombre, String descripcion,
            String color, Set<Integer> productosAnteriores,
            List<Integer> productosNuevos) {
        lblStatus.setText("Actualizando sección...");

        new Thread(() -> {
            try {
                // 1. Obtener la sección existente
                SeccionMenu seccion = allMenus.getSeccionById(id);
                if (seccion == null) {
                    throw new Exception("Sección no encontrada");
                }

                // 2. Actualizar datos básicos
                seccion.setNombre(nombre);
                seccion.setDescripcion(descripcion);
                seccion.setColor(color);

                // 3. Actualizar productos
                List<SeccionProducto> nuevosProductos = new ArrayList<>();
                int orden = 1;

                for (Integer idProducto : productosNuevos) {
                    Producto producto = allProductos.getById(idProducto);
                    String nombreProducto = producto != null ? producto.getNombre() : "Producto";

                    SeccionProducto seccionProducto = new SeccionProducto(
                            0, id, idProducto, nombreProducto, orden, false);
                    nuevosProductos.add(seccionProducto);
                    orden++;
                }

                seccion.setProductos(nuevosProductos);

                // 4. Guardar cambios
                allMenus.updateSeccion(seccion);

                Platform.runLater(() -> {
                    lblStatus.setText("✅ Sección actualizada correctamente");
                    cargarSecciones();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void confirmarEliminarSeccion(SeccionMenu seccion) {
        String nombre = seccion.getNombre();
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Eliminar la sección \"" + nombre + "\"?\n\n" +
                        "Se eliminarán " + seccion.getProductos().size() + " productos asociados.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminación");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                eliminarSeccion(seccion.getId());
            }
        });
    }

    private void eliminarSeccion(int id) {
        lblStatus.setText("Eliminando sección...");

        new Thread(() -> {
            try {
                allMenus.removeSeccion(id);

                Platform.runLater(() -> {
                    lblStatus.setText("✅ Sección eliminada correctamente");
                    cargarSecciones();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    // =========================
    // 🔧 UTILIDADES
    // =========================

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}