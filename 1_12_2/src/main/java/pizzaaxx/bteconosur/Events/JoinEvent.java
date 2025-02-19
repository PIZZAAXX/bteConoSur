package pizzaaxx.bteconosur.Events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.Managers.ScoreboardManager;
import pizzaaxx.bteconosur.Player.Notifications.Notification;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class JoinEvent implements Listener {

    private final BTEConoSur plugin;

    public JoinEvent(BTEConoSur bteConoSur) {
        this.plugin = bteConoSur;
    }

    @EventHandler
    public void onJoin(@NotNull PlayerJoinEvent event) throws SQLException {
        plugin.getPlayerRegistry().load(event.getPlayer().getUniqueId());
        event.getPlayer().setGameMode(GameMode.CREATIVE);
        ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.GREEN);
        embedBuilder.setAuthor(plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId()).getName() + " ha entrado al servidor.", null,
                "https://cravatar.eu/helmavatar/" + event.getPlayer().getUniqueId() + "/190.png");
        MessageEmbed embed = embedBuilder.build();
        for (Country country : plugin.getCountryManager().getAllCountries()) {
            country.getGlobalChatChannel().sendMessageEmbeds(embed).queue();
        }
        ScoreboardManager scoreboardManager = serverPlayer.getScoreboardManager();
        scoreboardManager.loadDisplay(); // <- If player was loaded before joining.
        try {
            Chat defaultChat = serverPlayer.getChatManager().getDefaultChat();
            serverPlayer.getChatManager().setCurrentChat(defaultChat);
            defaultChat.addPlayer(event.getPlayer().getUniqueId());
        } catch (SQLException e) {
            plugin.error("Error loading chat: " + serverPlayer.getChatManager().getCurrentChatName());
        }

        if (scoreboardManager.isAuto()) {
            plugin.getScoreboardHandler().registerAuto(serverPlayer.getUUID());
        }
        try {
            scoreboardManager.setDisplay(scoreboardManager.getDisplay());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (!serverPlayer.getChatManager().hasCountryPrefix()) {
            plugin.getPrefixCommand().openPrefixMenu(event.getPlayer());
        }

        plugin.getScoreboardHandler().update(plugin);

        event.getPlayer().sendMessage("§8+---------------------------------------------------+");
        event.getPlayer().sendMessage("§7                     §2§lBuildTheEarth: §a§lCono Sur");
        event.getPlayer().sendMessage("§8+---------------------------------------------------+");
        List<Notification> notifications = plugin.getNotificationsService().getNotifications(event.getPlayer().getUniqueId());
        if (!notifications.isEmpty()) {
            event.getPlayer().sendMessage("§7 Notificaciones:");
            int counter = 1;
            for (Notification notification : notifications) {
                event.getPlayer().sendMessage("§7" + counter + ". " + notification.getMinecraftMessage());
            }
            event.getPlayer().sendMessage("§8+---------------------------------------------------+");
        }
        plugin.getNotificationsService().deleteNotifications(event.getPlayer().getUniqueId());
    }
}
