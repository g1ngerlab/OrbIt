package server;

import server.listener.PlayerInputListener;
import server.world.MapCollisionGridCache;

/**
 * Main server launcher that starts the Kryonet server
 */
public class ServerLauncher {
    private static final int SERVER_PORT = 54555;

    public static void main(String[] args) {
        System.out.println("[Server] Starting server on port " + SERVER_PORT);

        // Preload map collision grid to avoid blocking lobby creation.
        MapCollisionGridCache.get();

        // Initialize and start the server
        PlayerInputListener listener = new PlayerInputListener();
        listener.startServer(SERVER_PORT);
    }
}
