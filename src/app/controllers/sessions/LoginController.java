package app.controllers.sessions;

import core.data.Users.AllUsers;
import core.data.Users.User;
import app.controllers.dashboard.DashboardRouter;
import core.SessionManager;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField txtExpediente;   // campo de clave
    @FXML private PasswordField txtNip;      // campo de password
    @FXML private Label lblStatus;
    @FXML private Button btnGoSignup;

    private final AllUsers allUsers = AllUsers.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    /**
     * Acción al presionar el botón "Iniciar Sesión"
     */
     @FXML
    private void onLoginClicked() {
        String expediente = txtExpediente.getText().trim();
        String nip = txtNip.getText().trim();

        // Validaciones básicas
        if (expediente.isEmpty() || nip.isEmpty()) {
            lblStatus.setText("⚠️ Ingrese su expediente y NIP.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Buscar usuario
        User user = allUsers.getUserByClave(expediente);
        if (user == null) {
            lblStatus.setText("❌ Usuario no encontrado.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Validar NIP
        if (!user.getPassword().equals(nip)) {
            lblStatus.setText("❌ NIP incorrecto.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Guardar sesión
        sessionManager.setCurrentUser(user);

        lblStatus.setText("✅ Bienvenido, " + user.getName());
        lblStatus.setStyle("-fx-text-fill: green;");

        // 🔹 Cargar Dashboard correspondiente
        Stage currentStage = (Stage) txtExpediente.getScene().getWindow();
        DashboardRouter.loadDashboard(currentStage);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
