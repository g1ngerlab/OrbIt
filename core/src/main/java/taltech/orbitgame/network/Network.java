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
        kryo.register(JoinLobby.class);
        kryo.register(PlayerPositionUpdate.class);
        kryo.register(PlayerJoined.class);
        kryo.register(PlayerLeft.class);
        kryo.register(LobbyState.class);
        kryo.register(java.util.ArrayList.class);
        kryo.register(PlayerInfo.class);
        kryo.register(LobbyJoinConfirmed.class);
        kryo.register(RequestLobbyInfo.class);
        kryo.register(LobbyInfo.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(NpcState.class);
        kryo.register(NpcStateUpdate.class);
        kryo.register(ShootRequest.class);
        kryo.register(BulletState.class);
        kryo.register(BulletStateUpdate.class);
        kryo.register(CorpseState.class);
        kryo.register(CorpseStateUpdate.class);
        kryo.register(PlayerHealthUpdate.class);
        kryo.register(EntityDied.class);
        kryo.register(CreateLobby.class);
        kryo.register(LobbyCreated.class);
        kryo.register(RequestAllLobbies.class);
        kryo.register(AllLobbiesInfo.class);
        kryo.register(LobbyListItem.class);
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
        public boolean esc;

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

    /**
     * Sent by client to join a specific lobby
     */
    public static class JoinLobby {
        public int lobbyId;

        public JoinLobby() {}

        public JoinLobby(int lobbyId) {
            this.lobbyId = lobbyId;
        }
    }

    /**
     * Sent by client to server with position updates
     */
    public static class PlayerPositionUpdate {
        public int clientId;
        public float x;
        public float y;
        public boolean moving;
        public float aimX;
        public float aimY;

        public PlayerPositionUpdate() {}

        public PlayerPositionUpdate(int clientId, float x, float y, boolean moving, float aimX, float aimY) {
            this.clientId = clientId;
            this.x = x;
            this.y = y;
            this.moving = moving;
            this.aimX = aimX;
            this.aimY = aimY;
        }
    }

    /**
     * Sent by server when a new player joins the lobby
     */
    public static class PlayerJoined {
        public int clientId;
        public String playerName;
        public float x;
        public float y;

        public PlayerJoined() {}

        public PlayerJoined(int clientId, String playerName, float x, float y) {
            this.clientId = clientId;
            this.playerName = playerName;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Sent by server when a player leaves the lobby
     */
    public static class PlayerLeft {
        public int clientId;

        public PlayerLeft() {}

        public PlayerLeft(int clientId) {
            this.clientId = clientId;
        }
    }

    /**
     * Player info for lobby state
     */
    public static class PlayerInfo {
        public int clientId;
        public String playerName;
        public float x;
        public float y;
        public float aimX;
        public float aimY;

        public PlayerInfo() {}

        public PlayerInfo(int clientId, String playerName, float x, float y, float aimX, float aimY) {
            this.clientId = clientId;
            this.playerName = playerName;
            this.x = x;
            this.y = y;
            this.aimX = aimX;
            this.aimY = aimY;
        }
    }

    /**
     * Sent by server to new player joining - contains all existing players
     */
    public static class LobbyState {
        public java.util.ArrayList<PlayerInfo> players;

        public LobbyState() {
            players = new java.util.ArrayList<>();
        }
    }

    /**
     * Sent by server to confirm that the player has fully joined the lobby and can spawn
     */
    public static class LobbyJoinConfirmed {
        public int lobbyId;
        public float spawnX;
        public float spawnY;

        public LobbyJoinConfirmed() {}

        public LobbyJoinConfirmed(int lobbyId, float spawnX, float spawnY) {
            this.lobbyId = lobbyId;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
        }
    }

    /**
     * Sent by client to request info about all lobbies
     */
    public static class RequestLobbyInfo {
        public RequestLobbyInfo() {}
    }

    /**
     * Sent by server with information about all lobbies
     */
    public static class LobbyInfo {
        public java.util.HashMap<Integer, Integer> lobbyPlayerCounts; // lobbyId -> player count

        public LobbyInfo() {
            lobbyPlayerCounts = new java.util.HashMap<>();
        }

        public LobbyInfo(java.util.HashMap<Integer, Integer> counts) {
            this.lobbyPlayerCounts = counts;
        }
    }

    public static class NpcState {
        public int npcId;
        public String type;
        public float x;
        public float y;
        public float size;
        public boolean chasing;
        /** Current health of the NPC (0 = dead, removed from next snapshot) */
        public float health;
        /** Maximum health of the NPC */
        public float maxHealth;

        public NpcState() {}
    }

    public static class NpcStateUpdate {
        public int lobbyId;
        public long tick;
        public java.util.ArrayList<NpcState> npcs;

        public NpcStateUpdate() {
            npcs = new java.util.ArrayList<>();
        }
    }

    public static class ShootRequest {
        public float targetX;
        public float targetY;

        public ShootRequest() {}

        public ShootRequest(float targetX, float targetY) {
            this.targetX = targetX;
            this.targetY = targetY;
        }
    }

    public static class BulletState {
        public int bulletId;
        public float x;
        public float y;
        public float angleDeg;
        public float radius;

        public BulletState() {}
    }

    public static class BulletStateUpdate {
        public int lobbyId;
        public long tick;
        public java.util.ArrayList<BulletState> bullets;

        public BulletStateUpdate() {
            bullets = new java.util.ArrayList<>();
        }
    }

    public static class CorpseState {
        public static final int KIND_PLAYER = 0;
        public static final int KIND_ROBOT_BUTLER = 1;

        public int corpseId;
        public int kind;
        /** Center position in world coordinates. */
        public float x;
        public float y;
        public float width;
        public float height;
        public float rotationDeg;

        public CorpseState() {}
    }

    public static class CorpseStateUpdate {
        public int lobbyId;
        public long tick;
        public java.util.ArrayList<CorpseState> corpses;

        public CorpseStateUpdate() {
            corpses = new java.util.ArrayList<>();
        }
    }

    /**
     * Sent by server to all clients when a player takes damage or is healed.
     * Used to sync health bars in multiplayer.
     */
    public static class PlayerHealthUpdate {
        public int clientId;
        /** Current health points */
        public float health;
        /** Maximum health points */
        public float maxHealth;

        public PlayerHealthUpdate() {}

        public PlayerHealthUpdate(int clientId, float health, float maxHealth) {
            this.clientId = clientId;
            this.health = health;
            this.maxHealth = maxHealth;
        }
    }

    /**
     * Sent by server to all clients when a player or NPC dies and must be removed.
     */
    public static class EntityDied {
        public static final int TYPE_PLAYER = 0;
        public static final int TYPE_NPC = 1;

        /** TYPE_PLAYER or TYPE_NPC */
        public int entityType;
        /** clientId for players, npcId for NPCs */
        public int entityId;

        public EntityDied() {}

        public EntityDied(int entityType, int entityId) {
            this.entityType = entityType;
            this.entityId = entityId;
        }
    }

    /**
     * Sent by client to create a new lobby
     */
    public static class CreateLobby {
        public String lobbyName;

        public CreateLobby() {}

        public CreateLobby(String lobbyName) {
            this.lobbyName = lobbyName;
        }
    }

    /**
     * Sent by server to confirm lobby creation and provide lobby ID
     */
    public static class LobbyCreated {
        public int lobbyId;
        public String lobbyName;

        public LobbyCreated() {}

        public LobbyCreated(int lobbyId, String lobbyName) {
            this.lobbyId = lobbyId;
            this.lobbyName = lobbyName;
        }
    }

    /**
     * Sent by client to request list of all available lobbies
     */
    public static class RequestAllLobbies {
        public RequestAllLobbies() {}
    }

    /**
     * Information about a single lobby
     */
    public static class LobbyListItem {
        public int lobbyId;
        public String lobbyName;
        public int playerCount;

        public LobbyListItem() {}

        public LobbyListItem(int lobbyId, String lobbyName, int playerCount) {
            this.lobbyId = lobbyId;
            this.lobbyName = lobbyName;
            this.playerCount = playerCount;
        }
    }

    /**
     * Sent by server with list of all available lobbies
     */
    public static class AllLobbiesInfo {
        public java.util.ArrayList<LobbyListItem> lobbies;

        public AllLobbiesInfo() {
            lobbies = new java.util.ArrayList<>();
        }
    }
}
