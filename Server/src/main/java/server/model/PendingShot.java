package server.model;

public class PendingShot {
    private final int ownerClientId;
    private final float targetX;
    private final float targetY;

    public PendingShot(int ownerClientId, float targetX, float targetY) {
        this.ownerClientId = ownerClientId;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    public int getOwnerClientId() {
        return ownerClientId;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }
}

