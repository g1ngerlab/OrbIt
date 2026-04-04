package taltech.orbitgame.extraction;

import com.badlogic.gdx.math.Vector2;

/**
 * Represents a single extraction point on the map.
 * Handles state transitions and activation logic.
 */
public class ExtractionPoint {
    public enum State {
        AVAILABLE(0),           // Ready to use
        EXTRACTING(1),          // Player is extracting
        BLOCKED(2);             // Temporarily blocked after failed extraction

        public final int id;

        State(int id) {
            this.id = id;
        }

        public static State fromId(int id) {
            for (State state : State.values()) {
                if (state.id == id) return state;
            }
            return AVAILABLE;
        }
    }

    private final int id;
    private final Vector2 position;
    private final float radius;

    private State currentState = State.AVAILABLE;
    private int activatingClientId = -1;
    private float activationTimer = 0f;
    private float blockTimer = 0f;

    private static final float EXTRACTION_TIME = 10f;    // 10 seconds to extract
    private static final float BLOCK_TIME = 30f;          // 30 seconds blocked after cancel

    public ExtractionPoint(int id, float x, float y, float radius) {
        this.id = id;
        this.position = new Vector2(x, y);
        this.radius = radius;
    }

    public void update(float deltaTime) {
        if (currentState == State.EXTRACTING) {
            activationTimer += deltaTime;
            if (activationTimer >= EXTRACTION_TIME) {
                // Extraction complete - player exits
                extractionComplete();
            }
        } else if (currentState == State.BLOCKED) {
            blockTimer += deltaTime;
            if (blockTimer >= BLOCK_TIME) {
                // Block time expired
                currentState = State.AVAILABLE;
                blockTimer = 0f;
                activatingClientId = -1;
            }
        }
    }

    /**
     * Called when a player attempts to activate this extraction point
     */
    public boolean activate(int clientId) {
        if (currentState != State.AVAILABLE) {
            return false;
        }
        currentState = State.EXTRACTING;
        activatingClientId = clientId;
        activationTimer = 0f;
        return true;
    }

    /**
     * Called when a player cancels extraction (moves away or manually cancels)
     */
    public void cancelExtraction() {
        if (currentState == State.EXTRACTING) {
            // Cancel only resets progress; point stays immediately reusable.
            currentState = State.AVAILABLE;
            activatingClientId = -1;
            activationTimer = 0f;
            blockTimer = 0f;
        }
    }

    /**
     * Called when extraction completes successfully
     */
    private void extractionComplete() {
        currentState = State.AVAILABLE;
        activatingClientId = -1;
        activationTimer = 0f;
        blockTimer = 0f;
    }

    // Getters
    public int getId() {
        return id;
    }

    public Vector2 getPosition() {
        return position;
    }

    public float getRadius() {
        return radius;
    }

    public State getCurrentState() {
        return currentState;
    }

    public int getActivatingClientId() {
        return activatingClientId;
    }

    public float getActivationTimer() {
        return activationTimer;
    }

    public float getBlockTimer() {
        return blockTimer;
    }

    public float getExtractionTime() {
        return EXTRACTION_TIME;
    }

    public float getBlockTime() {
        return BLOCK_TIME;
    }

    public boolean isPlayerInRange(float playerX, float playerY) {
        float distX = position.x - playerX;
        float distY = position.y - playerY;
        float distSquared = distX * distX + distY * distY;
        return distSquared <= radius * radius;
    }

    /**
     * Applies state update from server
     */
    public void applyNetworkState(int stateId, int activatingClientId, float remainingTime) {
        this.currentState = State.fromId(stateId);
        this.activatingClientId = activatingClientId;

        if (currentState == State.EXTRACTING) {
            this.activationTimer = EXTRACTION_TIME - remainingTime;
        } else if (currentState == State.BLOCKED) {
            this.blockTimer = BLOCK_TIME - remainingTime;
        }
    }
}

