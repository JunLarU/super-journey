import java.net.http.HttpResponse;
import java.util.Optional;

import core.HTTPConnection;
import core.SessionManager;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import app.controllers.FXDialogs;

public class Main extends Application {
    private static final String APP_NAME = "CAFI";

    // Instancias de los Singleton utilizados para el almacenamiento de todos los
    // datos.
    // Se empieza cargando todo a memoria, para poderlo usar en la aplicación
    @Override
    public void start(Stage primaryStage) throws Exception {
        

        Task<HttpResponse<String>> task = HTTPConnection.getInstance().requestAsync(
                "",
                Optional.of(0),
                Optional.empty(),
                Optional.of(0),
                Optional.of("Error de conexión"),
                Optional.of("No se pudo conectar al servidor. Por favor, verifica tu conexión a internet.")
            );

        task.setOnSucceeded(e -> {
            initAndShowUI(primaryStage);
        });

        task.setOnFailed(e -> {
            // No necesitas mostrar Alert aquí, ya se mostró dentro del método
            System.out.println("Request falló");
        });

        new Thread(task, "http-task").start();

    }

    private void initAndShowUI(Stage primaryStage) {
        try{
            // Implementación del método si es necesario
            // System.out.println("🚀 Iniciando la aplicación...");

            // Carga la vista inicial (Login)
            String viewName = "sessions/Login";
            String fxmlPath = "/app/views/" + viewName + ".fxml";
            // System.out.println("Cargando vista: " + fxmlPath);

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // Configura la escena
            Scene scene = new Scene(root, 600, 500);
            scene.getStylesheets().add(getClass().getResource("/assets/css/app.css").toExternalForm());

            // Icono, título y propiedades de la ventana
            primaryStage.getIcons().add(
                    new javafx.scene.image.Image(getClass().getResourceAsStream("/assets/img/CAFI_LOGO.png")));
            primaryStage.setTitle(APP_NAME);
            primaryStage.setResizable(false);
            primaryStage.setScene(scene);
            primaryStage.show();

            // System.out.println("✅ Ventana iniciada correctamente: " +
            // primaryStage.getTitle());
        }catch(Exception e){
            e.printStackTrace();
            System.out.println("❌ Error al iniciar la interfaz: " + e.getMessage());
            FXDialogs.error(
                    "Error al iniciar la aplicación",
                    "No se pudo cargar la interfaz de usuario",
                    e.getMessage() != null ? e.getMessage() : "Error desconocido"
            );
        }

        // Hook para guardar datos al cerrar la aplicación
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // System.out.println("Guardando usuarios e ingredientes en JSON antes de
            // salir...");
            // System.out.println("Datos guardados correctamente.");
        }));

        // También guarda si se cierra la ventana manualmente
        primaryStage.setOnCloseRequest(event -> {
            // System.out.println("Evento de cierre detectado. Guardando usuarios e
            // ingredientes...");
            // System.out.println("Datos guardados correctamente.");
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
