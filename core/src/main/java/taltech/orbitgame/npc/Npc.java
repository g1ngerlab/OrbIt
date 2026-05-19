package taltech.orbitgame.npc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public abstract class Npc {
    protected final int id;
    protected Vector2 position;
    protected Vector2 velocity;
    protected float speed;
    protected float detectionRange;
    protected float size;
    protected boolean isChasing;

    // Health system
    protected float maxHealth;
    protected float currentHealth;

    public Npc(int id, float x, float y, float maxHealth) {
        this.id = id;
        this.position = new Vector2(x, y);
        this.velocity = new Vector2(0, 0);
        this.isChasing = false;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public abstract void update(float deltaTime, List<Vector2> playerPositions);

    public abstract void draw(SpriteBatch batch);

    public void dispose() {
        // Override in subclasses if needed
    }

    public void onAttack() {
        // Override in subclasses if needed
    }

    public void onAttack(float targetX, float targetY) {
        onAttack();
    }

    public void onDeath() {
        // Override in subclasses if needed
    }

    public boolean shouldKeepRenderingWithoutSnapshot() {
        return false;
    }

    public int getId() {
        return id;
    }

    public Vector2 getPosition() {
        return position;
    }

    public boolean isChasing() {
        return isChasing;
    }

    public float getSize() {
        return size;
    }

    public void applyNetworkState(float x, float y, boolean chasing, float size) {
        this.position.set(x, y);
        this.velocity.set(0f, 0f);
        this.isChasing = chasing;
        this.size = size;
    }

    /**
     * Syncs health from the server's authoritative state.
     * Called on the client side when a snapshot is received.
     * @param health    current health from server
     * @param maxHealth max health from server
     */
    public void applyHealthState(float health, float maxHealth) {
        this.currentHealth = health;
        this.maxHealth = maxHealth;
    }

    // Health management methods

    /**
     * Deals damage to this NPC
     * @param damage Amount of damage to deal
     */
    public void takeDamage(float damage) {
        currentHealth = Math.max(0f, currentHealth - damage);
    }

    /**
     * Heals this NPC
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
     * @return True if this NPC is dead (health <= 0)
     */
    public boolean isDead() {
        return currentHealth <= 0f;
    }

    /**
     * @return True if this NPC is alive (health > 0)
     */
    public boolean isAlive() {
        return currentHealth > 0f;
    }
}
