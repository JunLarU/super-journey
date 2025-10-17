// DashboardUserController.java
package app.controllers.dashboard;

import core.data.Ingredientes.AllIngredientes;
import core.data.Ingredientes.Ingrediente;
import core.SessionManager;
import core.data.Users.User;
import core.data.Menus.*;
import core.data.Menus.Menu;
import core.data.Productos.*;
import core.data.Avisos.*;
import core.data.Ingredientes.*;
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
import java.util.stream.Collectors;

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

    // =========================
    // 📅 SECCIÓN DE MENÚ SEMANAL
    // =========================

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

    private void cargarMenuSemanal() {
        vboxMenuContenido.getChildren().clear();
        lblSemanaActual.setText("📅 Semana " + semanaActual + " del " + anioActual);

        try {
            List<Menu> menusSemana = allMenus.getMenusBySemana(semanaActual, anioActual);
            String horarioFiltro = comboHorario.getValue();

            if (menusSemana.isEmpty()) {
                Label lblVacio = new Label("No hay menú disponible para esta semana");
                lblVacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 20;");
                vboxMenuContenido.getChildren().add(lblVacio);
                return;
            }

            // Agrupar por día y ORDENAR DE LUNES A VIERNES
            Map<LocalDate, List<Menu>> menusPorDia = menusSemana.stream()
                    .filter(menu -> horarioFiltro.equals("Todos") || menu.getHorario().equalsIgnoreCase(horarioFiltro))
                    .collect(Collectors.groupingBy(Menu::getFecha));

            // Ordenar los días de lunes a viernes
            List<LocalDate> diasOrdenados = menusPorDia.keySet().stream()
                    .sorted()
                    .collect(Collectors.toList());

            // Mostrar en orden
            diasOrdenados.forEach(fecha -> {
                List<Menu> menusDia = menusPorDia.get(fecha);
                VBox diaCard = crearCardDia(fecha, menusDia);
                vboxMenuContenido.getChildren().add(diaCard);
            });

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo cargar el menú: " + e.getMessage());
        }
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
        Label lblHorario = new Label(menu.getHorario().equalsIgnoreCase("Desayuno") ? "🥚DESAYUNO" :"🍕COMIDA");
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

    // =========================
    // 📢 SECCIÓN DE AVISOS
    // =========================

    private void cargarAvisos() {
        vboxAvisosContenido.getChildren().clear();

        try {
            List<Aviso> avisos = allAvisos.getAvisosVigentes();
            String filtro = comboFiltroAvisos.getValue();

            // Aplicar filtros
            List<Aviso> avisosFiltrados = avisos.stream()
                    .filter(aviso -> {
                        switch (filtro) {
                            case "Cafetería":
                                return aviso.getEstablecimiento() == Aviso.Establecimiento.Cafeteria;
                            case "Cafecito":
                                return aviso.getEstablecimiento() == Aviso.Establecimiento.Cafecito;
                            case "Importantes":
                                return aviso.getPrioridad() == Aviso.Prioridad.Importante;
                            default:
                                return true;
                        }
                    })
                    .collect(Collectors.toList());

            if (avisosFiltrados.isEmpty()) {
                Label lblVacio = new Label("No hay avisos disponibles");
                lblVacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 20;");
                vboxAvisosContenido.getChildren().add(lblVacio);
                return;
            }

            avisosFiltrados.forEach(aviso -> {
                VBox avisoCard = crearCardAviso(aviso);
                vboxAvisosContenido.getChildren().add(avisoCard);
            });

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudieron cargar los avisos: " + e.getMessage());
        }
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

    // =========================
    // ⭐ SECCIÓN DE PRODUCTOS ESPECIALES
    // =========================

    private void cargarProductosEspeciales() {
        vboxEspecialesContenido.getChildren().clear();

        try {
            List<ProductoEspecial> especiales = allEspeciales.getEspecialesVigentes();

            if (especiales.isEmpty()) {
                Label lblVacio = new Label("No hay productos especiales disponibles en este momento");
                lblVacio.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic; -fx-padding: 20;");
                vboxEspecialesContenido.getChildren().add(lblVacio);
                return;
            }

            especiales.forEach(especial -> {
                VBox especialCard = crearCardEspecial(especial);
                vboxEspecialesContenido.getChildren().add(especialCard);
            });

        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudieron cargar los productos especiales: " + e.getMessage());
        }
    }

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

    // =========================
    // 🔧 MÉTODOS UTILITARIOS
    // =========================

    @FXML
    private void onCerrarSesionClicked() {
        session.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/sessions/Login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("CAFI - Inicio de Sesión");
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
}