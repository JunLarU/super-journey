import core.SessionManager;
import core.data.Users.AllUsers;
import core.data.Avisos.AllAvisos;
import core.data.Ingredientes.AllIngredientes;
import core.data.Menus.AllMenus;
import core.data.Productos.AllProductos;
import core.data.Productos.AllProductosEspeciales;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    private static final String APP_NAME = "CAFI";

    // Instancias de los Singleton utilizados para el almacenamiento de todos los datos.
    // Se empieza cargando todo a memoria, para poderlo usar en la aplicación
    private final AllUsers allUsers = AllUsers.getInstance();
    private final AllIngredientes allIngredientes = AllIngredientes.getInstance();
    private final AllProductos allProductos = AllProductos.getInstance();
    private final AllMenus allMenus = AllMenus.getInstance();
    private final AllProductosEspeciales allProductosEspeciales = AllProductosEspeciales.getInstance();
    private final AllAvisos allAvisos = AllAvisos.getInstance();
    private final SessionManager sessionManager = SessionManager.getInstance();


    @Override
    public void start(Stage primaryStage) throws Exception {
        //System.out.println("🚀 Iniciando la aplicación...");

        // Carga la vista inicial (Login)
        String viewName = "sessions/Login";
        String fxmlPath = "/app/views/" + viewName + ".fxml";
        //System.out.println("Cargando vista: " + fxmlPath);

        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        Parent root = loader.load();

        // Configura la escena
        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/assets/css/app.css").toExternalForm());

        // Icono, título y propiedades de la ventana
        primaryStage.getIcons().add(
            new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/img/CAFI_LOGO.png"))
        );
        primaryStage.setTitle(APP_NAME);
        primaryStage.setResizable(false);
        primaryStage.setScene(scene);
        primaryStage.show();

        //System.out.println("✅ Ventana iniciada correctamente: " + primaryStage.getTitle());

        // Hook para guardar datos al cerrar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //System.out.println("Guardando usuarios e ingredientes en JSON antes de salir...");
            allUsers.saveUsers();
            allIngredientes.saveToFile();
            allProductos.saveToFile();
            allMenus.saveToFile();
            allProductosEspeciales.saveToFile();
            allAvisos.saveToFile();
            //System.out.println("Datos guardados correctamente.");
        }));

        // También guarda si se cierra la ventana manualmente
        primaryStage.setOnCloseRequest(event -> {
            //System.out.println("Evento de cierre detectado. Guardando usuarios e ingredientes...");
            allUsers.saveUsers();
            allProductos.saveToFile();
            allIngredientes.saveToFile();
            allMenus.saveToFile();
            allProductosEspeciales.saveToFile();
            allAvisos.saveToFile();
            //System.out.println("Datos guardados correctamente.");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
