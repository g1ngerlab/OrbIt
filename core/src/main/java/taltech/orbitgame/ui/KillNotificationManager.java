package taltech.orbitgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import taltech.orbitgame.util.FontManager;

/**
 * Manages kill notifications that appear in the top right corner
 */
public class KillNotificationManager {
    private static final float DISPLAY_DURATION = 3f; // Show for 3 seconds
    private static final float FADE_OUT_DURATION = 0.5f; // Fade out last 0.5 seconds
    private static final float POPUP_PADDING = 16f;

    private String killMessage = null;
    private float killDisplayTimer = 0f;
    private boolean hasActiveKill = false;

    private final BitmapFont font;
    private final GlyphLayout layout;

    public KillNotificationManager() {
        this.font = FontManager.getMenuFont();
        this.layout = new GlyphLayout();
    }

    /**
     * Add a kill notification to display
     */
    public void addKillNotification(String killerName, String killedPlayerName) {
        this.killMessage = killerName + " slayed " + killedPlayerName;
        this.killDisplayTimer = DISPLAY_DURATION;
        this.hasActiveKill = true;
    }

    /**
     * Update kill notification timer
     */
    public void update(float delta) {
        if (!hasActiveKill) {
            return;
        }

        killDisplayTimer -= delta;
        if (killDisplayTimer <= 0) {
            hasActiveKill = false;
            killMessage = null;
        }
    }

    /**
     * Render kill notification in top right corner
     */
    public void render(SpriteBatch batch) {
        if (!hasActiveKill || killMessage == null) {
            return;
        }

        batch.begin();

        // Calculate alpha with fade out
        float alpha = 1f;
        if (killDisplayTimer < FADE_OUT_DURATION) {
            alpha = killDisplayTimer / FADE_OUT_DURATION;
        }

        // Set font and color (golden yellow for kill notification)
        font.setColor(1f, 0.84f, 0f, alpha); // Gold color

        // Calculate position (top right corner)
        layout.setText(font, killMessage);
        float x = Gdx.graphics.getWidth() - layout.width - POPUP_PADDING;
        float y = Gdx.graphics.getHeight() - POPUP_PADDING;

        // Draw the kill notification
        font.draw(batch, killMessage, x, y);

        // Reset color
        font.setColor(Color.WHITE);
        batch.end();
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        // Font is managed by FontManager, don't dispose it here
    }
}
