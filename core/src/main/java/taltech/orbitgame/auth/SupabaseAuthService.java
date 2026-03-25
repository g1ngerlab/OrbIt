package taltech.orbitgame.auth;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

/**
 * Minimal Supabase auth client for email+nickname+password workflow.
 */
public class SupabaseAuthService {
    public interface AuthCallback {
        void onSuccess(AuthSession session);

        void onError(String message);
    }

    public interface RegisterCallback {
        void onRegistered(String message);

        void onError(String message);
    }

    private static final long REFRESH_SKEW_SEC = 30L;

    private final AuthConfig config;
    private final AuthSessionStore sessionStore;
    private final JsonReader jsonReader;

    public SupabaseAuthService() {
        this.config = new AuthConfig();
        this.sessionStore = new AuthSessionStore();
        this.jsonReader = new JsonReader();
    }

    public boolean isConfigured() {
        return config.isConfigured();
    }

    public String getConfigurationError() {
        return "Supabase is not configured. Fill orbit.supabase.url and orbit.supabase.anon.key in core/src/main/resources/orbit.properties.";
    }

    public void restoreSession(AuthCallback callback) {
        if (!isConfigured()) {
            callback.onError(getConfigurationError());
            return;
        }

        AuthSession saved = sessionStore.load();
        if (saved == null) {
            callback.onError("No saved session");
            return;
        }

        long nowSec = System.currentTimeMillis() / 1000L;
        if (!saved.isExpired(nowSec + REFRESH_SKEW_SEC)) {
            callback.onSuccess(saved);
            return;
        }

        refreshSession(saved.getRefreshToken(), callback);
    }

    public void register(String email, String username, String password, RegisterCallback callback) {
        if (!isConfigured()) {
            callback.onError(getConfigurationError());
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedUsername = normalizeUsername(username);
        if (!validateRegisterInput(normalizedEmail, normalizedUsername, password, callback)) {
            return;
        }

        String body = "{\"email\":\"" + escapeJson(normalizedEmail) + "\",\"password\":\"" + escapeJson(password)
            + "\",\"data\":{\"username\":\"" + escapeJson(normalizedUsername) + "\"}}";

        sendAuthRequest(
            "/auth/v1/signup",
            "POST",
            body,
            null,
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    AuthSession session = parseAuthSession(json, normalizedUsername);
                    if (session != null) {
                        persistSessionOnly(session);
                        callback.onRegistered("Account created and logged in.");
                        return;
                    }
                    callback.onRegistered("Account created. Please confirm your email, then login.");
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            }
        );
    }

    public void login(String emailOrNickname, String password, AuthCallback callback) {
        if (!isConfigured()) {
            callback.onError(getConfigurationError());
            return;
        }

        String normalizedIdentifier = normalizeIdentifier(emailOrNickname);
        if (!validateLoginInput(normalizedIdentifier, password, callback)) {
            return;
        }

        if (looksLikeEmail(normalizedIdentifier)) {
            performPasswordLogin(normalizedIdentifier, password, callback);
            return;
        }

        resolveEmailByNickname(normalizedIdentifier, new ValueCallback() {
            @Override
            public void onValue(String value) {
                performPasswordLogin(value, password, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void logout() {
        sessionStore.clear();
        AuthState.clear();
    }

    private void performPasswordLogin(String email, String password, AuthCallback callback) {
        String body = "{\"email\":\"" + escapeJson(email) + "\",\"password\":\"" + escapeJson(password) + "\"}";

        sendAuthRequest(
            "/auth/v1/token?grant_type=password",
            "POST",
            body,
            null,
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    String metadataUsername = readMetadataUsername(json);
                    AuthSession baseSession = parseAuthSession(json, metadataUsername);
                    if (baseSession == null) {
                        callback.onError("Invalid auth response from Supabase.");
                        return;
                    }
                    ensureProfile(baseSession, email, metadataUsername, callback);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            }
        );
    }

    private void refreshSession(String refreshToken, AuthCallback callback) {
        String body = "{\"refresh_token\":\"" + escapeJson(refreshToken) + "\"}";
        sendAuthRequest(
            "/auth/v1/token?grant_type=refresh_token",
            "POST",
            body,
            null,
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    AuthSession refreshed = parseAuthSession(json, "Player");
                    if (refreshed == null) {
                        callback.onError("Saved session is invalid. Please sign in again.");
                        return;
                    }
                    loadProfileUsername(refreshed, refreshed.getUsername(), callback);
                }

                @Override
                public void onError(String message) {
                    sessionStore.clear();
                    callback.onError(message);
                }
            }
        );
    }

    private void ensureProfile(AuthSession session, String email, String fallbackUsername, AuthCallback callback) {
        String path = "/rest/v1/profiles?select=username&user_id=eq." + urlEncode(session.getUserId()) + "&limit=1";
        sendAuthRequest(
            path,
            "GET",
            null,
            session.getAccessToken(),
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue existingProfileRows) {
                    if (existingProfileRows != null && existingProfileRows.isArray() && existingProfileRows.size > 0) {
                        JsonValue row = existingProfileRows.get(0);
                        String profileUsername = row.getString("username", fallbackUsername);
                        AuthSession merged = new AuthSession(
                            session.getAccessToken(),
                            session.getRefreshToken(),
                            session.getUserId(),
                            profileUsername,
                            session.getExpiresAtEpochSec()
                        );
                        persistAndNotify(merged, callback);
                        return;
                    }

                    String usernameToInsert = normalizeUsername(fallbackUsername);
                    if (usernameToInsert.isEmpty()) {
                        usernameToInsert = "user_" + session.getUserId().substring(0, 8);
                    }
                    insertProfile(session, email, usernameToInsert, callback);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            }
        );
    }

    private void insertProfile(AuthSession session, String email, String username, AuthCallback callback) {
        String path = "/rest/v1/profiles?on_conflict=user_id";
        String body = "[{\"user_id\":\"" + escapeJson(session.getUserId()) + "\",\"username\":\""
            + escapeJson(username) + "\",\"login_email\":\"" + escapeJson(normalizeEmail(email)) + "\"}]";
        sendAuthRequest(
            path,
            "POST",
            body,
            session.getAccessToken(),
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue ignored) {
                    AuthSession merged = new AuthSession(
                        session.getAccessToken(),
                        session.getRefreshToken(),
                        session.getUserId(),
                        username,
                        session.getExpiresAtEpochSec()
                    );
                    persistAndNotify(merged, callback);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            },
            "Prefer",
            "resolution=merge-duplicates,return=minimal"
        );
    }

    private void loadProfileUsername(AuthSession session, String fallbackUsername, AuthCallback callback) {
        String path = "/rest/v1/profiles?select=username&user_id=eq." + urlEncode(session.getUserId()) + "&limit=1";
        sendAuthRequest(
            path,
            "GET",
            null,
            session.getAccessToken(),
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    String username = fallbackUsername;
                    if (json != null && json.isArray() && json.size > 0) {
                        JsonValue row = json.get(0);
                        String fromProfile = row.getString("username", "").trim();
                        if (!fromProfile.isEmpty()) {
                            username = fromProfile;
                        }
                    }

                    AuthSession merged = new AuthSession(
                        session.getAccessToken(),
                        session.getRefreshToken(),
                        session.getUserId(),
                        username,
                        session.getExpiresAtEpochSec()
                    );
                    persistAndNotify(merged, callback);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            }
        );
    }

    private void resolveEmailByNickname(String nickname, ValueCallback callback) {
        String path = "/rest/v1/profiles?select=login_email&username=eq." + urlEncode(nickname) + "&limit=1";
        sendAuthRequest(
            path,
            "GET",
            null,
            null,
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    if (json == null || !json.isArray() || json.size == 0) {
                        callback.onError("Nickname not found.");
                        return;
                    }
                    JsonValue row = json.get(0);
                    String email = row.getString("login_email", "").trim();
                    if (email.isEmpty()) {
                        callback.onError("Account email was not found for this nickname.");
                        return;
                    }
                    callback.onValue(email);
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            }
        );
    }

    private void persistAndNotify(AuthSession session, AuthCallback callback) {
        persistSessionOnly(session);
        callback.onSuccess(session);
    }

    private void persistSessionOnly(AuthSession session) {
        sessionStore.save(session);
        AuthState.setCurrentSession(session);
    }

    private void sendAuthRequest(
        String path,
        String method,
        String body,
        String bearerToken,
        JsonResponseCallback callback,
        String... extraHeaders
    ) {
        HttpRequestBuilder builder = new HttpRequestBuilder();
        Net.HttpRequest request = builder.newRequest()
            .method(method)
            .url(config.getSupabaseUrl() + path)
            .header("apikey", config.getAnonKey())
            .header("Content-Type", "application/json")
            .build();

        if (bearerToken != null && !bearerToken.isEmpty()) {
            request.setHeader("Authorization", "Bearer " + bearerToken);
        }

        if (extraHeaders != null && extraHeaders.length >= 2) {
            for (int i = 0; i + 1 < extraHeaders.length; i += 2) {
                request.setHeader(extraHeaders[i], extraHeaders[i + 1]);
            }
        }

        if (body != null) {
            request.setContent(body);
        }

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                String responseBody = httpResponse.getResultAsString();

                if (statusCode >= 200 && statusCode < 300) {
                    JsonValue parsed = parseJsonOrDefault(responseBody);
                    Gdx.app.postRunnable(() -> callback.onSuccess(parsed));
                } else {
                    String message = extractErrorMessage(responseBody, statusCode);
                    Gdx.app.postRunnable(() -> callback.onError(message));
                }
            }

            @Override
            public void failed(Throwable t) {
                String message = t == null ? "Network request failed" : t.getMessage();
                Gdx.app.postRunnable(() -> callback.onError(message));
            }

            @Override
            public void cancelled() {
                Gdx.app.postRunnable(() -> callback.onError("Request cancelled"));
            }
        });
    }

    private JsonValue parseJsonOrDefault(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            return new JsonValue(JsonValue.ValueType.object);
        }
        try {
            return jsonReader.parse(jsonText);
        } catch (Exception e) {
            return new JsonValue(JsonValue.ValueType.object);
        }
    }

    private AuthSession parseAuthSession(JsonValue json, String fallbackUsername) {
        if (json == null) {
            return null;
        }

        String accessToken = json.getString("access_token", "");
        String refreshToken = json.getString("refresh_token", "");
        long expiresAt = json.getLong("expires_at", 0L);
        JsonValue user = json.get("user");
        String userId = user == null ? "" : user.getString("id", "");

        if (accessToken.isEmpty() || refreshToken.isEmpty() || userId.isEmpty()) {
            return null;
        }

        return new AuthSession(accessToken, refreshToken, userId, fallbackUsername, expiresAt);
    }

    private boolean validateRegisterInput(String email, String username, String password, RegisterCallback callback) {
        if (email.isEmpty()) {
            callback.onError("Email is required");
            return false;
        }
        if (!looksLikeEmail(email)) {
            callback.onError("Enter a valid email");
            return false;
        }
        if (username == null || username.trim().isEmpty()) {
            callback.onError("Nickname is required");
            return false;
        }
        if (username.length() < 3 || username.length() > 16) {
            callback.onError("Nickname must be 3..16 characters");
            return false;
        }
        if (!username.matches("[A-Za-z0-9_]+")) {
            callback.onError("Nickname can contain only letters, numbers and _");
            return false;
        }
        if (!isPasswordStrong(password)) {
            callback.onError("Password must be 8+ chars and include at least one digit");
            return false;
        }
        if (password.length() > 72) {
            callback.onError("Password is too long");
            return false;
        }
        return true;
    }

    private boolean validateLoginInput(String identifier, String password, AuthCallback callback) {
        if (identifier.isEmpty()) {
            callback.onError("Enter email or nickname");
            return false;
        }
        if (!isPasswordStrong(password)) {
            callback.onError("Password must be 8+ chars and include at least one digit");
            return false;
        }
        return true;
    }

    private boolean isPasswordStrong(String password) {
        return password != null && password.length() >= 8 && password.matches(".*\\d.*");
    }

    private String extractErrorMessage(String responseBody, int statusCode) {
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            try {
                JsonValue parsed = jsonReader.parse(responseBody);
                String message = parsed.getString("msg", "");
                if (message.isEmpty()) {
                    message = parsed.getString("message", "");
                }
                if (!message.isEmpty()) {
                    return message;
                }
            } catch (Exception ignored) {
                // Fallback to status code message below.
            }
        }
        return "Supabase request failed (HTTP " + statusCode + ")";
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String normalizeIdentifier(String identifier) {
        return identifier == null ? "" : identifier.trim();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private boolean looksLikeEmail(String value) {
        return value != null && value.contains("@") && value.contains(".");
    }

    private String readMetadataUsername(JsonValue json) {
        if (json == null) {
            return "";
        }
        JsonValue user = json.get("user");
        if (user == null) {
            return "";
        }
        JsonValue metadata = user.get("user_metadata");
        if (metadata == null) {
            return "";
        }
        return metadata.getString("username", "").trim();
    }

    private String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding is not available", e);
        }
    }

    private String escapeJson(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private interface JsonResponseCallback {
        void onSuccess(JsonValue json);

        void onError(String message);
    }

    private interface ValueCallback {
        void onValue(String value);

        void onError(String message);
    }
}

