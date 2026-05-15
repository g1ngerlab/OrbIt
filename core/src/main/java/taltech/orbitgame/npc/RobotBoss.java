package taltech.orbitgame.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class RobotBoss extends Npc {
    private static final String SHEET_PATH = "npc/blueboss.png";
    private static final int FRAME_SIZE = 128;
    private static final int FRAME_COLUMNS = 24;

    private static final float MAX_HEALTH_VALUE = 450f;
    private static final float MOVE_SPEED = 22f;
    private static final float RETURN_SPEED = 28f;
    private static final float LEASH_DISTANCE = 420f;
    private static final float REGEN_PER_SECOND = 4f;
    private static final float FACE_FLIP_THRESHOLD = 1.5f;
    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float WALK_FRAME_DURATION = 0.07f;
    private static final float ATTACK_FRAME_DURATION = 0.05f;
    private static final float HIT_FRAME_DURATION = 0.05f;
    private static final float DEATH_FRAME_DURATION = 0.08f;
    private static final float ATTACK_ANIM_DURATION = 1.10f;
    private static final float HIT_ANIM_DURATION = 0.45f;

    private static Texture sheetTexture;
    private static Texture fallbackTexture;
    private static Animation<TextureRegion> idleAnimation;
    private static Animation<TextureRegion> walkAnimation;
    private static Animation<TextureRegion> attackAnimation;
    private static Animation<TextureRegion> hitAnimation;
    private static Animation<TextureRegion> deathAnimation;

    private final Vector2 homePosition;

    private boolean aggro;
    private boolean movingForAnimation;
    private boolean facingLeft;
    private float idleTime;
    private float walkTime;
    private float attackTime;
    private float hitTime;
    private float deathTime;
    private float attackTimer;
    private float hitTimer;
    private float lastNetworkX;
    private float lastNetworkY;
    private boolean hasNetworkPos;

    public RobotBoss(int id, float x, float y) {
        super(id, x, y, MAX_HEALTH_VALUE);
        this.homePosition = new Vector2(x, y);
        this.speed = MOVE_SPEED;
        this.detectionRange = 0f;
        this.size = 104f;
        this.aggro = false;
        this.movingForAnimation = false;
        this.facingLeft = false;
        this.idleTime = 0f;
        this.walkTime = 0f;
        this.attackTime = 0f;
        this.hitTime = 0f;
        this.deathTime = 0f;
        this.attackTimer = 0f;
        this.hitTimer = 0f;
        this.hasNetworkPos = false;
    }

    private static void ensureTexturesLoaded() {
        if (idleAnimation != null && walkAnimation != null && attackAnimation != null && hitAnimation != null && deathAnimation != null) {
            return;
        }

        try {
            sheetTexture = new Texture(Gdx.files.internal(SHEET_PATH));
            idleAnimation = buildAnimation(sheetTexture, 0, IDLE_FRAME_DURATION, true);
            walkAnimation = buildAnimation(sheetTexture, 1, WALK_FRAME_DURATION, true);
            attackAnimation = buildAnimation(sheetTexture, 2, ATTACK_FRAME_DURATION, false);
            hitAnimation = buildAnimation(sheetTexture, 3, HIT_FRAME_DURATION, false);
            deathAnimation = buildAnimation(sheetTexture, 4, DEATH_FRAME_DURATION, false);
        } catch (Exception e) {
            idleAnimation = null;
            walkAnimation = null;
            attackAnimation = null;
            hitAnimation = null;
            deathAnimation = null;
            ensureFallbackTexture();
        }
    }

    private static Animation<TextureRegion> buildAnimation(Texture texture, int rowIndex, float frameDuration, boolean loop) {
        TextureRegion[][] split = TextureRegion.split(texture, FRAME_SIZE, FRAME_SIZE);
        int safeRowIndex = Math.max(0, Math.min(rowIndex, split.length - 1));
        int frameCount = Math.min(FRAME_COLUMNS, split[safeRowIndex].length);
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = split[safeRowIndex][i];
        }
        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames);
        animation.setPlayMode(loop ? Animation.PlayMode.LOOP : Animation.PlayMode.NORMAL);
        return animation;
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
        if (isDead()) {
            deathTime += deltaTime;
            velocity.set(0f, 0f);
            isChasing = false;
            movingForAnimation = false;
            return;
        }

        if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - deltaTime);
            attackTime += deltaTime;
        }

        if (hitTimer > 0f) {
            hitTimer = Math.max(0f, hitTimer - deltaTime);
            hitTime += deltaTime;
        }

        if (!aggro) {
            isChasing = false;
            velocity.set(0f, 0f);
            movingForAnimation = false;
            heal(REGEN_PER_SECOND * deltaTime);
            idleTime += deltaTime;
            return;
        }

        Vector2 nearestPlayer = findNearestPlayer(playerPositions);
        if (nearestPlayer == null) {
            returnHomeAndRegen(deltaTime);
            return;
        }

        float distanceFromHomeToPlayer = homePosition.dst(nearestPlayer);
        if (distanceFromHomeToPlayer > LEASH_DISTANCE) {
            returnHomeAndRegen(deltaTime);
            return;
        }

        Vector2 toPlayer = new Vector2(nearestPlayer).sub(position);
        float distanceToPlayer = toPlayer.len();

        isChasing = true;

        if (distanceToPlayer > 8f) {
            velocity.set(toPlayer.nor().scl(speed));
            position.add(velocity.cpy().scl(deltaTime));
            movingForAnimation = true;
            walkTime += deltaTime;
        } else {
            velocity.set(0f, 0f);
            movingForAnimation = false;
            idleTime += deltaTime;
        }

        if (Math.abs(velocity.x) > FACE_FLIP_THRESHOLD) {
            facingLeft = velocity.x < 0f;
        }
    }

    private void returnHomeAndRegen(float deltaTime) {
        aggro = false;
        isChasing = false;

        Vector2 toHome = new Vector2(homePosition).sub(position);
        float distanceToHome = toHome.len();

        if (distanceToHome > 6f) {
            velocity.set(toHome.nor().scl(RETURN_SPEED));
            position.add(velocity.cpy().scl(deltaTime));
            movingForAnimation = true;
            walkTime += deltaTime;
            if (Math.abs(velocity.x) > FACE_FLIP_THRESHOLD) {
                facingLeft = velocity.x < 0f;
            }
        } else {
            position.set(homePosition);
            velocity.set(0f, 0f);
            movingForAnimation = false;
            idleTime += deltaTime;
        }

        heal(REGEN_PER_SECOND * deltaTime);
    }

    private Vector2 findNearestPlayer(List<Vector2> playerPositions) {
        if (playerPositions == null || playerPositions.isEmpty()) {
            return null;
        }

        Vector2 nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        for (Vector2 playerPosition : playerPositions) {
            if (playerPosition == null) {
                continue;
            }
            float distance = position.dst(playerPosition);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = playerPosition;
            }
        }
        return nearest;
    }

    @Override
    public void draw(SpriteBatch batch) {
        ensureTexturesLoaded();

        if (idleAnimation == null || walkAnimation == null || attackAnimation == null || hitAnimation == null || deathAnimation == null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(fallbackTexture, position.x - size / 2f, position.y - size / 2f, size, size);
            return;
        }

        Animation<TextureRegion> currentAnimation;
        float currentTime;
        boolean looping;

        if (isDead()) {
            currentAnimation = deathAnimation;
            currentTime = deathTime;
            looping = false;
        } else if (hitTimer > 0f) {
            currentAnimation = hitAnimation;
            currentTime = hitTime;
            looping = false;
        } else if (attackTimer > 0f) {
            currentAnimation = attackAnimation;
            currentTime = attackTime;
            looping = false;
        } else if (movingForAnimation) {
            currentAnimation = walkAnimation;
            currentTime = walkTime;
            looping = true;
        } else {
            currentAnimation = idleAnimation;
            currentTime = idleTime;
            looping = true;
        }

        TextureRegion currentFrame = currentAnimation.getKeyFrame(currentTime, looping);

        float renderSize = 180f;

        float drawX = position.x - renderSize / 2f;
        float drawY = position.y - renderSize / 2f;

        if (facingLeft) {
            batch.draw(currentFrame, drawX + renderSize, drawY, -renderSize, renderSize);
        } else {
            batch.draw(currentFrame, drawX, drawY, renderSize, renderSize);
        }
    }

    @Override
    public void takeDamage(float damage) {
        if (damage <= 0f) {
            return;
        }
        aggro = true;
        hitTimer = Math.max(hitTimer, HIT_ANIM_DURATION);
        hitTime = 0f;
        super.takeDamage(damage);
        if (isDead()) {
            velocity.set(0f, 0f);
            isChasing = false;
            movingForAnimation = false;
            aggro = false;
            deathTime = 0f;
        }
    }

    @Override
    public void onAttack() {
        attackTimer = Math.max(attackTimer, ATTACK_ANIM_DURATION);
        attackTime = 0f;
    }

    @Override
    public void applyNetworkState(float x, float y, boolean chasing, float size) {
        if (hasNetworkPos) {
            float dx = x - lastNetworkX;
            movingForAnimation = Math.abs(dx) > 0.1f;
            if (Math.abs(dx) > FACE_FLIP_THRESHOLD) {
                facingLeft = dx < 0f;
            }
        } else {
            movingForAnimation = false;
            hasNetworkPos = true;
        }

        lastNetworkX = x;
        lastNetworkY = y;
        aggro = chasing;
        super.applyNetworkState(x, y, chasing, size);
    }

    public Vector2 getHomePosition() {
        return homePosition;
    }

    public float getLeashDistance() {
        return LEASH_DISTANCE;
    }

    public static void disposeTextures() {
        if (sheetTexture != null) {
            sheetTexture.dispose();
            sheetTexture = null;
        }
        if (fallbackTexture != null) {
            fallbackTexture.dispose();
            fallbackTexture = null;
        }
        idleAnimation = null;
        walkAnimation = null;
        attackAnimation = null;
        hitAnimation = null;
        deathAnimation = null;
    }
}

