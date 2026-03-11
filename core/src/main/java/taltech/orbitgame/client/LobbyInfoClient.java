package taltech.orbitgame.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import taltech.orbitgame.network.Network;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight client for querying lobby information without joining
 */
public class LobbyInfoClient {
    private Client client;
    private Map<Integer, Integer> lobbyPlayerCounts;
    private boolean connected = false;
    private LobbyInfoListener listener;

    public interface LobbyInfoListener {
        void onLobbyInfoReceived(Map<Integer, Integer> lobbyPlayerCounts);
    }

    public LobbyInfoClient() {
        lobbyPlayerCounts = new HashMap<>();
        client = new Client();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                connected = true;
                System.out.println("[LobbyInfoClient] Connected to server");
                // Request lobby info immediately
                requestLobbyInfo();
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.LobbyInfo) {
                    Network.LobbyInfo info = (Network.LobbyInfo) object;
                    if (info.lobbyPlayerCounts != null) {
                        lobbyPlayerCounts = new HashMap<>(info.lobbyPlayerCounts); // Create a copy
                        System.out.println("[LobbyInfoClient] Received lobby info: " + lobbyPlayerCounts);
                        if (listener != null) {
                            listener.onLobbyInfoReceived(new HashMap<>(lobbyPlayerCounts)); // Pass a copy
                        }
                    } else {
                        System.out.println("[LobbyInfoClient] WARNING: Received LobbyInfo with null lobbyPlayerCounts");
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                connected = false;
                System.out.println("[LobbyInfoClient] Disconnected from server");
            }
        });
    }

    public void setListener(LobbyInfoListener listener) {
        this.listener = listener;
    }

    public void connect() {
        if (!connected && !client.isConnected()) {
            client.start();
            new Thread(() -> {
                try {
                    client.connect(5000, "localhost", Network.PORT);
                } catch (IOException e) {
                    System.err.println("[LobbyInfoClient] Connection failed: " + e.getMessage());
                }
            }, "LobbyInfoClient-Connect").start();
        }
    }

    public void requestLobbyInfo() {
        if (client.isConnected()) {
            Network.RequestLobbyInfo request = new Network.RequestLobbyInfo();
            client.sendTCP(request);
            System.out.println("[LobbyInfoClient] Requested lobby info");
        }
    }

    public Map<Integer, Integer> getLobbyPlayerCounts() {
        return new HashMap<>(lobbyPlayerCounts);
    }

    public int getPlayerCount(int lobbyId) {
        return lobbyPlayerCounts.getOrDefault(lobbyId, 0);
    }

    public boolean isConnected() {
        return connected && client.isConnected();
    }

    public void disconnect() {
        if (client != null) {
            client.close();
        }
    }
}
