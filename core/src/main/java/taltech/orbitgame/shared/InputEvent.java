package taltech.orbitgame.shared;

/**
 * Represents player input events that can be sent over the network
 * Shared between client and server
 */
public class InputEvent {

    public int clientId;
    public boolean moveUp;
    public boolean moveDown;
    public boolean moveLeft;
    public boolean moveRight;
    public boolean jump;
    public float mouseX;
    public float mouseY;
    public boolean mouseClickLeft;
    public boolean mouseClickRight;
    public long timestamp;

    // Default constructor for Kryo
    public InputEvent() {
    }

    public InputEvent(int clientId, boolean moveUp, boolean moveDown, boolean moveLeft,
                     boolean moveRight, boolean jump, float mouseX, float mouseY, boolean mouseClickLeft, boolean mouseClickRight) {
        this.clientId = clientId;
        this.moveUp = moveUp;
        this.moveDown = moveDown;
        this.moveLeft = moveLeft;
        this.moveRight = moveRight;
        this.jump = jump;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.mouseClickLeft = mouseClickLeft;
        this.mouseClickRight = mouseClickRight;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Get state of an action
     * Valid states: "moveup", "movedown", "moveleft", "moveright", "jump", "mouseclickleft", "mouseclickright"
     */
    public boolean stateGetter(String state) {
        switch (state.toLowerCase()) {
            case "moveup":
                return moveUp;
            case "movedown":
                return moveDown;
            case "moveleft":
                return moveLeft;
            case "moveright":
                return moveRight;
            case "jump":
                return jump;
            case "mouseclickleft":
                return mouseClickLeft;
            case "mouseclickright":
                return mouseClickRight;
            default:
                return false;
        }
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }
}
