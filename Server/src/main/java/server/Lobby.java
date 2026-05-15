package server;

import com.badlogic.gdx.math.Vector2;
import com.esotericsoftware.kryonet.Connection;
import server.combat.BulletCombatSystem;
import server.combat.NpcMeleeSystem;
import server.lobby.LobbyBroadcaster;
import server.lobby.LobbyInventoryManager;
import server.lobby.LobbyItemDropManager;
import server.lobby.LobbyPlayerManager;
import server.lobby.LobbyStateManager;
import server.lobby.LobbyTimerManager;
import server.lobby.LobbyWorldManager;
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
import taltech.orbitgame.npc.RobotBoss;

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
    private static final int PLAYER_SPAWN_ATTEMPTS = 600;
    private static final float PLAYER_SPAWN_SCAN_STEP = 8f;

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
    private static final int STARTER_WEAPON_ITEM_TYPE = ItemKind.STARTER_SHOTGUN.getId();
    private static final float ORB_INITIAL_SPEED_MIN = 70f;
    private static final float ORB_INITIAL_SPEED_MAX = 150f;
    private static final float ORB_DRAG_PER_SECOND = 3.2f;
    private static final float ORB_BOUNCE_DAMPING = 0.55f;
    private static final float ORB_SETTLE_SPEED = 12f;
    private static final int INVENTORY_SLOTS = 12;
    private static final float PLAYER_DROP_MIN_DISTANCE = 44f;
    private static final float PLAYER_DROP_MAX_DISTANCE = 72f;
    private static final long LOBBY_DURATION_MS = 10L * 60L * 1000L;
    private static final long FREEZE_PHASE_START_MS = 60L * 1000L;
    private static final long FREEZE_DAMAGE_DURATION_MS = 10L * 1000L;
    private static final float BOSS_SPAWN_X = 2365f;
    private static final float BOSS_SPAWN_Y = 3660f;
    private static final int BOSS_ORB_DROP_MIN = 20;
    private static final int BOSS_ORB_DROP_MAX = 35;

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

    // Refactored components
    private final LobbyBroadcaster broadcaster;
    private final LobbyItemDropManager itemDropManager;
    private final LobbyInventoryManager inventoryManager;
    private final LobbyStateManager stateManager;
    private final LobbyPlayerManager playerManager;
    private final LobbyWorldManager worldManager;
    private final LobbyTimerManager timerManager;

    private int nextBulletId;
    private int nextCorpseId;
    private int nextOrbId;

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
        npcManager.spawnRobotBoss(BOSS_SPAWN_X, BOSS_SPAWN_Y);
        LOGGER.info(lobbyTag() + "Spawned boss at (" + BOSS_SPAWN_X + ", " + BOSS_SPAWN_Y + ")");

        // Initialize refactored components
        this.broadcaster = new LobbyBroadcaster(players);
        this.itemDropManager = new LobbyItemDropManager(orbs, collisionGrid, random, 1);
        this.inventoryManager = new LobbyInventoryManager(players, playerInventories, itemDropManager);
        this.stateManager = new LobbyStateManager(lobbyId);
        this.playerManager = new LobbyPlayerManager(lobbyId, players, broadcaster, inventoryManager);
        this.worldManager = new LobbyWorldManager(lobbyId, players, broadcaster);
        this.timerManager = new LobbyTimerManager(lobbyId, players, broadcaster);
    }

    public void addPlayer(int clientId, String playerName, Connection connection) {
        addPlayer(clientId, playerName, 1, connection, null);
    }

    public void addPlayer(int clientId, String playerName, Connection connection, List<Network.InventorySlotState> loadoutSlots) {
        addPlayer(clientId, playerName, 1, connection, loadoutSlots);
    }

    public void addPlayer(int clientId, String playerName, int characterId, Connection connection) {
        addPlayer(clientId, playerName, characterId, connection, null);
    }

    public void addPlayer(int clientId, String playerName, int characterId, Connection connection, List<Network.InventorySlotState> loadoutSlots) {
        if (stateManager.isClosed()) {
            return;
        }
        LobbyPlayer player = createPlayer(clientId, playerName, characterId, connection);
        players.put(clientId, player);
        playerInventories.put(clientId, inventoryManager.createEmptyInventory());
        if (loadoutSlots == null || loadoutSlots.isEmpty()) {
            inventoryManager.grantStarterWeaponIfMissing(clientId, STARTER_WEAPON_ITEM_TYPE);
        } else {
            inventoryManager.applyStartingLoadout(clientId, loadoutSlots);
        }
        stateManager.clearEmpty();

        LOGGER.info(lobbyTag() + "Player " + playerName + " joined. Total players: " + players.size());

        sendLobbySnapshotToNewPlayer(clientId, connection, player);
        playerManager.notifyNewPlayer(player);
        sendWorldSnapshotTo(connection);
    }

    public void removePlayer(int clientId) {
        LobbyPlayer player = players.get(clientId);
        if (player != null) {
            playerInventories.remove(clientId);
            LOGGER.info(lobbyTag() + "Player " + player.getPlayerName() + " left. Total players: " + players.size());

            if (players.isEmpty()) {
                stateManager.markEmpty();
                stateManager.closeLobby("empty");
            }

            Network.PlayerLeft left = new Network.PlayerLeft(clientId);
            broadcaster.broadcastToAll(left);
        }
    }

    public void updatePlayerPosition(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
        playerManager.updatePlayerPosition(clientId, x, y, moving, aimX, aimY);
    }

    public void queueShootRequest(int clientId, float targetX, float targetY) {
        if (players.containsKey(clientId)) {
            LobbyPlayer shooter = players.get(clientId);

            pendingShots.offer(new PendingShot(clientId, targetX, targetY));

            Network.PlayerShoot shootEvent = new Network.PlayerShoot(
                clientId,
                shooter.getPlayerName(),
                shooter.getX(),
                shooter.getY()
            );
            broadcaster.broadcastToAll(shootEvent);

            LOGGER.fine(lobbyTag() + "Player " + shooter.getPlayerName() + " shot at (" + targetX + ", " + targetY + ")");
        }
    }

    public void dropInventoryItem(int clientId, int slotIndex) {
        inventoryManager.dropInventoryItem(clientId, slotIndex);
    }

    public void moveInventoryItem(int clientId, int fromSlotIndex, int toSlotIndex) {
        inventoryManager.moveInventoryItem(clientId, fromSlotIndex, toSlotIndex);
    }

    public void purchaseShopItem(int clientId, int itemTypeId, int amount, int orbCost) {
        LobbyPlayer player = players.get(clientId);
        if (player == null) {
            return;
        }

        boolean success = inventoryManager.purchaseItemWithOrbCost(clientId, itemTypeId, amount, orbCost);
        String message = success ? "Purchase successful" : "Not enough ORB or inventory space";
        player.getConnection().sendTCP(new Network.ShopPurchaseResult(success, message, itemTypeId, amount, orbCost));
    }

    public void updateNpcs(float deltaTime) {
        System.out.println("[LOOP] tick");
        if (stateManager.isClosed()) {
            return;
        }
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
        itemDropManager.updateOrbsPhysics(deltaTime);
        updateOrbPickups();

        worldManager.incrementNpcTick();
        broadcaster.broadcastToAll(createNpcStateUpdate());
        broadcaster.broadcastToAll(createBulletStateUpdate());
        broadcaster.broadcastToAll(createOrbStateUpdate());
    }

    public void updateLobbyTimer(float deltaTime) {
        if (stateManager.isClosed()) {
            return;
        }

        List<LobbyPlayer> killedPlayers = timerManager.updateTimer(deltaTime);

        for (LobbyPlayer killedPlayer : killedPlayers) {
            onPlayerKilled(killedPlayer, "freeze");
        }

        if (timerManager.isTimeExpired()) {
            stateManager.closeLobby("timer_expired");
        }
    }

    public void handleMelee(int clientId, float targetX, float targetY) {

        System.out.println("[MELEE] start");

        LobbyPlayer attacker = players.get(clientId);
        if (attacker == null || attacker.isDead()) {
            System.out.println("[MELEE] attacker null/dead");
            return;
        }

        broadcaster.broadcastToAll(new Network.PlayerMelee(
            clientId,
            attacker.getPlayerName(),
            attacker.getX(),
            attacker.getY()
        ));

        float range = 50f;
        float damage = 3f;

        for (Npc npc : npcManager.getNpcs()) {
            if (!npc.isAlive()) {
                continue;
            }

            System.out.println("[MELEE] checking npc " + npc.getId());

            float dx = npc.getPosition().x - attacker.getX();
            float dy = npc.getPosition().y - attacker.getY();

            if (dx * dx + dy * dy <= range * range) {
                System.out.println("[MELEE] HIT npc " + npc.getId());

                npc.takeDamage(damage);
                broadcaster.broadcastToAll(new Network.NpcHitSound(
                    npc.getId(),
                    npc.getPosition().x,
                    npc.getPosition().y,
                    Network.NpcHitSound.TYPE_PLAYER_MELEE
                ));
                if (npc.isDead()) {
                    onNpcKilledByBullet(npc);
                }

                System.out.println("[MELEE] damage applied");

                break;
            }
        }

        System.out.println("[MELEE] end");
    }

    public void forceAdvanceToFinalMinuteForTesting() {
        if (stateManager.isClosed()) {
            return;
        }
        timerManager.forceAdvanceToFinalMinute();
    }

    public boolean isClosed() {
        return stateManager.isClosed();
    }

    public long getRemainingLobbyTimeMs() {
        return timerManager.getRemainingTimeMs();
    }

    private void closeLobby(String reason) {
        stateManager.closeLobby(reason);
        broadcaster.broadcastToAll(new Network.LobbyClosed(lobbyId, reason));
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
        return stateManager.isEmptyForLongerThan(durationMs);
    }


    private Network.NpcStateUpdate createNpcStateUpdate() {
        Network.NpcStateUpdate update = new Network.NpcStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = worldManager.getNpcTick();
        update.npcs.addAll(npcManager.createStateSnapshot());
        return update;
    }

    private Network.BulletStateUpdate createBulletStateUpdate() {
        return bulletCombatSystem.createSnapshot(lobbyId, worldManager.getNpcTick(), bullets);
    }

    private Network.CorpseStateUpdate createCorpseStateUpdate() {
        Network.CorpseStateUpdate update = new Network.CorpseStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = worldManager.getNpcTick();
        update.corpses.addAll(corpses.values());
        return update;
    }

    private Network.OrbStateUpdate createOrbStateUpdate() {
        Network.OrbStateUpdate update = new Network.OrbStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = worldManager.getNpcTick();

        for (ServerOrb orb : orbs.values()) {
            Network.OrbState state = new Network.OrbState();
            state.orbId = orb.getOrbId();
            state.itemType = orb.getItemTypeId();
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
        broadcaster.broadcastToAll(createCorpseStateUpdate());
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
        broadcaster.broadcastToAll(createCorpseStateUpdate());
    }

    private void addRobotBossCorpse(Npc npc) {
        Network.CorpseState corpse = new Network.CorpseState();
        corpse.corpseId = nextCorpseId++;
        corpse.kind = Network.CorpseState.KIND_ROBOT_BOSS;
        corpse.x = npc.getPosition().x;
        corpse.y = npc.getPosition().y;
        corpse.width = npc.getSize();
        corpse.height = npc.getSize();
        corpse.rotationDeg = 90f;
        corpses.put(corpse.corpseId, corpse);
        broadcaster.broadcastToAll(createCorpseStateUpdate());
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
                    player.getAimY(),
                    player.getCharacterId()
                ));
            }
        }
        connection.sendTCP(lobbyState);
        connection.sendTCP(new Network.LobbyJoinConfirmed(lobbyId, joiningPlayer.getX(), joiningPlayer.getY()));
    }

    private LobbyPlayer createPlayer(int clientId, String playerName, Connection connection) {
        return createPlayer(clientId, playerName, 1, connection);
    }

    private LobbyPlayer createPlayer(int clientId, String playerName, int characterId, Connection connection) {
        int normalizedCharacterId = characterId <= 0 ? 1 : characterId;
        float[] spawn = findRandomPlayerSpawn();
        return new LobbyPlayer(clientId, playerName, normalizedCharacterId,
            connection, spawn[0], spawn[1], PLAYER_WIDTH, PLAYER_HEIGHT, 100f);
    }

    private float[] findRandomPlayerSpawn() {
        float maxSpawnX = Math.max(0f, collisionGrid.getMapWidthPx() - PLAYER_WIDTH);
        float maxSpawnY = Math.max(0f, collisionGrid.getMapHeightPx() - PLAYER_HEIGHT);

        for (int attempt = 0; attempt < PLAYER_SPAWN_ATTEMPTS; attempt++) {
            float x = maxSpawnX <= 0f ? 0f : random.nextFloat() * maxSpawnX;
            float y = maxSpawnY <= 0f ? 0f : random.nextFloat() * maxSpawnY;
            if (isValidPlayerSpawn(x, y)) {
                return new float[] {x, y};
            }
        }

        for (float y = 0f; y <= maxSpawnY; y += PLAYER_SPAWN_SCAN_STEP) {
            for (float x = 0f; x <= maxSpawnX; x += PLAYER_SPAWN_SCAN_STEP) {
                if (isValidPlayerSpawn(x, y)) {
                    return new float[] {x, y};
                }
            }
        }

        float[] randomOpen = collisionGrid.findRandomOpenPosition(random);
        float fallbackX = clamp(randomOpen[0] - PLAYER_WIDTH * 0.5f, 0f, maxSpawnX);
        float fallbackY = clamp(randomOpen[1] - PLAYER_HEIGHT * 0.5f, 0f, maxSpawnY);
        if (isValidPlayerSpawn(fallbackX, fallbackY)) {
            return new float[] {fallbackX, fallbackY};
        }
        return new float[] {0f, 0f};
    }

    private boolean isValidPlayerSpawn(float x, float y) {
        float maxSpawnX = collisionGrid.getMapWidthPx() - PLAYER_WIDTH;
        float maxSpawnY = collisionGrid.getMapHeightPx() - PLAYER_HEIGHT;
        if (x < 0f || y < 0f || x > maxSpawnX || y > maxSpawnY) {
            return false;
        }

        float right = x + PLAYER_WIDTH - 0.01f;
        float top = y + PLAYER_HEIGHT - 0.01f;
        float centerX = x + PLAYER_WIDTH * 0.5f;
        float centerY = y + PLAYER_HEIGHT * 0.5f;

        if (collisionGrid.isBlocked(x, y)
            || collisionGrid.isBlocked(right, y)
            || collisionGrid.isBlocked(x, top)
            || collisionGrid.isBlocked(right, top)
            || collisionGrid.isBlocked(centerX, centerY)) {
            return false;
        }

        for (LobbyPlayer existing : players.values()) {
            if (rectanglesOverlap(x, y, PLAYER_WIDTH, PLAYER_HEIGHT,
                existing.getX(), existing.getY(), PLAYER_WIDTH, PLAYER_HEIGHT)) {
                return false;
            }
        }
        return true;
    }

    private static boolean rectanglesOverlap(float ax, float ay, float aw, float ah,
                                             float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }


    private void sendWorldSnapshotTo(Connection connection) {
        connection.sendTCP(createNpcStateUpdate());
        connection.sendTCP(createBulletStateUpdate());
        connection.sendTCP(createCorpseStateUpdate());
        connection.sendTCP(createOrbStateUpdate());
        connection.sendTCP(inventoryManager.getInventoryStateUpdate(connection.getID()));
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
                if (inventoryManager.addItemToInventory(collector.getClientId(), orb.getItemTypeId(), 1)) {
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
        broadcaster.broadcastToAll(new Network.PlayerHealthUpdate(
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
                broadcaster.broadcastToAll(hitSound);
                break; // Only one NPC hit at a time
            }
        }
    }

    private void onPlayerKilled(LobbyPlayer player, String reason) {
        onPlayerKilled(player, reason, -1);
    }

    private void onPlayerKilled(LobbyPlayer player, String reason, int killerClientId) {
        LOGGER.info(lobbyTag() + "Player " + player.getPlayerName() + " killed by " + reason + "!");
        if ("bullet".equals(reason) || "freeze".equals(reason)) {
            inventoryManager.dropAllInventoryItems(player);
        }
        addPlayerCorpse(player);
        Network.EntityDied entityDied = new Network.EntityDied(Network.EntityDied.TYPE_PLAYER, player.getClientId());
        entityDied.killerId = killerClientId;
        broadcaster.broadcastToAll(entityDied);
        players.remove(player.getClientId());
        playerInventories.remove(player.getClientId());
        if (players.isEmpty()) {
            closeLobby("empty");
        }
        if ("NPC melee".equals(reason)) {
            broadcastNpcAttack();
        }
    }

    private void broadcastNpcAttack() {
        for (Npc npc : npcManager.getNpcs()) {
            if (npc.isChasing()) {
                broadcaster.broadcastToAll(new Network.NpcAttack(npc.getId()));
            }
        }
    }

    private void onNpcKilledByBullet(Npc npc) {
        LOGGER.info(lobbyTag() + "NPC #" + npc.getId() + " killed!");

        if (npc instanceof RobotBoss) {
            addRobotBossCorpse(npc);
            spawnBossOrbsAroundNpc(npc);
        } else {
            addRobotButlerCorpse(npc);
            itemDropManager.spawnOrbsAroundNpc(npc);
        }

        broadcaster.broadcastToAll(new Network.EntityDied(Network.EntityDied.TYPE_NPC, npc.getId()));

        Network.NpcDeathSound deathSound = new Network.NpcDeathSound(npc.getId(), npc.getPosition().x, npc.getPosition().y);
        broadcaster.broadcastToAll(deathSound);
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

    private void spawnBossOrbsAroundNpc(Npc npc) {
        int dropCount = BOSS_ORB_DROP_MIN + random.nextInt(BOSS_ORB_DROP_MAX - BOSS_ORB_DROP_MIN + 1);
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
            ServerOrb orb = new ServerOrb(orbId, ItemKind.ORB.getId(), x, y, ORB_RADIUS, velocityX, velocityY);
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
        public void onPlayerKilled(LobbyPlayer player, int killerClientId) {
            Lobby.this.onPlayerKilled(player, "bullet", killerClientId);
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
