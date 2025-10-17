package app.controllers.administradores;

import core.SessionManager;
import core.data.Users.AllUsers;
import core.data.Users.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

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

    private final AllUsers allUsers = AllUsers.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();

    // Control de modo edición
    private boolean modoEdicion = false;
    private User administradorEditando = null;

    @FXML
    private void initialize() {
        // Verificar permisos de administrador
        if (!sessionManager.isAdmin()) {
            mostrarAlerta("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }
        
        configurarValidaciones();
    }

    /**
     * Acción del botón "Registrar Administrador"
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

        // Validar formato de correo
        if (!correo.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            lblStatus.setText("⚠️ Ingrese un correo electrónico válido.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Validar formato de teléfono (opcional)
        if (!telefono.isEmpty() && !telefono.matches("\\d{10}")) {
            lblStatus.setText("⚠️ El teléfono debe tener 10 dígitos.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        if (modoEdicion && administradorEditando != null) {
            // Modo edición
            actualizarAdministrador(clave, nombre, apellidoP, apellidoM, correo, telefono, nip);
        } else {
            // Modo registro
            registrarNuevoAdministrador(clave, nombre, apellidoP, apellidoM, correo, telefono, nip);
        }
    }

    private void registrarNuevoAdministrador(String clave, String nombre, String apellidoP, String apellidoM, 
                                           String correo, String telefono, String nip) {
        // Verificar si ya existe el expediente
        if (allUsers.getUserByClave(clave) != null) {
            lblStatus.setText("⚠️ El expediente ya está registrado.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Verificar si ya existe el correo
        if (existeCorreo(correo)) {
            lblStatus.setText("⚠️ El correo electrónico ya está registrado.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Crear nuevo administrador
        User nuevoAdmin = new User(
            clave,           // username
            nip,             // password
            nombre,
            apellidoP,
            apellidoM,
            correo,
            telefono
        );
        nuevoAdmin.setClave(clave);
        nuevoAdmin.setAdmin(true); // Siempre será administrador

        // Guardar el nuevo administrador
        allUsers.addUser(nuevoAdmin);

        lblStatus.setText("✅ Administrador registrado correctamente.");
        lblStatus.setStyle("-fx-text-fill: green;");

        limpiarCampos();
    }

    private void actualizarAdministrador(String clave, String nombre, String apellidoP, String apellidoM, 
                                       String correo, String telefono, String nip) {
        // Verificar si el correo ya existe en otro usuario
        if (existeCorreo(correo) && !administradorEditando.getEmail().equalsIgnoreCase(correo)) {
            lblStatus.setText("⚠️ El correo electrónico ya está registrado en otra cuenta.");
            lblStatus.setStyle("-fx-text-fill: red;");
            return;
        }

        // Actualizar datos del administrador
        administradorEditando.setClave(clave);
        administradorEditando.setName(nombre);
        administradorEditando.setApellidoPaterno(apellidoP);
        administradorEditando.setApellidoMaterno(apellidoM);
        administradorEditando.setEmail(correo);
        administradorEditando.setPhone(telefono);
        
        // Solo actualizar password si se proporcionó uno nuevo
        if (!nip.isEmpty()) {
            // En un sistema real aquí se debería hashear la contraseña
            // Por ahora usamos el texto plano como en el sistema actual
        }

        // Guardar cambios
        allUsers.saveUsers();

        lblStatus.setText("✅ Administrador actualizado correctamente.");
        lblStatus.setStyle("-fx-text-fill: green;");

        // Cerrar ventana después de actualizar
        cerrarVentana();
    }

    private boolean existeCorreo(String correo) {
        for (User user : allUsers.getUsers()) {
            if (user.getEmail().equalsIgnoreCase(correo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Cancelar y regresar al listado
     */
    @FXML
    private void onCancelarClicked() {
        cerrarVentana();
    }

    /**
     * Cargar datos existentes para edición
     */
    public void cargarDatosExistentes(User administrador) {
        if (administrador == null) return;

        modoEdicion = true;
        administradorEditando = administrador;

        // Cargar datos en los campos
        txtExpediente.setText(administrador.getClave());
        txtNombre.setText(administrador.getName());
        txtApellidoPaterno.setText(administrador.getApellidoPaterno());
        txtApellidoMaterno.setText(administrador.getApellidoMaterno());
        txtCorreo.setText(administrador.getEmail());
        txtTelefono.setText(administrador.getPhone());
        
        // El NIP se deja vacío por seguridad

        // Actualizar UI para modo edición
        btnRegistrar.setText("💾 Actualizar Administrador");
        lblStatus.setText("📝 Editando administrador: " + administrador.getName());
    }

    /**
     * Configurar validaciones de campos
     */
    private void configurarValidaciones() {
        // Validar que el expediente solo contenga letras y números
        txtExpediente.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[A-Za-z0-9]*")) {
                txtExpediente.setText(oldValue);
            }
        });

        // Validar que el nombre solo contenga letras y espacios
        txtNombre.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*")) {
                txtNombre.setText(oldValue);
            }
        });

        // Validar que los apellidos solo contengan letras
        txtApellidoPaterno.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*")) {
                txtApellidoPaterno.setText(oldValue);
            }
        });

        txtApellidoMaterno.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*")) {
                txtApellidoMaterno.setText(oldValue);
            }
        });

        // Validar que el teléfono solo contenga números
        txtTelefono.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtTelefono.setText(oldValue);
            }
        });
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