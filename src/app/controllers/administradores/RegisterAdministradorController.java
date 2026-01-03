package app.controllers.administradores;

import core.SessionManager;
import core.data.Users.User;
import core.services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.json.JSONObject;

public class RegisterAdministradorController {

    @FXML private TextField txtExpediente;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidoPaterno;
    @FXML private TextField txtApellidoMaterno;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private PasswordField txtNip;
    @FXML private Label lblStatus;
    @FXML private Button btnRegistrar;
    @FXML private Button btnCancelar;

    private final SessionManager sessionManager = SessionManager.getInstance();

    private boolean modoEdicion = false;
    private User administradorEditando = null;

    @FXML
    private void initialize() {
        if (!sessionManager.isAdmin()) {
            mostrarAlerta("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            btnRegistrar.setDisable(true);
        }
        configurarValidaciones();
    }

    @FXML
    private void onRegistrarClicked() {
        String clave      = txtExpediente.getText().trim();
        String nombre     = txtNombre.getText().trim();
        String apellidoP  = txtApellidoPaterno.getText().trim();
        String apellidoM  = txtApellidoMaterno.getText().trim();
        String correo     = txtCorreo.getText().trim();
        String telefono   = txtTelefono.getText().trim();
        String nip        = txtNip.getText().trim();

        if (clave.isEmpty() || nombre.isEmpty() || apellidoP.isEmpty() || correo.isEmpty()) {
            lblStatus.setText("⚠️ Complete los campos obligatorios.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!correo.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            lblStatus.setText("⚠️ Correo no válido.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        if (!telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            lblStatus.setText("⚠️ Teléfono debe tener 10 dígitos.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        if (modoEdicion) {
            actualizarAdministrador(clave, nombre, apellidoP, apellidoM, correo, telefono, nip);
        } else {
            registrarNuevoAdministrador(clave, nombre, apellidoP, apellidoM, correo, telefono, nip);
        }
    }

    private void registrarNuevoAdministrador(
        String clave, String nombre, String apellidoP, String apellidoM,
        String correo, String telefono, String nip
    ) {
        lblStatus.setText("⏳ Registrando...");
        lblStatus.setStyle("-fx-text-fill: black;");

        UserService.signup(
            clave, nombre, apellidoP, apellidoM,
            correo, telefono, nip, "Administrador",
            new UserService.SignupCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    javafx.application.Platform.runLater(() -> {
                        lblStatus.setText("✅ Administrador registrado.");
                        lblStatus.setStyle("-fx-text-fill: green;");
                        limpiarCampos();
                    });
                }
                @Override
                public void onError(String error) {
                    javafx.application.Platform.runLater(() -> {
                        lblStatus.setText("❌ " + error);
                        lblStatus.setStyle("-fx-text-fill: red;");
                    });
                }
            }
        );
    }

    private void actualizarAdministrador(
        String clave, String nombre, String apellidoP, String apellidoM,
        String correo, String telefono, String nip
    ) {
        lblStatus.setText("⏳ Actualizando...");
        lblStatus.setStyle("-fx-text-fill: black;");

        JSONObject body = new JSONObject();
        body.put("expediente", clave);
        body.put("nombre", nombre);
        body.put("apellidoPaterno", apellidoP);
        body.put("apellidoMaterno", apellidoM);
        body.put("correo", correo);
        body.put("telefono", telefono);
        body.put("tipo", "Administrador");
        // Si nip está vacío, backend lo ignorará (mantendrá el anterior)
        body.put("nip", nip);

        UserService.updateUser(body, new UserService.UpdateCallback() {
            @Override
            public void onSuccess() {
                javafx.application.Platform.runLater(() -> {
                    lblStatus.setText("✅ Administrador actualizado.");
                    lblStatus.setStyle("-fx-text-fill: green;");
                    cerrarVentana();
                });
            }
            @Override
            public void onError(String error) {
                javafx.application.Platform.runLater(() -> {
                    lblStatus.setText("❌ " + error);
                    lblStatus.setStyle("-fx-text-fill: red;");
                });
            }
        });
    }

    public void cargarDatosExistentes(User administrador) {
        if (administrador == null) return;
        modoEdicion = true;
        administradorEditando = administrador;

        txtExpediente.setText(administrador.getClave());
        txtNombre.setText(administrador.getName());
        txtApellidoPaterno.setText(administrador.getApellidoPaterno());
        txtApellidoMaterno.setText(administrador.getApellidoMaterno());
        txtCorreo.setText(administrador.getEmail());
        txtTelefono.setText(administrador.getPhone());

        btnRegistrar.setText("💾 Actualizar Administrador");
        lblStatus.setText("📝 Editando administrador: " + administrador.getName());
    }

    @FXML
    private void onCancelarClicked() {
        cerrarVentana();
    }

    private void configurarValidaciones() {
        txtExpediente.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[A-Za-z0-9]*")) txtExpediente.setText(o);
        });

        txtNombre.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*")) txtNombre.setText(o);
        });

        txtApellidoPaterno.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*")) txtApellidoPaterno.setText(o);
        });

        txtApellidoMaterno.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*")) txtApellidoMaterno.setText(o);
        });

        txtTelefono.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("\\d*")) txtTelefono.setText(o);
        });
    }

    private void limpiarCampos() {
        txtExpediente.clear();
        txtNombre.clear();
        txtApellidoPaterno.clear();
        txtApellidoMaterno.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtNip.clear();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
