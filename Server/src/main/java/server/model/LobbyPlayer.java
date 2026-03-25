package server.model;

import com.esotericsoftware.kryonet.Connection;

public class LobbyPlayer {
    private final int clientId;
    private final String playerName;
    private final Connection connection;

    private final float maxHealth;
    private float currentHealth;
    private float meleeDamageCooldown;

    private float x;
    private float y;
    private float aimX;
    private float aimY;

    public LobbyPlayer(int clientId, String playerName, Connection connection, float spawnX, float spawnY,
                       float playerWidth, float playerHeight, float maxHealth) {
        this.clientId = clientId;
        this.playerName = playerName;
        this.connection = connection;
        this.x = spawnX;
        this.y = spawnY;
        this.aimX = spawnX + playerWidth;
        this.aimY = spawnY + playerHeight * 0.5f;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
    }

    public int getClientId() {
        return clientId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Connection getConnection() {
        return connection;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getAimX() {
        return aimX;
    }

    public float getAimY() {
        return aimY;
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public float getCurrentHealth() {
        return currentHealth;
    }

    public void setPositionAndAim(float x, float y, float aimX, float aimY) {
        this.x = x;
        this.y = y;
        this.aimX = aimX;
        this.aimY = aimY;
    }

    public void takeDamage(float damage) {
        currentHealth = Math.max(0f, currentHealth - damage);
    }

    public boolean isDead() {
        return currentHealth <= 0f;
    }

    public float getMeleeDamageCooldown() {
        return meleeDamageCooldown;
    }

    public void tickMeleeDamageCooldown(float deltaTime) {
        if (meleeDamageCooldown > 0f) {
            meleeDamageCooldown -= deltaTime;
        }
    }

    public void resetMeleeDamageCooldown(float cooldownSeconds) {
        meleeDamageCooldown = cooldownSeconds;
    }
}

