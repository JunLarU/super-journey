package app.controllers.productos;

import core.SessionManager;
import core.data.Ingredientes.Ingrediente;
import core.data.Productos.*;
import core.services.CategoriaService;
import core.services.IngredienteService;
import core.services.ProductoService;

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

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RegistroProductoController {

    // ================= BASICOS =================
    @FXML
    private TextField txtNombre;
    @FXML
    private TextArea txtDescripcion;
    @FXML
    private ComboBox<CategoriaProducto> cmbCategoria;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TextField txtCalorias;
    @FXML
    private TextField txtGramaje;
    @FXML
    private CheckBox chkDisponible;

    @FXML
    private Label lblTitulo;
    @FXML
    private Label lblStatus;
    @FXML
    private VBox vboxInfo;

    @FXML
    private Button btnLimpiar;
    @FXML
    private Button btnRegistrar;

    // ================= INGREDIENTES =================
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

    // ================= TAMAÑOS =================
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

    // ================= ESTADO =================
    private final SessionManager session = SessionManager.getInstance();

    private final ObservableList<ProductoIngrediente> ingredientes = FXCollections.observableArrayList();
    private final ObservableList<TamanoProducto> tamanos = FXCollections.observableArrayList();
    private final ObservableList<CategoriaProducto> categorias = FXCollections.observableArrayList();

    private boolean modoEdicion = false;
    private boolean modoVisualizacion = false;
    private int idProducto = 0;
    private int nextTamanoId = 1;

    @FXML
    public void initialize() {
        if (!session.isAdmin()) {
            mostrarAlerta("Acceso denegado", "Solo administradores.");
            return;
        }

        configurarValidaciones();
        configurarCategoriasDesdeBD();

        configurarTablaIngredientes(); // ✅ aqui se arregla “Sustituible”
        configurarBusquedaIngredientes();
        configurarTamanos();
    }

    // =====================================================
    // CATEGORIAS DESDE BD
    // =====================================================
    private void configurarCategoriasDesdeBD() {

        CategoriaService.listCategoriasProductos(new CategoriaService.ListCallback() {
            @Override
            public void onSuccess(List<JSONObject> list) {
                var nombres = list.stream()
                        .map(j -> j.getString("Nombre"))
                        .toList();

                javafx.application.Platform.runLater(() -> cmbCategoria.getItems().setAll(nombres));
            }

            @Override
            public void onError(String error) {
                javafx.application.Platform.runLater(() -> lblStatus.setText("❌ " + error));
            }
        });
    }

    // =====================================================
    // TABLA INGREDIENTES (FIX Sustituible + Acciones + Conteo)
    // =====================================================
    private void configurarTablaIngredientes() {

        // ✅ NECESARIO para CheckBoxTableCell
        tablaIngredientes.setEditable(true);
        colIngEliminar.setEditable(true);
        colIngSustituible.setEditable(true);

        colIngNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreIngrediente()));

        colIngSustitutos.setCellValueFactory(d -> {
            int n = d.getValue().getSustitutos() == null ? 0 : d.getValue().getSustitutos().size();
            return new SimpleStringProperty(n == 0 ? "-" : (n + " sustituto(s)"));
        });

        colIngEliminar.setCellValueFactory(d -> {
            ProductoIngrediente ing = d.getValue();
            SimpleBooleanProperty p = new SimpleBooleanProperty(ing.isEliminable());
            p.addListener((o, ov, nv) -> ing.setEliminable(nv));
            return p;
        });
        colIngEliminar.setCellFactory(CheckBoxTableCell.forTableColumn(colIngEliminar));

        colIngSustituible.setCellValueFactory(d -> {
            ProductoIngrediente ing = d.getValue();
            SimpleBooleanProperty p = new SimpleBooleanProperty(ing.isSustituible());
            p.addListener((o, ov, nv) -> {
                ing.setSustituible(nv);

                // si lo desmarcan, puedes opcionalmente limpiar sustitutos
                // if (!nv) ing.setSustitutos(new ArrayList<>());
                tablaIngredientes.refresh();
            });
            return p;
        });
        colIngSustituible.setCellFactory(CheckBoxTableCell.forTableColumn(colIngSustituible));

        // Acciones: Eliminar + ♻️
        colIngAcciones.setCellFactory(tc -> new TableCell<>() {

            private final Button btnEliminar = new Button("Eliminar");
            private final Button btnSustituir = new Button("♻️");

            {
                btnEliminar.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;");
                btnSustituir.setStyle("-fx-background-color:#3498db;-fx-text-fill:white;");

                btnEliminar.setOnAction(e -> {
                    ProductoIngrediente ing = getTableView().getItems().get(getIndex());
                    ingredientes.remove(ing);
                    lblStatus.setText("🗑️ Ingrediente eliminado: " + ing.getNombreIngrediente());
                });

                btnSustituir.setOnAction(e -> {
                    ProductoIngrediente ing = getTableView().getItems().get(getIndex());
                    if (!ing.isSustituible()) {
                        mostrarAlerta("No sustituible", "Primero marca el checkbox 'Sustituible'.");
                        return;
                    }
                    mostrarDialogoSustitutos(ing);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(6, btnEliminar, btnSustituir);
                box.setAlignment(Pos.CENTER);
                setGraphic(box);
            }
        });

        tablaIngredientes.setItems(ingredientes);
    }

    // =====================================================
    // BUSQUEDA INGREDIENTES (API)
    // =====================================================
    private void configurarBusquedaIngredientes() {

        txtBuscarIngrediente.textProperty().addListener((o, ov, nv) -> {
            if (nv == null || nv.trim().length() < 2) {
                listaIngredientesBuscados.getItems().clear();
                return;
            }

            IngredienteService.listIngredientes(new IngredienteService.ListCallback() {
                @Override
                public void onSuccess(List<JSONObject> list) {

                    String q = nv.toLowerCase().trim();

                    List<String> nombres = list.stream()
                            .map(Ingrediente::new)
                            .map(Ingrediente::getNombre)
                            .filter(n -> n.toLowerCase().contains(q))
                            .filter(n -> ingredientes.stream()
                                    .noneMatch(i -> i.getNombreIngrediente().equalsIgnoreCase(n)))
                            .collect(Collectors.toList());

                    Platform.runLater(() -> listaIngredientesBuscados.getItems().setAll(nombres));
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> lblStatus.setText("❌ " + error));
                }
            });
        });

        listaIngredientesBuscados.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                String n = listaIngredientesBuscados.getSelectionModel().getSelectedItem();
                if (n == null)
                    return;

                // OJO: aquí aún no tienes el ID del ingrediente si tu API no lo trae por
                // nombre.
                // Lo correcto es que IngredienteService retorne objetos completos y selecciones
                // el ID.
                ingredientes.add(new ProductoIngrediente(
                        0, n, 0.0, false, false, ingredientes.size() + 1));

                listaIngredientesBuscados.getItems().remove(n);
                txtBuscarIngrediente.clear();
                lblStatus.setText("✅ Ingrediente agregado: " + n);
            }
        });
    }

    // =====================================================
    // DIALOGO SUSTITUTOS (mínimo funcional)
    // =====================================================
    private void mostrarDialogoSustitutos(ProductoIngrediente ingrediente) {

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Sustitutos de: " + ingrediente.getNombreIngrediente());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));

        TextField txtBuscar = new TextField();
        txtBuscar.setPromptText("Buscar ingrediente sustituto...");

        ListView<String> lista = new ListView<>();
        lista.setPrefHeight(160);

        TableView<Sustituto> tabla = new TableView<>();
        tabla.setPrefHeight(160);

        TableColumn<Sustituto, String> cNom = new TableColumn<>("Ingrediente");
        cNom.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombreIngrediente()));
        cNom.setPrefWidth(280);

        TableColumn<Sustituto, String> cCosto = new TableColumn<>("Costo");
        cCosto.setCellValueFactory(d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getCostoExtra())));
        cCosto.setPrefWidth(100);

        TableColumn<Sustituto, Void> cAcc = new TableColumn<>("Quitar");
        cAcc.setCellFactory(tc -> new TableCell<>() {
            private final Button b = new Button("❌");
            {
                b.setOnAction(e -> tabla.getItems().remove(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : b);
            }
        });
        cAcc.setPrefWidth(70);

        tabla.getColumns().addAll(cNom, cCosto, cAcc);

        ObservableList<Sustituto> actuales = FXCollections.observableArrayList(
                ingrediente.getSustitutos() == null ? new ArrayList<>() : ingrediente.getSustitutos());
        tabla.setItems(actuales);

        TextField txtCosto = new TextField("0.00");
        txtCosto.setPrefWidth(90);

        Button btnAdd = new Button("➕ Agregar");
        btnAdd.setOnAction(e -> {
            String sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null)
                return;

            double costo;
            try {
                costo = Double.parseDouble(txtCosto.getText().trim());
            } catch (Exception ex) {
                mostrarAlerta("Costo inválido", "Debe ser número");
                return;
            }

            actuales.add(new Sustituto(0, sel, costo, true));
            lista.getItems().remove(sel);
            txtCosto.setText("0.00");
        });

        Button btnGuardar = new Button("💾 Guardar");
        btnGuardar.setStyle("-fx-background-color:#2ecc71;-fx-text-fill:white;");
        btnGuardar.setOnAction(e -> {
            ingrediente.setSustitutos(new ArrayList<>(actuales));
            tablaIngredientes.refresh();
            dialog.close();
        });

        // búsqueda API
        txtBuscar.textProperty().addListener((o, ov, nv) -> {
            if (nv == null || nv.trim().length() < 2) {
                lista.getItems().clear();
                return;
            }

            IngredienteService.listIngredientes(new IngredienteService.ListCallback() {
                @Override
                public void onSuccess(List<JSONObject> list) {
                    String q = nv.toLowerCase().trim();
                    List<String> nombres = list.stream()
                            .map(Ingrediente::new)
                            .map(Ingrediente::getNombre)
                            .filter(n -> n.toLowerCase().contains(q))
                            .filter(n -> !n.equalsIgnoreCase(ingrediente.getNombreIngrediente()))
                            .filter(n -> actuales.stream().noneMatch(s -> s.getNombreIngrediente().equalsIgnoreCase(n)))
                            .toList();

                    Platform.runLater(() -> lista.getItems().setAll(nombres));
                }

                @Override
                public void onError(String error) {
                    Platform.runLater(() -> lblStatus.setText("❌ " + error));
                }
            });
        });

        HBox addBox = new HBox(8, new Label("Costo:"), txtCosto, btnAdd);
        addBox.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(
                new Label("Buscar sustituto:"),
                txtBuscar,
                lista,
                new Separator(),
                addBox,
                new Separator(),
                new Label("Sustitutos actuales:"),
                tabla,
                new Separator(),
                btnGuardar);

        dialog.getDialogPane().setContent(root);
        dialog.showAndWait();
    }

    // =====================================================
    // TAMAÑOS (igual que traías)
    // =====================================================
    private void configurarTamanos() {

        colTamNombre.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNombre()));
        colTamDescripcion.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescripcion()));
        colTamPrecio
                .setCellValueFactory(d -> new SimpleStringProperty(String.format("$%.2f", d.getValue().getPrecio())));

        tablaTamanos.setItems(tamanos);

        btnAgregarTamano.setOnAction(e -> {
            if (txtTamNombre.getText().isBlank() || txtTamPrecio.getText().isBlank())
                return;

            double precio;
            try {
                precio = Double.parseDouble(txtTamPrecio.getText().trim());
            } catch (Exception ex) {
                mostrarAlerta("Precio inválido", "Debe ser número");
                return;
            }

            double capacidad = txtTamCapacidad.getText().isBlank() ? 0 : Double.parseDouble(txtTamCapacidad.getText());
            double gramaje = txtTamGramaje.getText().isBlank() ? 0 : Double.parseDouble(txtTamGramaje.getText());
            int piezas = txtTamPiezas.getText().isBlank() ? 0 : Integer.parseInt(txtTamPiezas.getText());

            tamanos.add(new TamanoProducto(
                    nextTamanoId++,
                    txtTamNombre.getText().trim(),
                    txtTamDescripcion.getText().trim(),
                    capacidad,
                    gramaje,
                    piezas,
                    precio,
                    tamanos.size() + 1,
                    true));

            txtTamNombre.clear();
            txtTamDescripcion.clear();
            txtTamPrecio.clear();
            txtTamCapacidad.clear();
            txtTamGramaje.clear();
            txtTamPiezas.clear();
        });
    }

    // =====================================================
    // GUARDAR (manda idCategoria)
    // =====================================================
    @FXML
    private void onRegistrarClicked() {

        if (txtNombre.getText().isBlank()) {
            mostrarAlerta("Falta nombre", "Nombre requerido");
            return;
        }
        if (cmbCategoria.getValue() == null) {
            mostrarAlerta("Falta categoría", "Selecciona categoría");
            return;
        }
        if (txtPrecio.getText().isBlank()) {
            mostrarAlerta("Falta precio", "Precio requerido");
            return;
        }

        double precio;
        try {
            precio = Double.parseDouble(txtPrecio.getText().trim());
        } catch (Exception ex) {
            mostrarAlerta("Precio inválido", "Debe ser número");
            return;
        }

        CategoriaProducto cat = cmbCategoria.getValue();

        Producto p = new Producto();
        p.setId(modoEdicion ? idProducto : 0);
        p.setNombre(txtNombre.getText().trim());
        p.setDescripcion(txtDescripcion.getText().trim());
        p.setPrecioBase(precio);

        // ✅ lo importante:
        p.setIdCategoria(cat.getId());
        p.setCategoria(cat.getNombre()); // opcional, para UI

        p.setCalorias(txtCalorias.getText().isBlank() ? 0 : Double.parseDouble(txtCalorias.getText()));
        p.setGramaje(txtGramaje.getText().isBlank() ? 0 : Double.parseDouble(txtGramaje.getText()));
        p.setDisponible(chkDisponible.isSelected());

        p.setIngredientes(new ArrayList<>(ingredientes));
        p.setTamanos(new ArrayList<>(tamanos));

        ProductoService.saveProducto(
                p.toJson(), // asegúrate que incluya idCategoria
                () -> Platform.runLater(() -> lblStatus.setText("✅ Producto guardado")),
                err -> Platform.runLater(() -> lblStatus.setText("❌ " + err)));
    }

    // =====================================================
    // CARGAR / VISUALIZAR
    // =====================================================
    public void cargarDatosExistentes(Producto p) {
        modoEdicion = true;
        idProducto = p.getId();

        txtNombre.setText(p.getNombre());
        txtDescripcion.setText(p.getDescripcion());
        txtPrecio.setText(String.valueOf(p.getPrecioBase()));
        txtCalorias.setText(String.valueOf(p.getCalorias()));
        txtGramaje.setText(String.valueOf(p.getGramaje()));
        chkDisponible.setSelected(p.isDisponible());

        // Seleccionar categoría por ID si ya la tienes
        if (p.getIdCategoria() > 0) {
            categorias.stream()
                    .filter(c -> c.getId() == p.getIdCategoria())
                    .findFirst()
                    .ifPresent(c -> cmbCategoria.setValue(c));
        }

        ingredientes.setAll(p.getIngredientes());
        tamanos.setAll(p.getTamanos());
        tablaIngredientes.refresh();
        tablaTamanos.refresh();
    }

    public void visualizarProducto(Producto p) {
        cargarDatosExistentes(p);
        modoVisualizacion = true;

        btnRegistrar.setVisible(false);
        btnLimpiar.setVisible(false);

        // deshabilitar edición
        tablaIngredientes.setEditable(false);
        tablaTamanos.setEditable(false);
        vboxInfo.setDisable(true);
    }

    // =====================================================
    // LIMPIAR (requerido por FXML)
    // =====================================================
    @FXML
    private void limpiarCampos() {

        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecio.clear();
        txtCalorias.clear();
        txtGramaje.clear();
        chkDisponible.setSelected(true);

        txtBuscarIngrediente.clear();
        listaIngredientesBuscados.getItems().clear();

        ingredientes.clear();
        tamanos.clear();

        txtTamNombre.clear();
        txtTamDescripcion.clear();
        txtTamPrecio.clear();
        txtTamCapacidad.clear();
        txtTamGramaje.clear();
        txtTamPiezas.clear();

        modoEdicion = false;
        modoVisualizacion = false;
        idProducto = 0;
        nextTamanoId = 1;

        lblStatus.setText("🧹 Campos limpiados");
    }

    // =====================================================
    // VALIDACIONES
    // =====================================================
    private void configurarValidaciones() {
        txtPrecio.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?"))
                txtPrecio.setText(old);
        });
        txtCalorias.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?"))
                txtCalorias.setText(old);
        });
        txtGramaje.textProperty().addListener((obs, old, nw) -> {
            if (!nw.matches("\\d*(\\.\\d*)?"))
                txtGramaje.setText(old);
        });
    }

    private void mostrarAlerta(String t, String m) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle(t);
        a.setHeaderText(null);
        a.setContentText(m);
        a.showAndWait();
    }
}
