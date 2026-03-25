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
    private static final String IDLE_SHEET_PATH = "npc/robotbutler.png";
    private static final String RUN_SHEET_PATH = "npc/runbutler.png";
    private static final String FIGHT_SHEET_PATH = "npc/fightbutler.png";
    private static final int RUN_FRAME_COUNT = 10;
    private static final int FIGHT_FRAME_COUNT = 5;
    private static Texture idleTexture;
    private static Texture runTexture;
    private static Texture fightTexture;
    private static Texture fallbackTexture;
    private static Animation<TextureRegion> idleAnimation;
    private static Animation<TextureRegion> runAnimation;
    private static Animation<TextureRegion> fightAnimation;

    private final Vector2 homePosition;
    private final float territoryRadius;
    private final float chaseStopDistance;
    private float patrolTimer;
    private Vector2 patrolTarget;
    private float stateTime;
    private boolean movingForAnimation;
    private boolean facingLeft;
    private float lastNetworkX;
    private float lastNetworkY;
    private boolean hasNetworkPos;
    private float attackTimer;

    public RobotButler(int id, float x, float y, float territoryRadius) {
        super(id, x, y, 25f); // RobotButler has 25 HP
        this.homePosition = new Vector2(x, y);
        this.territoryRadius = territoryRadius;
        this.speed = 80f;
        this.detectionRange = 150f;
        this.chaseStopDistance = 250f;
        this.size = 40f;
        this.patrolTimer = 0;
        this.patrolTarget = new Vector2(homePosition);
        this.stateTime = 0f;
        this.movingForAnimation = false;
        this.facingLeft = false;
        this.hasNetworkPos = false;
        this.attackTimer = 0f;
    }

    private static void ensureTexturesLoaded() {
        if (idleAnimation != null && runAnimation != null && fightAnimation != null) {
            return;
        }

        try {
            idleTexture = new Texture(Gdx.files.internal(IDLE_SHEET_PATH));
            runTexture = new Texture(Gdx.files.internal(RUN_SHEET_PATH));
            fightTexture = new Texture(Gdx.files.internal(FIGHT_SHEET_PATH));

            // Single-row sheets. Idle is a single frame, run is a 10-frame strip.
            idleAnimation = buildAnimation(idleTexture, 1, 0.15f);
            runAnimation = buildAnimation(runTexture, RUN_FRAME_COUNT, 0.08f);
            fightAnimation = buildAnimation(fightTexture, FIGHT_FRAME_COUNT, 0.07f);
        } catch (Exception e) {
            // If texture loading fails, keep robots visible with a plain square.
            idleAnimation = null;
            runAnimation = null;
            fightAnimation = null;
            ensureFallbackTexture();
        }
    }

    private static Animation<TextureRegion> buildAnimation(Texture texture, int frameCount, float frameDuration) {
        int safeFrameCount = Math.max(1, frameCount);
        int frameWidth = Math.max(1, texture.getWidth() / safeFrameCount);
        TextureRegion[][] split = TextureRegion.split(texture, frameWidth, texture.getHeight());
        TextureRegion[] frames = new TextureRegion[safeFrameCount];
        for (int i = 0; i < safeFrameCount; i++) {
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

        movingForAnimation = velocity.len() > 0.1f;
        if (Math.abs(velocity.x) > 0.01f) {
            facingLeft = velocity.x < 0f;
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

        if (idleAnimation == null || runAnimation == null || fightAnimation == null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(fallbackTexture, position.x - size / 2f, position.y - size / 2f, size, size);
            return;
        }

        if (attackTimer > 0f) {
            attackTimer -= Gdx.graphics.getDeltaTime();
        }

        boolean moving = movingForAnimation;
        boolean animating = moving || attackTimer > 0f;
        if (animating) {
            stateTime += Gdx.graphics.getDeltaTime();
        } else {
            stateTime = 0f;
        }

        Animation<TextureRegion> currentAnimation;
        if (attackTimer > 0f) {
            currentAnimation = fightAnimation;
        } else {
            currentAnimation = moving ? runAnimation : idleAnimation;
        }
        TextureRegion currentFrame = currentAnimation.getKeyFrame(stateTime, true);

        float drawX = position.x - size / 2f;
        float drawY = position.y - size / 2f;
        if (facingLeft) {
            batch.draw(currentFrame, drawX + size, drawY, -size, size);
        } else {
            batch.draw(currentFrame, drawX, drawY, size, size);
        }
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

    @Override
    public void applyNetworkState(float x, float y, boolean chasing, float size) {
        if (hasNetworkPos) {
            float dx = x - lastNetworkX;
            float dy = y - lastNetworkY;
            movingForAnimation = (Math.abs(dx) + Math.abs(dy)) > 0.1f;
            if (Math.abs(dx) > 0.01f) {
                facingLeft = dx < 0f;
            }
        } else {
            movingForAnimation = false;
            hasNetworkPos = true;
        }
        lastNetworkX = x;
        lastNetworkY = y;
        super.applyNetworkState(x, y, chasing, size);
    }

    @Override
    public void onAttack() {
        float duration = FIGHT_FRAME_COUNT * 0.07f;
        attackTimer = Math.max(attackTimer, duration);
        stateTime = 0f;
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
        if (fightTexture != null) {
            fightTexture.dispose();
            fightTexture = null;
        }
        if (fallbackTexture != null) {
            fallbackTexture.dispose();
            fallbackTexture = null;
        }
        idleAnimation = null;
        runAnimation = null;
        fightAnimation = null;
    }
}
