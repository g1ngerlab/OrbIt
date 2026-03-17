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
    private int nextLobbyId = 1; // Counter for generating unique lobby IDs

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
                    System.out.println("[Server] Lobby " + lobbyId + " (" + lobby.getLobbyName() + ") now has " + lobby.getPlayerCount() + " players");
                }
            }
        }
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof Network.CreateLobby) {
            Network.CreateLobby createLobby = (Network.CreateLobby) object;
            String lobbyName = createLobby.lobbyName;

            // Validate lobby name
            if (lobbyName == null || lobbyName.trim().isEmpty()) {
                lobbyName = "Lobby " + nextLobbyId;
            } else {
                lobbyName = lobbyName.trim();
                if (lobbyName.length() > 12) {
                    lobbyName = lobbyName.substring(0, 12);
                }
            }

            // Create new lobby
            int lobbyId = nextLobbyId++;
            Lobby lobby = new Lobby(lobbyId, lobbyName);
            lobbies.put(lobbyId, lobby);

            System.out.println("[Server] Created lobby " + lobbyId + " with name: " + lobbyName);

            // Send confirmation to client
            Network.LobbyCreated lobbyCreated = new Network.LobbyCreated(lobbyId, lobbyName);
            connection.sendTCP(lobbyCreated);
            return;
        }

        if (object instanceof Network.RequestAllLobbies) {
            // Send list of all available lobbies
            Network.AllLobbiesInfo allLobbies = new Network.AllLobbiesInfo();
            for (Lobby lobby : lobbies.values()) {
                allLobbies.lobbies.add(new Network.LobbyListItem(
                    lobby.getLobbyId(),
                    lobby.getLobbyName(),
                    lobby.getPlayerCount()
                ));
            }
            connection.sendTCP(allLobbies);
            System.out.println("[Server] Sent all lobbies info to client " + connection.getID() + ": " + allLobbies.lobbies.size() + " lobbies");
            return;
        }

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
                // Get lobby (no longer auto-create, lobbies must be created explicitly)
                Lobby lobby = lobbies.get(joinLobby.lobbyId);
                if (lobby == null) {
                    System.out.println("[Server] Client " + clientId + " tried to join non-existent lobby " + joinLobby.lobbyId);
                    return;
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

                // Broadcast to all clients so every player sees what others press
                Network.KeyPressEvent event = new Network.KeyPressEvent(
                    connection.getID(), name, keysStr);
                server.sendToAllTCP(event);
            }
        }
    }

    private static final long EMPTY_LOBBY_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    private void startNpcLoop() {
        long periodMs = 1000L / NPC_TICK_RATE;
        npcLoop.scheduleAtFixedRate(() -> {
            for (Lobby lobby : lobbies.values()) {
                lobby.updateNpcs(NPC_DELTA_TIME);
            }
            // Clean up lobbies that have been empty for too long
            lobbies.entrySet().removeIf(entry -> {
                if (entry.getValue().isEmptyForLongerThan(EMPTY_LOBBY_TIMEOUT_MS)) {
                    System.out.println("[Server] Cleaned up stale lobby " + entry.getKey()
                            + " (" + entry.getValue().getLobbyName() + ")");
                    return true;
                }
                return false;
            });
        }, 0L, periodMs, TimeUnit.MILLISECONDS);
    }
}
