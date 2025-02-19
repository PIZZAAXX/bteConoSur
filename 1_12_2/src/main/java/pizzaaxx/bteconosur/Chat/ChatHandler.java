package pizzaaxx.bteconosur.Chat;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Player.Managers.ChatManager;

import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHandler extends ListenerAdapter implements Listener, Prefixable {

    private final BTEConoSur plugin;
    private final Map<String, Chat> chats = new HashMap<>();


    public ChatHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public boolean isLoaded(String name) {
        return chats.containsKey(name);
    }

    public void registerChat(Chat chat) {
        chats.put(chat.getID(), chat);
        new BukkitRunnable() {
            @Override
            public void run() {
                tryUnregister(chat);
            }
        }.runTaskLaterAsynchronously(plugin, 6000);
    }

    public void tryUnregister(@NotNull Chat chat) {
        if (chat.isUnloadable()) {
            if (chat.getPlayers().isEmpty()) {
                chats.remove(chat.getID());
            }
        }
    }

    public Chat getChat(String name) {
        return chats.get(name);
    }

    @EventHandler
    public void onMessage(@NotNull AsyncPlayerChatEvent event) {
        event.setCancelled(true);
        Player player = event.getPlayer();

        ChatManager chatManager = plugin.getPlayerRegistry().get(player.getUniqueId()).getChatManager();

        if (chatManager.isHidden()) {
            player.sendMessage(getPrefix() + "No puedes hablar mientras tienes el chat oculto.");
            return;
        }

        String message = event.getMessage();

        Pattern pattern = Pattern.compile("@[a-zA-Z0-9_]{1,32}");
        Matcher matcher = pattern.matcher(message);

        Chat originChat;
        try {
            originChat = plugin.getPlayerRegistry().get(player.getUniqueId()).getChatManager().getCurrentChat();
        } catch (SQLException e) {
            plugin.warn("Problem with Chat Manager: " + player.getUniqueId());
            player.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
            return;
        }

        Set<Chat> targetChats = new HashSet<>();
        Set<UUID> targetPlayers = new HashSet<>();

        while (matcher.find()) {
            String match = matcher.group();
            String partialName = match.replace("@", "");
            Player target = plugin.getOnlinePlayer(partialName);
            if (target != null) {
                try {
                    ChatManager targetChatManager = plugin.getPlayerRegistry().get(target.getUniqueId()).getChatManager();
                    if (!targetChatManager.isHidden()) {
                        message = matcher.replaceAll("§a" + target.getName() + "§f");
                        targetChats.add(targetChatManager.getCurrentChat());
                        targetPlayers.add(target.getUniqueId());
                    } else {
                        message = matcher.replaceAll("§7" + target.getName() + "§f");
                    }
                } catch (SQLException e) {
                    plugin.warn("Problem with Chat Manager: " + target.getUniqueId());
                }
            }
        }

        originChat.sendMessage(player.getUniqueId(), message);
        for (Chat chat : targetChats) {
            if (chat != originChat) {
                chat.sendMessageFromOther(originChat, player.getUniqueId(), message);
            }
        }

        for (UUID uuid : targetPlayers) {
            Player target = Bukkit.getPlayer(uuid);
            BukkitRunnable pitch1 = new BukkitRunnable() {
                @Override
                public void run() {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 1, 1);
                }
            };
            BukkitRunnable pitch2 = new BukkitRunnable() {
                @Override
                public void run() {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 1, 2);
                }
            };
            BukkitRunnable pitch3 = new BukkitRunnable() {
                @Override
                public void run() {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 1, 1);
                }
            };
            BukkitRunnable pitch4 = new BukkitRunnable() {
                @Override
                public void run() {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 1, 2);
                }
            };
            BukkitRunnable pitch5 = new BukkitRunnable() {
                @Override
                public void run() {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 1, 1);
                }
            };
            BukkitRunnable pitch6 = new BukkitRunnable() {
                @Override
                public void run() {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BELL, 1, 2);
                }
            };

            pitch1.runTaskLaterAsynchronously(plugin, 2);
            pitch2.runTaskLaterAsynchronously(plugin, 4);
            pitch3.runTaskLaterAsynchronously(plugin, 6);
            pitch4.runTaskLaterAsynchronously(plugin, 8);
            pitch5.runTaskLaterAsynchronously(plugin, 10);
            pitch6.runTaskLaterAsynchronously(plugin, 12);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (event.getChannelType() != ChannelType.TEXT) {
            return;
        }

        User user = event.getMember().getUser();

        if (user.isBot()) {
            return;
        }

        String discriminator = (user.getDiscriminator().equals("0000") ? "" : "#" + user.getDiscriminator());

        String channelID = event.getChannel().getId();
        if (plugin.getCountryManager().globalChannels.contains(channelID)) {

            Chat chat = this.getChat("global");

            Country originCountry = plugin.getCountryManager().guilds.get(event.getGuild().getId());

            StringBuilder newMessage = new StringBuilder();
            newMessage.append(":flag_").append(originCountry.getAbbreviation()).append(": **").append(user.getName()).append(discriminator).append(":** ").append(event.getMessage().getContentDisplay());
            for (Country country : plugin.getCountryManager().getAllCountries()) {
                if (country != originCountry) {
                    MessageCreateAction action = country.getGlobalChatChannel().sendMessage(newMessage);
                    if (event.getMessage().getAttachments().size() > 0) {
                        action.addActionRow(
                                        Button.link(
                                                event.getMessage().getJumpUrl(),
                                                "Ver archivo(s) adjunto(s)"
                                        )
                                );
                    }
                    action.queue();
                }
            }

            chat.broadcast("[§bDISCORD§f] §7>> §a" + user.getName() + discriminator + "§f: " + event.getMessage().getContentDisplay(), false);

        } else if (plugin.getCountryManager().countryChannels.containsKey(channelID)) {

            Country country = plugin.getCountryManager().countryChannels.get(channelID);
            Chat chat = this.getChat(country.getName());

            chat.broadcast("[§bDISCORD§f] §7>> §a" + user.getName() + discriminator + "§f: " + event.getMessage().getContentDisplay(), false);

        }
    }

    @Override
    public String getPrefix() {
        return "§f[§aCHAT§f] §7>> §f";
    }
}
