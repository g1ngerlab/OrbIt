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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens for player input events from connected clients
 * Handles multiple client connections and echoes input to terminal
 */
public class PlayerInputListener implements Listener {
    private static final Logger LOGGER = Logger.getLogger(PlayerInputListener.class.getName());
    private static final int NPC_TICK_RATE = 20;
    private static final float NPC_DELTA_TIME = 1f / NPC_TICK_RATE;

    private final Server server;
    private final Map<Integer, String> connectedClients;
    private final Map<Integer, Lobby> lobbies;
    private final Map<Integer, Integer> clientToLobby; // Maps clientId to lobbyId
    private final ScheduledExecutorService npcLoop;
    private final ScheduledExecutorService lobbyCreationExecutor;
    private final AtomicInteger nextLobbyId = new AtomicInteger(1); // Counter for generating unique lobby IDs

    public PlayerInputListener() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.lobbies = new ConcurrentHashMap<>();
        this.clientToLobby = new ConcurrentHashMap<>();
        this.server = new Server();
        this.npcLoop = Executors.newSingleThreadScheduledExecutor();
        this.lobbyCreationExecutor = Executors.newSingleThreadScheduledExecutor();
        Network.register(server);

        server.addListener(this);
        startNpcLoop();
    }

    public void startServer(int port) {
        server.start();
        try {
            server.bind(port);
            LOGGER.log(Level.INFO, "[Server] Listening on port {0}", port);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e, () -> "[Server] Failed to bind port " + port);
        }
    }

    public void stopServer() {
        lobbyCreationExecutor.shutdownNow();
        npcLoop.shutdownNow();
        server.stop();
    }

    @Override
    public void connected(Connection connection) {
        LOGGER.info("[Server] New connection from: " + connection.getRemoteAddressTCP());
    }

    @Override
    public void disconnected(Connection connection) {
        String name = connectedClients.remove(connection.getID());
        if (name != null) {
            LOGGER.log(Level.INFO, "[Server] Client disconnected: {0}", name);

            // Remove player from their lobby
            Integer lobbyId = clientToLobby.remove(connection.getID());
            if (lobbyId != null) {
                Lobby lobby = lobbies.get(lobbyId);
                if (lobby != null) {
                    lobby.removePlayer(connection.getID());
                    LOGGER.log(Level.INFO,
                        "[Server] Lobby {0} ({1}) now has {2} players",
                        new Object[]{lobbyId, lobby.getLobbyName(), lobby.getPlayerCount()});
                }
            }
        }
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof Network.CreateLobby) {
            handleCreateLobby(connection, (Network.CreateLobby) object);
            return;
        }

        if (object instanceof Network.RequestAllLobbies) {
            handleRequestAllLobbies(connection);
            return;
        }

        if (object instanceof Network.RequestLobbyInfo) {
            handleRequestLobbyInfo(connection);
            return;
        }

        if (object instanceof Network.Login) {
            handleLogin(connection, (Network.Login) object);
            return;
        }

        if (object instanceof Network.JoinLobby) {
            handleJoinLobby(connection, (Network.JoinLobby) object);
            return;
        }

        if (object instanceof Network.PlayerPositionUpdate) {
            handlePlayerPositionUpdate(connection, (Network.PlayerPositionUpdate) object);
            return;
        }

        if (object instanceof Network.ShootRequest) {
            handleShootRequest(connection, (Network.ShootRequest) object);
            return;
        }

        if (object instanceof Network.DropItemRequest) {
            handleDropItemRequest(connection, (Network.DropItemRequest) object);
            return;
        }

        if (object instanceof Network.MoveInventoryItemRequest) {
            handleMoveInventoryItemRequest(connection, (Network.MoveInventoryItemRequest) object);
            return;
        }

        if (object instanceof Network.ShopPurchaseRequest) {
            handleShopPurchaseRequest(connection, (Network.ShopPurchaseRequest) object);
            return;
        }

        if (object instanceof Network.LobbyTimerDebugSkip) {
            handleLobbyTimerDebugSkip(connection);
            return;
        }

        if (object instanceof Network.InputState) {
            handleInputState(connection, (Network.InputState) object);
        }
    }

    private void handleCreateLobby(Connection connection, Network.CreateLobby createLobby) {
        String lobbyName = normalizeLobbyName(createLobby == null ? null : createLobby.lobbyName);
        int lobbyId = nextLobbyId.getAndIncrement();

        lobbyCreationExecutor.execute(() -> {
            try {
                long startedMs = System.currentTimeMillis();
                Lobby lobby = new Lobby(lobbyId, lobbyName);
                lobbies.put(lobbyId, lobby);

                LOGGER.log(Level.INFO,
                    "[Server] Created lobby {0} with name: {1} in {2} ms",
                    new Object[]{lobbyId, lobbyName, System.currentTimeMillis() - startedMs});
                connection.sendTCP(new Network.LobbyCreated(lobbyId, lobbyName));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "[Server] Failed to create lobby", e);
                connection.sendTCP(new Network.LobbyCreateFailed(
                    e.getMessage() == null ? "Lobby creation failed" : e.getMessage()
                ));
            }
        });
    }

    private String normalizeLobbyName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return "Lobby " + nextLobbyId.get();
        }
        String normalized = rawName.trim();
        return normalized.length() > 12 ? normalized.substring(0, 12) : normalized;
    }

    private void handleRequestAllLobbies(Connection connection) {
        Network.AllLobbiesInfo allLobbies = new Network.AllLobbiesInfo();
        for (Lobby lobby : lobbies.values()) {
            allLobbies.lobbies.add(new Network.LobbyListItem(
                lobby.getLobbyId(),
                lobby.getLobbyName(),
                lobby.getPlayerCount()
            ));
        }
        connection.sendTCP(allLobbies);
        LOGGER.info("[Server] Sent all lobbies info to client " + connection.getID() + ": " + allLobbies.lobbies.size() + " lobbies");
    }

    private void handleRequestLobbyInfo(Connection connection) {
        Network.LobbyInfo lobbyInfo = new Network.LobbyInfo();
        for (Map.Entry<Integer, Lobby> entry : lobbies.entrySet()) {
            lobbyInfo.lobbyPlayerCounts.put(entry.getKey(), entry.getValue().getPlayerCount());
        }
        connection.sendTCP(lobbyInfo);
        LOGGER.log(Level.INFO, "[Server] Sent lobby info to client {0}: {1}",
            new Object[]{connection.getID(), lobbyInfo.lobbyPlayerCounts});
    }

    private void handleLogin(Connection connection, Network.Login login) {
        String name = sanitizePlayerName(login == null ? null : login.name, connection.getID());
        connectedClients.put(connection.getID(), name);

        LOGGER.log(Level.INFO, "[Server] Client logged in: {0}", name);

        Network.Registration registration = new Network.Registration();
        registration.id = connection.getID();
        registration.name = name;
        connection.sendTCP(registration);
    }

    private void handleJoinLobby(Connection connection, Network.JoinLobby joinLobby) {
        int clientId = connection.getID();
        String playerName = connectedClients.get(clientId);
        if (playerName == null) {
            return;
        }

        Lobby lobby = lobbies.get(joinLobby.lobbyId);
        if (lobby == null) {
            LOGGER.log(Level.WARNING, "[Server] Client {0} tried to join non-existent lobby {1}",
                new Object[]{clientId, joinLobby.lobbyId});
            return;
        }

        int characterId = Math.max(1, joinLobby.characterId);
        lobby.addPlayer(clientId, playerName, characterId, connection, joinLobby.loadoutSlots);
        clientToLobby.put(clientId, joinLobby.lobbyId);
    }

    private void handlePlayerPositionUpdate(Connection connection, Network.PlayerPositionUpdate update) {
        Lobby lobby = getLobbyForConnection(connection);
        if (lobby != null) {
            lobby.updatePlayerPosition(update.clientId, update.x, update.y, update.moving, update.aimX, update.aimY);
        }
    }

    private void handleShootRequest(Connection connection, Network.ShootRequest shootRequest) {
        Lobby lobby = getLobbyForConnection(connection);
        if (lobby != null) {
            lobby.queueShootRequest(connection.getID(), shootRequest.targetX, shootRequest.targetY);
        }
    }

    private void handleDropItemRequest(Connection connection, Network.DropItemRequest dropItemRequest) {
        Lobby lobby = getLobbyForConnection(connection);
        if (lobby != null) {
            lobby.dropInventoryItem(connection.getID(), dropItemRequest.slotIndex);
        }
    }

    private void handleMoveInventoryItemRequest(Connection connection,
                                                Network.MoveInventoryItemRequest moveInventoryItemRequest) {
        Lobby lobby = getLobbyForConnection(connection);
        if (lobby != null) {
            lobby.moveInventoryItem(
                connection.getID(),
                moveInventoryItemRequest.fromSlotIndex,
                moveInventoryItemRequest.toSlotIndex
            );
        }
    }

    private void handleLobbyTimerDebugSkip(Connection connection) {
        Lobby lobby = getLobbyForConnection(connection);
        if (lobby != null) {
            lobby.forceAdvanceToFinalMinuteForTesting();
        }
    }

    private void handleShopPurchaseRequest(Connection connection, Network.ShopPurchaseRequest request) {
        Lobby lobby = getLobbyForConnection(connection);
        if (lobby == null || request == null) {
            return;
        }
        lobby.purchaseShopItem(connection.getID(), request.itemType, request.amount, request.orbCost);
    }

    private Lobby getLobbyForConnection(Connection connection) {
        Integer lobbyId = clientToLobby.get(connection.getID());
        return lobbyId == null ? null : lobbies.get(lobbyId);
    }

    private void handleInputState(Connection connection, Network.InputState state) {
        String name = connectedClients.get(connection.getID());
        if (name == null) {
            name = "Unknown Client " + connection.getID();
        }

        ArrayList<String> pressed = new ArrayList<>();
        if (state.stateGetter("w")) pressed.add("W");
        if (state.stateGetter("a")) pressed.add("A");
        if (state.stateGetter("s")) pressed.add("S");
        if (state.stateGetter("d")) pressed.add("D");
        if (state.stateGetter("left_click")) pressed.add("Left Click");
        if (state.stateGetter("right_click")) pressed.add("Right Click");
        if (pressed.isEmpty()) {
            return;
        }

        String keysStr = String.join(", ", pressed);
        Network.KeyPressEvent event = new Network.KeyPressEvent(connection.getID(), name, keysStr);
        server.sendToAllTCP(event);
    }

    private String sanitizePlayerName(String rawName, int connectionId) {
        String normalized = rawName == null ? "" : rawName.trim();
        if (normalized.isEmpty()) {
            return "Player" + connectionId;
        }

        if (normalized.length() > 16) {
            normalized = normalized.substring(0, 16);
        }

        if (!normalized.matches("\\w+")) {
            return "Player" + connectionId;
        }

        return normalized;
    }

    private static final long EMPTY_LOBBY_TIMEOUT_MS = 5L * 60L * 1000L; // 5 minutes

    private void startNpcLoop() {
        long periodMs = 1000L / NPC_TICK_RATE;
        npcLoop.scheduleAtFixedRate(() -> {
            ArrayList<Integer> closedLobbies = new ArrayList<>();
            for (Lobby lobby : lobbies.values()) {
                lobby.updateLobbyTimer(NPC_DELTA_TIME);
                lobby.updateNpcs(NPC_DELTA_TIME);
                if (lobby.isClosed()) {
                    closedLobbies.add(lobby.getLobbyId());
                }
            }
            for (Integer lobbyId : closedLobbies) {
                lobbies.remove(lobbyId);
            }
            // Clean up lobbies that have been empty for too long
            lobbies.entrySet().removeIf(entry -> {
                if (entry.getValue().isEmptyForLongerThan(EMPTY_LOBBY_TIMEOUT_MS)) {
                    LOGGER.info("[Server] Cleaned up stale lobby " + entry.getKey()
                            + " (" + entry.getValue().getLobbyName() + ")");
                    return true;
                }
                return false;
            });
        }, 0L, periodMs, TimeUnit.MILLISECONDS);
    }
}
