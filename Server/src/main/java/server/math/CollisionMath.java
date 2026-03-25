package server.math;

public final class CollisionMath {
    private CollisionMath() {
    }

    public static boolean rectsOverlap(float ax, float ay, float aw, float ah,
                                       float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx
            && ay < by + bh && ay + ah > by;
    }

    public static boolean circleIntersectsRect(float cx, float cy, float radius,
                                               float rx, float ry, float rw, float rh) {
        float nearestX = Math.max(rx, Math.min(cx, rx + rw));
        float nearestY = Math.max(ry, Math.min(cy, ry + rh));
        float dx = cx - nearestX;
        float dy = cy - nearestY;
        return dx * dx + dy * dy <= radius * radius;
    }
}

