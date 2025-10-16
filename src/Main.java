import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class Main extends Application {
    private static final String APP_NAME = "CAFI";
    @Override
    public void start(Stage primaryStage) throws Exception {
        String viewName = "test";
        String fxmlPath = "/app/views/" + viewName + ".fxml";

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/assets/css/app.css").toExternalForm());

        primaryStage.getIcons().add(
            new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/img/CAFI_LOGO.png"))
        );

        primaryStage.setTitle(APP_NAME);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        System.out.println("✅ Ventana iniciada: " + primaryStage.getTitle());
    }
    public static void main(String[] args) {
        launch(args);
    }
}
