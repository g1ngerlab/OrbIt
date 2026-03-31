package server;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryonet.Connection;
import server.combat.BulletCombatSystem;
import server.combat.NpcMeleeSystem;
import server.model.LobbyPlayer;
import server.model.PendingShot;
import server.model.ServerBullet;
import server.model.ServerOrb;
import server.npc.RobotSpawner;
import server.world.MapCollisionGridCache;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.world.MapCollisionGrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Represents a game lobby that can hold multiple players
 */
public class Lobby {
    private static final Logger LOGGER = Logger.getLogger(Lobby.class.getName());
    private static final int ROBOT_COUNT = 40;
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
    private static final int ORB_DROP_MIN = 3;
    private static final int ORB_DROP_MAX = 7;
    private static final float ORB_DROP_MIN_DISTANCE = 18f;
    private static final float ORB_DROP_MAX_DISTANCE = 54f;
    private static final float ORB_RADIUS = 16f;
    private static final float ORB_PICKUP_RADIUS = 34f;
    private static final float ORB_INITIAL_SPEED_MIN = 70f;
    private static final float ORB_INITIAL_SPEED_MAX = 150f;
    private static final float ORB_DRAG_PER_SECOND = 3.2f;
    private static final float ORB_BOUNCE_DAMPING = 0.55f;
    private static final float ORB_SETTLE_SPEED = 12f;
    private static final int INVENTORY_SLOTS = 12;
    private static final float PLAYER_DROP_MIN_DISTANCE = 44f;
    private static final float PLAYER_DROP_MAX_DISTANCE = 72f;

    private final int lobbyId;
    private final String lobbyName;
    private final Map<Integer, LobbyPlayer> players;
    private final NpcManager npcManager;
    private final MapCollisionGrid collisionGrid;
    private final Random random;
    private final Queue<PendingShot> pendingShots;
    private final Map<Integer, ServerBullet> bullets;
    private final Map<Integer, Network.CorpseState> corpses;
    private final Map<Integer, ServerOrb> orbs;
    private final Map<Integer, ItemStack[]> playerInventories;
    private final RobotSpawner robotSpawner;
    private final BulletCombatSystem bulletCombatSystem;
    private final NpcMeleeSystem npcMeleeSystem;
    private final BulletCombatCallbacks bulletCombatCallbacks;
    private final NpcMeleeCallbacks npcMeleeCallbacks;

    private int nextBulletId;
    private int nextCorpseId;
    private int nextOrbId;
    private long npcTick;
    /** Timestamp (System.currentTimeMillis) when lobby became empty, or 0 if not empty */
    private long emptyTimestamp = 0;

    public Lobby(int lobbyId, String lobbyName) {
        this.lobbyId = lobbyId;
        this.lobbyName = lobbyName;
        this.players = new ConcurrentHashMap<>();
        this.npcManager = new NpcManager();
        this.collisionGrid = MapCollisionGridCache.get();
        this.random = new Random();
        this.pendingShots = new ConcurrentLinkedQueue<>();
        this.bullets = new HashMap<>();
        this.corpses = new ConcurrentHashMap<>();
        this.orbs = new ConcurrentHashMap<>();
        this.playerInventories = new ConcurrentHashMap<>();
        this.robotSpawner = new RobotSpawner(ROBOT_COUNT, ROBOT_TERRITORY_RADIUS, MIN_ROBOT_SPAWN_DISTANCE);
        this.bulletCombatSystem = new BulletCombatSystem(
            PLAYER_WIDTH,
            PLAYER_HEIGHT,
            BULLET_SPEED,
            BULLET_RADIUS,
            BULLET_LIFETIME,
            BULLET_DAMAGE
        );
        this.npcMeleeSystem = new NpcMeleeSystem(
            PLAYER_WIDTH,
            PLAYER_HEIGHT,
            NPC_MELEE_DAMAGE,
            NPC_MELEE_INTERVAL
        );
        this.bulletCombatCallbacks = new BulletCombatCallbacks();
        this.npcMeleeCallbacks = new NpcMeleeCallbacks();
        this.nextBulletId = 1;
        this.nextCorpseId = 1;
        this.nextOrbId = 1;
        int spawned = robotSpawner.spawnInitial(npcManager, collisionGrid, random);
        LOGGER.info(lobbyTag() + "Spawned robots: " + spawned);
        this.npcTick = 0;
    }

    public void addPlayer(int clientId, String playerName, Connection connection) {
        LobbyPlayer player = createPlayer(clientId, playerName, connection);
        players.put(clientId, player);
        playerInventories.put(clientId, createEmptyInventory());
        emptyTimestamp = 0; // Lobby is no longer empty

        LOGGER.info(lobbyTag() + "Player " + playerName + " joined. Total players: " + players.size());

        sendLobbySnapshotToNewPlayer(clientId, connection, player);
        notifyOthersAboutNewPlayer(player);
        sendWorldSnapshotTo(connection);
    }

    public void removePlayer(int clientId) {
        LobbyPlayer player = players.remove(clientId);
        if (player != null) {
            playerInventories.remove(clientId);
            LOGGER.info(lobbyTag() + "Player " + player.getPlayerName() + " left. Total players: " + players.size());

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
        LobbyPlayer player = players.get(clientId);
        if (player != null) {
            player.setPositionAndAim(x, y, aimX, aimY);

            // Broadcast position update to all other players
            Network.PlayerPositionUpdate update = new Network.PlayerPositionUpdate(clientId, x, y, moving, aimX, aimY);
            broadcastToOthers(clientId, update);

            // Send step sound if player is moving
            if (moving) {
                Network.PlayerStep stepSound = new Network.PlayerStep(clientId, player.getPlayerName(), x, y);
                broadcastToOthers(clientId, stepSound);
            }
        }
    }

    public void queueShootRequest(int clientId, float targetX, float targetY) {
        if (players.containsKey(clientId)) {
            LobbyPlayer shooter = players.get(clientId);

            // Queue the actual bullet
            pendingShots.offer(new PendingShot(clientId, targetX, targetY));

            // Send sound event to all clients for audio playback
            Network.PlayerShoot shootEvent = new Network.PlayerShoot(
                clientId,
                shooter.getPlayerName(),
                shooter.getX(),
                shooter.getY()
            );
            broadcastToAll(shootEvent);

            LOGGER.fine(lobbyTag() + "Player " + shooter.getPlayerName() + " shot at (" + targetX + ", " + targetY + ")");
        }
    }

    public void dropInventoryItem(int clientId, int slotIndex) {
        LobbyPlayer player = players.get(clientId);
        ItemStack[] inventory = playerInventories.get(clientId);
        if (player == null || inventory == null || slotIndex < 0 || slotIndex >= inventory.length) {
            return;
        }
        ItemStack stack = inventory[slotIndex];
        if (stack == null || stack.getAmount() <= 0) {
            return;
        }

        if (!dropStackFromPlayerPosition(player, stack.getItemTypeId(), 1)) {
            return;
        }
        stack.remove(1);
        if (stack.isEmpty()) {
            inventory[slotIndex] = null;
        }
        sendInventoryUpdate(clientId);
    }

    public void moveInventoryItem(int clientId, int fromSlotIndex, int toSlotIndex) {
        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null
            || fromSlotIndex < 0 || fromSlotIndex >= inventory.length
            || toSlotIndex < 0 || toSlotIndex >= inventory.length
            || fromSlotIndex == toSlotIndex) {
            return;
        }

        ItemStack from = inventory[fromSlotIndex];
        ItemStack to = inventory[toSlotIndex];
        if (from == null || from.isEmpty()) {
            return;
        }

        if (to == null || to.isEmpty()) {
            inventory[toSlotIndex] = from;
            inventory[fromSlotIndex] = null;
            sendInventoryUpdate(clientId);
            return;
        }

        if (from.getItemTypeId() == to.getItemTypeId()) {
            int maxStack = Math.max(1, ItemRegistry.getMaxStack(from.getItemTypeId()));
            int canMove = Math.min(from.getAmount(), Math.max(0, maxStack - to.getAmount()));
            if (canMove > 0) {
                to.add(canMove);
                from.remove(canMove);
                if (from.isEmpty()) {
                    inventory[fromSlotIndex] = null;
                }
                sendInventoryUpdate(clientId);
            }
            return;
        }

        // Different item types -> swap slots.
        inventory[fromSlotIndex] = to;
        inventory[toSlotIndex] = from;
        sendInventoryUpdate(clientId);
    }

    public void updateNpcs(float deltaTime) {
        List<Vector2> playerPositions = collectPlayerPositions();

        npcManager.update(deltaTime, playerPositions, collisionGrid::isBlocked);
        nextBulletId = bulletCombatSystem.spawnPendingShots(
            pendingShots,
            players,
            bullets,
            collisionGrid,
            nextBulletId
        );
        bulletCombatSystem.updateBullets(deltaTime, bullets, players, npcManager, collisionGrid, bulletCombatCallbacks);
        npcMeleeSystem.applyDamage(deltaTime, players, npcManager, npcMeleeCallbacks);
        updateOrbsPhysics(deltaTime);
        updateOrbPickups();

        npcTick++;
        broadcastToAll(createNpcStateUpdate());
        broadcastToAll(createBulletStateUpdate());
        broadcastToAll(createOrbStateUpdate());
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


    private Network.NpcStateUpdate createNpcStateUpdate() {
        Network.NpcStateUpdate update = new Network.NpcStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        update.npcs.addAll(npcManager.createStateSnapshot());
        return update;
    }

    private Network.BulletStateUpdate createBulletStateUpdate() {
        return bulletCombatSystem.createSnapshot(lobbyId, npcTick, bullets);
    }

    private Network.CorpseStateUpdate createCorpseStateUpdate() {
        Network.CorpseStateUpdate update = new Network.CorpseStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = npcTick;
        update.corpses.addAll(corpses.values());
        return update;
    }

    private Network.OrbStateUpdate createOrbStateUpdate() {
        Network.OrbStateUpdate update = new Network.OrbStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = npcTick;

        for (ServerOrb orb : orbs.values()) {
            Network.OrbState state = new Network.OrbState();
            state.orbId = orb.getOrbId();
            state.x = orb.getX();
            state.y = orb.getY();
            state.radius = orb.getRadius();
            update.orbs.add(state);
        }

        return update;
    }

    private void addPlayerCorpse(LobbyPlayer player) {
        Network.CorpseState corpse = new Network.CorpseState();
        corpse.corpseId = nextCorpseId++;
        corpse.kind = Network.CorpseState.KIND_PLAYER;
        corpse.x = player.getX() + PLAYER_WIDTH * 0.5f;
        corpse.y = player.getY() + PLAYER_HEIGHT * 0.5f;
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
        for (LobbyPlayer player : players.values()) {
            player.getConnection().sendTCP(message);
        }
    }

    private void broadcastToOthers(int excludeClientId, Object message) {
        for (LobbyPlayer player : players.values()) {
            if (player.getClientId() != excludeClientId) {
                player.getConnection().sendTCP(message);
            }
        }
    }

    private LobbyPlayer createPlayer(int clientId, String playerName, Connection connection) {
        return new LobbyPlayer(clientId, playerName, connection, 400f, 300f, PLAYER_WIDTH, PLAYER_HEIGHT, 100f);
    }

    private void sendLobbySnapshotToNewPlayer(int clientId, Connection connection, LobbyPlayer joiningPlayer) {
        Network.LobbyState lobbyState = new Network.LobbyState();
        for (LobbyPlayer player : players.values()) {
            if (player.getClientId() != clientId) {
                lobbyState.players.add(new Network.PlayerInfo(
                    player.getClientId(),
                    player.getPlayerName(),
                    player.getX(),
                    player.getY(),
                    player.getAimX(),
                    player.getAimY()
                ));
            }
        }
        connection.sendTCP(lobbyState);
        connection.sendTCP(new Network.LobbyJoinConfirmed(lobbyId, joiningPlayer.getX(), joiningPlayer.getY()));
    }

    private void notifyOthersAboutNewPlayer(LobbyPlayer player) {
        Network.PlayerJoined joined = new Network.PlayerJoined(
            player.getClientId(),
            player.getPlayerName(),
            player.getX(),
            player.getY()
        );
        broadcastToOthers(player.getClientId(), joined);
    }

    private void sendWorldSnapshotTo(Connection connection) {
        connection.sendTCP(createNpcStateUpdate());
        connection.sendTCP(createBulletStateUpdate());
        connection.sendTCP(createCorpseStateUpdate());
        connection.sendTCP(createOrbStateUpdate());
        connection.sendTCP(createInventoryStateUpdate(connection.getID()));
    }

    private void updateOrbPickups() {
        float pickupRadiusSq = ORB_PICKUP_RADIUS * ORB_PICKUP_RADIUS;
        List<Integer> toRemove = new ArrayList<>();

        for (ServerOrb orb : orbs.values()) {
            LobbyPlayer collector = null;
            for (LobbyPlayer player : players.values()) {
                float playerCenterX = player.getX() + PLAYER_WIDTH * 0.5f;
                float playerCenterY = player.getY() + PLAYER_HEIGHT * 0.5f;
                float dx = playerCenterX - orb.getX();
                float dy = playerCenterY - orb.getY();
                if (dx * dx + dy * dy <= pickupRadiusSq) {
                    collector = player;
                    break;
                }
            }

            if (collector != null) {
                if (addItemToInventory(collector.getClientId(), ItemKind.ORB.getId(), 1)) {
                    toRemove.add(orb.getOrbId());
                }
            }
        }

        for (Integer orbId : toRemove) {
            orbs.remove(orbId);
        }
    }

    private void updateOrbsPhysics(float deltaTime) {
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

    private List<Vector2> collectPlayerPositions() {
        List<Vector2> playerPositions = new ArrayList<>();
        for (LobbyPlayer player : players.values()) {
            playerPositions.add(new Vector2(player.getX(), player.getY()));
        }
        return playerPositions;
    }

    private void broadcastPlayerHealth(LobbyPlayer player) {
        broadcastToAll(new Network.PlayerHealthUpdate(
            player.getClientId(),
            player.getCurrentHealth(),
            player.getMaxHealth()
        ));
    }

    private void onPlayerDamagedByBullet(LobbyPlayer player) {
        LOGGER.info(lobbyTag() + "Player " + player.getPlayerName()
            + " hit! HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth());
        broadcastPlayerHealth(player);
    }

    private void onPlayerDamagedByMelee(LobbyPlayer player) {
        LOGGER.info(lobbyTag() + "NPC melee hit player "
            + player.getPlayerName() + "! HP: " + player.getCurrentHealth() + "/" + player.getMaxHealth());
        broadcastPlayerHealth(player);
        broadcastNpcAttack();

        // Send hit sound to all clients for audio playback
        // Find the attacking NPC(s) and get their position
        for (Npc npc : npcManager.getNpcs()) {
            if (npc.isChasing() && Math.hypot(npc.getPosition().x - player.getX(), npc.getPosition().y - player.getY()) < 100f) {
                Network.NpcHitSound hitSound = new Network.NpcHitSound(npc.getId(), npc.getPosition().x, npc.getPosition().y);
                broadcastToAll(hitSound);
                break; // Only one NPC hit at a time
            }
        }
    }

    private void onPlayerKilled(LobbyPlayer player, String reason) {
        LOGGER.info(lobbyTag() + "Player " + player.getPlayerName() + " killed by " + reason + "!");
        if ("bullet".equals(reason)) {
            dropAllInventoryItems(player);
        }
        addPlayerCorpse(player);
        broadcastToAll(new Network.EntityDied(Network.EntityDied.TYPE_PLAYER, player.getClientId()));
        players.remove(player.getClientId());
        playerInventories.remove(player.getClientId());
        if ("NPC melee".equals(reason)) {
            broadcastNpcAttack();
        }
    }

    private void broadcastNpcAttack() {
        for (Npc npc : npcManager.getNpcs()) {
            if (npc.isChasing()) {
                broadcastToAll(new Network.NpcAttack(npc.getId()));
            }
        }
    }

    private void onNpcKilledByBullet(Npc npc) {
        LOGGER.info(lobbyTag() + "NPC #" + npc.getId() + " killed!");
        addRobotButlerCorpse(npc);
        spawnOrbsAroundNpc(npc);
        broadcastToAll(new Network.EntityDied(Network.EntityDied.TYPE_NPC, npc.getId()));

        // Send death sound to all clients for audio playback
        Network.NpcDeathSound deathSound = new Network.NpcDeathSound(npc.getId(), npc.getPosition().x, npc.getPosition().y);
        broadcastToAll(deathSound);
    }

    private void spawnOrbsAroundNpc(Npc npc) {
        int dropCount = ORB_DROP_MIN + random.nextInt(ORB_DROP_MAX - ORB_DROP_MIN + 1);
        spawnOrbsAroundPoint(
            npc.getPosition().x,
            npc.getPosition().y,
            dropCount,
            ORB_DROP_MIN_DISTANCE,
            ORB_DROP_MAX_DISTANCE
        );
    }

    private void spawnOrbsAroundPoint(float centerX, float centerY, int dropCount, float minDistance, float maxDistance) {

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
            ServerOrb orb = new ServerOrb(orbId, x, y, ORB_RADIUS, velocityX, velocityY);
            orbs.put(orbId, orb);
        }
    }

    private ItemStack[] createEmptyInventory() {
        return new ItemStack[INVENTORY_SLOTS];
    }

    private boolean addItemToInventory(int clientId, int itemTypeId, int amount) {
        if (amount <= 0 || itemTypeId == ItemKind.NONE.getId()) {
            return false;
        }

        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null) {
            return false;
        }

        int remaining = amount;
        int maxStack = Math.max(1, ItemRegistry.getMaxStack(itemTypeId));

        for (int i = 0; i < inventory.length; i++) {
            ItemStack slot = inventory[i];
            if (slot == null || slot.getItemTypeId() != itemTypeId || slot.getAmount() >= maxStack) {
                continue;
            }

            int canAdd = Math.min(remaining, maxStack - slot.getAmount());
            slot.add(canAdd);
            remaining -= canAdd;
            if (remaining <= 0) {
                sendInventoryUpdate(clientId);
                return true;
            }
        }

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null || inventory[i].isEmpty()) {
                int put = Math.min(remaining, maxStack);
                inventory[i] = new ItemStack(itemTypeId, put);
                remaining -= put;
                if (remaining <= 0) {
                    sendInventoryUpdate(clientId);
                    return true;
                }
            }
        }

        if (remaining < amount) {
            sendInventoryUpdate(clientId);
            return true;
        }

        return false;
    }

    private void dropAllInventoryItems(LobbyPlayer player) {
        ItemStack[] inventory = playerInventories.get(player.getClientId());
        if (inventory == null) {
            return;
        }

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.getAmount() > 0) {
                dropStackFromPlayerPosition(player, stack.getItemTypeId(), stack.getAmount());
            }
            inventory[i] = null;
        }
    }

    private void sendInventoryUpdate(int clientId) {
        LobbyPlayer player = players.get(clientId);
        if (player == null) {
            return;
        }
        player.getConnection().sendTCP(createInventoryStateUpdate(clientId));
    }

    private Network.InventoryStateUpdate createInventoryStateUpdate(int clientId) {
        Network.InventoryStateUpdate update = new Network.InventoryStateUpdate(clientId);
        ItemStack[] inventory = playerInventories.get(clientId);

        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            ItemStack stack = (inventory != null && i < inventory.length) ? inventory[i] : null;
            int amount = stack == null ? 0 : Math.max(0, stack.getAmount());
            int itemType = stack == null ? Network.ItemType.NONE : stack.getItemTypeId();
            update.slots.add(new Network.InventorySlotState(itemType, amount));
        }

        return update;
    }

    private boolean dropStackFromPlayerPosition(LobbyPlayer player, int itemTypeId, int amount) {
        if (amount <= 0) {
            return false;
        }

        if (!ItemRegistry.isDroppableInWorld(itemTypeId)) {
            return false;
        }

        float centerX = player.getX() + PLAYER_WIDTH * 0.5f;
        float centerY = player.getY() + PLAYER_HEIGHT * 0.5f;
        if (itemTypeId == ItemKind.ORB.getId()) {
            spawnOrbsAroundPoint(centerX, centerY, amount, PLAYER_DROP_MIN_DISTANCE, PLAYER_DROP_MAX_DISTANCE);
            return true;
        }

        return false;
    }

    private String lobbyTag() {
        return "[Lobby " + lobbyId + "] ";
    }

    private final class BulletCombatCallbacks implements BulletCombatSystem.Callbacks {
        @Override
        public void onPlayerDamaged(LobbyPlayer player) {
            onPlayerDamagedByBullet(player);
        }

        @Override
        public void onPlayerKilled(LobbyPlayer player) {
            Lobby.this.onPlayerKilled(player, "bullet");
        }

        @Override
        public void onNpcKilled(Npc npc) {
            onNpcKilledByBullet(npc);
        }
    }

    private final class NpcMeleeCallbacks implements NpcMeleeSystem.Callbacks {
        @Override
        public void onPlayerDamaged(LobbyPlayer player) {
            onPlayerDamagedByMelee(player);
        }

        @Override
        public void onPlayerKilled(LobbyPlayer player) {
            Lobby.this.onPlayerKilled(player, "NPC melee");
        }
    }
}
