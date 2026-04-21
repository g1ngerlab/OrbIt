package server;

import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.world.MapCollisionGrid;
import server.model.ServerOrb;

import java.util.Map;
import java.util.Random;

/**
 * Manages spawning and physics of orbs and other droppable items in the world.
 */
public class LobbyItemDropManager {
    private static final int ORB_DROP_MIN = 3;
    private static final int ORB_DROP_MAX = 7;
    private static final float ORB_DROP_MIN_DISTANCE = 18f;
    private static final float ORB_DROP_MAX_DISTANCE = 54f;
    private static final float ORB_RADIUS = 16f;
    private static final float ORB_INITIAL_SPEED_MIN = 70f;
    private static final float ORB_INITIAL_SPEED_MAX = 150f;
    private static final float ORB_DRAG_PER_SECOND = 3.2f;
    private static final float ORB_BOUNCE_DAMPING = 0.55f;
    private static final float ORB_SETTLE_SPEED = 12f;

    private final Map<Integer, ServerOrb> orbs;
    private final MapCollisionGrid collisionGrid;
    private final Random random;
    private int nextOrbId;

    public LobbyItemDropManager(Map<Integer, ServerOrb> orbs, MapCollisionGrid collisionGrid, Random random, int initialOrbId) {
        this.orbs = orbs;
        this.collisionGrid = collisionGrid;
        this.random = random;
        this.nextOrbId = initialOrbId;
    }

    public void spawnOrbsAroundNpc(Npc npc) {
        int dropCount = ORB_DROP_MIN + random.nextInt(ORB_DROP_MAX - ORB_DROP_MIN + 1);
        spawnItemsAroundPoint(
            ItemKind.ORB.getId(),
            npc.getPosition().x,
            npc.getPosition().y,
            dropCount,
            ORB_DROP_MIN_DISTANCE,
            ORB_DROP_MAX_DISTANCE
        );
    }

    public void spawnOrbsAroundPoint(float centerX, float centerY, int dropCount, float minDistance, float maxDistance) {
        spawnItemsAroundPoint(ItemKind.ORB.getId(), centerX, centerY, dropCount, minDistance, maxDistance);
    }

    public void spawnItemsAroundPoint(int itemTypeId,
                                      float centerX,
                                      float centerY,
                                      int dropCount,
                                      float minDistance,
                                      float maxDistance) {
        for (int i = 0; i < dropCount; i++) {
            float x = centerX;
            float y = centerY;
            boolean placed = false;

            for (int attempt = 0; attempt < 10; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0;
                float distance = minDistance + random.nextFloat() * (maxDistance - minDistance);
                float candidateX = centerX + (float) Math.cos(angle) * distance;
                float candidateY = centerY + (float) Math.sin(angle) * distance;
                if (!collisionGrid.isBlocked(candidateX, candidateY)) {
                    x = candidateX;
                    y = candidateY;
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                x = centerX;
                y = centerY;
            }

            double angle = random.nextDouble() * Math.PI * 2.0;
            float initialSpeed = ORB_INITIAL_SPEED_MIN
                + random.nextFloat() * (ORB_INITIAL_SPEED_MAX - ORB_INITIAL_SPEED_MIN);
            float velocityX = (float) Math.cos(angle) * initialSpeed;
            float velocityY = (float) Math.sin(angle) * initialSpeed;

            int orbId = nextOrbId++;
            ServerOrb orb = new ServerOrb(orbId, itemTypeId, x, y, ORB_RADIUS, velocityX, velocityY);
            orbs.put(orbId, orb);
        }
    }

    public void updateOrbsPhysics(float deltaTime) {
        float dragFactor = Math.max(0f, 1f - ORB_DRAG_PER_SECOND * deltaTime);

        for (ServerOrb orb : orbs.values()) {
            if (orb.isSettled()) {
                continue;
            }

            float vx = orb.getVelocityX() * dragFactor;
            float vy = orb.getVelocityY() * dragFactor;

            float nextX = orb.getX() + vx * deltaTime;
            float nextY = orb.getY() + vy * deltaTime;

            boolean movedX = false;
            boolean movedY = false;

            if (!collisionGrid.isBlocked(nextX, orb.getY())) {
                orb.setPosition(nextX, orb.getY());
                movedX = true;
            } else {
                vx = -vx * ORB_BOUNCE_DAMPING;
            }

            if (!collisionGrid.isBlocked(orb.getX(), nextY)) {
                orb.setPosition(orb.getX(), nextY);
                movedY = true;
            } else {
                vy = -vy * ORB_BOUNCE_DAMPING;
            }

            if (!movedX && !movedY) {
                vx *= 0.5f;
                vy *= 0.5f;
            }

            orb.setVelocity(vx, vy);
            if (vx * vx + vy * vy < ORB_SETTLE_SPEED * ORB_SETTLE_SPEED) {
                orb.setVelocity(0f, 0f);
                orb.setSettled(true);
            }
        }
    }

    public int getNextOrbId() {
        return nextOrbId;
    }

    public void setNextOrbId(int nextOrbId) {
        this.nextOrbId = nextOrbId;
    }
}

