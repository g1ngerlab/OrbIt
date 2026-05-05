package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import taltech.orbitgame.util.FontManager;

/**
 * Manages loading and disposal of assets for the lobby choice screen.
 */
public class LobbyChoiceAssetManager {
    private Texture backgroundTexture;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private BitmapFont pixelFontLarge;
    private BitmapFont pixelFont;

    /**
     * Loads all required assets for the lobby choice screen
     */
    public void load() {
        backgroundTexture = new Texture(Gdx.files.internal("LobbyBg.png"));

        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        buttonPressedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        pixelFont = FontManager.getMenuFont();
        pixelFontLarge = FontManager.getMenuFontLarge();
    }

    /**
     * Gets the background texture
     */
    public Texture getBackgroundTexture() {
        return backgroundTexture;
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
     * Gets the large pixel font
     */
    public BitmapFont getPixelFontLarge() {
        return pixelFontLarge;
    }

    /**
     * Gets the regular pixel font
     */
    public BitmapFont getPixelFont() {
        return pixelFont;
    }

    /**
     * Disposes of all loaded assets
     */
    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        if (buttonTexture != null) {
            buttonTexture.dispose();
        }
        if (buttonPressedTexture != null) {
            buttonPressedTexture.dispose();
        }
        // Fonts are managed by FontManager, don't dispose here
    }
}
