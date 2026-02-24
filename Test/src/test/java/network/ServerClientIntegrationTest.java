package network;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import org.junit.jupiter.api.*;
import server.listener.PlayerInputListener;
import taltech.orbitgame.network.Network;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ServerClientIntegrationTest {
    private static final int PORT = 54555;
    private static PlayerInputListener server;
    private Client client;
    @BeforeAll
    static void startServer() {
        server = new PlayerInputListener();
        server.startServer(PORT);
    }
    @AfterAll
    static void stopServer() {
        if (server != null) server.stopServer();
    }
    @AfterEach
    void tearDownClient() throws Exception {
        if (client != null) {
            client.stop();
            client = null;
        }
        Thread.sleep(300);
    }
    @Test
    @Order(1)
    void testClientReceivesRegistrationAfterLogin() throws Exception {
        CountDownLatch registered = new CountDownLatch(1);
        AtomicReference<String> receivedName = new AtomicReference<>();
        client = new Client();
        Network.register(client);
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Network.Login login = new Network.Login();
                login.name = "TestPlayer";
                client.sendTCP(login);
            }
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.Registration) {
                    Network.Registration reg = (Network.Registration) object;
                    receivedName.set(reg.name);
                    registered.countDown();
                }
            }
        });
        client.start();
        new Thread(() -> {
            try { client.connect(3000, "localhost", PORT); }
            catch (Exception e) { e.printStackTrace(); }
        }).start();
        assertTrue(registered.await(5, TimeUnit.SECONDS),
            "Client should receive Registration within 5 seconds");
        assertTrue(receivedName.get().startsWith("TestPlayer"),
            "Registered name should start with TestPlayer, got: " + receivedName.get());
        System.out.println("[TEST] Registration OK: " + receivedName.get());
    }
    @Test
    @Order(2)
    void testServerBroadcastsKeyPressEventToClient() throws Exception {
        CountDownLatch registeredLatch = new CountDownLatch(1);
        CountDownLatch keyPressLatch = new CountDownLatch(1);
        AtomicReference<Network.KeyPressEvent> receivedEvent = new AtomicReference<>();
        client = new Client();
        Network.register(client);
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Network.Login login = new Network.Login();
                login.name = "Player";
                client.sendTCP(login);
            }
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.Registration) {
                    registeredLatch.countDown();
                }
                if (object instanceof Network.KeyPressEvent) {
                    receivedEvent.set((Network.KeyPressEvent) object);
                    keyPressLatch.countDown();
                }
            }
        });
        client.start();
        new Thread(() -> {
            try { client.connect(3000, "localhost", PORT); }
            catch (Exception e) { e.printStackTrace(); }
        }).start();
        assertTrue(registeredLatch.await(5, TimeUnit.SECONDS), "Should register within 5s");
        Network.InputState state = new Network.InputState();
        state.w = true;
        client.sendTCP(state);
        assertTrue(keyPressLatch.await(5, TimeUnit.SECONDS),
            "Client should receive KeyPressEvent within 5 seconds");
        Network.KeyPressEvent event = receivedEvent.get();
        assertNotNull(event, "KeyPressEvent must not be null");
        assertTrue(event.keys.contains("W"),
            "KeyPressEvent should contain W, got: " + event.keys);
        System.out.println("[TEST] KeyPressEvent OK - player=" + event.playerName + " keys=" + event.keys);
    }
}