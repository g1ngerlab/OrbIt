package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import taltech.orbitgame.util.FontManager;

/**
 * Manages loading and disposal of assets for the settings menu screen.
 */
public class SettingsMenuAssetManager {
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture backgroundTexture;
    private BitmapFont pixelFont;

    /**
     * Loads all required assets for the settings menu
     */
    public void load() {
        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        buttonPressedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        backgroundTexture = new Texture(Gdx.files.internal("MainMenuAssets/set.png"));
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        pixelFont = FontManager.getMenuFont();
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
     * Gets the background texture
     */
    public Texture getBackgroundTexture() {
        return backgroundTexture;
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
        if (buttonTexture != null) {
            buttonTexture.dispose();
        }
        if (buttonPressedTexture != null) {
            buttonPressedTexture.dispose();
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        // Font is managed by FontManager, don't dispose here
    }
}
