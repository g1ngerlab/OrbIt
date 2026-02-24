package taltech.orbitgame.client;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.network.Network.InputState;

import java.io.IOException;

public class GameClient {
    private Client client;
    private int clientId = -1;

    public GameClient() {
        client = new Client();
        Network.register(client);

        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("[Client] Connected to server.");
                Network.Login login = new Network.Login();
                login.name = "Player";
                client.sendTCP(login);
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.Registration) {
                    Network.Registration reg = (Network.Registration) object;
                    clientId = reg.id;
                    System.out.println("[Client] Registered as: " + reg.name + " (id=" + clientId + ")");
                }
                if (object instanceof Network.KeyPressEvent) {
                    Network.KeyPressEvent event = (Network.KeyPressEvent) object;
                    System.out.println("[Client] " + event.playerName + " pressed: " + event.keys);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("[Client] Disconnected from server.");
            }
        });
    }

    public void connect() {
        // client.start() launches the Kryonet update loop in its own thread.
        // client.connect() must then be called from a DIFFERENT thread — never
        // from the Kryonet update thread itself.
        client.start(); // starts the "Client" update-loop thread

        new Thread(() -> {
            try {
                client.connect(5000, "localhost", Network.PORT);
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

    public boolean isConnected() {
        return client != null && client.isConnected();
    }
}

