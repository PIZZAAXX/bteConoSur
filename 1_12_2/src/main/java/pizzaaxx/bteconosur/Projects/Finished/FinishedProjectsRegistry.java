package pizzaaxx.bteconosur.Projects.Finished;

import org.bukkit.scheduler.BukkitRunnable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FinishedProjectsRegistry {

    private final BTEConoSur plugin;
    private final Map<String, FinishedProject> projectsCache = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();
    private final Set<String> ids = new HashSet<>();

    public Set<String> getIds() {
        return ids;
    }

    public FinishedProjectsRegistry(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "finished_projects",
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
            projectsCache.put(id, new FinishedProject(plugin, id));
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

    public FinishedProject get(String id) {
        if (!this.isLoaded(id)) {
            try {
                load(id);
            } catch (SQLException | IOException e) {
                e.printStackTrace();
                plugin.error("Error with finished project " + id + ".");
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
}
