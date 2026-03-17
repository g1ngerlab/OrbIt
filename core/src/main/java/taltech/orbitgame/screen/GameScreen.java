package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import taltech.orbitgame.Main; // Add import

import com.badlogic.gdx.graphics.GL20;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.InputHandler;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.player.PlayerController;
import taltech.orbitgame.rendering.GameRenderer;
import taltech.orbitgame.world.MapManager;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Main game screen that handles input and renders the game
 */
public class GameScreen implements Screen, GameClient.GameClientListener {

    private final Main game;
    private final InputHandler inputHandler;
    private final GameClient gameClient;
    private final MapManager map;
    private final Player player;
    private final NpcManager npcManager;
    private final Map<Integer, Network.CorpseState> corpses;

    // Refactored components
    private final PlayerController playerController;
    private final GameRenderer gameRenderer;

    private final Map<Integer, RemotePlayer> remotePlayers;

    private boolean isInLobby = false;
    private boolean isPaused = false;
    private boolean loggedEmptyNpcSnapshot = false;
    private boolean disposed = false;

    public GameScreen(Main game, GameClient gameClient) {
        this.game = game;
        this.gameClient = gameClient;
        this.gameClient.setGameListener(this);
        this.inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(this.inputHandler);

        this.map = new MapManager();
        this.player = new Player();
        this.remotePlayers = new HashMap<>();

        // NPCs are now created and simulated only on server
        this.npcManager = new NpcManager();
        this.corpses = new HashMap<>();

        // Initialize renderer
        this.gameRenderer = new GameRenderer();

        // Initialize refactored components
        float speed = 120f;
        this.playerController = new PlayerController(player, inputHandler, gameClient, map, gameRenderer.getCameraController().getCamera(), speed);

        System.out.println("[GameScreen] Screen initialized, waiting for connection...");
    }

    @Override
    public void show() {
        gameRenderer.setCrosshairCursor();
        Gdx.input.setInputProcessor(inputHandler);
        System.out.println("[GameScreen] Screen shown");
    }

    @Override
    public void render(float delta) {
        // Don't render if already disposed
        if (disposed) {
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Handle ESC key to open menu
        if (!isPaused && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // Need to pass 'this' (GameScreen) to GameEscScreen?
            // Yes, checking GameEscScreen constructor: public GameEscScreen(Main game, GameScreen gameScreen)
            game.setScreen(new GameEscScreen(game, this));
            return;
        }

        // Update player and game state if not paused and connected
        if (!isPaused && isInLobby) {
            playerController.update(delta, isInLobby);

            for (RemotePlayer remotePlayer : remotePlayers.values()) {
                remotePlayer.update(delta, false);
            }
        }

        // Delegate rendering to GameRenderer
        gameRenderer.render(delta, isInLobby, player, remotePlayers, map, npcManager, corpses, playerController);

        // Send updates to server if not paused and connected
        if (!isPaused && isInLobby) {
            playerController.sendInputToServer(isInLobby);
            playerController.sendPositionUpdate(delta, isInLobby);
        }
    }

    @Override
    public void resize(int width, int height) {
        gameRenderer.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        gameRenderer.resetCursor();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return; // Already disposed
        }
        disposed = true;

        System.out.println("[GameScreen] Disposing resources...");

        // Disconnect from server first (in a non-blocking way)
        try {
            gameClient.disconnect();
        } catch (Exception e) {
            System.err.println("[GameScreen] Error disconnecting: " + e.getMessage());
        }

        // Dispose resources
        try {
            map.dispose();
            player.dispose();
            npcManager.dispose();
            gameRenderer.dispose();
            for (RemotePlayer remotePlayer : remotePlayers.values()) {
                remotePlayer.dispose();
            }
        } catch (Exception e) {
            System.err.println("[GameScreen] Error disposing resources: " + e.getMessage());
        }

        System.out.println("[GameScreen] Disposal complete");
    }

    // GameClientListener callbacks
    @Override
    public void onPlayerJoined(int clientId, String playerName, float x, float y) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[GameScreen] Adding remote player: " + playerName + " at (" + x + ", " + y + ")");
            RemotePlayer remotePlayer = new RemotePlayer(clientId, playerName, x, y);
            remotePlayers.put(clientId, remotePlayer);
        });
    }

    @Override
    public void onPlayerLeft(int clientId) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[GameScreen] Removing remote player: " + clientId);
            RemotePlayer removed = remotePlayers.remove(clientId);
            if (removed != null) {
                removed.dispose();
            }
        });
    }

    @Override
    public void onPlayerPositionUpdate(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        Gdx.app.postRunnable(() -> {
            RemotePlayer remotePlayer = remotePlayers.get(clientId);
            if (remotePlayer != null) {
                remotePlayer.updatePosition(x, y, moving, aimX, aimY);
            }
        });
    }

    @Override
    public void onLobbyState(Network.LobbyState lobbyState) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[GameScreen] Received lobby state with " + lobbyState.players.size() + " players");
            for (Network.PlayerInfo playerInfo : lobbyState.players) {
                if (playerInfo.clientId != gameClient.getClientId()) {
                    RemotePlayer remotePlayer = new RemotePlayer(
                        playerInfo.clientId,
                        playerInfo.playerName,
                        playerInfo.x,
                        playerInfo.y
                    );
                    remotePlayer.updatePosition(playerInfo.x, playerInfo.y, false, playerInfo.aimX, playerInfo.aimY);
                    remotePlayers.put(playerInfo.clientId, remotePlayer);
                }
            }
        });
    }

    @Override
    public void onLobbyJoinConfirmed(float spawnX, float spawnY) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[GameScreen] Lobby join confirmed! Spawning player at (" + spawnX + ", " + spawnY + ")");
            player.x = spawnX;
            player.y = spawnY;
            playerController.resetAim();
            isInLobby = true;
        });
    }

    @Override
    public void onNpcStateUpdate(Network.NpcStateUpdate npcStateUpdate) {
        Gdx.app.postRunnable(() -> {
            if ((npcStateUpdate.npcs == null || npcStateUpdate.npcs.isEmpty()) && !loggedEmptyNpcSnapshot) {
                System.out.println("[GameScreen] WARNING: Received empty NPC snapshot.");
                loggedEmptyNpcSnapshot = true;
            }
            if (npcStateUpdate.npcs != null && !npcStateUpdate.npcs.isEmpty() && loggedEmptyNpcSnapshot) {
                System.out.println("[GameScreen] NPC snapshots recovered, count=" + npcStateUpdate.npcs.size());
                loggedEmptyNpcSnapshot = false;
            }
            npcManager.applySnapshot(npcStateUpdate.npcs);
        });
    }

    @Override
    public void onBulletStateUpdate(Network.BulletStateUpdate bulletStateUpdate) {
        Gdx.app.postRunnable(() -> {
            Set<Integer> seen = new HashSet<>();
            for (Network.BulletState state : bulletStateUpdate.bullets) {
                seen.add(state.bulletId);
                gameRenderer.getBulletRenderer().updateBullet(state.bulletId, state.x, state.y, state.angleDeg, state.radius);
            }
            gameRenderer.getBulletRenderer().cleanupBullets(seen);
        });
    }

    @Override
    public void onCorpseStateUpdate(Network.CorpseStateUpdate corpseStateUpdate) {
        Gdx.app.postRunnable(() -> {
            corpses.clear();
            if (corpseStateUpdate.corpses == null) {
                return;
            }
            for (Network.CorpseState state : corpseStateUpdate.corpses) {
                corpses.put(state.corpseId, state);
            }
        });
    }

    @Override
    public void onPlayerHealthUpdate(Network.PlayerHealthUpdate healthUpdate) {
        Gdx.app.postRunnable(() -> {
            int myId = gameClient.getClientId();
            if (healthUpdate.clientId == myId) {
                // Update local player health bar
                player.setHealth(healthUpdate.health, healthUpdate.maxHealth);
            } else {
                RemotePlayer remotePlayer = remotePlayers.get(healthUpdate.clientId);
                if (remotePlayer != null) {
                    remotePlayer.setHealth(healthUpdate.health, healthUpdate.maxHealth);
                }
            }
        });
    }

    @Override
    public void onEntityDied(Network.EntityDied entityDied) {
        Gdx.app.postRunnable(() -> {
            if (entityDied.entityType == Network.EntityDied.TYPE_NPC) {
                // NPC will be removed from npcManager on the next snapshot update automatically.
                System.out.println("[GameScreen] NPC #" + entityDied.entityId + " died.");
            } else if (entityDied.entityType == Network.EntityDied.TYPE_PLAYER) {
                int myId = gameClient.getClientId();
                if (entityDied.entityId == myId) {
                    // Local player died - show death screen or return to menu
                    System.out.println("[GameScreen] You died!");
                    isInLobby = false;
                    // Disconnect and return to main menu
                    gameClient.disconnect();
                } else {
                    // A remote player died - remove them from the world
                    RemotePlayer removed = remotePlayers.remove(entityDied.entityId);
                    if (removed != null) {
                        removed.dispose();
                        System.out.println("[GameScreen] Remote player #" + entityDied.entityId + " died and was removed.");
                    }
                }
            }
        });
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isDisposed() {
        return disposed;
    }

}
