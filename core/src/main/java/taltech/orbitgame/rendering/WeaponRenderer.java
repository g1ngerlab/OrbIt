package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;

/**
 * Handles rendering of weapons (shotguns) in the game
 */
public class WeaponRenderer implements Disposable {

    private static final float SHOTGUN_DRAW_WIDTH = 28f;
    private static final float SHOTGUN_DRAW_HEIGHT = 12f;

    private final Texture shotgunTexture;
    private final Vector2 direction;

    public WeaponRenderer() {
        this.shotgunTexture = new Texture(Gdx.files.internal("guns/test_shotgun.png"));
        this.direction = new Vector2();
    }

    /**
     * Draw a shotgun at the specified position aiming towards the target
     */
    public void drawShotgun(SpriteBatch batch, float ownerCenterX, float ownerCenterY, float aimX, float aimY) {
        direction.set(aimX - ownerCenterX, aimY - ownerCenterY);
        if (direction.isZero(0.001f)) {
            direction.set(1f, 0f);
        } else {
            direction.nor();
        }

        float angleDeg = direction.angleDeg();
        float anchorX = ownerCenterX + direction.x * 10f;
        float anchorY = ownerCenterY + direction.y * 6f;
        float originX = SHOTGUN_DRAW_WIDTH * 0.2f;
        float originY = SHOTGUN_DRAW_HEIGHT * 0.5f;

        batch.draw(
            shotgunTexture,
            anchorX - originX,
            anchorY - originY,
            originX,
            originY,
            SHOTGUN_DRAW_WIDTH,
            SHOTGUN_DRAW_HEIGHT,
            1f,
            1f,
            angleDeg,
            0,
            0,
            shotgunTexture.getWidth(),
            shotgunTexture.getHeight(),
            false,
            false
        );
    }

    @Override
    public void dispose() {
        shotgunTexture.dispose();
    }
}
