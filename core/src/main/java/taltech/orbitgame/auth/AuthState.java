package taltech.orbitgame.auth;

/**
 * Process-local auth state shared between screens.
 */
public final class AuthState {
    private static volatile AuthSession currentSession;

    private AuthState() {
    }

    public static void setCurrentSession(AuthSession session) {
        currentSession = session;
    }

    public static AuthSession getCurrentSession() {
        return currentSession;
    }

    public static String getCurrentUsername() {
        return currentSession == null ? "Player" : currentSession.getUsername();
    }

    public static void clear() {
        currentSession = null;
    }
}

