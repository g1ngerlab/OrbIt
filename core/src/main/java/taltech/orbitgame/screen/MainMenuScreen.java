package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.audio.AudioManager;
/**
 * Main menu screen with Play, Settings, and Leave buttons
 */
public class MainMenuScreen implements Screen {
    private Main game;
    private Stage stage;
    private SpriteBatch batch;
    private Texture spaceBackground;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private BitmapFont pixelFont;
    private LobbyInfoClient lobbyInfoClient; // Shared connection from ConnectionScreen

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
        spaceBackground = new Texture(Gdx.files.internal("LobbyBg.png"));

        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        buttonPressedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Load pixel font from assets
        pixelFont = FontManager.getMenuFont();

        // Create UI layout
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Add "Orbit" title text above buttons
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = pixelFont;
        titleStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label("Orbit", titleStyle);
        titleLabel.setFontScale(2.0f); // Make title larger
        table.add(titleLabel).padBottom(50).row();

        // Create Play button
        TextButton playButton = createButton("PLAY");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LobbyChoiceScreen(game, lobbyInfoClient));
            }
        });
        table.add(playButton).padBottom(20).size(playButton.getWidth(), playButton.getHeight()).row();

        // Create Settings button
        TextButton settingsButton = createButton("SETTINGS");
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SettingsMenuScreen(game));
            }
        });
        table.add(settingsButton).padBottom(20).size(settingsButton.getWidth(), settingsButton.getHeight()).row();

        // Create Leave button
        TextButton leaveButton = createButton("LEAVE");
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        table.add(leaveButton).padBottom(20).size(leaveButton.getWidth(), leaveButton.getHeight()).row();
    }

    private TextButton createButton(String text) {
        // Create button style with background images
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = pixelFont;
        buttonStyle.fontColor = Color.WHITE;

        TextButton button = new TextButton(text, buttonStyle);

        // Make buttons smaller - scale to 60% of original size
        float buttonWidth = buttonTexture.getWidth() * 0.6f;
        float buttonHeight = buttonTexture.getHeight() * 0.6f;
        button.setSize(buttonWidth, buttonHeight);

        // Center text in the button by ensuring the label cell fills the button area
        button.getLabel().setAlignment(Align.center);
        button.getLabelCell().size(buttonWidth, buttonHeight);

        return button;
    }

    @Override
    public void render(float delta) {
        // Clear screen with black background
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw space background as fullscreen
        batch.begin();
        BackgroundRenderer.drawCover(batch, spaceBackground);
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
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
        spaceBackground.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        // Font is managed by FontManager, don't dispose here
    }
}
