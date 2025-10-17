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

            // Obtener los bounds FÍSICOS (resolución nativa)
            Rectangle2D bounds = pantalla.getVisualBounds();

            int ancho = ((int) bounds.getWidth()>1100)?1100:(int) bounds.getWidth();
            int alto = ((int) bounds.getHeight()>700)?700:(int) bounds.getHeight();
            stage.setScene(new Scene(root, ancho, alto));
            stage.centerOnScreen();
            stage.setTitle("CAFI");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
