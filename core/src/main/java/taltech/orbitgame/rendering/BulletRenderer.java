package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Handles rendering of bullets in the game
 */
public class BulletRenderer implements Disposable {

    public static class ClientBullet {
        public float x;
        public float y;
        public float angleDeg;
        public float radius;
    }

    private static final float BULLET_DRAW_SCALE = 2.0f;

    private final Texture bulletTexture;
    private final Map<Integer, ClientBullet> bullets;

    public BulletRenderer() {
        this.bulletTexture = createOrangeBulletTexture();
        this.bullets = new HashMap<>();
    }

    /**
     * Update bullet state from server data
     */
    public void updateBullet(int bulletId, float x, float y, float angleDeg, float radius) {
        ClientBullet bullet = bullets.get(bulletId);
        if (bullet == null) {
            bullet = new ClientBullet();
            bullets.put(bulletId, bullet);
        }
        bullet.x = x;
        bullet.y = y;
        bullet.angleDeg = angleDeg;
        bullet.radius = radius;
    }

    /**
     * Remove bullets that are no longer active
     */
    public void cleanupBullets(Set<Integer> activeBulletIds) {
        bullets.keySet().removeIf(id -> !activeBulletIds.contains(id));
    }

    /**
     * Render all active bullets
     */
    public void render(SpriteBatch batch) {
        for (ClientBullet bullet : bullets.values()) {
            float drawW = Math.max(6f, bullet.radius * BULLET_DRAW_SCALE);
            float drawH = drawW;
            float origin = drawW * 0.5f;

            batch.draw(
                bulletTexture,
                bullet.x - origin,
                bullet.y - origin,
                origin,
                origin,
                drawW,
                drawH,
                1f,
                1f,
                bullet.angleDeg,
                0,
                0,
                bulletTexture.getWidth(),
                bulletTexture.getHeight(),
                false,
                false
            );
        }
    }

    /**
     * Create the orange bullet texture
     */
    private Texture createOrangeBulletTexture() {
        int size = 24;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(0f, 0f, 0f, 0f);
        pixmap.fill();

        int center = size / 2;
        int radius = 10;

        // Main orange body
        pixmap.setColor(1f, 0.55f, 0.1f, 1f);
        pixmap.fillCircle(center, center, radius);

        // Bright inner core for a denser look
        pixmap.setColor(1f, 0.75f, 0.3f, 1f);
        pixmap.fillCircle(center, center, radius - 3);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    @Override
    public void dispose() {
        bulletTexture.dispose();
    }
}
