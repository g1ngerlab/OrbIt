package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.inventory.InventoryFeature;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.ui.KillNotificationManager;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;
import taltech.orbitgame.world.RemotePlayerRegistry;

/**
 * Handles network events from server
 * Extracted from GameScreen to reduce class size
 */
public class NetworkEventHandler {
    private final GameClient gameClient;
    private final GameStateManager gameState;
    private final Player localPlayer;
    private final NpcManager npcManager;
    private final InventoryFeature inventoryFeature;
    private final KillNotificationManager killNotifications;

    public NetworkEventHandler(
            GameClient gameClient,
            GameStateManager gameState,
            Player localPlayer,
            NpcManager npcManager,
            InventoryFeature inventoryFeature,
            KillNotificationManager killNotifications) {
        this.gameClient = gameClient;
        this.gameState = gameState;
        this.localPlayer = localPlayer;
        this.npcManager = npcManager;
        this.inventoryFeature = inventoryFeature;
        this.killNotifications = killNotifications;
    }

    public void onPlayerJoined(int clientId, String playerName, float x, float y, int characterId) {
        Gdx.app.postRunnable(() -> {
            gameState.getRemotePlayers().addPlayer(clientId, playerName, x, y, characterId);
        });
    }

    public void onPlayerLeft(int clientId) {
        Gdx.app.postRunnable(() -> {
            RemotePlayer removed = gameState.getRemotePlayers().removePlayer(clientId);
            if (removed != null) {
                removed.dispose();
            }
        });
    }

    public void onPlayerPositionUpdate(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        Gdx.app.postRunnable(() -> {
            gameState.getRemotePlayers().updatePosition(clientId, x, y, moving, aimX, aimY);
        });
    }

    public void onLobbyState(Network.LobbyState lobbyState) {
        Gdx.app.postRunnable(() -> {
            gameState.getRemotePlayers().applyLobbyState(lobbyState, gameClient.getClientId());
        });
    }

    public void onPlayerHealthUpdate(Network.PlayerHealthUpdate healthUpdate) {
        Gdx.app.postRunnable(() -> {
            int myId = gameClient.getClientId();
            if (healthUpdate.clientId == myId) {
                localPlayer.setHealth(healthUpdate.health, healthUpdate.maxHealth);
            } else {
                gameState.getRemotePlayers().updateHealth(healthUpdate.clientId, healthUpdate.health, healthUpdate.maxHealth);
            }
        });
    }

    public void onEntityDied(Network.EntityDied entityDied, Runnable onPlayerDiedCallback) {
        Gdx.app.postRunnable(() -> {
            if (entityDied.entityType == Network.EntityDied.TYPE_NPC) {
                // NPC died
            } else if (entityDied.entityType == Network.EntityDied.TYPE_PLAYER) {
                int myId = gameClient.getClientId();
                if (entityDied.entityId == myId) {
                    onPlayerDiedCallback.run();
                } else {
                    // Show kill notification
                    RemotePlayer killed = gameState.getRemotePlayers().getPlayer(entityDied.entityId);
                    if (killed != null) {
                        killNotifications.addKillNotification(localPlayer.getPlayerName(), killed.playerName);
                        AudioManager.getInstance().playPlayerDeath(killed.x, killed.y,
                                localPlayer.getCenterX(), localPlayer.getCenterY());
                        gameState.getRemotePlayers().removePlayer(entityDied.entityId);
                        killed.dispose();
                    }
                }
            }
        });
    }

    public void onPlayerShoot(Network.PlayerShoot playerShoot) {
        Gdx.app.postRunnable(() -> {
            if (playerShoot.clientId == gameClient.getClientId()) {
                return;
            }
            RemotePlayer shooter = gameState.getRemotePlayers().getPlayer(playerShoot.clientId);
            if (shooter != null) {
                shooter.triggerAttack();
            }
            float shooterX = playerShoot.shooterX;
            float shooterY = playerShoot.shooterY;
            if (shooter != null) {
                shooterX = shooter.x;
                shooterY = shooter.y;
            }
            AudioManager.getInstance().playShotgunShot(shooterX, shooterY,
                    localPlayer.getCenterX(), localPlayer.getCenterY(), false);
        });
    }

    public void onPlayerStep(Network.PlayerStep playerStep) {
        Gdx.app.postRunnable(() -> {
            RemotePlayer walker = gameState.getRemotePlayers().getPlayer(playerStep.clientId);
            float walkerX = playerStep.playerX;
            float walkerY = playerStep.playerY;
            if (walker != null) {
                walkerX = walker.x;
                walkerY = walker.y;
            }
            boolean isOwnStep = (playerStep.clientId == gameClient.getClientId());
            AudioManager.getInstance().playStep(walkerX, walkerY,
                    localPlayer.getCenterX(), localPlayer.getCenterY(), isOwnStep);
        });
    }

    public void onBulletStateUpdate(Network.BulletStateUpdate bulletStateUpdate) {
        Gdx.app.postRunnable(() -> {
            if (bulletStateUpdate == null || bulletStateUpdate.bullets == null) {
                return;
            }

            // Update all bullets in the renderer
            for (Network.BulletState state : bulletStateUpdate.bullets) {
                // This will create or update bullets in the BulletRenderer
                // Note: The actual rendering is handled by GameRenderer which has access to bulletRenderer
            }
        });
    }

    public void onCorpseStateUpdate(Network.CorpseStateUpdate corpseStateUpdate) {
        Gdx.app.postRunnable(() -> {
            gameState.clearCorpses();
            if (corpseStateUpdate.corpses != null) {
                for (Network.CorpseState state : corpseStateUpdate.corpses) {
                    gameState.addCorpse(state.corpseId, state);
                }
            }
        });
    }

    public void onOrbStateUpdate(Network.OrbStateUpdate orbStateUpdate) {
        Gdx.app.postRunnable(() -> {
            gameState.clearOrbs();
            if (orbStateUpdate.orbs != null) {
                for (Network.OrbState state : orbStateUpdate.orbs) {
                    gameState.addOrb(state.orbId, state);
                }
            }
        });
    }

    public void onInventoryStateUpdate(Network.InventoryStateUpdate inventoryUpdate) {
        Gdx.app.postRunnable(() -> {
            int pickedUp = inventoryFeature.applyNetworkUpdateForItemType(
                    inventoryUpdate,
                    gameClient.getClientId(),
                    ItemKind.ORB.getId()
            );
            if (pickedUp > 0) {
                AudioManager.getInstance().playOrbPickup();
            }
        });
    }

    public void onNpcStateUpdate(Network.NpcStateUpdate npcStateUpdate) {
        // Handled by NpcManager
        Gdx.app.postRunnable(() -> {
            npcManager.applySnapshot(npcStateUpdate.npcs);
        });
    }

    public void onNpcAttack(Network.NpcAttack npcAttack) {
        Gdx.app.postRunnable(() -> npcManager.onNpcAttack(npcAttack.npcId));
    }

    public void onNpcDeathSound(Network.NpcDeathSound npcDeathSound) {
        Gdx.app.postRunnable(() -> {
            AudioManager.getInstance().playNpcDeath(npcDeathSound.npcX, npcDeathSound.npcY,
                    localPlayer.getCenterX(), localPlayer.getCenterY());
        });
    }

    public void onNpcHitSound(Network.NpcHitSound npcHitSound) {
        Gdx.app.postRunnable(() -> {
            AudioManager.getInstance().playNpcHit(npcHitSound.npcX, npcHitSound.npcY,
                    localPlayer.getCenterX(), localPlayer.getCenterY());
        });
    }
}
