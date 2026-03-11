package taltech.orbitgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Player {

    private Texture texture;
    private Animation<TextureRegion> runAnimation;
    private float stateTime = 0f;

    public float x = 100;
    public float y = 100;

    private int frameWidth;
    private int frameHeight;
    private int drawWidth;
    private int drawHeight;
    private int colliderWidth;
    private int colliderHeight;

    // Health system
    private float maxHealth;
    private float currentHealth;

    public Player() {

        texture = new Texture(Gdx.files.internal("player/BadBoy.png"));

        frameWidth = texture.getWidth() / 16;
        frameHeight = texture.getHeight();
        drawWidth = frameWidth;
        drawHeight = frameHeight;
        colliderWidth = 32;
        colliderHeight = 48;

        TextureRegion[][] tmp = TextureRegion.split(texture, frameWidth, frameHeight);

        TextureRegion[] frames = new TextureRegion[16];

        for (int i = 0; i < 16; i++) {
            frames[i] = tmp[0][i];
        }

        runAnimation = new Animation<>(0.07f, frames);

        // Initialize health
        this.maxHealth = 100f;
        this.currentHealth = 100f;
    }

    public void update(float delta, boolean moving) {
        if (moving) {
            stateTime += delta;
        } else {
            stateTime = 0;
        }
    }

    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = runAnimation.getKeyFrame(stateTime, true);

        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, 1f);
        float drawX = x + (colliderWidth - drawWidth) * 0.5f;
        float drawY = y;
        batch.draw(currentFrame, drawX, drawY, drawWidth, drawHeight);
        batch.setColor(prev);
    }

    public void dispose() {
        texture.dispose();
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

}
