package pizzaaxx.bteconosur.Utils;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RegionUtils {

    @NotNull
    @Contract("_ -> new")
    public static BlockVector2D getAveragePoint(@NotNull ProtectedPolygonalRegion region) {
        double maxX = region.getPoints().get(0).getX();
        double minX = region.getPoints().get(0).getX();
        double maxZ = region.getPoints().get(0).getZ();
        double minZ = region.getPoints().get(0).getZ();
        for (BlockVector2D vector : region.getPoints()) {
            if (maxX < vector.getX()) {
                maxX = vector.getX();
            }
            if (minX > vector.getX()) {
                minX = vector.getX();
            }
            if (maxZ < vector.getZ()) {
                maxZ = vector.getZ();
            }
            if (minZ > vector.getZ()) {
                minZ = vector.getZ();
            }
        }
        return new BlockVector2D((maxX + minX) / 2, (maxZ + minZ) / 2);
    }

    public static BlockVector2D getAveragePoint(@NotNull List<BlockVector2D> points) {
        double maxX = points.get(0).getX();
        double minX = points.get(0).getX();
        double maxZ = points.get(0).getZ();
        double minZ = points.get(0).getZ();
        for (BlockVector2D vector : points) {
            if (maxX < vector.getX()) {
                maxX = vector.getX();
            }
            if (minX > vector.getX()) {
                minX = vector.getX();
            }
            if (maxZ < vector.getZ()) {
                maxZ = vector.getZ();
            }
            if (minZ > vector.getZ()) {
                minZ = vector.getZ();
            }
        }
        return new BlockVector2D((maxX + minX) / 2, (maxZ + minZ) / 2);
    }

}
