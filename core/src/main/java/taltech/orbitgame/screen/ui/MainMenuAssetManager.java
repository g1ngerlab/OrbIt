package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import taltech.orbitgame.util.FontManager;

/**
 * Manages loading and disposal of assets for the main menu screen.
 */
public class MainMenuAssetManager {
    private Texture spaceBackground;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private BitmapFont pixelFont;

    /**
     * Loads all required assets for the main menu
     */
    public void load() {
        spaceBackground = new Texture(Gdx.files.internal("LobbyBg.png"));

        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        buttonPressedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        pixelFont = FontManager.getMenuFont();
    }

    /**
     * Gets the space background texture
     */
    public Texture getSpaceBackground() {
        return spaceBackground;
    }

    /**
     * Gets the button up state texture
     */
    public Texture getButtonTexture() {
        return buttonTexture;
    }

    /**
     * Gets the button pressed state texture
     */
    public Texture getButtonPressedTexture() {
        return buttonPressedTexture;
    }

    /**
     * Gets the pixel font for menu text
     */
    public BitmapFont getPixelFont() {
        return pixelFont;
    }

    /**
     * Disposes of all loaded assets
     */
    public void dispose() {
        if (spaceBackground != null) {
            spaceBackground.dispose();
        }
        if (buttonTexture != null) {
            buttonTexture.dispose();
        }
        if (buttonPressedTexture != null) {
            buttonPressedTexture.dispose();
        }
        // Font is managed by FontManager, don't dispose here
    }
}
