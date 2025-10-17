package app.controllers.sessions;

import core.SessionManager;
import core.data.Users.AllUsers;
import core.data.Users.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class SignupController {

    @FXML private TextField txtExpediente;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField txtNip;
    @FXML private RadioButton rbUsuario;
    @FXML private RadioButton rbAdministrador;
    @FXML private Label lblStatus;
    @FXML private HBox roleBox;

    private final AllUsers allUsers = AllUsers.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    @FXML
    private void initialize() {
        // Configura grupo de botones para tipo de usuario
        ToggleGroup roleGroup = new ToggleGroup();
        rbUsuario.setToggleGroup(roleGroup);
        rbAdministrador.setToggleGroup(roleGroup);
        rbUsuario.setSelected(true); // por defecto

        // 🔹 Verificar si hay sesión y si es administrador
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            // Ocultar el campo de tipo de usuario si no hay sesión o si no es admin
            roleBox.setVisible(false);
            roleBox.setManaged(false); // evita que ocupe espacio
            rbUsuario.setSelected(true);
        } else {
            roleBox.setVisible(true);
            roleBox.setManaged(true);
        }
    }

    /**
     * Acción del botón "Registrar Usuario"
     */
    @FXML
    private void onRegistrarClicked() {
        String clave = txtExpediente.getText().trim();
        String nombre = txtNombre.getText().trim();
        String apellidoP = txtApellidoPaterno.getText().trim();
        String apellidoM = txtApellidoMaterno.getText().trim();
        String correo = txtCorreo.getText().trim();
        String telefono = txtTelefono.getText().trim();
        String nip = txtNip.getText().trim();

        // Validación de campos vacíos
        if (clave.isEmpty() || nombre.isEmpty() || apellidoP.isEmpty() || correo.isEmpty() || nip.isEmpty()) {
            lblStatus.setText("⚠️ Complete todos los campos obligatorios.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Verificar si ya existe el expediente
        if (allUsers.getUserByClave(clave) != null) {
            lblStatus.setText("⚠️ El expediente ya está registrado.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Crear nuevo usuario
        User nuevo = new User(
            clave,           // username
            nip,             // password
            nombre,
            apellidoP,
            apellidoM,
            correo,
            telefono
        );
        nuevo.setClave(clave);

        // Solo se puede asignar rol de administrador si el usuario actual es admin
        User currentUser = sessionManager.getCurrentUser();
        if (currentUser != null && currentUser.isAdmin()) {
            nuevo.setAdmin(rbAdministrador.isSelected());
        } else {
            nuevo.setAdmin(false); // por defecto, usuario normal
        }

        // Guardar el nuevo usuario
        allUsers.addUser(nuevo);

        lblStatus.setText("✅ Usuario registrado correctamente.");
        lblStatus.setStyle("-fx-text-fill: green;");

        limpiarCampos();
    }

    /**
     * Regresar a la vista de inicio de sesión
     */
    @FXML
    private void onGoLoginClicked() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/app/views/sessions/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) txtExpediente.getScene().getWindow();
            stage.setScene(new Scene(root, 600, 500));
            stage.setTitle("CAFI - Inicio de Sesión");
        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("⚠️ Error al cargar la vista de inicio de sesión.");
            lblStatus.setStyle("-fx-text-fill: red;");
        }
    }

    /**
     * Limpia los campos del formulario
     */
    private void limpiarCampos() {
        txtExpediente.clear();
        txtNombre.clear();
        txtApellidoPaterno.clear();
        txtApellidoMaterno.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtNip.clear();
        rbUsuario.setSelected(true);
    }
}
