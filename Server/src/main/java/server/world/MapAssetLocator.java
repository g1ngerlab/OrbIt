package server.world;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class MapAssetLocator {
    private static final String ASSETS_DIR = "assets";
    private static final String MAPS_DIR = "maps";
    private static final String MAP_FILE = "TalTechMAP.ldtk";

    private MapAssetLocator() {
    }

    public static Path resolveTalTechMapPath() {
        Path current = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 8 && current != null; depth++) {
            Path candidate = current.resolve(ASSETS_DIR).resolve(MAPS_DIR).resolve(MAP_FILE);
            if (Files.exists(candidate)) {
                return candidate.normalize();
            }
            current = current.getParent();
        }

        throw new IllegalStateException("Cannot find assets/maps/TalTechMAP.ldtk from current working directory");
    }
}

