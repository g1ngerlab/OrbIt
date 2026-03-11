package taltech.orbitgame.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

/**
 * Helper class for rendering health bars above entities
 */
public class HealthBarRenderer {
    private final ShapeRenderer shapeRenderer;

    private static final float BAR_WIDTH = 40f;
    private static final float BAR_HEIGHT = 5f;
    private static final float BAR_OFFSET_Y = 10f;

    private static final Color BACKGROUND_COLOR = new Color(0.2f, 0.2f, 0.2f, 0.8f);
    private static final Color HEALTH_COLOR = new Color(0.2f, 0.8f, 0.2f, 1f);
    private static final Color LOW_HEALTH_COLOR = new Color(0.8f, 0.2f, 0.2f, 1f);
    private static final float LOW_HEALTH_THRESHOLD = 0.3f;

    public HealthBarRenderer(ShapeRenderer shapeRenderer) {
        this.shapeRenderer = shapeRenderer;
    }

    /**
     * Draw a health bar above an entity
     * @param x Center X position of the entity
     * @param y Top Y position of the entity (health bar will be drawn above this)
     * @param entityWidth Width of the entity (for centering)
     * @param healthPercentage Health as a percentage (0.0 to 1.0)
     */
    public void drawHealthBar(float x, float y, float entityWidth, float healthPercentage) {
        float barX = x + (entityWidth - BAR_WIDTH) * 0.5f;
        float barY = y + BAR_OFFSET_Y;

        // Draw background
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT);

        // Draw health
        float healthWidth = BAR_WIDTH * Math.max(0f, Math.min(1f, healthPercentage));
        Color healthColor = healthPercentage < LOW_HEALTH_THRESHOLD ? LOW_HEALTH_COLOR : HEALTH_COLOR;
        shapeRenderer.setColor(healthColor);
        shapeRenderer.rect(barX, barY, healthWidth, BAR_HEIGHT);
    }

    /**
     * Draw a health bar with custom dimensions
     */
    public void drawHealthBar(float x, float y, float width, float height, float healthPercentage) {

        // Draw background
        shapeRenderer.setColor(BACKGROUND_COLOR);
        shapeRenderer.rect(x, y, width, height);

        // Draw health
        float healthWidth = width * Math.max(0f, Math.min(1f, healthPercentage));
        Color healthColor = healthPercentage < LOW_HEALTH_THRESHOLD ? LOW_HEALTH_COLOR : HEALTH_COLOR;
        shapeRenderer.setColor(healthColor);
        shapeRenderer.rect(x, y, healthWidth, height);
    }
}

