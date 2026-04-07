package taltech.orbitgame.network;

/**
 * Runtime network configuration.
 * To switch between LOCAL and REMOTE servers, comment/uncomment the lines below.
 */
public final class NetworkConfig {
    // ===== SWITCH SERVER CONFIGURATION HERE =====
    // Uncomment ONE of the following pairs:

    // === REMOTE SERVER (TalTech) - DEFAULT ===
    //private static final String DEFAULT_HOST = "193.40.157.153";
    //private static final int DEFAULT_PORT = 8080;

    // === LOCAL SERVER - Uncomment to use locally ===
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 54555;

    // ============================================

    private NetworkConfig() {
    }

    public static String getServerHost() {
        System.out.println("[NetworkConfig] Using server: " + DEFAULT_HOST + ":" + DEFAULT_PORT);
        return DEFAULT_HOST;
    }

    public static int getServerPort() {
        return DEFAULT_PORT;
    }
}
