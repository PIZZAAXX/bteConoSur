package pizzaaxx.bteconosur.Player.Managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MiscManager {

    private final BTEConoSur plugin;
    private final ServerPlayer serverPlayer;
    private final Map<String, Location> pwarps = new HashMap<>();

    public MiscManager(@NotNull BTEConoSur plugin, ServerPlayer serverPlayer, @NotNull ResultSet set) throws SQLException, JsonProcessingException {
        this.plugin = plugin;
        this.serverPlayer = serverPlayer;

        Map<String, Object> pwarpsRaw = plugin.getJSONMapper().readValue(set.getString("pwarps"), HashMap.class);
        for (Map.Entry<String, Object> entry : pwarpsRaw.entrySet()) {
            Map<String, Double> coords = (Map<String, Double>) entry.getValue();
            pwarps.put(entry.getKey(), new Location(plugin.getWorld(), coords.get("x"), coords.get("y"), coords.get("z")));
        }
    }

    public boolean existsPWarp(String name) {
        return pwarps.containsKey(name);
    }

    public Location getPWarp(String name) {
        return pwarps.get(name);
    }

    public Map<String, Location> getPWarps() {
        return pwarps;
    }

    public void setPWarp(String name, Location loc) throws SQLException {
        pwarps.put(name, loc);
        this.updatePWarps();
    }

    public void deletePwarp(String name) throws SQLException {
        if (this.existsPWarp(name)) {
            pwarps.remove(name);
            this.updatePWarps();
        }
    }

    private void updatePWarps() throws SQLException {
        plugin.getSqlManager().update(
                "players",
                new SQLValuesSet(
                        new SQLValue(
                                "pwarps", pwarps
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", serverPlayer.getUUID()
                        )
                )
        ).execute();

        plugin.getScoreboardHandler().update(serverPlayer);

    }

}
