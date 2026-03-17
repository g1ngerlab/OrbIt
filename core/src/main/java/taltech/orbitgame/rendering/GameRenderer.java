package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import taltech.orbitgame.camera.CameraController;
import taltech.orbitgame.npc.Npc;
import taltech.orbitgame.npc.NpcManager;
import taltech.orbitgame.player.PlayerController;
import taltech.orbitgame.ui.HealthBarRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.world.MapManager;
import taltech.orbitgame.world.Player;
import taltech.orbitgame.world.RemotePlayer;
import taltech.orbitgame.network.Network;

import java.util.Map;

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

    private static final int CROSSHAIR_SCALE = 2;

    public GameRenderer() {
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.healthBarRenderer = new HealthBarRenderer(shapeRenderer);

        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        this.cameraController = new CameraController(camera);

        this.bulletRenderer = new BulletRenderer();
        this.weaponRenderer = new WeaponRenderer();
        this.corpseRenderer = new CorpseRenderer();
        this.cursorManager = new CursorManager("guns/crosshair.png", CROSSHAIR_SCALE);
        this.font = FontManager.getMenuFont();
    }

    public void render(float delta, boolean isInLobby, Player player, Map<Integer, RemotePlayer> remotePlayers,
                      MapManager map, NpcManager npcManager, Map<Integer, Network.CorpseState> corpses,
                      PlayerController playerController) {

        if (!isInLobby) {
            batch.begin();
            String message = "Connecting to lobby...";
            font.getData().setScale(1.5f);
            float textWidth = font.draw(batch, message, 0, 0).width;
            font.draw(batch, message,
                (Gdx.graphics.getWidth() - textWidth) / 2,
                Gdx.graphics.getHeight() / 2f);
            batch.end();
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
        map.render(batch);
        corpseRenderer.render(batch, corpses);
        npcManager.draw(batch);

        // Render remote players with weapons
        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            remotePlayer.render(batch);
            weaponRenderer.drawShotgun(
                batch,
                remotePlayer.getCenterX(),
                remotePlayer.getCenterY() + 8f,
                remotePlayer.getAimX(),
                remotePlayer.getAimY()
            );
        }

        // Render local player with weapon
        player.render(batch);
        weaponRenderer.drawShotgun(
            batch,
            player.getCenterX(),
            player.getCenterY() + 8f,
            playerController.getLocalAimX(),
            playerController.getLocalAimY()
        );

        // Render bullets
        bulletRenderer.render(batch);
        batch.end();

        // Render health bars
        renderHealthBars(player, remotePlayers, npcManager);
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
            player.y + player.getHeight(),
            player.getWidth(),
            player.getHealthPercentage()
        );

        for (RemotePlayer remotePlayer : remotePlayers.values()) {
            healthBarRenderer.drawHealthBar(
                remotePlayer.x,
                remotePlayer.y + remotePlayer.getHeight(),
                remotePlayer.getWidth(),
                remotePlayer.getHealthPercentage()
            );
        }

        shapeRenderer.end();
    }

    public void resize(int width, int height) {
        // Handle resize if needed
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
    }
}
