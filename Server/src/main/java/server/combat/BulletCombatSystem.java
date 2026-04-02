package server.combat;

import com.badlogic.gdx.math.Vector2;
import server.math.CollisionMath;
import server.model.LobbyPlayer;
import server.model.PendingShot;
import server.model.ServerBullet;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.world.MapCollisionGrid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class BulletCombatSystem {
    public interface Callbacks {
        void onPlayerDamaged(LobbyPlayer player);
        void onPlayerKilled(LobbyPlayer player);
        void onNpcKilled(Npc npc);
    }

    private final float playerWidth;
    private final float playerHeight;
    private final float bulletSpeed;
    private final float bulletRadius;
    private final float bulletLifetime;
    private final float bulletDamage;

    public BulletCombatSystem(float playerWidth, float playerHeight,
                              float bulletSpeed, float bulletRadius,
                              float bulletLifetime, float bulletDamage) {
        this.playerWidth = playerWidth;
        this.playerHeight = playerHeight;
        this.bulletSpeed = bulletSpeed;
        this.bulletRadius = bulletRadius;
        this.bulletLifetime = bulletLifetime;
        this.bulletDamage = bulletDamage;
    }

    public int spawnPendingShots(Queue<PendingShot> pendingShots,
                                 Map<Integer, LobbyPlayer> players,
                                 Map<Integer, ServerBullet> bullets,
                                 MapCollisionGrid collisionGrid,
                                 int nextBulletId) {
        PendingShot shot;
        while ((shot = pendingShots.poll()) != null) {
            LobbyPlayer owner = players.get(shot.getOwnerClientId());
            if (owner == null) {
                continue;
            }

            Vector2 origin = new Vector2(owner.getX() + playerWidth * 0.5f, owner.getY() + playerHeight * 0.5f);
            Vector2 direction = new Vector2(shot.getTargetX() - origin.x, shot.getTargetY() - origin.y);
            if (direction.isZero(0.001f)) {
                direction.set(1f, 0f);
            } else {
                direction.nor();
            }

            Vector2 spawnPoint = new Vector2(origin).mulAdd(direction, 18f);
            if (collisionGrid.isBlocked(spawnPoint.x, spawnPoint.y)) {
                continue;
            }

            Vector2 velocity = new Vector2(direction).scl(bulletSpeed);
            int bulletId = nextBulletId++;
            int bulletType = owner.getCharacterId() == 2 ? 2 : 1;
            bullets.put(bulletId, new ServerBullet(bulletId, shot.getOwnerClientId(), bulletType,
                spawnPoint, velocity, bulletLifetime));
        }

        return nextBulletId;
    }

    public void updateBullets(float deltaTime,
                              Map<Integer, ServerBullet> bullets,
                              Map<Integer, LobbyPlayer> players,
                              NpcManager npcManager,
                              MapCollisionGrid collisionGrid,
                              Callbacks callbacks) {
        List<Integer> toRemove = new ArrayList<>();

        for (ServerBullet bullet : bullets.values()) {
            bullet.getPosition().mulAdd(bullet.getVelocity(), deltaTime);
            bullet.tickLifetime(deltaTime);

            if (isExpiredOrBlocked(bullet, collisionGrid)
                || hitsAnyPlayer(bullet, players, callbacks)
                || hitsAnyNpc(bullet, npcManager, callbacks)) {
                toRemove.add(bullet.getBulletId());
            }
        }

        for (Integer bulletId : toRemove) {
            bullets.remove(bulletId);
        }
    }

    public Network.BulletStateUpdate createSnapshot(int lobbyId, long tick, Map<Integer, ServerBullet> bullets) {
        Network.BulletStateUpdate update = new Network.BulletStateUpdate();
        update.lobbyId = lobbyId;
        update.tick = tick;

        for (ServerBullet bullet : bullets.values()) {
            Network.BulletState state = new Network.BulletState();
            state.bulletId = bullet.getBulletId();
            state.x = bullet.getPosition().x;
            state.y = bullet.getPosition().y;
            state.radius = bulletRadius;
            state.angleDeg = bullet.getVelocity().angleDeg();
            state.bulletType = bullet.getBulletType();
            update.bullets.add(state);
        }

        return update;
    }

    private boolean isExpiredOrBlocked(ServerBullet bullet, MapCollisionGrid collisionGrid) {
        return bullet.getTimeRemaining() <= 0f
            || collisionGrid.isBlocked(bullet.getPosition().x, bullet.getPosition().y);
    }

    private boolean hitsAnyPlayer(ServerBullet bullet,
                                  Map<Integer, LobbyPlayer> players,
                                  Callbacks callbacks) {
        for (LobbyPlayer player : players.values()) {
            if (player.getClientId() == bullet.getOwnerClientId() || player.isDead()) {
                continue;
            }

            boolean hit = CollisionMath.circleIntersectsRect(
                bullet.getPosition().x,
                bullet.getPosition().y,
                bulletRadius,
                player.getX(),
                player.getY(),
                playerWidth,
                playerHeight
            );

            if (hit) {
                player.takeDamage(bulletDamage);
                callbacks.onPlayerDamaged(player);
                if (player.isDead()) {
                    callbacks.onPlayerKilled(player);
                }
                return true;
            }
        }
        return false;
    }

    private boolean hitsAnyNpc(ServerBullet bullet,
                               NpcManager npcManager,
                               Callbacks callbacks) {
        for (Npc npc : npcManager.getNpcs()) {
            if (!npc.isAlive()) {
                continue;
            }

            float half = npc.getSize() * 0.5f;
            boolean hit = CollisionMath.circleIntersectsRect(
                bullet.getPosition().x,
                bullet.getPosition().y,
                bulletRadius,
                npc.getPosition().x - half,
                npc.getPosition().y - half,
                npc.getSize(),
                npc.getSize()
            );

            if (hit) {
                npc.takeDamage(bulletDamage);
                if (npc.isDead()) {
                    callbacks.onNpcKilled(npc);
                }
                return true;
            }
        }
        return false;
    }
}

