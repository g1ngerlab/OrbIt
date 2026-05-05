package taltech.orbitgame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.screen.ui.MusicPlayerUI;
import taltech.orbitgame.screen.ui.LobbyChoiceAssetManager;
import taltech.orbitgame.screen.ui.LobbyChoiceUIBuilder;

/**
 * Screen for choosing between creating a new lobby or joining an existing one.
 * Delegates UI building to LobbyChoiceUIBuilder and asset management to LobbyChoiceAssetManager.
 */
public class LobbyChoiceScreen implements Screen {
    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private MusicPlayerUI musicPlayerUI;
    private LobbyChoiceAssetManager assetManager;
    private LobbyChoiceUIBuilder uiBuilder;

    public LobbyChoiceScreen(Game game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        // Start lobby music
        AudioManager.getInstance().playRandomLobbyMusic();

        // Load assets
        assetManager = new LobbyChoiceAssetManager();
        assetManager.load();

        // Initialize music player UI
        musicPlayerUI = new MusicPlayerUI(stage);
        musicPlayerUI.initialize();
        musicPlayerUI.layoutControls(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        musicPlayerUI.updateVisibility();

        // Create UI builder
        uiBuilder = new LobbyChoiceUIBuilder(
            stage,
            assetManager.getButtonTexture(),
            assetManager.getButtonPressedTexture(),
            assetManager.getPixelFont(),
            assetManager.getPixelFontLarge()
        );

        // Build the menu layout
        uiBuilder.buildLayout(
            () -> game.setScreen(new CreateLobbyScreen(game, lobbyInfoClient)),
            () -> game.setScreen(new JoinLobbyScreen(game, lobbyInfoClient)),
            () -> game.setScreen(new MainMenuScreen((Main) game, lobbyInfoClient))
        );
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, assetManager.getBackgroundTexture());
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
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

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
