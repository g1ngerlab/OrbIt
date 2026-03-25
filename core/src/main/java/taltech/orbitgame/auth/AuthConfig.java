package taltech.orbitgame.auth;

import taltech.orbitgame.config.OrbitConfig;

/**
 * Reads Supabase connection settings from runtime properties/environment.
 */
public final class AuthConfig {
    private static final String URL_PROPERTY = "orbit.supabase.url";
    private static final String KEY_PROPERTY = "orbit.supabase.anon.key";
    private static final String URL_ENV = "ORBIT_SUPABASE_URL";
    private static final String KEY_ENV = "ORBIT_SUPABASE_ANON_KEY";

    private final String supabaseUrl;
    private final String anonKey;

    public AuthConfig() {
        this.supabaseUrl = readSetting(URL_PROPERTY, URL_ENV);
        this.anonKey = readSetting(KEY_PROPERTY, KEY_ENV);
    }

    public String getSupabaseUrl() {
        return supabaseUrl;
    }

    public String getAnonKey() {
        return anonKey;
    }

    public boolean isConfigured() {
        return isValueSet(supabaseUrl) && isValueSet(anonKey);
    }

    private static String readSetting(String propertyName, String envName) {
        return OrbitConfig.get(propertyName, envName, "");
    }

    private static boolean isValueSet(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
