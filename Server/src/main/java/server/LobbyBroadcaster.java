package server;

import server.model.LobbyPlayer;
import java.util.Map;

/**
 * Handles all network broadcasting to players in a lobby.
 * Encapsulates sending TCP messages to all or specific players.
 */
public class LobbyBroadcaster {
    private final Map<Integer, LobbyPlayer> players;

    public LobbyBroadcaster(Map<Integer, LobbyPlayer> players) {
        this.players = players;
    }

    public void broadcastToAll(Object message) {
        for (LobbyPlayer player : players.values()) {
            player.getConnection().sendTCP(message);
        }
    }

    public void broadcastToOthers(int excludeClientId, Object message) {
        for (LobbyPlayer player : players.values()) {
            if (player.getClientId() != excludeClientId) {
                player.getConnection().sendTCP(message);
            }
        }
    }

    public void sendToPlayer(int clientId, Object message) {
        LobbyPlayer player = players.get(clientId);
        if (player != null) {
            player.getConnection().sendTCP(message);
        }
    }
}

