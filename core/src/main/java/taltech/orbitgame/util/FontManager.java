package taltech.orbitgame.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Utility class for loading and managing fonts
 */
public class FontManager {
    private static BitmapFont pixelFont;
    private static BitmapFont pixelFontLarge;

    /**
     * Load the pixel font from assets
     * @param size Font size
     * @return Generated BitmapFont
     */
    public static BitmapFont getPixelFont(int size) {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/PixelifySans-VariableFont_wght.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = size;
        parameter.color = Color.WHITE;
        parameter.borderWidth = 1;
        parameter.borderColor = Color.BLACK;
        BitmapFont font = generator.generateFont(parameter);
        generator.dispose();
        return font;
    }

    /**
     * Get default menu font (size 32)
     */
    public static BitmapFont getMenuFont() {
        if (pixelFont == null) {
            pixelFont = getPixelFont(32);
        }
        return pixelFont;
    }

    /**
     * Get large menu font (size 48)
     */
    public static BitmapFont getMenuFontLarge() {
        if (pixelFontLarge == null) {
            pixelFontLarge = getPixelFont(48);
        }
        return pixelFontLarge;
    }

    /**
     * Dispose all loaded fonts
     */
    public static void dispose() {
        if (pixelFont != null) {
            pixelFont.dispose();
            pixelFont = null;
        }
        if (pixelFontLarge != null) {
            pixelFontLarge.dispose();
            pixelFontLarge = null;
        }
    }
}
