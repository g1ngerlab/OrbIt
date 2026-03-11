package taltech.orbitgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.files.FileHandle;

import java.util.HashMap;
import java.util.Map;

public class MapManager {

    private JsonValue mapData;
    private int[][] collision;
    private int mapWidthPx;
    private int mapHeightPx;
    private int collisionGridSize = 16;

    private static class TilesetInfo {
        final Texture texture;
        final int tileSize;

        TilesetInfo(Texture texture, int tileSize) {
            this.texture = texture;
            this.tileSize = tileSize;
        }
    }

    private final Map<Integer, TilesetInfo> tilesetsByUid = new HashMap<>();

    public MapManager() {
        JsonReader reader = new JsonReader();
        mapData = reader.parse(Gdx.files.internal("maps/TalTechMAP.ldtk"));

        JsonValue level = mapData.get("levels").get(0);
        mapWidthPx = level.getInt("pxWid");
        mapHeightPx = level.getInt("pxHei");

        loadTilesets();

        buildCollision();
    }

    private void loadTilesets() {
        FileHandle ldtkFile = Gdx.files.internal("maps/TalTechMAP.ldtk");
        JsonValue defs = mapData.get("defs");
        JsonValue tilesets = defs != null ? defs.get("tilesets") : null;
        if (tilesets == null) return;

        for (JsonValue tileset : tilesets) {
            int uid = tileset.getInt("uid");
            int tileSize = tileset.getInt("tileGridSize", 16);
            String relPath = tileset.getString("relPath", null);

            if (relPath == null) continue;

            FileHandle textureFile = ldtkFile.parent().child(relPath);
            if (!textureFile.exists()) {
                String fileName = textureFile.name();
                FileHandle fallback = Gdx.files.internal("tiles/" + fileName);
                if (fallback.exists()) {
                    textureFile = fallback;
                } else {
                    System.out.println("[MapManager] Missing tileset file: " + textureFile.path());
                    continue;
                }
            }

            tilesetsByUid.put(uid, new TilesetInfo(new Texture(textureFile), tileSize));
        }
    }

    private void buildCollision() {
        JsonValue level = mapData.get("levels").get(0);
        JsonValue layers = level.get("layerInstances");

        for (JsonValue layer : layers) {

            String id = layer.getString("__identifier");
            if ("Solid".equals(id) || "Collision".equals(id)) {

                int width = layer.getInt("__cWid");
                int height = layer.getInt("__cHei");
                collisionGridSize = layer.getInt("__gridSize", collisionGridSize);

                collision = new int[width][height];

                JsonValue intGrid = layer.get("intGridCsv");

                for (int i = 0; i < intGrid.size; i++) {
                    int x = i % width;
                    int y = i / width;
                    collision[x][y] = intGrid.getInt(i);
                }

                return;
            }
        }
    }


    public boolean isBlocked(float worldX, float worldY) {
        int tileX = (int) (worldX / collisionGridSize);
        int tileY = (int) (worldY / collisionGridSize);

        if (collision == null) return false;


        tileY = collision[0].length - 1 - tileY;

        if (tileX < 0 || tileY < 0 || tileX >= collision.length || tileY >= collision[0].length) {
            return true;
        }

        return collision[tileX][tileY] != 0;
    }

    public void render(SpriteBatch batch) {
        JsonValue level = mapData.get("levels").get(0);
        JsonValue layers = level.get("layerInstances");

        for (int i = layers.size - 1; i >= 0; i--) {
            JsonValue layer = layers.get(i);
            String type = layer.getString("__type");
            if ("Tiles".equals(type) || "AutoLayer".equals(type)) {

                JsonValue tiles = "AutoLayer".equals(type) ? layer.get("autoLayerTiles") : layer.get("gridTiles");
                if (tiles == null || tiles.size == 0) continue;

                int mapHeight = level.getInt("pxHei");
                int tilesetUid = layer.getInt("__tilesetDefUid", -1);
                TilesetInfo tilesetInfo = tilesetsByUid.get(tilesetUid);
                if (tilesetInfo == null) continue;

                Texture tilesetTexture = tilesetInfo.texture;
                int tileSize = tilesetInfo.tileSize;

                for (JsonValue tile : tiles) {
                    int px = tile.get("px").getInt(0);
                    int py = tile.get("px").getInt(1);

                    int srcX = tile.get("src").getInt(0);
                    int srcY = tile.get("src").getInt(1);

                    TextureRegion region = new TextureRegion(tilesetTexture, srcX, srcY, tileSize, tileSize);
                    int flip = tile.getInt("f", 0);
                    if ((flip & 1) != 0) region.flip(true, false);
                    if ((flip & 2) != 0) region.flip(false, true);
                    float correctedY = mapHeight - py - tileSize;
                    batch.draw(region, px, correctedY);
                }
            }
        }
    }

    public void dispose() {
        for (TilesetInfo info : tilesetsByUid.values()) {
            if (info.texture != null) info.texture.dispose();
        }
    }

    public float[] findFirstOpenPosition() {
        if (collision == null) return new float[] {0f, 0f};

        int width = collision.length;
        int height = collision[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (collision[x][y] == 0) {
                    float worldX = x * collisionGridSize;
                    float worldY = (height - 1 - y) * collisionGridSize;
                    return new float[] {worldX, worldY};
                }
            }
        }

        return new float[] {0f, 0f};
    }

    public int getWidthPx() {
        return mapWidthPx;
    }

    public int getHeightPx() {
        return mapHeightPx;
    }
}
