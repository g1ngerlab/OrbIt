package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.loot.PlayerLootService;

import java.util.function.BooleanSupplier;

/**
 * Handles game-session exit flow so gameplay code stays focused on runtime logic.
 */
public class GameSessionExitCoordinator {
    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private final PlayerLootService playerLootService;

    public GameSessionExitCoordinator(Main game, LobbyInfoClient lobbyInfoClient, PlayerLootService playerLootService) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
        this.playerLootService = playerLootService;
    }

    public void leaveToMainMenu(BooleanSupplier isDisposed, Runnable disposeAction) {
        Gdx.app.postRunnable(() -> {
            if (!isDisposed.getAsBoolean()) {
                disposeAction.run();
            }
            AudioManager.getInstance().stopCurrentMusic();
            AudioManager.getInstance().playRandomLobbyMusic();
            LobbyInfoClient clientToUse = (lobbyInfoClient != null) ? lobbyInfoClient : new LobbyInfoClient();
            game.setScreen(new MainMenuScreen(game, clientToUse));
        });
    }

    public void saveExtractedLootAndLeave(ItemStack[] slots, Runnable leaveAction) {
        playerLootService.saveExtractedLoot(slots, new PlayerLootService.SaveCallback() {
            @Override
            public void onSuccess() {
                leaveAction.run();
            }

            @Override
            public void onError(String message) {
                System.err.println("[GameSessionExitCoordinator] Failed to save extracted loot: " + message);
                leaveAction.run();
            }
        });
    }
}

