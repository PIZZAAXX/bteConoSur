package pizzaaxx.bteconosur.join;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pizzaaxx.bteconosur.server.player.*;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.ArrayList;
import java.util.List;

import static pizzaaxx.bteconosur.chats.ChatCommand.CHAT_PREFIX;

public class Join implements Listener {

    private final PlayerRegistry playerRegistry;

    public Join(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        for (Player player : Bukkit.getOnlinePlayers()) {

            if (player.getUniqueId() != event.getPlayer().getUniqueId()) {
                ServerPlayer serverPlayer = playerRegistry.get(player.getUniqueId());

                ScoreboardManager.ScoreboardType scoreboard = serverPlayer.getScoreboardManager().getType();
                if (scoreboard == ScoreboardManager.ScoreboardType.SERVER) {
                    serverPlayer.getScoreboardManager().update();
                }
            }
        }

        playerRegistry.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        ServerPlayer serverPlayer = new ServerPlayer(player.getUniqueId());
        DataManager data = serverPlayer.getDataManager();

        event.setJoinMessage(player.getDisplayName() + " ha entrado al servidor.");


        // SEND MESSAGES

        player.sendMessage(">+--------------+[-< ============= >-]+--------------+<");
        player.sendMessage(" ");
        player.sendMessage("§7                                Bienvenido a");
        player.sendMessage("§7                      §9§lBuildTheEarth: §a§lCono Sur");
        player.sendMessage(" ");

        // NOTIFICACIONES

        if (player.hasPlayedBefore()) {

            if (serverPlayer.getNotifications().size() > 0) {
                player.sendMessage(">+--------------+[-< NOTIFICACIONES >-]+--------------+<");
                int i = 1;
                for (String notif : serverPlayer.getNotifications()) {
                    player.sendMessage(i + ". " + notif.replace("&", "§"));
                    player.sendMessage(" ");
                    i++;
                }

                data.set("notifications", null);
                data.save();
            } else if (!serverPlayer.getDiscordManager().isLinked()) {
                player.sendMessage(">+--------------+[-< NOTIFICACIONES >-]+--------------+<");
                player.sendMessage("§c                   No tienes notificaciones nuevas.");
                player.sendMessage(" ");
            }
        }

        // DISCORD

        if (!serverPlayer.getDiscordManager().isLinked()) {
            player.sendMessage(">+-----------------+[-< DISCORD >-]+-----------------+<");
            player.sendMessage(BookUtil.TextBuilder.of("§f               §f").build(), BookUtil.TextBuilder.of("§f[§aHAZ CLICK PARA CONECTAR TU CUENTA§f]").onHover(BookUtil.HoverAction.showText("Haz click para conectar tu cuenta.")).onClick(BookUtil.ClickAction.runCommand("/link")).build());
            player.sendMessage(" ");
        }

        player.sendMessage(">+--------------+[-< ============= >-]+--------------+<");

        // SET PLAYER'S CHAT TO DEFAULT

        ChatManager manager = serverPlayer.getChatManager();
        if (!manager.getChat().getName().equals(manager.getDefaultChat().getName())) {
            player.sendMessage(CHAT_PREFIX + "Te has unido al chat §a" + manager.getChat().getFormattedName() + "§f. §7(Jugadores: " + manager.getChat().getMembers().size() + ")");
        }
        manager.setChat(manager.getDefaultChat().getName());

        if (serverPlayer.getScoreboardManager().getType() == ScoreboardManager.ScoreboardType.ME) {
            serverPlayer.getScoreboardManager().update();
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            ServerPlayer serverPlayerOnline = new ServerPlayer(online.getUniqueId());
            if (serverPlayerOnline.getScoreboardManager().getType() == ScoreboardManager.ScoreboardType.SERVER) {
                serverPlayerOnline.getScoreboardManager().update();
            }
        }

        if (data.contains("isFirst")) {
            // TODO WELCOME BOOK

            BookUtil.BookBuilder builder = BookUtil.writtenBook();

            List<BaseComponent[]> pages = new ArrayList<>();

            BookUtil.PageBuilder page = new BookUtil.PageBuilder();

            page.add("      §7Bienvenido a");
            page.newLine();
            page.add("     §9§lBuildTheEarth:");
            page.newLine();
            page.add("       §a§lCono Sur");
            page.newLine();
            page.newLine();
            page.add("   §8¿Qué te gustaría");
            page.newLine();
            page.add("        §8hacer?");
            page.newLine();
            page.newLine();
            page.add("      ");
            page.add(
                    BookUtil.TextBuilder.of("[CONSTRUIR]").onClick(BookUtil.ClickAction.runCommand("welcomeBook build")).build()
            );
            page.newLine();
            page.newLine();
            page.add("       ");
            page.add(
                    BookUtil.TextBuilder.of("[VISITAR]").onClick(BookUtil.ClickAction.runCommand("welcomeBook visit")).build()
            );

            pages.add(page.build());

            builder.pages(pages);

            BookUtil.openPlayer(player, builder.build());
        }
    }
}
