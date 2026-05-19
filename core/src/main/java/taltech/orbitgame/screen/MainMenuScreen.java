package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.screen.ui.MusicPlayerUI;
import taltech.orbitgame.screen.ui.MainMenuAssetManager;
import taltech.orbitgame.screen.ui.MainMenuUIBuilder;

/**
 * Main menu screen with Play, Settings, and Leave buttons.
 * Provides access to lobbies, inventory, shop, and settings.
 *
 * Delegates UI building to MainMenuUIBuilder and asset management to MainMenuAssetManager.
 */
public class MainMenuScreen implements Screen {
    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private MusicPlayerUI musicPlayerUI;
    private MainMenuAssetManager assetManager;
    private MainMenuUIBuilder uiBuilder;

    public MainMenuScreen(Main game) {
        this(game, null);
    }

    public MainMenuScreen(Main game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Start lobby music
        AudioManager.getInstance().playRandomLobbyMusic();

        // Load assets
        assetManager = new MainMenuAssetManager();
        assetManager.load();

        // Create UI layout
        uiBuilder = new MainMenuUIBuilder(
            stage,
            assetManager.getButtonTexture(),
            assetManager.getButtonPressedTexture(),
            assetManager.getPixelFont()
        );

        // Initialize music player UI
        musicPlayerUI = new MusicPlayerUI(stage);
        musicPlayerUI.initialize();

        // Build the menu layout
        uiBuilder.buildLayout(
            () -> game.setScreen(new CharacterSelectScreen(game, lobbyInfoClient)),
            () -> game.setScreen(new LobbyChoiceScreen(game, lobbyInfoClient)),
            () -> game.setScreen(new InventoryMenuScreen(game, lobbyInfoClient)),
            () -> game.setScreen(new ShopMenuScreen(game, lobbyInfoClient)),
            () -> game.setScreen(new SettingsMenuScreen(game, lobbyInfoClient)),
            () -> Gdx.app.exit()
        );
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, assetManager.getSpaceBackground());
        batch.end();

        stage.act(delta);
        if (musicPlayerUI != null) {
            musicPlayerUI.update(delta);
            musicPlayerUI.updateVisibility();
        }
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        if (musicPlayerUI != null) {
            musicPlayerUI.layoutControls(width, height);
            musicPlayerUI.updateVisibility();
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        if (assetManager != null) {
            assetManager.dispose();
        }
        if (musicPlayerUI != null) {
            musicPlayerUI.dispose();
        }
    }
}
