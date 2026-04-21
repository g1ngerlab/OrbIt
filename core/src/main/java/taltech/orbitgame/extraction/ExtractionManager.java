package taltech.orbitgame.extraction;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages all extraction points in the game.
 * Responsible for point lifecycle and state synchronization.
 */
public class ExtractionManager {
    private static final String MAP_PATH = "maps/TalTechMAP.ldtk";
    private static final Set<String> PLATFORM_LAYERS = new HashSet<>(Arrays.asList(
        "ROCKGROUND",
        "Rocks_n_stuff",
        "Stena",
        "LESTNICA",
        "RANDOM"
    ));
    private static final int MIN_CLUSTER_TILES = 10;
    private static final float MIN_CLUSTER_SIZE_PX = 80f;
    private static final float MAX_CLUSTER_SIZE_PX = 160f;
    private static final float MERGE_DISTANCE_PX = 96f;

    private final Map<Integer, ExtractionPoint> points = new HashMap<>();
    private int nextPointId = 0;

    public ExtractionManager() {
        initializeExtractionPoints();
    }

    /**
     * Initialize extraction points based on confirmed screenshot coordinates.
     * More points can be added here as they are confirmed.
     */
    private void initializeExtractionPoints() {
        points.clear();
        nextPointId = 0;

        // Confirmed extraction points from screenshots
        addExtractionPoint(268.2f, 238.0f, 96f);
        addExtractionPoint(1262.6f, 1466.3f, 96f);
        addExtractionPoint(1406.7f, 2939.7f, 96f);
        addExtractionPoint(3500f, 585f, 96f);
        addExtractionPoint(4720f, 1640f, 96f);
        addExtractionPoint(2975f, 1350f, 96f);
        addExtractionPoint(260f, 2095f, 96f);
        addExtractionPoint(370f, 3930f, 96f);
        addExtractionPoint(1490f, 4444f, 96f);
        addExtractionPoint(4525f, 3065f, 96f);
        addExtractionPoint(2835f, 2945f, 96f);
    }

    private void loadExtractionPointsFromMap() {
        JsonValue mapData = new JsonReader().parse(Gdx.files.internal(MAP_PATH));
        JsonValue level = mapData.get("levels").get(0);
        int mapHeightPx = level.getInt("pxHei");

        List<ExtractionCandidate> candidates = new ArrayList<>();
        JsonValue layers = level.get("layerInstances");
        for (JsonValue layer : layers) {
            String identifier = layer.getString("__identifier", "");
            if (!PLATFORM_LAYERS.contains(identifier)) {
                continue;
            }

            JsonValue tiles = layer.get("gridTiles");
            if (tiles == null || tiles.size == 0) {
                tiles = layer.get("autoLayerTiles");
            }
            if (tiles == null || tiles.size == 0) {
                continue;
            }

            int tileSize = layer.getInt("__gridSize", 16);
            Map<Long, Integer> sourceCounts = new HashMap<>();
            for (JsonValue tile : tiles) {
                int srcX = tile.get("src").getInt(0);
                int srcY = tile.get("src").getInt(1);
                long key = (((long) srcX) << 32) ^ (srcY & 0xffffffffL);
                sourceCounts.put(key, sourceCounts.getOrDefault(key, 0) + 1);
            }

            Set<Long> rareSources = new HashSet<>();
            for (Map.Entry<Long, Integer> entry : sourceCounts.entrySet()) {
                if (entry.getValue() <= 5) {
                    rareSources.add(entry.getKey());
                }
            }
            if (rareSources.isEmpty()) {
                continue;
            }

            Map<Long, JsonValue> tileByCell = new HashMap<>();
            Set<Long> cells = new HashSet<>();
            for (JsonValue tile : tiles) {
                int srcX = tile.get("src").getInt(0);
                int srcY = tile.get("src").getInt(1);
                long sourceKey = (((long) srcX) << 32) ^ (srcY & 0xffffffffL);
                if (!rareSources.contains(sourceKey)) {
                    continue;
                }

                int px = tile.get("px").getInt(0);
                int py = tile.get("px").getInt(1);
                long cellKey = cellKey(px / tileSize, py / tileSize);
                cells.add(cellKey);
                tileByCell.put(cellKey, tile);
            }

            Set<Long> visited = new HashSet<>();
            for (Long startCell : cells) {
                if (visited.contains(startCell)) {
                    continue;
                }

                List<Long> component = new ArrayList<>();
                ArrayList<Long> stack = new ArrayList<>();
                stack.add(startCell);
                visited.add(startCell);

                while (!stack.isEmpty()) {
                    Long cell = stack.remove(stack.size() - 1);
                    component.add(cell);
                    int cellX = cellX(cell);
                    int cellY = cellY(cell);

                    long[] neighbors = new long[] {
                        cellKey(cellX + 1, cellY),
                        cellKey(cellX - 1, cellY),
                        cellKey(cellX, cellY + 1),
                        cellKey(cellX, cellY - 1)
                    };
                    for (long neighbor : neighbors) {
                        if (cells.contains(neighbor) && !visited.contains(neighbor)) {
                            visited.add(neighbor);
                            stack.add(neighbor);
                        }
                    }
                }

                if (component.size() < MIN_CLUSTER_TILES) {
                    continue;
                }

                float minX = Float.MAX_VALUE;
                float minY = Float.MAX_VALUE;
                float maxX = Float.MIN_VALUE;
                float maxY = Float.MIN_VALUE;
                float sumCenterX = 0f;
                float sumCenterY = 0f;
                for (Long cell : component) {
                    JsonValue tile = tileByCell.get(cell);
                    int px = tile.get("px").getInt(0);
                    int py = tile.get("px").getInt(1);
                    float centerX = px + tileSize * 0.5f;
                    float centerY = mapHeightPx - (py + tileSize * 0.5f);
                    sumCenterX += centerX;
                    sumCenterY += centerY;
                    minX = Math.min(minX, px);
                    minY = Math.min(minY, py);
                    maxX = Math.max(maxX, px + tileSize);
                    maxY = Math.max(maxY, py + tileSize);
                }

                float worldWidth = maxX - minX;
                float worldHeight = maxY - minY;
                if (worldWidth < MIN_CLUSTER_SIZE_PX || worldWidth > MAX_CLUSTER_SIZE_PX
                    || worldHeight < MIN_CLUSTER_SIZE_PX || worldHeight > MAX_CLUSTER_SIZE_PX) {
                    continue;
                }

                candidates.add(new ExtractionCandidate(
                    sumCenterX / component.size(),
                    sumCenterY / component.size(),
                    Math.max(worldWidth, worldHeight) * 0.5f + 8f
                ));
            }
        }

        candidates.sort((a, b) -> {
            int xCompare = Float.compare(a.x, b.x);
            if (xCompare != 0) {
                return xCompare;
            }
            return Float.compare(a.y, b.y);
        });

        for (ExtractionCandidate candidate : candidates) {
            if (isTooCloseToExisting(candidate.x, candidate.y)) {
                continue;
            }
            addExtractionPoint(candidate.x, candidate.y, candidate.radius);
        }
    }

    private boolean isTooCloseToExisting(float x, float y) {
        for (ExtractionPoint point : points.values()) {
            float dx = point.getPosition().x - x;
            float dy = point.getPosition().y - y;
            if (dx * dx + dy * dy <= MERGE_DISTANCE_PX * MERGE_DISTANCE_PX) {
                return true;
            }
        }
        return false;
    }

    private long cellKey(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    private int cellX(long key) {
        return (int) (key >> 32);
    }

    private int cellY(long key) {
        return (int) key;
    }

    private static class ExtractionCandidate {
        final float x;
        final float y;
        final float radius;

        ExtractionCandidate(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    /**
     * Add an extraction point to the manager
     */
    public ExtractionPoint addExtractionPoint(float x, float y, float radius) {
        ExtractionPoint point = new ExtractionPoint(nextPointId++, x, y, radius);
        points.put(point.getId(), point);
        return point;
    }

    /**
     * Update all extraction points
     */
    public void update(float deltaTime) {
        for (ExtractionPoint point : points.values()) {
            point.update(deltaTime);
        }
    }

    /**
     * Get extraction point by ID
     */
    public ExtractionPoint getPoint(int pointId) {
        return points.get(pointId);
    }

    /**
     * Find extraction point at player's current position
     */
    public ExtractionPoint getPointAtPosition(float playerX, float playerY) {
        for (ExtractionPoint point : points.values()) {
            if (point.isPlayerInRange(playerX, playerY)) {
                return point;
            }
        }
        return null;
    }

    /**
     * Get all extraction points
     */
    public List<ExtractionPoint> getAllPoints() {
        return new ArrayList<>(points.values());
    }

    /**
     * Get all extraction points data for network transmission
     */
    public List<taltech.orbitgame.network.Network.ExtractionPointData> getPointsData() {
        List<taltech.orbitgame.network.Network.ExtractionPointData> data = new ArrayList<>();
        for (ExtractionPoint point : points.values()) {
            data.add(new taltech.orbitgame.network.Network.ExtractionPointData(
                point.getId(),
                point.getPosition().x,
                point.getPosition().y,
                point.getRadius()
            ));
        }
        return data;
    }

    /**
     * Find the closest extraction point to player position
     */
    public ExtractionPoint getClosestPoint(float playerX, float playerY) {
        ExtractionPoint closest = null;
        float minDistance = Float.MAX_VALUE;

        for (ExtractionPoint point : points.values()) {
            float dx = point.getPosition().x - playerX;
            float dy = point.getPosition().y - playerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < minDistance) {
                minDistance = distance;
                closest = point;
            }
        }

        return closest;
    }

    /**
     * Dispose all resources
     */
    public void dispose() {
        points.clear();
    }
}
