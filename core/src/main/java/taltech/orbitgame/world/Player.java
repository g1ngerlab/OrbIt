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

public class Player {
    private static final float NAME_Y_OFFSET = 40f;

    private final CharacterType characterType;
    private final Texture runSideTexture;
    private final Texture runUpTexture;
    private final Texture runDownTexture;
    private final Texture idleTexture;
    private final Texture attackTexture;
    private final Texture punchTexture;

    private final Animation<TextureRegion> runSideAnimation;
    private final Animation<TextureRegion> runUpAnimation;
    private final Animation<TextureRegion> runDownAnimation;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> attackAnimation;
    private final Animation<TextureRegion> punchAnimation;

    private float runTime = 0f;
    private float idleTime = 0f;
    private float attackTime = 0f;
    private float punchTime = 0f;
    private boolean attacking = false;
    private boolean punching = false;

    private Direction direction = Direction.DOWN;
    private boolean moving = false;

    public float x = 100;
    public float y = 100;
    private float aimX;

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
    private int punchFrameWidth;
    private int punchFrameHeight;

    private int colliderWidth;
    private int colliderHeight;

    // Health system
    private float maxHealth;
    private float currentHealth;

    // Kill count tracking
    private int killCount = 0;

    private String playerName = "Player";
    private final BitmapFont nameFont;
    private final GlyphLayout nameLayout;

    public Player() {
        this(CharacterType.MATH);
    }

    public Player(CharacterType characterType) {
        this.characterType = characterType == null ? CharacterType.MATH : characterType;

        runSideTexture = new Texture(Gdx.files.internal(this.characterType.getRunSideTexturePath()));
        runUpTexture = new Texture(Gdx.files.internal(this.characterType.getRunUpTexturePath()));
        runDownTexture = new Texture(Gdx.files.internal(this.characterType.getRunDownTexturePath()));
        idleTexture = new Texture(Gdx.files.internal(this.characterType.getIdleTexturePath()));
        attackTexture = this.characterType.hasAttackAnimation()
            ? new Texture(Gdx.files.internal(this.characterType.getAttackTexturePath()))
            : null;
        punchTexture = this.characterType.hasPunchAnimation()
            ? new Texture(Gdx.files.internal(this.characterType.getPunchTexturePath()))
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
        if (punchTexture != null) {
            punchFrameWidth = punchTexture.getWidth() / this.characterType.getPunchFrames();
            punchFrameHeight = punchTexture.getHeight();
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
        if (punchTexture != null) {
            punchAnimation = createAnimation(punchTexture, punchFrameWidth, punchFrameHeight,
                this.characterType.getPunchFrames(), this.characterType.getPunchFrameTime());
        } else {
            punchAnimation = null;
        }

        // Initialize health
        this.maxHealth = 100f;
        this.currentHealth = 100f;
        this.nameFont = FontManager.getPixelFont(12);
        this.nameLayout = new GlyphLayout();
        this.aimX = getCenterX() + 1f;
    }

    public void update(float delta, boolean moving) {
        this.moving = moving;
        if (attacking) {
            attackTime += delta;
            if (attackAnimation != null && attackTime >= attackAnimation.getAnimationDuration()) {
                attacking = false;
            }
        }
        if (punching) {
            punchTime += delta;
            if (punchAnimation != null && punchTime >= punchAnimation.getAnimationDuration()) {
                punching = false;
            }
        }
        if (moving) {
            runTime += delta;
        } else {
            idleTime += delta;
        }
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

        drawName(batch);
    }

    public void dispose() {
        runSideTexture.dispose();
        runUpTexture.dispose();
        runDownTexture.dispose();
        idleTexture.dispose();
        if (attackTexture != null) {
            attackTexture.dispose();
        }
        if (punchTexture != null) {
            punchTexture.dispose();
        }
        nameFont.dispose();
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

    public void setAimX(float aimX) {
        this.aimX = aimX;
    }

    public float getAimX() {
        return aimX;
    }

    public void setDirection(Direction direction) {
        if (direction != null) {
            this.direction = direction;
        }
    }

    public Direction getDirection() {
        return direction;
    }

    public void setPlayerName(String playerName) {
        String normalized = playerName == null ? "" : playerName.trim();
        if (normalized.isEmpty()) {
            this.playerName = "Player";
            return;
        }
        this.playerName = normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    public String getPlayerName() {
        return playerName;
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

    public void triggerPunch() {
        if (punchAnimation == null) {
            return;
        }
        punching = true;
        punchTime = 0f;
    }

    // Health management methods

    /**
     * Deals damage to this player
     * @param damage Amount of damage to deal
     */
    public void takeDamage(float damage) {
        currentHealth = Math.max(0f, currentHealth - damage);
    }

    /**
     * Heals this player
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
     * @return True if this player is dead (health <= 0)
     */
    public boolean isDead() {
        return currentHealth <= 0f;
    }

    /**
     * @return True if this player is alive (health > 0)
     */
    public boolean isAlive() {
        return currentHealth > 0f;
    }

    // Kill count methods
    /**
     * Increments kill count by 1
     */
    public void addKill() {
        killCount++;
    }

    /**
     * @return Current kill count
     */
    public int getKillCount() {
        return killCount;
    }

    /**
     * Resets kill count to 0
     */
    public void resetKillCount() {
        killCount = 0;
    }

    /**
     * Resets player state for a new match
     */
    public void resetForNewMatch() {
        this.maxHealth = 100f;
        this.currentHealth = 100f;
        this.killCount = 0;
        this.attacking = false;
        this.punching = false;
        this.moving = false;
        this.runTime = 0f;
        this.idleTime = 0f;
        this.attackTime = 0f;
        this.punchTime = 0f;
    }

    private void drawName(SpriteBatch batch) {
        nameLayout.setText(nameFont, playerName);
        float nameX = x + colliderWidth * 0.5f - nameLayout.width * 0.5f;
        float nameY = y + colliderHeight + NAME_Y_OFFSET;
        nameFont.draw(batch, playerName, nameX, nameY);
    }

    private Animation<TextureRegion> resolveAnimation() {
        if (punching && punchAnimation != null) {
            return punchAnimation;
        }
        if (attacking && attackAnimation != null) {
            return attackAnimation;
        }
        if (isMovingDirection()) {
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

    private boolean isMovingDirection() {
        return moving;
    }

    private int resolveFrameWidth(Animation<TextureRegion> animation) {
        if (animation == attackAnimation) {
            return attackFrameWidth;
        }
        if (animation == punchAnimation) {
            return punchFrameWidth;
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
        if (animation == punchAnimation) {
            return punchFrameHeight;
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
        if (animation == punchAnimation) {
            return punchTime;
        }
        return moving ? runTime : idleTime;
    }

    private boolean shouldFlipForDirection(Animation<TextureRegion> animation) {
        if (animation != runSideAnimation && animation != attackAnimation && animation != punchAnimation) {
            return false;
        }
        if (animation == attackAnimation || animation == punchAnimation) {
            boolean aimRight = aimX >= getCenterX();
            boolean flip = aimRight ? characterType.isBaseFacingLeft() : !characterType.isBaseFacingLeft();
            if (animation == punchAnimation) {
                return characterType.isPunchFlipInverted() ? !flip : flip;
            }
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
