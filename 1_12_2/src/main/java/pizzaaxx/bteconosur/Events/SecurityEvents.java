package pizzaaxx.bteconosur.Events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.RegionSelectors.MemberProjectSelector;

import java.util.List;

public class SecurityEvents implements Listener {

    private final BTEConoSur plugin;

    public SecurityEvents(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortal(@NotNull PortalCreateEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(@NotNull BlockIgniteEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onTNT(@NotNull ExplosionPrimeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onCommand(@NotNull PlayerCommandPreprocessEvent event) {
        if (event.getMessage().replace("/", "").startsWith("up")) {
            List<String> ids = plugin.getProjectRegistry().getProjectsAt(event.getPlayer().getLocation(), new MemberProjectSelector(event.getPlayer().getUniqueId()));
            event.setCancelled(ids.isEmpty());
        }
    }

    @EventHandler
    public void onBlock(@NotNull ItemSpawnEvent event) {
        event.getEntity().remove();
    }

    @EventHandler
    public void onInteract(@NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            event.setCancelled(true);
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
            if (!s.canBuild(event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
            }
        }
    }
}
