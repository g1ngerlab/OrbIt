package taltech.orbitgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Texture;
import taltech.orbitgame.util.FontManager;

public class Player {

    private static final int RUN_SIDE_FRAMES = 7;
    private static final int RUN_UP_FRAMES = 6;
    private static final int RUN_DOWN_FRAMES = 8;
    private static final int IDLE_FRAMES = 10;

    private static final float RUN_FRAME_TIME = 0.07f;
    private static final float IDLE_FRAME_TIME = 0.09f;
    private static final float DRAW_SCALE = 1f / 1.5f;
    private static final float NAME_Y_OFFSET = 40f;

    private final Texture runSideTexture;
    private final Texture runUpTexture;
    private final Texture runDownTexture;
    private final Texture idleTexture;

    private final Animation<TextureRegion> runSideAnimation;
    private final Animation<TextureRegion> runUpAnimation;
    private final Animation<TextureRegion> runDownAnimation;
    private final Animation<TextureRegion> idleAnimation;

    private float runTime = 0f;
    private float idleTime = 0f;

    private Direction direction = Direction.DOWN;
    private boolean moving = false;

    public float x = 100;
    public float y = 100;

    private int sideFrameWidth;
    private int sideFrameHeight;
    private int upFrameWidth;
    private int upFrameHeight;
    private int downFrameWidth;
    private int downFrameHeight;
    private int idleFrameWidth;
    private int idleFrameHeight;

    private int colliderWidth;
    private int colliderHeight;

    // Health system
    private float maxHealth;
    private float currentHealth;

    private String playerName = "Player";
    private final BitmapFont nameFont;
    private final GlyphLayout nameLayout;

    public Player() {

        runSideTexture = new Texture(Gdx.files.internal("player/run_side.png"));
        runUpTexture = new Texture(Gdx.files.internal("player/run_up.png"));
        runDownTexture = new Texture(Gdx.files.internal("player/run_down.png"));
        idleTexture = new Texture(Gdx.files.internal("player/without_run.png"));

        sideFrameWidth = runSideTexture.getWidth() / RUN_SIDE_FRAMES;
        sideFrameHeight = runSideTexture.getHeight();
        upFrameWidth = runUpTexture.getWidth() / RUN_UP_FRAMES;
        upFrameHeight = runUpTexture.getHeight();
        downFrameWidth = runDownTexture.getWidth() / RUN_DOWN_FRAMES;
        downFrameHeight = runDownTexture.getHeight();
        idleFrameWidth = idleTexture.getWidth() / IDLE_FRAMES;
        idleFrameHeight = idleTexture.getHeight();

        colliderWidth = 32;
        colliderHeight = 48;

        runSideAnimation = createAnimation(runSideTexture, sideFrameWidth, sideFrameHeight, RUN_SIDE_FRAMES, RUN_FRAME_TIME);
        runUpAnimation = createAnimation(runUpTexture, upFrameWidth, upFrameHeight, RUN_UP_FRAMES, RUN_FRAME_TIME);
        runDownAnimation = createAnimation(runDownTexture, downFrameWidth, downFrameHeight, RUN_DOWN_FRAMES, RUN_FRAME_TIME);
        idleAnimation = createAnimation(idleTexture, idleFrameWidth, idleFrameHeight, IDLE_FRAMES, IDLE_FRAME_TIME);

        // Initialize health
        this.maxHealth = 100f;
        this.currentHealth = 100f;
        this.nameFont = FontManager.getPixelFont(12);
        this.nameLayout = new GlyphLayout();
    }

    public void update(float delta, boolean moving) {
        this.moving = moving;
        if (moving) {
            runTime += delta;
        } else {
            idleTime += delta;
        }
    }

    public void render(SpriteBatch batch) {
        Animation<TextureRegion> animation = resolveAnimation();
        TextureRegion currentFrame = animation.getKeyFrame(moving ? runTime : idleTime, true);
        int drawWidth = Math.round(resolveFrameWidth(animation) * DRAW_SCALE);
        int drawHeight = Math.round(resolveFrameHeight(animation) * DRAW_SCALE);

        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, 1f);
        float drawX = x + (colliderWidth - drawWidth) * 0.5f;
        float drawY = y;
        if (direction == Direction.RIGHT && animation == runSideAnimation) {
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

    private void drawName(SpriteBatch batch) {
        nameLayout.setText(nameFont, playerName);
        float nameX = x + colliderWidth * 0.5f - nameLayout.width * 0.5f;
        float nameY = y + colliderHeight + NAME_Y_OFFSET;
        nameFont.draw(batch, playerName, nameX, nameY);
    }

    private Animation<TextureRegion> resolveAnimation() {
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
