package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.screen.ui.SettingsMenuAssetManager;
import taltech.orbitgame.screen.ui.SettingsMenuUIBuilder;

/**
 * Settings menu screen with volume sliders.
 * Delegates UI building to SettingsMenuUIBuilder and asset management to SettingsMenuAssetManager.
 */
public class SettingsMenuScreen implements Screen {
    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private SettingsMenuAssetManager assetManager;
    private SettingsMenuUIBuilder uiBuilder;

    public SettingsMenuScreen(Main game) {
        this(game, null);
    }

    public SettingsMenuScreen(Main game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Load assets
        assetManager = new SettingsMenuAssetManager();
        assetManager.load();

        // Create UI builder
        uiBuilder = new SettingsMenuUIBuilder(
            stage,
            assetManager.getButtonTexture(),
            assetManager.getButtonPressedTexture(),
            assetManager.getPixelFont()
        );

        // Build the menu layout
        uiBuilder.buildLayout(
            () -> game.setScreen(new MainMenuScreen(game, lobbyInfoClient))
        );
    }


    @Override
    public void render(float delta) {
        // Clear screen with black background
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw background
        batch.begin();
        batch.draw(assetManager.getBackgroundTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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
    }
}




