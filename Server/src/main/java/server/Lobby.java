package server;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryonet.Connection;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.world.MapCollisionGrid;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a game lobby that can hold multiple players
 */
public class Lobby {
    private static final int ROBOT_COUNT = 24;
    private static final float ROBOT_TERRITORY_RADIUS = 120f;
    private static final float MIN_ROBOT_SPAWN_DISTANCE = 48f;

    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 48f;
    private static final float PLAYER_CORPSE_SCALE = 1f;

    private static final float BULLET_SPEED = 480f;
    private static final float BULLET_RADIUS = 4f;
    private static final float BULLET_LIFETIME = 1.8f;
    /** Damage dealt per bullet hit */
    private static final float BULLET_DAMAGE = 5f;
    /** Damage dealt by an NPC touching a player, per second */
    private static final float NPC_MELEE_DAMAGE = 10f;
    /** How often (in seconds) an NPC can damage a player while in contact */
    private static final float NPC_MELEE_INTERVAL = 1.0f;

    private static class PendingShot {
        final int ownerClientId;
        final float targetX;
        final float targetY;

        PendingShot(int ownerClientId, float targetX, float targetY) {
            this.ownerClientId = ownerClientId;
            this.targetX = targetX;
            this.targetY = targetY;
        }
    }

    private static class ServerBullet {
        final int bulletId;
        final int ownerClientId;
        final Vector2 position;
        final Vector2 velocity;
        float timeRemaining;

        ServerBullet(int bulletId, int ownerClientId, Vector2 position, Vector2 velocity, float timeRemaining) {
            this.bulletId = bulletId;
            this.ownerClientId = ownerClientId;
            this.position = position;
            this.velocity = velocity;
            this.timeRemaining = timeRemaining;
        }
    }

    private final int lobbyId;
    private final String lobbyName;
    private final Map<Integer, PlayerData> players;
    private final NpcManager npcManager;
    private final MapCollisionGrid collisionGrid;
    private final Random random;
    private final Queue<PendingShot> pendingShots;
    private final Map<Integer, ServerBullet> bullets;
    private final Map<Integer, Network.CorpseState> corpses;

    private int nextBulletId;
    private int nextCorpseId;
    private long npcTick;
    /** Timestamp (System.currentTimeMillis) when lobby became empty, or 0 if not empty */
    private long emptyTimestamp = 0;

    public static class PlayerData {
        public int clientId;
        public String playerName;
        public float x;
        public float y;
        public float aimX;
        public float aimY;
        public Connection connection;

        // Server-authoritative health
        public float maxHealth = 100f;
        public float currentHealth = 100f;

        /**
         * Time remaining until this player can take melee damage again.
         * Counts down each tick; damage is applied when it reaches 0.
         */
        public float meleeDamageCooldown = 0f;

        public boolean isDead() { return currentHealth <= 0f; }

        public void takeDamage(float damage) {
            currentHealth = Math.max(0f, currentHealth - damage);
        }

        public PlayerData(int clientId, String playerName, Connection connection) {
            this.clientId = clientId;
            this.playerName = playerName;
            this.connection = connection;
            this.x = 400;
            this.y = 300;
            this.aimX = this.x + PLAYER_WIDTH;
            this.aimY = this.y + PLAYER_HEIGHT * 0.5f;
        }
    }

    public Lobby(int lobbyId, String lobbyName) {
        this.lobbyId = lobbyId;
        this.lobbyName = lobbyName;
        this.players = new ConcurrentHashMap<>();
        this.npcManager = new NpcManager();
        this.collisionGrid = new MapCollisionGrid(resolveMapPath());
        this.random = new Random();
        this.pendingShots = new ConcurrentLinkedQueue<>();
        this.bullets = new HashMap<>();
        this.corpses = new ConcurrentHashMap<>();
        this.nextBulletId = 1;
        this.nextCorpseId = 1;
        spawnInitialRobots();
        this.npcTick = 0;
    }

    public void addPlayer(int clientId, String playerName, Connection connection) {
        PlayerData player = new PlayerData(clientId, playerName, connection);
        players.put(clientId, player);
        emptyTimestamp = 0; // Lobby is no longer empty

        System.out.println("[Lobby " + lobbyId + "] Player " + playerName + " joined. Total players: " + players.size());

        // Send current lobby state to the new player
        Network.LobbyState lobbyState = new Network.LobbyState();
        for (PlayerData p : players.values()) {
            if (p.clientId != clientId) { // Don't include the joining player
                lobbyState.players.add(new Network.PlayerInfo(p.clientId, p.playerName, p.x, p.y, p.aimX, p.aimY));
            }
        }
        connection.sendTCP(lobbyState);

        // Send confirmation that player can spawn
        Network.LobbyJoinConfirmed confirmed = new Network.LobbyJoinConfirmed(lobbyId, player.x, player.y);
        connection.sendTCP(confirmed);

        // Notify all other players about the new player
        Network.PlayerJoined joined = new Network.PlayerJoined(clientId, playerName, player.x, player.y);
        broadcastToOthers(clientId, joined);

        // New client needs current NPC and bullet world state immediately after joining.
        connection.sendTCP(createNpcStateUpdate());
        connection.sendTCP(createBulletStateUpdate());
        connection.sendTCP(createCorpseStateUpdate());
    }

    public void removePlayer(int clientId) {
        PlayerData player = players.remove(clientId);
        if (player != null) {
            System.out.println("[Lobby " + lobbyId + "] Player " + player.playerName + " left. Total players: " + players.size());

            // Track when lobby became empty
            if (players.isEmpty() && emptyTimestamp == 0) {
                emptyTimestamp = System.currentTimeMillis();
            }

            // Notify all remaining players
            Network.PlayerLeft left = new Network.PlayerLeft(clientId);
            broadcastToAll(left);
        }
    }

    public void updatePlayerPosition(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        PlayerData player = players.get(clientId);
        if (player != null) {
            player.x = x;
            player.y = y;
            player.aimX = aimX;
            player.aimY = aimY;

            // Broadcast position update to all other players
            Network.PlayerPositionUpdate update = new Network.PlayerPositionUpdate(clientId, x, y, moving, aimX, aimY);
            broadcastToOthers(clientId, update);
        }
    }

    public void queueShootRequest(int clientId, float targetX, float targetY) {
        if (players.containsKey(clientId)) {
            pendingShots.offer(new PendingShot(clientId, targetX, targetY));
        }
    }

    public void updateNpcs(float deltaTime) {
        List<Vector2> playerPositions = new ArrayList<>();
        for (PlayerData player : players.values()) {
            playerPositions.add(new Vector2(player.x, player.y));
        }

        npcManager.update(deltaTime, playerPositions, collisionGrid::isBlocked);
        spawnPendingBullets();
        updateBullets(deltaTime);
        applyNpcMeleeDamage(deltaTime);

        npcTick++;
        broadcastToAll(createNpcStateUpdate());
        broadcastToAll(createBulletStateUpdate());
    }

    public boolean hasPlayer(int clientId) {
        return players.containsKey(clientId);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public int getLobbyId() {
        return lobbyId;
    }

    public String getLobbyName() {
        return lobbyName;
    }

    /**
     * Returns true if the lobby is empty and has been empty for longer than the given duration.
     */
    public boolean isEmptyForLongerThan(long durationMs) {
        return players.isEmpty() && emptyTimestamp > 0
                && (System.currentTimeMillis() - emptyTimestamp) > durationMs;
    }

    private void spawnInitialRobots() {
        int spawned = 0;
        int maxAttempts = ROBOT_COUNT * 50;

        for (int attempt = 0; attempt < maxAttempts && spawned < ROBOT_COUNT; attempt++) {
            float[] pos = collisionGrid.findRandomOpenPosition(random);
            if (!isFarEnoughFromOtherRobots(pos[0], pos[1])) {
                continue;
            }
            npcManager.spawnRobotButler(pos[0], pos[1], ROBOT_TERRITORY_RADIUS);
            spawned++;
        }

        // Fallback: ensure target count even if map is cramped.
        while (spawned < ROBOT_COUNT) {
            float[] pos = collisionGrid.findRandomOpenPosition(random);
            npcManager.spawnRobotButler(pos[0], pos[1], ROBOT_TERRITORY_RADIUS);
            spawned++;
        }

        System.out.println("[Lobby " + lobbyId + "] Spawned robots: " + spawned);
    }

    private void spawnPendingBullets() {
        PendingShot shot;
        while ((shot = pendingShots.poll()) != null) {
            PlayerData owner = players.get(shot.ownerClientId);
            if (owner == null) {
                continue;
            }

            Vector2 origin = new Vector2(owner.x + PLAYER_WIDTH * 0.5f, owner.y + PLAYER_HEIGHT * 0.5f);
            Vector2 direction = new Vector2(shot.targetX - origin.x, shot.targetY - origin.y);
            if (direction.isZero(0.001f)) {
                direction.set(1f, 0f);
            } else {
                direction.nor();
            }

            Vector2 spawnPoint = new Vector2(origin).mulAdd(direction, 18f);
            if (collisionGrid.isBlocked(spawnPoint.x, spawnPoint.y)) {
                continue;
            }

            Vector2 velocity = new Vector2(direction).scl(BULLET_SPEED);
            int bulletId = nextBulletId++;
            bullets.put(bulletId, new ServerBullet(bulletId, shot.ownerClientId, spawnPoint, velocity, BULLET_LIFETIME));
        }
    }

    private void updateBullets(float deltaTime) {
        Iterator<ServerBullet> iterator = bullets.values().iterator();
        while (iterator.hasNext()) {
            ServerBullet bullet = iterator.next();
            bullet.position.mulAdd(bullet.velocity, deltaTime);
            bullet.timeRemaining -= deltaTime;

            if (bullet.timeRemaining <= 0f) {
                iterator.remove();
                continue;
            }
            if (collisionGrid.isBlocked(bullet.position.x, bullet.position.y)) {
                iterator.remove();
                continue;
            }
            if (hitsAnyOtherPlayer(bullet)) {
                iterator.remove();
                continue;
            }
            if (hitsAnyNpc(bullet)) {
                iterator.remove();
            }
        }
    }

    /**
     * Checks if any alive NPC overlaps any player's hitbox and deals melee damage
     * once per NPC_MELEE_INTERVAL seconds while contact is maintained.
     */
    private void applyNpcMeleeDamage(float deltaTime) {
        for (PlayerData player : players.values()) {
            if (player.isDead()) continue;

            // Tick down the player's melee cooldown
            if (player.meleeDamageCooldown > 0f) {
                player.meleeDamageCooldown -= deltaTime;
            }

            // Check every alive NPC for contact with this player
            boolean anyContact = false;
            for (Npc npc : npcManager.getNpcs()) {
                if (!npc.isAlive()) continue;

                float half = npc.getSize() * 0.5f;
                if (rectsOverlap(
                    npc.getPosition().x - half, npc.getPosition().y - half,
                    npc.getSize(), npc.getSize(),
                    player.x, player.y,
                    PLAYER_WIDTH, PLAYER_HEIGHT
                )) {
                    anyContact = true;
                    break;
                }
            }

            // Deal damage when cooldown expired and NPC is touching the player
            if (anyContact && player.meleeDamageCooldown <= 0f) {
                player.takeDamage(NPC_MELEE_DAMAGE);
                player.meleeDamageCooldown = NPC_MELEE_INTERVAL;

                System.out.println("[Lobby " + lobbyId + "] NPC melee hit player "
                    + player.playerName + "! HP: " + player.currentHealth + "/" + player.maxHealth);

                broadcastToAll(new Network.PlayerHealthUpdate(
                    player.clientId, player.currentHealth, player.maxHealth));

                if (player.isDead()) {
                    System.out.println("[Lobby " + lobbyId + "] Player " + player.playerName + " killed by NPC melee!");
                    addPlayerCorpse(player);
                    broadcastToAll(new Network.EntityDied(
                        Network.EntityDied.TYPE_PLAYER, player.clientId));
                    players.remove(player.clientId);
                }
            }
        }
    }

    /** Returns true if two axis-aligned rectangles overlap. */
    private boolean rectsOverlap(float ax, float ay, float aw, float ah,
                                  float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx
            && ay < by + bh && ay + ah > by;
    }

    private boolean hitsAnyOtherPlayer(ServerBullet bullet) {
        for (PlayerData player : players.values()) {
            if (player.clientId == bullet.ownerClientId) {
                continue;
            }
            if (player.isDead()) {
                continue;
            }
            if (circleIntersectsRect(
                bullet.position.x,
                bullet.position.y,
                BULLET_RADIUS,
                player.x,
                player.y,
                PLAYER_WIDTH,
                PLAYER_HEIGHT
            )) {
                // Deal damage - shotgun does 25 damage per bullet
                player.takeDamage(BULLET_DAMAGE);
                System.out.println("[Lobby " + lobbyId + "] Player " + player.playerName
                    + " hit! HP: " + player.currentHealth + "/" + player.maxHealth);

                // Notify all clients about the updated health
                broadcastToAll(new Network.PlayerHealthUpdate(
                    player.clientId, player.currentHealth, player.maxHealth));

                if (player.isDead()) {
                    System.out.println("[Lobby " + lobbyId + "] Player " + player.playerName + " died!");
                    addPlayerCorpse(player);
                    broadcastToAll(new Network.EntityDied(
                        Network.EntityDied.TYPE_PLAYER, player.clientId));
                    // Remove player from the lobby
                    players.remove(player.clientId);
                }
                return true;
            }
        }
        return false;
    }

    private boolean hitsAnyNpc(ServerBullet bullet) {
        for (Npc npc : npcManager.getNpcs()) {
            if (!npc.isAlive()) {
                continue;
            }
            float half = npc.getSize() * 0.5f;
            if (circleIntersectsRect(
                bullet.position.x,
                bullet.position.y,
                BULLET_RADIUS,
                npc.getPosition().x - half,
                npc.getPosition().y - half,
                npc.getSize(),
                npc.getSize()
            )) {
                // Deal damage - shotgun does 25 damage per bullet
                npc.takeDamage(BULLET_DAMAGE);
                System.out.println("[Lobby " + lobbyId + "] NPC #" + npc.getId()
                    + " hit! HP: " + npc.getHealth() + "/" + npc.getMaxHealth());

                if (npc.isDead()) {
                    System.out.println("[Lobby " + lobbyId + "] NPC #" + npc.getId() + " killed!");
                    addRobotButlerCorpse(npc);
                    broadcastToAll(new Network.EntityDied(
                        Network.EntityDied.TYPE_NPC, npc.getId()));
                    // NPC will be excluded from next snapshot automatically (isAlive() == false)
                }
                return true;
            }
        }
        return false;
    }

    private boolean circleIntersectsRect(float cx, float cy, float radius, float rx, float ry, float rw, float rh) {
        float nearestX = Math.max(rx, Math.min(cx, rx + rw));
        float nearestY = Math.max(ry, Math.min(cy, ry + rh));
        float dx = cx - nearestX;
        float dy = cy - nearestY;
        return dx * dx + dy * dy <= radius * radius;
    }

    private boolean isFarEnoughFromOtherRobots(float x, float y) {
        for (Npc npc : npcManager.getNpcs()) {
            if (npc.getPosition().dst2(x, y) < MIN_ROBOT_SPAWN_DISTANCE * MIN_ROBOT_SPAWN_DISTANCE) {
                return false;
            }
        }
        return true;
    }

    private static Path resolveMapPath() {
        Path[] candidates = new Path[] {
            Paths.get("assets", "maps", "TalTechMAP.ldtk"),
            Paths.get("..", "assets", "maps", "TalTechMAP.ldtk"),
            Paths.get("..", "..", "assets", "maps", "TalTechMAP.ldtk")
        };

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.exists(normalized)) {
                return normalized;
            }
        }

        throw new IllegalStateException("Cannot find assets/maps/TalTechMAP.ldtk from current working directory");
    }

    private Network.NpcStateUpdate createNpcStateUpdate() {
        Network.NpcStateUpdate update = new Network.NpcStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        update.npcs.addAll(npcManager.createStateSnapshot());
        return update;
    }

    private Network.BulletStateUpdate createBulletStateUpdate() {
        Network.BulletStateUpdate update = new Network.BulletStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = npcTick;

        for (ServerBullet bullet : bullets.values()) {
            Network.BulletState state = new Network.BulletState();
            state.bulletId = bullet.bulletId;
            state.x = bullet.position.x;
            state.y = bullet.position.y;
            state.radius = BULLET_RADIUS;
            state.angleDeg = bullet.velocity.angleDeg();
            update.bullets.add(state);
        }

        return update;
    }

    private Network.CorpseStateUpdate createCorpseStateUpdate() {
        Network.CorpseStateUpdate update = new Network.CorpseStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        update.corpses.addAll(corpses.values());
        return update;
    }

    private void addPlayerCorpse(PlayerData player) {
        Network.CorpseState corpse = new Network.CorpseState();
        corpse.corpseId = nextCorpseId++;
        corpse.kind = Network.CorpseState.KIND_PLAYER;
        corpse.x = player.x + PLAYER_WIDTH * 0.5f;
        corpse.y = player.y + PLAYER_HEIGHT * 0.5f;
        corpse.width = PLAYER_WIDTH * PLAYER_CORPSE_SCALE;
        corpse.height = PLAYER_HEIGHT * PLAYER_CORPSE_SCALE;
        corpse.rotationDeg = 90f;
        corpses.put(corpse.corpseId, corpse);
        broadcastToAll(createCorpseStateUpdate());
    }

    private void addRobotButlerCorpse(Npc npc) {
        Network.CorpseState corpse = new Network.CorpseState();
        corpse.corpseId = nextCorpseId++;
        corpse.kind = Network.CorpseState.KIND_ROBOT_BUTLER;
        corpse.x = npc.getPosition().x;
        corpse.y = npc.getPosition().y;
        corpse.width = npc.getSize();
        corpse.height = npc.getSize();
        corpse.rotationDeg = 90f;
        corpses.put(corpse.corpseId, corpse);
        broadcastToAll(createCorpseStateUpdate());
    }

    private void broadcastToAll(Object message) {
        for (PlayerData player : players.values()) {
            player.connection.sendTCP(message);
        }
    }

    private void broadcastToOthers(int excludeClientId, Object message) {
        for (PlayerData player : players.values()) {
            if (player.clientId != excludeClientId) {
                player.connection.sendTCP(message);
            }
        }
    }
}
