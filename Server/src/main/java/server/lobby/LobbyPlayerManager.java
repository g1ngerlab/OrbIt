package server.lobby;

import server.model.LobbyPlayer;
import taltech.orbitgame.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages player-related operations in a lobby.
 * Handles player joining, leaving, and position updates.
 */
public class LobbyPlayerManager {
    private static final Logger LOGGER = Logger.getLogger(LobbyPlayerManager.class.getName());

    private final int lobbyId;
    private final Map<Integer, LobbyPlayer> players;
    private final LobbyBroadcaster broadcaster;
    private final LobbyInventoryManager inventoryManager;

    public LobbyPlayerManager(int lobbyId, Map<Integer, LobbyPlayer> players,
                             LobbyBroadcaster broadcaster, LobbyInventoryManager inventoryManager) {
        this.lobbyId = lobbyId;
        this.players = players;
        this.broadcaster = broadcaster;
        this.inventoryManager = inventoryManager;
    }

    /**
     * Gets the number of players in the lobby.
     */
    public int getPlayerCount() {
        return players.size();
    }

    /**
     * Checks if a player exists in the lobby.
     */
    public boolean hasPlayer(int clientId) {
        return players.containsKey(clientId);
    }

    /**
     * Gets a player by client ID.
     */
    public LobbyPlayer getPlayer(int clientId) {
        return players.get(clientId);
    }

    /**
     * Removes a player from the lobby.
     */
    public void removePlayer(int clientId) {
        LobbyPlayer player = players.remove(clientId);
        if (player != null) {
            inventoryManager.removeInventory(clientId);
            LOGGER.info(logTag() + "Player " + player.getPlayerName() + " left. Total players: " + players.size());

            Network.PlayerLeft left = new Network.PlayerLeft(clientId);
            broadcaster.broadcastToAll(left);
        }
    }

    /**
     * Updates a player's position and aim direction.
     */
    public void updatePlayerPosition(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        LobbyPlayer player = players.get(clientId);
        if (player != null) {
            player.setPositionAndAim(x, y, aimX, aimY);

            Network.PlayerPositionUpdate update = new Network.PlayerPositionUpdate(clientId, x, y, moving, aimX, aimY);
            broadcaster.broadcastToOthers(clientId, update);

            if (moving) {
                Network.PlayerStep stepSound = new Network.PlayerStep(clientId, player.getPlayerName(), x, y);
                broadcaster.broadcastToOthers(clientId, stepSound);
            }
        }
    }

    /**
     * Broadcasts a player's health update to all clients.
     */
    public void broadcastPlayerHealth(LobbyPlayer player) {
        broadcaster.broadcastToAll(new Network.PlayerHealthUpdate(
            player.getClientId(),
            player.getCurrentHealth(),
            player.getMaxHealth()
        ));
    }

    /**
     * Notifies all other players about a new player.
     */
    public void notifyNewPlayer(LobbyPlayer player) {
        Network.PlayerJoined joined = new Network.PlayerJoined(
            player.getClientId(),
            player.getPlayerName(),
            player.getX(),
            player.getY(),
            player.getCharacterId()
        );
        broadcaster.broadcastToOthers(player.getClientId(), joined);
    }

    /**
     * Sends the lobby snapshot to a newly joined player.
     */
    public void sendLobbySnapshotToNewPlayer(int clientId, LobbyPlayer joiningPlayer) {
        LobbyPlayer player = players.get(clientId);
        if (player == null) {
            return;
        }

        Network.LobbyState lobbyState = new Network.LobbyState();
        for (LobbyPlayer p : players.values()) {
            if (p.getClientId() != clientId) {
                lobbyState.players.add(new Network.PlayerInfo(
                    p.getClientId(),
                    p.getPlayerName(),
                    p.getX(),
                    p.getY(),
                    p.getAimX(),
                    p.getAimY(),
                    p.getCharacterId()
                ));
            }
        }

        player.getConnection().sendTCP(lobbyState);
        player.getConnection().sendTCP(new Network.LobbyJoinConfirmed(lobbyId, joiningPlayer.getX(), joiningPlayer.getY()));
    }

    /**
     * Gets all players as a list.
     */
    public List<LobbyPlayer> getAllPlayers() {
        return new ArrayList<>(players.values());
    }

    /**
     * Checks if the lobby is empty.
     */
    public boolean isEmpty() {
        return players.isEmpty();
    }

    private String logTag() {
        return "[Lobby " + lobbyId + "] ";
    }
}
