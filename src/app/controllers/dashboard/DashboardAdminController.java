package app.controllers.dashboard;

import core.SessionManager;
import core.data.Users.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.net.URL;

public class DashboardAdminController {

    @FXML
    private Label lblWelcome;
    @FXML
    private AnchorPane contentArea;
    @FXML
    private Button btnProductos;
    @FXML
    private Button btnMenus;
    @FXML
    private Button btnBebidas;
    @FXML
    private Button btnIngredientes;
    @FXML
    private Button btnAvisos;
    @FXML
    private Button btnAdministradores;
    @FXML
    private Button btnEstadisticas;
    @FXML
    private Button btnCerrarSesion;

    // Variables para controlar el tamaño
    private Stage currentStage;
    private String currentView = "";
    private final SessionManager session = SessionManager.getInstance();

    @FXML
    public void initialize() {
        try {
            User current = session.getCurrentUser();

            if (current != null) {
                lblWelcome.setText("👋 Bienvenido, " + current.getName() + " " + current.getApellidoPaterno());
            } else {
                lblWelcome.setText("👋 Bienvenido, Administrador");
            }

            // Obtener referencia al Stage actual DESPUÉS de que la escena esté lista
            Platform.runLater(() -> {
                currentStage = (Stage) contentArea.getScene().getWindow();
                System.out.println("Stage obtenido: " + currentStage.getTitle());

                // Vista por defecto al abrir el panel
                cargarVista("Menus", "/app/views/menus/Menus.fxml");
            });

        } catch (Exception e) {
            e.printStackTrace();
            lblWelcome.setText("⚠️ Error al cargar sesión.");
        }
    }

    // --- Navegación entre secciones ---
    @FXML
    private void onProductosClicked() {
        cargarVista("Productos", "/app/views/productos/Productos.fxml");
    }

    @FXML
    private void onMenusClicked() {
        cargarVista("Menus", "/app/views/menus/Menus.fxml");
    }

    @FXML
    private void onBebidasClicked() {
        cargarVista("Productos Especiales", "/app/views/productos/productosEspeciales.fxml");
    }

    @FXML
    private void onIngredientesClicked() {
        cargarVista("Ingredientes", "/app/views/ingredientes/Ingredientes.fxml");
    }

    @FXML
    private void onAvisosClicked() {
        cargarVista("Avisos", "/app/views/avisos/Avisos.fxml");
    }

    @FXML
    private void onAdministradoresClicked() {
        cargarVista("Administradores", "/app/views/administradores/Administradores.fxml");
    }

    @FXML
    private void onEstadisticasClicked() {
        cargarVista("Estadísticas", "/app/views/estadisticas/EstadisticasController.fxml");
    }

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

    // --- Método para cargar las vistas dinámicamente con validación ---
    private void cargarVista(String nombreVista, String rutaFxml) {
        try {
            // Verificar que el recurso existe
            URL resource = getClass().getResource(rutaFxml);

            if (resource == null) {
                System.err.println("No se encontró la pantalla de " + nombreVista + " en: " + rutaFxml);
                mostrarVistaNoDisponible(nombreVista, rutaFxml);
                return;
            }

            // Cargar la vista
            FXMLLoader loader = new FXMLLoader(resource);
            Parent vista = loader.load();

            // Limpiar el área de contenido y agregar la nueva vista
            contentArea.getChildren().clear();
            contentArea.getChildren().add(vista);

            // Configurar anclajes para que ocupe todo el espacio
            AnchorPane.setTopAnchor(vista, 0.0);
            AnchorPane.setBottomAnchor(vista, 0.0);
            AnchorPane.setLeftAnchor(vista, 0.0);
            AnchorPane.setRightAnchor(vista, 0.0);

            // Asegurarse de que tenemos el Stage
            if (currentStage == null) {
                currentStage = (Stage) contentArea.getScene().getWindow();
            }

            // Actualizar el título de la ventana
            currentStage.setTitle("CAFI - " + nombreVista);

            // Controlar si la ventana es resizable o no
            controlarResizable(nombreVista);

            // Guardar la vista actual
            currentView = nombreVista;

            System.out.println("✅ Vista cargada: " + nombreVista + " - Resizable: " + currentStage.isResizable());

        } catch (Exception e) {
            e.printStackTrace();
            mostrarVistaError(nombreVista, e.getMessage());
        }
    }

    // --- Método para controlar si la ventana es resizable ---
    private void controlarResizable(String nombreVista) {
        if (currentStage == null) {
            return;
        }

        // Solo Estadísticas es resizable
        if (nombreVista.equalsIgnoreCase("Estadísticas")) {
            // Hacer la ventana resizable
            currentStage.setResizable(true);

            // Configurar tamaño mínimo para estadísticas
            currentStage.setMinWidth(1200);
            currentStage.setMinHeight(800);

            // Ajustar tamaño actual si es muy pequeño
            if (currentStage.getWidth() < 1200) {
                currentStage.setWidth(1200);
            }
            if (currentStage.getHeight() < 800) {
                currentStage.setHeight(800);
            }

            // Centrar la ventana
            currentStage.centerOnScreen();

        } else {
            // Para todas las demás vistas, hacer NO resizable
            currentStage.setResizable(false);

            // Restaurar tamaño estándar (ajustar según tu preferencia)
            if (!currentStage.isMaximized()) {
                // Tamaño estándar para otras vistas
                currentStage.setWidth(1100);
                currentStage.setHeight(700);
                currentStage.centerOnScreen();
            }
        }
    }

    // --- Método para restaurar tamaño estándar cuando se sale de Estadísticas ---
    public void restaurarTamanoEstandar() {
        if (currentStage != null && !currentView.equalsIgnoreCase("Estadísticas")) {
            currentStage.setResizable(false);
            currentStage.centerOnScreen();
            if (!currentStage.isMaximized()) {
                currentStage.centerOnScreen();
                currentStage.setWidth(1100);
                currentStage.setHeight(700);
                currentStage.centerOnScreen();
            }
        }
    }

    // Muestra un mensaje cuando la vista no está disponible
    private void mostrarVistaNoDisponible(String nombreVista, String ruta) {
        Label mensaje = new Label("🚧 Vista no disponible\n\n" +
                "La sección \"" + nombreVista + "\" aún no está implementada.\n\n" +
                "Ruta esperada: " + ruta);
        mensaje.setStyle("-fx-font-size: 16px; -fx-text-fill: #666; -fx-padding: 40; " +
                "-fx-alignment: center; -fx-text-alignment: center;");
        mensaje.setMaxWidth(Double.MAX_VALUE);
        mensaje.setMaxHeight(Double.MAX_VALUE);

        contentArea.getChildren().setAll(mensaje);

        AnchorPane.setTopAnchor(mensaje, 0.0);
        AnchorPane.setBottomAnchor(mensaje, 0.0);
        AnchorPane.setLeftAnchor(mensaje, 0.0);
        AnchorPane.setRightAnchor(mensaje, 0.0);
    }

    // Muestra un mensaje de error
    private void mostrarVistaError(String nombreVista, String error) {
        Label mensaje = new Label("❌ Error al cargar vista\n\n" +
                "Sección: " + nombreVista + "\n\n" +
                "Error: " + error);
        mensaje.setStyle("-fx-font-size: 16px; -fx-text-fill: #d32f2f; -fx-padding: 40; " +
                "-fx-alignment: center; -fx-text-alignment: center;");
        mensaje.setMaxWidth(Double.MAX_VALUE);
        mensaje.setMaxHeight(Double.MAX_VALUE);

        contentArea.getChildren().setAll(mensaje);

        AnchorPane.setTopAnchor(mensaje, 0.0);
        AnchorPane.setBottomAnchor(mensaje, 0.0);
        AnchorPane.setLeftAnchor(mensaje, 0.0);
        AnchorPane.setRightAnchor(mensaje, 0.0);
    }
}