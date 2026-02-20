package server;

import server.listener.PlayerInputListener;

/**
 * Main server launcher that starts the Kryonet server
 */
public class ServerLauncher {
    private static final int SERVER_PORT = 54555;

    public static void main(String[] args) {
        System.out.println("[Server] Starting server on port " + SERVER_PORT);

        // Initialize and start the server
        PlayerInputListener listener = new PlayerInputListener();
        listener.startServer(SERVER_PORT);
    }
}
