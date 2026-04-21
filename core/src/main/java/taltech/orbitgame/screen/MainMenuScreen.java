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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.player.PlayerProfile;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.audio.AudioManager;
/**
 * Main menu screen with Play, Settings, and Leave buttons
 */
public class MainMenuScreen implements Screen {
    private static final float MUSIC_CONTROL_WIDTH = 390f;
    private static final float MUSIC_CONTROL_WIDTH_WINDOWED = 270f;
    private static final float MUSIC_CONTROL_MARGIN = 16f;
    private static final float MUSIC_CONTROL_MARGIN_WINDOWED = 10f;
    private static final float MUSIC_CONTROL_PRESS_SCALE = 0.96f;
    private static final float MUSIC_SEEK_MARGIN_TOP = 0f;

    private Main game;
    private Stage stage;
    private SpriteBatch batch;
    private Texture spaceBackground;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture characterPreviewTexture;
    private Texture musicControlTexture;
    private Image musicControlImage;
    private Slider musicSeekSlider;
    private Label musicSeekTimeLabel;
    private Texture musicSeekKnobTexture;
    private Texture musicSeekBackgroundTexture;
    private float musicSeekMaxSeconds = 1f;
    private boolean updatingMusicSeekSlider;
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
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        musicControlTexture = new Texture(Gdx.files.internal("Buttons/music_button.png"));
        musicControlImage = new Image(new TextureRegionDrawable(musicControlTexture));
        float musicControlWidth = MUSIC_CONTROL_WIDTH;
        float musicControlHeight = musicControlWidth * (musicControlTexture.getHeight() / (float) musicControlTexture.getWidth());
        musicControlImage.setSize(musicControlWidth, musicControlHeight);
        musicControlImage.setOrigin(musicControlWidth * 0.5f, musicControlHeight * 0.5f);
        musicControlImage.setPosition(
            Gdx.graphics.getWidth() - musicControlWidth - MUSIC_CONTROL_MARGIN,
            Gdx.graphics.getHeight() - musicControlHeight - MUSIC_CONTROL_MARGIN
        );
        musicControlImage.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                musicControlImage.setScale(MUSIC_CONTROL_PRESS_SCALE);
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                float relativeX = x / event.getListenerActor().getWidth();
                if (relativeX < 0.33f) {
                    AudioManager.getInstance().playPreviousLobbyMusicTrack();
                } else if (relativeX > 0.66f) {
                    AudioManager.getInstance().playNextLobbyMusicTrack();
                } else {
                    AudioManager.getInstance().togglePauseResumeCurrentMusic();
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                musicControlImage.setScale(1f);
                super.touchUp(event, x, y, pointer, button);
            }
        });
        stage.addActor(musicControlImage);

        musicSeekMaxSeconds = Math.max(1f, AudioManager.getInstance().getCurrentTrackDurationSeconds());
        musicSeekSlider = new Slider(0f, musicSeekMaxSeconds, 0.1f, false, createMusicSeekSliderStyle());
        musicSeekSlider.setValue(Math.min(
            AudioManager.getInstance().getCurrentTrackPositionSeconds(),
            musicSeekMaxSeconds
        ));
        musicSeekSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (updatingMusicSeekSlider) {
                    return;
                }
                AudioManager.getInstance().seekCurrentTrackSeconds(musicSeekSlider.getValue());
            }
        });
        stage.addActor(musicSeekSlider);
        musicSeekTimeLabel = new Label("00:00", new Label.LabelStyle(FontManager.getPixelFont(12), Color.WHITE));
        musicSeekTimeLabel.setAlignment(Align.center);
        stage.addActor(musicSeekTimeLabel);
        layoutMusicControls(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        updateMusicControlsVisibility();

        Table leftTable = new Table();
        Table rightTable = new Table();
        mainTable.add(leftTable).padRight(60).left();
        mainTable.add(rightTable).padLeft(60).right();

        // Add "Orbit" title text above buttons
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = pixelFont;
        titleStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label("Orbit", titleStyle);
        titleLabel.setFontScale(2.0f); // Make title larger
        rightTable.add(titleLabel).padBottom(50).row();

        CharacterType selectedCharacter = CharacterType.fromId(PlayerProfile.getSelectedCharacterId());
        characterPreviewTexture = new Texture(Gdx.files.internal(selectedCharacter.getPreviewTexturePath()));
        characterPreviewTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        Image characterImage = new Image(new TextureRegionDrawable(characterPreviewTexture));
        leftTable.add(characterImage).size(260, 260).padBottom(25).row();

        TextButton charactersButton = createButton("CHARACTERS");
        charactersButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new CharacterSelectScreen(game, lobbyInfoClient));
            }
        });
        leftTable.add(charactersButton).padBottom(20).size(charactersButton.getWidth(), charactersButton.getHeight()).row();

        // Create Play button
        TextButton playButton = createButton("PLAY");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LobbyChoiceScreen(game, lobbyInfoClient));
            }
        });
        rightTable.add(playButton).padBottom(16).size(playButton.getWidth(), playButton.getHeight()).row();

        TextButton inventoryButton = createButton("INVENTORY");
        inventoryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new InventoryMenuScreen(game, lobbyInfoClient));
            }
        });
        rightTable.add(inventoryButton).padBottom(16).size(inventoryButton.getWidth(), inventoryButton.getHeight()).row();

        TextButton shopButton = createButton("SHOP");
        shopButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new ShopMenuScreen(game, lobbyInfoClient));
            }
        });
        rightTable.add(shopButton).padBottom(16).size(shopButton.getWidth(), shopButton.getHeight()).row();

        // Create Settings button
        TextButton settingsButton = createButton("SETTINGS");
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new SettingsMenuScreen(game, lobbyInfoClient));
            }
        });
        rightTable.add(settingsButton).padBottom(16).size(settingsButton.getWidth(), settingsButton.getHeight()).row();

        // Create Leave button
        TextButton leaveButton = createButton("LEAVE");
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        rightTable.add(leaveButton).padBottom(16).size(leaveButton.getWidth(), leaveButton.getHeight()).row();
    }

    private TextButton createButton(String text) {
        // Create button style with background images
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = pixelFont;
        buttonStyle.fontColor = Color.WHITE;

        TextButton button = new TextButton(text, buttonStyle);

        // Keep all buttons compact so extra actions (inventory) fit without clutter.
        float buttonWidth = buttonTexture.getWidth() * 0.5f;
        float buttonHeight = buttonTexture.getHeight() * 0.5f;
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
        updateMusicControlsVisibility();
        refreshMusicSeekSlider();
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        layoutMusicControls(width, height);
        updateMusicControlsVisibility();
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
        if (characterPreviewTexture != null) {
            characterPreviewTexture.dispose();
        }
        if (musicControlTexture != null) {
            musicControlTexture.dispose();
        }
        if (musicSeekKnobTexture != null) {
            musicSeekKnobTexture.dispose();
            musicSeekKnobTexture = null;
        }
        if (musicSeekBackgroundTexture != null) {
            musicSeekBackgroundTexture.dispose();
            musicSeekBackgroundTexture = null;
        }
        // Font is managed by FontManager, don't dispose here
    }

    private void layoutMusicControls(int width, int height) {
        if (musicControlImage == null || musicControlTexture == null) {
            return;
        }
        float musicControlWidth = resolveMusicControlWidth();
        float musicControlHeight = musicControlWidth * (musicControlTexture.getHeight() / (float) musicControlTexture.getWidth());
        float margin = resolveMusicControlMargin();
        float x = width - musicControlWidth - margin;
        float y = height - musicControlHeight - margin;
        musicControlImage.setSize(musicControlWidth, musicControlHeight);
        musicControlImage.setOrigin(musicControlWidth * 0.5f, musicControlHeight * 0.5f);
        musicControlImage.setPosition(x, y);

        if (musicSeekSlider != null) {
            musicSeekSlider.setWidth(musicControlWidth);
            float sliderY = y - musicSeekSlider.getHeight() - MUSIC_SEEK_MARGIN_TOP;
            musicSeekSlider.setPosition(x, sliderY);
            if (musicSeekTimeLabel != null) {
                musicSeekTimeLabel.setSize(musicControlWidth, 16f);
                musicSeekTimeLabel.setPosition(x, sliderY + musicSeekSlider.getHeight() + 2f);
            }
        }
    }

    private void refreshMusicSeekSlider() {
        if (musicSeekSlider == null || musicSeekSlider.isDragging()) {
            return;
        }

        float dynamicMax = Math.max(1f, AudioManager.getInstance().getCurrentTrackDurationSeconds());
        if (Math.abs(dynamicMax - musicSeekMaxSeconds) > 0.2f) {
            musicSeekMaxSeconds = dynamicMax;
            updatingMusicSeekSlider = true;
            musicSeekSlider.setRange(0f, musicSeekMaxSeconds);
            updatingMusicSeekSlider = false;
        }

        float position = Math.min(AudioManager.getInstance().getCurrentTrackPositionSeconds(), musicSeekMaxSeconds);
        updatingMusicSeekSlider = true;
        musicSeekSlider.setValue(position);
        updatingMusicSeekSlider = false;
        if (musicSeekTimeLabel != null) {
            musicSeekTimeLabel.setText(formatTrackTime(position));
        }
    }

    private Slider.SliderStyle createMusicSeekSliderStyle() {
        Slider.SliderStyle style = new Slider.SliderStyle();

        Pixmap knobPixmap = new Pixmap(12, 22, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(1f, 1f, 1f, 1f);
        knobPixmap.fill();
        Texture knobTexture = new Texture(knobPixmap);
        knobPixmap.dispose();

        Pixmap bgPixmap = new Pixmap((int) MUSIC_CONTROL_WIDTH, 6, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.25f, 0.25f, 0.25f, 1f);
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        this.musicSeekKnobTexture = knobTexture;
        this.musicSeekBackgroundTexture = bgTexture;
        style.knob = new TextureRegionDrawable(knobTexture);
        style.background = new TextureRegionDrawable(bgTexture);
        return style;
    }

    private String formatTrackTime(float seconds) {
        int totalSeconds = Math.max(0, Math.round(seconds));
        int minutes = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, sec);
    }

    private void updateMusicControlsVisibility() {
        boolean visible = true;
        if (musicControlImage != null) {
            musicControlImage.setVisible(visible);
        }
        if (musicSeekSlider != null) {
            musicSeekSlider.setVisible(visible);
        }
        if (musicSeekTimeLabel != null) {
            musicSeekTimeLabel.setVisible(visible);
        }
    }

    private float resolveMusicControlWidth() {
        return Gdx.graphics.isFullscreen() ? MUSIC_CONTROL_WIDTH : MUSIC_CONTROL_WIDTH_WINDOWED;
    }

    private float resolveMusicControlMargin() {
        return Gdx.graphics.isFullscreen() ? MUSIC_CONTROL_MARGIN : MUSIC_CONTROL_MARGIN_WINDOWED;
    }
}
