package taltech.orbitgame.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Persists auth session locally so next launch can restore login.
 */
public class AuthSessionStore {
    private static final String PREFS_NAME = "orbit_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EXPIRES_AT = "expires_at";

    public AuthSession load() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        String accessToken = prefs.getString(KEY_ACCESS_TOKEN, "");
        String refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "");
        String userId = prefs.getString(KEY_USER_ID, "");
        String username = prefs.getString(KEY_USERNAME, "");
        long expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L);

        if (accessToken.isEmpty() || refreshToken.isEmpty() || userId.isEmpty()) {
            return null;
        }
        return new AuthSession(accessToken, refreshToken, userId, username, expiresAt);
    }

    public void save(AuthSession session) {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.putString(KEY_ACCESS_TOKEN, session.getAccessToken());
        prefs.putString(KEY_REFRESH_TOKEN, session.getRefreshToken());
        prefs.putString(KEY_USER_ID, session.getUserId());
        prefs.putString(KEY_USERNAME, session.getUsername());
        prefs.putLong(KEY_EXPIRES_AT, session.getExpiresAtEpochSec());
        prefs.flush();
    }

    public void clear() {
        Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
        prefs.remove(KEY_ACCESS_TOKEN);
        prefs.remove(KEY_REFRESH_TOKEN);
        prefs.remove(KEY_USER_ID);
        prefs.remove(KEY_USERNAME);
        prefs.remove(KEY_EXPIRES_AT);
        prefs.flush();
    }
}

