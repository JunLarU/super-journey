package core.data.Users;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Clase Singleton para gestionar todos los usuarios.
 * Carga los datos desde un archivo JSON al iniciar y los guarda automáticamente al cerrar.
 */
public class AllUsers {
    private static AllUsers instance;
    private List<User> users;
    private static final String FILE_NAME = "data/users.json";

    // Constructor privado
    private AllUsers() {
        users = new ArrayList<>();
        loadUsers(); // intenta cargar desde archivo

        if (users.isEmpty()) {
            //System.out.println("⚠️ No hay usuarios cargados. Creando usuario de prueba...");
            User testUser = new User("testuser", "testpassword", "Juan", "Larios", "Estrada",
                                     "juan.larios@example.com", "5551234567");
            testUser.setClave("U001");
            testUser.setAdmin(true);
            users.add(testUser);
            saveUsers(); // crea el archivo si no existía
        }

        // Registrar hook para guardar antes de cerrar la JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            //System.out.println("🧩 Guardando usuarios antes de salir...");
            saveUsers();
        }));
    }

    // Obtener instancia única
    public static AllUsers getInstance() {
        if (instance == null) {
            instance = new AllUsers();
        }
        return instance;
    }

    // Agregar usuario
    public void addUser(User user) {
        users.add(user);
        saveUsers();
    }

    // Obtener todos los usuarios
    public List<User> getUsers() {
        return users;
    }

    // Buscar usuario por nombre de usuario
    public User getUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username)) {
                return user;
            }
        }
        return null;
    }

    // Buscar usuario por clave
    public User getUserByClave(String clave) {
        for (User user : users) {
            if (user.getClave() != null && user.getClave().equalsIgnoreCase(clave)) {
                return user;
            }
        }
        return null;
    }

    // Validar credenciales
    public boolean validateCredentials(String clave, String password) {
        User user = getUserByClave(clave);
        return user != null && user.getPassword().equals(password);
    }

    // Cargar usuarios desde JSON
    private void loadUsers() {
        File file = new File(FILE_NAME);

        if (!file.exists()) {
            //System.out.println("⚠️ Archivo " + FILE_NAME + " no existe. Se creará automáticamente.");
            saveUsers(); // crea un archivo vacío o con usuarios iniciales
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder jsonText = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonText.append(line);
            }

            JSONArray array = new JSONArray(jsonText.toString());
            users.clear();
            for (int i = 0; i < array.length(); i++) {
                users.add(User.fromJSON(array.getJSONObject(i)));
            }

            //System.out.println("✅ Usuarios cargados desde " + FILE_NAME + ": " + users.size());
        } catch (Exception e) {
            //System.err.println("⚠️ Error al cargar usuarios: " + e.getMessage());
        }
    }

    // Guardar usuarios en JSON
    public synchronized void saveUsers() {
        JSONArray array = new JSONArray();
        for (User u : users) {
            array.put(u.toJSON());
        }

        try (FileWriter writer = new FileWriter(FILE_NAME)) {
            writer.write(array.toString(4)); // formato bonito
            //System.out.println("💾 Usuarios guardados en " + FILE_NAME);
        } catch (Exception e) {
            //System.err.println("⚠️ Error al guardar usuarios: " + e.getMessage());
        }
    }
}
