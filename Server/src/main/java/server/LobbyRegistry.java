package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages the lifecycle of lobbies: creation, retrieval, cleanup.
 */
public class LobbyRegistry {
    private final Map<Integer, Lobby> lobbies;
    private final AtomicInteger nextLobbyId;

    public LobbyRegistry() {
        this.lobbies = new ConcurrentHashMap<>();
        this.nextLobbyId = new AtomicInteger(1);
    }

    public Lobby createLobby(String lobbyName) {
        int lobbyId = nextLobbyId.getAndIncrement();
        Lobby lobby = new Lobby(lobbyId, lobbyName);
        lobbies.put(lobbyId, lobby);
        return lobby;
    }

    public Lobby getLobby(int lobbyId) {
        return lobbies.get(lobbyId);
    }

    public void removeLobby(int lobbyId) {
        lobbies.remove(lobbyId);
    }

    public Map<Integer, Lobby> getAllLobbies() {
        return new ConcurrentHashMap<>(lobbies);
    }

    public int getLobbyCount() {
        return lobbies.size();
    }

    public void cleanupEmptyLobbies(long emptyTimeThresholdMs) {
        lobbies.entrySet().removeIf(entry -> {
            Lobby lobby = entry.getValue();
            if (lobby.isEmptyForLongerThan(emptyTimeThresholdMs)) {
                return true;
            }
            return false;
        });
    }
}

