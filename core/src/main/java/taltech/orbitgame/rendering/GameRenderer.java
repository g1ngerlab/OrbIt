package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import taltech.orbitgame.camera.CameraController;
import taltech.orbitgame.inventory.InventoryHudRenderer;
import taltech.orbitgame.inventory.InventoryState;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.player.PlayerController;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.ui.HealthBarRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.world.MapManager;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;
import taltech.orbitgame.network.Network;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GameRenderer {
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final HealthBarRenderer healthBarRenderer;
    private final CameraController cameraController;
    private final BulletRenderer bulletRenderer;
    private final WeaponRenderer weaponRenderer;
    private final CorpseRenderer corpseRenderer;
    private final CursorManager cursorManager;
    private final BitmapFont font;
    private final BitmapFont hudFont;
    private final GlyphLayout layout;
    private final Texture orbTexture;
    private final InventoryHudRenderer inventoryHudRenderer;
    private final OrthographicCamera hudCamera;
    private final CharacterType localCharacterType;

    private static final int CROSSHAIR_SCALE = 2;
    private static final float PLAYER_HEALTH_Y_OFFSET = 8f;
    private static final float ORB_SCALE_AMPLITUDE = 0.05f;
    private static final float ORB_ALPHA_BASE = 0.8f;
    private static final float ORB_ALPHA_AMPLITUDE = 0.2f;
    private static final float ORB_ANIM_SPEED = 2f;
    private static final float ORB_WORLD_SIZE_MULTIPLIER = 1.25f;
    private static final float ORB_DROP_HOP_DURATION = 1.25f;
    private static final float ORB_DROP_HOP_HEIGHT = 14f;
    private static final float ORB_LEVITATION_HEIGHT = 3.5f;
    private float orbAnimTime = 0f;
    private Map<Integer, Network.OrbState> currentOrbs = Collections.emptyMap();
    private final Map<Integer, Float> orbFirstSeenTime = new HashMap<>();
    private InventoryState inventoryState;

    public GameRenderer(CharacterType localCharacterType) {
        this.localCharacterType = localCharacterType == null ? CharacterType.MATH : localCharacterType;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.healthBarRenderer = new HealthBarRenderer(shapeRenderer);

        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        this.cameraController = new CameraController(camera);

        String bulletTexturePath = this.localCharacterType == CharacterType.PUPU
            ? "guns/banaan_bullet2.png"
            : null;
        this.bulletRenderer = new BulletRenderer(bulletTexturePath);
        this.weaponRenderer = new WeaponRenderer();
        this.corpseRenderer = new CorpseRenderer();
        this.cursorManager = new CursorManager("guns/crosshair.png", CROSSHAIR_SCALE);
        this.font = FontManager.getMenuFont();
        this.hudFont = FontManager.getPixelFont(32);
        this.layout = new GlyphLayout();
        this.orbTexture = new Texture(Gdx.files.internal("loot/orb_v1.png"));
        this.inventoryHudRenderer = new InventoryHudRenderer();
        this.hudCamera = new OrthographicCamera();
        this.hudCamera.setToOrtho(false, 800, 600);
        this.inventoryState = new InventoryState();
    }

    public void render(float delta, boolean isInLobby, Player player, Map<Integer, RemotePlayer> remotePlayers,
                      MapManager map, NpcManager npcManager, Map<Integer, Network.CorpseState> corpses,
                      PlayerController playerController) {

        orbAnimTime += delta;

        if (!isInLobby) {
            renderCenteredMessage("Connecting to lobby...");
            return;
        }

        if (map == null || playerController == null) {
            renderCenteredMessage("Loading map...");
            return;
        }

        // Update camera
        cameraController.updateCamera(player, map.getWidthPx(), map.getHeightPx());
        cameraController.getCamera().update();
        batch.setProjectionMatrix(cameraController.getCamera().combined);
        shapeRenderer.setProjectionMatrix(cameraController.getCamera().combined);

        // Render game objects
        batch.enableBlending();
        batch.begin();
        map.render(batch, cameraController.getCamera());
        corpseRenderer.render(batch, corpses);
        renderWorldOrbs(currentOrbs);
        npcManager.draw(batch);

        // Render remote players with weapons
        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.render(batch);
            if (remotePlayer.getCharacterType() != CharacterType.PUPU) {
                weaponRenderer.drawShotgun(
                    batch,
                    remotePlayer.getCenterX(),
                    remotePlayer.getCenterY() + 8f,
                    remotePlayer.getAimX(),
                    remotePlayer.getAimY()
                );
            }
        }

        // Render local player with weapon
        player.render(batch);
        if (player.getCharacterType() != CharacterType.PUPU) {
            weaponRenderer.drawShotgun(
                batch,
                player.getCenterX(),
                player.getCenterY() + 8f,
                playerController.getLocalAimX(),
                playerController.getLocalAimY()
            );
        }

        // Render bullets
        bulletRenderer.render(batch);
        batch.end();

        // Render health bars
        renderHealthBars(player, remotePlayers, npcManager);
        renderHud(delta);
    }

    public void setOrbHudState(Map<Integer, Network.OrbState> orbs) {
        this.currentOrbs = orbs == null ? Collections.emptyMap() : orbs;

        Set<Integer> activeIds = new HashSet<>(this.currentOrbs.keySet());
        orbFirstSeenTime.keySet().removeIf(id -> !activeIds.contains(id));
        for (Integer orbId : activeIds) {
            orbFirstSeenTime.computeIfAbsent(orbId, id -> orbAnimTime);
        }
    }

    private void renderWorldOrbs(Map<Integer, Network.OrbState> orbs) {
        if (orbs == null || orbs.isEmpty()) {
            return;
        }

        for (Network.OrbState orb : orbs.values()) {
            float phase = orbAnimTime * ORB_ANIM_SPEED + orb.orbId * 0.31f;
            float scale = 1f + ORB_SCALE_AMPLITUDE * MathUtils.sin(phase);
            float alpha = ORB_ALPHA_BASE + ORB_ALPHA_AMPLITUDE * MathUtils.sin(phase);

            float firstSeen = orbFirstSeenTime.getOrDefault(orb.orbId, orbAnimTime);
            float age = Math.max(0f, orbAnimTime - firstSeen);
            float hop = 0f;
            if (age < ORB_DROP_HOP_DURATION) {
                float decay = 1f - (age / ORB_DROP_HOP_DURATION);
                hop = ORB_DROP_HOP_HEIGHT * decay * Math.abs(MathUtils.sin(age * 16f));
            }
            float levitation = ORB_LEVITATION_HEIGHT * MathUtils.sin(phase * 0.9f);

            float width = orb.radius * 2f * scale * ORB_WORLD_SIZE_MULTIPLIER;
            float height = orb.radius * 2f * scale * ORB_WORLD_SIZE_MULTIPLIER;
            float drawY = orb.y + hop + levitation;

            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(
                orbTexture,
                orb.x - width * 0.5f,
                drawY - height * 0.5f,
                width,
                height
            );
        }

        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void setInventoryState(InventoryState inventoryState) {
        this.inventoryState = inventoryState == null ? new InventoryState() : inventoryState;
    }

    public int getInventorySlotAtScreenPosition(int screenX, int screenY) {
        return inventoryHudRenderer.getSlotAtScreenPosition(screenX, screenY, inventoryState);
    }

    public boolean isInventoryScreenPositionInside(int screenX, int screenY) {
        return inventoryHudRenderer.isInsidePanel(screenX, screenY, inventoryState);
    }

    private void renderHud(float delta) {
        hudCamera.update();
        batch.setProjectionMatrix(hudCamera.combined);
        inventoryHudRenderer.render(batch, shapeRenderer, hudCamera, hudFont, delta, inventoryState);
    }

    private void renderHealthBars(Player player, Map<Integer, RemotePlayer> remotePlayers, NpcManager npcManager) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Npc npc : npcManager.getNpcs()) {
            if (npc.isAlive()) {
                healthBarRenderer.drawHealthBar(
                    npc.getPosition().x - npc.getSize() / 2,
                    npc.getPosition().y + npc.getSize() / 2,
                    npc.getSize(),
                    npc.getHealthPercentage()
                );
            }
        }

        healthBarRenderer.drawHealthBar(
            player.x,
            player.y + player.getHeight() + PLAYER_HEALTH_Y_OFFSET,
            player.getWidth(),
            player.getHealthPercentage()
        );

        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            healthBarRenderer.drawHealthBar(
                remotePlayer.x,
                remotePlayer.y + remotePlayer.getHeight() + PLAYER_HEALTH_Y_OFFSET,
                remotePlayer.getWidth(),
                remotePlayer.getHealthPercentage()
            );
        }

        shapeRenderer.end();
    }

    public void resize(int width, int height) {
        hudCamera.setToOrtho(false, width, height);
    }

    public void setCrosshairCursor() {
        cursorManager.setCrosshairCursor();
    }

    public void resetCursor() {
        cursorManager.resetCursor();
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public BulletRenderer getBulletRenderer() {
        return bulletRenderer;
    }

    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        bulletRenderer.dispose();
        weaponRenderer.dispose();
        corpseRenderer.dispose();
        cursorManager.dispose();
        hudFont.dispose();
        orbTexture.dispose();
        inventoryHudRenderer.dispose();
    }

    private void renderCenteredMessage(String message) {
        batch.begin();
        font.getData().setScale(1.5f);
        layout.setText(font, message);
        font.draw(batch, message,
            (Gdx.graphics.getWidth() - layout.width) / 2,
            Gdx.graphics.getHeight() / 2f);
        batch.end();
    }

    public void renderStatus(String message) {
        renderCenteredMessage(message);
    }
}
