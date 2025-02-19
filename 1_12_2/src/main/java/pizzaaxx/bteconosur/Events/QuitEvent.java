package pizzaaxx.bteconosur.Events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.SQL.Conditions.SQLANDConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.SQL.Values.SQLValue;
import pizzaaxx.bteconosur.SQL.Values.SQLValuesSet;

import java.awt.*;
import java.sql.SQLException;

public class QuitEvent implements Listener {

    private final BTEConoSur plugin;

    public QuitEvent(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(@NotNull PlayerQuitEvent event) throws SQLException {
        ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
        try {
            Chat chat = serverPlayer.getChatManager().getCurrentChat();
            chat.removePlayer(event.getPlayer().getUniqueId());
        } catch (SQLException e) {
            plugin.error("Error loading chat: " + serverPlayer.getChatManager().getCurrentChatName());
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.RED);
        embedBuilder.setAuthor(plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId()).getName() + " ha salido del servidor.", null,
                "https://cravatar.eu/helmavatar/" + event.getPlayer().getUniqueId() + "/190.png");
        MessageEmbed embed = embedBuilder.build();
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            country.getGlobalChatChannel().sendMessageEmbeds(embed).queue();
        }

        plugin.getScoreboardHandler().unregisterAuto(serverPlayer.getUUID());
        plugin.getScoreboardHandler().unregisterDisplay(serverPlayer.getUUID());
        plugin.getScoreboardHandler().update(plugin);

        plugin.getSqlManager().update(
                "players",
                new SQLValuesSet(
                        new SQLValue(
                                "last_disconnected", System.currentTimeMillis()
                        )
                ),
                new SQLANDConditionSet(
                        new SQLOperatorCondition(
                                "uuid", "=", event.getPlayer().getUniqueId()
                        )
                )
        ).execute();
    }

}
