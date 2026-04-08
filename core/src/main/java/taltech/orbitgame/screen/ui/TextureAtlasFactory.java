package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

/**
 * Factory for creating reusable TextureRegion drawables from a texture atlas.
 */
public class TextureAtlasFactory {
    private final Texture atlasTexture;
    private final int tileSize;

    public TextureAtlasFactory(Texture atlasTexture, int tileSize) {
        this.atlasTexture = atlasTexture;
        this.tileSize = tileSize;
    }

    public TextureRegionDrawable createDrawableFromAtlas(int column, int row) {
        TextureRegion region = new TextureRegion(
            atlasTexture,
            column * tileSize,
            row * tileSize,
            tileSize,
            tileSize
        );
        return new TextureRegionDrawable(region);
    }

    public TextureRegion createRegionFromAtlas(int column, int row) {
        return new TextureRegion(
            atlasTexture,
            column * tileSize,
            row * tileSize,
            tileSize,
            tileSize
        );
    }
}

