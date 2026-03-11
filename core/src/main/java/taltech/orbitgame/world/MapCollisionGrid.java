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

    private final int[][] collision;
    private final int collisionGridSize;
    private final int mapWidthPx;
    private final int mapHeightPx;

    public MapCollisionGrid(Path ldtkPath) {
        try {
            // Java 8-compatible file loading (avoids Files.readString which requires Java 11+).
            String mapJson = new String(Files.readAllBytes(ldtkPath), StandardCharsets.UTF_8);
            JsonValue mapData = new JsonReader().parse(mapJson);
            JsonValue level = mapData.get("levels").get(0);

            this.mapWidthPx = level.getInt("pxWid");
            this.mapHeightPx = level.getInt("pxHei");

            CollisionLayerData layerData = parseCollisionLayer(level);
            this.collisionGridSize = layerData.gridSize;
            this.collision = new int[layerData.width][layerData.height];

            for (int i = 0; i < layerData.intGrid.size; i++) {
                int x = i % layerData.width;
                int y = i / layerData.width;
                collision[x][y] = layerData.intGrid.getInt(i);
            }
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

        tileY = collision[0].length - 1 - tileY;

        if (tileX < 0 || tileY < 0 || tileX >= collision.length || tileY >= collision[0].length) {
            return true;
        }

        return collision[tileX][tileY] != 0;
    }

    public float[] findRandomOpenPosition(Random random) {
        for (int attempt = 0; attempt < 200; attempt++) {
            float x = random.nextFloat() * mapWidthPx;
            float y = random.nextFloat() * mapHeightPx;
            if (!isBlocked(x, y)) {
                return new float[] {x, y};
            }
        }

        for (int y = 0; y < collision[0].length; y++) {
            for (int x = 0; x < collision.length; x++) {
                if (collision[x][y] == 0) {
                    float worldX = x * collisionGridSize;
                    float worldY = (collision[0].length - 1 - y) * collisionGridSize;
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
