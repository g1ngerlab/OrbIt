package taltech.orbitgame.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import taltech.orbitgame.network.Network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight client for querying lobby information without joining
 */
public class LobbyInfoClient {
    private final Client client;
    private Map<Integer, Integer> lobbyPlayerCounts;
    private volatile boolean connected = false;
    private LobbyInfoListener listener;
    private LobbyCreatedListener lobbyCreatedListener;
    private AllLobbiesListener allLobbiesListener;

    public interface LobbyInfoListener {
        void onLobbyInfoReceived(Map<Integer, Integer> lobbyPlayerCounts);
    }

    public interface LobbyCreatedListener {
        void onLobbyCreated(int lobbyId, String lobbyName);
        void onLobbyCreateFailed(String reason);
    }

    public interface AllLobbiesListener {
        void onAllLobbiesReceived(List<Network.LobbyListItem> lobbies);
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
                requestLobbyInfo();
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.LobbyInfo) {
                    Network.LobbyInfo info = (Network.LobbyInfo) object;
                    if (info.lobbyPlayerCounts != null) {
                        lobbyPlayerCounts = new HashMap<>(info.lobbyPlayerCounts);
                        System.out.println("[LobbyInfoClient] Received lobby info: " + lobbyPlayerCounts);
                        if (listener != null) {
                            listener.onLobbyInfoReceived(new HashMap<>(lobbyPlayerCounts));
                        }
                    }
                }
                if (object instanceof Network.LobbyCreated) {
                    Network.LobbyCreated created = (Network.LobbyCreated) object;
                    System.out.println("[LobbyInfoClient] Lobby created: " + created.lobbyId + " - " + created.lobbyName);
                    if (lobbyCreatedListener != null) {
                        lobbyCreatedListener.onLobbyCreated(created.lobbyId, created.lobbyName);
                    }
                }
                if (object instanceof Network.AllLobbiesInfo) {
                    Network.AllLobbiesInfo info = (Network.AllLobbiesInfo) object;
                    System.out.println("[LobbyInfoClient] Received all lobbies: " + info.lobbies.size());
                    if (allLobbiesListener != null) {
                        allLobbiesListener.onAllLobbiesReceived(new ArrayList<>(info.lobbies));
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                connected = false;
                System.out.println("[LobbyInfoClient] Disconnected from server");
            }
        });

        // Auto-connect on construction
        connect();
    }

    public void setListener(LobbyInfoListener listener) {
        this.listener = listener;
    }

    public void setLobbyCreatedListener(LobbyCreatedListener listener) {
        this.lobbyCreatedListener = listener;
    }

    public void setAllLobbiesListener(AllLobbiesListener listener) {
        this.allLobbiesListener = listener;
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
            client.sendTCP(new Network.RequestLobbyInfo());
        }
    }

    public void requestAllLobbies() {
        if (client.isConnected()) {
            client.sendTCP(new Network.RequestAllLobbies());
            System.out.println("[LobbyInfoClient] Requested all lobbies");
        } else {
            System.err.println("[LobbyInfoClient] Cannot request lobbies - not connected");
            if (allLobbiesListener != null) {
                allLobbiesListener.onAllLobbiesReceived(new ArrayList<>());
            }
        }
    }

    public void createLobby(String lobbyName) {
        if (client.isConnected()) {
            client.sendTCP(new Network.CreateLobby(lobbyName));
            System.out.println("[LobbyInfoClient] Sent create lobby request: " + lobbyName);
        } else {
            System.err.println("[LobbyInfoClient] Cannot create lobby - not connected");
            if (lobbyCreatedListener != null) {
                lobbyCreatedListener.onLobbyCreateFailed("Not connected to server");
            }
        }
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
