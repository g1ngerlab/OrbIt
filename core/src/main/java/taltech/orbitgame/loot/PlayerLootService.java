package taltech.orbitgame.loot;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import taltech.orbitgame.auth.AuthConfig;
import taltech.orbitgame.auth.AuthSession;
import taltech.orbitgame.auth.AuthState;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemStack;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Supabase gateway for persistent player loot.
 */
public class PlayerLootService {
    public interface SaveCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface LoadCallback {
        void onSuccess(List<LootEntry> entries);

        void onError(String message);
    }

    private final AuthConfig config;
    private final JsonReader jsonReader;

    public PlayerLootService() {
        this.config = new AuthConfig();
        this.jsonReader = new JsonReader();
    }

    public boolean isConfigured() {
        return config.isConfigured();
    }

    public void saveExtractedLoot(ItemStack[] slots, SaveCallback callback) {
        if (!isConfigured()) {
            callback.onError("Supabase is not configured");
            return;
        }

        AuthSession session = AuthState.getCurrentSession();
        if (session == null || session.getAccessToken().isEmpty() || session.getUserId().isEmpty()) {
            callback.onError("User session is missing");
            return;
        }

        Map<String, Integer> extracted = aggregateByItemCode(slots);
        if (extracted.isEmpty()) {
            callback.onSuccess();
            return;
        }

        loadCurrentTotals(session, new TotalsCallback() {
            @Override
            public void onSuccess(Map<String, Integer> currentTotals) {
                Map<String, Integer> merged = new LinkedHashMap<>(currentTotals);
                for (Map.Entry<String, Integer> entry : extracted.entrySet()) {
                    String code = entry.getKey();
                    int amount = entry.getValue();
                    int prev = merged.containsKey(code) ? merged.get(code) : 0;
                    merged.put(code, prev + Math.max(0, amount));
                }
                upsertTotals(session, merged, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void loadPlayerLoot(LoadCallback callback) {
        if (!isConfigured()) {
            callback.onError("Supabase is not configured");
            return;
        }

        AuthSession session = AuthState.getCurrentSession();
        if (session == null || session.getAccessToken().isEmpty() || session.getUserId().isEmpty()) {
            callback.onError("Sign in first to open inventory");
            return;
        }

        String path = "/rest/v1/player_loot?select=item_code,quantity&user_id=eq."
            + urlEncode(session.getUserId())
            + "&order=item_code.asc";
        sendRequest(path, "GET", null, session.getAccessToken(), new JsonResponseCallback() {
            @Override
            public void onSuccess(JsonValue json) {
                List<LootEntry> result = new ArrayList<>();
                if (json != null && json.isArray()) {
                    for (JsonValue row = json.child; row != null; row = row.next) {
                        String itemCode = row.getString("item_code", "").trim();
                        int quantity = row.getInt("quantity", 0);
                        if (!itemCode.isEmpty() && quantity > 0) {
                            result.add(new LootEntry(itemCode, quantity));
                        }
                    }
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void deleteLootItem(String itemCode, SaveCallback callback) {
        if (!isConfigured()) {
            callback.onError("Supabase is not configured");
            return;
        }
        if (itemCode == null || itemCode.trim().isEmpty()) {
            callback.onError("Invalid item code");
            return;
        }

        AuthSession session = AuthState.getCurrentSession();
        if (session == null || session.getAccessToken().isEmpty() || session.getUserId().isEmpty()) {
            callback.onError("Sign in first to modify inventory");
            return;
        }

        String path = "/rest/v1/player_loot?user_id=eq."
            + urlEncode(session.getUserId())
            + "&item_code=eq."
            + urlEncode(itemCode.trim());

        sendRequest(
            path,
            "DELETE",
            null,
            session.getAccessToken(),
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    callback.onSuccess();
                }

                @Override
                public void onError(String message) {
                    callback.onError(message);
                }
            },
            "Prefer",
            "return=minimal"
        );
    }

    public void deleteLootItemAmount(String itemCode, int amount, SaveCallback callback) {
        if (amount <= 0) {
            callback.onError("Delete amount must be greater than zero");
            return;
        }
        if (itemCode == null || itemCode.trim().isEmpty()) {
            callback.onError("Invalid item code");
            return;
        }

        AuthSession session = AuthState.getCurrentSession();
        if (session == null || session.getAccessToken().isEmpty() || session.getUserId().isEmpty()) {
            callback.onError("Sign in first to modify inventory");
            return;
        }

        String normalizedCode = itemCode.trim();
        String path = "/rest/v1/player_loot?select=quantity&user_id=eq."
            + urlEncode(session.getUserId())
            + "&item_code=eq."
            + urlEncode(normalizedCode)
            + "&limit=1";

        sendRequest(path, "GET", null, session.getAccessToken(), new JsonResponseCallback() {
            @Override
            public void onSuccess(JsonValue json) {
                int current = 0;
                if (json != null && json.isArray() && json.size > 0) {
                    current = Math.max(0, json.get(0).getInt("quantity", 0));
                }
                if (current <= 0) {
                    callback.onSuccess();
                    return;
                }

                int remaining = current - amount;
                if (remaining <= 0) {
                    deleteLootItem(normalizedCode, callback);
                    return;
                }
                updateLootQuantity(session, normalizedCode, remaining, callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void updateLootQuantity(AuthSession session, String itemCode, int quantity, SaveCallback callback) {
        String path = "/rest/v1/player_loot?on_conflict=user_id,item_code";
        Map<String, Integer> single = new LinkedHashMap<>();
        single.put(itemCode, Math.max(0, quantity));
        String body = buildUpsertBody(session.getUserId(), single);

        sendRequest(
            path,
            "POST",
            body,
            session.getAccessToken(),
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    callback.onSuccess();
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

    private void loadCurrentTotals(AuthSession session, TotalsCallback callback) {
        String path = "/rest/v1/player_loot?select=item_code,quantity&user_id=eq."
            + urlEncode(session.getUserId());
        sendRequest(path, "GET", null, session.getAccessToken(), new JsonResponseCallback() {
            @Override
            public void onSuccess(JsonValue json) {
                Map<String, Integer> totals = new LinkedHashMap<>();
                if (json != null && json.isArray()) {
                    for (JsonValue row = json.child; row != null; row = row.next) {
                        String code = row.getString("item_code", "").trim();
                        int quantity = Math.max(0, row.getInt("quantity", 0));
                        if (!code.isEmpty() && quantity > 0) {
                            totals.put(code, quantity);
                        }
                    }
                }
                callback.onSuccess(totals);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    private void upsertTotals(AuthSession session, Map<String, Integer> totals, SaveCallback callback) {
        if (totals.isEmpty()) {
            callback.onSuccess();
            return;
        }

        String path = "/rest/v1/player_loot?on_conflict=user_id,item_code";
        String body = buildUpsertBody(session.getUserId(), totals);
        sendRequest(
            path,
            "POST",
            body,
            session.getAccessToken(),
            new JsonResponseCallback() {
                @Override
                public void onSuccess(JsonValue json) {
                    callback.onSuccess();
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

    private Map<String, Integer> aggregateByItemCode(ItemStack[] slots) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        if (slots == null) {
            return totals;
        }

        for (ItemStack stack : slots) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            ItemKind kind = ItemKind.fromId(stack.getItemTypeId());
            if (kind == ItemKind.NONE) {
                continue;
            }

            String code = kind.getCode();
            int amount = Math.max(0, stack.getAmount());
            if (amount <= 0) {
                continue;
            }

            int prev = totals.containsKey(code) ? totals.get(code) : 0;
            totals.put(code, prev + amount);
        }
        return totals;
    }

    private String buildUpsertBody(String userId, Map<String, Integer> totals) {
        StringBuilder body = new StringBuilder("[");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : totals.entrySet()) {
            if (!first) {
                body.append(',');
            }
            body.append("{\"user_id\":\"")
                .append(escapeJson(userId))
                .append("\",\"item_code\":\"")
                .append(escapeJson(entry.getKey()))
                .append("\",\"quantity\":")
                .append(Math.max(0, entry.getValue()))
                .append(",\"metadata\":{}}");
            first = false;
        }
        body.append(']');
        return body.toString();
    }

    private void sendRequest(
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

    private String extractErrorMessage(String responseBody, int statusCode) {
        if (responseBody != null && !responseBody.trim().isEmpty()) {
            try {
                JsonValue parsed = jsonReader.parse(responseBody);
                String message = parsed.getString("message", "");
                if (message.isEmpty()) {
                    message = parsed.getString("msg", "");
                }
                if (!message.isEmpty()) {
                    return message;
                }
            } catch (Exception ignored) {
                // Fallback to status code below.
            }
        }
        return "Supabase request failed (HTTP " + statusCode + ")";
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

    private interface TotalsCallback {
        void onSuccess(Map<String, Integer> currentTotals);

        void onError(String message);
    }
}

