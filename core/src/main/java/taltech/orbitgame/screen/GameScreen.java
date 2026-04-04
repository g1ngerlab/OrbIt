package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.InputHandler;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.extraction.ExtractionInputController;
import taltech.orbitgame.extraction.ExtractionManager;
import taltech.orbitgame.extraction.ExtractionPointRenderer;
import taltech.orbitgame.extraction.ui.ExtractionUiHud;
import taltech.orbitgame.inventory.InventoryFeature;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.player.PlayerController;
import taltech.orbitgame.player.PlayerProfile;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.rendering.GameRenderer;
import taltech.orbitgame.Main;
import taltech.orbitgame.world.MapManager;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main game screen that handles input and renders the game
 */
public class GameScreen implements Screen, GameClient.GameClientListener {
    private static final ExecutorService MAP_LOAD_EXECUTOR = Executors.newSingleThreadExecutor();

    private final Main game;
    private final InputHandler inputHandler;
    private final GameClient gameClient;
    private final LobbyInfoClient lobbyInfoClient;
    private MapManager map;
    private final Player player;
    private final NpcManager npcManager;
    private final Map<Integer, Network.CorpseState> corpses;
    private final Map<Integer, Network.OrbState> orbs;

    // Refactored components
    private PlayerController playerController;
    private final GameRenderer gameRenderer;

    private final Map<Integer, RemotePlayer> remotePlayers;

    private boolean isInLobby = false;
    private boolean isPaused = false;
    private boolean leavingGame = false;
    private boolean loggedEmptyNpcSnapshot = false;
    private boolean disposed = false;
    private final InventoryFeature inventoryFeature;
    private CompletableFuture<MapManager.LoadedMapData> mapLoadFuture;
    private Throwable mapLoadError;
    private final long mapLoadStartedAtMs;

    // Extraction points system
    private final ExtractionManager extractionManager;
    private ExtractionPointRenderer extractionPointRenderer;
    private ExtractionInputController extractionInputController;
    private ExtractionUiHud extractionUiHud;

    // Debugging
    private boolean debugCoordsVisible = false;

    public GameScreen(Main game, GameClient gameClient) {
        this(game, gameClient, null);
    }

    public GameScreen(Main game, GameClient gameClient, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.gameClient = gameClient;
        this.lobbyInfoClient = lobbyInfoClient;
        this.gameClient.setGameListener(this);
        this.inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(this.inputHandler);

        // Stop lobby music and start game session music
        AudioManager.getInstance().playRandomGameSessionMusic();

        CharacterType characterType = CharacterType.fromId(
            PlayerProfile.getSelectedCharacterId()
        );
        this.player = new Player(characterType);
        this.player.setPlayerName(gameClient.getPlayerName());
        this.remotePlayers = new HashMap<>();

        // NPCs are now created and simulated only on server
        this.npcManager = new NpcManager();
        this.corpses = new HashMap<>();
        this.orbs = new HashMap<>();

        // Initialize renderer
        this.gameRenderer = new GameRenderer(player.getCharacterType());
        this.inventoryFeature = new InventoryFeature();

        // Initialize extraction points system
        this.extractionManager = new ExtractionManager();

        this.mapLoadStartedAtMs = System.currentTimeMillis();
        this.mapLoadFuture = CompletableFuture.supplyAsync(MapManager::loadData, MAP_LOAD_EXECUTOR);

        System.out.println("[GameScreen] Screen initialized, map loading in background...");
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugCoordsVisible = !debugCoordsVisible;
        }

        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        completeMapLoadingIfReady();

        if (mapLoadError != null) {
            System.err.println("[GameScreen] Map load failed: " + mapLoadError.getMessage());
            gameRenderer.renderStatus("Map load failed. Check logs.");
            return;
        }

        // Handle ESC key to open menu
        if (!isPaused && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new GameEscScreen(game, this, lobbyInfoClient));
            return;
        }

        // Handle inventory input before gameplay input to avoid click-through actions.
        inventoryFeature.handleInput(
            isInLobby,
            isPaused,
            gameRenderer::getInventorySlotAtScreenPosition,
            gameClient::sendDropItem,
            gameClient::sendMoveInventoryItem
        );

        // Update player and game state if not paused and connected
        if (!isPaused && isInLobby && playerController != null) {
            boolean cursorInsideInventory = gameRenderer.isInventoryScreenPositionInside(
                Gdx.input.getX(),
                Gdx.input.getY()
            );
            playerController.setShootingBlocked(cursorInsideInventory);
            playerController.update(delta, isInLobby);

            // Update extraction input
            extractionInputController.update(delta, player.getCenterX(), player.getCenterY());

            for (RemotePlayer remotePlayer : remotePlayers.values()) {
                remotePlayer.update(delta);
            }

            // Update extraction manager
            extractionManager.update(delta);
        }

        // Delegate rendering to GameRenderer

        gameRenderer.setOrbHudState(orbs);
        gameRenderer.setInventoryState(inventoryFeature.getState());
        gameRenderer.setDebugOverlayVisible(debugCoordsVisible);
        gameRenderer.render(delta, isInLobby, player, remotePlayers, map, npcManager, corpses, playerController);

        // Render extraction points
        if (isInLobby
            && extractionPointRenderer != null
            && extractionUiHud != null
            && extractionInputController != null) {
            extractionPointRenderer.render(gameRenderer.getBatch());
            extractionUiHud.setNearbyPoint(extractionInputController.getNearbyPoint());
            extractionUiHud.setExtractingPoint(extractionInputController.getExtractingPoint());
            extractionUiHud.setExtractionTimer(extractionInputController.getRemainingTime());
            extractionUiHud.render(gameRenderer.getBatch());
        }

        // Send updates to server if not paused and connected
        if (!isPaused && isInLobby && playerController != null) {
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
            if (map != null) {
                map.dispose();
            }
            player.dispose();
            npcManager.dispose();
            gameRenderer.dispose();
            extractionManager.dispose();
            if (extractionPointRenderer != null) {
                extractionPointRenderer.dispose();
            }
            if (extractionUiHud != null) {
                extractionUiHud.dispose();
            }
            for (RemotePlayer remotePlayer : remotePlayers.values()) {
                remotePlayer.dispose();
            }
        } catch (Exception e) {
            System.err.println("[GameScreen] Error disposing resources: " + e.getMessage());
        }

        System.out.println("[GameScreen] Disposal complete");
    }

    private void completeMapLoadingIfReady() {
        if (map != null || mapLoadFuture == null || !mapLoadFuture.isDone()) {
            return;
        }

        try {
            MapManager.LoadedMapData loadedData = mapLoadFuture.get();
            this.map = new MapManager(loadedData);
            float speed = 120f;
            this.playerController = new PlayerController(
                player,
                inputHandler,
                gameClient,
                map,
                gameRenderer.getCameraController().getCamera(),
                speed
            );

            // Initialize extraction system
            this.extractionPointRenderer = new ExtractionPointRenderer(extractionManager);
            this.extractionInputController = new ExtractionInputController(
                extractionManager,
                gameClient,
                gameClient.getClientId()
            );
            this.extractionInputController.setExtractionListener(this::leaveToMainMenuLikeEscLeave);
            this.extractionUiHud = new ExtractionUiHud(gameClient.getClientId());

            System.out.println("[GameScreen] Map ready. Collision grid=" + map.getCollisionGridSize()
                + ", render chunks=" + map.getRenderChunkCount()
                + ", total async load=" + (System.currentTimeMillis() - mapLoadStartedAtMs) + " ms");
        } catch (Exception e) {
            mapLoadError = e;
            e.printStackTrace();
        } finally {
            mapLoadFuture = null;
        }
    }

    // GameClientListener callbacks
    @Override
    public void onPlayerJoined(int clientId, String playerName, float x, float y, int characterId) {
        Gdx.app.postRunnable(() -> {
            System.out.println("[GameScreen] Adding remote player: " + playerName + " at (" + x + ", " + y + ")");
            RemotePlayer remotePlayer = new RemotePlayer(
                clientId,
                playerName,
                x,
                y,
                CharacterType.fromId(characterId)
            );
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
                        playerInfo.y,
                        CharacterType.fromId(playerInfo.characterId)
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
            if (playerController != null) {
                playerController.resetAim();
            }
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
    public void onNpcAttack(Network.NpcAttack npcAttack) {
        Gdx.app.postRunnable(() -> npcManager.onNpcAttack(npcAttack.npcId));
    }

    @Override
    public void onBulletStateUpdate(Network.BulletStateUpdate bulletStateUpdate) {
        Gdx.app.postRunnable(() -> {
            Set<Integer> seen = new HashSet<>();
            for (Network.BulletState state : bulletStateUpdate.bullets) {
                seen.add(state.bulletId);
                gameRenderer.getBulletRenderer().updateBullet(
                    state.bulletId,
                    state.x,
                    state.y,
                    state.angleDeg,
                    state.radius,
                    state.bulletType
                );
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
    public void onOrbStateUpdate(Network.OrbStateUpdate orbStateUpdate) {
        Gdx.app.postRunnable(() -> {
            orbs.clear();
            if (orbStateUpdate.orbs == null) {
                return;
            }
            for (Network.OrbState state : orbStateUpdate.orbs) {
                orbs.put(state.orbId, state);
            }
        });
    }

    @Override
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
                        // Play player death sound
                        AudioManager.getInstance().playPlayerDeath(removed.x, removed.y,
                                                                    player.getCenterX(), player.getCenterY());
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

    @Override
    public void onPlayerShoot(Network.PlayerShoot playerShoot) {
        Gdx.app.postRunnable(() -> {
            if (playerShoot.clientId == gameClient.getClientId()) {
                // Own shot - play at full volume (this is handled in PlayerController)
                return;
            }
            // Other player's shot - play sound with distance attenuation
            RemotePlayer shooter = remotePlayers.get(playerShoot.clientId);
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
                                                       player.getCenterX(), player.getCenterY(), false);
        });
    }

    @Override
    public void onPlayerStep(Network.PlayerStep playerStep) {
        Gdx.app.postRunnable(() -> {
            RemotePlayer walker = remotePlayers.get(playerStep.clientId);
            float walkerX = playerStep.playerX;
            float walkerY = playerStep.playerY;

            if (walker != null) {
                walkerX = walker.x;
                walkerY = walker.y;
            }

            boolean isOwnStep = (playerStep.clientId == gameClient.getClientId());
            AudioManager.getInstance().playStep(walkerX, walkerY,
                                                player.getCenterX(), player.getCenterY(), isOwnStep);
        });
    }

    @Override
    public void onNpcDeathSound(Network.NpcDeathSound npcDeathSound) {
        Gdx.app.postRunnable(() -> {
            AudioManager.getInstance().playNpcDeath(npcDeathSound.npcX, npcDeathSound.npcY,
                                                     player.getCenterX(), player.getCenterY());
        });
    }

    @Override
    public void onNpcHitSound(Network.NpcHitSound npcHitSound) {
        Gdx.app.postRunnable(() -> {
            AudioManager.getInstance().playNpcHit(npcHitSound.npcX, npcHitSound.npcY,
                                                   player.getCenterX(), player.getCenterY());
        });
    }

    @Override
    public void onExtractionPointStateUpdate(Network.ExtractionPointStateUpdate stateUpdate) {
        Gdx.app.postRunnable(() -> {
            if (extractionInputController != null) {
                extractionInputController.applyServerUpdate(
                    stateUpdate.extractionPointId,
                    stateUpdate.state,
                    stateUpdate.activatingClientId,
                    stateUpdate.remainingTime
                );
            }
        });
    }

    public boolean isDisposed() {
        return disposed;
    }

    private void leaveToMainMenuLikeEscLeave() {
        if (leavingGame || disposed) {
            return;
        }
        leavingGame = true;
        isPaused = true;

        Gdx.app.postRunnable(() -> {
            if (!disposed) {
                dispose();
            }
            AudioManager.getInstance().stopCurrentMusic();
            AudioManager.getInstance().playRandomLobbyMusic();
            LobbyInfoClient clientToUse = (lobbyInfoClient != null) ? lobbyInfoClient : new LobbyInfoClient();
            game.setScreen(new MainMenuScreen(game, clientToUse));
        });
    }

}
