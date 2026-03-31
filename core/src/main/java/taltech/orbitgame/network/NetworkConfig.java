package taltech.orbitgame.network;

import taltech.orbitgame.config.OrbitConfig;

/**
 * Runtime network configuration loaded from JVM properties or environment.
 */
public final class NetworkConfig {
    private static final String HOST_PROPERTY = "orbit.server.host";
    private static final String HOST_ENV = "ORBIT_SERVER_HOST";
    private static final String PORT_PROPERTY = "orbit.server.port";
    private static final String PORT_ENV = "ORBIT_SERVER_PORT";
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 54555;

    private NetworkConfig() {
    }

    public static String getServerHost() {
        return OrbitConfig.get(HOST_PROPERTY, HOST_ENV, DEFAULT_HOST);
    }

    public static int getServerPort() {
        String rawValue = OrbitConfig.get(PORT_PROPERTY, PORT_ENV, String.valueOf(DEFAULT_PORT));
        try {
            int port = Integer.parseInt(rawValue.trim());
            if (port > 0 && port <= 65535) {
                return port;
            }
        } catch (NumberFormatException ignored) {
            // Fall back to default if env/property value is invalid.
        }
        return DEFAULT_PORT;
    }
}
