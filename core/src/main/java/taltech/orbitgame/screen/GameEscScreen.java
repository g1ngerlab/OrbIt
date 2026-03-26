package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.util.FontManager;

public class GameEscScreen implements Screen {

    private final Main game;
    private final GameScreen gameScreen;
    private final Stage stage;
    private final Texture overlay;
    private final TextButton.TextButtonStyle buttonStyle;

    public GameEscScreen(Main game, GameScreen gameScreen) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.stage = new Stage(new ScreenViewport());

        // Create a semi-transparent black overlay
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0, 0, 0, 0.5f);
        pixmap.fill();
        this.overlay = new Texture(pixmap);
        pixmap.dispose();

        // Setup button style
        Texture buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        Texture buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        BitmapFont font = FontManager.getMenuFont();

        this.buttonStyle = new TextButton.TextButtonStyle();
        this.buttonStyle.font = font;
        this.buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        this.buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);

        setupUI();
    }

    private void setupUI() {
        Table table = new Table();
        table.setFillParent(true);
        table.center();

        TextButton resumeButton = new TextButton("Resume", buttonStyle);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                resumeGame();
            }
        });

        TextButton leaveButton = new TextButton("Leave", buttonStyle);
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                leaveGame();
            }
        });

        table.add(resumeButton).padBottom(20).row();
        table.add(leaveButton).row();

        stage.addActor(table);
    }

    private void resumeGame() {
        gameScreen.setPaused(false);
        game.setScreen(gameScreen);
    }

    private void leaveGame() {
        // Set paused first to stop rendering
        gameScreen.setPaused(true);

        // Use Gdx.app.postRunnable to ensure we're on the right thread
        Gdx.app.postRunnable(() -> {
            gameScreen.dispose(); // Disconnects and cleans up
            // Switch back to lobby music (explicitly stop game music first)
            AudioManager.getInstance().stopCurrentMusic();
            AudioManager.getInstance().playRandomLobbyMusic();
            // Create a fresh LobbyInfoClient so lobby browsing works after returning
            game.setScreen(new MainMenuScreen(game, new LobbyInfoClient()));
        });
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        gameScreen.setPaused(true);
    }

    @Override
    public void render(float delta) {
        // Render game in background (only if not disposed)
        if (gameScreen != null && !gameScreen.isDisposed()) {
            gameScreen.render(delta);
        }

        // Draw overlay
        stage.getBatch().begin();
        stage.getBatch().draw(overlay, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();

        // Draw UI
        stage.act(delta);
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            resumeGame();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (gameScreen != null) {
            gameScreen.resize(width, height);
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
        overlay.dispose();
        // Do not dispose gameScreen here as we might resume it
    }
}
