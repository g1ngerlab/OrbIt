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
    private static final String IDLE_SHEET_PATH = "npc/BossN/boss_idle.png";
    private static final String WALK_SHEET_PATH = "npc/BossN/boss_walks.png";
    private static final String PUNCH_SHEET_PATH = "npc/BossN/boss_punch.png";
    private static final String DEATH_SHEET_PATH = "npc/BossN/boss_death.png";

    private static final float MAX_HEALTH_VALUE = 450f;
    private static final float MOVE_SPEED = 22f;
    private static final float RETURN_SPEED = 28f;
    private static final float LEASH_DISTANCE = 420f;
    private static final float REGEN_PER_SECOND = 4f;
    private static final float FACE_FLIP_THRESHOLD = 1.5f;
    private static final float IDLE_FRAME_DURATION = 0.12f;
    private static final float WALK_FRAME_DURATION = 0.07f;
    private static final float PUNCH_FRAME_DURATION = 0.04f;
    private static final float DEATH_FRAME_DURATION = 0.08f;
    private static final float ATTACK_ANIM_DURATION = 0.90f;
    private static final float RENDER_SIZE = 180f;

    private static Texture idleTexture;
    private static Texture walkTexture;
    private static Texture punchTexture;
    private static Texture deathTexture;
    private static Texture fallbackTexture;
    private static Animation<TextureRegion> idleAnimation;
    private static Animation<TextureRegion> walkAnimation;
    private static Animation<TextureRegion> punchAnimation;
    private static Animation<TextureRegion> deathAnimation;

    private final Vector2 homePosition;

    private boolean aggro;
    private boolean movingForAnimation;
    private boolean facingLeft;
    private boolean movingEastForAnimation;
    private boolean dying;
    private float idleTime;
    private float walkTime;
    private float punchTime;
    private float deathTime;
    private float attackTimer;
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
        this.movingEastForAnimation = false;
        this.dying = false;
        this.idleTime = 0f;
        this.walkTime = 0f;
        this.punchTime = 0f;
        this.deathTime = 0f;
        this.attackTimer = 0f;
        this.hasNetworkPos = false;
    }

    private static void ensureTexturesLoaded() {
        if (idleAnimation != null && walkAnimation != null && punchAnimation != null && deathAnimation != null) {
            return;
        }

        try {
            idleTexture = new Texture(Gdx.files.internal(IDLE_SHEET_PATH));
            walkTexture = new Texture(Gdx.files.internal(WALK_SHEET_PATH));
            punchTexture = new Texture(Gdx.files.internal(PUNCH_SHEET_PATH));
            deathTexture = new Texture(Gdx.files.internal(DEATH_SHEET_PATH));

            idleAnimation = buildAnimation(idleTexture, IDLE_FRAME_DURATION, true);
            walkAnimation = buildAnimation(walkTexture, WALK_FRAME_DURATION, true);
            punchAnimation = buildAnimation(punchTexture, PUNCH_FRAME_DURATION, false);
            deathAnimation = buildAnimation(deathTexture, DEATH_FRAME_DURATION, false);
        } catch (Exception e) {
            idleAnimation = null;
            walkAnimation = null;
            punchAnimation = null;
            deathAnimation = null;
            ensureFallbackTexture();
        }
    }

    private static Animation<TextureRegion> buildAnimation(Texture texture, float frameDuration, boolean loop) {
        int frameSize = texture.getHeight();
        int frameCount = Math.max(1, texture.getWidth() / frameSize);
        TextureRegion[][] split = TextureRegion.split(texture, frameSize, frameSize);
        TextureRegion[] frames = new TextureRegion[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = split[0][i];
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
        if (isDead() || dying) {
            velocity.set(0f, 0f);
            isChasing = false;
            movingForAnimation = false;
            movingEastForAnimation = false;
            return;
        }

        if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - deltaTime);
            punchTime += deltaTime;
        }

        if (!aggro) {
            isChasing = false;
            velocity.set(0f, 0f);
            movingForAnimation = false;
            movingEastForAnimation = false;
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
            movingEastForAnimation = velocity.x > FACE_FLIP_THRESHOLD;
            walkTime += deltaTime;
        } else {
            velocity.set(0f, 0f);
            movingForAnimation = false;
            movingEastForAnimation = false;
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
            movingEastForAnimation = velocity.x > FACE_FLIP_THRESHOLD;
            walkTime += deltaTime;
            if (Math.abs(velocity.x) > FACE_FLIP_THRESHOLD) {
                facingLeft = velocity.x < 0f;
            }
        } else {
            position.set(homePosition);
            velocity.set(0f, 0f);
            movingForAnimation = false;
            movingEastForAnimation = false;
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

        if (idleAnimation == null || walkAnimation == null || punchAnimation == null || deathAnimation == null) {
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(fallbackTexture, position.x - size / 2f, position.y - size / 2f, size, size);
            return;
        }

        float delta = Gdx.graphics.getDeltaTime();
        Animation<TextureRegion> currentAnimation;
        float currentTime;
        boolean looping;
        boolean flipHorizontal = false;

        if (dying || isDead()) {
            deathTime += delta;
            currentAnimation = deathAnimation;
            currentTime = deathTime;
            looping = false;
        } else if (attackTimer > 0f) {
            attackTimer = Math.max(0f, attackTimer - delta);
            punchTime += delta;
            currentAnimation = punchAnimation;
            currentTime = punchTime;
            looping = false;
            flipHorizontal = facingLeft;
        } else if (movingForAnimation) {
            walkTime += delta;
            currentAnimation = walkAnimation;
            currentTime = walkTime;
            looping = true;
            flipHorizontal = movingEastForAnimation;
        } else {
            idleTime += delta;
            currentAnimation = idleAnimation;
            currentTime = idleTime;
            looping = true;
        }

        TextureRegion currentFrame = currentAnimation.getKeyFrame(currentTime, looping);

        float drawX = position.x - RENDER_SIZE / 2f;
        float drawY = position.y - RENDER_SIZE / 2f;

        if (flipHorizontal) {
            batch.draw(currentFrame, drawX + RENDER_SIZE, drawY, -RENDER_SIZE, RENDER_SIZE);
        } else {
            batch.draw(currentFrame, drawX, drawY, RENDER_SIZE, RENDER_SIZE);
        }
    }

    @Override
    public void takeDamage(float damage) {
        if (damage <= 0f) {
            return;
        }
        aggro = true;
        super.takeDamage(damage);
        if (isDead()) {
            velocity.set(0f, 0f);
            isChasing = false;
            movingForAnimation = false;
            movingEastForAnimation = false;
            aggro = false;
            dying = true;
            deathTime = 0f;
        }
    }

    @Override
    public void onAttack() {
        attackTimer = Math.max(attackTimer, ATTACK_ANIM_DURATION);
        punchTime = 0f;
    }

    @Override
    public void onAttack(float targetX, float targetY) {
        facingLeft = targetX < position.x;
        onAttack();
    }

    @Override
    public void onDeath() {
        currentHealth = 0f;
        dying = true;
        deathTime = 0f;
        attackTimer = 0f;
        velocity.set(0f, 0f);
        isChasing = false;
        movingForAnimation = false;
        movingEastForAnimation = false;
        aggro = false;
    }

    @Override
    public boolean shouldKeepRenderingWithoutSnapshot() {
        ensureTexturesLoaded();
        return dying && deathAnimation != null && deathTime < deathAnimation.getAnimationDuration();
    }

    @Override
    public void applyNetworkState(float x, float y, boolean chasing, float size) {
        if (hasNetworkPos) {
            float dx = x - lastNetworkX;
            float dy = y - lastNetworkY;
            movingForAnimation = (Math.abs(dx) + Math.abs(dy)) > 0.1f;
            if (Math.abs(dx) > 0.1f) {
                facingLeft = dx < 0f;
                movingEastForAnimation = dx > 0f;
            } else if (!movingForAnimation) {
                movingEastForAnimation = false;
            }
        } else {
            movingForAnimation = false;
            movingEastForAnimation = false;
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
        if (idleTexture != null) {
            idleTexture.dispose();
            idleTexture = null;
        }
        if (walkTexture != null) {
            walkTexture.dispose();
            walkTexture = null;
        }
        if (punchTexture != null) {
            punchTexture.dispose();
            punchTexture = null;
        }
        if (deathTexture != null) {
            deathTexture.dispose();
            deathTexture = null;
        }
        if (fallbackTexture != null) {
            fallbackTexture.dispose();
            fallbackTexture = null;
        }
        idleAnimation = null;
        walkAnimation = null;
        punchAnimation = null;
        deathAnimation = null;
    }
}

