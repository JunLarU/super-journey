package app.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class TestController {
    @FXML private Button btnGoSignup;
    @FXML
    private void onGoSignupClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/Signup.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnGoSignup.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("CAFI – Registro de Usuario");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }    
}
