package taltech.orbitgame.extraction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import taltech.orbitgame.client.GameClient;

/**
 * Handles user input for extraction points.
 * Manages E key presses and communication with server.
 */
public class ExtractionInputController {
    public interface ExtractionListener {
        void onExtractionCompleted();
    }

    private final ExtractionManager extractionManager;
    private final GameClient gameClient;
    private final int localClientId;

    private ExtractionPoint nearbyPoint;
    private ExtractionPoint extractingPoint;
    private float currentPlayerX;
    private float currentPlayerY;
    private boolean eKeyPressed = false;
    private float holdProgressSeconds = 0f;
    private float localRemainingTime = 0f;
    private ExtractionListener extractionListener;

    private static final float EXTRACTION_DURATION_SECONDS = 10f;
    private static final float HOLD_RADIUS_MAX = 48f;
    private static final float MAX_TIMER_STEP_SECONDS = 0.1f;

    public ExtractionInputController(ExtractionManager extractionManager, GameClient gameClient, int localClientId) {
        this.extractionManager = extractionManager;
        this.gameClient = gameClient;
        this.localClientId = localClientId;
    }

    /**
     * Update input handling
     */
    public void update(float delta, float playerX, float playerY) {
        this.currentPlayerX = playerX;
        this.currentPlayerY = playerY;

        // Find nearby extraction point
        ExtractionPoint point = null;
        for (ExtractionPoint p : extractionManager.getAllPoints()) {
            if (p.isPlayerInRange(playerX, playerY)) {
                point = p;
                break;
            }
        }
        this.nearbyPoint = point;

        // Check for E key press
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (!eKeyPressed) {
                handleEKeyPress();
                eKeyPressed = true;
            }
        } else {
            eKeyPressed = false;
        }

        // Handle active extraction lifecycle without pausing world simulation.
        if (extractingPoint != null) {
            if (!isHoldingOnPlatform(extractingPoint, playerX, playerY)) {
                cancelExtraction();
            } else {
                float step = Math.min(Math.max(delta, 0f), MAX_TIMER_STEP_SECONDS);
                holdProgressSeconds = Math.min(EXTRACTION_DURATION_SECONDS, holdProgressSeconds + step);
                localRemainingTime = Math.max(0f, EXTRACTION_DURATION_SECONDS - holdProgressSeconds);
                if (holdProgressSeconds >= EXTRACTION_DURATION_SECONDS) {
                    finishExtraction();
                }
            }
        }
    }

    /**
     * Handle E key press
     */
    private void handleEKeyPress() {
        if (extractingPoint == null
            && nearbyPoint != null
            && nearbyPoint.getCurrentState() == ExtractionPoint.State.AVAILABLE
            && isHoldingOnPlatform(nearbyPoint, currentPlayerX, currentPlayerY)) {
            // Local extraction flow only: server does not process extraction activation yet.
            extractingPoint = nearbyPoint;
            holdProgressSeconds = 0f;
            localRemainingTime = EXTRACTION_DURATION_SECONDS;
            extractingPoint.activate(localClientId);
        }
    }

    /**
     * Cancel extraction when player moves away
     */
    private void cancelExtraction() {
        if (extractingPoint != null) {
            extractingPoint.cancelExtraction();
            extractingPoint = null;
            holdProgressSeconds = 0f;
            localRemainingTime = 0f;
        }
    }

    private void finishExtraction() {
        extractingPoint = null;
        holdProgressSeconds = 0f;
        localRemainingTime = 0f;
        if (extractionListener != null) {
            extractionListener.onExtractionCompleted();
        }
    }

    private boolean isHoldingOnPlatform(ExtractionPoint point, float playerX, float playerY) {
        float dx = point.getPosition().x - playerX;
        float dy = point.getPosition().y - playerY;
        float holdRadius = Math.min(HOLD_RADIUS_MAX, point.getRadius() * 0.55f);
        return dx * dx + dy * dy <= holdRadius * holdRadius;
    }

    /**
     * Get nearby extraction point (for UI)
     */
    public ExtractionPoint getNearbyPoint() {
        return nearbyPoint;
    }

    /**
     * Get extraction point currently being extracted from (for UI)
     */
    public ExtractionPoint getExtractingPoint() {
        return extractingPoint;
    }

    public boolean isExtracting() {
        return extractingPoint != null;
    }

    public float getRemainingTime() {
        return localRemainingTime;
    }

    public void setExtractionListener(ExtractionListener extractionListener) {
        this.extractionListener = extractionListener;
    }

    /**
     * Apply server state update
     */
    public void applyServerUpdate(int extractionPointId, int state, int activatingClientId, float remainingTime) {
        ExtractionPoint point = extractionManager.getPoint(extractionPointId);
        if (point != null) {
            point.applyNetworkState(state, activatingClientId, remainingTime);

            // Update our local extracting point if it's this one
            if (extractingPoint != null && extractingPoint.getId() == extractionPointId) {
                if (ExtractionPoint.State.fromId(state) != ExtractionPoint.State.EXTRACTING) {
                    extractingPoint = null;
                    holdProgressSeconds = 0f;
                    localRemainingTime = 0f;
                }
            }
        }
    }
}

