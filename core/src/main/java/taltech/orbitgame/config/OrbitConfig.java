package taltech.orbitgame.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads runtime settings from JVM properties, environment variables and classpath defaults.
 */
public final class OrbitConfig {
    private static final String CLASSPATH_CONFIG_NAME = "orbit.properties";
    private static final Properties CLASSPATH_DEFAULTS = loadClasspathDefaults();

    private OrbitConfig() {
    }

    public static String get(String propertyName, String envName, String fallback) {
        String fromProperty = System.getProperty(propertyName);
        if (isValueSet(fromProperty)) {
            return fromProperty.trim();
        }

        String fromEnv = System.getenv(envName);
        if (isValueSet(fromEnv)) {
            return fromEnv.trim();
        }

        String fromClasspath = CLASSPATH_DEFAULTS.getProperty(propertyName, "");
        if (isValueSet(fromClasspath)) {
            return fromClasspath.trim();
        }

        return fallback;
    }

    private static Properties loadClasspathDefaults() {
        Properties properties = new Properties();
        ClassLoader classLoader = OrbitConfig.class.getClassLoader();
        if (classLoader == null) {
            return properties;
        }

        try (InputStream stream = classLoader.getResourceAsStream(CLASSPATH_CONFIG_NAME)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // Keep empty defaults if file is missing or unreadable.
        }

        return properties;
    }

    private static boolean isValueSet(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

