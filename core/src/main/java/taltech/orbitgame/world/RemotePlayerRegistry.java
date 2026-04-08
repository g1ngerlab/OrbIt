package taltech.orbitgame.world;

import taltech.orbitgame.network.Network;
import taltech.orbitgame.player.CharacterType;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Keeps lifecycle and state updates of remote players in one place.
 */
public class RemotePlayerRegistry {
    private final Map<Integer, RemotePlayer> players = new HashMap<>();

    public void addPlayer(int clientId, String playerName, float x, float y, int characterId) {
        RemotePlayer remotePlayer = new RemotePlayer(
            clientId,
            playerName,
            x,
            y,
            CharacterType.fromId(characterId)
        );
        players.put(clientId, remotePlayer);
    }

    public RemotePlayer removePlayer(int clientId) {
        return players.remove(clientId);
    }

    public RemotePlayer getPlayer(int clientId) {
        return players.get(clientId);
    }

    public void updatePosition(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        RemotePlayer remotePlayer = players.get(clientId);
        if (remotePlayer != null) {
            remotePlayer.updatePosition(x, y, moving, aimX, aimY);
        }
    }

    public void updateAll(float delta) {
        for (RemotePlayer remotePlayer : players.values()) {
            remotePlayer.update(delta);
        }
    }

    public void applyLobbyState(Network.LobbyState lobbyState, int localClientId) {
        for (Network.PlayerInfo playerInfo : lobbyState.players) {
            if (playerInfo.clientId == localClientId) {
                continue;
            }
            addPlayer(
                playerInfo.clientId,
                playerInfo.playerName,
                playerInfo.x,
                playerInfo.y,
                playerInfo.characterId
            );
            updatePosition(
                playerInfo.clientId,
                playerInfo.x,
                playerInfo.y,
                false,
                playerInfo.aimX,
                playerInfo.aimY
            );
        }
    }

    public void updateHealth(int clientId, float health, float maxHealth) {
        RemotePlayer remotePlayer = players.get(clientId);
        if (remotePlayer != null) {
            remotePlayer.setHealth(health, maxHealth);
        }
    }

    public Map<Integer, RemotePlayer> asMap() {
        return players;
    }

    public Collection<RemotePlayer> values() {
        return players.values();
    }

    public void disposeAll() {
        for (RemotePlayer remotePlayer : players.values()) {
            remotePlayer.dispose();
        }
        players.clear();
    }
}

