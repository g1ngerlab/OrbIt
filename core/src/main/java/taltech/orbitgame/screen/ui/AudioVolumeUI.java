package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.util.FontManager;

/**
 * Reusable audio volume control UI component.
 * Provides sliders for music and SFX volume with percentage display.
 */
public class AudioVolumeUI {
    private Label musicValueLabel;
    private Label sfxValueLabel;
    private Slider musicSlider;
    private Slider sfxSlider;

    /**
     * Creates the music and SFX volume UI within the given table.
     */
    public void createVolumeControls(Table table) {
        // Label style
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = FontManager.getMenuFont();
        labelStyle.fontColor = Color.WHITE;

        // Create slider styles
        Slider.SliderStyle sliderStyle = createSliderStyle();

        // Music Volume Section
        Label musicLabel = new Label("Music Volume", labelStyle);
        musicSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        float currentMusicVolume = AudioManager.getInstance().getMusicVolume();
        musicSlider.setValue(currentMusicVolume);
        musicValueLabel = new Label((int)(currentMusicVolume * 100) + "%", labelStyle);

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                AudioManager.getInstance().setMusicVolume(musicSlider.getValue());
                musicValueLabel.setText((int)(musicSlider.getValue() * 100) + "%");
            }
        });

        table.add(musicLabel).left().padBottom(10).row();
        Table musicTable = new Table();
        musicTable.add(musicSlider).width(300).height(24).padRight(20);
        musicTable.add(musicValueLabel).width(50);
        table.add(musicTable).padBottom(30).row();

        // SFX Volume Section
        Label sfxLabel = new Label("SFX Volume", labelStyle);
        sfxSlider = new Slider(0f, 1f, 0.01f, false, sliderStyle);
        float currentSfxVolume = AudioManager.getInstance().getSfxVolume();
        sfxSlider.setValue(currentSfxVolume);
        sfxValueLabel = new Label((int)(currentSfxVolume * 100) + "%", labelStyle);

        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeListener.ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                AudioManager.getInstance().setSfxVolume(sfxSlider.getValue());
                sfxValueLabel.setText((int)(sfxSlider.getValue() * 100) + "%");
            }
        });

        table.add(sfxLabel).left().padBottom(10).row();
        Table sfxTable = new Table();
        sfxTable.add(sfxSlider).width(300).height(24).padRight(20);
        sfxTable.add(sfxValueLabel).width(50);
        table.add(sfxTable).padBottom(60).row();
    }

    private Slider.SliderStyle createSliderStyle() {
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();

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
}
