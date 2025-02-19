package pizzaaxx.bteconosur.Geo;

import com.sk89q.worldedit.BlockVector2D;
import net.buildtheearth.terraplusplus.generator.EarthGeneratorSettings;
import net.buildtheearth.terraplusplus.projection.GeographicProjection;
import net.buildtheearth.terraplusplus.projection.OutOfProjectionBoundsException;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

public class Coords2D {
    private final BTEConoSur plugin;
    private final GeographicProjection geographicProjection =
            EarthGeneratorSettings.parse(EarthGeneratorSettings.BTE_DEFAULT_SETTINGS).projection();

    private double x;
    private double z;
    private double lon;
    private double lat;

    public Coords2D(BTEConoSur plugin, double lat, double lon) {
        this.plugin = plugin;
        try {
            double[] mcCoords = geographicProjection.fromGeo(lon, lat);
            initialize(mcCoords[0], mcCoords[1], lon, lat);
        } catch (OutOfProjectionBoundsException e) {
            e.printStackTrace();
        }
    }

    public Coords2D(BTEConoSur plugin, @NotNull Location location) {
        this(plugin, new BlockVector2D(location.getX(), location.getZ()));
    }

    public Coords2D(BTEConoSur plugin, @NotNull BlockVector2D point) {
        this.plugin = plugin;
        try {
            double[] geoCoords = geographicProjection.toGeo(point.getX(), point.getZ());
            initialize(point.getX(), point.getZ(), geoCoords[0], geoCoords[1]);
        } catch (OutOfProjectionBoundsException e) {
            e.printStackTrace();
        }
    }

    private void initialize(double x, double z, double lon, double lat) {
        this.x = x;
        this.z = z;
        this.lon = lon;
        this.lat = lat;
    }

    public BlockVector2D toBlockVector2D() {
        return new BlockVector2D(this.x, this.z);
    }

    public Location toHighestLocation() {
        return new Location(plugin.getWorld(), this.x, getHighestY(), this.z);
    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }

    public int getHighestY() {
        return plugin.getWorld().getHighestBlockYAt(new Location(plugin.getWorld(), this.x, 100, this.z));
    }
}
