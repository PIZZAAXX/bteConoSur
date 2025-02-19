package pizzaaxx.bteconosur.Projects.Listeners;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.ScoreboardManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Regions.RegionEnterEvent;
import pizzaaxx.bteconosur.Regions.RegionLeaveEvent;
import pizzaaxx.bteconosur.Regions.RegionListener;

import java.sql.SQLException;

public class ProjectRegionListener extends RegionListener {

    private final BTEConoSur plugin;

    public ProjectRegionListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onRegionEnter(@NotNull RegionEnterEvent event) {
        String projectID = event.getRegionID().replace("project_", "");
        if (plugin.getProjectRegistry().exists(projectID)) {
            Project project = plugin.getProjectRegistry().get(projectID);
            if (project.isClaimed()) {
                event.getPlayer().sendActionBar("§eProyecto " + project.getDisplayName() + " §8- §7" + plugin.getPlayerRegistry().get(project.getOwner()).getName());
            } else {
                event.getPlayer().sendActionBar("§aProyecto " + project.getDisplayName() + " §8- §7Disponible");
            }
        }

        ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
        ScoreboardManager manager = s.getScoreboardManager();
        if (manager.getDisplayClass() == Project.class) {
            try {
                manager.setDisplay(plugin.getScoreboardHandler().getDisplay(Project.class, s, event.getTo()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRegionLeave(@NotNull RegionLeaveEvent event) {
        ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
        ScoreboardManager manager = s.getScoreboardManager();
        if (manager.getDisplayClass() == Project.class) {
            try {
                manager.setDisplay(plugin.getScoreboardHandler().getDisplay(Project.class, s, event.getTo()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
