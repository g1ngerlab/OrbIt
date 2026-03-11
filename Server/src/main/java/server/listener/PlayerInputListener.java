package server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import server.Lobby;
import taltech.orbitgame.network.Network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Listens for player input events from connected clients
 * Handles multiple client connections and echoes input to terminal
 */
public class PlayerInputListener implements Listener {
    private static final int NPC_TICK_RATE = 20;
    private static final float NPC_DELTA_TIME = 1f / NPC_TICK_RATE;

    private final Server server;
    private final Map<Integer, String> connectedClients;
    private final Map<Integer, Lobby> lobbies;
    private final Map<Integer, Integer> clientToLobby; // Maps clientId to lobbyId
    private final ScheduledExecutorService npcLoop;

    public PlayerInputListener() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.lobbies = new ConcurrentHashMap<>();
        this.clientToLobby = new ConcurrentHashMap<>();
        this.server = new Server();
        this.npcLoop = Executors.newSingleThreadScheduledExecutor();
        Network.register(server);

        server.addListener(this);
        startNpcLoop();
    }

    public void startServer(int port) {
        server.start();
        try {
            server.bind(port);
            System.out.println("[Server] Listening on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        npcLoop.shutdownNow();
        server.stop();
    }

    @Override
    public void connected(Connection connection) {
        System.out.println("[Server] New connection from: " + connection.getRemoteAddressTCP());
    }

    @Override
    public void disconnected(Connection connection) {
        String name = connectedClients.remove(connection.getID());
        if (name != null) {
            System.out.println("[Server] Client disconnected: " + name);

            // Remove player from their lobby
            Integer lobbyId = clientToLobby.remove(connection.getID());
            if (lobbyId != null) {
                Lobby lobby = lobbies.get(lobbyId);
                if (lobby != null) {
                    lobby.removePlayer(connection.getID());
                }
            }
        }
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof Network.RequestLobbyInfo) {
            // Send lobby information back to client
            Network.LobbyInfo lobbyInfo = new Network.LobbyInfo();
            for (Map.Entry<Integer, Lobby> entry : lobbies.entrySet()) {
                lobbyInfo.lobbyPlayerCounts.put(entry.getKey(), entry.getValue().getPlayerCount());
            }
            connection.sendTCP(lobbyInfo);
            System.out.println("[Server] Sent lobby info to client " + connection.getID() + ": " + lobbyInfo.lobbyPlayerCounts);
            return;
        }

        if (object instanceof Network.Login) {
            Network.Login login = (Network.Login) object;
            String name = login.name + connection.getID(); // Append ID for uniqueness
            connectedClients.put(connection.getID(), name);

            System.out.println("[Server] Client logged in: " + name);

            Network.Registration reg = new Network.Registration();
            reg.id = connection.getID();
            reg.name = name;
            connection.sendTCP(reg);
            return;
        }

        if (object instanceof Network.JoinLobby) {
            Network.JoinLobby joinLobby = (Network.JoinLobby) object;
            int clientId = connection.getID();
            String playerName = connectedClients.get(clientId);

            if (playerName != null) {
                // Get or create lobby
                Lobby lobby = lobbies.get(joinLobby.lobbyId);
                if (lobby == null) {
                    lobby = new Lobby(joinLobby.lobbyId);
                    lobbies.put(joinLobby.lobbyId, lobby);
                    System.out.println("[Server] Created lobby " + joinLobby.lobbyId);
                }

                // Add player to lobby
                lobby.addPlayer(clientId, playerName, connection);
                clientToLobby.put(clientId, joinLobby.lobbyId);
            }
            return;
        }

        if (object instanceof Network.PlayerPositionUpdate) {
            Network.PlayerPositionUpdate update = (Network.PlayerPositionUpdate) object;
            Integer lobbyId = clientToLobby.get(connection.getID());

            if (lobbyId != null) {
                Lobby lobby = lobbies.get(lobbyId);
                if (lobby != null) {
                    lobby.updatePlayerPosition(update.clientId, update.x, update.y, update.moving, update.aimX, update.aimY);
                }
            }
            return;
        }

        if (object instanceof Network.ShootRequest) {
            Network.ShootRequest shootRequest = (Network.ShootRequest) object;
            Integer lobbyId = clientToLobby.get(connection.getID());
            if (lobbyId != null) {
                Lobby lobby = lobbies.get(lobbyId);
                if (lobby != null) {
                    lobby.queueShootRequest(connection.getID(), shootRequest.targetX, shootRequest.targetY);
                }
            }
            return;
        }

        if (object instanceof Network.InputState) {
            Network.InputState state = (Network.InputState) object;

            String name = connectedClients.get(connection.getID());
            if (name == null) name = "Unknown Client " + connection.getID();

            ArrayList<String> pressed = new ArrayList<>();
            if (state.stateGetter("w")) pressed.add("W");
            if (state.stateGetter("a")) pressed.add("A");
            if (state.stateGetter("s")) pressed.add("S");
            if (state.stateGetter("d")) pressed.add("D");
            if (state.stateGetter("left_click")) pressed.add("Left Click");
            if (state.stateGetter("right_click")) pressed.add("Right Click");
            if (!pressed.isEmpty()) {
                String keysStr = String.join(", ", pressed);
                System.out.println(String.format("[Server] %s pressed: %s", name, keysStr));

                // Broadcast to all clients so every player sees what others press
                Network.KeyPressEvent event = new Network.KeyPressEvent(
                    connection.getID(), name, keysStr);
                server.sendToAllTCP(event);
            }
        }
    }

    private void startNpcLoop() {
        long periodMs = 1000L / NPC_TICK_RATE;
        npcLoop.scheduleAtFixedRate(() -> {
            for (Lobby lobby : lobbies.values()) {
                lobby.updateNpcs(NPC_DELTA_TIME);
            }
        }, 0L, periodMs, TimeUnit.MILLISECONDS);
    }
}
