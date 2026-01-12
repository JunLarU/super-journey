package app.controllers.avisos;

import core.SessionManager;
import core.data.Avisos.AllAvisos;
import core.data.Avisos.Aviso;
import core.services.AvisosService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controlador para el formulario de registro/edición de avisos
 * Conectado al servidor para operaciones CRUD
 */
public class RegistroAvisoController {

    // COMPONENTES FXML

    @FXML
    private Label lblTitulo;
    @FXML
    private TextField txtTitulo;
    @FXML
    private TextArea txtContenido;
    @FXML
    private ComboBox<Aviso.Establecimiento> cmbEstablecimiento;
    @FXML
    private ComboBox<Aviso.TipoAviso> cmbTipoAviso;
    @FXML
    private ComboBox<Aviso.Prioridad> cmbPrioridad;
    @FXML
    private DatePicker dtpFechaInicio;
    @FXML
    private DatePicker dtpFechaFin;
    @FXML
    private Spinner<Integer> spnHoraInicio;
    @FXML
    private Spinner<Integer> spnMinutoInicio;
    @FXML
    private Spinner<Integer> spnHoraFin;
    @FXML
    private Spinner<Integer> spnMinutoFin;
    @FXML
    private CheckBox chkActivo;
    @FXML
    private Button btnGuardar;
    @FXML
    private Button btnCancelar;
    @FXML
    private Label lblStatus;
    @FXML
    private VBox vboxInfo;

    // MODELOS Y DATOS

    private final AllAvisos allAvisos = AllAvisos.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private boolean modoEdicion = false;
    private Aviso avisoEditando = null;

    // INICIALIZACIÓN

    @FXML
    public void initialize() {
        // Verificar permisos de administrador
        if (!session.isAdmin()) {
            mostrarAlerta("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        configurarControles();
        configurarValoresPorDefecto();
    }

    private void configurarControles() {
        // Configurar spinners de hora y minuto
        spnHoraInicio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 8));
        spnMinutoInicio.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        spnHoraFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 20));
        spnMinutoFin.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));

        // Configurar DatePickers con valores por defecto
        dtpFechaInicio.setValue(LocalDate.now());
        dtpFechaFin.setValue(LocalDate.now().plusDays(7));

        // Configurar ComboBox de Establecimiento
        ObservableList<Aviso.Establecimiento> establecimientos = FXCollections
                .observableArrayList(Aviso.Establecimiento.values());
        cmbEstablecimiento.setItems(establecimientos);
        cmbEstablecimiento.setConverter(new StringConverter<Aviso.Establecimiento>() {
            @Override
            public String toString(Aviso.Establecimiento establecimiento) {
                if (establecimiento == null)
                    return "";
                switch (establecimiento) {
                    case Cafeteria:
                        return "🏢 Cafetería";
                    case Cafecito:
                        return "☕ Cafecito";
                    case Ambos:
                        return "🏢☕ Ambos Establecimientos";
                    default:
                        return establecimiento.name();
                }
            }

            @Override
            public Aviso.Establecimiento fromString(String string) {
                return null; // No necesario para display
            }
        });

        // Configurar ComboBox de TipoAviso
        ObservableList<Aviso.TipoAviso> tiposAviso = FXCollections.observableArrayList(Aviso.TipoAviso.values());
        cmbTipoAviso.setItems(tiposAviso);
        cmbTipoAviso.setConverter(new StringConverter<Aviso.TipoAviso>() {
            @Override
            public String toString(Aviso.TipoAviso tipoAviso) {
                if (tipoAviso == null)
                    return "";
                switch (tipoAviso) {
                    case General:
                        return "📢 General";
                    case Horario:
                        return "⏰ Horario";
                    case NoLaboral:
                        return "🚫 No Laboral";
                    case Oferta:
                        return "💰 Oferta";
                    case Evento:
                        return "🎉 Evento";
                    default:
                        return tipoAviso.name();
                }
            }

            @Override
            public Aviso.TipoAviso fromString(String string) {
                return null; // No necesario para display
            }
        });

        // Configurar ComboBox de Prioridad
        ObservableList<Aviso.Prioridad> prioridades = FXCollections.observableArrayList(Aviso.Prioridad.values());
        cmbPrioridad.setItems(prioridades);
        cmbPrioridad.setConverter(new StringConverter<Aviso.Prioridad>() {
            @Override
            public String toString(Aviso.Prioridad prioridad) {
                if (prioridad == null)
                    return "";
                switch (prioridad) {
                    case Normal:
                        return "📄 Normal";
                    case Importante:
                        return "⭐ Importante";
                    default:
                        return prioridad.name();
                }
            }

            @Override
            public Aviso.Prioridad fromString(String string) {
                return null; // No necesario para display
            }
        });

        // Establecer tooltips
        txtTitulo.setTooltip(new Tooltip("Título breve y descriptivo del aviso"));
        txtContenido.setTooltip(new Tooltip("Contenido detallado del aviso"));
        chkActivo.setTooltip(new Tooltip("Activar/desactivar el aviso"));
    }

    private void configurarValoresPorDefecto() {
        // Establecer valores por defecto
        cmbEstablecimiento.setValue(Aviso.Establecimiento.Ambos);
        cmbTipoAviso.setValue(Aviso.TipoAviso.General);
        cmbPrioridad.setValue(Aviso.Prioridad.Normal);
        chkActivo.setSelected(true);

        lblStatus.setText("Completa los campos para crear un aviso");
    }

    // MÉTODOS DE ACCIÓN

    @FXML
    private void onGuardarClicked() {
        if (!validarFormulario())
            return;

        btnGuardar.setDisable(true);
        lblStatus.setText("Guardando aviso en el servidor...");

        try {
            String titulo = txtTitulo.getText().trim();
            String contenido = txtContenido.getText().trim();
            Aviso.Establecimiento establecimiento = cmbEstablecimiento.getValue();
            Aviso.TipoAviso tipoAviso = cmbTipoAviso.getValue();
            Aviso.Prioridad prioridad = cmbPrioridad.getValue();
            boolean activo = chkActivo.isSelected();

            // Crear LocalDateTime para inicio y fin
            LocalDateTime fechaInicio = LocalDateTime.of(
                    dtpFechaInicio.getValue(),
                    LocalTime.of(spnHoraInicio.getValue(), spnMinutoInicio.getValue()));

            LocalDateTime fechaFin = LocalDateTime.of(
                    dtpFechaFin.getValue(),
                    LocalTime.of(spnHoraFin.getValue(), spnMinutoFin.getValue()));

            // Validar que la fecha de fin sea posterior a la de inicio
            if (fechaFin.isBefore(fechaInicio)) {
                lblStatus.setText("❌ La fecha/hora de fin debe ser posterior a la de inicio");
                btnGuardar.setDisable(false);
                mostrarAlerta("Error de validación", "La fecha/hora de fin debe ser posterior a la de inicio");
                return;
            }

            if (modoEdicion && avisoEditando != null) {
                // Modo edición
                avisoEditando.setTitulo(titulo);
                avisoEditando.setContenido(contenido);
                avisoEditando.setEstablecimiento(establecimiento);
                avisoEditando.setTipoAviso(tipoAviso);
                avisoEditando.setPrioridad(prioridad);
                avisoEditando.setFechaInicio(fechaInicio);
                avisoEditando.setFechaFin(fechaFin);
                avisoEditando.setActivo(activo);

                // Guardar en servidor
                guardarEnServidor(avisoEditando, true);
            } else {
                // Modo nuevo
                Aviso nuevoAviso = new Aviso(
                        0, // ID se asignará desde el servidor
                        titulo,
                        contenido,
                        establecimiento,
                        tipoAviso,
                        prioridad,
                        LocalDateTime.now(), // Fecha de publicación = ahora
                        fechaInicio,
                        fechaFin,
                        session.getCurrentUser() != null ? session.getCurrentUser().getId() : 1,
                        activo);

                // Guardar en servidor
                guardarEnServidor(nuevoAviso, false);
            }

        } catch (Exception e) {
            Platform.runLater(() -> {
                lblStatus.setText("❌ Error al guardar: " + e.getMessage());
                btnGuardar.setDisable(false);
                mostrarAlerta("Error", "Error al procesar los datos: " + e.getMessage());
            });
            e.printStackTrace();
        }
    }

    private void guardarEnServidor(Aviso aviso, boolean esEdicion) {
        Task<Boolean> task;
        AvisosService avisosService = AvisosService.getInstance();

        if (esEdicion) {
            task = avisosService.actualizar(aviso);
        } else {
            task = avisosService.crear(aviso);
        }

        task.setOnSucceeded(event -> {
            Platform.runLater(() -> {
                try {
                    if (task.getValue()) {
                        // Actualizar cache local
                        if (esEdicion) {
                            allAvisos.updateAviso(aviso);
                        } else {
                            allAvisos.addAviso(aviso);
                        }

                        lblStatus.setText("✅ Aviso " + (esEdicion ? "actualizado" : "creado") + " correctamente");

                        if (!esEdicion) {
                            // Limpiar campos para nuevo registro
                            limpiarCampos();
                        }

                        // Esperar un momento antes de cerrar para que el usuario vea el mensaje
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(() -> {
                                    Stage stage = (Stage) btnCancelar.getScene().getWindow();
                                    stage.close();
                                });
                            } catch (InterruptedException e) {
                                Platform.runLater(() -> {
                                    Stage stage = (Stage) btnCancelar.getScene().getWindow();
                                    stage.close();
                                });
                            }
                        }).start();

                    } else {
                        lblStatus.setText("❌ Error: No se pudo guardar en el servidor");
                        btnGuardar.setDisable(false);
                        mostrarAlerta("Error del servidor", "No se pudo guardar el aviso. Intente nuevamente.");
                    }
                } catch (Exception e) {
                    lblStatus.setText("❌ Error al procesar respuesta: " + e.getMessage());
                    btnGuardar.setDisable(false);
                    mostrarAlerta("Error", "Error al procesar respuesta del servidor: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        });

        task.setOnFailed(event -> {
            Platform.runLater(() -> {
                Throwable ex = task.getException();
                String errorMsg = ex != null ? ex.getMessage() : "Error desconocido";
                lblStatus.setText("❌ Error de conexión: " + errorMsg);
                btnGuardar.setDisable(false);
                mostrarAlerta("Error de conexión", "No se pudo conectar al servidor: " + errorMsg);
                if (ex != null) {
                    ex.printStackTrace();
                }
            });
        });

        new Thread(task).start();
    }

    @FXML
    private void onCancelarClicked() {
        cerrarVentana();
    }

    // VALIDACIÓN

    private boolean validarFormulario() {
        // Validar título
        if (txtTitulo.getText().trim().isEmpty()) {
            mostrarAlerta("⚠️ Campo requerido", "Ingresa el título del aviso");
            txtTitulo.requestFocus();
            return false;
        }

        // Validar contenido
        if (txtContenido.getText().trim().isEmpty()) {
            mostrarAlerta("⚠️ Campo requerido", "Ingresa el contenido del aviso");
            txtContenido.requestFocus();
            return false;
        }

        // Validar establecimiento
        if (cmbEstablecimiento.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona un establecimiento");
            cmbEstablecimiento.requestFocus();
            return false;
        }

        // Validar tipo de aviso
        if (cmbTipoAviso.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona el tipo de aviso");
            cmbTipoAviso.requestFocus();
            return false;
        }

        // Validar prioridad
        if (cmbPrioridad.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona la prioridad");
            cmbPrioridad.requestFocus();
            return false;
        }

        // Validar fechas
        if (dtpFechaInicio.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona la fecha de inicio");
            dtpFechaInicio.requestFocus();
            return false;
        }

        if (dtpFechaFin.getValue() == null) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona la fecha de fin");
            dtpFechaFin.requestFocus();
            return false;
        }

        return true;
    }

    // CARGAR DATOS EXISTENTES

    public void cargarDatosExistentes(Aviso aviso) {
        if (aviso == null)
            return;

        modoEdicion = true;
        avisoEditando = aviso;

        // Configurar título
        lblTitulo.setText("✏️ Editar Aviso");

        // Cargar datos existentes
        txtTitulo.setText(aviso.getTitulo());
        txtContenido.setText(aviso.getContenido());
        cmbEstablecimiento.setValue(aviso.getEstablecimiento());
        cmbTipoAviso.setValue(aviso.getTipoAviso());
        cmbPrioridad.setValue(aviso.getPrioridad());
        chkActivo.setSelected(aviso.isActivo());

        // Cargar fechas y horas
        dtpFechaInicio.setValue(aviso.getFechaInicio().toLocalDate());
        dtpFechaFin.setValue(aviso.getFechaFin().toLocalDate());

        spnHoraInicio.getValueFactory().setValue(aviso.getFechaInicio().getHour());
        spnMinutoInicio.getValueFactory().setValue(aviso.getFechaInicio().getMinute());
        spnHoraFin.getValueFactory().setValue(aviso.getFechaFin().getHour());
        spnMinutoFin.getValueFactory().setValue(aviso.getFechaFin().getMinute());

        // Actualizar UI
        btnGuardar.setText("💾 Actualizar");
        lblStatus.setText("✏️ Editando aviso #" + aviso.getId());
    }

    // MODO VISUALIZACIÓN

    public void visualizarAviso(Aviso aviso) {
        cargarDatosExistentes(aviso);

        vboxInfo.setVisible(false);
        vboxInfo.setManaged(false);

        // Deshabilitar todos los controles
        txtTitulo.setDisable(true);
        txtContenido.setDisable(true);
        cmbEstablecimiento.setDisable(true);
        cmbTipoAviso.setDisable(true);
        cmbPrioridad.setDisable(true);
        dtpFechaInicio.setDisable(true);
        dtpFechaFin.setDisable(true);
        spnHoraInicio.setDisable(true);
        spnMinutoInicio.setDisable(true);
        spnHoraFin.setDisable(true);
        spnMinutoFin.setDisable(true);
        chkActivo.setDisable(true);

        btnGuardar.setVisible(false);
        btnGuardar.setManaged(false);

        lblTitulo.setText("👁️ Visualizar Aviso");
        lblStatus.setText("👁️ Visualizando aviso #" + aviso.getId());
    }

    // UTILIDADES

    private void limpiarCampos() {
        txtTitulo.clear();
        txtContenido.clear();
        cmbEstablecimiento.setValue(Aviso.Establecimiento.Ambos);
        cmbTipoAviso.setValue(Aviso.TipoAviso.General);
        cmbPrioridad.setValue(Aviso.Prioridad.Normal);
        dtpFechaInicio.setValue(LocalDate.now());
        dtpFechaFin.setValue(LocalDate.now().plusDays(7));
        spnHoraInicio.getValueFactory().setValue(8);
        spnMinutoInicio.getValueFactory().setValue(0);
        spnHoraFin.getValueFactory().setValue(20);
        spnMinutoFin.getValueFactory().setValue(0);
        chkActivo.setSelected(true);

        modoEdicion = false;
        avisoEditando = null;
        btnGuardar.setText("💾 Guardar");
        btnGuardar.setDisable(false);
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