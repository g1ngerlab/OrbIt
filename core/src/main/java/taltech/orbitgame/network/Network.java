package taltech.orbitgame.network;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.EndPoint;

// This class will be shared by both the client and server.
public class Network {
    public static final int PORT = 54555;

    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();
        kryo.register(Login.class);
        kryo.register(Registration.class);
        kryo.register(MoveRequest.class);
        kryo.register(InputState.class);
        kryo.register(KeyPressEvent.class);
    }

    public static class Login {
        public String name;
    }

    public static class Registration {
        public int id;
        public String name;
    }

    public static class MoveRequest {
        public int x, y;
    }

    // Example of input state for echoing
    public static class InputState {
        public boolean w, a, s, d;
        public boolean mouseClicked;
        public boolean leftClick, rightClick;
        public int mouseX, mouseY;
        public int clientId;

        public boolean stateGetter(String key) {
            switch (key.toLowerCase()) {
                case "w": return w;
                case "a": return a;
                case "s": return s;
                case "d": return d;
                case "mouseclicked": return mouseClicked;
                case "left_click": return leftClick;
                case "right_click": return rightClick;
                default: return false;
            }
        }
    }

    /**
     * Sent by the server to all clients when a player presses a key.
     */
    public static class KeyPressEvent {
        public int clientId;
        public String playerName;
        public String keys; // e.g. "W, A"

        public KeyPressEvent() {}

        public KeyPressEvent(int clientId, String playerName, String keys) {
            this.clientId = clientId;
            this.playerName = playerName;
            this.keys = keys;
        }
    }
}
