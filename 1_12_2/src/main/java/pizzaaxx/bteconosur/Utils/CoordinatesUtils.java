package pizzaaxx.bteconosur.Utils;

import com.sk89q.worldedit.BlockVector2D;
import org.bukkit.Location;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Geo.Coords2D;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CoordinatesUtils {

    public static Location blockVector2DtoLocation(@NotNull BTEConoSur plugin, @NotNull BlockVector2D vector) {

        return plugin.getWorld().getHighestBlockAt(vector.getBlockX(), vector.getBlockZ()).getLocation();

    }

    @NotNull
    public static List<Coords2D> getCoords2D(BTEConoSur plugin, @NotNull Collection<BlockVector2D> vectors) {
        List<Coords2D> coords2D = new ArrayList<>();
        for (BlockVector2D vector : vectors) {
            coords2D.add(new Coords2D(plugin, vector));
        }
        return coords2D;
    }

}
