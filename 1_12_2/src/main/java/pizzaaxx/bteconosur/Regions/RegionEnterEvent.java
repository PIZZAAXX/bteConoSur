package pizzaaxx.bteconosur.Regions;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;

public class RegionEnterEvent {

    private final BTEConoSur plugin;
    private final Player player;
    private final Location from;
    private final Location to;
    private final String regionID;
    private final ProtectedRegion region;

    public RegionEnterEvent(BTEConoSur plugin, Player player, Location from, Location to, String regionID, ProtectedRegion region) {
        this.plugin = plugin;
        this.player = player;
        this.from = from;
        this.to = to;
        this.regionID = regionID;
        this.region = region;
    }

    public BTEConoSur getPlugin() {
        return plugin;
    }

    public Player getPlayer() {
        return player;
    }

    public Location getFrom() {
        return from;
    }

    public Location getTo() {
        return to;
    }

    public String getRegionID() {
        return regionID;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

}
