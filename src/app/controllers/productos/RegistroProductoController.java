package app.controllers.productos;

import core.SessionManager;
import core.data.Ingredientes.AllIngredientes;
import core.data.Ingredientes.Ingrediente;
import core.data.Productos.AllProductos;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador del formulario de registro/edición de productos.
 * Ahora usa AllProductos y AllIngredientes localmente.
 */
public class RegistroProductoController {

    // =========================
    // 🔹 Campos principales
    // =========================
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private ComboBox<String> cmbCategoria;
    @FXML private TextField txtPrecio;
    @FXML private TextField txtCalorias;
    @FXML private TextField txtGramaje;
    @FXML private CheckBox chkDisponible;
    @FXML private Button btnLimpiar;

    // =========================
    // 🔹 Campos para control de visualización
    // =========================
    @FXML private Label lblTitulo;
    @FXML private VBox vboxInfo;

    // =========================
    // 🔹 Ingredientes
    // =========================
    @FXML private TextField txtBuscarIngrediente;
    @FXML private ListView<String> listaIngredientesBuscados;
    @FXML private TableView<ProductoIngrediente> tablaIngredientes;
    @FXML private TableColumn<ProductoIngrediente, String> colIngNombre;
    @FXML private TableColumn<ProductoIngrediente, String> colIngSustitutos;
    @FXML private TableColumn<ProductoIngrediente, Boolean> colIngEliminar;
    @FXML private TableColumn<ProductoIngrediente, Boolean> colIngSustituible;
    @FXML private TableColumn<ProductoIngrediente, Void> colIngAcciones;

    // =========================
    // 🔹 Tamaños (NUEVO - ahora funcional)
    // =========================
    @FXML private TableView<TamanoProducto> tablaTamanos;
    @FXML private TableColumn<TamanoProducto, String> colTamNombre;
    @FXML private TableColumn<TamanoProducto, String> colTamDescripcion;
    @FXML private TableColumn<TamanoProducto, String> colTamPrecio;
    @FXML private TableColumn<TamanoProducto, Void> colTamAcciones;
    
    @FXML private TextField txtTamNombre;
    @FXML private TextField txtTamDescripcion;
    @FXML private TextField txtTamPrecio;
    @FXML private TextField txtTamCapacidad;
    @FXML private TextField txtTamGramaje;
    @FXML private TextField txtTamPiezas;
    @FXML private Button btnAgregarTamano;

    @FXML private Button btnRegistrar;
    @FXML private Label lblStatus;

    // =========================
    // 🔹 Objetos de datos
    // =========================
    private final AllProductos allProductos = AllProductos.getInstance();
    private final AllIngredientes allIngredientes = AllIngredientes.getInstance();
    private final SessionManager session = SessionManager.getInstance();

    private final ObservableList<ProductoIngrediente> ingredientesSeleccionados = FXCollections.observableArrayList();
    private final ObservableList<TamanoProducto> tamanosDefinidos = FXCollections.observableArrayList(); // NUEVO

    // Control de modo edición
    private boolean modoEdicion = false;
    private boolean modoVisualizacion = false;
    private int idProductoEditando = 0;
    private int nextTamanoId = 1; // NUEVO: Para generar IDs de tamaños

    // =========================
    // 🔹 Categorías hardcoded
    // =========================
    private final ObservableList<String> categoriasDisponibles = FXCollections.observableArrayList(
            "Desayuno Mexicano", "Desayuno Continental", "Desayuno Express",
            "Plato Fuerte", "Antojitos Mexicanos", "Hamburguesas", "Tortas y Sandwiches",
            "Ensaladas", "Sopas y Cremas", "Pastas", "Alitas y Boneless",
            "Guarniciones", "Extras", "Postres", "Repostería",
            "Café", "Té e Infusiones", "Chocolate Caliente", "Bebidas de Temporada Calientes",
            "Café Frío", "Smoothies", "Jugos y Licuados", "Aguas Frescas", "Refrescos",
            "Bebidas Energéticas", "Bebidas de Temporada Frías",
            "Snacks Dulces", "Snacks Salados", "Panadería", "Baguettes y Croissants", "Yogurt y Parfait");

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

        configurarCategorias();
        configurarTablas();
        configurarBusquedaIngredientes();
        configurarValidaciones();
        configurarTamanos(); // NUEVO
    }

    // ---------------------------------------------------
    // ⚙️ Configuración de Tamaños (NUEVO)
    // ---------------------------------------------------
    private void configurarTamanos() {
        // Configurar tabla de tamaños
        colTamNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombre()));
        
        colTamDescripcion.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getDescripcion()));
        
        colTamPrecio.setCellValueFactory(data -> 
            new SimpleStringProperty(String.format("$%.2f", data.getValue().getPrecio())));

        colTamAcciones.setCellFactory(tc -> new TableCell<>() {
            private final Button btnEliminar = new Button("Eliminar");
            {
                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnEliminar.setTooltip(new Tooltip("Eliminar tamaño"));
                btnEliminar.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < tamanosDefinidos.size()) {
                        String nombreTam = tamanosDefinidos.get(idx).getNombre();
                        tamanosDefinidos.remove(idx);
                        lblStatus.setText("Eliminar Tamaño eliminado: " + nombreTam);
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
    // ➕ Agregar Tamaño (NUEVO)
    // ---------------------------------------------------
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
            double capacidad = txtTamCapacidad.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtTamCapacidad.getText().trim());
            double gramaje = txtTamGramaje.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtTamGramaje.getText().trim());
            int piezas = txtTamPiezas.getText().trim().isEmpty() ? 0 : Integer.parseInt(txtTamPiezas.getText().trim());

            // Crear nuevo tamaño
            TamanoProducto tamano = new TamanoProducto(
                nextTamanoId++, // ID autoincremental
                nombre,
                descripcion,
                capacidad,
                gramaje,
                piezas,
                precio,
                tamanosDefinidos.size() + 1, // orden
                true // disponible por defecto
            );

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
    // ⚙️ Configuración de Categorías
    // ---------------------------------------------------
    private void configurarCategorias() {
        cmbCategoria.setEditable(true);
        cmbCategoria.setItems(categoriasDisponibles);

        final boolean[] isUpdating = { false };

        cmbCategoria.getEditor().textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdating[0]) return;

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
    // ⚙️ Configuración de Tablas
    // ---------------------------------------------------
    private void configurarTablas() {
        tablaIngredientes.setEditable(true);

        // Columna: Nombre
        colIngNombre.setCellValueFactory(data -> 
            new SimpleStringProperty(data.getValue().getNombreIngrediente()));

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
                //System.out.println("✅ Eliminable actualizado: " + ing.getNombreIngrediente() + " = " + newVal);
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
                //System.out.println("✅ Sustituible actualizado: " + ing.getNombreIngrediente() + " = " + newVal);
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
                btnEliminar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");
                btnSustituir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 5;");

                btnEliminar.setTooltip(new Tooltip("Eliminar ingrediente"));
                btnSustituir.setTooltip(new Tooltip("Definir sustitutos"));

                btnEliminar.setOnAction(e -> {
                    int idx = getIndex();
                    if (idx >= 0 && idx < ingredientesSeleccionados.size()) {
                        String nombreIng = ingredientesSeleccionados.get(idx).getNombreIngrediente();
                        ingredientesSeleccionados.remove(idx);
                        lblStatus.setText("Eliminar Ingrediente eliminado: " + nombreIng);
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
        new Thread(() -> {
            try {
                List<Ingrediente> todos = allIngredientes.getAll();
                String queryLower = query.toLowerCase();
                
                List<String> resultados = todos.stream()
                    .filter(ing -> ing.getNombre().toLowerCase().contains(queryLower))
                    .map(Ingrediente::getNombre)
                    .filter(nombre -> {
                        // Filtrar los que ya están seleccionados
                        return ingredientesSeleccionados.stream()
                                .noneMatch(pi -> pi.getNombreIngrediente().equalsIgnoreCase(nombre));
                    })
                    .collect(Collectors.toList());

                Platform.runLater(() -> {
                    listaIngredientesBuscados.getItems().clear();
                    listaIngredientesBuscados.getItems().addAll(resultados);
                });
            } catch (Exception e) {
                Platform.runLater(() -> lblStatus.setText("❌ Error buscando ingredientes: " + e.getMessage()));
            }
        }).start();
    }

    private void agregarIngredienteSeleccionado(String nombreIngrediente) {
        // Buscar el ingrediente en AllIngredientes para obtener su ID
        Ingrediente ing = allIngredientes.getByNombre(nombreIngrediente);
        
        if (ing == null) {
            mostrarAlerta("Error", "No se encontró el ingrediente: " + nombreIngrediente);
            return;
        }

        ProductoIngrediente pi = new ProductoIngrediente(
            ing.getId(),
            nombreIngrediente,
            0.0, // cantidad por defecto
            false, // no eliminable por defecto
            false, // no sustituible por defecto
            ingredientesSeleccionados.size() + 1 // orden
        );

        ingredientesSeleccionados.add(pi);
        listaIngredientesBuscados.getItems().remove(nombreIngrediente);
        txtBuscarIngrediente.clear();
        lblStatus.setText("✅ Ingrediente agregado: " + nombreIngrediente);
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
        colCosto.setCellValueFactory(d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getCostoExtra())));

        TableColumn<Sustituto, Void> colAccion = new TableColumn<>("Acción");
        colAccion.setPrefWidth(80);
        colAccion.setCellFactory(tc -> new TableCell<>() {
            private final Button btnQuitar = new Button("❌");
            {
                btnQuitar.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                btnQuitar.setOnAction(e -> tablaSustitutos.getItems().remove(getTableView().getItems().get(getIndex())));
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
        txtBuscar.textProperty().addListener((o, ov, nv) -> {
            if (nv.length() > 1) {
                new Thread(() -> {
                    try {
                        List<Ingrediente> todos = allIngredientes.getAll();
                        String queryLower = nv.toLowerCase();
                        
                        List<String> resultados = todos.stream()
                            .filter(ing -> ing.getNombre().toLowerCase().contains(queryLower))
                            .filter(ing -> !ing.getNombre().equalsIgnoreCase(ingrediente.getNombreIngrediente()))
                            .map(Ingrediente::getNombre)
                            .filter(nombre -> {
                                // Filtrar los que ya están agregados como sustitutos
                                return listaSustitutos.stream()
                                        .noneMatch(s -> s.getNombreIngrediente().equalsIgnoreCase(nombre));
                            })
                            .collect(Collectors.toList());

                        Platform.runLater(() -> {
                            listaBusqueda.getItems().clear();
                            listaBusqueda.getItems().addAll(resultados);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> lblStatus.setText("Error: " + ex.getMessage()));
                    }
                }).start();
            } else {
                listaBusqueda.getItems().clear();
            }
        });

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
                Ingrediente ingSustituto = allIngredientes.getByNombre(seleccionado);
                if (ingSustituto == null) {
                    mostrarAlerta("Error", "No se encontró el ingrediente.");
                    return;
                }

                Sustituto nuevoSustituto = new Sustituto(
                    ingSustituto.getId(),
                    seleccionado,
                    costo,
                    true
                );

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

        // NUEVO: Validaciones para campos de tamaños
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
    // 💾 Guardar producto
    // ---------------------------------------------------
    @FXML
    private void onRegistrarClicked() {
        if (!validarCampos()) return;

        btnRegistrar.setDisable(true);
        lblStatus.setText("Guardando producto...");

        new Thread(() -> {
            try {
                String nombre = txtNombre.getText().trim();
                String descripcion = txtDescripcion.getText().trim();
                String categoria = cmbCategoria.getValue();
                double precio = Double.parseDouble(txtPrecio.getText().trim());
                double calorias = txtCalorias.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtCalorias.getText().trim());
                double gramaje = txtGramaje.getText().trim().isEmpty() ? 0.0 : Double.parseDouble(txtGramaje.getText().trim());
                boolean disponible = chkDisponible.isSelected();

                if (modoEdicion) {
                    // Actualizar producto existente
                    Producto producto = allProductos.getById(idProductoEditando);
                    if (producto == null) {
                        Platform.runLater(() -> {
                            lblStatus.setText("❌ Producto no encontrado.");
                            btnRegistrar.setDisable(false);
                        });
                        return;
                    }

                    producto.setNombre(nombre);
                    producto.setDescripcion(descripcion);
                    producto.setCategoria(categoria);
                    producto.setPrecioBase(precio);
                    producto.setCalorias(calorias);
                    producto.setGramaje(gramaje);
                    producto.setDisponible(disponible);
                    producto.setIngredientes(new ArrayList<>(ingredientesSeleccionados));
                    producto.setTamanos(new ArrayList<>(tamanosDefinidos)); // NUEVO: Guardar tamaños

                    allProductos.updateProducto(producto);

                    Platform.runLater(() -> {
                        lblStatus.setText("✅ Producto actualizado correctamente.");
                        btnRegistrar.setDisable(false);
                    });
                } else {
                    // Verificar que no exista un producto con el mismo nombre
                    Producto existente = allProductos.getByNombre(nombre);
                    if (existente != null) {
                        Platform.runLater(() -> {
                            lblStatus.setText("⚠️ Ya existe un producto con ese nombre.");
                            btnRegistrar.setDisable(false);
                        });
                        return;
                    }

                    // Crear nuevo producto
                    Producto nuevoProducto = new Producto(
                        0, // AllProductos asignará el ID
                        nombre,
                        descripcion,
                        precio,
                        categoria,
                        gramaje,
                        calorias,
                        "", // urlFoto vacía por ahora
                        disponible
                    );

                    // Agregar ingredientes
                    nuevoProducto.setIngredientes(new ArrayList<>(ingredientesSeleccionados));
                    // NUEVO: Agregar tamaños
                    nuevoProducto.setTamanos(new ArrayList<>(tamanosDefinidos));

                    allProductos.addProducto(nuevoProducto);

                    Platform.runLater(() -> {
                        lblStatus.setText("✅ Producto registrado correctamente.");
                        limpiarCampos();
                        btnRegistrar.setDisable(false);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    lblStatus.setText("❌ Error: " + e.getMessage());
                    btnRegistrar.setDisable(false);
                });
            }
        }).start();
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
    // 📥 Cargar datos existentes
    // ---------------------------------------------------
    public void cargarDatosExistentes(Producto producto) {
        if (producto == null) {
            //System.err.println("⚠️ Producto nulo recibido");
            return;
        }

        //System.out.println("🔍 Cargando producto: " + producto.getNombre());
        lblTitulo.setText("Editar Producto");
        modoEdicion = true;
        idProductoEditando = producto.getId();

        // Datos básicos
        txtNombre.setText(producto.getNombre());
        txtDescripcion.setText(producto.getDescripcion());
        cmbCategoria.setValue(producto.getCategoria());
        txtPrecio.setText(String.valueOf(producto.getPrecioBase()));
        txtCalorias.setText(producto.getCalorias() > 0 ? String.valueOf(producto.getCalorias()) : "");
        txtGramaje.setText(producto.getGramaje() > 0 ? String.valueOf(producto.getGramaje()) : "");
        chkDisponible.setSelected(producto.isDisponible());

        // Cargar ingredientes
        ingredientesSeleccionados.clear();
        ingredientesSeleccionados.addAll(producto.getIngredientes());

        // NUEVO: Cargar tamaños
        tamanosDefinidos.clear();
        tamanosDefinidos.addAll(producto.getTamanos());
        
        // Actualizar el nextTamanoId basado en los tamaños existentes
        if (!tamanosDefinidos.isEmpty()) {
            nextTamanoId = tamanosDefinidos.stream()
                    .mapToInt(TamanoProducto::getId)
                    .max()
                    .orElse(0) + 1;
        }

        // Actualizar UI
        btnRegistrar.setText("💾 Actualizar Producto");
        lblStatus.setText("📝 Editando producto: " + producto.getNombre());

        Platform.runLater(() -> {
            tablaIngredientes.refresh();
            tablaTamanos.refresh(); // NUEVO: Refrescar tabla de tamaños
        });
    }

    // ---------------------------------------------------
    // visualizar producto (solo lectura)
    // ---------------------------------------------------
    public void visualizarProducto(Producto producto) {
        cargarDatosExistentes(producto);
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

        // NUEVO: Deshabilitar campos de tamaños
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

        // NUEVO: Deshabilitar tabla de tamaños
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

        lblStatus.setText("visualizando producto: " + producto.getNombre());
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
        
        // NUEVO: Limpiar tamaños
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
        lblStatus.setText("Editar Completa los campos marcados con * y guarda el producto");
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
}