// DashboardUserController.java
package app.controllers.dashboard;

import core.data.Ingredientes.AllIngredientes;
import core.data.Ingredientes.Ingrediente;
import core.SessionManager;
import core.data.Users.User;
import core.services.NormalUserService;
import core.data.Menus.*;
import core.data.Menus.Menu;
import core.data.Productos.*;
import core.data.Avisos.*;
import core.data.Ingredientes.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Controlador para el dashboard de usuarios no administradores (alumnos)
 */
public class DashboardUserController {

    @FXML
    private Label lblWelcome;
    @FXML
    private TabPane mainTabPane;
    @FXML
    private Tab tabMenu, tabAvisos, tabEspeciales;
    @FXML
    private Button btnCerrarSesion;

    // Componentes de la pestaña Menú
    @FXML
    private Label lblSemanaActual;
    @FXML
    private VBox vboxMenuContenido;
    @FXML
    private ComboBox<String> comboHorario;
    @FXML
    private Button btnSemanaAnterior, btnSemanaSiguiente;

    // Componentes de la pestaña Avisos
    @FXML
    private VBox vboxAvisosContenido;
    @FXML
    private ComboBox<String> comboFiltroAvisos;

    // Componentes de la pestaña Especiales
    @FXML
    private VBox vboxEspecialesContenido;

    private final SessionManager session = SessionManager.getInstance();
    private final AllMenus allMenus = AllMenus.getInstance();
    private final AllAvisos allAvisos = AllAvisos.getInstance();
    private final AllProductosEspeciales allEspeciales = AllProductosEspeciales.getInstance();
    private final AllProductos allProductos = AllProductos.getInstance();

    private LocalDate fechaActual = LocalDate.now();
    private int semanaActual;
    private int anioActual;

    @FXML
    public void initialize() {
        try {
            User current = session.getCurrentUser();
            if (current != null) {
                lblWelcome.setText("👋 Bienvenido, " + current.getName() + " " + current.getApellidoPaterno());
            }

            configurarInterfaz();
            cargarDatosIniciales();

            // FORZAR TAMAÑO DEL DASHBOARD AL INICIALIZAR
            Platform.runLater(() -> {
                Stage stage = (Stage) lblWelcome.getScene().getWindow();
                if (stage != null) {
                    // Asegurar tamaño del dashboard
                    stage.setResizable(true);
                    stage.setWidth(1100);
                    stage.setHeight(700);
                    stage.setResizable(false);
                    stage.centerOnScreen();
                    System.out.println("Dashboard User - Tamaño forzado: 1100x700");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error al inicializar", "No se pudo cargar el dashboard: " + e.getMessage());
        }
    }

    private void configurarInterfaz() {
        // Configurar combo de horarios
        comboHorario.getItems().addAll("Todos", "Desayuno", "Comida");
        comboHorario.setValue("Todos");
        comboHorario.setOnAction(e -> cargarMenuSemanal());

        // Configurar combo de filtros para avisos
        comboFiltroAvisos.getItems().addAll("Todos", "Cafetería", "Cafecito", "Importantes");
        comboFiltroAvisos.setValue("Todos");
        comboFiltroAvisos.setOnAction(e -> cargarAvisos());

        // Calcular semana actual
        semanaActual = fechaActual
                .get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfWeekBasedYear());
        anioActual = fechaActual.getYear();
    }

    private void cargarDatosIniciales() {
        cargarMenuSemanal();
        cargarAvisos();
        cargarProductosEspeciales();
    }

    // 📅 SECCIÓN DE MENÚ SEMANAL

    @FXML
    private void onSemanaAnterior() {
        fechaActual = fechaActual.minusWeeks(1);
        actualizarSemana();
        cargarMenuSemanal();
    }

    @FXML
    private void onSemanaSiguiente() {
        fechaActual = fechaActual.plusWeeks(1);
        actualizarSemana();
        cargarMenuSemanal();
    }

    private void actualizarSemana() {
        semanaActual = fechaActual
                .get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfWeekBasedYear());
        anioActual = fechaActual.getYear();
    }

    // En DashboardUserController.java, actualiza el método cargarMenuSemanal:

    private void cargarMenuSemanal() {
        vboxMenuContenido.getChildren().clear();
        lblSemanaActual.setText("📅 Semana " + semanaActual + " del " + anioActual);

        // Usar el nuevo servicio
        NormalUserService service = NormalUserService.getInstance();
        String horarioFiltro = comboHorario.getValue();

        Task<JSONObject> task = service.getMenuSemanal(semanaActual, anioActual, horarioFiltro);

        task.setOnSucceeded(event -> {
            try {
                JSONObject result = task.getValue();

                if (!result.has("menus") || result.getJSONArray("menus").length() == 0) {
                    mostrarMensajeSinMenu();
                    return;
                }

                // Procesar el JSON y crear la interfaz
                procesarMenuJSON(result);

            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error", "No se pudo procesar el menú: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            mostrarError("Error", "No se pudo cargar el menú: " + ex.getMessage());
        });

        // Ejecutar la tarea en un hilo separado
        new Thread(task).start();
    }

    private void mostrarMensajeSinMenu() {
        Label lblVacio = new Label("No hay menú disponible para esta semana");
        lblVacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 20;");
        vboxMenuContenido.getChildren().add(lblVacio);
    }

    private void procesarMenuJSON(JSONObject result) {
        JSONArray menusArray = result.getJSONArray("menus");

        // Agrupar por fecha
        Map<String, List<JSONObject>> menusPorFecha = new HashMap<>();

        for (int i = 0; i < menusArray.length(); i++) {
            JSONObject menu = menusArray.getJSONObject(i);
            String fecha = menu.getString("Fecha");

            if (!menusPorFecha.containsKey(fecha)) {
                menusPorFecha.put(fecha, new ArrayList<>());
            }
            menusPorFecha.get(fecha).add(menu);
        }

        // Ordenar fechas y crear cards
        List<String> fechasOrdenadas = new ArrayList<>(menusPorFecha.keySet());
        Collections.sort(fechasOrdenadas);

        for (String fecha : fechasOrdenadas) {
            LocalDate fechaDate = LocalDate.parse(fecha);
            List<JSONObject> menusDia = menusPorFecha.get(fecha);
            VBox diaCard = crearCardDiaDesdeJSON(fechaDate, menusDia);
            vboxMenuContenido.getChildren().add(diaCard);
        }
    }

    private VBox crearCardDiaDesdeJSON(LocalDate fecha, List<JSONObject> menusDia) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-padding: 15;");

        // Header del día
        String[] diasSemana = { "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO" };
        int diaSemana = fecha.getDayOfWeek().getValue() - 1;
        String nombreDia = diasSemana[diaSemana];

        Label lblDia = new Label(nombreDia + " - " + fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM")));
        lblDia.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-background-color: #e9ecef; -fx-padding: 8 12; -fx-background-radius: 6;");

        HBox header = new HBox(lblDia);
        header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);

        // Contenido por horario
        for (JSONObject menu : menusDia) {
            VBox horarioSection = crearSeccionHorarioDesdeJSON(menu);
            card.getChildren().add(horarioSection);
        }

        return card;
    }

    private VBox crearSeccionHorarioDesdeJSON(JSONObject menu) {
        VBox section = new VBox(8);
        section.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 6; -fx-padding: 12;");

        // Header del horario
        String horario = menu.getString("Horario");
        Label lblHorario = new Label(horario.equalsIgnoreCase("Desayuno") ? "🥚DESAYUNO" : "🍕COMIDA");
        lblHorario.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        HBox header = new HBox(lblHorario);
        header.setAlignment(Pos.CENTER_LEFT);
        section.getChildren().add(header);

        // Secciones del menú
        if (menu.has("secciones")) {
            JSONArray seccionesArray = menu.getJSONArray("secciones");

            if (seccionesArray.length() == 0) {
                Label lblVacio = new Label("No hay platillos asignados para este horario");
                lblVacio.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
                section.getChildren().add(lblVacio);
            } else {
                for (int i = 0; i < seccionesArray.length(); i++) {
                    JSONObject seccionJson = seccionesArray.getJSONObject(i);
                    VBox seccionCard = crearCardSeccionDesdeJSON(seccionJson);
                    section.getChildren().add(seccionCard);
                }
            }
        }

        return section;
    }

    private VBox crearCardDia(LocalDate fecha, List<Menu> menusDia) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-padding: 15;");
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);

        // Formatear día de la semana en español
        String[] diasSemana = { "LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO" };
        int diaSemana = fecha.getDayOfWeek().getValue() - 1; // Lunes = 0, Domingo = 6
        String nombreDia = diasSemana[diaSemana];

        // Header del día - USAR COLOR OSCURO
        Label lblDia = new Label(nombreDia + " - " + fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM")));
        lblDia.setStyle(
                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-background-color: #e9ecef; -fx-padding: 8 12; -fx-background-radius: 6;");

        HBox header = new HBox(lblDia);
        header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);

        // Contenido por horario
        menusDia.forEach(menu -> {
            VBox horarioSection = crearSeccionHorario(menu);
            card.getChildren().add(horarioSection);
        });

        return card;
    }

    private VBox crearSeccionHorario(Menu menu) {
        VBox section = new VBox(8);
        section.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 6; -fx-padding: 12;");

        // Header del horario
        Label lblHorario = new Label(menu.getHorario().equalsIgnoreCase("Desayuno") ? "🥚DESAYUNO" : "🍕COMIDA");
        lblHorario.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #495057;");

        HBox header = new HBox(lblHorario);
        header.setAlignment(Pos.CENTER_LEFT);
        section.getChildren().add(header);

        // Secciones del menú
        List<MenuSeccion> secciones = menu.getSecciones();
        if (secciones.isEmpty()) {
            Label lblVacio = new Label("No hay platillos asignados para este horario");
            lblVacio.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            section.getChildren().add(lblVacio);
        } else {
            secciones.forEach(menuSeccion -> {
                VBox seccionCard = crearCardSeccion(menuSeccion);
                section.getChildren().add(seccionCard);
            });
        }

        return section;
    }

    private VBox crearCardSeccion(MenuSeccion menuSeccion) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 4; -fx-padding: 10;");

        // Obtener la sección completa
        SeccionMenu seccion = allMenus.getSeccionById(menuSeccion.getIdSeccion());
        if (seccion == null)
            return card;

        // Determinar si el color de fondo es oscuro para ajustar el texto
        String colorFondo = seccion.getColor();
        String colorTexto = esColorOscuro(colorFondo) ? "white" : "#2c3e50";

        // Header de la sección con color - TEXTO AJUSTADO AUTOMÁTICAMENTE
        Label lblSeccion = new Label("✔️" + seccion.getNombre());
        lblSeccion.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colorTexto +
                        "; -fx-padding: 5 10; -fx-background-radius: 15;");
        lblSeccion.setStyle(lblSeccion.getStyle() + "-fx-background-color: " + colorFondo + ";");

        card.getChildren().add(lblSeccion);

        // Productos de la sección
        List<SeccionProducto> productosSeccion = seccion.getProductos();
        if (productosSeccion.isEmpty()) {
            Label lblVacio = new Label("No hay productos en esta sección");
            lblVacio.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            card.getChildren().add(lblVacio);
        } else {
            productosSeccion.forEach(seccionProducto -> {
                VBox productoCard = crearCardProducto(seccionProducto);
                card.getChildren().add(productoCard);
            });
        }

        return card;
    }

    /**
     * Determina si un color hexadecimal es oscuro para ajustar el color del texto
     */
    private boolean esColorOscuro(String colorHex) {
        try {
            if (colorHex == null || colorHex.isEmpty())
                return false;

            // Asegurar que el color tenga formato #RRGGBB
            String hex = colorHex.startsWith("#") ? colorHex.substring(1) : colorHex;
            if (hex.length() == 3) {
                // Expandir formato #RGB a #RRGGBB
                hex = String.format("%c%c%c%c%c%c",
                        hex.charAt(0), hex.charAt(0),
                        hex.charAt(1), hex.charAt(1),
                        hex.charAt(2), hex.charAt(2));
            }

            if (hex.length() != 6)
                return false;

            // Convertir a componentes RGB
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);

            // Calcular luminosidad (fórmula estándar)
            double luminosidad = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

            // Si la luminosidad es menor a 0.5, es un color oscuro
            return luminosidad < 0.5;

        } catch (Exception e) {
            // En caso de error, asumir que es claro
            return false;
        }
    }

    private VBox crearCardProducto(SeccionProducto seccionProducto) {
        VBox card = new VBox(6);
        card.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 8;");

        // Obtener el producto completo
        Producto producto = allProductos.getById(seccionProducto.getIdProducto());
        if (producto == null)
            return card;

        // Información básica del producto - CORREGIDO: texto oscuro
        HBox infoBasica = new HBox(10);
        infoBasica.setAlignment(Pos.CENTER_LEFT);

        Label lblNombre = new Label(producto.getNombre());
        lblNombre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;"); // CORREGIDO

        Label lblPrecio = new Label(String.format("$%.2f", producto.getPrecioBase()));
        lblPrecio.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblCalorias = new Label(String.format("%.0f cal", producto.getCalorias()));
        lblCalorias.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        infoBasica.getChildren().addAll(lblNombre, spacer, lblPrecio, lblCalorias);
        card.getChildren().add(infoBasica);

        // Descripción
        if (producto.getDescripcion() != null && !producto.getDescripcion().isEmpty()) {
            Label lblDesc = new Label(producto.getDescripcion());
            lblDesc.setStyle("-fx-text-fill: #495057; -fx-font-size: 12px;");
            lblDesc.setWrapText(true);
            card.getChildren().add(lblDesc);
        }

        // Tamaños disponibles
        List<TamanoProducto> tamanos = producto.getTamanos().stream()
                .filter(TamanoProducto::isDisponible)
                .collect(Collectors.toList());

        if (!tamanos.isEmpty()) {
            VBox tamanosBox = new VBox(4);
            Label lblTamanos = new Label("📏 Tamaños disponibles:");
            lblTamanos.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #495057;");
            tamanosBox.getChildren().add(lblTamanos);

            tamanos.forEach(tamano -> {
                Label lblTamano = new Label(
                        "  • " + tamano.getNombre() + " - $" + String.format("%.2f", tamano.getPrecio()));
                lblTamano.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                tamanosBox.getChildren().add(lblTamano);
            });
            card.getChildren().add(tamanosBox);
        }

        // Ingredientes
        List<ProductoIngrediente> ingredientes = producto.getIngredientes();
        if (!ingredientes.isEmpty()) {
            VBox ingredientesBox = new VBox(4);
            Label lblIngredientes = new Label("🥗 Ingredientes:");
            lblIngredientes.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #495057;");
            ingredientesBox.getChildren().add(lblIngredientes);

            ingredientes.forEach(ingrediente -> {
                HBox ingRow = new HBox(5);
                ingRow.setAlignment(Pos.CENTER_LEFT);

                Label lblIng = new Label("• " + ingrediente.getNombreIngrediente());
                lblIng.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;"); // CORREGIDO: texto oscuro

                // Indicadores de opciones
                HBox opciones = new HBox(5);
                if (ingrediente.isEliminable()) {
                    Label lblEliminable = new Label("🚫");
                    lblEliminable.setTooltip(new Tooltip("Puede eliminarse"));
                    lblEliminable.setStyle("-fx-font-size: 10px;");
                    opciones.getChildren().add(lblEliminable);
                }
                if (ingrediente.isSustituible()) {
                    Label lblSustituible = new Label("🔄");
                    lblSustituible.setTooltip(new Tooltip("Puede sustituirse"));
                    lblSustituible.setStyle("-fx-font-size: 10px;");
                    opciones.getChildren().add(lblSustituible);
                }

                // Obtener el ingrediente completo para verificar si es alergénico
                Ingrediente ingredienteCompleto = AllIngredientes.getInstance().getById(ingrediente.getIdIngrediente());
                if (ingredienteCompleto != null && ingredienteCompleto.isAlergenico()) {
                    Label lblAlergenico = new Label("⚠️");
                    lblAlergenico.setTooltip(new Tooltip("Alergénico"));
                    lblAlergenico.setStyle("-fx-font-size: 10px;");
                    opciones.getChildren().add(lblAlergenico);
                }

                Region spacerIng = new Region();
                HBox.setHgrow(spacerIng, Priority.ALWAYS);

                ingRow.getChildren().addAll(lblIng, spacerIng, opciones);
                ingredientesBox.getChildren().add(ingRow);

                // Mostrar sustitutos si los hay
                if (ingrediente.isSustituible() && !ingrediente.getSustitutos().isEmpty()) {
                    VBox sustitutosBox = new VBox(2);
                    sustitutosBox.setStyle("-fx-padding: 0 0 0 15;");
                    ingrediente.getSustitutos().stream()
                            .filter(Sustituto::isDisponible)
                            .forEach(sustituto -> {
                                Label lblSust = new Label("  ↳ " + sustituto.getNombreIngrediente());
                                lblSust.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
                                if (sustituto.getCostoExtra() > 0) {
                                    lblSust.setText(
                                            lblSust.getText() + String.format(" (+$%.2f)", sustituto.getCostoExtra()));
                                }
                                sustitutosBox.getChildren().add(lblSust);
                            });
                    ingredientesBox.getChildren().add(sustitutosBox);
                }
            });
            card.getChildren().add(ingredientesBox);
        }

        // Botón para ver detalles completos
        Button btnDetalles = new Button("Ver detalles completos");
        btnDetalles.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #007bff; -fx-border-color: #007bff; -fx-border-width: 1; -fx-font-size: 11px;");
        btnDetalles.setOnAction(e -> mostrarDetallesProducto(producto));
        card.getChildren().add(btnDetalles);

        return card;
    }

    private void mostrarDetallesProducto(Producto producto) {
        // Implementar diálogo modal con información completa del producto
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("🍽️ " + producto.getNombre());
        dialog.setHeaderText("Información completa del producto");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Información básica
        Label lblPrecio = new Label("💰 Precio base: $" + String.format("%.2f", producto.getPrecioBase()));
        Label lblCalorias = new Label("🔥 Calorías: " + String.format("%.0f", producto.getCalorias()));
        Label lblGramaje = new Label("⚖️ Gramaje: " + String.format("%.0fg", producto.getGramaje()));

        if (producto.getDescripcion() != null && !producto.getDescripcion().isEmpty()) {
            Label lblDesc = new Label("📝 " + producto.getDescripcion());
            lblDesc.setWrapText(true);
            content.getChildren().add(lblDesc);
        }

        content.getChildren().addAll(lblPrecio, lblCalorias, lblGramaje);
        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // 📢 SECCIÓN DE AVISOS

    private void cargarAvisos() {
        vboxAvisosContenido.getChildren().clear();

        NormalUserService service = NormalUserService.getInstance();
        String filtro = comboFiltroAvisos.getValue();

        String establecimiento = "Todos";
        String prioridad = "Todos";

        if (filtro.equals("Cafetería")) {
            establecimiento = "Cafeteria";
        } else if (filtro.equals("Cafecito")) {
            establecimiento = "Cafecito";
        } else if (filtro.equals("Importantes")) {
            prioridad = "Importante";
        }

        Task<JSONObject> task = service.getAvisos(establecimiento, "Todos", prioridad);

        task.setOnSucceeded(event -> {
            try {
                JSONObject result = task.getValue();
                JSONArray avisosArray = result.getJSONArray("avisos");

                if (avisosArray.length() == 0) {
                    Label lblVacio = new Label("No hay avisos disponibles");
                    lblVacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 20;");
                    vboxAvisosContenido.getChildren().add(lblVacio);
                    return;
                }

                for (int i = 0; i < avisosArray.length(); i++) {
                    JSONObject avisoJson = avisosArray.getJSONObject(i);
                    VBox avisoCard = crearCardAvisoDesdeJSON(avisoJson);
                    vboxAvisosContenido.getChildren().add(avisoCard);
                }

            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error", "No se pudieron cargar los avisos: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            mostrarError("Error", "No se pudieron cargar los avisos: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void cargarProductosEspeciales() {
        vboxEspecialesContenido.getChildren().clear();

        NormalUserService service = NormalUserService.getInstance();
        Task<JSONObject> task = service.getProductosEspeciales();

        task.setOnSucceeded(event -> {
            try {
                JSONObject result = task.getValue();
                JSONArray especialesArray = result.getJSONArray("productos_especiales");

                if (especialesArray.length() == 0) {
                    Label lblVacio = new Label("No hay productos especiales disponibles en este momento");
                    lblVacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 20;");
                    vboxEspecialesContenido.getChildren().add(lblVacio);
                    return;
                }

                for (int i = 0; i < especialesArray.length(); i++) {
                    JSONObject especialJson = especialesArray.getJSONObject(i);
                    VBox especialCard = crearCardEspecialDesdeJSON(especialJson);
                    vboxEspecialesContenido.getChildren().add(especialCard);
                }

            } catch (Exception e) {
                e.printStackTrace();
                mostrarError("Error", "No se pudieron cargar los productos especiales: " + e.getMessage());
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            mostrarError("Error", "No se pudieron cargar los productos especiales: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private VBox crearCardAviso(Aviso aviso) {
        VBox card = new VBox(10);

        // Color según prioridad y establecimiento
        String colorBorde = aviso.getPrioridad() == Aviso.Prioridad.Importante ? "#dc3545"
                : aviso.getEstablecimiento() == Aviso.Establecimiento.Cafeteria ? "#007bff"
                        : aviso.getEstablecimiento() == Aviso.Establecimiento.Cafecito ? "#28a745" : "#6c757d";

        card.setStyle("-fx-background-color: white; -fx-border-color: " + colorBorde
                + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 15;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icono según establecimiento
        Label lblIcono = new Label();
        switch (aviso.getEstablecimiento()) {
            case Cafeteria:
                lblIcono.setText("🍽️");
                break;
            case Cafecito:
                lblIcono.setText("☕");
                break;
            default:
                lblIcono.setText("📢");
        }

        Label lblTitulo = new Label(aviso.getTitulo());
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;"); // CORREGIDO: texto
                                                                                                   // oscuro

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de prioridad
        Label lblPrioridad = new Label(aviso.getPrioridad() == Aviso.Prioridad.Importante ? "❗ IMPORTANTE" : "Normal");
        lblPrioridad.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50; -fx-background-color: " +
                (aviso.getPrioridad() == Aviso.Prioridad.Importante ? "#ffc107" : "#e9ecef") +
                "; -fx-padding: 2 8; -fx-background-radius: 10;");

        header.getChildren().addAll(lblIcono, lblTitulo, spacer, lblPrioridad);

        // Contenido
        Label lblContenido = new Label(aviso.getContenido());
        lblContenido.setWrapText(true);
        lblContenido.setStyle("-fx-text-fill: #495057;");

        // Footer con fechas
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        Label lblFechas = new Label(
                "📅 " + aviso.getFechaInicio().format(formatter) + " - " + aviso.getFechaFin().format(formatter));
        lblFechas.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        Label lblTipo = new Label("🎯 " + aviso.getTipoAviso().toString());
        lblTipo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        footer.getChildren().addAll(lblFechas, lblTipo);

        card.getChildren().addAll(header, lblContenido, footer);
        return card;
    }

    // ⭐ SECCIÓN DE PRODUCTOS ESPECIALES

    private VBox crearCardEspecial(ProductoEspecial especial) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: #fff3cd; -fx-border-color: #ffeaa7; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 15;");

        // Obtener información del producto
        Producto producto = allProductos.getById(especial.getIdProducto());
        if (producto == null)
            return card;

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblIcono = new Label("⭐");
        Label lblTitulo = new Label("OFERTA ESPECIAL");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #856404;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de descuento
        double descuento = ((producto.getPrecioBase() - especial.getPrecioEspecial()) / producto.getPrecioBase()) * 100;
        Label lblDescuento = new Label(String.format("-%.0f%%", descuento));
        lblDescuento.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #dc3545; -fx-padding: 3 8; -fx-background-radius: 10;");

        header.getChildren().addAll(lblIcono, lblTitulo, spacer, lblDescuento);

        // Información del producto - CORREGIDO: texto oscuro
        Label lblProducto = new Label("⭐" + producto.getNombre());
        lblProducto.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Precios
        HBox precios = new HBox(10);
        precios.setAlignment(Pos.CENTER_LEFT);

        Label lblPrecioOriginal = new Label(String.format("$%.2f", producto.getPrecioBase()));
        lblPrecioOriginal.setStyle("-fx-text-fill: #6c757d; -fx-strikethrough: true;");

        Label lblPrecioEspecial = new Label(String.format("$%.2f", especial.getPrecioEspecial()));
        lblPrecioEspecial.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 18px; -fx-font-weight: bold;");

        precios.getChildren().addAll(lblPrecioOriginal, lblPrecioEspecial);

        // Descripción del especial
        if (especial.getDescripcion() != null && !especial.getDescripcion().isEmpty()) {
            Label lblDescEspecial = new Label("💡 " + especial.getDescripcion());
            lblDescEspecial.setWrapText(true);
            lblDescEspecial.setStyle("-fx-text-fill: #856404; -fx-font-style: italic;");
            card.getChildren().add(lblDescEspecial);
        }

        // Descripción del producto
        if (producto.getDescripcion() != null && !producto.getDescripcion().isEmpty()) {
            Label lblDescProducto = new Label(producto.getDescripcion());
            lblDescProducto.setWrapText(true);
            lblDescProducto.setStyle("-fx-text-fill: #495057;");
            card.getChildren().add(lblDescProducto);
        }

        // Vigencia
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");
        Label lblVigencia = new Label("⏰ Válido hasta: " + especial.getFechaFin().format(formatter));
        lblVigencia.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        card.getChildren().addAll(header, lblProducto, precios, lblVigencia);
        return card;
    }

    // 🔧 MÉTODOS UTILITARIOS

    @FXML
    private void onCerrarSesionClicked() {
        session.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/sessions/Login.fxml"));
            Parent root = loader.load();

            // Obtener el Stage actual de manera segura
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();

            // RESTAURAR PRIMERO las propiedades del Stage
            stage.setResizable(true); // Primero hacer resizable para poder cambiar tamaño

            // Configurar tamaño específico del login
            stage.setWidth(600);
            stage.setHeight(500);

            // Configurar NO redimensionable DESPUÉS de establecer tamaño
            stage.setResizable(false);

            // Configurar la nueva escena
            stage.setScene(new Scene(root));
            stage.setTitle("CAFI - Inicio de Sesión");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    // DashboardUserController.java - Métodos adicionales que faltan

    private VBox crearCardSeccionDesdeJSON(JSONObject seccionJson) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 4; -fx-padding: 10;");

        // Obtener información de la sección
        String nombreSeccion = seccionJson.getString("nombre_seccion");
        String colorSeccion = seccionJson.optString("color_seccion", "#6c757d");
        String descripcionSeccion = seccionJson.optString("descripcion_seccion", "");

        // Determinar si el color de fondo es oscuro para ajustar el texto
        String colorTexto = esColorOscuro(colorSeccion) ? "white" : "#2c3e50";

        // Header de la sección con color
        Label lblSeccion = new Label("✔️ " + nombreSeccion);
        lblSeccion.setStyle(
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + colorTexto +
                        "; -fx-padding: 5 10; -fx-background-radius: 15;");
        lblSeccion.setStyle(lblSeccion.getStyle() + "-fx-background-color: " + colorSeccion + ";");

        card.getChildren().add(lblSeccion);

        // Descripción de la sección (si existe)
        if (!descripcionSeccion.isEmpty()) {
            Label lblDesc = new Label(descripcionSeccion);
            lblDesc.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px; -fx-font-style: italic;");
            lblDesc.setWrapText(true);
            card.getChildren().add(lblDesc);
        }

        // Productos de la sección
        if (seccionJson.has("productos")) {
            JSONArray productosArray = seccionJson.getJSONArray("productos");

            if (productosArray.length() == 0) {
                Label lblVacio = new Label("No hay productos en esta sección");
                lblVacio.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
                card.getChildren().add(lblVacio);
            } else {
                for (int i = 0; i < productosArray.length(); i++) {
                    JSONObject productoJson = productosArray.getJSONObject(i);
                    VBox productoCard = crearCardProductoDesdeJSON(productoJson);
                    card.getChildren().add(productoCard);
                }
            }
        } else {
            Label lblVacio = new Label("No hay productos en esta sección");
            lblVacio.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            card.getChildren().add(lblVacio);
        }

        return card;
    }

    private VBox crearCardProductoDesdeJSON(JSONObject productoJson) {
        VBox card = new VBox(6);
        card.setStyle(
                "-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 4; -fx-padding: 8;");

        // Obtener información del producto
        String nombre = productoJson.getString("Nombre");
        double precioBase = productoJson.getDouble("PrecioBase");
        double calorias = productoJson.optDouble("Calorias", 0);
        String descripcion = productoJson.optString("Descripcion", "");
        String categoria = productoJson.optString("categoria", "");
        boolean tieneOferta = productoJson.optBoolean("tiene_oferta", false);
        double precioEspecial = productoJson.optDouble("precio_especial", 0);

        // Información básica del producto
        HBox infoBasica = new HBox(10);
        infoBasica.setAlignment(Pos.CENTER_LEFT);

        Label lblNombre = new Label(nombre);
        lblNombre.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50;");

        // Mostrar precio especial si tiene oferta
        Label lblPrecio;
        if (tieneOferta && precioEspecial > 0) {
            lblPrecio = new Label(String.format("$%.2f", precioEspecial));
            lblPrecio.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

            // Mostrar precio original tachado
            Label lblPrecioOriginal = new Label(String.format("$%.2f", precioBase));
            lblPrecioOriginal.setStyle("-fx-text-fill: #6c757d; -fx-strikethrough: true; -fx-font-size: 10px;");

            HBox preciosBox = new HBox(5);
            preciosBox.setAlignment(Pos.CENTER_LEFT);
            preciosBox.getChildren().addAll(lblPrecioOriginal, lblPrecio);
            infoBasica.getChildren().addAll(lblNombre, preciosBox);
        } else {
            lblPrecio = new Label(String.format("$%.2f", precioBase));
            lblPrecio.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            infoBasica.getChildren().addAll(lblNombre, lblPrecio);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblCalorias = new Label(String.format("%.0f cal", calorias));
        lblCalorias.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        infoBasica.getChildren().addAll(spacer, lblCalorias);
        card.getChildren().add(infoBasica);

        // Descripción
        if (!descripcion.isEmpty()) {
            Label lblDesc = new Label(descripcion);
            lblDesc.setStyle("-fx-text-fill: #495057; -fx-font-size: 12px;");
            lblDesc.setWrapText(true);
            card.getChildren().add(lblDesc);
        }

        // Categoría
        if (!categoria.isEmpty()) {
            Label lblCategoria = new Label("📂 " + categoria);
            lblCategoria.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px; -fx-font-style: italic;");
            card.getChildren().add(lblCategoria);
        }

        // Tamaños disponibles
        if (productoJson.has("tamanos")) {
            JSONArray tamanosArray = productoJson.getJSONArray("tamanos");

            if (tamanosArray.length() > 0) {
                VBox tamanosBox = new VBox(4);
                Label lblTamanos = new Label("📏 Tamaños disponibles:");
                lblTamanos.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #495057;");
                tamanosBox.getChildren().add(lblTamanos);

                for (int i = 0; i < tamanosArray.length(); i++) {
                    JSONObject tamanoJson = tamanosArray.getJSONObject(i);
                    String nombreTamano = tamanoJson.getString("Nombre");
                    double precioTamano = tamanoJson.getDouble("Precio");
                    String descTamano = tamanoJson.optString("Descripcion", "");

                    String tamanoText = "  • " + nombreTamano + " - $" + String.format("%.2f", precioTamano);
                    if (!descTamano.isEmpty()) {
                        tamanoText += " (" + descTamano + ")";
                    }

                    Label lblTamano = new Label(tamanoText);
                    lblTamano.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                    tamanosBox.getChildren().add(lblTamano);
                }
                card.getChildren().add(tamanosBox);
            }
        }

        // Ingredientes
        if (productoJson.has("ingredientes")) {
            JSONArray ingredientesArray = productoJson.getJSONArray("ingredientes");

            if (ingredientesArray.length() > 0) {
                VBox ingredientesBox = new VBox(4);
                Label lblIngredientes = new Label("🥗 Ingredientes:");
                lblIngredientes.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #495057;");
                ingredientesBox.getChildren().add(lblIngredientes);

                for (int i = 0; i < ingredientesArray.length(); i++) {
                    JSONObject ingredienteJson = ingredientesArray.getJSONObject(i);
                    String nombreIngrediente = ingredienteJson.getString("nombre_ingrediente");
                    boolean eliminable = ingredienteJson.optBoolean("Eliminable", false);
                    boolean sustituible = ingredienteJson.optBoolean("Sustituible", false);
                    boolean alergeno = ingredienteJson.optBoolean("Alergeno", false);
                    String categoriaIngrediente = ingredienteJson.optString("categoria_ingrediente", "");

                    HBox ingRow = new HBox(5);
                    ingRow.setAlignment(Pos.CENTER_LEFT);

                    Label lblIng = new Label("• " + nombreIngrediente);
                    lblIng.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50;");

                    // Indicadores de opciones
                    HBox opciones = new HBox(5);
                    if (eliminable) {
                        Label lblEliminable = new Label("🚫");
                        lblEliminable.setTooltip(new Tooltip("Puede eliminarse"));
                        lblEliminable.setStyle("-fx-font-size: 10px;");
                        opciones.getChildren().add(lblEliminable);
                    }
                    if (sustituible) {
                        Label lblSustituible = new Label("🔄");
                        lblSustituible.setTooltip(new Tooltip("Puede sustituirse"));
                        lblSustituible.setStyle("-fx-font-size: 10px;");
                        opciones.getChildren().add(lblSustituible);
                    }
                    if (alergeno) {
                        Label lblAlergenico = new Label("⚠️");
                        lblAlergenico.setTooltip(new Tooltip("Alergénico"));
                        lblAlergenico.setStyle("-fx-font-size: 10px;");
                        opciones.getChildren().add(lblAlergenico);
                    }

                    Region spacerIng = new Region();
                    HBox.setHgrow(spacerIng, Priority.ALWAYS);

                    ingRow.getChildren().addAll(lblIng, spacerIng, opciones);
                    ingredientesBox.getChildren().add(ingRow);

                    // Categoría del ingrediente
                    if (!categoriaIngrediente.isEmpty()) {
                        Label lblCatIng = new Label("   ↳ Categoría: " + categoriaIngrediente);
                        lblCatIng.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
                        ingredientesBox.getChildren().add(lblCatIng);
                    }

                    // Mostrar sustitutos si los hay
                    if (sustituible && ingredienteJson.has("sustitutos")) {
                        JSONArray sustitutosArray = ingredienteJson.getJSONArray("sustitutos");

                        if (sustitutosArray.length() > 0) {
                            VBox sustitutosBox = new VBox(2);
                            sustitutosBox.setStyle("-fx-padding: 0 0 0 15;");

                            for (int j = 0; j < sustitutosArray.length(); j++) {
                                JSONObject sustitutoJson = sustitutosArray.getJSONObject(j);
                                String nombreSustituto = sustitutoJson.getString("nombre_sustituto");
                                double costoExtra = sustitutoJson.optDouble("CostoExtra", 0);
                                boolean disponible = sustitutoJson.optBoolean("Disponible", true);

                                if (disponible) {
                                    String sustText = "  ↳ " + nombreSustituto;
                                    if (costoExtra > 0) {
                                        sustText += String.format(" (+$%.2f)", costoExtra);
                                    }
                                    Label lblSust = new Label(sustText);
                                    lblSust.setStyle("-fx-font-size: 10px; -fx-text-fill: #6c757d;");
                                    sustitutosBox.getChildren().add(lblSust);
                                }
                            }

                            if (sustitutosBox.getChildren().size() > 0) {
                                ingredientesBox.getChildren().add(sustitutosBox);
                            }
                        }
                    }
                }
                card.getChildren().add(ingredientesBox);
            }
        }

        // Botón para ver detalles completos
        Button btnDetalles = new Button("Ver detalles completos");
        btnDetalles.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #007bff; -fx-border-color: #007bff; -fx-border-width: 1; -fx-font-size: 11px;");
        btnDetalles.setOnAction(e -> mostrarDetallesProductoDesdeJSON(productoJson));
        card.getChildren().add(btnDetalles);

        return card;
    }

    private void mostrarDetallesProductoDesdeJSON(JSONObject productoJson) {
        String nombre = productoJson.getString("Nombre");
        double precioBase = productoJson.getDouble("PrecioBase");
        double calorias = productoJson.optDouble("Calorias", 0);
        double gramaje = productoJson.optDouble("Gramaje", 0);
        String descripcion = productoJson.optString("Descripcion", "");
        String categoria = productoJson.optString("categoria", "");
        boolean tieneOferta = productoJson.optBoolean("tiene_oferta", false);
        double precioEspecial = productoJson.optDouble("precio_especial", 0);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("🍽️ " + nombre);
        dialog.setHeaderText("Información completa del producto");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Descripción
        if (!descripcion.isEmpty()) {
            Label lblDesc = new Label("📝 " + descripcion);
            lblDesc.setWrapText(true);
            lblDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #495057;");
            content.getChildren().add(lblDesc);
        }

        // Categoría
        if (!categoria.isEmpty()) {
            Label lblCat = new Label("📂 Categoría: " + categoria);
            lblCat.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
            content.getChildren().add(lblCat);
        }

        // Precio
        HBox precioBox = new HBox(5);
        precioBox.setAlignment(Pos.CENTER_LEFT);

        if (tieneOferta && precioEspecial > 0) {
            Label lblPrecioOriginal = new Label("💰 Precio original: $" + String.format("%.2f", precioBase));
            lblPrecioOriginal.setStyle("-fx-text-fill: #6c757d; -fx-strikethrough: true;");

            Label lblPrecioEspecial = new Label("💰 Precio especial: $" + String.format("%.2f", precioEspecial));
            lblPrecioEspecial.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");

            double descuento = ((precioBase - precioEspecial) / precioBase) * 100;
            Label lblDescuento = new Label(String.format("(%.0f%% de descuento)", descuento));
            lblDescuento.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");

            precioBox.getChildren().addAll(lblPrecioOriginal, lblPrecioEspecial, lblDescuento);
        } else {
            Label lblPrecio = new Label("💰 Precio: $" + String.format("%.2f", precioBase));
            lblPrecio.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            precioBox.getChildren().add(lblPrecio);
        }

        content.getChildren().add(precioBox);

        // Información nutricional
        if (calorias > 0) {
            Label lblCalorias = new Label("🔥 Calorías: " + String.format("%.0f", calorias));
            lblCalorias.setStyle("-fx-text-fill: #495057;");
            content.getChildren().add(lblCalorias);
        }

        if (gramaje > 0) {
            Label lblGramaje = new Label("⚖️ Gramaje: " + String.format("%.0fg", gramaje));
            lblGramaje.setStyle("-fx-text-fill: #495057;");
            content.getChildren().add(lblGramaje);
        }

        // Tamaños disponibles
        if (productoJson.has("tamanos")) {
            JSONArray tamanosArray = productoJson.getJSONArray("tamanos");

            if (tamanosArray.length() > 0) {
                VBox tamanosBox = new VBox(5);
                Label lblTituloTamanos = new Label("📏 Tamaños disponibles:");
                lblTituloTamanos.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
                tamanosBox.getChildren().add(lblTituloTamanos);

                for (int i = 0; i < tamanosArray.length(); i++) {
                    JSONObject tamanoJson = tamanosArray.getJSONObject(i);
                    String nombreTamano = tamanoJson.getString("Nombre");
                    double precioTamano = tamanoJson.getDouble("Precio");
                    String descTamano = tamanoJson.optString("Descripcion", "");
                    double capacidad = tamanoJson.optDouble("Capacidad", 0);
                    double gramajeTamano = tamanoJson.optDouble("Gramaje", 0);
                    int piezas = tamanoJson.optInt("Piezas", 0);

                    String tamanoInfo = "  • " + nombreTamano + " - $" + String.format("%.2f", precioTamano);

                    if (!descTamano.isEmpty()) {
                        tamanoInfo += " (" + descTamano + ")";
                    }
                    if (capacidad > 0) {
                        tamanoInfo += String.format(" [%.0f ml]", capacidad);
                    }
                    if (gramajeTamano > 0) {
                        tamanoInfo += String.format(" [%.0f g]", gramajeTamano);
                    }
                    if (piezas > 0) {
                        tamanoInfo += String.format(" [%d piezas]", piezas);
                    }

                    Label lblTamano = new Label(tamanoInfo);
                    lblTamano.setStyle("-fx-text-fill: #6c757d;");
                    tamanosBox.getChildren().add(lblTamano);
                }
                content.getChildren().add(tamanosBox);
            }
        }

        // Ingredientes
        if (productoJson.has("ingredientes")) {
            JSONArray ingredientesArray = productoJson.getJSONArray("ingredientes");

            if (ingredientesArray.length() > 0) {
                VBox ingredientesBox = new VBox(5);
                Label lblTituloIngredientes = new Label("🥗 Ingredientes:");
                lblTituloIngredientes.setStyle("-fx-font-weight: bold; -fx-text-fill: #495057;");
                ingredientesBox.getChildren().add(lblTituloIngredientes);

                int alergenosCount = 0;
                int eliminablesCount = 0;
                int sustituiblesCount = 0;

                for (int i = 0; i < ingredientesArray.length(); i++) {
                    JSONObject ingredienteJson = ingredientesArray.getJSONObject(i);
                    String nombreIngrediente = ingredienteJson.getString("nombre_ingrediente");
                    boolean eliminable = ingredienteJson.optBoolean("Eliminable", false);
                    boolean sustituible = ingredienteJson.optBoolean("Sustituible", false);
                    boolean alergeno = ingredienteJson.optBoolean("Alergeno", false);
                    String categoriaIngrediente = ingredienteJson.optString("categoria_ingrediente", "");
                    double cantidad = ingredienteJson.optDouble("Cantidad", 0);

                    String ingredienteInfo = "  • " + nombreIngrediente;

                    if (cantidad > 0) {
                        ingredienteInfo += String.format(" (%.0f g/ml)", cantidad);
                    }

                    if (alergeno) {
                        ingredienteInfo += " ⚠️";
                        alergenosCount++;
                    }
                    if (eliminable) {
                        ingredienteInfo += " 🚫";
                        eliminablesCount++;
                    }
                    if (sustituible) {
                        ingredienteInfo += " 🔄";
                        sustituiblesCount++;
                    }

                    if (!categoriaIngrediente.isEmpty()) {
                        ingredienteInfo += " [" + categoriaIngrediente + "]";
                    }

                    Label lblIngrediente = new Label(ingredienteInfo);
                    lblIngrediente.setStyle("-fx-text-fill: #495057;");
                    ingredientesBox.getChildren().add(lblIngrediente);

                    // Mostrar sustitutos si los hay
                    if (sustituible && ingredienteJson.has("sustitutos")) {
                        JSONArray sustitutosArray = ingredienteJson.getJSONArray("sustitutos");

                        if (sustitutosArray.length() > 0) {
                            for (int j = 0; j < sustitutosArray.length(); j++) {
                                JSONObject sustitutoJson = sustitutosArray.getJSONObject(j);
                                String nombreSustituto = sustitutoJson.getString("nombre_sustituto");
                                double costoExtra = sustitutoJson.optDouble("CostoExtra", 0);

                                String sustInfo = "    ↳ " + nombreSustituto;
                                if (costoExtra > 0) {
                                    sustInfo += String.format(" (+$%.2f)", costoExtra);
                                }

                                Label lblSustituto = new Label(sustInfo);
                                lblSustituto.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                                ingredientesBox.getChildren().add(lblSustituto);
                            }
                        }
                    }
                }

                // Resumen de características
                if (alergenosCount > 0 || eliminablesCount > 0 || sustituiblesCount > 0) {
                    HBox resumenBox = new HBox(10);
                    resumenBox.setStyle("-fx-padding: 5 0 0 0;");

                    if (alergenosCount > 0) {
                        Label lblAlergenos = new Label("⚠️ " + alergenosCount + " alergeno(s)");
                        lblAlergenos.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px;");
                        resumenBox.getChildren().add(lblAlergenos);
                    }

                    if (eliminablesCount > 0) {
                        Label lblEliminables = new Label("🚫 " + eliminablesCount + " eliminable(s)");
                        lblEliminables.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
                        resumenBox.getChildren().add(lblEliminables);
                    }

                    if (sustituiblesCount > 0) {
                        Label lblSustituibles = new Label("🔄 " + sustituiblesCount + " sustituible(s)");
                        lblSustituibles.setStyle("-fx-text-fill: #007bff; -fx-font-size: 11px;");
                        resumenBox.getChildren().add(lblSustituibles);
                    }

                    ingredientesBox.getChildren().add(resumenBox);
                }

                content.getChildren().add(ingredientesBox);
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(400);
        dialog.showAndWait();
    }

    private VBox crearCardAvisoDesdeJSON(JSONObject avisoJson) {
        VBox card = new VBox(10);

        // Obtener información del aviso
        String titulo = avisoJson.getString("Titulo");
        String contenido = avisoJson.getString("Contenido");
        String establecimiento = avisoJson.getString("Establecimiento");
        String tipoAviso = avisoJson.getString("TipoAviso");
        String prioridad = avisoJson.getString("Prioridad");
        String fechaPublicacion = avisoJson.optString("FechaPublicacion", "");
        String fechaInicio = avisoJson.optString("FechaInicio", "");
        String fechaFin = avisoJson.optString("FechaFin", "");
        String creadorNombre = avisoJson.optString("creador_nombre", "");
        String creadorApellido = avisoJson.optString("creador_apellido", "");
        String colorBorde = avisoJson.optString("color_borde", "#6c757d");
        String icono = avisoJson.optString("icono", "📢");

        // Color según prioridad y establecimiento
        if (prioridad.equals("Importante")) {
            colorBorde = "#dc3545";
        } else if (establecimiento.equals("Cafeteria")) {
            colorBorde = "#007bff";
        } else if (establecimiento.equals("Cafecito")) {
            colorBorde = "#28a745";
        }

        card.setStyle("-fx-background-color: white; -fx-border-color: " + colorBorde
                + "; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 15;");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblIcono = new Label(icono);

        Label lblTitulo = new Label(titulo);
        lblTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de prioridad
        Label lblPrioridad = new Label(prioridad.equals("Importante") ? "❗ IMPORTANTE" : "Normal");
        lblPrioridad.setStyle("-fx-font-size: 11px; -fx-text-fill: #2c3e50; -fx-background-color: " +
                (prioridad.equals("Importante") ? "#ffc107" : "#e9ecef") +
                "; -fx-padding: 2 8; -fx-background-radius: 10;");

        header.getChildren().addAll(lblIcono, lblTitulo, spacer, lblPrioridad);

        // Contenido
        Label lblContenido = new Label(contenido);
        lblContenido.setWrapText(true);
        lblContenido.setStyle("-fx-text-fill: #495057;");

        // Información adicional
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-padding: 10 0 0 0;");

        // Establecimiento y tipo
        HBox tipoBox = new HBox(10);
        tipoBox.setAlignment(Pos.CENTER_LEFT);

        Label lblEstablecimiento = new Label("🏢 " + getEstablecimientoDisplayName(establecimiento));
        lblEstablecimiento.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        Label lblTipo = new Label("🎯 " + getTipoAvisoDisplayName(tipoAviso));
        lblTipo.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");

        tipoBox.getChildren().addAll(lblEstablecimiento, lblTipo);
        infoBox.getChildren().add(tipoBox);

        // Fechas
        if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            Label lblFechas = new Label("📅 " + fechaInicio + " - " + fechaFin);
            lblFechas.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
            infoBox.getChildren().add(lblFechas);
        }

        // Creador (si existe)
        if (!creadorNombre.isEmpty()) {
            String creadorCompleto = creadorNombre;
            if (!creadorApellido.isEmpty()) {
                creadorCompleto += " " + creadorApellido;
            }
            Label lblCreador = new Label("👤 Publicado por: " + creadorCompleto);
            lblCreador.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px; -fx-font-style: italic;");
            infoBox.getChildren().add(lblCreador);
        }

        // Fecha de publicación
        if (!fechaPublicacion.isEmpty()) {
            Label lblPublicacion = new Label("📝 Publicado: " + fechaPublicacion);
            lblPublicacion.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 10px; -fx-font-style: italic;");
            infoBox.getChildren().add(lblPublicacion);
        }

        card.getChildren().addAll(header, lblContenido, infoBox);
        return card;
    }

    private VBox crearCardEspecialDesdeJSON(JSONObject especialJson) {
        VBox card = new VBox(12);
        card.setStyle(
                "-fx-background-color: #fff3cd; -fx-border-color: #ffeaa7; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 15;");

        // Obtener información del producto especial
        String nombreProducto = especialJson.getString("nombre_producto");
        String descripcionProducto = especialJson.optString("descripcion_producto", "");
        String descripcionEspecial = especialJson.optString("descripcion_especial", "");
        double precioBase = especialJson.getDouble("PrecioBase");
        double precioEspecial = especialJson.getDouble("PrecioEspecial");
        double calorias = especialJson.optDouble("Calorias", 0);
        double gramaje = especialJson.optDouble("Gramaje", 0);
        String categoria = especialJson.optString("categoria", "");
        String fechaInicio = especialJson.optString("FechaInicio", "");
        String fechaFin = especialJson.optString("FechaFin", "");
        int porcentajeDescuento = especialJson.optInt("porcentaje_descuento", 0);
        String fotoURL = especialJson.optString("URLFoto", "");

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblIcono = new Label("⭐");
        Label lblTitulo = new Label("OFERTA ESPECIAL");
        lblTitulo.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #856404;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de descuento
        if (porcentajeDescuento > 0) {
            Label lblDescuento = new Label(String.format("-%.0f%%", (double) porcentajeDescuento));
            lblDescuento.setStyle(
                    "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #dc3545; -fx-padding: 3 8; -fx-background-radius: 10;");
            header.getChildren().addAll(lblIcono, lblTitulo, spacer, lblDescuento);
        } else {
            header.getChildren().addAll(lblIcono, lblTitulo, spacer);
        }

        // Información del producto
        Label lblProducto = new Label("⭐ " + nombreProducto);
        lblProducto.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");

        // Precios
        HBox precios = new HBox(10);
        precios.setAlignment(Pos.CENTER_LEFT);

        Label lblPrecioOriginal = new Label(String.format("$%.2f", precioBase));
        lblPrecioOriginal.setStyle("-fx-text-fill: #6c757d; -fx-strikethrough: true;");

        Label lblPrecioEspecial = new Label(String.format("$%.2f", precioEspecial));
        lblPrecioEspecial.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 18px; -fx-font-weight: bold;");

        precios.getChildren().addAll(lblPrecioOriginal, lblPrecioEspecial);

        // Descripción del especial
        if (!descripcionEspecial.isEmpty()) {
            Label lblDescEspecial = new Label("💡 " + descripcionEspecial);
            lblDescEspecial.setWrapText(true);
            lblDescEspecial.setStyle("-fx-text-fill: #856404; -fx-font-style: italic; -fx-font-size: 12px;");
            card.getChildren().add(lblDescEspecial);
        }

        // Descripción del producto
        if (!descripcionProducto.isEmpty()) {
            Label lblDescProducto = new Label(descripcionProducto);
            lblDescProducto.setWrapText(true);
            lblDescProducto.setStyle("-fx-text-fill: #495057; -fx-font-size: 12px;");
            card.getChildren().add(lblDescProducto);
        }

        // Información adicional
        VBox infoBox = new VBox(5);
        infoBox.setStyle("-fx-padding: 5 0 0 0;");

        // Categoría
        if (!categoria.isEmpty()) {
            Label lblCategoria = new Label("📂 " + categoria);
            lblCategoria.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
            infoBox.getChildren().add(lblCategoria);
        }

        // Información nutricional
        if (calorias > 0) {
            Label lblCalorias = new Label("🔥 " + String.format("%.0f cal", calorias));
            lblCalorias.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
            infoBox.getChildren().add(lblCalorias);
        }

        if (gramaje > 0) {
            Label lblGramaje = new Label("⚖️ " + String.format("%.0f g", gramaje));
            lblGramaje.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px;");
            infoBox.getChildren().add(lblGramaje);
        }

        // Vigencia
        if (!fechaFin.isEmpty()) {
            Label lblVigencia = new Label("⏰ Válido hasta: " + fechaFin);
            lblVigencia.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 11px; -fx-font-weight: bold;");
            infoBox.getChildren().add(lblVigencia);
        }

        // Botón para ver más detalles
        Button btnDetalles = new Button("Ver detalles del producto");
        btnDetalles.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #007bff; -fx-border-color: #007bff; -fx-border-width: 1; -fx-font-size: 11px;");
        btnDetalles.setOnAction(e -> mostrarDetallesProductoEspecial(especialJson));

        card.getChildren().addAll(header, lblProducto, precios);

        if (infoBox.getChildren().size() > 0) {
            card.getChildren().add(infoBox);
        }

        card.getChildren().add(btnDetalles);

        return card;
    }

    private void mostrarDetallesProductoEspecial(JSONObject especialJson) {
        String nombreProducto = especialJson.getString("nombre_producto");
        String descripcionProducto = especialJson.optString("descripcion_producto", "");
        String descripcionEspecial = especialJson.optString("descripcion_especial", "");
        double precioBase = especialJson.getDouble("PrecioBase");
        double precioEspecial = especialJson.getDouble("PrecioEspecial");
        double calorias = especialJson.optDouble("Calorias", 0);
        double gramaje = especialJson.optDouble("Gramaje", 0);
        String categoria = especialJson.optString("categoria", "");
        String fechaInicio = especialJson.optString("FechaInicio", "");
        String fechaFin = especialJson.optString("FechaFin", "");
        int porcentajeDescuento = especialJson.optInt("porcentaje_descuento", 0);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("⭐ " + nombreProducto);
        dialog.setHeaderText("Detalles de la oferta especial");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // Descripción del especial
        if (!descripcionEspecial.isEmpty()) {
            Label lblDescEspecial = new Label("💡 " + descripcionEspecial);
            lblDescEspecial.setWrapText(true);
            lblDescEspecial.setStyle("-fx-text-fill: #856404; -fx-font-style: italic; -fx-font-size: 13px;");
            content.getChildren().add(lblDescEspecial);
        }

        // Descripción del producto
        if (!descripcionProducto.isEmpty()) {
            Label lblDescProducto = new Label("📝 " + descripcionProducto);
            lblDescProducto.setWrapText(true);
            lblDescProducto.setStyle("-fx-text-fill: #495057; -fx-font-size: 12px;");
            content.getChildren().add(lblDescProducto);
        }

        // Información de precios
        VBox preciosBox = new VBox(5);

        Label lblPrecioOriginal = new Label("💰 Precio original: $" + String.format("%.2f", precioBase));
        lblPrecioOriginal.setStyle("-fx-text-fill: #6c757d; -fx-strikethrough: true;");

        Label lblPrecioEspecial = new Label("💰 Precio especial: $" + String.format("%.2f", precioEspecial));
        lblPrecioEspecial.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold; -fx-font-size: 14px;");

        if (porcentajeDescuento > 0) {
            Label lblDescuento = new Label(String.format("🎉 %.0f%% DE DESCUENTO", (double) porcentajeDescuento));
            lblDescuento.setStyle("-fx-text-fill: #28a745; -fx-font-weight: bold;");
            preciosBox.getChildren().addAll(lblPrecioOriginal, lblPrecioEspecial, lblDescuento);
        } else {
            preciosBox.getChildren().addAll(lblPrecioOriginal, lblPrecioEspecial);
        }

        content.getChildren().add(preciosBox);

        // Información adicional
        VBox infoBox = new VBox(5);

        // Categoría
        if (!categoria.isEmpty()) {
            Label lblCategoria = new Label("📂 Categoría: " + categoria);
            lblCategoria.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
            infoBox.getChildren().add(lblCategoria);
        }

        // Información nutricional
        if (calorias > 0) {
            Label lblCalorias = new Label("🔥 Calorías: " + String.format("%.0f", calorias));
            lblCalorias.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
            infoBox.getChildren().add(lblCalorias);
        }

        if (gramaje > 0) {
            Label lblGramaje = new Label("⚖️ Gramaje: " + String.format("%.0f g", gramaje));
            lblGramaje.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
            infoBox.getChildren().add(lblGramaje);
        }

        // Vigencia
        if (!fechaInicio.isEmpty() && !fechaFin.isEmpty()) {
            Label lblVigencia = new Label("⏰ Vigencia: " + fechaInicio + " - " + fechaFin);
            lblVigencia.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px; -fx-font-weight: bold;");
            infoBox.getChildren().add(lblVigencia);

            // Calcular días restantes
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                LocalDateTime fin = LocalDateTime.parse(fechaFin, formatter);
                LocalDateTime ahora = LocalDateTime.now();

                if (ahora.isBefore(fin)) {
                    long diasRestantes = java.time.Duration.between(ahora, fin).toDays();
                    Label lblDiasRestantes = new Label("⏳ " + diasRestantes + " días restantes");
                    lblDiasRestantes.setStyle("-fx-text-fill: #dc3545; -fx-font-size: 11px; -fx-font-weight: bold;");
                    infoBox.getChildren().add(lblDiasRestantes);
                }
            } catch (Exception e) {
                // Ignorar error de formato
            }
        }

        if (infoBox.getChildren().size() > 0) {
            content.getChildren().add(infoBox);
        }

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setMinWidth(400);
        dialog.showAndWait();
    }

    // Métodos auxiliares para formatear texto
    private String getEstablecimientoDisplayName(String establecimiento) {
        switch (establecimiento) {
            case "Cafeteria":
                return "Cafetería";
            case "Cafecito":
                return "Cafecito";
            case "Ambos":
                return "Ambos establecimientos";
            default:
                return establecimiento;
        }
    }

    private String getTipoAvisoDisplayName(String tipoAviso) {
        switch (tipoAviso) {
            case "General":
                return "General";
            case "Horario":
                return "Cambio de horario";
            case "NoLaboral":
                return "Día no laboral";
            case "Oferta":
                return "Oferta especial";
            case "Evento":
                return "Evento";
            default:
                return tipoAviso;
        }
    }
}