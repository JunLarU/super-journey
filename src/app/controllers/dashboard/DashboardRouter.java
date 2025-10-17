package app.controllers.dashboard;

import core.SessionManager;
import core.data.Users.User;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
            stage.setScene(new Scene(root, 900, 700));
            stage.setTitle("CAFI – Panel Principal");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
