package taltech.orbitgame.network;

import taltech.orbitgame.config.OrbitConfig;

/**
 * Runtime network configuration loaded from JVM properties or environment.
 */
public final class NetworkConfig {
    private static final String HOST_PROPERTY = "orbit.server.host";
    private static final String HOST_ENV = "ORBIT_SERVER_HOST";
    private static final String DEFAULT_HOST = "localhost";

    private NetworkConfig() {
    }

    public static String getServerHost() {
        return OrbitConfig.get(HOST_PROPERTY, HOST_ENV, DEFAULT_HOST);
    }
}
