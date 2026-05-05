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
 * Builds the lobby choice screen UI layout.
 * Handles create and join lobby buttons.
 */
public class LobbyChoiceUIBuilder {
    private final Stage stage;
    private final Texture buttonTexture;
    private final Texture buttonPressedTexture;
    private final BitmapFont pixelFont;
    private final BitmapFont pixelFontLarge;

    public LobbyChoiceUIBuilder(Stage stage, Texture buttonTexture, Texture buttonPressedTexture,
                                BitmapFont pixelFont, BitmapFont pixelFontLarge) {
        this.stage = stage;
        this.buttonTexture = buttonTexture;
        this.buttonPressedTexture = buttonPressedTexture;
        this.pixelFont = pixelFont;
        this.pixelFontLarge = pixelFontLarge;
    }

    /**
     * Builds the lobby choice layout with create/join buttons
     */
    public void buildLayout(Runnable onCreate, Runnable onJoin, Runnable onBack) {
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Play", new Label.LabelStyle(pixelFontLarge, Color.WHITE));
        table.add(titleLabel).padTop(30).padBottom(50).row();

        // Create Lobby button
        TextButton createButton = createButton("Create Lobby");
        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onCreate.run();
            }
        });
        table.add(createButton).padBottom(20).size(500, 150).row();

        // Join Lobby button
        TextButton joinButton = createButton("Join Lobby");
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onJoin.run();
            }
        });
        table.add(joinButton).padBottom(20).size(500, 150).row();

        // Back button
        TextButton backButton = createButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onBack.run();
            }
        });
        table.add(backButton).padBottom(20).size(500, 150).row();
    }

    private TextButton createButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = pixelFont;
        style.fontColor = Color.WHITE;
        style.up = new TextureRegionDrawable(buttonTexture);
        style.down = new TextureRegionDrawable(buttonPressedTexture);

        return new TextButton(text, style);
    }
}
