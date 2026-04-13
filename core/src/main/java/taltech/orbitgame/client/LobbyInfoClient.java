package taltech.orbitgame.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.network.NetworkConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight client for querying lobby information without joining
 */
public class LobbyInfoClient {
    private static final int CONNECT_TIMEOUT_MS = 1000;  // Reduced from 2000ms for local network
    private static final long INITIAL_RETRY_DELAY_MS = 100L;  // Reduced from 300ms
    private static final long MAX_RETRY_DELAY_MS = 500L;  // Reduced from 2000ms
    private static final int MAX_INITIAL_RETRIES = 3;  // Reduced from 5 attempts

    private final Client client;
    private final String serverHost;
    private final int serverPort;
    private Map<Integer, Integer> lobbyPlayerCounts;
    private volatile boolean connected = false;
    private volatile boolean connectLoopRunning;
    private volatile String lastConnectionError;
    private volatile int retryAttempt;
    private Thread connectThread;
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
        serverHost = NetworkConfig.getServerHost();
        serverPort = NetworkConfig.getServerPort();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                connected = true;
                retryAttempt = 0;
                lastConnectionError = null;
                System.out.println("[LobbyInfoClient] Connected to server " + serverHost + ":" + serverPort);
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
                if (object instanceof Network.LobbyCreateFailed) {
                    Network.LobbyCreateFailed failed = (Network.LobbyCreateFailed) object;
                    System.out.println("[LobbyInfoClient] Lobby creation failed: " + failed.reason);
                    if (lobbyCreatedListener != null) {
                        lobbyCreatedListener.onLobbyCreateFailed(failed.reason);
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
        if (connectLoopRunning) {
            return;
        }

        connectLoopRunning = true;
        if (connectThread == null || !connectThread.isAlive()) {
            client.start();
            connectThread = new Thread(() -> {
                long retryDelay = INITIAL_RETRY_DELAY_MS;
                while (connectLoopRunning && !Thread.currentThread().isInterrupted()) {
                    if (client.isConnected()) {
                        sleepQuietly(300L);
                        continue;
                    }

                    retryAttempt++;
                    try {
                        System.out.println("[LobbyInfoClient] Connecting (attempt " + retryAttempt + ") to "
                            + serverHost + ":" + serverPort);
                        client.connect(CONNECT_TIMEOUT_MS, serverHost, serverPort);
                        retryDelay = INITIAL_RETRY_DELAY_MS; // Reset delay on successful connection
                    } catch (IOException e) {
                        lastConnectionError = e.getMessage();
                        System.err.println("[LobbyInfoClient] Connection failed: " + e.getMessage());

                        // Stop retrying after max attempts
                        if (retryAttempt >= MAX_INITIAL_RETRIES) {
                            System.out.println("[LobbyInfoClient] Max retries reached (" + MAX_INITIAL_RETRIES + "), giving up");
                            connectLoopRunning = false;
                            break;
                        }

                        // Exponential backoff up to max
                        sleepQuietly(retryDelay);
                        retryDelay = Math.min(retryDelay * 2, MAX_RETRY_DELAY_MS);
                    }
                }
            }, "LobbyInfoClient-Connect");
            connectThread.start();
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    public String getConnectionStatusMessage() {
        if (isConnected()) {
            return "Connected";
        }

        if (retryAttempt <= 1 || lastConnectionError == null || lastConnectionError.isEmpty()) {
            return "Connecting to server";
        }

        return "Server not ready, retrying";
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
            System.out.println("[LobbyInfoClient] Cannot request lobbies - not connected yet");
        }
    }

    public void createLobby(String lobbyName) {
        if (client.isConnected()) {
            client.sendTCP(new Network.CreateLobby(lobbyName));
            System.out.println("[LobbyInfoClient] Sent create lobby request: " + lobbyName);
        } else {
            System.out.println("[LobbyInfoClient] Cannot create lobby - not connected yet");
            if (lobbyCreatedListener != null) {
                lobbyCreatedListener.onLobbyCreateFailed("Not connected to server");
            }
        }
    }

    public boolean isConnected() {
        return connected && client.isConnected();
    }

    public void disconnect() {
        connectLoopRunning = false;
        if (connectThread != null) {
            connectThread.interrupt();
            connectThread = null;
        }
        client.close();
    }
}
