package pizzaaxx.bteconosur.Events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class PreLoginEvent implements Listener {

    private final BTEConoSur plugin;
    private final Random random = new Random();
    private final int MAX_PLAYER_AMOUNT = 25;

    public PreLoginEvent(BTEConoSur plugin) {
        this.plugin = plugin;
    }


    // THIS HAS TO BE SYNC BECAUSE IF NOT DATA WON'T BE AVAILABLE ON LOGIN
    @EventHandler
    public void onPreLogin(@NotNull PlayerPreLoginEvent event) {

        UUID uuid = event.getUniqueId();
        try {
            ResultSet set = plugin.getSqlManager().select(
                    "players",
                    new SQLColumnSet(
                            "name"
                    ),
                    new SQLANDConditionSet(
                            new SQLOperatorCondition("uuid", "=", event.getUniqueId())
                    )
            ).retrieve();
            if (set.next()) {
                if (!set.getString("name").equals(event.getName())) {
                    plugin.getSqlManager().update(
                            "players",
                            new SQLValuesSet(
                                    new SQLValue("name", event.getName())
                            ),
                            new SQLANDConditionSet(
                                    new SQLOperatorCondition("uuid", "=", event.getUniqueId())
                            )
                    ).execute();
                    plugin.log("Updated name of player with UUID §f" + uuid + "§7: §f" + set.getString("name") + "§7 > §f" + event.getName());
                }
            } else {
                plugin.getSqlManager().insert(
                        "players",
                        new SQLValuesSet(
                                new SQLValue("uuid", event.getUniqueId()),
                                new SQLValue("name", event.getName())
                        )
                ).execute();
                plugin.getPlayerRegistry().registerUUID(event.getUniqueId());
                plugin.log("Created database registry for player with UUID §f" + uuid);
            }

            if (Bukkit.getOnlinePlayers().size() >= MAX_PLAYER_AMOUNT) {
                ServerPlayer s = plugin.getPlayerRegistry().get(uuid);

                int priority = s.getPriority();

                Map<UUID, Integer> priorities = new HashMap<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    ServerPlayer s2 = plugin.getPlayerRegistry().get(p.getUniqueId());
                    priorities.put(s2.getUUID(), s2.getPriority());
                }

                int min = Collections.min(priorities.values());
                if (priority <= min) {
                    event.disallow(PlayerPreLoginEvent.Result.KICK_OTHER, "El servidor está lleno y no tienes suficiente prioridad de entrada. Sube de rango o dona para tener más prioridad.");
                    return;
                }

                List<UUID> mins = new ArrayList<>();
                for (Map.Entry<UUID, Integer> entry : priorities.entrySet()) {
                    if (entry.getValue() == min) {
                        mins.add(entry.getKey());
                    }
                }

                Bukkit.getPlayer(mins.get(random.nextInt(mins.size()))).kickPlayer("Ha entrado alguien con mayor prioridad de entrada y has sido aleatoriamente sacado del servidor. Sube de rango o dona para tener más prioridad.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            event.disallow(PlayerPreLoginEvent.Result.KICK_OTHER, "Ha ocurrido un error con la base de datos.");
        }
    }
}
