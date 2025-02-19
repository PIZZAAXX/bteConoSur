package pizzaaxx.bteconosur.Regions;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Utils.StringMatcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RegionListenersHandler implements Listener {

    private final BTEConoSur plugin;

    private final Map<StringMatcher, RegionListener> enterListeners = new HashMap<>();
    private final Map<StringMatcher, RegionListener> leaveListeners = new HashMap<>();

    public RegionListenersHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public BTEConoSur getPlugin() {
        return plugin;
    }

    public void registerEnter(StringMatcher matcher, RegionListener listener) {
        enterListeners.put(matcher, listener);
    }

    public void registerEnter(String regionID, RegionListener listener) {
        enterListeners.put(input -> input.equals(regionID), listener);
    }

    public void registerLeave(StringMatcher matcher, RegionListener listener) {
        leaveListeners.put(matcher, listener);
    }

    public void registerLeave(String regionID, RegionListener listener) {
        leaveListeners.put(input -> input.equals(regionID), listener);
    }

    public void registerBoth(StringMatcher matcher, RegionListener listener) {
        enterListeners.put(matcher, listener);
        leaveListeners.put(matcher, listener);
    }

    public void registerBoth(String regionID, RegionListener listener) {
        enterListeners.put(input -> input.equals(regionID), listener);
        leaveListeners.put(input -> input.equals(regionID), listener);
    }

    @EventHandler
    public void onMove(@NotNull PlayerMoveEvent event) {
        this.checkListeners(event.getFrom(), event.getTo(), event.getPlayer());
    }

    @EventHandler
    public void onTeleport(@NotNull PlayerTeleportEvent event) {
        this.checkListeners(event.getFrom(), event.getTo(), event.getPlayer());
    }

    private void checkListeners(Location from, Location to, Player player) {
        for (ProtectedRegion region : this.getLeaveRegions(from, to)) {
            for (Map.Entry<StringMatcher, RegionListener> entry : leaveListeners.entrySet()) {
                if (entry.getKey().matches(region.getId())) {
                    RegionLeaveEvent leaveEvent = new RegionLeaveEvent(
                            plugin,
                            player,
                            from,
                            to,
                            region.getId(),
                            region
                    );
                    entry.getValue().onRegionLeave(leaveEvent);
                }
            }
        }

        for (ProtectedRegion region : this.getEnterRegions(from, to)) {
            for (Map.Entry<StringMatcher, RegionListener> entry : enterListeners.entrySet()) {
                if (entry.getKey().matches(region.getId())) {
                    RegionEnterEvent enterEvent = new RegionEnterEvent(
                            plugin,
                            player,
                            from,
                            to,
                            region.getId(),
                            region
                    );
                    entry.getValue().onRegionEnter(enterEvent);
                }
            }
        }
    }

    private @NotNull Set<ProtectedRegion> getLeaveRegions(Location from, Location to) {
        Set<ProtectedRegion> fromRegions = plugin.getApplicableRegions(from);
        Set<ProtectedRegion> toRegions = plugin.getApplicableRegions(to);

        Set<ProtectedRegion> result = new HashSet<>();
        for (ProtectedRegion region : fromRegions) {
            if (!toRegions.contains(region)) {
                result.add(region);
            }
        }
        return result;
    }

    private @NotNull Set<ProtectedRegion> getEnterRegions(Location from, Location to) {
        Set<ProtectedRegion> fromRegions = plugin.getApplicableRegions(from);
        Set<ProtectedRegion> toRegions = plugin.getApplicableRegions(to);

        Set<ProtectedRegion> result = new HashSet<>();
        for (ProtectedRegion region : toRegions) {
            if (!fromRegions.contains(region)) {
                result.add(region);
            }
        }
        return result;
    }

}
