package pizzaaxx.bteconosur.Player;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PlayerRegistry { // REGISTRO CIVIL XD

    private final BTEConoSur plugin;
    private final Map<UUID, ServerPlayer> cache = new HashMap<>();
    private final Map<UUID, Long> deletionCache = new HashMap<>();
    private final Set<UUID> uuids = new HashSet<>();

    public PlayerRegistry(@NotNull BTEConoSur plugin) throws SQLException, IOException {
        this.plugin = plugin;

        ResultSet set = plugin.getSqlManager().select(
                "players",
                new SQLColumnSet(
                        "uuid"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            uuids.add(plugin.getSqlManager().getUUID(set, "uuid"));
        }
    }

    public boolean hasPlayedBefore(String name) throws SQLException {
        return plugin.getSqlManager().select(
                "players",
                new SQLColumnSet(
                        "name"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", name
                        )
                )
        ).retrieve().next();
    }

    public boolean hasPlayedBefore(UUID uuid) {
        return uuids.contains(uuid);
    }

    public Collection<ServerPlayer> getLoadedPlayers() {
        return cache.values();
    }

    public void registerUUID(UUID uuid) {
        uuids.add(uuid);
    }

    public void load(UUID uuid) {
        if (!cache.containsKey(uuid)) {
            try {
                cache.put(uuid, new ServerPlayer(plugin, uuid));
            } catch (SQLException | JsonProcessingException e) {
                e.printStackTrace();
                plugin.error("SQL error loading player data: " + uuid);
            }
        }
    }

    public boolean isLoaded(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public ServerPlayer get(String name) throws SQLException, IOException {
        ResultSet set = plugin.getSqlManager().select(
                "players",
                new SQLColumnSet(
                        "uuid"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "name", "=", name
                        )
                )
        ).retrieve();

        if (set.next()) {
            UUID uuid = plugin.getSqlManager().getUUID(set, "uuid");
            return this.get(uuid);
        } else {
            return null;
        }
    }

    public ServerPlayer get(UUID uuid) {
        if (this.hasPlayedBefore(uuid)) {
            if (!this.isLoaded(uuid)) {
                this.load(uuid);
            }
            if (!Bukkit.getOfflinePlayer(uuid).isOnline()) {
                this.scheduleUnload(uuid);
            }
            return cache.get(uuid);
        }
        return null;
    }

    public void unload(UUID uuid) {
        cache.remove(uuid);
        deletionCache.remove(uuid);
    }

    public void scheduleUnload(UUID uuid) {
        deletionCache.put(uuid, System.currentTimeMillis());
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (!Bukkit.getOfflinePlayer(uuid).isOnline() && deletionCache.containsKey(uuid) && System.currentTimeMillis() - deletionCache.get(uuid) > 550000) {
                    unload(uuid);
                }
            }
        };
        runnable.runTaskLaterAsynchronously(plugin, 12000);
    }

    public List<String> getNames(@NotNull Collection<UUID> names) {
        List<String> result = new ArrayList<>();

        for (UUID uuid : names) {
            result.add(this.get(uuid).getName());
        }

        return result;
    }

}
