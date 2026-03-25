package server.combat;

import server.math.CollisionMath;
import server.model.LobbyPlayer;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcMeleeSystem {
    public interface Callbacks {
        void onPlayerDamaged(LobbyPlayer player);
        void onPlayerKilled(LobbyPlayer player);
    }

    private final float playerWidth;
    private final float playerHeight;
    private final float meleeDamage;
    private final float meleeInterval;

    public NpcMeleeSystem(float playerWidth, float playerHeight, float meleeDamage, float meleeInterval) {
        this.playerWidth = playerWidth;
        this.playerHeight = playerHeight;
        this.meleeDamage = meleeDamage;
        this.meleeInterval = meleeInterval;
    }

    public void applyDamage(float deltaTime,
                            Map<Integer, LobbyPlayer> players,
                            NpcManager npcManager,
                            Callbacks callbacks) {
        List<Integer> deadPlayers = new ArrayList<>();

        for (LobbyPlayer player : players.values()) {
            if (player.isDead()) {
                continue;
            }

            player.tickMeleeDamageCooldown(deltaTime);

            if (player.getMeleeDamageCooldown() <= 0f && isAnyNpcTouchingPlayer(player, npcManager)) {
                player.takeDamage(meleeDamage);
                player.resetMeleeDamageCooldown(meleeInterval);
                callbacks.onPlayerDamaged(player);

                if (player.isDead()) {
                    callbacks.onPlayerKilled(player);
                    deadPlayers.add(player.getClientId());
                }
            }
        }

        for (Integer deadPlayerId : deadPlayers) {
            players.remove(deadPlayerId);
        }
    }

    private boolean isAnyNpcTouchingPlayer(LobbyPlayer player, NpcManager npcManager) {
        for (Npc npc : npcManager.getNpcs()) {
            if (!npc.isAlive()) {
                continue;
            }

            float half = npc.getSize() * 0.5f;
            boolean touching = CollisionMath.rectsOverlap(
                npc.getPosition().x - half,
                npc.getPosition().y - half,
                npc.getSize(),
                npc.getSize(),
                player.getX(),
                player.getY(),
                playerWidth,
                playerHeight
            );

            if (touching) {
                return true;
            }
        }

        return false;
    }
}

