package app.controllers.sessions;

import core.data.Users.AllUsers;
import core.data.Users.User;
import core.services.UserService;
import app.controllers.dashboard.DashboardRouter;
import core.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML
    private TextField txtExpediente; // campo de clave
    @FXML
    private PasswordField txtNip; // campo de password
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnGoSignup;

    private final AllUsers allUsers = AllUsers.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    /**
     * Acción al presionar el botón "Iniciar Sesión"
     */
    @FXML
    private void onLoginClicked() {
        String expediente = txtExpediente.getText().trim();
        String nip = txtNip.getText().trim();

        if (expediente.isEmpty() || nip.isEmpty()) {
            lblStatus.setText("⚠️ Ingrese su expediente y NIP.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        lblStatus.setText("⏳ Iniciando sesión...");
        lblStatus.setStyle("-fx-text-fill: black;");

        UserService.login(expediente, nip, new UserService.LoginCallback() {
            @Override
            public void onSuccess(org.json.JSONObject userJson) {
                System.out.println("[LoginController] Login exitoso: " + userJson);

                javafx.application.Platform.runLater(() -> {
                    // Crear usuario local a partir del JSON
                    User loggedIn = new User();
                    loggedIn.setClave(userJson.optString("Expediente"));
                    loggedIn.setName(userJson.optString("Nombre"));
                    loggedIn.setApellidoPaterno(userJson.optString("ApellidoPaterno"));
                    loggedIn.setApellidoMaterno(userJson.optString("ApellidoMaterno"));
                    loggedIn.setEmail(userJson.optString("Correo"));
                    loggedIn.setPhone(userJson.optString("Telefono"));
                    loggedIn.setAdmin("Administrador".equals(userJson.optString("Tipo")));

                    // Guardar sesión
                    sessionManager.setCurrentUser(loggedIn);

                    // Mostrar éxito
                    lblStatus.setText("✅ Bienvenido, " + loggedIn.getName());
                    lblStatus.setStyle("-fx-text-fill: green;");

                    // Cargar Dashboard
                    Stage currentStage = (Stage) txtExpediente.getScene().getWindow();
                    DashboardRouter.loadDashboard(currentStage);
                });
            }

            @Override
            public void onError(String error) {
                System.out.println("[LoginController] Login error: " + error);
                javafx.application.Platform.runLater(() -> {
                    lblStatus.setText("❌ " + error);
                    lblStatus.setStyle("-fx-text-fill: red;");
                });
            }
        });
    }

    /**
     * Acción para cambiar a la vista de registro
     */
    @FXML
    private void onGoSignupClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/sessions/Signup.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) btnGoSignup.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("CAFI - Registro de Usuario");
            stage.centerOnScreen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
