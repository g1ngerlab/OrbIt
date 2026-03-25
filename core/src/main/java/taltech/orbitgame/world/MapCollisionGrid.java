package taltech.orbitgame.world;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class MapCollisionGrid {
    private static class CollisionLayerData {
        final int width;
        final int height;
        final int gridSize;
        final JsonValue intGrid;

        CollisionLayerData(int width, int height, int gridSize, JsonValue intGrid) {
            this.width = width;
            this.height = height;
            this.gridSize = gridSize;
            this.intGrid = intGrid;
        }
    }

    private final byte[] collision;
    private final int collisionGridSize;
    private final int collisionWidth;
    private final int collisionHeight;
    private final int mapWidthPx;
    private final int mapHeightPx;

    public MapCollisionGrid(Path ldtkPath) {
        long startMs = System.currentTimeMillis();
        try {
            // Java 8-compatible file loading (avoids Files.readString which requires Java 11+).
            String mapJson = new String(Files.readAllBytes(ldtkPath), StandardCharsets.UTF_8);
            long readMs = System.currentTimeMillis();
            JsonValue mapData = new JsonReader().parse(mapJson);
            long parseMs = System.currentTimeMillis();
            JsonValue level = mapData.get("levels").get(0);

            this.mapWidthPx = level.getInt("pxWid");
            this.mapHeightPx = level.getInt("pxHei");

            CollisionLayerData layerData = parseCollisionLayer(level);
            this.collisionGridSize = layerData.gridSize;
            this.collisionWidth = layerData.width;
            this.collisionHeight = layerData.height;
            this.collision = new byte[layerData.width * layerData.height];

            int i = 0;
            for (JsonValue cell = layerData.intGrid.child; cell != null; cell = cell.next) {
                collision[i++] = (byte) (cell.asInt() == 0 ? 0 : 1);
            }
            long buildMs = System.currentTimeMillis();
            System.out.println("[MapCollisionGrid] Loaded in " + (buildMs - startMs)
                + " ms | read_bytes=" + (readMs - startMs)
                + " ms | parse_json=" + (parseMs - readMs)
                + " ms | build_collision=" + (buildMs - parseMs) + " ms");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read map file: " + ldtkPath, e);
        }
    }

    private CollisionLayerData parseCollisionLayer(JsonValue level) {
        JsonValue layers = level.get("layerInstances");
        for (JsonValue layer : layers) {
            String id = layer.getString("__identifier", "");
            if ("Solid".equals(id) || "Collision".equals(id)) {
                int width = layer.getInt("__cWid");
                int height = layer.getInt("__cHei");
                int gridSize = layer.getInt("__gridSize", 16);
                return new CollisionLayerData(width, height, gridSize, layer.get("intGridCsv"));
            }
        }
        throw new IllegalStateException("Collision layer not found in LDtk map");
    }

    public boolean isBlocked(float worldX, float worldY) {
        int tileX = (int) (worldX / collisionGridSize);
        int tileY = (int) (worldY / collisionGridSize);

        tileY = collisionHeight - 1 - tileY;

        if (tileX < 0 || tileY < 0 || tileX >= collisionWidth || tileY >= collisionHeight) {
            return true;
        }

        return collision[tileY * collisionWidth + tileX] != 0;
    }

    public float[] findRandomOpenPosition(Random random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            float x = random.nextFloat() * mapWidthPx;
            float y = random.nextFloat() * mapHeightPx;
            if (!isBlocked(x, y)) {
                return new float[] {x, y};
            }
        }

        for (int y = 0; y < collisionHeight; y++) {
            for (int x = 0; x < collisionWidth; x++) {
                if (collision[y * collisionWidth + x] == 0) {
                    float worldX = x * collisionGridSize;
                    float worldY = (collisionHeight - 1 - y) * collisionGridSize;
                    return new float[] {worldX, worldY};
                }
            }
        }

        return new float[] {0f, 0f};
    }

    public int getMapWidthPx() {
        return mapWidthPx;
    }

    public int getMapHeightPx() {
        return mapHeightPx;
    }
}
