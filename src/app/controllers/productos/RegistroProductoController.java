package app.controllers.productos;

import core.SessionManager;
import core.services.ProductoService;
import core.services.IngredienteService;
import core.data.Productos.Producto;
import core.data.Productos.ProductoIngrediente;
import core.data.Productos.Sustituto;
import core.data.Productos.TamanoProducto;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controlador del formulario de registro/edición de productos.
 * Ahora se comunica directamente con el servidor.
 */
public class RegistroProductoController {

    // =========================
    // 🔹 Campos principales
    // =========================
    @FXML
    private TextField txtNombre;
    @FXML
    private TextArea txtDescripcion;
    @FXML
    private ComboBox<String> cmbCategoria;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtCalorias;
    @FXML
    private TextField txtGramaje;
    @FXML
    private CheckBox chkDisponible;
    @FXML
    private Button btnLimpiar;

    // =========================
    // 🔹 Campos para control de visualización
    // =========================
    @FXML
    private Label lblTitulo;
    @FXML
    private VBox vboxInfo;

    // =========================
    // 🔹 Ingredientes
    // =========================
    @FXML
    private TextField txtBuscarIngrediente;
    @FXML
    private ListView<String> listaIngredientesBuscados;
    @FXML
    private TableView<ProductoIngrediente> tablaIngredientes;
    @FXML
    private TableColumn<ProductoIngrediente, String> colIngNombre;
    @FXML
    private TableColumn<ProductoIngrediente, String> colIngSustitutos;
    @FXML
    private TableColumn<ProductoIngrediente, Boolean> colIngEliminar;
    @FXML
    private TableColumn<ProductoIngrediente, Boolean> colIngSustituible;
    @FXML
    private TableColumn<ProductoIngrediente, Void> colIngAcciones;

    // =========================
    // 🔹 Tamaños
    // =========================
    @FXML
    private TableView<TamanoProducto> tablaTamanos;
    @FXML
    private TableColumn<TamanoProducto, String> colTamNombre;
    @FXML
    private TableColumn<TamanoProducto, String> colTamDescripcion;
    @FXML
    private TableColumn<TamanoProducto, String> colTamPrecio;
    @FXML
    private TableColumn<TamanoProducto, Void> colTamAcciones;

    @FXML
    private TextField txtTamNombre;
    @FXML
    private TextField txtTamDescripcion;
    @FXML
    private TextField txtTamPrecio;
    @FXML
    private TextField txtTamCapacidad;
    @FXML
    private TextField txtTamGramaje;
    @FXML
    private TextField txtTamPiezas;
    @FXML
    private Button btnAgregarTamano;

    @FXML
    private Button btnRegistrar;
    @FXML
    private Label lblStatus;

    // =========================
    // 🔹 Objetos de datos
    // =========================
    private final SessionManager session = SessionManager.getInstance();

    private final ObservableList<ProductoIngrediente> ingredientesSeleccionados = FXCollections.observableArrayList();
    private final ObservableList<TamanoProducto> tamanosDefinidos = FXCollections.observableArrayList();
    private final ObservableList<String> categoriasDisponibles = FXCollections.observableArrayList();
    private final ObservableList<JSONObject> ingredientesDisponibles = FXCollections.observableArrayList();

    // Control de modo edición
    private boolean modoEdicion = false;
    private boolean modoVisualizacion = false;
    private int idProductoEditando = 0;
    private int nextTamanoId = 1;

    // =========================
    // 🔹 Inicialización
    // =========================
    @FXML
    public void initialize() {
        // Verificar permisos
        if (!session.isAdmin()) {
            mostrarAlerta("Acceso denegado", "Solo los administradores pueden acceder a esta función.");
            return;
        }

        cargarCategoriasDesdeServidor();
        cargarIngredientesDesdeServidor();
        configurarTablas();
        configurarBusquedaIngredientes();
        configurarValidaciones();
        configurarTamanos();
    }

    // ---------------------------------------------------
    // 🔄 Cargar categorías desde servidor
    // ---------------------------------------------------
    private void cargarCategoriasDesdeServidor() {
        ProductoService.listCategoriasProductos(new ProductoService.ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> categorias) {
                Platform.runLater(() -> {
                    categoriasDisponibles.clear();
                    for (JSONObject cat : categorias) {
                        categoriasDisponibles.add(cat.getString("Nombre"));
                    }
                    configurarComboBoxCategorias();
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    mostrarAlerta("Error", "No se pudieron cargar las categorías: " + error);
                    // Cargar lista por defecto si falla
                    cargarCategoriasPorDefecto();
                    configurarComboBoxCategorias();
                });
            }
        });
    }

    private void cargarCategoriasPorDefecto() {
        categoriasDisponibles.setAll(
                // Categorías generales
                "Desayuno",
                "Comida",
                "Bebida Fría",
                "Bebida Caliente",
                "Snack",
                "Postre",

                // Desayunos
                "Desayuno Mexicano",
                "Desayuno Continental",
                "Desayuno Express",

                // Comidas
                "Plato Fuerte",
                "Antojitos Mexicanos",
                "Hamburguesas",
                "Tortas y Sandwiches",
                "Ensaladas",
                "Sopas y Cremas",
                "Pastas",
                "Alitas y Boneless",

                // Complementos
                "Guarniciones",
                "Extras",

                // Postres
                "Postres",
                "Repostería",

                // Bebidas calientes
                "Café",
                "Té e Infusiones",
                "Chocolate Caliente",
                "Bebidas de Temporada Calientes",

                // Bebidas frías
                "Café Frío",
                "Smoothies",
                "Jugos y Licuados",
                "Aguas Frescas",
                "Refrescos",
                "Bebidas Energéticas",
                "Bebidas de Temporada Frías",

                // Snacks y panadería
                "Snacks Dulces",
                "Snacks Salados",
                "Panadería",
                "Baguettes y Croissants",
                "Yogurt y Parfait",

                // Catch-all
                "General");
    }

    private void configurarComboBoxCategorias() {
        cmbCategoria.setEditable(true);
        cmbCategoria.setItems(categoriasDisponibles);

        final boolean[] isUpdating = { false };

        cmbCategoria.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdating[0])
                return;

            try {
                isUpdating[0] = true;
                final String texto = newValue;

                if (texto == null || texto.isEmpty()) {
                    cmbCategoria.setItems(categoriasDisponibles);
                } else {
                    final String lower = texto.toLowerCase();
                    ObservableList<String> filtradas = categoriasDisponibles.filtered(
                            c -> c.toLowerCase().contains(lower));
                    cmbCategoria.setItems(filtradas);
                }

                if (!cmbCategoria.getItems().isEmpty() && cmbCategoria.isFocused()) {
                    cmbCategoria.show();
                }
            } finally {
                isUpdating[0] = false;
            }
        });
    }

    // ---------------------------------------------------
    // 🔄 Cargar ingredientes desde servidor
    // ---------------------------------------------------
    private void cargarIngredientesDesdeServidor() {
        IngredienteService.listIngredientes(new IngredienteService.ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> ingredientes) {
                Platform.runLater(() -> {
                    ingredientesDisponibles.clear();
                    ingredientesDisponibles.addAll(ingredientes);
                    lblStatus.setText("✅ " + ingredientes.size() + " ingredientes cargados");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    mostrarAlerta("Error", "No se pudieron cargar los ingredientes: " + error);
                    lblStatus.setText("❌ Error cargando ingredientes");
                });
            }
        });
    }

    // ---------------------------------------------------
    // ⚙️ Configuración de Tamaños
    // ---------------------------------------------------
    private void configurarTamanos() {
        // Configurar tabla de tamaños
        colTamNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));

        colTamDescripcion.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescripcion()));

        colTamPrecio.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrecio())));

        colTamAcciones.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEliminar = new Button("Eliminar");
            {
                btnEliminar.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnEliminar.setTooltip(new Tooltip("Eliminar tamaño"));
                btnEliminar.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < tamanosDefinidos.size()) {
                        String nombreTam = tamanosDefinidos.get(idx).getNombre();
                        tamanosDefinidos.remove(idx);
                        lblStatus.setText("Tamaño eliminado: " + nombreTam);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });

        tablaTamanos.setItems(tamanosDefinidos);

        // Configurar botón agregar tamaño
        btnAgregarTamano.setOnAction(e -> agregarTamano());
    }

    // ---------------------------------------------------
    // ➕ Agregar Tamaño
    // ---------------------------------------------------
    // En el método agregarTamano, usa Double e Integer para los parámetros
    // opcionales:
    private void agregarTamano() {
        String nombre = txtTamNombre.getText().trim();
        String precioStr = txtTamPrecio.getText().trim();

        if (nombre.isEmpty() || precioStr.isEmpty()) {
            mostrarAlerta("⚠️ Campos requeridos", "Ingresa al menos el nombre y precio del tamaño.");
            return;
        }

        try {
            double precio = Double.parseDouble(precioStr);

            // Obtener valores opcionales
            String descripcion = txtTamDescripcion.getText().trim();
            Double capacidad = txtTamCapacidad.getText().trim().isEmpty() ? null
                    : Double.parseDouble(txtTamCapacidad.getText().trim());
            Double gramaje = txtTamGramaje.getText().trim().isEmpty() ? null
                    : Double.parseDouble(txtTamGramaje.getText().trim());
            Integer piezas = txtTamPiezas.getText().trim().isEmpty() ? null
                    : Integer.parseInt(txtTamPiezas.getText().trim());

            // Crear nuevo tamaño
            TamanoProducto tamano = new TamanoProducto(
                    0, // ID temporal
                    nombre,
                    descripcion,
                    capacidad, // Ahora es Double, no double
                    gramaje, // Ahora es Double, no double
                    piezas, // Ahora es Integer, no int
                    precio,
                    tamanosDefinidos.size() + 1,
                    true);

            tamanosDefinidos.add(tamano);

            // Limpiar campos
            txtTamNombre.clear();
            txtTamDescripcion.clear();
            txtTamPrecio.clear();
            txtTamCapacidad.clear();
            txtTamGramaje.clear();
            txtTamPiezas.clear();

            lblStatus.setText("✅ Tamaño agregado: " + nombre);
        } catch (NumberFormatException e) {
            mostrarAlerta("⚠️ Formato inválido", "El precio, capacidad, gramaje y piezas deben ser números válidos.");
        }
    }

    // ---------------------------------------------------
    // ⚙️ Configuración de Tablas
    // ---------------------------------------------------
    private void configurarTablas() {
        tablaIngredientes.setEditable(true);

        // Columna: Nombre
        colIngNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre())); // Cambiado de
                                                                                                         // getNombreIngrediente()

        // Columna: Cantidad de sustitutos
        colIngSustitutos.setCellValueFactory(data -> {
            int cantidad = data.getValue().getSustitutos().size();
            return new SimpleStringProperty(cantidad > 0 ? cantidad + " sustituto(s)" : "-");
        });

        // Columna: Eliminable (checkbox editable)
        colIngEliminar.setCellValueFactory(data -> {
            ProductoIngrediente ing = data.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(ing.isEliminable());

            prop.addListener((obs, oldVal, newVal) -> {
                ing.setEliminable(newVal);
            });

            return prop;
        });
        colIngEliminar.setCellFactory(tc -> new CheckBoxTableCell<>());

        // Columna: Sustituible (checkbox editable)
        colIngSustituible.setCellValueFactory(data -> {
            ProductoIngrediente ing = data.getValue();
            SimpleBooleanProperty prop = new SimpleBooleanProperty(ing.isSustituible());

            prop.addListener((obs, oldVal, newVal) -> {
                ing.setSustituible(newVal);
                tablaIngredientes.refresh();
            });

            return prop;
        });
        colIngSustituible.setCellFactory(tc -> new CheckBoxTableCell<>());

        // Columna: Acciones
        colIngAcciones.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEliminar = new Button("Eliminar");
            private final Button btnSustituir = new Button("♻️");

            {
                btnEliminar.setStyle(
                        "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnSustituir.setStyle(
                        "-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnEliminar.setTooltip(new Tooltip("Eliminar ingrediente"));
                btnSustituir.setTooltip(new Tooltip("Definir sustitutos"));

                btnEliminar.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < ingredientesSeleccionados.size()) {
                        String nombreIng = ingredientesSeleccionados.get(idx).getNombreIngrediente();
                        ingredientesSeleccionados.remove(idx);
                        lblStatus.setText("Ingrediente eliminado: " + nombreIng);
                    }
                });

                btnSustituir.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < ingredientesSeleccionados.size()) {
                        ProductoIngrediente ing = ingredientesSeleccionados.get(idx);

                        if (ing.isSustituible()) {
                            mostrarDialogoSustitutos(ing);
                        } else {
                            mostrarAlerta("⚠️ Ingrediente no sustituible",
                                    "Primero marca el checkbox 'Sustituible' para este ingrediente.");
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5, btnEliminar, btnSustituir);
                    hbox.setAlignment(Pos.CENTER);
                    setGraphic(hbox);
                }
            }
        });

        tablaIngredientes.setItems(ingredientesSeleccionados);
    }

    // ---------------------------------------------------
    // 🔍 Búsqueda y selección de ingredientes
    // ---------------------------------------------------
    private void configurarBusquedaIngredientes() {
        txtBuscarIngrediente.textProperty().addListener((obs, o, n) -> {
            if (n.length() > 1)
                buscarIngrediente(n);
            else
                listaIngredientesBuscados.getItems().clear();
        });

        listaIngredientesBuscados.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String nombre = listaIngredientesBuscados.getSelectionModel().getSelectedItem();
                if (nombre != null) {
                    agregarIngredienteSeleccionado(nombre);
                }
            }
        });
    }

    private void buscarIngrediente(String query) {
        String queryLower = query.toLowerCase();

        List<String> resultados = ingredientesDisponibles.stream()
                .filter(ing -> {
                    String nombre = getIngredienteNombre(ing);
                    return nombre.toLowerCase().contains(queryLower);
                })
                .map(this::getIngredienteNombre)
                .filter(nombre -> !nombre.isEmpty() &&
                        ingredientesSeleccionados.stream()
                                .noneMatch(pi -> pi.getNombre().equalsIgnoreCase(nombre)))
                .collect(Collectors.toList());

        listaIngredientesBuscados.getItems().clear();
        listaIngredientesBuscados.getItems().addAll(resultados);
    }

    // En el método agregarIngredienteSeleccionado, usa el constructor correcto:
    private void agregarIngredienteSeleccionado(String nombreIngrediente) {
        // Buscar el ingrediente en la lista de disponibles para obtener su ID
        JSONObject ingrediente = ingredientesDisponibles.stream()
                .filter(ing -> {
                    // Usar getIngredienteNombre que maneja ambas posibilidades
                    String nombre = getIngredienteNombre(ing);
                    return nombre.equalsIgnoreCase(nombreIngrediente);
                })
                .findFirst()
                .orElse(null);

        if (ingrediente == null) {
            mostrarAlerta("Error", "No se encontró el ingrediente: " + nombreIngrediente);
            return;
        }

        // Usar los métodos helper para obtener ID y nombre
        int idIngrediente = getIngredienteId(ingrediente);
        String nombre = getIngredienteNombre(ingrediente);

        if (idIngrediente == 0 || nombre.isEmpty()) {
            mostrarAlerta("Error", "Datos del ingrediente incompletos");
            return;
        }

        ProductoIngrediente pi = new ProductoIngrediente(
                idIngrediente,
                nombre,
                1.0, // cantidad por defecto
                false, // no eliminable por defecto
                false, // no sustituible por defecto
                ingredientesSeleccionados.size() + 1 // orden
        );

        ingredientesSeleccionados.add(pi);
        listaIngredientesBuscados.getItems().remove(nombreIngrediente);
        txtBuscarIngrediente.clear();
        lblStatus.setText("✅ Ingrediente agregado: " + nombre);
    }

    // ---------------------------------------------------
    // 🔄 Diálogo para seleccionar sustitutos
    // ---------------------------------------------------
    private void mostrarDialogoSustitutos(ProductoIngrediente ingrediente) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sustitutos de: " + ingrediente.getNombreIngrediente());
        dialog.setHeaderText("Selecciona ingredientes sustitutos y define el costo adicional");

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(600);

        // Búsqueda
        Label lblBuscar = new Label("🔍 Buscar ingrediente sustituto:");
        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Escribe para buscar...");

        ListView<String> listaBusqueda = new ListView<>();
        listaBusqueda.setPrefHeight(120);

        // Agregar con costo
        HBox hboxAgregar = new HBox(10);
        hboxAgregar.setAlignment(Pos.CENTER_LEFT);
        TextField txtCostoExtra = new TextField("0.00");
        txtCostoExtra.setPromptText("Costo extra");
        txtCostoExtra.setPrefWidth(100);
        Button btnAgregar = new Button("➕ Agregar");
        btnAgregar.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        hboxAgregar.getChildren().addAll(new Label("Costo adicional: $"), txtCostoExtra, btnAgregar);

        // Tabla de sustitutos
        Label lblActuales = new Label("📋 Sustitutos actuales:");
        TableView<Sustituto> tablaSustitutos = new TableView<>();
        tablaSustitutos.setPrefHeight(150);

        TableColumn<Sustituto, String> colNombre = new TableColumn<>("Ingrediente");
        colNombre.setPrefWidth(300);
        colNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreIngrediente()));

        TableColumn<Sustituto, String> colCosto = new TableColumn<>("Costo Extra");
        colCosto.setPrefWidth(120);
        colCosto.setCellValueFactory(
                d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getCostoExtra())));

        TableColumn<Sustituto, Void> colAccion = new TableColumn<>("Acción");
        colAccion.setPrefWidth(80);
        colAccion.setCellFactory(tc -> new TableCell<>() {
            private final Button btnQuitar = new Button("❌");
            {
                btnQuitar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnQuitar
                        .setOnAction(e -> tablaSustitutos.getItems().remove(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnQuitar);
            }
        });

        tablaSustitutos.getColumns().addAll(colNombre, colCosto, colAccion);

        // Cargar sustitutos existentes
        ObservableList<Sustituto> listaSustitutos = FXCollections.observableArrayList(ingrediente.getSustitutos());
        tablaSustitutos.setItems(listaSustitutos);

        // Búsqueda de ingredientes
        // En el método mostrarDialogoSustitutos, líneas 359-368:
        // Búsqueda de ingredientes
        txtBuscar.textProperty().addListener((o, ov, nv) -> {
            if (nv.length() > 1) {
                String queryLower = nv.toLowerCase();

                List<String> resultados = ingredientesDisponibles.stream()
                        .filter(ing -> {
                            String nombre = getIngredienteNombre(ing);
                            return nombre.toLowerCase().contains(queryLower);
                        })
                        .filter(ing -> {
                            String nombre = getIngredienteNombre(ing);
                            return !nombre.equalsIgnoreCase(ingrediente.getNombreIngrediente());
                        })
                        .map(this::getIngredienteNombre)
                        .filter(nombre -> {
                            // Filtrar los que ya están agregados como sustitutos
                            return listaSustitutos.stream()
                                    .noneMatch(s -> s.getNombreIngrediente().equalsIgnoreCase(nombre));
                        })
                        .collect(Collectors.toList());

                listaBusqueda.getItems().clear();
                listaBusqueda.getItems().addAll(resultados);
            } else {
                listaBusqueda.getItems().clear();
            }
        });

        // En el mismo método, líneas 385-395:
        // Agregar sustituto
        btnAgregar.setOnAction(e -> {
            String seleccionado = listaBusqueda.getSelectionModel().getSelectedItem();
            if (seleccionado == null) {
                mostrarAlerta("⚠️ Selección requerida", "Selecciona un ingrediente de la lista.");
                return;
            }

            try {
                double costo = Double.parseDouble(txtCostoExtra.getText());

                // Obtener ID del ingrediente sustituto
                JSONObject ingSustituto = ingredientesDisponibles.stream()
                        .filter(ing -> {
                            String nombre = getIngredienteNombre(ing);
                            return nombre.equalsIgnoreCase(seleccionado);
                        })
                        .findFirst()
                        .orElse(null);

                if (ingSustituto == null) {
                    mostrarAlerta("Error", "No se encontró el ingrediente.");
                    return;
                }

                int idSustituto = getIngredienteId(ingSustituto);
                String nombreSustituto = getIngredienteNombre(ingSustituto);

                Sustituto nuevoSustituto = new Sustituto(
                        idSustituto,
                        nombreSustituto,
                        costo,
                        true);

                listaSustitutos.add(nuevoSustituto);
                listaBusqueda.getItems().remove(seleccionado);
                txtCostoExtra.setText("0.00");
                txtBuscar.clear();
            } catch (NumberFormatException ex) {
                mostrarAlerta("⚠️ Formato inválido", "El costo debe ser un número válido.");
            }
        });

        // Guardar
        Button btnGuardar = new Button("💾 Guardar Sustitutos");
        btnGuardar.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-pref-width: 200;");
        btnGuardar.setOnAction(e -> {
            ingrediente.setSustitutos(new ArrayList<>(listaSustitutos));
            tablaIngredientes.refresh();
            lblStatus.setText("✅ Sustitutos guardados para: " + ingrediente.getNombreIngrediente());
            dialog.close();
        });

        content.getChildren().addAll(
                lblBuscar, txtBuscar, listaBusqueda,
                new Separator(),
                hboxAgregar,
                new Separator(),
                lblActuales, tablaSustitutos,
                new Separator(),
                btnGuardar);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ---------------------------------------------------
    // ⚙️ Configurar validaciones
    // ---------------------------------------------------
    private void configurarValidaciones() {
        txtPrecio.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?")) {
                txtPrecio.setText(old);
            }
        });

        txtCalorias.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?")) {
                txtCalorias.setText(old);
            }
        });

        txtGramaje.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?")) {
                txtGramaje.setText(old);
            }
        });

        // Validaciones para campos de tamaños
        txtTamPrecio.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?")) {
                txtTamPrecio.setText(old);
            }
        });

        txtTamCapacidad.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?")) {
                txtTamCapacidad.setText(old);
            }
        });

        txtTamGramaje.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?")) {
                txtTamGramaje.setText(old);
            }
        });

        txtTamPiezas.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*")) {
                txtTamPiezas.setText(old);
            }
        });
    }

    // ---------------------------------------------------
    // 💾 Guardar producto (comunicación con servidor)
    // ---------------------------------------------------
    // ---------------------------------------------------
    // 💾 Guardar producto (comunicación con servidor)
    // ---------------------------------------------------
    @FXML
    private void onRegistrarClicked() {
        if (!validarCampos())
            return;

        btnRegistrar.setDisable(true);
        lblStatus.setText("Guardando producto...");

        // OBTENER EL ID DE LA CATEGORÍA ANTES DE CREAR EL PRODUCTO
        String nombreCategoria = cmbCategoria.getValue();
        int idCategoria = obtenerIdCategoria(nombreCategoria);

        if (idCategoria == 0) {
            // Si no se pudo obtener el ID, mostrar error
            mostrarAlerta("Error", "No se pudo obtener el ID de la categoría: " + nombreCategoria);
            btnRegistrar.setDisable(false);
            return;
        }

        // Crear objeto Producto con los datos del formulario
        // USAR EL CONSTRUCTOR CON IDCATEGORIA
        Producto producto = new Producto(
                modoEdicion ? idProductoEditando : 0,
                txtNombre.getText().trim(),
                txtDescripcion.getText().trim(),
                Double.parseDouble(txtPrecio.getText().trim()),
                cmbCategoria.getValue(), // NOMBRE de la categoría
                idCategoria, // ← ID de la categoría obtenido
                txtGramaje.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtGramaje.getText().trim()),
                txtCalorias.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtCalorias.getText().trim()),
                "", // urlFoto vacía por ahora
                chkDisponible.isSelected());

        // Agregar ingredientes y tamaños
        producto.setIngredientes(new ArrayList<>(ingredientesSeleccionados));
        producto.setTamanos(new ArrayList<>(tamanosDefinidos));

        // Definir callback para manejar la respuesta del servidor
        ProductoService.CrudCallback callback = new ProductoService.CrudCallback() {
            @Override
            public void onSuccess() {
                Platform.runLater(() -> {
                    lblStatus.setText(modoEdicion ? "✅ Producto actualizado correctamente."
                            : "✅ Producto registrado correctamente.");
                    btnRegistrar.setDisable(false);

                    if (!modoEdicion) {
                        limpiarCampos();
                    }
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + error);
                    btnRegistrar.setDisable(false);
                    mostrarAlerta("Error", error);
                });
            }
        };

        // Llamar al servicio correspondiente
        if (modoEdicion) {
            ProductoService.updateProducto(producto, callback);
        } else {
            ProductoService.createProducto(producto, callback);
        }
    }

    // ---------------------------------------------------
    // 🔍 Método auxiliar para obtener ID de categoría
    // ---------------------------------------------------
    private int obtenerIdCategoria(String nombreCategoria) {
        // Buscar en las categorías cargadas
        // Primero intenta obtener el ID desde el servidor
        try {
            // Buscar en la lista de categorías disponibles (si están en memoria)
            for (String cat : categoriasDisponibles) {
                if (cat.equalsIgnoreCase(nombreCategoria)) {
                    // En un caso real, aquí deberías hacer una llamada al servidor
                    // para obtener el ID, o tener un mapa de nombre->ID
                    return buscarIdCategoriaEnServidor(nombreCategoria);
                }
            }

            // Si no se encuentra, buscar en el servidor
            return buscarIdCategoriaEnServidor(nombreCategoria);

        } catch (Exception e) {
            mostrarAlerta("Error", "No se pudo obtener ID de categoría: " + e.getMessage());
            return 0;
        }
    }

    // ---------------------------------------------------
    // 🔄 Buscar ID de categoría en el servidor
    // ---------------------------------------------------
    private int buscarIdCategoriaEnServidor(String nombreCategoria) {
        // Esto debería ser una llamada al servidor
        // Por ahora, devolvemos un valor temporal
        // En producción, deberías tener un servicio que devuelva el ID de la categoría

        // Buscar en las categorías por defecto (esto es solo temporal)
        Map<String, Integer> categoriasMap = new HashMap<>();

        categoriasMap.put("Desayuno", 1);
        categoriasMap.put("Comida", 2);
        categoriasMap.put("Bebida Fría", 3);
        categoriasMap.put("Bebida Caliente", 4);
        categoriasMap.put("Snack", 5);
        categoriasMap.put("Postre", 6);

        categoriasMap.put("Desayuno Mexicano", 7);
        categoriasMap.put("Desayuno Continental", 8);
        categoriasMap.put("Desayuno Express", 9);

        categoriasMap.put("Plato Fuerte", 10);
        categoriasMap.put("Antojitos Mexicanos", 11);
        categoriasMap.put("Hamburguesas", 12);
        categoriasMap.put("Tortas y Sandwiches", 13);
        categoriasMap.put("Ensaladas", 14);
        categoriasMap.put("Sopas y Cremas", 15);
        categoriasMap.put("Pastas", 16);
        categoriasMap.put("Alitas y Boneless", 17);

        categoriasMap.put("Guarniciones", 18);
        categoriasMap.put("Extras", 19);

        categoriasMap.put("Postres", 20);
        categoriasMap.put("Repostería", 21);

        categoriasMap.put("Café", 22);
        categoriasMap.put("Té e Infusiones", 23);
        categoriasMap.put("Chocolate Caliente", 24);
        categoriasMap.put("Bebidas de Temporada Calientes", 25);

        categoriasMap.put("Café Frío", 26);
        categoriasMap.put("Smoothies", 27);
        categoriasMap.put("Jugos y Licuados", 28);
        categoriasMap.put("Aguas Frescas", 29);
        categoriasMap.put("Refrescos", 30);
        categoriasMap.put("Bebidas Energéticas", 31);
        categoriasMap.put("Bebidas de Temporada Frías", 32);

        categoriasMap.put("Snacks Dulces", 33);
        categoriasMap.put("Snacks Salados", 34);
        categoriasMap.put("Panadería", 35);
        categoriasMap.put("Baguettes y Croissants", 36);
        categoriasMap.put("Yogurt y Parfait", 37);

        categoriasMap.put("General", 38);

        // ... agregar todas las categorías conocidas

        Integer id = categoriasMap.get(nombreCategoria);
        return id != null ? id : 0;
    }

    // ---------------------------------------------------
    // ✅ Validar campos
    // ---------------------------------------------------
    private boolean validarCampos() {
        if (txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta("⚠️ Campo requerido", "El nombre del producto es obligatorio.");
            return false;
        }

        if (cmbCategoria.getValue() == null || cmbCategoria.getValue().isEmpty()) {
            mostrarAlerta("⚠️ Campo requerido", "Selecciona una categoría.");
            return false;
        }

        if (txtPrecio.getText().trim().isEmpty()) {
            mostrarAlerta("⚠️ Campo requerido", "El precio base es obligatorio.");
            return false;
        }

        try {
            double precio = Double.parseDouble(txtPrecio.getText().trim());
            if (precio <= 0) {
                mostrarAlerta("⚠️ Precio inválido", "El precio debe ser mayor a cero.");
                return false;
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("⚠️ Formato inválido", "El precio debe ser un número válido.");
            return false;
        }

        return true;
    }

    // ---------------------------------------------------
    // 📥 Cargar datos existentes desde JSON del servidor
    // ---------------------------------------------------
    // En cargarDatosExistentes, corrige el constructor de ProductoIngrediente:
    public void cargarDatosExistentes(JSONObject productoJson) {
        if (productoJson == null) {
            mostrarAlerta("Error", "Producto no válido recibido");
            return;
        }

        lblTitulo.setText("Editar Producto");
        modoEdicion = true;
        idProductoEditando = productoJson.getInt("ID");

        // Datos básicos
        txtNombre.setText(productoJson.optString("Nombre", ""));
        txtDescripcion.setText(productoJson.optString("Descripcion", ""));
        cmbCategoria.setValue(productoJson.optString("Categoria", ""));
        txtPrecio.setText(String.valueOf(productoJson.optDouble("PrecioBase", 0.0)));

        if (!productoJson.isNull("Calorias") && productoJson.getDouble("Calorias") > 0) {
            txtCalorias.setText(String.valueOf(productoJson.getDouble("Calorias")));
        }

        if (!productoJson.isNull("Gramaje") && productoJson.getDouble("Gramaje") > 0) {
            txtGramaje.setText(String.valueOf(productoJson.getDouble("Gramaje")));
        }

        chkDisponible.setSelected(productoJson.optInt("Disponible", 1) == 1);

        // Cargar ingredientes
        ingredientesSeleccionados.clear();
        if (productoJson.has("Ingredientes")) {
            JSONArray ingredientesArray = productoJson.getJSONArray("Ingredientes");
            for (int i = 0; i < ingredientesArray.length(); i++) {
                JSONObject ingJson = ingredientesArray.getJSONObject(i);

                // Usar el constructor con JSONObject
                ProductoIngrediente pi = new ProductoIngrediente(ingJson);

                ingredientesSeleccionados.add(pi);
            }
        }

        // Cargar tamaños
        tamanosDefinidos.clear();
        if (productoJson.has("Tamanos")) {
            JSONArray tamanosArray = productoJson.getJSONArray("Tamanos");
            for (int i = 0; i < tamanosArray.length(); i++) {
                JSONObject tamJson = tamanosArray.getJSONObject(i);

                // Usar el constructor con JSONObject
                TamanoProducto tamano = new TamanoProducto(tamJson);

                tamanosDefinidos.add(tamano);
            }

            // Actualizar el nextTamanoId basado en los tamaños existentes
            if (!tamanosDefinidos.isEmpty()) {
                nextTamanoId = tamanosDefinidos.stream()
                        .mapToInt(TamanoProducto::getId)
                        .max()
                        .orElse(0) + 1;
            }
        }

        // Actualizar UI
        btnRegistrar.setText("💾 Actualizar Producto");
        lblStatus.setText("📝 Editando producto: " + productoJson.getString("Nombre"));

        Platform.runLater(() -> {
            tablaIngredientes.refresh();
            tablaTamanos.refresh();
        });
    }

    // ---------------------------------------------------
    // visualizar producto (solo lectura)
    // ---------------------------------------------------
    public void visualizarProducto(JSONObject productoJson) {
        cargarDatosExistentes(productoJson);
        lblTitulo.setText("Visualizar Producto");
        modoVisualizacion = true;

        vboxInfo.setVisible(false);
        vboxInfo.setManaged(false);

        // Deshabilitar todos los campos
        txtNombre.setDisable(true);
        txtDescripcion.setDisable(true);
        cmbCategoria.setDisable(true);
        txtPrecio.setDisable(true);
        txtCalorias.setDisable(true);
        txtGramaje.setDisable(true);
        chkDisponible.setDisable(true);
        txtBuscarIngrediente.setDisable(true);
        listaIngredientesBuscados.setDisable(true);

        // Deshabilitar campos de tamaños
        txtTamNombre.setDisable(true);
        txtTamDescripcion.setDisable(true);
        txtTamPrecio.setDisable(true);
        txtTamCapacidad.setDisable(true);
        txtTamGramaje.setDisable(true);
        txtTamPiezas.setDisable(true);
        btnAgregarTamano.setDisable(true);

        // Deshabilitar tabla de ingredientes
        tablaIngredientes.setEditable(false);
        tablaIngredientes.setMouseTransparent(true);
        tablaIngredientes.setFocusTraversable(false);
        tablaIngredientes.setStyle("-fx-opacity: 0.8;");

        // Deshabilitar tabla de tamaños
        tablaTamanos.setEditable(false);
        tablaTamanos.setMouseTransparent(true);
        tablaTamanos.setFocusTraversable(false);
        tablaTamanos.setStyle("-fx-opacity: 0.8;");

        colIngEliminar.setEditable(false);
        colIngSustituible.setEditable(false);

        // Ocultar botones
        btnRegistrar.setVisible(false);
        btnRegistrar.setManaged(false);
        btnLimpiar.setVisible(false);
        btnLimpiar.setManaged(false);

        lblStatus.setText("visualizando producto: " + productoJson.getString("Nombre"));
        lblStatus.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 13px;");
    }

    // ---------------------------------------------------
    // 🔄 Limpiar campos
    // ---------------------------------------------------
    @FXML
    private void limpiarCampos() {
        txtNombre.clear();
        txtDescripcion.clear();
        cmbCategoria.setValue(null);
        txtPrecio.clear();
        txtCalorias.clear();
        txtGramaje.clear();
        chkDisponible.setSelected(true);
        ingredientesSeleccionados.clear();

        // Limpiar tamaños
        tamanosDefinidos.clear();
        txtTamNombre.clear();
        txtTamDescripcion.clear();
        txtTamPrecio.clear();
        txtTamCapacidad.clear();
        txtTamGramaje.clear();
        txtTamPiezas.clear();
        nextTamanoId = 1;

        modoEdicion = false;
        modoVisualizacion = false;
        idProductoEditando = 0;
        btnRegistrar.setText("💾 Guardar Producto");
        lblStatus.setText("Completa los campos marcados con * y guarda el producto");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(titulo);
            alert.setHeaderText(null);
            alert.setContentText(mensaje);
            alert.showAndWait();
        });
    }

    private String getIngredienteNombre(JSONObject ing) {
        // Intentar con diferentes nombres de campo
        if (ing.has("Nombre"))
            return ing.optString("Nombre", "");
        if (ing.has("nombre"))
            return ing.optString("nombre", "");
        return "";
    }

    private int getIngredienteId(JSONObject ing) {
        // Intentar con diferentes nombres de campo
        if (ing.has("ID"))
            return ing.optInt("ID", 0);
        if (ing.has("id"))
            return ing.optInt("id", 0);
        return 0;
    }
}