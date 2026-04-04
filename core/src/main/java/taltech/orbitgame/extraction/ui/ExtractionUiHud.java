package taltech.orbitgame.extraction.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import taltech.orbitgame.extraction.ExtractionPoint;
import taltech.orbitgame.util.FontManager;

/**
 * Renders extraction point UI elements:
 * - "E" button prompt when near extraction point
 * - Extraction timer when in progress
 */
public class ExtractionUiHud {
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final GlyphLayout layout;

    private ExtractionPoint nearbyPoint;
    private boolean extracting;
    private float remainingTime;
    private int localClientId;

    private static final float BUTTON_PROMPT_DISTANCE = 80f;
    private static final float TIMER_SIZE = 100f;

    public ExtractionUiHud(int localClientId) {
        this.localClientId = localClientId;
        this.shapeRenderer = new ShapeRenderer();
        this.font = FontManager.getMenuFont();
        this.layout = new GlyphLayout();
    }

    /**
     * Set the nearby extraction point (if any)
     */
    public void setNearbyPoint(ExtractionPoint point) {
        this.nearbyPoint = point;
    }

    /**
     * Set the extraction point currently being extracted from
     */
    public void setExtractingPoint(ExtractionPoint point) {
        this.extracting = point != null;
    }

    public void setExtractionTimer(float remainingTime) {
        this.remainingTime = Math.max(0f, remainingTime);
    }

    /**
     * Render UI elements
     */
    public void render(SpriteBatch batch) {
        // Render "E" button prompt
        if (nearbyPoint != null && nearbyPoint.getCurrentState() == ExtractionPoint.State.AVAILABLE) {
            renderButtonPrompt(batch, nearbyPoint);
        }

        // Render extraction timer
        if (extracting) {
            renderExtractionTimer(batch, remainingTime);
        }
    }

    /**
     * Render the "E" button prompt near the extraction point
     */
    private void renderButtonPrompt(SpriteBatch batch, ExtractionPoint point) {
        float screenCenterX = Gdx.graphics.getWidth() / 2f;
        float screenCenterY = Gdx.graphics.getHeight() / 2f;

        batch.begin();

        font.setColor(0.8f, 1f, 1f, 1f);
        layout.setText(font, "Press E to Extract");
        font.draw(batch, "Press E to Extract",
            screenCenterX - layout.width / 2f,
            screenCenterY + 20f);

        batch.end();
    }

    /**
     * Render the extraction timer
     */
    private void renderExtractionTimer(SpriteBatch batch, float remainingTime) {
        float screenCenterX = Gdx.graphics.getWidth() / 2f;
        float screenCenterY = Gdx.graphics.getHeight() / 2f;

        batch.begin();

        font.setColor(0f, 1f, 0.5f, 1f);
        String timerText = String.format("%.1f s", remainingTime);
        layout.setText(font, timerText);
        font.draw(batch, timerText,
            screenCenterX - layout.width / 2f,
            screenCenterY + layout.height / 2f);

        font.setColor(1f, 1f, 1f, 1f);
        layout.setText(font, "Extracting...");
        font.draw(batch, "Extracting...",
            screenCenterX - layout.width / 2f,
            screenCenterY - 40f);

        batch.end();
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
