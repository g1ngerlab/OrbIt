package taltech.orbitgame.network;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryo.Kryo;
import taltech.orbitgame.shared.InputEvent;
import taltech.orbitgame.shared.Network;

import java.io.IOException;

/**
 * Handles client-side network communication with the server using Kryonet
 */
public class ClientNetworkManager {
    private Client client;
    private int clientId;
    private String serverHost;
    private int serverPort;
    private boolean connected;
    private Thread clientThread;

    public ClientNetworkManager(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.connected = false;
        this.clientId = -1;
    }

    /**
     * Connect to the server
     */
    public boolean connect() {
        try {
            client = new Client(8192, 8192);

            // Register classes using consistent helper
            Network.register(client.getKryo());

            System.out.println("[Client] Attempting to connect to " + serverHost + ":" + serverPort);

            // Start client in a separate thread
            clientThread = new Thread(client);
            clientThread.setDaemon(true);
            clientThread.start();

            // Connect with timeout
            client.connect(5000, serverHost, serverPort);

            this.connected = true;
            this.clientId = client.getID();

            System.out.println("[Client] Connected successfully with ID: " + clientId);
            return true;
        } catch (Exception e) {
            System.err.println("[Client] Failed to connect: " + e.getMessage());
            this.connected = false;
            return false;
        }
    }


    /**
     * Send input event to server
     */
    public void sendInputEvent(InputEvent inputEvent) {
        if (!connected || client == null) {
            return;
        }
        try {
            inputEvent.setClientId(clientId);
            client.sendTCP(inputEvent);
        } catch (Exception e) {
            System.err.println("[Client] Failed to send input: " + e.getMessage());
        }
    }

    /**
     * Disconnect from server
     */
    public void disconnect() {
        try {
            if (client != null) {
                System.out.println("[Client] Disconnecting from server");
                client.close();
                this.connected = false;
            }
        } catch (Exception e) {
            System.err.println("[Client] Failed to disconnect: " + e.getMessage());
        }
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }
}
