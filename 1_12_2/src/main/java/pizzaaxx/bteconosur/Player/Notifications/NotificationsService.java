package pizzaaxx.bteconosur.Player.Notifications;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Player.Managers.DiscordManager;
import pizzaaxx.bteconosur.SQL.Actions.SelectAction;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificationsService {

    private final BTEConoSur plugin;

    public NotificationsService(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void sendNotification(
            UUID target,
            String minecraftMessage,
            String discordMessage
    ) {
        if (Bukkit.getOfflinePlayer(target).isOnline()) {
            Player p = Bukkit.getPlayer(target);
            p.sendMessage(minecraftMessage);
        } else if (plugin.getPlayerRegistry().get(target).getDiscordManager().isLinked()) {
            DiscordManager manager = plugin.getPlayerRegistry().get(target).getDiscordManager();
            plugin.getBot().openPrivateChannelById(manager.getId()).queue(
                    channel -> channel.sendMessage(discordMessage).queue()
            );
        } else {
            try {
                plugin.getSqlManager().insert(
                        "notifications",
                        new SQLValuesSet(
                                new SQLValue(
                                        "target", target
                                ),
                                new SQLValue(
                                        "minecraft_message", minecraftMessage
                                ),
                                new SQLValue(
                                        "discord_message", discordMessage
                                )
                        )
                ).execute();
            } catch (SQLException ignored) {}
        }
    }

    public void deleteNotifications(UUID uuid) throws SQLException {
        plugin.getSqlManager().delete(
                "notifications",
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "target", "=", uuid
                        )
                )
        ).execute();
    }

    public List<Notification> getNotifications(UUID uuid) throws SQLException {
        List<Notification> result = new ArrayList<>();

        SelectAction action = plugin.getSqlManager().select(
                "notifications",
                new SQLColumnSet(
                        "minecraft_message",
                        "discord_message"
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "target", "=", uuid
                        )
                )
        );
        action.addText(
                " ORDER BY date ASC"
        );
        ResultSet set = action.retrieve();

        while (set.next()) {
            Notification notification = new Notification(
                    set.getString("minecraft_message"),
                    set.getString("discord_message")
            );
            result.add(notification);
        }
        return result;
    }

}
