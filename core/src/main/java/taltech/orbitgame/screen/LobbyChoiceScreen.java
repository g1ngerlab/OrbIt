package taltech.orbitgame.screen;

import com.badlogic.gdx.Game;
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
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
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
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

/**
 * Screen for choosing between creating a new lobby or joining an existing one
 */
public class LobbyChoiceScreen implements Screen {
    private static final float MUSIC_CONTROL_WIDTH = 390f;
    private static final float MUSIC_CONTROL_WIDTH_WINDOWED = 270f;
    private static final float MUSIC_CONTROL_MARGIN = 16f;
    private static final float MUSIC_CONTROL_MARGIN_WINDOWED = 10f;
    private static final float MUSIC_CONTROL_PRESS_SCALE = 0.96f;
    private static final float MUSIC_SEEK_MARGIN_TOP = 0f;

    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture musicControlTexture;
    private Image musicControlImage;
    private Slider musicSeekSlider;
    private Label musicSeekTimeLabel;
    private Texture musicSeekKnobTexture;
    private Texture musicSeekBackgroundTexture;
    private float musicSeekMaxSeconds = 1f;
    private boolean updatingMusicSeekSlider;

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

        background = new Texture(Gdx.files.internal("LobbyBg.png"));

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

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Play", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        table.add(titleLabel).padTop(30).padBottom(50).row();

        // Create Lobby button
        TextButton createButton = createButton("Create Lobby");
        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new CreateLobbyScreen(game, lobbyInfoClient));
            }
        });
        table.add(createButton).padBottom(20).size(500, 150).row();

        // Join Lobby button
        TextButton joinButton = createButton("Join Lobby");
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new JoinLobbyScreen(game, lobbyInfoClient));
            }
        });
        table.add(joinButton).padBottom(20).size(500, 150).row();

        // Back button
        TextButton backButton = createButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen((Main) game, lobbyInfoClient));
            }
        });
        table.add(backButton).padBottom(20).size(500, 150).row();
    }

    private TextButton createButton(String text) {
        Texture buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        Texture buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        BitmapFont font = FontManager.getMenuFont();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonTexture);
        style.down = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonPressedTexture);

        return new TextButton(text, style);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
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
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        background.dispose();
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
