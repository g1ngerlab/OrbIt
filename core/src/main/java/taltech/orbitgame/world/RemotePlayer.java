package taltech.orbitgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Texture;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.util.FontManager;

/**
 * Represents a remote player (other players in the lobby)
 */
public class RemotePlayer {
    private static final float NAME_Y_OFFSET = 40f;

    private final CharacterType characterType;
    private final Texture runSideTexture;
    private final Texture runUpTexture;
    private final Texture runDownTexture;
    private final Texture idleTexture;
    private final Texture attackTexture;

    private final Animation<TextureRegion> runSideAnimation;
    private final Animation<TextureRegion> runUpAnimation;
    private final Animation<TextureRegion> runDownAnimation;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> attackAnimation;

    private float runTime = 0f;
    private float idleTime = 0f;
    private float attackTime = 0f;
    private boolean attacking = false;

    private Direction direction = Direction.DOWN;
    private boolean moving = false;

    public float x = 100;
    public float y = 100;
    public int clientId;
    public String playerName;

    private int sideFrameWidth;
    private int sideFrameHeight;
    private int upFrameWidth;
    private int upFrameHeight;
    private int downFrameWidth;
    private int downFrameHeight;
    private int idleFrameWidth;
    private int idleFrameHeight;
    private int attackFrameWidth;
    private int attackFrameHeight;

    private int colliderWidth;
    private int colliderHeight;

    private float lastX;
    private float lastY;

    private float aimX;
    private float aimY;

    private BitmapFont font;
    private GlyphLayout nameLayout;

    // Health system
    private float maxHealth;
    private float currentHealth;

    public RemotePlayer(int clientId, String playerName, float x, float y) {
        this(clientId, playerName, x, y, CharacterType.MATH);
    }

    public RemotePlayer(int clientId, String playerName, float x, float y, CharacterType characterType) {
        this.clientId = clientId;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.lastX = x;
        this.lastY = y;
        this.characterType = characterType == null ? CharacterType.MATH : characterType;

        runSideTexture = new Texture(Gdx.files.internal(this.characterType.getRunSideTexturePath()));
        runUpTexture = new Texture(Gdx.files.internal(this.characterType.getRunUpTexturePath()));
        runDownTexture = new Texture(Gdx.files.internal(this.characterType.getRunDownTexturePath()));
        idleTexture = new Texture(Gdx.files.internal(this.characterType.getIdleTexturePath()));
        attackTexture = this.characterType.hasAttackAnimation()
            ? new Texture(Gdx.files.internal(this.characterType.getAttackTexturePath()))
            : null;

        sideFrameWidth = runSideTexture.getWidth() / this.characterType.getRunSideFrames();
        sideFrameHeight = runSideTexture.getHeight();
        upFrameWidth = runUpTexture.getWidth() / this.characterType.getRunUpFrames();
        upFrameHeight = runUpTexture.getHeight();
        downFrameWidth = runDownTexture.getWidth() / this.characterType.getRunDownFrames();
        downFrameHeight = runDownTexture.getHeight();
        idleFrameWidth = idleTexture.getWidth() / this.characterType.getIdleFrames();
        idleFrameHeight = idleTexture.getHeight();
        if (attackTexture != null) {
            attackFrameWidth = attackTexture.getWidth() / this.characterType.getAttackFrames();
            attackFrameHeight = attackTexture.getHeight();
        }

        colliderWidth = 32;
        colliderHeight = 48;

        runSideAnimation = createAnimation(runSideTexture, sideFrameWidth, sideFrameHeight,
            this.characterType.getRunSideFrames(), this.characterType.getRunFrameTime());
        runUpAnimation = createAnimation(runUpTexture, upFrameWidth, upFrameHeight,
            this.characterType.getRunUpFrames(), this.characterType.getRunFrameTime());
        runDownAnimation = createAnimation(runDownTexture, downFrameWidth, downFrameHeight,
            this.characterType.getRunDownFrames(), this.characterType.getRunFrameTime());
        idleAnimation = createAnimation(idleTexture, idleFrameWidth, idleFrameHeight,
            this.characterType.getIdleFrames(), this.characterType.getIdleFrameTime());
        if (attackTexture != null) {
            attackAnimation = createAnimation(attackTexture, attackFrameWidth, attackFrameHeight,
                this.characterType.getAttackFrames(), this.characterType.getAttackFrameTime());
        } else {
            attackAnimation = null;
        }

        font = FontManager.getPixelFont(12);
        font.setColor(Color.YELLOW);
        nameLayout = new GlyphLayout();

        // Initialize health
        this.maxHealth = 100f;
        this.currentHealth = 100f;

        this.aimX = getCenterX() + 1f;
        this.aimY = getCenterY();
    }

    public void update(float delta) {
        if (attacking) {
            attackTime += delta;
            if (attackAnimation != null && attackTime >= attackAnimation.getAnimationDuration()) {
                attacking = false;
            }
        }
        if (moving) {
            runTime += delta;
        } else {
            idleTime += delta;
        }
    }

    public void updatePosition(float x, float y, boolean moving, float aimX, float aimY) {
        float dx = x - this.lastX;
        float dy = y - this.lastY;
        this.moving = moving;
        if (this.moving) {
            updateDirectionFromDelta(dx, dy);
        }
        this.lastX = x;
        this.lastY = y;
        this.x = x;
        this.y = y;
        this.aimX = aimX;
        this.aimY = aimY;
        update(0);
    }

    public void render(SpriteBatch batch) {
        Animation<TextureRegion> animation = resolveAnimation();
        float time = resolveAnimationTime(animation);
        TextureRegion currentFrame = animation.getKeyFrame(time, true);
        int drawWidth = Math.round(resolveFrameWidth(animation) * characterType.getDrawScale());
        int drawHeight = Math.round(resolveFrameHeight(animation) * characterType.getDrawScale());

        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, 1f);
        float drawX = x + (colliderWidth - drawWidth) * 0.5f;
        float drawY = y;
        if (shouldFlipForDirection(animation)) {
            batch.draw(currentFrame, drawX + drawWidth, drawY, -drawWidth, drawHeight);
        } else {
            batch.draw(currentFrame, drawX, drawY, drawWidth, drawHeight);
        }
        batch.setColor(prev);

        // Draw player name above the player
        nameLayout.setText(font, playerName);
        float nameX = x + colliderWidth / 2f - nameLayout.width / 2f;
        float nameY = y + colliderHeight + NAME_Y_OFFSET;
        font.draw(batch, playerName, nameX, nameY);
    }

    public void dispose() {
        runSideTexture.dispose();
        runUpTexture.dispose();
        runDownTexture.dispose();
        idleTexture.dispose();
        if (attackTexture != null) {
            attackTexture.dispose();
        }
        font.dispose();
    }

    public int getWidth() {
        return colliderWidth;
    }

    public int getHeight() {
        return colliderHeight;
    }

    public float getCenterX() {
        return x + colliderWidth * 0.5f;
    }

    public float getCenterY() {
        return y + colliderHeight * 0.5f;
    }

    public float getAimX() {
        return aimX;
    }

    public float getAimY() {
        return aimY;
    }

    // Health management methods

    /**
     * Deals damage to this remote player
     * @param damage Amount of damage to deal
     */
    public void takeDamage(float damage) {
        currentHealth = Math.max(0f, currentHealth - damage);
    }

    /**
     * Heals this remote player
     * @param amount Amount to heal
     */
    public void heal(float amount) {
        currentHealth = Math.min(maxHealth, currentHealth + amount);
    }

    /**
     * @return Current health points
     */
    public float getHealth() {
        return currentHealth;
    }

    /**
     * @return Maximum health points
     */
    public float getMaxHealth() {
        return maxHealth;
    }

    /**
     * @return Health as a percentage (0.0 to 1.0)
     */
    public float getHealthPercentage() {
        return maxHealth > 0 ? currentHealth / maxHealth : 0f;
    }

    /**
     * Sets health values from the server's authoritative state.
     * @param health    current health
     * @param maxHealth maximum health
     */
    public void setHealth(float health, float maxHealth) {
        this.currentHealth = Math.max(0f, health);
        this.maxHealth = Math.max(1f, maxHealth);
    }

    /**
     * @return True if this remote player is dead (health <= 0)
     */
    public boolean isDead() {
        return currentHealth <= 0f;
    }

    /**
     * @return True if this remote player is alive (health > 0)
     */
    public boolean isAlive() {
        return currentHealth > 0f;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }

    public void triggerAttack() {
        if (attackAnimation == null) {
            return;
        }
        attacking = true;
        attackTime = 0f;
    }

    private Animation<TextureRegion> resolveAnimation() {
        if (attacking && attackAnimation != null) {
            return attackAnimation;
        }
        if (moving) {
            switch (direction) {
                case UP:
                    return runUpAnimation;
                case DOWN:
                    return runDownAnimation;
                case LEFT:
                case RIGHT:
                default:
                    return runSideAnimation;
            }
        }
        return idleAnimation;
    }

    private int resolveFrameWidth(Animation<TextureRegion> animation) {
        if (animation == attackAnimation) {
            return attackFrameWidth;
        }
        if (animation == runUpAnimation) {
            return upFrameWidth;
        }
        if (animation == runDownAnimation) {
            return downFrameWidth;
        }
        if (animation == idleAnimation) {
            return idleFrameWidth;
        }
        return sideFrameWidth;
    }

    private int resolveFrameHeight(Animation<TextureRegion> animation) {
        if (animation == attackAnimation) {
            return attackFrameHeight;
        }
        if (animation == runUpAnimation) {
            return upFrameHeight;
        }
        if (animation == runDownAnimation) {
            return downFrameHeight;
        }
        if (animation == idleAnimation) {
            return idleFrameHeight;
        }
        return sideFrameHeight;
    }

    private float resolveAnimationTime(Animation<TextureRegion> animation) {
        if (animation == attackAnimation) {
            return attackTime;
        }
        return moving ? runTime : idleTime;
    }

    private boolean shouldFlipForDirection(Animation<TextureRegion> animation) {
        if (animation != runSideAnimation && animation != attackAnimation) {
            return false;
        }
        if (animation == attackAnimation) {
            boolean aimRight = aimX >= getCenterX();
            boolean flip = aimRight ? characterType.isBaseFacingLeft() : !characterType.isBaseFacingLeft();
            return characterType.isAttackFlipInverted() ? !flip : flip;
        }
        if (direction == Direction.RIGHT) {
            return characterType.isBaseFacingLeft();
        }
        if (direction == Direction.LEFT) {
            return !characterType.isBaseFacingLeft();
        }
        return false;
    }

    private void updateDirectionFromDelta(float dx, float dy) {
        float absX = Math.abs(dx);
        float absY = Math.abs(dy);
        if (absX < 0.01f && absY < 0.01f) {
            return;
        }
        if (absX >= absY) {
            direction = dx >= 0f ? Direction.RIGHT : Direction.LEFT;
        } else {
            direction = dy >= 0f ? Direction.UP : Direction.DOWN;
        }
    }

    private static Animation<TextureRegion> createAnimation(Texture texture, int frameWidth, int frameHeight,
                                                            int frames, float frameTime) {
        TextureRegion[][] tmp = TextureRegion.split(texture, frameWidth, frameHeight);
        TextureRegion[] regions = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            regions[i] = tmp[0][i];
        }
        return new Animation<>(frameTime, regions);
    }
}
