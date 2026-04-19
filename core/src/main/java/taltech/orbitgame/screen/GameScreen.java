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
import taltech.orbitgame.loot.PlayerLootService;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.player.PlayerController;
import taltech.orbitgame.player.PlayerProfile;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.rendering.GameRenderer;
import taltech.orbitgame.Main;
import taltech.orbitgame.ui.KillNotificationManager;
import taltech.orbitgame.world.MapManager;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;
import taltech.orbitgame.world.RemotePlayerRegistry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main game screen - delegates to helper classes for state management and event handling
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

    // Refactored components
    private PlayerController playerController;
    private final GameRenderer gameRenderer;
    private final RemotePlayerRegistry remotePlayers;
    private final GameSessionExitCoordinator sessionExitCoordinator;

    // Extracted managers
    private final GameStateManager gameState;
    private final NetworkEventHandler networkHandler;
    private final KillNotificationManager killNotifications;

    private boolean isPaused = false;
    private boolean leavingGame = false;
    private boolean disposed = false;
    private final InventoryFeature inventoryFeature;
    private final PlayerLootService playerLootService;
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

    // Death/Extraction state
    private String killedByName = null;
    private boolean playerDead = false;
    private String shopStatusMessage;
    private boolean shopPurchaseInProgress;
    private int pendingShopItemType = ItemKind.NONE.getId();
    private int pendingShopAmount;
    private int pendingShopOrbCost;

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

        AudioManager.getInstance().playRandomGameSessionMusic();

        CharacterType characterType = CharacterType.fromId(PlayerProfile.getSelectedCharacterId());
        this.player = new Player(characterType);
        this.player.setPlayerName(gameClient.getPlayerName());
        this.remotePlayers = new RemotePlayerRegistry();

        this.npcManager = new NpcManager();
        this.gameRenderer = new GameRenderer(player.getCharacterType());
        this.inventoryFeature = new InventoryFeature();
        this.playerLootService = new PlayerLootService();

        this.extractionManager = new ExtractionManager();
        this.sessionExitCoordinator = new GameSessionExitCoordinator(game, lobbyInfoClient, playerLootService);

        // Initialize extracted managers
        this.gameState = new GameStateManager(remotePlayers);
        this.killNotifications = new KillNotificationManager();
        this.networkHandler = new NetworkEventHandler(
            gameClient, gameState, player, npcManager, inventoryFeature, killNotifications
        );

        this.mapLoadStartedAtMs = System.currentTimeMillis();
        this.mapLoadFuture = CompletableFuture.supplyAsync(MapManager::loadData, MAP_LOAD_EXECUTOR);
    }

    @Override
    public void show() {
        gameRenderer.setCrosshairCursor();
        Gdx.input.setInputProcessor(inputHandler);
    }

    @Override
    public void render(float delta) {
        if (disposed) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debugCoordsVisible = !debugCoordsVisible;
        }

        float scrollY = inputHandler.consumeScrollY();
        if (scrollY != 0f) {
            gameRenderer.getCameraController().applyZoom(scrollY);
        }

        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        completeMapLoadingIfReady();

        if (mapLoadError != null) {
            System.err.println("[GameScreen] Map load failed: " + mapLoadError.getMessage());
            gameRenderer.renderStatus("Map load failed. Check logs.");
            return;
        }

        if (!isPaused && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new GameEscScreen(game, this, lobbyInfoClient));
            return;
        }

        inventoryFeature.handleInput(
            gameState.isInLobby(),
            isPaused,
            gameRenderer::getInventorySlotAtScreenPosition,
            gameClient::sendDropItem,
            gameClient::sendMoveInventoryItem
        );

        if (!isPaused && gameState.isInLobby() && playerController != null) {
            boolean cursorInsideInventory = gameRenderer.isInventoryScreenPositionInside(
                Gdx.input.getX(), Gdx.input.getY()
            );
            playerController.setShootingBlocked(cursorInsideInventory);
            playerController.update(delta, gameState.isInLobby());

            if (extractionInputController != null) {
                extractionInputController.setExtractionLocked(gameState.isLobbyFreezePhase());
            }
            extractionInputController.update(delta, player.getCenterX(), player.getCenterY());

            for (RemotePlayer remotePlayer : remotePlayers.values()) {
                remotePlayer.update(delta);
            }

            extractionManager.update(delta);
        }

        // Update kill notifications
        killNotifications.update(delta);

        gameRenderer.setOrbHudState(gameState.getOrbs());
        gameRenderer.setInventoryState(inventoryFeature.getState());
        gameRenderer.setLobbyTimerState(gameState.getLobbyTimerRemainingMs(), gameState.isLobbyFreezePhase());
        gameRenderer.setDebugOverlayVisible(debugCoordsVisible);
        gameRenderer.render(delta, gameState.isInLobby(), player, remotePlayers.asMap(), map, npcManager,
                           gameState.getCorpses(), playerController);

        // Render kill notification popup
        killNotifications.render(gameRenderer.getBatch());

        if (gameState.isInLobby() && extractionPointRenderer != null && extractionUiHud != null && extractionInputController != null) {
            extractionPointRenderer.render(gameRenderer.getBatch());
            extractionUiHud.setNearbyPoint(extractionInputController.getNearbyPoint());
            extractionUiHud.setExtractingPoint(extractionInputController.getExtractingPoint());
            extractionUiHud.setExtractionTimer(extractionInputController.getRemainingTime());
            extractionUiHud.render(gameRenderer.getBatch());
        }

        if (!isPaused && gameState.isInLobby() && playerController != null) {
            playerController.sendInputToServer(gameState.isInLobby());
            playerController.sendPositionUpdate(delta, gameState.isInLobby());
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
            return;
        }
        disposed = true;

        try {
            gameClient.disconnect();
        } catch (Exception e) {
            System.err.println("[GameScreen] Error disconnecting: " + e.getMessage());
        }

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
            killNotifications.dispose();
        } catch (Exception e) {
            System.err.println("[GameScreen] Error disposing resources: " + e.getMessage());
        }
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
                player, inputHandler, gameClient, map,
                gameRenderer.getCameraController().getCamera(), speed, gameRenderer, inventoryFeature.getState()
            );

            this.extractionPointRenderer = new ExtractionPointRenderer(extractionManager);
            this.extractionInputController = new ExtractionInputController(
                extractionManager, gameClient, gameClient.getClientId()
            );
            this.extractionInputController.setExtractionListener(this::handleExtractionCompleted);
            this.extractionUiHud = new ExtractionUiHud(gameClient.getClientId());
        } catch (Exception e) {
            mapLoadError = e;
            e.printStackTrace();
        } finally {
            mapLoadFuture = null;
        }
    }

    // ... Delegate all network callbacks to NetworkEventHandler ...
    @Override public void onPlayerJoined(int clientId, String playerName, float x, float y, int characterId) {
        networkHandler.onPlayerJoined(clientId, playerName, x, y, characterId);
    }

    @Override public void onPlayerLeft(int clientId) {
        networkHandler.onPlayerLeft(clientId);
    }

    @Override public void onPlayerPositionUpdate(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        networkHandler.onPlayerPositionUpdate(clientId, x, y, moving, aimX, aimY);
    }

    @Override public void onLobbyState(Network.LobbyState lobbyState) {
        networkHandler.onLobbyState(lobbyState);
    }

    @Override public void onLobbyJoinConfirmed(float spawnX, float spawnY) {
        Gdx.app.postRunnable(() -> {
            // Reset player state for new match
            player.resetForNewMatch();
            player.x = spawnX;
            player.y = spawnY;
            if (playerController != null) {
                playerController.resetAim();
            }
            playerDead = false;
            killedByName = null;
            gameState.setInLobby(true);
        });
    }

    @Override public void onLobbyTimerState(Network.LobbyTimerState timerState) {
        Gdx.app.postRunnable(() -> {
            if (timerState == null) return;
            gameState.updateLobbyTimer(timerState.remainingMs, timerState.freezePhase);
            if (timerState.freezePhase && extractionInputController != null) {
                extractionInputController.setExtractionLocked(true);
            }
        });
    }

    @Override public void onLobbyClosed(Network.LobbyClosed lobbyClosed) {
        Gdx.app.postRunnable(this::leaveToMainMenuLikeEscLeave);
    }

    @Override public void onNpcStateUpdate(Network.NpcStateUpdate npcStateUpdate) {
        networkHandler.onNpcStateUpdate(npcStateUpdate);
    }

    @Override public void onNpcAttack(Network.NpcAttack npcAttack) {
        networkHandler.onNpcAttack(npcAttack);
    }

    @Override public void onBulletStateUpdate(Network.BulletStateUpdate bulletStateUpdate) {
        updateBulletState(bulletStateUpdate);
    }

    @Override public void onCorpseStateUpdate(Network.CorpseStateUpdate corpseStateUpdate) {
        networkHandler.onCorpseStateUpdate(corpseStateUpdate);
    }

    @Override public void onOrbStateUpdate(Network.OrbStateUpdate orbStateUpdate) {
        networkHandler.onOrbStateUpdate(orbStateUpdate);
    }

    @Override public void onInventoryStateUpdate(Network.InventoryStateUpdate inventoryUpdate) {
        networkHandler.onInventoryStateUpdate(inventoryUpdate);
    }

    @Override public void onPlayerHealthUpdate(Network.PlayerHealthUpdate healthUpdate) {
        networkHandler.onPlayerHealthUpdate(healthUpdate);
    }

    @Override public void onEntityDied(Network.EntityDied entityDied) {
        networkHandler.onEntityDied(entityDied, (killerName) -> {
            gameState.setInLobby(false);
            killedByName = killerName;
            playerDead = true;
            showDeathScreen();
        });
    }

    @Override public void onPlayerShoot(Network.PlayerShoot playerShoot) {
        networkHandler.onPlayerShoot(playerShoot);
    }

    @Override public void onPlayerStep(Network.PlayerStep playerStep) {
        networkHandler.onPlayerStep(playerStep);
    }

    @Override public void onNpcDeathSound(Network.NpcDeathSound npcDeathSound) {
        networkHandler.onNpcDeathSound(npcDeathSound);
    }

    @Override public void onNpcHitSound(Network.NpcHitSound npcHitSound) {
        networkHandler.onNpcHitSound(npcHitSound);
    }

    @Override public void onExtractionPointStateUpdate(Network.ExtractionPointStateUpdate stateUpdate) {
        Gdx.app.postRunnable(() -> {
            if (extractionInputController != null) {
                extractionInputController.applyServerUpdate(
                    stateUpdate.extractionPointId, stateUpdate.state,
                    stateUpdate.activatingClientId, stateUpdate.remainingTime
                );
            }
        });
    }

    @Override public void onShopPurchaseResult(Network.ShopPurchaseResult result) {
        Gdx.app.postRunnable(() -> {
            shopPurchaseInProgress = false;
            if (result == null) {
                shopStatusMessage = "Purchase failed";
                return;
            }

            shopStatusMessage = result.message == null || result.message.trim().isEmpty()
                ? (result.success ? "Purchase successful" : "Purchase failed")
                : result.message;

            if (!result.success) {
                return;
            }

            int itemTypeId = result.itemType == ItemKind.NONE.getId() ? pendingShopItemType : result.itemType;
            int itemAmount = result.amount > 0 ? result.amount : Math.max(1, pendingShopAmount);
            int orbCost = result.orbCost > 0 ? result.orbCost : Math.max(1, pendingShopOrbCost);
            ItemKind purchasedKind = ItemKind.fromId(itemTypeId);
            if (purchasedKind == ItemKind.NONE) {
                return;
            }

            playerLootService.syncShopPurchase(
                ItemKind.ORB.getCode(),
                orbCost,
                purchasedKind.getCode(),
                itemAmount,
                new PlayerLootService.SaveCallback() {
                    @Override
                    public void onSuccess() {
                        // Keep server message as the main UI status.
                    }

                    @Override
                    public void onError(String message) {
                        if (message != null && !message.trim().isEmpty()) {
                            shopStatusMessage = "Purchased in match. Supabase sync: " + message;
                        }
                    }
                }
            );
        });
    }

    public void setPaused(boolean paused) {
        this.isPaused = paused;
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void skipLobbyTimerToFinalMinuteForTesting() {
        gameClient.sendToServer(new Network.LobbyTimerDebugSkip());
    }

    public int getCurrentOrbBalance() {
        return inventoryFeature.getState().getTotalItemsByType(ItemKind.ORB.getId());
    }

    public boolean isShopPurchaseInProgress() {
        return shopPurchaseInProgress;
    }

    public String pollShopStatusMessage() {
        String current = shopStatusMessage;
        shopStatusMessage = null;
        return current;
    }

    public boolean requestShopPurchase(int itemTypeId, int amount, int orbCost) {
        if (shopPurchaseInProgress || amount <= 0 || orbCost <= 0) {
            return false;
        }

        if (getCurrentOrbBalance() < orbCost) {
            shopStatusMessage = "Not enough ORB";
            return false;
        }

        if (!gameClient.isConnected()) {
            shopStatusMessage = "Server connection lost";
            return false;
        }

        shopPurchaseInProgress = true;
        pendingShopItemType = itemTypeId;
        pendingShopAmount = amount;
        pendingShopOrbCost = orbCost;
        shopStatusMessage = "Processing purchase...";
        gameClient.sendShopPurchase(itemTypeId, amount, orbCost);
        return true;
    }

    private void leaveToMainMenuLikeEscLeave() {
        if (leavingGame || disposed) {
            return;
        }
        leavingGame = true;
        isPaused = true;
        sessionExitCoordinator.leaveToMainMenu(this::isDisposed, this::dispose);
    }

    private void handleExtractionCompleted() {
        if (leavingGame || disposed) {
            return;
        }
        // Show extraction result screen
        showExtractionScreen();
        // Save loot in background
        sessionExitCoordinator.saveExtractedLootAndLeave(
            inventoryFeature.getState().getSlots(),
            () -> {} // Extraction screen handles the navigation
        );
    }

    private void updateBulletState(Network.BulletStateUpdate bulletStateUpdate) {
        Gdx.app.postRunnable(() -> {
            if (bulletStateUpdate == null || bulletStateUpdate.bullets == null) {
                return;
            }

            java.util.Set<Integer> seen = new java.util.HashSet<>();
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

    private void showDeathScreen() {
        isPaused = true;
        DeathScreen deathScreen = new DeathScreen(
            game,
            player.getPlayerName(),
            killedByName,
            this::leaveToMainMenuLikeEscLeave
        );
        game.setScreen(deathScreen);
    }

    private void showExtractionScreen() {
        isPaused = true;
        ExtractionResultScreen extractionScreen = new ExtractionResultScreen(
            game,
            player.getPlayerName(),
            player.getKillCount(),
            this::leaveToMainMenuLikeEscLeave
        );
        game.setScreen(extractionScreen);
    }

    // Callback interface for death with killer information
    @FunctionalInterface
    public interface DeathCallback {
        void onPlayerDied(String killerName);
    }
}
