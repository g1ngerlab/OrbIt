package server.model;

/**
 * Server-side state for a dropped orb.
 */
public class ServerOrb {
    private final int orbId;
    private float x;
    private float y;
    private final float radius;
    private float velocityX;
    private float velocityY;
    private boolean settled;

    public ServerOrb(int orbId, float x, float y, float radius, float velocityX, float velocityY) {
        this.orbId = orbId;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.settled = false;
    }

    public int getOrbId() {
        return orbId;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getRadius() {
        return radius;
    }

    public float getVelocityX() {
        return velocityX;
    }

    public float getVelocityY() {
        return velocityY;
    }

    public void setVelocity(float velocityX, float velocityY) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean isSettled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}

