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
        client.start();

        // Register classes
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
                    System.out.println("[Client] Registered as: " + reg.name);
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("[Client] Disconnected from server.");
            }
        });
    }

    public void connect() {
        new Thread(() -> {
            try {
                client.connect(5000, "localhost", Network.PORT);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void sendInput(InputState state) {
        if (client.isConnected()) {
            client.sendTCP(state);
        }
    }
}
