package taltech.orbitgame.auth;

/**
 * Lightweight authenticated session payload used by the client.
 */
public class AuthSession {
    private final String accessToken;
    private final String refreshToken;
    private final String userId;
    private final String username;
    private final long expiresAtEpochSec;

    public AuthSession(String accessToken, String refreshToken, String userId, String username, long expiresAtEpochSec) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.username = username;
        this.expiresAtEpochSec = expiresAtEpochSec;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public long getExpiresAtEpochSec() {
        return expiresAtEpochSec;
    }

    public boolean isExpired(long nowEpochSec) {
        return expiresAtEpochSec > 0 && nowEpochSec >= expiresAtEpochSec;
    }
}

