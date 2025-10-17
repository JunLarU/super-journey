package app.controllers.dashboard;

import core.SessionManager;
import core.data.Users.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

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
    private Button btnCerrarSesion;

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

            // Vista por defecto al abrir el panel - comenzar con ingredientes si productos
            // no existe
            cargarVista("Menus", "/app/views/menus/Menus.fxml");

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

    // --- Método para cargar las vistas dinámicamente con validación ---
    private void cargarVista(String nombreVista, String rutaFxml) {
        try {
            // Verificar que el recurso existe
            URL resource = getClass().getResource(rutaFxml);

            if (resource == null) {
                // System.err.println("No se encontró la pantalla de " + nombreVista);
                // mostrarVistaNoDisponible(nombreVista, rutaFxml);
                return;
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent vista = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(vista);

            AnchorPane.setTopAnchor(vista, 0.0);
            AnchorPane.setBottomAnchor(vista, 0.0);
            AnchorPane.setLeftAnchor(vista, 0.0);
            AnchorPane.setRightAnchor(vista, 0.0);

            // System.out.println("✅ Vista cargada: " + nombreVista);

            try {
                // Obtener el stage de manera segura
                Stage stage = (Stage) contentArea.getScene().getWindow();
                if (stage != null) {
                    stage.setTitle("CAFI - " + nombreVista);
                }
            } catch (Exception e) {
                System.err.println("Error al actualizar título: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            // System.err.println("❌ Error cargando vista: " + nombreVista);
            mostrarVistaError(nombreVista, e.getMessage());
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