package taltech.orbitgame.screen;

import taltech.orbitgame.network.Network;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;
import taltech.orbitgame.world.RemotePlayerRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages game state (players, health, lobby timer, etc.)
 * Extracted from GameScreen to reduce class size
 */
public class GameStateManager {
    private boolean isInLobby = false;
    private int lobbyTimerRemainingMs = 10 * 60 * 1000;
    private boolean lobbyFreezePhase = false;
    private boolean loggedEmptyNpcSnapshot = false;

    private final Map<Integer, Network.CorpseState> corpses;
    private final Map<Integer, Network.OrbState> orbs;
    private final RemotePlayerRegistry remotePlayers;

    public GameStateManager(RemotePlayerRegistry remotePlayers) {
        this.remotePlayers = remotePlayers;
        this.corpses = new HashMap<>();
        this.orbs = new HashMap<>();
    }

    // Lobby state
    public void setInLobby(boolean inLobby) {
        this.isInLobby = inLobby;
    }

    public boolean isInLobby() {
        return isInLobby;
    }

    public void updateLobbyTimer(int remainingMs, boolean freezePhase) {
        this.lobbyTimerRemainingMs = Math.max(0, remainingMs);
        this.lobbyFreezePhase = freezePhase;
    }

    public int getLobbyTimerRemainingMs() {
        return lobbyTimerRemainingMs;
    }

    public boolean isLobbyFreezePhase() {
        return lobbyFreezePhase;
    }

    // NPC snapshots
    public void setLoggedEmptyNpcSnapshot(boolean logged) {
        this.loggedEmptyNpcSnapshot = logged;
    }

    public boolean hasLoggedEmptyNpcSnapshot() {
        return loggedEmptyNpcSnapshot;
    }

    // Corpses
    public Map<Integer, Network.CorpseState> getCorpses() {
        return corpses;
    }

    public void clearCorpses() {
        corpses.clear();
    }

    public void addCorpse(int corpseId, Network.CorpseState state) {
        corpses.put(corpseId, state);
    }

    // Orbs
    public Map<Integer, Network.OrbState> getOrbs() {
        return orbs;
    }

    public void clearOrbs() {
        orbs.clear();
    }

    public void addOrb(int orbId, Network.OrbState state) {
        orbs.put(orbId, state);
    }

    // Remote players
    public RemotePlayerRegistry getRemotePlayers() {
        return remotePlayers;
    }
}
