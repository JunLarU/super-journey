package app.controllers.dashboard;

import core.SessionManager;
import core.data.Users.User;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class DashboardRouter {

    public static void loadDashboard(Stage stage) {
        try {
            User current = SessionManager.getInstance().getCurrentUser();
            String fxmlPath;

            if (current != null && current.isAdmin()) {
                fxmlPath = "/app/views/dashboard/DashboardAdmin.fxml";
            } else {
                fxmlPath = "/app/views/dashboard/DashboardUser.fxml";
            }

            FXMLLoader loader = new FXMLLoader(DashboardRouter.class.getResource(fxmlPath));
            Parent root = loader.load();
            
            // Obtener la pantalla primaria
            Screen pantalla = Screen.getPrimary();
            Rectangle2D bounds = pantalla.getVisualBounds();

            // Tamaño estándar para el dashboard
            int ancho = Math.min(1100, (int) bounds.getWidth());
            int alto = Math.min(700, (int) bounds.getHeight());
            
            Scene scene = new Scene(root, ancho, alto);
            stage.setScene(scene);
            
            // IMPORTANTE: Dashboard inicia NO resizable
            stage.setResizable(false);
            stage.centerOnScreen();
            stage.setTitle("CAFI - Dashboard");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}