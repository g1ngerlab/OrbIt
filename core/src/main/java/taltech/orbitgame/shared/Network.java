package taltech.orbitgame.shared;

import com.esotericsoftware.kryo.Kryo;

public class Network {
    public static final int PORT = 54555;

    public static void register(Kryo kryo) {
        // Register with explicit ID to ensure consistency between client/server
        kryo.register(InputEvent.class, 10);
    }
}
