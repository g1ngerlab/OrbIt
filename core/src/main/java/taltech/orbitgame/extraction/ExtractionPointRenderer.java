package taltech.orbitgame.extraction;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Renders extraction points on the game world.
 * Displays visual feedback based on extraction point state.
 */
public class ExtractionPointRenderer {
    private final ShapeRenderer shapeRenderer;
    private final ExtractionManager extractionManager;

    public ExtractionPointRenderer(ExtractionManager extractionManager) {
        this.extractionManager = extractionManager;
        this.shapeRenderer = new ShapeRenderer();
    }

    /**
     * Render all extraction points
     */
    public void render(SpriteBatch batch) {
        // Intentionally left blank: extraction platforms are now read from the map and should not be outlined.
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
