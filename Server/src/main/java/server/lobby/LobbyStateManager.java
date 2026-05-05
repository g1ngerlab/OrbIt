package server.lobby;

import java.util.logging.Logger;

/**
 * Manages the state transitions and lifecycle of a lobby.
 * Handles closed state, empty lobby tracking, and reason tracking.
 */
public class LobbyStateManager {
    private static final Logger LOGGER = Logger.getLogger(LobbyStateManager.class.getName());

    private final int lobbyId;
    private boolean lobbyClosed;
    private long emptyTimestamp;

    public LobbyStateManager(int lobbyId) {
        this.lobbyId = lobbyId;
        this.lobbyClosed = false;
        this.emptyTimestamp = 0;
    }

    /**
     * Marks the lobby as closed.
     */
    public void closeLobby(String reason) {
        if (lobbyClosed) {
            return;
        }
        lobbyClosed = true;
        LOGGER.info(logTag() + "Lobby closed: " + reason);
    }

    /**
     * Records when the lobby became empty.
     */
    public void markEmpty() {
        if (emptyTimestamp == 0) {
            emptyTimestamp = System.currentTimeMillis();
            LOGGER.info(logTag() + "Lobby marked as empty");
        }
    }

    /**
     * Clears the empty timestamp when a player joins.
     */
    public void clearEmpty() {
        emptyTimestamp = 0;
    }

    /**
     * Checks if the lobby is closed.
     */
    public boolean isClosed() {
        return lobbyClosed;
    }

    /**
     * Checks if the lobby is empty and has been empty for longer than the given duration.
     */
    public boolean isEmptyForLongerThan(long durationMs) {
        return emptyTimestamp > 0 && (System.currentTimeMillis() - emptyTimestamp) > durationMs;
    }

    private String logTag() {
        return "[Lobby " + lobbyId + "] ";
    }
}
