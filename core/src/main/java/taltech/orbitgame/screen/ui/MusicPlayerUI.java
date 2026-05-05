package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.util.FontManager;

/**
 * Reusable music player UI component for lobby screens.
 * Handles music controls (previous, play/pause, next) and seek slider.
 */
public class MusicPlayerUI {
    private static final float MUSIC_CONTROL_WIDTH = 390f;
    private static final float MUSIC_CONTROL_WIDTH_WINDOWED = 270f;
    private static final float MUSIC_CONTROL_MARGIN = 16f;
    private static final float MUSIC_CONTROL_MARGIN_WINDOWED = 10f;
    private static final float MUSIC_CONTROL_PRESS_SCALE = 0.96f;
    private static final float MUSIC_SEEK_MARGIN_TOP = 0f;

    private Stage stage;
    private Image musicControlImage;
    private Slider musicSeekSlider;
    private Label musicSeekTimeLabel;
    private Texture musicControlTexture;
    private Texture musicSeekKnobTexture;
    private Texture musicSeekBackgroundTexture;
    private float musicSeekMaxSeconds = 1f;
    private boolean updatingMusicSeekSlider;

    public MusicPlayerUI(Stage stage) {
        this.stage = stage;
    }

    public void initialize() {
        // Load music control button
        musicControlTexture = new Texture(Gdx.files.internal("Buttons/music_button.png"));
        musicControlImage = new Image(new TextureRegionDrawable(musicControlTexture));
        float musicControlWidth = MUSIC_CONTROL_WIDTH;
        float musicControlHeight = musicControlWidth * (musicControlTexture.getHeight() / (float) musicControlTexture.getWidth());
        musicControlImage.setSize(musicControlWidth, musicControlHeight);
        musicControlImage.setOrigin(musicControlWidth * 0.5f, musicControlHeight * 0.5f);
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

        // Setup music seek slider
        musicSeekMaxSeconds = Math.max(1f, AudioManager.getInstance().getCurrentTrackDurationSeconds());
        musicSeekSlider = new Slider(0f, musicSeekMaxSeconds, 0.1f, false, createMusicSeekSliderStyle());
        musicSeekSlider.setValue(Math.min(
            AudioManager.getInstance().getCurrentTrackPositionSeconds(),
            musicSeekMaxSeconds
        ));
        musicSeekSlider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (updatingMusicSeekSlider) {
                    return;
                }
                AudioManager.getInstance().seekCurrentTrackSeconds(musicSeekSlider.getValue());
            }
        });
        stage.addActor(musicSeekSlider);

        // Setup time label
        musicSeekTimeLabel = new Label("00:00", new Label.LabelStyle(FontManager.getPixelFont(12), Color.WHITE));
        musicSeekTimeLabel.setAlignment(Align.center);
        stage.addActor(musicSeekTimeLabel);

        layoutControls(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        updateVisibility();
    }

    public void layoutControls(int width, int height) {
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

    public void update(float delta) {
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

    public void updateVisibility() {
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

    public void dispose() {
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

    private float resolveMusicControlWidth() {
        return Gdx.graphics.isFullscreen() ? MUSIC_CONTROL_WIDTH : MUSIC_CONTROL_WIDTH_WINDOWED;
    }

    private float resolveMusicControlMargin() {
        return Gdx.graphics.isFullscreen() ? MUSIC_CONTROL_MARGIN : MUSIC_CONTROL_MARGIN_WINDOWED;
    }
}
