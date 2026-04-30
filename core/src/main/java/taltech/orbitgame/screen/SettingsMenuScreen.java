package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.util.FontManager;

/**
 * Settings menu screen with volume sliders
 */
public class SettingsMenuScreen implements Screen {
    private Main game;
    private LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private BitmapFont pixelFont;
    private Texture backgroundTexture;

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
        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        buttonPressedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        backgroundTexture = new Texture(Gdx.files.internal("MainMenuAssets/set.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Load pixel font from assets
        pixelFont = FontManager.getMenuFont();

        // Create UI layout
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = pixelFont;
        titleStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label("SETTINGS", titleStyle);
        titleLabel.setFontScale(1.5f);
        table.add(titleLabel).padBottom(60).row();

        // Create slider styles
        SliderStyle sliderStyle = createSliderStyle();

        // Music Volume Label and Slider
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = pixelFont;
        labelStyle.fontColor = Color.WHITE;

        Label musicLabel = new Label("Music Volume", labelStyle);
        Slider musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        float currentMusicVolume = AudioManager.getInstance().getMusicVolume();
        musicSlider.setValue(currentMusicVolume);

        // Music Volume Value Label - Initialize with current value
        Label musicValueLabel = new Label((int)(currentMusicVolume * 100) + "%", labelStyle);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                AudioManager.getInstance().setMusicVolume(musicSlider.getValue());
                musicValueLabel.setText((int)(musicSlider.getValue() * 100) + "%");
            }
        });

        // SFX Volume Label and Slider
        Label sfxLabel = new Label("SFX Volume", labelStyle);
        Slider sfxSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        float currentSfxVolume = AudioManager.getInstance().getSfxVolume();

        // SFX Volume Value Label - Initialize with current value
        Label sfxValueLabel = new Label((int)(currentSfxVolume * 100) + "%", labelStyle);

        sfxSlider.setValue(currentSfxVolume);
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                AudioManager.getInstance().setSfxVolume(sfxSlider.getValue());
                sfxValueLabel.setText((int)(sfxSlider.getValue() * 100) + "%");
            }
        });

        // Add Music Volume section
        table.add(musicLabel).left().padBottom(10).row();
        Table musicTable = new Table();
        musicTable.add(musicSlider).width(300).height(24).padRight(20);
        musicTable.add(musicValueLabel).width(50);
        table.add(musicTable).padBottom(30).row();

        // Add SFX Volume section
        table.add(sfxLabel).left().padBottom(10).row();
        Table sfxTable = new Table();
        sfxTable.add(sfxSlider).width(300).height(24).padRight(20);
        sfxTable.add(sfxValueLabel).width(50);
        table.add(sfxTable).padBottom(60).row();

        // Create Back button
        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Pass lobbyInfoClient to MainMenuScreen to keep connection alive
                game.setScreen(new MainMenuScreen(game, lobbyInfoClient));
            }
        });
        table.add(backButton).padBottom(20).row();
    }

    private SliderStyle createSliderStyle() {
        SliderStyle sliderStyle = new SliderStyle();

        // Create a small white rectangle for the knob
        Pixmap knobPixmap = new Pixmap(12, 24, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(1, 1, 1, 1); // White
        knobPixmap.fill();
        Texture knobTexture = new Texture(knobPixmap);
        knobPixmap.dispose();

        // Create a gray rectangle for the background
        Pixmap bgPixmap = new Pixmap(300, 8, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.3f, 0.3f, 0.3f, 1); // Dark gray
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        sliderStyle.background = new TextureRegionDrawable(bgTexture);
        sliderStyle.knob = new TextureRegionDrawable(knobTexture);

        return sliderStyle;
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

        // Draw background
        batch.begin();
        batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
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
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        backgroundTexture.dispose();
        // Font is managed by FontManager, don't dispose here
    }
}




