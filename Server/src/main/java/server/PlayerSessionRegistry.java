package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for tracking which players are connected and which lobby they're in.
 */
public class PlayerSessionRegistry {
    private final Map<Integer, String> connectedClients;
    private final Map<Integer, Integer> clientToLobby;

    public PlayerSessionRegistry() {
        this.connectedClients = new ConcurrentHashMap<>();
        this.clientToLobby = new ConcurrentHashMap<>();
    }

    public void registerClient(int clientId, String playerName) {
        connectedClients.put(clientId, playerName);
    }

    public void unregisterClient(int clientId) {
        connectedClients.remove(clientId);
        clientToLobby.remove(clientId);
    }

    public String getClientName(int clientId) {
        return connectedClients.get(clientId);
    }

    public boolean isClientConnected(int clientId) {
        return connectedClients.containsKey(clientId);
    }

    public void assignClientToLobby(int clientId, int lobbyId) {
        clientToLobby.put(clientId, lobbyId);
    }

    public Integer getClientLobby(int clientId) {
        return clientToLobby.get(clientId);
    }

    public int getConnectedClientCount() {
        return connectedClients.size();
    }

    public Map<Integer, String> getAllConnectedClients() {
        return new ConcurrentHashMap<>(connectedClients);
    }
}

