package taltech.orbitgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapManager {
    private static final String MAP_PATH = "maps/TalTechMAP.ldtk";
    private static final String PACKAGED_CACHE_PATH = "maps/TalTechMAP.cache";
    private static final int DEFAULT_TILE_SIZE = 16;
    private static final int RENDER_CHUNK_SIZE_PX = 512;
    private static final int CACHE_MAGIC = 0x4F52424D;
    private static final int CACHE_VERSION = 2;
    private static final int TILE_STRIDE = 7;

    private static class TilesetInfo {
        final Texture texture;

        TilesetInfo(Texture texture) {
            this.texture = texture;
        }
    }

    private static class TilesetDefinition {
        final int uid;
        final int tileSize;
        final String relPath;

        TilesetDefinition(int uid, int tileSize, String relPath) {
            this.uid = uid;
            this.tileSize = tileSize;
            this.relPath = relPath;
        }
    }

    private static class PackedIntBuffer {
        private int[] items = new int[128];
        private int size = 0;

        void add(int value) {
            if (size == items.length) {
                int[] grown = new int[items.length * 2];
                System.arraycopy(items, 0, grown, 0, items.length);
                items = grown;
            }
            items[size++] = value;
        }

        int[] toArray() {
            int[] out = new int[size];
            System.arraycopy(items, 0, out, 0, size);
            return out;
        }
    }

    private static class RenderChunk {
        final int minX;
        final int minY;
        final int maxX;
        final int maxY;
        final int[] tileData;

        RenderChunk(int minX, int minY, int maxX, int maxY, int[] tileData) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.tileData = tileData;
        }

        boolean isVisible(float viewMinX, float viewMinY, float viewMaxX, float viewMaxY) {
            return maxX >= viewMinX && maxY >= viewMinY && minX <= viewMaxX && minY <= viewMaxY;
        }
    }

    private static class LoadProfiler {
        private final long startNs = System.nanoTime();
        private final Map<String, Long> stages = new LinkedHashMap<>();
        private long lastNs = startNs;

        void mark(String stage) {
            long now = System.nanoTime();
            stages.put(stage, (now - lastNs) / 1_000_000L);
            lastNs = now;
        }

        void print(String source) {
            long totalMs = (System.nanoTime() - startNs) / 1_000_000L;
            StringBuilder sb = new StringBuilder("[MapManager] Loaded map from ")
                .append(source)
                .append(" in ")
                .append(totalMs)
                .append(" ms");
            for (Map.Entry<String, Long> entry : stages.entrySet()) {
                sb.append(" | ")
                    .append(entry.getKey())
                    .append('=')
                    .append(entry.getValue())
                    .append(" ms");
            }
            System.out.println(sb.toString());
        }
    }

    public static class LoadedMapData {
        final int mapWidthPx;
        final int mapHeightPx;
        final int collisionWidth;
        final int collisionHeight;
        final int collisionGridSize;
        final byte[] collision;
        final float[] firstOpenPosition;
        final List<TilesetDefinition> tilesets;
        final List<RenderChunk> renderChunks;

        LoadedMapData(
            int mapWidthPx,
            int mapHeightPx,
            int collisionWidth,
            int collisionHeight,
            int collisionGridSize,
            byte[] collision,
            float[] firstOpenPosition,
            List<TilesetDefinition> tilesets,
            List<RenderChunk> renderChunks
        ) {
            this.mapWidthPx = mapWidthPx;
            this.mapHeightPx = mapHeightPx;
            this.collisionWidth = collisionWidth;
            this.collisionHeight = collisionHeight;
            this.collisionGridSize = collisionGridSize;
            this.collision = collision;
            this.firstOpenPosition = firstOpenPosition;
            this.tilesets = tilesets;
            this.renderChunks = renderChunks;
        }
    }

    private static class CollisionData {
        final int width;
        final int height;
        final int gridSize;
        final byte[] collision;
        final float[] firstOpenPosition;

        CollisionData(int width, int height, int gridSize, byte[] collision, float[] firstOpenPosition) {
            this.width = width;
            this.height = height;
            this.gridSize = gridSize;
            this.collision = collision;
            this.firstOpenPosition = firstOpenPosition;
        }
    }

    private final int mapWidthPx;
    private final int mapHeightPx;
    private final int collisionWidth;
    private final int collisionHeight;
    private final int collisionGridSize;
    private final byte[] collision;
    private final float[] firstOpenPosition;
    private final List<RenderChunk> renderChunks;
    private final Map<Integer, TilesetInfo> tilesetsByUid = new HashMap<>();

    public static LoadedMapData loadData() {
        LoadProfiler profiler = new LoadProfiler();

        FileHandle packagedCache = Gdx.files.internal(PACKAGED_CACHE_PATH);
        if (packagedCache.exists()) {
            LoadedMapData cached = tryLoadPackagedCache(packagedCache, profiler);
            if (cached != null) {
                profiler.print("packaged-cache");
                return cached;
            }
        }

        JsonValue mapData = new JsonReader().parse(Gdx.files.internal(MAP_PATH));
        profiler.mark("parse_json");

        JsonValue level = mapData.get("levels").get(0);
        int mapWidthPx = level.getInt("pxWid");
        int mapHeightPx = level.getInt("pxHei");

        CollisionData collisionData = buildCollision(level);
        profiler.mark("build_collision");

        List<TilesetDefinition> tilesets = parseTilesets(mapData, level);
        profiler.mark("parse_tilesets");

        List<RenderChunk> renderChunks = buildRenderChunks(level, mapHeightPx, tilesets);
        profiler.mark("build_render_chunks");

        profiler.print("ldtk-json");
        return new LoadedMapData(
            mapWidthPx,
            mapHeightPx,
            collisionData.width,
            collisionData.height,
            collisionData.gridSize,
            collisionData.collision,
            collisionData.firstOpenPosition,
            tilesets,
            renderChunks
        );
    }

    public MapManager(LoadedMapData data) {
        this.mapWidthPx = data.mapWidthPx;
        this.mapHeightPx = data.mapHeightPx;
        this.collisionWidth = data.collisionWidth;
        this.collisionHeight = data.collisionHeight;
        this.collisionGridSize = data.collisionGridSize;
        this.collision = data.collision;
        this.firstOpenPosition = data.firstOpenPosition;
        this.renderChunks = data.renderChunks;
        loadTilesets(data.tilesets);
    }

    private static LoadedMapData tryLoadPackagedCache(FileHandle cacheFile, LoadProfiler profiler) {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(cacheFile.read()))) {
            if (in.readInt() != CACHE_MAGIC || in.readInt() != CACHE_VERSION) {
                return null;
            }

            int mapWidthPx = in.readInt();
            int mapHeightPx = in.readInt();
            int collisionWidth = in.readInt();
            int collisionHeight = in.readInt();
            int collisionGridSize = in.readInt();

            byte[] collision = new byte[in.readInt()];
            in.readFully(collision);

            float[] firstOpenPosition = new float[] {in.readFloat(), in.readFloat()};

            int tilesetCount = in.readInt();
            List<TilesetDefinition> tilesets = new ArrayList<>(tilesetCount);
            for (int i = 0; i < tilesetCount; i++) {
                tilesets.add(new TilesetDefinition(in.readInt(), in.readInt(), in.readUTF()));
            }

            int chunkCount = in.readInt();
            List<RenderChunk> renderChunks = new ArrayList<>(chunkCount);
            for (int i = 0; i < chunkCount; i++) {
                int minX = in.readInt();
                int minY = in.readInt();
                int maxX = in.readInt();
                int maxY = in.readInt();
                int[] tileData = new int[in.readInt()];
                for (int tile = 0; tile < tileData.length; tile++) {
                    tileData[tile] = in.readInt();
                }
                renderChunks.add(new RenderChunk(minX, minY, maxX, maxY, tileData));
            }

            profiler.mark("read_packaged_cache");
            return new LoadedMapData(
                mapWidthPx,
                mapHeightPx,
                collisionWidth,
                collisionHeight,
                collisionGridSize,
                collision,
                firstOpenPosition,
                tilesets,
                renderChunks
            );
        } catch (IOException e) {
            System.err.println("[MapManager] Failed to read packaged cache: " + e.getMessage());
            return null;
        }
    }

    private static CollisionData buildCollision(JsonValue level) {
        JsonValue layers = level.get("layerInstances");
        for (JsonValue layer : layers) {
            String id = layer.getString("__identifier", "");
            if ("Solid".equals(id) || "Collision".equals(id)) {
                int width = layer.getInt("__cWid");
                int height = layer.getInt("__cHei");
                int gridSize = layer.getInt("__gridSize", DEFAULT_TILE_SIZE);
                JsonValue intGrid = layer.get("intGridCsv");

                byte[] collision = new byte[width * height];
                float[] openPosition = new float[] {0f, 0f};
                boolean foundOpen = false;

                int i = 0;
                for (JsonValue cell = intGrid.child; cell != null; cell = cell.next) {
                    int value = cell.asInt();
                    collision[i] = (byte) (value == 0 ? 0 : 1);

                    if (!foundOpen && value == 0) {
                        int x = i % width;
                        int y = i / width;
                        openPosition[0] = x * gridSize;
                        openPosition[1] = (height - 1 - y) * gridSize;
                        foundOpen = true;
                    }
                    i++;
                }

                return new CollisionData(width, height, gridSize, collision, openPosition);
            }
        }

        return new CollisionData(0, 0, DEFAULT_TILE_SIZE, new byte[0], new float[] {0f, 0f});
    }

    private static List<TilesetDefinition> parseTilesets(JsonValue mapData, JsonValue level) {
        List<TilesetDefinition> tilesets = new ArrayList<>();
        Set<Integer> referencedTilesets = collectReferencedTilesets(level);
        JsonValue defs = mapData.get("defs");
        JsonValue tilesetsJson = defs != null ? defs.get("tilesets") : null;
        if (tilesetsJson == null) {
            return tilesets;
        }

        for (JsonValue tileset : tilesetsJson) {
            int uid = tileset.getInt("uid");
            if (!referencedTilesets.contains(uid)) {
                continue;
            }
            String relPath = tileset.getString("relPath", null);
            if (relPath == null) {
                continue;
            }

            tilesets.add(new TilesetDefinition(
                uid,
                tileset.getInt("tileGridSize", DEFAULT_TILE_SIZE),
                relPath
            ));
        }

        return tilesets;
    }

    private static Set<Integer> collectReferencedTilesets(JsonValue level) {
        Set<Integer> referencedTilesets = new HashSet<>();
        JsonValue layers = level.get("layerInstances");
        if (layers == null) {
            return referencedTilesets;
        }

        for (JsonValue layer : layers) {
            int tilesetUid = layer.getInt("__tilesetDefUid", -1);
            if (tilesetUid >= 0) {
                referencedTilesets.add(tilesetUid);
            }
        }

        return referencedTilesets;
    }

    private static List<RenderChunk> buildRenderChunks(JsonValue level, int mapHeightPx, List<TilesetDefinition> tilesets) {
        JsonValue layers = level.get("layerInstances");
        Map<Long, PackedIntBuffer> chunkTiles = new LinkedHashMap<>();
        Map<Integer, Integer> tileSizeByTileset = new HashMap<>();
        for (TilesetDefinition tileset : tilesets) {
            tileSizeByTileset.put(tileset.uid, tileset.tileSize);
        }

        for (int layerIndex = layers.size - 1; layerIndex >= 0; layerIndex--) {
            JsonValue layer = layers.get(layerIndex);
            String type = layer.getString("__type", "");
            if (!"Tiles".equals(type) && !"AutoLayer".equals(type)) {
                continue;
            }

            JsonValue tiles = "AutoLayer".equals(type) ? layer.get("autoLayerTiles") : layer.get("gridTiles");
            if (tiles == null || tiles.size == 0) {
                continue;
            }

            int tilesetUid = layer.getInt("__tilesetDefUid", -1);
            int tileSize = tileSizeByTileset.containsKey(tilesetUid)
                ? tileSizeByTileset.get(tilesetUid)
                : layer.getInt("__gridSize", DEFAULT_TILE_SIZE);

            for (JsonValue tile : tiles) {
                int px = tile.get("px").getInt(0);
                int py = tile.get("px").getInt(1);
                int srcX = tile.get("src").getInt(0);
                int srcY = tile.get("src").getInt(1);
                int flip = tile.getInt("f", 0);
                int drawY = mapHeightPx - py - tileSize;

                int chunkX = px / RENDER_CHUNK_SIZE_PX;
                int chunkY = drawY / RENDER_CHUNK_SIZE_PX;
                long key = chunkKey(chunkX, chunkY);
                PackedIntBuffer bucket = chunkTiles.get(key);
                if (bucket == null) {
                    bucket = new PackedIntBuffer();
                    chunkTiles.put(key, bucket);
                }

                bucket.add(tilesetUid);
                bucket.add(tileSize);
                bucket.add(srcX);
                bucket.add(srcY);
                bucket.add(px);
                bucket.add(drawY);
                bucket.add(flip);
            }
        }

        List<RenderChunk> renderChunks = new ArrayList<>(chunkTiles.size());
        for (Map.Entry<Long, PackedIntBuffer> entry : chunkTiles.entrySet()) {
            int chunkX = chunkX(entry.getKey());
            int chunkY = chunkY(entry.getKey());
            int minX = chunkX * RENDER_CHUNK_SIZE_PX;
            int minY = chunkY * RENDER_CHUNK_SIZE_PX;
            int maxX = minX + RENDER_CHUNK_SIZE_PX;
            int maxY = minY + RENDER_CHUNK_SIZE_PX;
            renderChunks.add(new RenderChunk(minX, minY, maxX, maxY, entry.getValue().toArray()));
        }
        return renderChunks;
    }

    private void loadTilesets(List<TilesetDefinition> tilesets) {
        FileHandle ldtkFile = Gdx.files.internal(MAP_PATH);
        long startMs = System.currentTimeMillis();
        for (TilesetDefinition tileset : tilesets) {
            FileHandle textureFile = ldtkFile.parent().child(tileset.relPath);
            if (!textureFile.exists()) {
                FileHandle fallback = Gdx.files.internal("tiles/" + textureFile.name());
                if (!fallback.exists()) {
                    System.out.println("[MapManager] Missing tileset file: " + textureFile.path());
                    continue;
                }
                textureFile = fallback;
            }

            tilesetsByUid.put(tileset.uid, new TilesetInfo(new Texture(textureFile)));
        }
        System.out.println("[MapManager] Created " + tilesetsByUid.size() + " tileset textures in "
            + (System.currentTimeMillis() - startMs) + " ms");
    }

    public boolean isBlocked(float worldX, float worldY) {
        if (collision.length == 0) {
            return false;
        }

        int tileX = (int) (worldX / collisionGridSize);
        int tileY = (int) (worldY / collisionGridSize);
        tileY = collisionHeight - 1 - tileY;

        if (tileX < 0 || tileY < 0 || tileX >= collisionWidth || tileY >= collisionHeight) {
            return true;
        }

        return collision[tileY * collisionWidth + tileX] != 0;
    }

    public void render(SpriteBatch batch, OrthographicCamera camera) {
        float halfWidth = camera.viewportWidth * 0.5f * camera.zoom;
        float halfHeight = camera.viewportHeight * 0.5f * camera.zoom;
        float viewMinX = camera.position.x - halfWidth;
        float viewMaxX = camera.position.x + halfWidth;
        float viewMinY = camera.position.y - halfHeight;
        float viewMaxY = camera.position.y + halfHeight;

        for (RenderChunk chunk : renderChunks) {
            if (!chunk.isVisible(viewMinX, viewMinY, viewMaxX, viewMaxY)) {
                continue;
            }

            int[] data = chunk.tileData;
            for (int i = 0; i < data.length; i += TILE_STRIDE) {
                TilesetInfo tileset = tilesetsByUid.get(data[i]);
                if (tileset == null) {
                    continue;
                }

                int tileSize = data[i + 1];
                int flip = data[i + 6];
                batch.draw(
                    tileset.texture,
                    data[i + 4],
                    data[i + 5],
                    tileSize,
                    tileSize,
                    data[i + 2],
                    data[i + 3],
                    tileSize,
                    tileSize,
                    (flip & 1) != 0,
                    (flip & 2) != 0
                );
            }
        }
    }

    public void dispose() {
        for (TilesetInfo info : tilesetsByUid.values()) {
            if (info.texture != null) {
                info.texture.dispose();
            }
        }
        tilesetsByUid.clear();
    }

    public float[] findFirstOpenPosition() {
        return new float[] {firstOpenPosition[0], firstOpenPosition[1]};
    }

    public int getWidthPx() {
        return mapWidthPx;
    }

    public int getHeightPx() {
        return mapHeightPx;
    }

    public int getCollisionGridSize() {
        return collisionGridSize;
    }

    public int getRenderChunkCount() {
        return renderChunks.size();
    }

    private static long chunkKey(int x, int y) {
        return (((long) x) << 32) ^ (y & 0xffffffffL);
    }

    private static int chunkX(long key) {
        return (int) (key >> 32);
    }

    private static int chunkY(long key) {
        return (int) key;
    }
}
