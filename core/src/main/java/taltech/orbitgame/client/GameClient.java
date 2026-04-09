package taltech.orbitgame.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import taltech.orbitgame.player.PlayerProfile;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.network.Network.InputState;
import taltech.orbitgame.network.NetworkConfig;

import java.io.IOException;

public class GameClient {
    private Client client;
    private int clientId = -1;
    private int lobbyId = 1; // Default lobby
    private final String playerName;
    private final String serverHost;
    private final int serverPort;
    private GameClientListener gameListener;

    public interface GameClientListener {
        void onPlayerJoined(int clientId, String playerName, float x, float y, int characterId);
        void onPlayerLeft(int clientId);
        void onPlayerPositionUpdate(int clientId, float x, float y, boolean moving, float aimX, float aimY);
        void onLobbyState(Network.LobbyState lobbyState);
        void onLobbyJoinConfirmed(float spawnX, float spawnY);
        void onLobbyTimerState(Network.LobbyTimerState timerState);
        void onLobbyClosed(Network.LobbyClosed lobbyClosed);
        void onNpcStateUpdate(Network.NpcStateUpdate npcStateUpdate);
        void onNpcAttack(Network.NpcAttack npcAttack);
        void onBulletStateUpdate(Network.BulletStateUpdate bulletStateUpdate);
        void onCorpseStateUpdate(Network.CorpseStateUpdate corpseStateUpdate);
        void onOrbStateUpdate(Network.OrbStateUpdate orbStateUpdate);
        void onInventoryStateUpdate(Network.InventoryStateUpdate inventoryUpdate);
        /** Called when a player's health changes (used to sync health bars). */
        void onPlayerHealthUpdate(Network.PlayerHealthUpdate healthUpdate);
        /** Called when a player or NPC dies and must be removed from the world. */
        void onEntityDied(Network.EntityDied entityDied);
        /** Called when a player shoots. */
        void onPlayerShoot(Network.PlayerShoot playerShoot);
        /** Called when a player takes a step. */
        void onPlayerStep(Network.PlayerStep playerStep);
        /** Called when an NPC dies. */
        void onNpcDeathSound(Network.NpcDeathSound npcDeathSound);
        /** Called when an NPC hits a player. */
        void onNpcHitSound(Network.NpcHitSound npcHitSound);
        /** Called when an extraction point state changes. */
        void onExtractionPointStateUpdate(Network.ExtractionPointStateUpdate stateUpdate);
    }

    public GameClient(int lobbyId, String playerName) {
        this.lobbyId = lobbyId;
        this.playerName = sanitizeName(playerName);
        this.serverHost = NetworkConfig.getServerHost();
        this.serverPort = NetworkConfig.getServerPort();
        client = new Client();
        Network.register(client);
        System.out.println("[Client] GameClient created for lobby " + lobbyId + ", host=" + serverHost
            + ", port=" + serverPort + ", player=" + this.playerName);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("[Client] Connected to server. Sending login...");
                Network.Login login = new Network.Login();
                login.name = GameClient.this.playerName;
                client.sendTCP(login);
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.Registration) {
                    Network.Registration reg = (Network.Registration) object;
                    clientId = reg.id;
                    System.out.println("[Client] Registered as: " + reg.name + " (id=" + clientId + ")");

                    // Join the lobby after registration
                    System.out.println("[Client] Requesting to join lobby " + lobbyId);
                    Network.JoinLobby joinLobby = new Network.JoinLobby(
                        lobbyId,
                        PlayerProfile.getSelectedCharacterId()
                    );
                    client.sendTCP(joinLobby);
                }
                if (object instanceof Network.LobbyState) {
                    Network.LobbyState lobbyState = (Network.LobbyState) object;
                    System.out.println("[Client] Received lobby state with " + lobbyState.players.size() + " players");
                    if (gameListener != null) {
                        gameListener.onLobbyState(lobbyState);
                    }
                }
                if (object instanceof Network.PlayerJoined) {
                    Network.PlayerJoined joined = (Network.PlayerJoined) object;
                    System.out.println("[Client] Player joined: " + joined.playerName + " (id=" + joined.clientId + ")");
                    if (gameListener != null && joined.clientId != clientId) {
                        gameListener.onPlayerJoined(joined.clientId, joined.playerName, joined.x, joined.y,
                            joined.characterId);
                    }
                }
                if (object instanceof Network.PlayerLeft) {
                    Network.PlayerLeft left = (Network.PlayerLeft) object;
                    System.out.println("[Client] Player left: id=" + left.clientId);
                    if (gameListener != null) {
                        gameListener.onPlayerLeft(left.clientId);
                    }
                }
                if (object instanceof Network.PlayerPositionUpdate) {
                    Network.PlayerPositionUpdate update = (Network.PlayerPositionUpdate) object;
                    if (gameListener != null && update.clientId != clientId) {
                        gameListener.onPlayerPositionUpdate(update.clientId, update.x, update.y, update.moving, update.aimX, update.aimY);
                    }
                }
                if (object instanceof Network.LobbyJoinConfirmed) {
                    Network.LobbyJoinConfirmed confirmed = (Network.LobbyJoinConfirmed) object;
                    System.out.println("[Client] Lobby join confirmed at spawn (" + confirmed.spawnX + ", " + confirmed.spawnY + ")");
                    if (gameListener != null) {
                        gameListener.onLobbyJoinConfirmed(confirmed.spawnX, confirmed.spawnY);
                    }
                }
                if (object instanceof Network.LobbyTimerState) {
                    Network.LobbyTimerState timerState = (Network.LobbyTimerState) object;
                    if (gameListener != null) {
                        gameListener.onLobbyTimerState(timerState);
                    }
                }
                if (object instanceof Network.LobbyClosed) {
                    Network.LobbyClosed lobbyClosed = (Network.LobbyClosed) object;
                    if (gameListener != null) {
                        gameListener.onLobbyClosed(lobbyClosed);
                    }
                }
                if (object instanceof Network.NpcStateUpdate) {
                    Network.NpcStateUpdate update = (Network.NpcStateUpdate) object;
                    if (gameListener != null) {
                        gameListener.onNpcStateUpdate(update);
                    }
                }
                if (object instanceof Network.NpcAttack) {
                    Network.NpcAttack attack = (Network.NpcAttack) object;
                    if (gameListener != null) {
                        gameListener.onNpcAttack(attack);
                    }
                }
                if (object instanceof Network.BulletStateUpdate) {
                    Network.BulletStateUpdate update = (Network.BulletStateUpdate) object;
                    if (gameListener != null) {
                        gameListener.onBulletStateUpdate(update);
                    }
                }
                if (object instanceof Network.CorpseStateUpdate) {
                    Network.CorpseStateUpdate update = (Network.CorpseStateUpdate) object;
                    if (gameListener != null) {
                        gameListener.onCorpseStateUpdate(update);
                    }
                }
                if (object instanceof Network.OrbStateUpdate) {
                    Network.OrbStateUpdate update = (Network.OrbStateUpdate) object;
                    if (gameListener != null) {
                        gameListener.onOrbStateUpdate(update);
                    }
                }
                if (object instanceof Network.InventoryStateUpdate) {
                    Network.InventoryStateUpdate update = (Network.InventoryStateUpdate) object;
                    if (gameListener != null) {
                        gameListener.onInventoryStateUpdate(update);
                    }
                }
                if (object instanceof Network.PlayerHealthUpdate) {
                    Network.PlayerHealthUpdate update = (Network.PlayerHealthUpdate) object;
                    if (gameListener != null) {
                        gameListener.onPlayerHealthUpdate(update);
                    }
                }
                if (object instanceof Network.EntityDied) {
                    Network.EntityDied died = (Network.EntityDied) object;
                    if (gameListener != null) {
                        gameListener.onEntityDied(died);
                    }
                }
                if (object instanceof Network.PlayerShoot) {
                    Network.PlayerShoot playerShoot = (Network.PlayerShoot) object;
                    if (gameListener != null) {
                        gameListener.onPlayerShoot(playerShoot);
                    }
                }
                if (object instanceof Network.PlayerStep) {
                    Network.PlayerStep playerStep = (Network.PlayerStep) object;
                    if (gameListener != null) {
                        gameListener.onPlayerStep(playerStep);
                    }
                }
                if (object instanceof Network.NpcDeathSound) {
                    Network.NpcDeathSound npcDeathSound = (Network.NpcDeathSound) object;
                    if (gameListener != null) {
                        gameListener.onNpcDeathSound(npcDeathSound);
                    }
                }
                if (object instanceof Network.NpcHitSound) {
                    Network.NpcHitSound npcHitSound = (Network.NpcHitSound) object;
                    if (gameListener != null) {
                        gameListener.onNpcHitSound(npcHitSound);
                    }
                }
                if (object instanceof Network.ExtractionPointStateUpdate) {
                    Network.ExtractionPointStateUpdate update = (Network.ExtractionPointStateUpdate) object;
                    if (gameListener != null) {
                        gameListener.onExtractionPointStateUpdate(update);
                    }
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("[Client] Disconnected from server.");
            }
        });
    }

    public void setGameListener(GameClientListener listener) {
        this.gameListener = listener;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void connect() {
        client.start();

        new Thread(() -> {
            try {
                client.connect(5000, serverHost, serverPort);
            } catch (IOException e) {
                System.err.println("[Client] Connection failed: " + e.getMessage());
            }
        }, "Client-Connect").start();
    }

    public void sendInput(InputState state) {
        if (client.isConnected()) {
            client.sendTCP(state);
        }
    }

    public void sendPosition(float x, float y, boolean moving, float aimX, float aimY) {
        if (client.isConnected() && clientId != -1) {
            Network.PlayerPositionUpdate update = new Network.PlayerPositionUpdate(clientId, x, y, moving, aimX, aimY);
            client.sendTCP(update);
        }
    }

    public void sendShoot(float targetX, float targetY) {
        if (client.isConnected() && clientId != -1) {
            client.sendTCP(new Network.ShootRequest(targetX, targetY));
        }
    }

    public void sendDropItem(int slotIndex) {
        if (client.isConnected() && clientId != -1) {
            client.sendTCP(new Network.DropItemRequest(slotIndex));
        }
    }

    public void sendMoveInventoryItem(int fromSlotIndex, int toSlotIndex) {
        if (client.isConnected() && clientId != -1) {
            client.sendTCP(new Network.MoveInventoryItemRequest(fromSlotIndex, toSlotIndex));
        }
    }

    public void sendToServer(Object packet) {
        if (client != null && client.isConnected()) {
            client.sendTCP(packet);
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public int getClientId() {
        return clientId;
    }

    public void disconnect() {
        if (client != null && client.isConnected()) {
            System.out.println("[GameClient] Disconnecting...");
            try {
                // Stop the client in a separate thread to avoid blocking
                new Thread(() -> {
                    try {
                        client.stop();
                    } catch (Exception e) {
                        System.err.println("[GameClient] Error stopping client: " + e.getMessage());
                    }
                }, "ClientDisconnectThread").start();

                // Close immediately without waiting
                client.close();
            } catch (Exception e) {
                System.err.println("[GameClient] Error disconnecting: " + e.getMessage());
            }
        }
    }

    private String sanitizeName(String rawName) {
        String normalized = rawName == null ? "" : rawName.trim();
        if (normalized.isEmpty()) {
            normalized = "Player";
        }
        if (normalized.length() > 16) {
            normalized = normalized.substring(0, 16);
        }
        return normalized;
    }
}
