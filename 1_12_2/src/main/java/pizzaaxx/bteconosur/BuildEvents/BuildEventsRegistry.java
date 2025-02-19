package pizzaaxx.bteconosur.BuildEvents;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bukkit.scheduler.BukkitRunnable;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BuildEventsRegistry {

    private final BTEConoSur plugin;
    private final Map<String, BuildEvent> cache = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();
    private final Set<String> ids = new HashSet<>();
    public final Map<String, String> channelIDToEventID = new HashMap<>();

    public BuildEventsRegistry(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException {
        ResultSet set = plugin.getSqlManager().select(
                "build_events",
                new SQLColumnSet(
                        "id", "post_channel_id"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            this.ids.add(set.getString("id"));
            this.channelIDToEventID.put(set.getString("post_channel_id"), set.getString("id"));
        }
    }

    public Set<String> getIds() {
        return ids;
    }

    public void addId(String id) {
        this.ids.add(id);
    }

    public BuildEvent get(String id) {

        if (!cache.containsKey(id)) {
            try {
                cache.put(id, new BuildEvent(plugin, id));
            } catch (SQLException | JsonProcessingException ignored) {}
        }

        this.scheduleDeletion(id);
        return cache.get(id);
    }

    private void scheduleDeletion(String id) {
        this.deletionCache.put(id, System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (deletionCache.containsKey(id) && System.currentTimeMillis() - deletionCache.get(id) > 10000) {
                    deletionCache.remove(id);
                    cache.remove(id);
                }
            }
        }.runTaskLaterAsynchronously(plugin, 12000);
    }

}
