package core;
/**
 * Manejador de sesiones del usuario.
 * Se trata de un singleton que almacena la información de la sesión actual.
 * 
 */
public class SessionManager {
    private static SessionManager instance;
    private static core.data.Users.User currentUser;
    private boolean isAuthenticated = false;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setCurrentUser(core.data.Users.User user) {
        currentUser = user;
        isAuthenticated = true;
    }

    public core.data.Users.User getCurrentUser() {
        return currentUser;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }
    
    public void logout() {
        currentUser = null;
        isAuthenticated = false;
    }

    public boolean isAdmin() {
        return currentUser!= null && currentUser.isAdmin();
    }
}
