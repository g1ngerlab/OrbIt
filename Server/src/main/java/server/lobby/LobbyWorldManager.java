package server.lobby;

import server.model.LobbyPlayer;
import taltech.orbitgame.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages world snapshots and broadcasting to all players in the lobby.
 * Responsible for collecting game state and sending it to clients.
 */
public class LobbyWorldManager {
    private final int lobbyId;
    private final Map<Integer, LobbyPlayer> players;
    private final LobbyBroadcaster broadcaster;

    private long npcTick;

    public LobbyWorldManager(int lobbyId, Map<Integer, LobbyPlayer> players, LobbyBroadcaster broadcaster) {
        this.lobbyId = lobbyId;
        this.players = players;
        this.broadcaster = broadcaster;
        this.npcTick = 0;
    }

    /**
     * Gets the current NPC tick.
     */
    public long getNpcTick() {
        return npcTick;
    }

    /**
     * Increments the NPC tick.
     */
    public void incrementNpcTick() {
        npcTick++;
    }

    /**
     * Creates and broadcasts an NPC state update.
     */
    public void broadcastNpcStateUpdate(Network.NpcStateUpdate update) {
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        broadcaster.broadcastToAll(update);
    }

    /**
     * Creates and broadcasts a bullet state update.
     */
    public void broadcastBulletStateUpdate(Network.BulletStateUpdate update) {
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        broadcaster.broadcastToAll(update);
    }

    /**
     * Creates and broadcasts a corpse state update.
     */
    public void broadcastCorpseStateUpdate(Network.CorpseStateUpdate update) {
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        broadcaster.broadcastToAll(update);
    }

    /**
     * Creates and broadcasts an orb state update.
     */
    public void broadcastOrbStateUpdate(Network.OrbStateUpdate update) {
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        broadcaster.broadcastToAll(update);
    }

    /**
     * Broadcasts a message to all players.
     */
    public void broadcastToAll(Object message) {
        broadcaster.broadcastToAll(message);
    }

    /**
     * Broadcasts a message to all players except one.
     */
    public void broadcastToOthers(int excludeClientId, Object message) {
        broadcaster.broadcastToOthers(excludeClientId, message);
    }

    /**
     * Sends a world snapshot to a specific connection (used when player joins).
     */
    public void sendWorldSnapshot(int clientId) {
        LobbyPlayer player = players.get(clientId);
        if (player != null) {
            player.getConnection().sendTCP(new Network.LobbyTimerState(lobbyId, 0, false));
        }
    }

    /**
     * Gets all player positions as a list of Vector2.
     */
    public List<com.badlogic.gdx.math.Vector2> collectPlayerPositions() {
        List<com.badlogic.gdx.math.Vector2> playerPositions = new ArrayList<>();
        for (LobbyPlayer player : players.values()) {
            playerPositions.add(new com.badlogic.gdx.math.Vector2(player.getX(), player.getY()));
        }
        return playerPositions;
    }
}
