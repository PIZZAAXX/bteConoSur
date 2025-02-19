package pizzaaxx.bteconosur.Projects;

import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Projects.Actions.CreateProjectAction;
import pizzaaxx.bteconosur.Projects.Actions.DeleteProjectAction;
import pizzaaxx.bteconosur.Projects.RegionSelectors.ProjectRegionSelector;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class ProjectRegistry {

    private final Map<String, Project> projectsCache = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();
    private final Set<String> ids = new HashSet<>();

    public Set<String> getIds() {
        return ids;
    }

    private final BTEConoSur plugin;

    public ProjectRegistry(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "id"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            ids.add(set.getString("id"));
        }
    }

    public boolean exists(String id) {
        return ids.contains(id);
    }

    public void load(String id) throws SQLException, IOException {
        if (this.exists(id)) {
            projectsCache.put(id, new Project(plugin, id));
        }
    }

    public void unload(String id) {
        if (this.isLoaded(id)) {
            deletionCache.remove(id);
            projectsCache.remove(id);
        }
    }

    public void registerID(String id){
        ids.add(id);
    }

    public void unregisterID(String id){
        ids.remove(id);
    }

    public CreateProjectAction createProject(Country country, ProjectType type, int points, List<BlockVector2D> region) {
        return new CreateProjectAction(plugin, country, type, points, region);
    }

    public DeleteProjectAction deleteProject(Project project, UUID moderator) {
        return new DeleteProjectAction(plugin, project, moderator);
    }

    public Project get(String id) {
        if (!this.isLoaded(id)) {
            try {
                load(id);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                plugin.error("Error with project " + id + ".");
            }
        }
        this.scheduleDeletion(id);
        return projectsCache.get(id);
    }

    public boolean isLoaded(String id) {
        return projectsCache.containsKey(id);
    }

    private void scheduleDeletion(String id) {
        if (this.isLoaded(id)) {
            deletionCache.put(id, System.currentTimeMillis());
            BukkitRunnable runnable = new BukkitRunnable() {
                @Override
                public void run() {
                    if (deletionCache.containsKey(id) && System.currentTimeMillis() - deletionCache.get(id) > 550000) {
                        unload(id);
                    }
                }
            };
            runnable.runTaskLaterAsynchronously(plugin, 12000);
        }
    }

    public List<String> getProjectsAt(Location loc, ProjectRegionSelector... selectors) {
        ApplicableRegionSet set = plugin.getRegionManager().getApplicableRegions(loc);
        List<String> ids = new ArrayList<>();
        for (ProtectedRegion region : set) {
            if (region.getId().startsWith("project_")) {
                String id = region.getId().replace("project_", "");
                if (!this.exists(id)) {
                    continue;
                }
                Project project = this.get(id);
                boolean applies = true;
                for (ProjectRegionSelector selector : selectors) {
                    if (!selector.applies(project)) {
                        applies = false;
                        break;
                    }
                }
                if (applies) {
                    ids.add(region.getId().replace("project_", ""));
                }
            }
        }
        return ids;
    }
}
