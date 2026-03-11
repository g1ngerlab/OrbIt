package taltech.orbitgame.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class RobotButler extends Npc {
    private static Texture idleTexture;
    private static Texture runTexture;
    private static Texture fallbackTexture;
    private static Animation<TextureRegion> idleAnimation;
    private static Animation<TextureRegion> runAnimation;

    private final Vector2 homePosition;
    private final float territoryRadius;
    private final float chaseStopDistance;
    private float patrolTimer;
    private Vector2 patrolTarget;
    private float stateTime;

    public RobotButler(int id, float x, float y, float territoryRadius) {
        super(id, x, y, 25f); // RobotButler has 25 HP
        this.homePosition = new Vector2(x, y);
        this.territoryRadius = territoryRadius;
        this.speed = 80f;
        this.detectionRange = 150f;
        this.chaseStopDistance = 250f;
        this.size = 32f;
        this.patrolTimer = 0;
        this.patrolTarget = new Vector2(homePosition);
        this.stateTime = 0f;
    }

    private static void ensureTexturesLoaded() {
        if (idleAnimation != null && runAnimation != null) {
            return;
        }

        try {
            idleTexture = new Texture(Gdx.files.internal("npc/npc1/npc1-idle-Sheet.png"));
            runTexture = new Texture(Gdx.files.internal("npc/npc1/npc1-Sheet.png"));

            // Current sheets are 64px high, one row of frames.
            idleAnimation = buildAnimation(idleTexture, 64, 0.15f);
            runAnimation = buildAnimation(runTexture, 64, 0.08f);
        } catch (Exception e) {
            // If texture loading fails, keep robots visible with a plain square.
            idleAnimation = null;
            runAnimation = null;
            ensureFallbackTexture();
        }
    }

    private static Animation<TextureRegion> buildAnimation(Texture texture, int frameSize, float frameDuration) {
        int frameCount = Math.max(1, texture.getWidth() / frameSize);
        TextureRegion[][] split = TextureRegion.split(texture, frameSize, texture.getHeight());
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = split[0][i];
        }
        return new Animation<>(frameDuration, frames);
    }

    private static void ensureFallbackTexture() {
        if (fallbackTexture != null) {
            return;
        }
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 1f, 1f, 1f);
        pixmap.fill();
        fallbackTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    @Override
    public void update(float deltaTime, List<Vector2> playerPositions) {
        Vector2 targetPlayer = findNearestPlayer(playerPositions);
        float distanceToPlayer = targetPlayer == null ? Float.MAX_VALUE : position.dst(targetPlayer);

        if (targetPlayer != null && distanceToPlayer < detectionRange) {
            isChasing = true;
            Vector2 direction = new Vector2(targetPlayer).sub(position).nor();
            velocity.set(direction.scl(speed));
        } else if (isChasing && distanceToPlayer > chaseStopDistance) {
            isChasing = false;
            patrolTarget = new Vector2(homePosition);
        }

        if (!isChasing) {
            updatePatrol(deltaTime);
        }

        position.add(velocity.cpy().scl(deltaTime));

        // Update animation timer
        if (velocity.len() > 0.1f) {
            stateTime += deltaTime;
        } else {
            stateTime = 0f;
        }
    }

    private Vector2 findNearestPlayer(List<Vector2> playerPositions) {
        if (playerPositions == null || playerPositions.isEmpty()) {
            return null;
        }

        Vector2 nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Vector2 playerPosition : playerPositions) {
            float distance = position.dst(playerPosition);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = playerPosition;
            }
        }
        return nearest;
    }

    private void updatePatrol(float deltaTime) {
        Vector2 directionToTarget = new Vector2(patrolTarget).sub(position);
        float distanceToTarget = directionToTarget.len();

        if (distanceToTarget < 5f) {
            patrolTimer -= deltaTime;
            if (patrolTimer <= 0) {
                float angle = (float) (Math.random() * Math.PI * 2);
                float distance = (float) (Math.random() * territoryRadius);
                patrolTarget = new Vector2(
                    homePosition.x + (float) Math.cos(angle) * distance,
                    homePosition.y + (float) Math.sin(angle) * distance
                );
                patrolTimer = 2f;
            }
            velocity.set(0f, 0f);
        } else {
            velocity.set(directionToTarget.nor().scl(speed));
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        ensureTexturesLoaded();

        if (idleAnimation == null || runAnimation == null) {
            batch.setColor(isChasing ? 1f : 0f, 0f, 0f, 1f);
            batch.draw(fallbackTexture, position.x - size / 2f, position.y - size / 2f, size, size);
            batch.setColor(1f, 1f, 1f, 1f);
            return;
        }

        Animation<TextureRegion> currentAnimation = isChasing ? runAnimation : idleAnimation;
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);

        // Tint red when chasing
        if (isChasing) {
            batch.setColor(1f, 0.3f, 0.3f, 1f);
        } else {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        batch.draw(currentFrame, position.x - size / 2f, position.y - size / 2f, size, size);
        batch.setColor(1f, 1f, 1f, 1f); // Reset color
    }

    @Override
    public void dispose() {
        // Textures are static and shared, dispose only once when all robots are done
    }

    public Vector2 getHomePosition() {
        return homePosition;
    }

    public float getTerritoryRadius() {
        return territoryRadius;
    }

    public static void disposeTextures() {
        if (idleTexture != null) {
            idleTexture.dispose();
            idleTexture = null;
        }
        if (runTexture != null) {
            runTexture.dispose();
            runTexture = null;
        }
        if (fallbackTexture != null) {
            fallbackTexture.dispose();
            fallbackTexture = null;
        }
        idleAnimation = null;
        runAnimation = null;
    }
}
