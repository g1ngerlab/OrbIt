package taltech.orbitgame.npc;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import taltech.orbitgame.network.Network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NpcManager {
    public interface CollisionChecker {
        boolean isBlocked(float worldX, float worldY);
    }

    private final Map<Integer, Npc> npcs;
    private int nextNpcId;

    public NpcManager() {
        this.npcs = new HashMap<>();
        this.nextNpcId = 1;
    }

    public int spawnRobotButler(float x, float y, float territoryRadius) {
        int id = nextNpcId++;
        npcs.put(id, new RobotButler(id, x, y, territoryRadius));
        return id;
    }

    public void spawnRobotsOnMap(int count, int mapWidth, int mapHeight) {
        for (int i = 0; i < count; i++) {
            float x = (float) (Math.random() * mapWidth);
            float y = (float) (Math.random() * mapHeight);
            float territoryRadius = 100f;
            spawnRobotButler(x, y, territoryRadius);
        }
    }

    public void update(float deltaTime, List<Vector2> playerPositions) {
        update(deltaTime, playerPositions, null);
    }

    public void update(float deltaTime, List<Vector2> playerPositions, CollisionChecker collisionChecker) {
        for (Npc npc : npcs.values()) {
            float previousX = npc.getPosition().x;
            float previousY = npc.getPosition().y;

            npc.update(deltaTime, playerPositions);

            if (collisionChecker != null && isNpcBlocked(npc, collisionChecker)) {
                npc.getPosition().set(previousX, previousY);
            }
        }
    }

    private boolean isNpcBlocked(Npc npc, CollisionChecker collisionChecker) {
        float halfSize = npc.getSize() * 0.5f;
        float left = npc.getPosition().x - halfSize;
        float right = npc.getPosition().x + halfSize;
        float bottom = npc.getPosition().y - halfSize;
        float top = npc.getPosition().y + halfSize;

        return collisionChecker.isBlocked(left, bottom)
            || collisionChecker.isBlocked(right, bottom)
            || collisionChecker.isBlocked(left, top)
            || collisionChecker.isBlocked(right, top);
    }

    public void draw(SpriteBatch batch) {
        for (Npc npc : npcs.values()) {
            npc.draw(batch);
        }
    }

    public void dispose() {
        for (Npc npc : npcs.values()) {
            npc.dispose();
        }
        RobotButler.disposeTextures();
    }

    public List<Network.NpcState> createStateSnapshot() {
        List<Network.NpcState> states = new ArrayList<>();
        for (Npc npc : npcs.values()) {
            // Dead NPCs are not included in the snapshot; clients remove them automatically.
            if (!npc.isAlive()) continue;
            Network.NpcState state = new Network.NpcState();
            state.npcId = npc.getId();
            state.type = npc.getClass().getSimpleName();
            state.x = npc.getPosition().x;
            state.y = npc.getPosition().y;
            state.size = npc.getSize();
            state.chasing = npc.isChasing();
            state.health = npc.getHealth();
            state.maxHealth = npc.getMaxHealth();
            states.add(state);
        }
        return states;
    }

    public void applySnapshot(List<Network.NpcState> states) {
        if (states == null) {
            return;
        }

        Set<Integer> presentIds = new HashSet<>();
        for (Network.NpcState state : states) {
            presentIds.add(state.npcId);
            Npc npc = npcs.get(state.npcId);
            if (npc == null) {
                npc = createNpcFromState(state);
                npcs.put(state.npcId, npc);
            }
            npc.applyNetworkState(state.x, state.y, state.chasing, state.size);
            // Sync health from server authoritative state
            npc.applyHealthState(state.health, state.maxHealth);
        }

        // Remove NPCs that are no longer in the snapshot (dead or removed on server)
        npcs.keySet().removeIf(id -> !presentIds.contains(id));
    }

    private Npc createNpcFromState(Network.NpcState state) {
        if ("RobotButler".equals(state.type)) {
            return new RobotButler(state.npcId, state.x, state.y, 100f);
        }
        return new RobotButler(state.npcId, state.x, state.y, 100f);
    }

    public List<Npc> getNpcs() {
        return new ArrayList<>(npcs.values());
    }
}
