package server.listener;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import taltech.orbitgame.network.Network;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Listens for player input events from connected clients
 * Handles multiple client connections and echoes input to terminal
 */
public class PlayerInputListener implements Listener {
    private Server server;
    private Map<Integer, String> connectedClients;

    public PlayerInputListener() {
        this.connectedClients = new HashMap<>();
        this.server = new Server();
        Network.register(server); // Use shared registration logic

        server.addListener(this);
    }

    public void startServer(int port) {
        server.start();
        try {
            server.bind(port);
            System.out.println("[Server] Listening on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        server.stop();
    }

    @Override
    public void connected(Connection connection) {
        System.out.println("[Server] New connection from: " + connection.getRemoteAddressTCP());
    }

    @Override
    public void disconnected(Connection connection) {
        String name = connectedClients.remove(connection.getID());
        if (name != null) {
            System.out.println("[Server] Client disconnected: " + name);
        }
    }

    @Override
    public void received(Connection connection, Object object) {
        if (object instanceof Network.Login) {
            Network.Login login = (Network.Login) object;
            String name = login.name + connection.getID(); // Append ID for uniqueness
            connectedClients.put(connection.getID(), name);

            System.out.println("[Server] Client logged in: " + name);

            Network.Registration reg = new Network.Registration();
            reg.id = connection.getID();
            reg.name = name;
            connection.sendTCP(reg);
            return;
        }

        if (object instanceof Network.InputState) {
            Network.InputState state = (Network.InputState) object;

            String name = connectedClients.get(connection.getID());
            if (name == null) name = "Unknown Client " + connection.getID();

            ArrayList<String> pressed = new ArrayList<>();
            if (state.stateGetter("w")) pressed.add("W");
            if (state.stateGetter("a")) pressed.add("A");
            if (state.stateGetter("s")) pressed.add("S");
            if (state.stateGetter("d")) pressed.add("D");
            if (state.stateGetter("left_click")) pressed.add("Left Click");
            if (state.stateGetter("right_click")) pressed.add("Right Click");
            if (!pressed.isEmpty()) {
                String keysStr = String.join(", ", pressed);
                System.out.println(String.format("[Server] %s pressed: %s", name, keysStr));

                // Broadcast to all clients so every player sees what others press
                Network.KeyPressEvent event = new Network.KeyPressEvent(
                    connection.getID(), name, keysStr);
                server.sendToAllTCP(event);
            }
        }
    }
}
