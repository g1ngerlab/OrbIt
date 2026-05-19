package server.lobby;

import server.model.LobbyPlayer;
import taltech.orbitgame.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manages the lobby timer and freeze damage logic.
 * Handles lobby duration tracking and damage application during freeze phase.
 */
public class LobbyTimerManager {
    private static final Logger LOGGER = Logger.getLogger(LobbyTimerManager.class.getName());

    private static final long LOBBY_DURATION_MS = 10L * 60L * 1000L;
    private static final long FREEZE_PHASE_START_MS = 60L * 1000L;
    private static final long FREEZE_DAMAGE_DURATION_MS = 10L * 1000L;

    private final int lobbyId;
    private final Map<Integer, LobbyPlayer> players;
    private final LobbyBroadcaster broadcaster;

    private long lobbyElapsedMs;
    private long freezeDamageElapsedMs;

    public LobbyTimerManager(int lobbyId, Map<Integer, LobbyPlayer> players, LobbyBroadcaster broadcaster) {
        this.lobbyId = lobbyId;
        this.players = players;
        this.broadcaster = broadcaster;
        this.lobbyElapsedMs = 0L;
        this.freezeDamageElapsedMs = 0L;
    }

    /**
     * Updates the lobby timer and applies freeze damage if needed.
     * Returns callback for killed players during freeze.
     */
    public List<LobbyPlayer> updateTimer(float deltaTime) {
        lobbyElapsedMs = Math.min(LOBBY_DURATION_MS, lobbyElapsedMs + (long) (Math.max(0f, deltaTime) * 1000f));
        long remainingMs = getRemainingTimeMs();
        boolean freezePhase = remainingMs <= FREEZE_PHASE_START_MS;

        List<LobbyPlayer> killedPlayers = new ArrayList<>();

        if (freezePhase && freezeDamageElapsedMs < FREEZE_DAMAGE_DURATION_MS && !players.isEmpty()) {
            killedPlayers = applyFreezeDamage(deltaTime);
            freezeDamageElapsedMs = Math.min(FREEZE_DAMAGE_DURATION_MS, freezeDamageElapsedMs + (long) (Math.max(0f, deltaTime) * 1000f));
        }

        broadcaster.broadcastToAll(new Network.LobbyTimerState(lobbyId, (int) remainingMs, freezePhase));

        return killedPlayers;
    }

    /**
     * Gets the remaining lobby time in milliseconds.
     */
    public long getRemainingTimeMs() {
        return Math.max(0L, LOBBY_DURATION_MS - lobbyElapsedMs);
    }

    /**
     * Checks if the lobby time has expired.
     */
    public boolean isTimeExpired() {
        return getRemainingTimeMs() <= 0L;
    }

    /**
     * Advances to the final minute for testing purposes.
     */
    public void forceAdvanceToFinalMinute() {
        lobbyElapsedMs = Math.max(lobbyElapsedMs, LOBBY_DURATION_MS - FREEZE_PHASE_START_MS);
        freezeDamageElapsedMs = 0L;
        broadcaster.broadcastToAll(new Network.LobbyTimerState(lobbyId, (int) getRemainingTimeMs(), true));
    }

    /**
     * Applies freeze damage to all alive players.
     * Returns list of players killed by freeze damage.
     */
    private List<LobbyPlayer> applyFreezeDamage(float deltaTime) {
        List<LobbyPlayer> killedPlayers = new ArrayList<>();

        if (players.isEmpty()) {
            return killedPlayers;
        }

        float damageFactor = Math.max(0f, deltaTime / (FREEZE_DAMAGE_DURATION_MS / 1000f));

        for (LobbyPlayer player : new ArrayList<>(players.values())) {
            if (player.isDead()) {
                continue;
            }
            float damage = player.getMaxHealth() * damageFactor;
            player.takeDamage(damage);
            broadcaster.broadcastToAll(new Network.PlayerHealthUpdate(
                player.getClientId(),
                player.getCurrentHealth(),
                player.getMaxHealth()
            ));
            if (player.isDead()) {
                killedPlayers.add(player);
            }
        }

        return killedPlayers;
    }

    private String logTag() {
        return "[Lobby " + lobbyId + "] ";
    }
}
