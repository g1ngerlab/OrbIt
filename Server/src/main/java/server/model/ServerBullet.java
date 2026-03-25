package server.model;

import com.badlogic.gdx.math.Vector2;

public class ServerBullet {
    private final int bulletId;
    private final int ownerClientId;
    private final Vector2 position;
    private final Vector2 velocity;
    private float timeRemaining;

    public ServerBullet(int bulletId, int ownerClientId, Vector2 position, Vector2 velocity, float timeRemaining) {
        this.bulletId = bulletId;
        this.ownerClientId = ownerClientId;
        this.position = position;
        this.velocity = velocity;
        this.timeRemaining = timeRemaining;
    }

    public int getBulletId() {
        return bulletId;
    }

    public int getOwnerClientId() {
        return ownerClientId;
    }

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getVelocity() {
        return velocity;
    }

    public float getTimeRemaining() {
        return timeRemaining;
    }

    public void tickLifetime(float deltaTime) {
        timeRemaining -= deltaTime;
    }
}

