package server;

import server.listener.PlayerInputListener;
import server.world.MapCollisionGridCache;
import taltech.orbitgame.network.NetworkConfig;

/**
 * Main server launcher that starts the Kryonet server
 */
public class ServerLauncher {
    public static void main(String[] args) {
        int serverPort = NetworkConfig.getServerPort();
        System.out.println("[Server] Starting server on port " + serverPort);

        // Preload map collision grid to avoid blocking lobby creation.
        MapCollisionGridCache.get();

        // Initialize and start the server
        PlayerInputListener listener = new PlayerInputListener();
        listener.startServer(serverPort);
    }
}
