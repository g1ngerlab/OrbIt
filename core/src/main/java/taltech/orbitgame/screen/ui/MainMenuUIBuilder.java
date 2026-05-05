package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.player.PlayerProfile;

/**
 * Builds the main menu UI layout.
 * Handles character preview display, button layouts, and title rendering.
 */
public class MainMenuUIBuilder {
    private final Stage stage;
    private final Texture buttonTexture;
    private final Texture buttonPressedTexture;
    private final BitmapFont pixelFont;

    public MainMenuUIBuilder(Stage stage, Texture buttonTexture, Texture buttonPressedTexture, BitmapFont pixelFont) {
        this.stage = stage;
        this.buttonTexture = buttonTexture;
        this.buttonPressedTexture = buttonPressedTexture;
        this.pixelFont = pixelFont;
    }

    /**
     * Builds the main menu layout with character preview and buttons
     */
    public void buildLayout(Runnable onCharacters, Runnable onPlay, Runnable onInventory,
                           Runnable onShop, Runnable onSettings, Runnable onLeave) {
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Table leftTable = new Table();
        Table rightTable = new Table();
        mainTable.add(leftTable).padRight(60).left();
        mainTable.add(rightTable).padLeft(60).right();

        // Add "Orbit" title
        Label titleLabel = createTitle("Orbit");
        rightTable.add(titleLabel).padBottom(50).row();

        // Character preview
        Texture characterPreviewTexture = loadCharacterPreview();
        Image characterImage = new Image(new TextureRegionDrawable(characterPreviewTexture));
        leftTable.add(characterImage).size(260, 260).padBottom(25).row();

        // Characters button
        TextButton charactersButton = createButton("CHARACTERS");
        charactersButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onCharacters.run();
            }
        });
        leftTable.add(charactersButton).padBottom(20).size(charactersButton.getWidth(), charactersButton.getHeight()).row();

        // Play button
        TextButton playButton = createButton("PLAY");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onPlay.run();
            }
        });
        rightTable.add(playButton).padBottom(16).size(playButton.getWidth(), playButton.getHeight()).row();

        // Inventory button
        TextButton inventoryButton = createButton("INVENTORY");
        inventoryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onInventory.run();
            }
        });
        rightTable.add(inventoryButton).padBottom(16).size(inventoryButton.getWidth(), inventoryButton.getHeight()).row();

        // Shop button
        TextButton shopButton = createButton("SHOP");
        shopButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onShop.run();
            }
        });
        rightTable.add(shopButton).padBottom(16).size(shopButton.getWidth(), shopButton.getHeight()).row();

        // Settings button
        TextButton settingsButton = createButton("SETTINGS");
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onSettings.run();
            }
        });
        rightTable.add(settingsButton).padBottom(16).size(settingsButton.getWidth(), settingsButton.getHeight()).row();

        // Leave button
        TextButton leaveButton = createButton("LEAVE");
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onLeave.run();
            }
        });
        rightTable.add(leaveButton).padBottom(16).size(leaveButton.getWidth(), leaveButton.getHeight()).row();
    }

    private Label createTitle(String text) {
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = pixelFont;
        titleStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label(text, titleStyle);
        titleLabel.setFontScale(2.0f);
        return titleLabel;
    }

    private TextButton createButton(String text) {
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = pixelFont;
        buttonStyle.fontColor = Color.WHITE;

        TextButton button = new TextButton(text, buttonStyle);
        float buttonWidth = buttonTexture.getWidth() * 0.5f;
        float buttonHeight = buttonTexture.getHeight() * 0.5f;
        button.setSize(buttonWidth, buttonHeight);
        button.getLabel().setAlignment(Align.center);
        button.getLabelCell().size(buttonWidth, buttonHeight);

        return button;
    }

    private Texture loadCharacterPreview() {
        CharacterType selectedCharacter = CharacterType.fromId(PlayerProfile.getSelectedCharacterId());
        Texture texture = new Texture(Gdx.files.internal(selectedCharacter.getPreviewTexturePath()));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }
}
