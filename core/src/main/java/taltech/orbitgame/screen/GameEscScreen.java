package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.util.FontManager;

public class GameEscScreen implements Screen {
    private static final float ESC_BUTTON_WIDTH = 260f;
    private static final float ESC_BUTTON_HEIGHT = 60f;
    private static final float ESC_BUTTON_FONT_SCALE = 0.78f;
    private static final float OPTIONS_BACK_BUTTON_SCALE = 0.55f;
    private static final float MUSIC_CONTROL_WIDTH = 450f;
    private static final float MUSIC_CONTROL_PRESS_SCALE = 0.96f;

    private final Main game;
    private final GameScreen gameScreen;
    private final LobbyInfoClient lobbyInfoClient;
    private final Stage stage;
    private final Texture overlay;
    private final TextButton.TextButtonStyle buttonStyle;
    private final Texture musicControlTexture;
    private Slider trackSeekSlider;
    private Label trackSeekValueLabel;
    private boolean updatingTrackSeekSlider;
    private boolean optionsVisible;
    private float trackSeekMaxSeconds = 1f;

    public GameEscScreen(Main game, GameScreen gameScreen) {
        this(game, gameScreen, null);
    }

    public GameEscScreen(Main game, GameScreen gameScreen, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.lobbyInfoClient = lobbyInfoClient;
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
        this.musicControlTexture = new Texture(Gdx.files.internal("Buttons/music_button.png"));

        setupUI();
    }

    private void setupUI() {
        optionsVisible = false;
        trackSeekSlider = null;
        trackSeekValueLabel = null;

        Table table = new Table();
        table.setFillParent(true);
        table.center();

        TextButton resumeButton = new TextButton("Resume", buttonStyle);
        resumeButton.getLabel().setFontScale(ESC_BUTTON_FONT_SCALE);
        resumeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                resumeGame();
            }
        });

        TextButton optionsButton = new TextButton("Options", buttonStyle);
        optionsButton.getLabel().setFontScale(ESC_BUTTON_FONT_SCALE);
        optionsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showOptions(table);
            }
        });

        TextButton shopButton = new TextButton("Shop", buttonStyle);
        shopButton.getLabel().setFontScale(ESC_BUTTON_FONT_SCALE);
        shopButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                openShop();
            }
        });

        TextButton skipTimerButton = new TextButton("Skip to Freeze Test", buttonStyle);
        skipTimerButton.getLabel().setFontScale(ESC_BUTTON_FONT_SCALE);
        skipTimerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameScreen.skipLobbyTimerToFinalMinuteForTesting();
            }
        });

        TextButton leaveButton = new TextButton("Leave", buttonStyle);
        leaveButton.getLabel().setFontScale(ESC_BUTTON_FONT_SCALE);
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                leaveGame();
            }
        });

        table.add(resumeButton).size(ESC_BUTTON_WIDTH, ESC_BUTTON_HEIGHT).padBottom(14).row();
        table.add(optionsButton).size(ESC_BUTTON_WIDTH, ESC_BUTTON_HEIGHT).padBottom(14).row();
        table.add(shopButton).size(ESC_BUTTON_WIDTH, ESC_BUTTON_HEIGHT).padBottom(14).row();
        table.add(skipTimerButton).size(ESC_BUTTON_WIDTH, ESC_BUTTON_HEIGHT).padBottom(14).row();
        table.add(leaveButton).size(ESC_BUTTON_WIDTH, ESC_BUTTON_HEIGHT).row();

        stage.addActor(table);
    }

    private void showOptions(Table parentTable) {
        // Clear and create options menu
        parentTable.clearChildren();
        optionsVisible = true;

        BitmapFont font = FontManager.getMenuFont();
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, null);
        labelStyle.fontColor = new com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f);

        // Create slider styles with proper rendering
        SliderStyle sliderStyle = createSliderStyle();

        // Title
        Label optionsTitle = new Label("OPTIONS", labelStyle);
        optionsTitle.setFontScale(1.2f);
        parentTable.add(optionsTitle).padBottom(50).row();

        // Music volume slider
        Label musicLabel = new Label("Music Volume", labelStyle);
        Slider musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        musicSlider.setValue(AudioManager.getInstance().getMusicVolume());

        Label musicValueLabel = new Label("", labelStyle);
        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                AudioManager.getInstance().setMusicVolume(musicSlider.getValue());
                musicValueLabel.setText((int)(musicSlider.getValue() * 100) + "%");
            }
        });
        // Initialize value label AFTER listener is added
        musicValueLabel.setText((int)(musicSlider.getValue() * 100) + "%");

        // SFX volume slider
        Label sfxLabel = new Label("SFX Volume", labelStyle);
        Slider sfxSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        sfxSlider.setValue(AudioManager.getInstance().getSfxVolume());

        Label sfxValueLabel = new Label("", labelStyle);
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                AudioManager.getInstance().setSfxVolume(sfxSlider.getValue());
                sfxValueLabel.setText((int)(sfxSlider.getValue() * 100) + "%");
            }
        });
        // Initialize value label AFTER listener is added
        sfxValueLabel.setText((int)(sfxSlider.getValue() * 100) + "%");

        Label playbackLabel = new Label("Music Playback", labelStyle);
        Image playbackControl = new Image(new TextureRegionDrawable(musicControlTexture));
        float controlWidth = MUSIC_CONTROL_WIDTH;
        float controlHeight = controlWidth * (musicControlTexture.getHeight() / (float) musicControlTexture.getWidth());
        playbackControl.setSize(controlWidth, controlHeight);
        playbackControl.setOrigin(controlWidth * 0.5f, controlHeight * 0.5f);
        playbackControl.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                playbackControl.setScale(MUSIC_CONTROL_PRESS_SCALE);
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                float relativeX = x / event.getListenerActor().getWidth();
                if (relativeX < 0.33f) {
                    AudioManager.getInstance().playPreviousTrack();
                } else if (relativeX > 0.66f) {
                    AudioManager.getInstance().playNextTrack();
                } else {
                    AudioManager.getInstance().togglePauseResumeCurrentMusic();
                }
                refreshTrackSeekValue();
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                playbackControl.setScale(1f);
                super.touchUp(event, x, y, pointer, button);
            }
        });

        trackSeekMaxSeconds = resolveTrackSeekMaxSeconds();
        Slider seekSlider = new Slider(0f, trackSeekMaxSeconds, 0.1f, false, sliderStyle);
        seekSlider.setValue(clampTrackSeconds(
            AudioManager.getInstance().getCurrentTrackPositionSeconds(),
            trackSeekMaxSeconds
        ));
        Label seekValueLabel = new Label("", labelStyle);
        seekValueLabel.setAlignment(Align.center);
        seekSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (updatingTrackSeekSlider) {
                    return;
                }
                float target = clampTrackSeconds(seekSlider.getValue(), trackSeekMaxSeconds);
                AudioManager.getInstance().seekCurrentTrackSeconds(target);
                seekValueLabel.setText(formatTrackTime(target));
            }
        });
        seekValueLabel.setText(formatTrackTime(seekSlider.getValue()));
        this.trackSeekSlider = seekSlider;
        this.trackSeekValueLabel = seekValueLabel;

        // Back button
        TextButton backButton = new TextButton("Back", buttonStyle);
        backButton.getLabel().setFontScale(ESC_BUTTON_FONT_SCALE);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                parentTable.clearChildren();
                setupUI();
            }
        });

        // Add Music Volume row - label above slider
        Table musicLabelRow = new Table();
        musicLabelRow.add(musicLabel).left();
        musicLabelRow.add(musicValueLabel).right().padLeft(100);
        parentTable.add(musicLabelRow).width(400).padBottom(5).row();

        Table musicSliderRow = new Table();
        musicSliderRow.add(musicSlider).width(250);
        parentTable.add(musicSliderRow).padBottom(16).row();

        // Add SFX Volume row - label above slider
        Table sfxLabelRow = new Table();
        sfxLabelRow.add(sfxLabel).left();
        sfxLabelRow.add(sfxValueLabel).right().padLeft(130);
        parentTable.add(sfxLabelRow).width(400).padBottom(5).row();

        Table sfxSliderRow = new Table();
        sfxSliderRow.add(sfxSlider).width(250);
        parentTable.add(sfxSliderRow).padBottom(12).row();

        parentTable.add(playbackLabel).padBottom(3).row();
        parentTable.add(playbackControl).size(controlWidth, controlHeight).padBottom(5).row();

        parentTable.add(seekValueLabel).width(300).padBottom(1).row();

        Table seekSliderRow = new Table();
        seekSliderRow.add(seekSlider).width(300);
        parentTable.add(seekSliderRow).padBottom(12).row();

        // Add back button
        parentTable.add(backButton)
            .size(ESC_BUTTON_WIDTH * OPTIONS_BACK_BUTTON_SCALE, ESC_BUTTON_HEIGHT * OPTIONS_BACK_BUTTON_SCALE)
            .row();
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
        Pixmap bgPixmap = new Pixmap(250, 8, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.3f, 0.3f, 0.3f, 1); // Dark gray
        bgPixmap.fill();
        Texture bgTexture = new Texture(bgPixmap);
        bgPixmap.dispose();

        sliderStyle.background = new TextureRegionDrawable(bgTexture);
        sliderStyle.knob = new TextureRegionDrawable(knobTexture);

        return sliderStyle;
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
            // Use existing LobbyInfoClient if available, otherwise create new one
            LobbyInfoClient clientToUse = (lobbyInfoClient != null) ? lobbyInfoClient : new LobbyInfoClient();
            game.setScreen(new MainMenuScreen(game, clientToUse));
        });
    }

    private void openShop() {
        game.setScreen(new InGameShopScreen(game, gameScreen, lobbyInfoClient));
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
        refreshTrackSeekValue();
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
        musicControlTexture.dispose();
        // Do not dispose gameScreen here as we might resume it
    }

    private void refreshTrackSeekValue() {
        if (!optionsVisible || trackSeekSlider == null || trackSeekValueLabel == null) {
            return;
        }
        if (trackSeekSlider.isDragging()) {
            return;
        }

        float dynamicMax = resolveTrackSeekMaxSeconds();
        if (Math.abs(dynamicMax - trackSeekMaxSeconds) > 0.2f) {
            trackSeekMaxSeconds = dynamicMax;
            updatingTrackSeekSlider = true;
            trackSeekSlider.setRange(0f, trackSeekMaxSeconds);
            updatingTrackSeekSlider = false;
        }

        float position = clampTrackSeconds(
            AudioManager.getInstance().getCurrentTrackPositionSeconds(),
            trackSeekMaxSeconds
        );
        updatingTrackSeekSlider = true;
        trackSeekSlider.setValue(position);
        trackSeekValueLabel.setText(formatTrackTime(position));
        updatingTrackSeekSlider = false;
    }

    private float resolveTrackSeekMaxSeconds() {
        return Math.max(1f, AudioManager.getInstance().getCurrentTrackDurationSeconds());
    }

    private float clampTrackSeconds(float seconds, float max) {
        return Math.max(0f, Math.min(max, seconds));
    }

    private String formatTrackTime(float seconds) {
        int totalSeconds = Math.max(0, Math.round(seconds));
        int minutes = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, sec);
    }
}
