package taltech.orbitgame.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import taltech.orbitgame.world.Player;

/**
 * Handles camera positioning and updates to follow the player
 */
public class CameraController {

    private final OrthographicCamera camera;

    public CameraController(OrthographicCamera camera) {
        this.camera = camera;
    }

    /**
     * Update camera position to follow the player within map bounds
     */
    public void updateCamera(Player player, float mapWidthPx, float mapHeightPx) {
        float halfW = camera.viewportWidth * 0.5f;
        float halfH = camera.viewportHeight * 0.5f;

        float targetX = player.x + player.getWidth() * 0.5f;
        float targetY = player.y + player.getHeight() * 0.5f;

        // Clamp camera X position
        if (mapWidthPx <= camera.viewportWidth) {
            camera.position.x = mapWidthPx * 0.5f;
        } else {
            camera.position.x = MathUtils.clamp(targetX, halfW, mapWidthPx - halfW);
        }

        // Clamp camera Y position
        if (mapHeightPx <= camera.viewportHeight) {
            camera.position.y = mapHeightPx * 0.5f;
        } else {
            camera.position.y = MathUtils.clamp(targetY, halfH, mapHeightPx - halfH);
        }
    }

    public OrthographicCamera getCamera() {
        return camera;
    }
}
