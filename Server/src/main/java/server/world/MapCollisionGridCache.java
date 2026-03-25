package server.world;

import taltech.orbitgame.world.MapCollisionGrid;

import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MapCollisionGridCache {
    private static final Logger LOGGER = Logger.getLogger(MapCollisionGridCache.class.getName());
    private static volatile MapCollisionGrid cachedGrid;

    private MapCollisionGridCache() {
    }

    public static MapCollisionGrid get() {
        MapCollisionGrid grid = cachedGrid;
        if (grid != null) {
            return grid;
        }
        synchronized (MapCollisionGridCache.class) {
            if (cachedGrid == null) {
                long startMs = System.currentTimeMillis();
                Path mapPath = MapAssetLocator.resolveTalTechMapPath();
                cachedGrid = new MapCollisionGrid(mapPath);
                long elapsedMs = System.currentTimeMillis() - startMs;
                LOGGER.log(Level.INFO, "[Server] Loaded map collision grid in {0} ms", elapsedMs);
            }
            return cachedGrid;
        }
    }
}
