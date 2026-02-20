package taltech.orbitgame.network;

/**
 * Singleton for managing server connection on the client side
 */
public class ServerConnection {
    private static ServerConnection instance;
    private ClientNetworkManager networkManager;
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 54555;

    private ServerConnection() {
        this.networkManager = new ClientNetworkManager(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Get the singleton instance
     */
    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    /**
     * Connect to the server
     */
    public void connect() {
        networkManager.connect();
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        networkManager.disconnect();
    }

    public ClientNetworkManager getNetworkManager() {
        return networkManager;
    }

    public boolean isConnected() {
        return networkManager.isConnected();
    }
}
