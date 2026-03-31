package taltech.orbitgame.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.InputHandler;
import taltech.orbitgame.network.Network.InputState;
import taltech.orbitgame.world.Direction;
import taltech.orbitgame.world.MapManager;
import taltech.orbitgame.world.Player;

/**
 * Handles player movement, input processing, and shooting
 */
public class PlayerController {

    private static final float SHOT_COOLDOWN = 0.3f;
    private static final float UPDATE_INTERVAL = 0.05f; // Send position updates every 50ms

    private final Player player;
    private final InputHandler inputHandler;
    private final GameClient gameClient;
    private final MapManager map;
    private final OrthographicCamera camera;
    private final float speed;

    private float localAimX;
    private float localAimY;
    private float shotCooldownTimer = 0f;
    private float timeSinceLastUpdate = 0f;
    private boolean shootingBlocked = false;

    private final Vector3 mouseScreen;
    private final Vector3 mouseWorld;

    public PlayerController(Player player, InputHandler inputHandler, GameClient gameClient,
                           MapManager map, OrthographicCamera camera, float speed) {
        this.player = player;
        this.inputHandler = inputHandler;
        this.gameClient = gameClient;
        this.map = map;
        this.camera = camera;
        this.speed = speed;

        this.mouseScreen = new Vector3();
        this.mouseWorld = new Vector3();

        // Initialize aim position
        this.localAimX = player.getCenterX() + 1f;
        this.localAimY = player.getCenterY();
    }

    /**
     * Update player position, aim, and handle shooting
     */
    public void update(float delta, boolean isInLobby) {
        InputState state = inputHandler.getState();
        boolean moving = false;

        updateAimFromMouse();

        // Calculate next position
        float nextX = player.x;
        float nextY = player.y;

        if (state.d) {
            nextX += speed * delta;
            moving = true;
            player.setDirection(Direction.RIGHT);
        }
        if (state.a) {
            nextX -= speed * delta;
            moving = true;
            player.setDirection(Direction.LEFT);
        }
        if (state.w) {
            nextY += speed * delta;
            moving = true;
            player.setDirection(Direction.UP);
        }
        if (state.s) {
            nextY -= speed * delta;
            moving = true;
            player.setDirection(Direction.DOWN);
        }

        // Apply movement if valid
        if (canMoveTo(nextX, player.y)) {
            player.x = nextX;
        }
        if (canMoveTo(player.x, nextY)) {
            player.y = nextY;
        }

        // Handle shooting cooldown
        if (shotCooldownTimer > 0f) {
            shotCooldownTimer -= delta;
        }

        // Handle shooting
        if (!shootingBlocked
            && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)
            && shotCooldownTimer <= 0f
            && isInLobby) {
            gameClient.sendShoot(localAimX, localAimY);
            // Play shotgun sound for own shot at full volume
            AudioManager.getInstance().playShotgunShot(player.getCenterX(), player.getCenterY(),
                                                       player.getCenterX(), player.getCenterY(), true);
            shotCooldownTimer = SHOT_COOLDOWN;
        }

        player.update(delta, moving);
    }

    /**
     * Send input state to server if active
     */
    public void sendInputToServer(boolean isInLobby) {
        if (gameClient != null && inputHandler.hasActiveInput() && isInLobby) {
            gameClient.sendInput(inputHandler.getState());
        }
    }

    /**
     * Send position updates to server periodically
     */
    public void sendPositionUpdate(float delta, boolean isInLobby) {
        if (!isInLobby) {
            return;
        }

        timeSinceLastUpdate += delta;
        if (timeSinceLastUpdate >= UPDATE_INTERVAL) {
            InputState state = inputHandler.getState();
            boolean moving = state.w || state.a || state.s || state.d;
            gameClient.sendPosition(player.x, player.y, moving, localAimX, localAimY);
            timeSinceLastUpdate = 0f;
        }
    }

    /**
     * Update aim position from mouse cursor
     */
    private void updateAimFromMouse() {
        mouseScreen.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        mouseWorld.set(mouseScreen);
        camera.unproject(mouseWorld);
        localAimX = mouseWorld.x;
        localAimY = mouseWorld.y;
    }

    /**
     * Check if player can move to the specified position (collision detection)
     */
    private boolean canMoveTo(float x, float y) {
        int width = player.getWidth();
        int height = player.getHeight();
        float right = x + width - 1;
        float top = y + height - 1;

        return !map.isBlocked(x, y)
            && !map.isBlocked(right, y)
            && !map.isBlocked(x, top)
            && !map.isBlocked(right, top);
    }

    /**
     * Reset aim position when spawning
     */
    public void resetAim() {
        this.localAimX = player.getCenterX() + 1f;
        this.localAimY = player.getCenterY();
    }

    public float getLocalAimX() {
        return localAimX;
    }

    public float getLocalAimY() {
        return localAimY;
    }

    public void setShootingBlocked(boolean blocked) {
        this.shootingBlocked = blocked;
    }
}
