package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;

/**
 * Manages custom cursor creation and handling
 */
public class CursorManager implements Disposable {

    private final Pixmap crosshairPixmap;
    private final Cursor crosshairCursor;

    public CursorManager(String crosshairPath, int scale) {
        this.crosshairPixmap = createScaledCrosshairPixmap(crosshairPath, scale);
        this.crosshairCursor = Gdx.graphics.newCursor(
            crosshairPixmap,
            crosshairPixmap.getWidth() / 2,
            crosshairPixmap.getHeight() / 2
        );
    }

    /**
     * Set the custom crosshair cursor
     */
    public void setCrosshairCursor() {
        Gdx.graphics.setCursor(crosshairCursor);
    }

    /**
     * Reset to system default cursor
     */
    public void resetCursor() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
    }

    /**
     * Create a scaled version of the crosshair pixmap
     */
    private Pixmap createScaledCrosshairPixmap(String internalPath, int scale) {
        Pixmap source = new Pixmap(Gdx.files.internal(internalPath));
        int scaledWidth = Math.max(1, source.getWidth() * scale);
        int scaledHeight = Math.max(1, source.getHeight() * scale);

        Pixmap scaled = new Pixmap(scaledWidth, scaledHeight, Pixmap.Format.RGBA8888);
        scaled.setBlending(Pixmap.Blending.None);
        scaled.drawPixmap(
            source,
            0,
            0,
            source.getWidth(),
            source.getHeight(),
            0,
            0,
            scaledWidth,
            scaledHeight
        );
        source.dispose();
        return scaled;
    }

    @Override
    public void dispose() {
        resetCursor();
        crosshairCursor.dispose();
        crosshairPixmap.dispose();
    }
}
