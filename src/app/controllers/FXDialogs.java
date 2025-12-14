package app.controllers;
import javafx.application.Platform;
import javafx.scene.control.Alert;

public class FXDialogs {

    public static void error(String title, String header, String content) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle(title);
            a.setHeaderText(header);
            a.setContentText(content);
            a.showAndWait();
        });
    }
}
