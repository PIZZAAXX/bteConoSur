package pizzaaxx.bteconosur.Projects.Listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.RegionSelectors.MemberProjectSelector;
import pizzaaxx.bteconosur.Projects.RegionSelectors.NonMemberProjectSelector;

import java.util.List;

public class ProjectClickListener implements Listener {

    private final BTEConoSur plugin;

    public ProjectClickListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getItem() == null || event.getItem().getType() == Material.WOOD_AXE) {
                return;
            }

            if (plugin.getProjectRegistry().getProjectsAt(event.getClickedBlock().getLocation(), new MemberProjectSelector(event.getPlayer().getUniqueId())).isEmpty()) {
                List<String> ids = plugin.getProjectRegistry().getProjectsAt(
                        event.getClickedBlock().getLocation(),
                        new NonMemberProjectSelector(event.getPlayer().getUniqueId())
                );

                if (ids.isEmpty()) {

                    event.getPlayer().sendActionBar("§cCrea un proyecto para construir aquí.");

                } else {
                    Project project = plugin.getProjectRegistry().get(ids.get(0));

                    if (project.isClaimed()) {
                        event.getPlayer().sendActionBar("§cEnvía una solicitud de unión para construir aquí.");
                    } else {
                        event.getPlayer().sendActionBar("§cReclama este proyecto para construir aquí.");
                    }
                }
            }
        }
    }
}
