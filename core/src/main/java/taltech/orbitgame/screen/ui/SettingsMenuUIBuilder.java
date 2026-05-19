package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Builds the settings menu UI layout.
 * Handles volume controls and settings options.
 */
public class SettingsMenuUIBuilder {
    private final Stage stage;
    private final Texture buttonTexture;
    private final Texture buttonPressedTexture;
    private final BitmapFont pixelFont;

    public SettingsMenuUIBuilder(Stage stage, Texture buttonTexture, Texture buttonPressedTexture, BitmapFont pixelFont) {
        this.stage = stage;
        this.buttonTexture = buttonTexture;
        this.buttonPressedTexture = buttonPressedTexture;
        this.pixelFont = pixelFont;
    }

    /**
     * Builds the settings menu layout with volume controls and back button
     */
    public void buildLayout(Runnable onBack) {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        Label titleLabel = createTitle("SETTINGS");
        table.add(titleLabel).padBottom(60).row();

        // Initialize audio volume UI
        AudioVolumeUI audioVolumeUI = new AudioVolumeUI();
        audioVolumeUI.createVolumeControls(table);

        // Create Back button
        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBack.run();
            }
        });
        table.add(backButton).padBottom(20).row();
    }

    private Label createTitle(String text) {
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = pixelFont;
        titleStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label(text, titleStyle);
        titleLabel.setFontScale(1.5f);
        return titleLabel;
    }

    private TextButton createButton(String text) {
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

        // Center text in the button
        button.getLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
        button.getLabelCell().size(buttonWidth, buttonHeight);

        return button;
    }
}
