package pizzaaxx.bteconosur.Discord.Link;

import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LinksRegistry {

    private final BTEConoSur plugin;
    public final Map<String, UUID> uuidFromID = new HashMap<>();

    public LinksRegistry(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException, IOException {

        ResultSet set = plugin.getSqlManager().select(
                "discord_managers",
                new SQLColumnSet(
                        "uuid", "id"
                ),
                new SQLANDConditionSet()
        ).retrieve();

        while (set.next()) {
            uuidFromID.put(set.getString("id"), plugin.getSqlManager().getUUID(set, "uuid"));
        }

    }

    public boolean isLinked(String id) {
        return uuidFromID.containsKey(id);
    }

    public UUID get(String id) {
        return uuidFromID.get(id);
    }
}
