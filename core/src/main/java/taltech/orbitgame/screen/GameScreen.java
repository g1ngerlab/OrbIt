package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import taltech.orbitgame.camera.CameraController;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.InputHandler;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.player.PlayerController;
import taltech.orbitgame.rendering.BulletRenderer;
import taltech.orbitgame.rendering.CursorManager;
import taltech.orbitgame.rendering.WeaponRenderer;
import taltech.orbitgame.ui.HealthBarRenderer;
import taltech.orbitgame.util.FontManager;
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

    private final InputHandler inputHandler;
    private final GameClient gameClient;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final HealthBarRenderer healthBarRenderer;
    private final MapManager map;
    private final Player player;
    private final NpcManager npcManager;

    // Refactored components
    private final PlayerController playerController;
    private final CameraController cameraController;
    private final BulletRenderer bulletRenderer;
    private final WeaponRenderer weaponRenderer;
    private final CursorManager cursorManager;

    private Map<Integer, RemotePlayer> remotePlayers;

    private static final int CROSSHAIR_SCALE = 2;

    private boolean isInLobby = false;
    private boolean loggedEmptyNpcSnapshot = false;
    private BitmapFont font;

    public GameScreen(GameClient gameClient) {
        this.gameClient = gameClient;
        this.gameClient.setGameListener(this);
        this.inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(this.inputHandler);

        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.healthBarRenderer = new HealthBarRenderer(shapeRenderer);

        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        this.cameraController = new CameraController(camera);

        this.map = new MapManager();
        this.player = new Player();
        this.remotePlayers = new HashMap<>();
        this.font = FontManager.getMenuFont();

        // NPCs are now created and simulated only on server
        this.npcManager = new NpcManager();

        // Initialize refactored components
        float speed = 120f;
        this.playerController = new PlayerController(player, inputHandler, gameClient, map, camera, speed);
        this.bulletRenderer = new BulletRenderer();
        this.weaponRenderer = new WeaponRenderer();
        this.cursorManager = new CursorManager("guns/crosshair.png", CROSSHAIR_SCALE);

        System.out.println("[GameScreen] Screen initialized, waiting for connection...");
    }

    @Override
    public void show() {
        cursorManager.setCrosshairCursor();
        System.out.println("[GameScreen] Screen shown");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!isInLobby) {
            batch.begin();
            String message = "Connecting to lobby...";
            font.getData().setScale(1.5f);
            float textWidth = font.draw(batch, message, 0, 0).width;
            font.draw(batch, message,
                (Gdx.graphics.getWidth() - textWidth) / 2,
                Gdx.graphics.getHeight() / 2);
            batch.end();
            return;
        }

        // Update player and game state
        playerController.update(delta, isInLobby);

        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.update(delta, false);
        }

        // Update camera
        cameraController.updateCamera(player, map.getWidthPx(), map.getHeightPx());
        cameraController.getCamera().update();
        batch.setProjectionMatrix(cameraController.getCamera().combined);
        shapeRenderer.setProjectionMatrix(cameraController.getCamera().combined);

        // Render game objects
        batch.enableBlending();
        batch.begin();
        map.render(batch);
        npcManager.draw(batch);

        // Render remote players with weapons
        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.render(batch);
            weaponRenderer.drawShotgun(
                batch,
                remotePlayer.getCenterX(),
                remotePlayer.getCenterY() + 8f,
                remotePlayer.getAimX(),
                remotePlayer.getAimY()
            );
        }

        // Render local player with weapon
        player.render(batch);
        weaponRenderer.drawShotgun(
            batch,
            player.getCenterX(),
            player.getCenterY() + 8f,
            playerController.getLocalAimX(),
            playerController.getLocalAimY()
        );

        // Render bullets
        bulletRenderer.render(batch);
        batch.end();

        // Render health bars
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Npc npc : npcManager.getNpcs()) {
            if (npc.isAlive()) {
                healthBarRenderer.drawHealthBar(
                    npc.getPosition().x - npc.getSize() / 2,
                    npc.getPosition().y + npc.getSize() / 2,
                    npc.getSize(),
                    npc.getHealthPercentage()
                );
            }
        }

        healthBarRenderer.drawHealthBar(
            player.x,
            player.y + player.getHeight(),
            player.getWidth(),
            player.getHealthPercentage()
        );

        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            healthBarRenderer.drawHealthBar(
                remotePlayer.x,
                remotePlayer.y + remotePlayer.getHeight(),
                remotePlayer.getWidth(),
                remotePlayer.getHealthPercentage()
            );
        }

        shapeRenderer.end();

        // Send updates to server
        playerController.sendInputToServer(isInLobby);
        playerController.sendPositionUpdate(delta, isInLobby);
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        cursorManager.resetCursor();
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        map.dispose();
        player.dispose();
        npcManager.dispose();
        bulletRenderer.dispose();
        weaponRenderer.dispose();
        cursorManager.dispose();
        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.dispose();
        }
        gameClient.disconnect();
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
                bulletRenderer.updateBullet(state.bulletId, state.x, state.y, state.angleDeg, state.radius);
            }
            bulletRenderer.cleanupBullets(seen);
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
}
