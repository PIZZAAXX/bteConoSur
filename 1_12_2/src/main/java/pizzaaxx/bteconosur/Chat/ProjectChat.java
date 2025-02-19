package pizzaaxx.bteconosur.Chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import xyz.upperlevel.spigot.book.BookUtil;

import java.util.*;

public class  ProjectChat implements Chat {

    private final String prefix = "§f[§aCHAT§f] §7>> §f";
    private final BTEConoSur plugin;
    private final ChatHandler handler;
    private final String projectID;

    private final Set<UUID> players = new HashSet<>();

    public ProjectChat(@NotNull Project project, ChatHandler handler) {
        this.plugin = project.getPlugin();
        this.projectID = project.getId();
        this.handler = handler;
    }

    public Project getProject() {
        return plugin.getProjectRegistry().get(projectID);
    }

    @Override
    public boolean isUnloadable() {
        return true;
    }

    @Override
    public String getID() {
        return "project_" + projectID;
    }

    @Override
    public String getDisplayName() {
        return "Proyecto " + this.getProject().getDisplayName();
    }

    @Override
    public String getEmoji() {
        return ":hammer_pick:";
    }

    @Override
    public boolean acceptsPlayer(UUID uuid) {
        return this.getProject().getAllMembers().contains(uuid);
    }

    @Override
    public Set<UUID> getPlayers() {
        return players;
    }

    @Override
    public void addPlayer(UUID uuid) {
        if (!players.contains(uuid)) {

            String role;
            Project project = this.getProject();
            if (project.getOwner().equals(uuid)) {
                role = "Líder";
            } else if (project.getMembers().contains(uuid)) {
                role = "Miembro";
            } else {
                role = "Invitado";
            }
            this.broadcast(prefix + "§a" + plugin.getPlayerRegistry().get(uuid).getName() + " §7(" + role + ")§f ha entrado al chat.", false);

            players.add(uuid);
        }
    }

    @Override
    public void removePlayer(UUID uuid) {
        if (players.contains(uuid)) {
            players.remove(uuid);

            String role;
            Project project = this.getProject();
            if (project.getOwner().equals(uuid)) {
                role = "Líder";
            } else if (project.getMembers().contains(uuid)) {
                role = "Miembro";
            } else {
                role = "Invitado";
            }
            this.broadcast(prefix + "§a" + plugin.getPlayerRegistry().get(uuid).getName() + " §7(" + role + ")§f ha salido del chat.", false);
            handler.tryUnregister(this);
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
        Project.ProjectRole projectRole = getProject().getProjectRole(uuid);
        for (UUID playerUUID : players) {
            ServerPlayer serverPlayer = plugin.getPlayerRegistry().get(playerUUID);
            if (!serverPlayer.getChatManager().isHidden()) {
                Bukkit.getPlayer(playerUUID).sendMessage(
                        BookUtil.TextBuilder.of(projectRole.getPrefix() + "<").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of(senderPlayer.getChatManager().getDisplayName()).onHover(BookUtil.HoverAction.showText(String.join("\n", senderPlayer.getLore(true)))).build(),
                        BookUtil.TextBuilder.of("> ").color(ChatColor.WHITE).build(),
                        BookUtil.TextBuilder.of(message).color(ChatColor.WHITE).build()
                );
            }
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
        Project project = this.getProject();

        ItemStack stack = project.getItem();

        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName("§aChat del proyecto " + project.getDisplayName());
        meta.setLore(
                Collections.singletonList(
                        "§fJugadores: §7" + players.size()
                )
        );
        stack.setItemMeta(meta);

        return stack;
    }
}
