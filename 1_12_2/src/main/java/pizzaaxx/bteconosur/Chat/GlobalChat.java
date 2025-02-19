package pizzaaxx.bteconosur.Chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalChat implements Chat {

    private final BTEConoSur plugin;
    private final ChatHandler handler;


    Set<UUID> players = new HashSet<>();

    public GlobalChat(BTEConoSur plugin, ChatHandler handler) {
        this.plugin = plugin;
        this.handler = handler;
    }

    @Override
    public boolean isUnloadable() {
        return false;
    }

    @Override
    public String getID() {
        return "global";
    }

    @Override
    public String getDisplayName() {
        return "Global";
    }

    @Override
    public String getEmoji() {
        return ":globe_with_meridians:";
    }

    @Override
    public boolean acceptsPlayer(UUID uuid) {
        return true;
    }

    @Override
    public Set<UUID> getPlayers() {
        return players;
    }

    @Override
    public void addPlayer(UUID uuid) {
        if (!players.contains(uuid)) {
            ServerPlayer s = plugin.getPlayerRegistry().get(uuid);
            this.broadcast(handler.getPrefix() + "§a" + s.getName() + " §fha entrado al chat.", false);
            players.add(uuid);
            for (Country country : plugin.getCountryManager().getAllCountries()) {
                country.getGlobalChatChannel().sendMessage("<:plus:1042295433969537055> **" + s.getName() + "** ha entrado al chat.").queue();
            }
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        if (players.contains(uuid)) {
            players.remove(uuid);
            ServerPlayer s = plugin.getPlayerRegistry().get(uuid);
            this.broadcast(handler.getPrefix() + "§a" + s.getName() + " §fha salido del chat.", false);
            handler.tryUnregister(this);
            for (Country country : plugin.getCountryManager().getAllCountries()) {
                country.getGlobalChatChannel().sendMessage("<:minus:1042295467322654736> **" + plugin.getPlayerRegistry().get(uuid).getName() + "** ha salido del chat.").queue();
            }
        }
    }

    @Override
    public void sendMessageFromOther(Chat originChat, UUID uuid, String message) {
        ServerPlayer senderPlayer = plugin.getPlayerRegistry().get(uuid);
        for (UUID playerUUID : players) {
            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(playerUUID);
            if (!serverPlayer.getChatManager().isHidden()) {
                Bukkit.getPlayer(playerUUID).sendMessage(
                        BookUtil.TextBuilder.of("§f[§ePING§f] §7(" + originChat.getDisplayName() + ") §f<").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of(senderPlayer.getChatManager().getDisplayName()).onHover(BookUtil.HoverAction.showText(String.join("\n", senderPlayer.getLore(true)))).build(),
                        BookUtil.TextBuilder.of("> ").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of(message).color(ChatColor.WHITE).build()
                );
            }
        }
    }

    @Override
    public void sendMessage(UUID uuid, String message) {
        ServerPlayer senderPlayer = plugin.getPlayerRegistry().get(uuid);

        ServerPlayer.BuilderRank builderRank = senderPlayer.getBuilderRank();
        List<ServerPlayer.SecondaryRoles> secondaryRoles = senderPlayer.getSecondaryRoles();

        List<PrefixHolder> prefixHolders = new ArrayList<>(secondaryRoles);
        if (!(builderRank == ServerPlayer.BuilderRank.VISITA && secondaryRoles.size() > 0)) {
            prefixHolders.add(builderRank);
        }

        prefixHolders.add(senderPlayer.getChatManager());

        for (UUID playerUUID : players) {
            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(playerUUID);
            if (!serverPlayer.getChatManager().isHidden()) {
                Bukkit.getPlayer(playerUUID).sendMessage(
                        BookUtil.TextBuilder.of(prefixHolders.stream().map(PrefixHolder::getPrefix).collect(Collectors.joining()) + "<").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of(senderPlayer.getChatManager().getDisplayName()).onHover(BookUtil.HoverAction.showText(String.join("\n", senderPlayer.getLore(true)))).build(),
                        BookUtil.TextBuilder.of("> ").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of(message).color(ChatColor.WHITE).build()
                );
            }
        }

        for (Country country : plugin.getCountryManager().getAllCountries()) {
            country.getGlobalChatChannel().sendMessage(
                    "<:chat:1042295395625209886> **" + prefixHolders.stream().map(PrefixHolder::getDiscordPrefix).collect(Collectors.joining()) + senderPlayer.getName() + ":** " + ChatColor.stripColor(message)
            ).queue();
        }
    }

    @Override
    public void broadcast(String message, boolean ignoreHidden) {
        for (UUID uuid : players) {
            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(uuid);
            if (ignoreHidden || !serverPlayer.getChatManager().isHidden()) {
                Bukkit.getPlayer(uuid).sendMessage(message);
            }
        }
    }

    @Override
    public ItemStack getHead() {
        return ItemBuilder.head(
                "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOThkYWExZTNlZDk0ZmYzZTMzZTFkNGM2ZTQzZjAyNGM0N2Q3OGE1N2JhNGQzOGU3NWU3YzkyNjQxMDYifX19",
                "§aChat global",
                Collections.singletonList(
                        "§fJugadores: §7" + players.size()
                )
        );
    }
}
