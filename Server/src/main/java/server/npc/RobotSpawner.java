package server.npc;

import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.world.MapCollisionGrid;

import java.util.Random;

public class RobotSpawner {
    private final int robotCount;
    private final float territoryRadius;
    private final float minSpawnDistance;

    public RobotSpawner(int robotCount, float territoryRadius, float minSpawnDistance) {
        this.robotCount = robotCount;
        this.territoryRadius = territoryRadius;
        this.minSpawnDistance = minSpawnDistance;
    }

    public int spawnInitial(NpcManager npcManager, MapCollisionGrid collisionGrid, Random random) {
        int spawned = 0;
        int maxAttempts = robotCount * 50;

        for (int attempt = 0; attempt < maxAttempts && spawned < robotCount; attempt++) {
            float[] pos = collisionGrid.findRandomOpenPosition(random);
            if (!isFarEnoughFromOtherRobots(npcManager, pos[0], pos[1])) {
                continue;
            }
            npcManager.spawnRobotButler(pos[0], pos[1], territoryRadius);
            spawned++;
        }

        while (spawned < robotCount) {
            float[] pos = collisionGrid.findRandomOpenPosition(random);
            npcManager.spawnRobotButler(pos[0], pos[1], territoryRadius);
            spawned++;
        }

        return spawned;
    }

    private boolean isFarEnoughFromOtherRobots(NpcManager npcManager, float x, float y) {
        float minDistSquared = minSpawnDistance * minSpawnDistance;
        for (Npc npc : npcManager.getNpcs()) {
            if (npc.getPosition().dst2(x, y) < minDistSquared) {
                return false;
            }
        }
        return true;
    }
}


