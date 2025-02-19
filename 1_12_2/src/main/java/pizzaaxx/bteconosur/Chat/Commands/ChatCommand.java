package pizzaaxx.bteconosur.Chat.Commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Chat.Chat;
import pizzaaxx.bteconosur.Chat.ChatHandler;
import pizzaaxx.bteconosur.Chat.Prefixable;
import pizzaaxx.bteconosur.Chat.ProjectChat;
import pizzaaxx.bteconosur.Countries.Country;
import pizzaaxx.bteconosur.Inventory.CustomSlotsPaginatedGUI;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.Managers.ChatManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.Projects.Project;
import pizzaaxx.bteconosur.Projects.ProjectTag;
import pizzaaxx.bteconosur.SQL.Columns.SQLColumnSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLJSONArrayCondition;
import pizzaaxx.bteconosur.SQL.Conditions.SQLORConditionSet;
import pizzaaxx.bteconosur.SQL.Conditions.SQLOperatorCondition;
import pizzaaxx.bteconosur.Utils.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static pizzaaxx.bteconosur.Utils.StringUtils.LOWER_CASE;

public class ChatCommand implements CommandExecutor, Prefixable, TabCompleter {

    private final BTEConoSur plugin;

    private final Map<String, String> invites = new HashMap<>();

    public ChatCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());
        ChatManager chatManager = s.getChatManager();

        if (args.length < 1) {
            try {
                Chat chat = chatManager.getCurrentChat();
                p.sendMessage(getPrefix() + "Tu chat actual es §a" + chat.getDisplayName() + "§f.");
            } catch (SQLException e) {
                p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                return true;
            }
        } else {
            ChatHandler handler = plugin.getChatHandler();
            try {

                if (args[0].equals("invite")) {

                    if (args.length < 2) {
                        p.sendMessage(getPrefix() + "Introduce un jugador.");
                        return true;
                    }

                    Player target = plugin.getOnlinePlayer(args[1]);

                    if (target == null) {
                        p.sendMessage(getPrefix() + "No se ha encontrado al jugador.");
                        return true;
                    }

                    ChatManager targetChatManager = plugin.getPlayerRegistry().get(target.getUniqueId()).getChatManager();

                    if (targetChatManager.getCurrentChatName().equals(chatManager.getCurrentChatName())) {
                        p.sendMessage(getPrefix() + "§a" + target.getName() + "§f ya está en tu chat.");
                        return true;
                    }

                    String code = StringUtils.generateCode(6, invites.keySet(), LOWER_CASE);
                    invites.put(code, chatManager.getCurrentChatName());

                    BukkitRunnable runnable = new BukkitRunnable() {
                        @Override
                        public void run() {
                            invites.remove(code);
                        }
                    };
                    runnable.runTaskLaterAsynchronously(plugin, 12000);

                    p.sendMessage(getPrefix() + "Invitación enviada a §a" + target.getName() + "§f.");
                    target.sendMessage(getPrefix() + "§a" + p.getName() + "§f te ha invitado al chat " + chatManager.getCurrentChat().getDisplayName() + "§f. Usa el comando §a/c " + code + "§f para unirte.");

                    return true;
                }

                if (args[0].equals("toggle")) {
                    if (chatManager.toggleHidden()) {
                        p.sendMessage(getPrefix() + "Has ocultado el chat.");
                    } else {
                        p.sendMessage(getPrefix() + "Ahora puedes ver el chat.");
                    }
                    return true;
                }

                if (args[0].equals("default")) {

                    if (args.length < 2) {

                        if (chatManager.getCurrentChatName().equals(chatManager.getDefaultChatName())) {
                            p.sendMessage(getPrefix() + "Ya estás en tu chat por defecto.");
                            return true;
                        }

                        Chat oldChat = chatManager.getCurrentChat();
                        oldChat.removePlayer(p.getUniqueId());

                        Chat defaultChat = chatManager.getDefaultChat();
                        chatManager.setCurrentChat(defaultChat);
                        defaultChat.addPlayer(p.getUniqueId());
                        p.sendMessage(getPrefix() + "Te has unido a tu chat predeterminado: §a" + defaultChat.getDisplayName() + "§f.");
                    } else if (args[1].equals("set")) {

                        this.openConfigDefaultChatMenu(p);

                    } else {
                        p.sendMessage(getPrefix() + "Introduce un subcomando válido.");
                    }
                    return true;
                }

                if (args[0].equals("set")) {
                    this.openConfigChatMenu(p);
                }

                if (args[0].matches("[a-z]{6}")) {
                    if (!invites.containsKey(args[0])) {
                        p.sendMessage(getPrefix() + "La invitación introducida no existe.");
                        return true;
                    }

                    String targetChatName = invites.get(args[0]);

                    if (!handler.isLoaded(targetChatName)) {
                        if (targetChatName.startsWith("project_")) {
                            String projectID = targetChatName.replace("project_", "");
                            if (plugin.getProjectRegistry().exists(projectID)) {
                                Project project = plugin.getProjectRegistry().get(projectID);
                                handler.registerChat(new ProjectChat(project, handler));
                            } else {
                                p.sendMessage(getPrefix() + "El chat de la invitación ya no está disponible.");
                                return true;
                            }
                        } else {
                            p.sendMessage(getPrefix() + "El chat de la invitación ya no está disponible.");
                            return true;
                        }
                    }

                    Chat oldChat = chatManager.getCurrentChat();
                    oldChat.removePlayer(p.getUniqueId());

                    Chat targetChat = handler.getChat(targetChatName);
                    chatManager.setCurrentChat(targetChat);
                    targetChat.addPlayer(p.getUniqueId());

                    p.sendMessage(getPrefix() + "Te has unido al chat §a" + targetChat.getDisplayName() + "§f.");
                    return true;
                }

                Player target = plugin.getOnlinePlayer(args[0]);

                if (target != null) {
                    ChatManager targetChatManager = plugin.getPlayerRegistry().get(target.getUniqueId()).getChatManager();
                    try {
                        Chat chat = targetChatManager.getCurrentChat();
                        p.sendMessage(getPrefix() + "El chat actual de §a" + target.getName() + "§f es §a" + chat.getDisplayName() + "§f.");
                    } catch (SQLException e) {
                        p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                        return true;
                    }
                }
            } catch (SQLException e) {
                p.sendMessage(getPrefix() + "Ha ocurrido un error en la base de datos.");
                return true;
            }
        }
        return true;
    }

    public void openConfigChatMenu(@NotNull Player p) throws SQLException {

        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        ChatManager manager = s.getChatManager();

        CustomSlotsPaginatedGUI gui = new CustomSlotsPaginatedGUI(
                "Elige un chat",
                6,
                new Integer[]{
                        18, 19, 20, 21, 22, 23, 24, 25, 26,
                        27, 28, 29, 30, 31, 32, 33, 34, 35,
                        36, 37, 38, 39, 40, 41, 42, 43, 44,
                        45, 46, 47, 48, 49, 50, 51, 52, 53
                },
                9, 17
        );

        Chat globalChat = plugin.getChatHandler().getChat("global");

        gui.addPaginated(
                globalChat.getHead(),
                event -> {
                    try {
                        if (manager.getCurrentChatName().equals("global")) {
                            event.closeGUI();
                            return;
                        }

                        Chat oldChat = manager.getCurrentChat();
                        oldChat.removePlayer(p.getUniqueId());

                        manager.setCurrentChat(globalChat);
                        globalChat.addPlayer(p.getUniqueId());
                        event.closeGUI();
                        p.sendMessage(plugin.getChatHandler().getPrefix() + "Te has unido al chat §aGlobal§f.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        p.sendMessage(plugin.getChatHandler().getPrefix() + "Ha ocurrido un error en la base de datos.");
                        event.closeGUI();
                    }
                },
                null, null, null
        );

        for (Country country : plugin.getCountryManager().getAllCountries()) {

            Chat countryChat = plugin.getChatHandler().getChat(country.getName());

            gui.addPaginated(
                    countryChat.getHead(),
                    event -> {
                        try {
                            if (manager.getCurrentChatName().equals(country.getName())) {
                                event.closeGUI();
                                return;
                            }

                            Chat oldChat = manager.getCurrentChat();
                            oldChat.removePlayer(p.getUniqueId());

                            manager.setCurrentChat(countryChat);
                            countryChat.addPlayer(p.getUniqueId());
                            event.closeGUI();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Te has unido al chat de §a" + country.getDisplayName() + "§f.");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Ha ocurrido un error en la base de datos.");
                            event.closeGUI();
                        }
                    },
                    null, null, null
            );
        }

        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "id", "name", "tag"
                ),
                new SQLORConditionSet(
                        new SQLOperatorCondition(
                                "owner", "=", p.getUniqueId()
                        ),
                        new SQLJSONArrayCondition(
                                "members", p.getUniqueId()
                        )
                )
        ).retrieve();

        while (set.next()) {

            String tagString = set.getString("tag");
            ProjectTag tag = (tagString != null ? ProjectTag.valueOf(tagString) : null);

            String name = set.getString("name");

            String id = set.getString("id");

            List<String> lore = new ArrayList<>();
            if (plugin.getChatHandler().isLoaded("project_" + id)) {
                Chat chat = plugin.getChatHandler().getChat("project_" + id);
                lore.add("Jugadores: §7" + chat.getPlayers().size());
            } else {
                lore.add("Jugadores: §70");
            }

            gui.addPaginated(
                    (tag != null ?
                            ItemBuilder.head(
                                    tag.getHeadValue(),
                                    "§aProyecto " + (name != null ? name : set.getString("id".toUpperCase())),
                                    lore
                            ) :
                            ItemBuilder.of(Material.MAP)
                                    .name("§aProyecto " + (name != null ? name : set.getString("id".toUpperCase())))
                                    .lore(lore)
                                    .build()
                    ),
                    event -> {
                        try {
                            if (manager.getCurrentChatName().equals("project_" + id)) {
                                event.closeGUI();
                                return;
                            }

                            Project project = plugin.getProjectRegistry().get(id);

                            if (!plugin.getChatHandler().isLoaded("project_" + id)) {
                                plugin.getChatHandler().registerChat(
                                        new ProjectChat(project, plugin.getChatHandler())
                                );
                            }

                            Chat projectChat = plugin.getChatHandler().getChat("project_" + id);

                            Chat oldChat = manager.getCurrentChat();
                            oldChat.removePlayer(p.getUniqueId());

                            manager.setCurrentChat(projectChat);
                            projectChat.addPlayer(p.getUniqueId());
                            event.closeGUI();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Te has unido al chat del proyecto §a" + project.getDisplayName() + "§f.");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Ha ocurrido un error en la base de datos.");
                            event.closeGUI();
                        }
                    },
                    null, null, null
            );

        }

        gui.openTo(p, plugin);

    }

    public void openConfigDefaultChatMenu(@NotNull Player p) throws SQLException {

        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        ChatManager manager = s.getChatManager();

        CustomSlotsPaginatedGUI gui = new CustomSlotsPaginatedGUI(
                "Elige un chat predeterminado",
                6,
                new Integer[]{
                        18, 19, 20, 21, 22, 23, 24, 25, 26,
                        27, 28, 29, 30, 31, 32, 33, 34, 35,
                        36, 37, 38, 39, 40, 41, 42, 43, 44,
                        45, 46, 47, 48, 49, 50, 51, 52, 53
                },
                9, 17
        );

        Chat globalChat = plugin.getChatHandler().getChat("global");

        gui.addPaginated(
                globalChat.getHead(),
                event -> {
                    try {
                        if (manager.getDefaultChatName().equals("global")) {
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Tu chat predeterminado ya es el chat §aGlobal§f.");
                            event.closeGUI();
                            return;
                        }

                        manager.setDefaultChat(globalChat);
                        event.closeGUI();
                        p.sendMessage(plugin.getChatHandler().getPrefix() + "Chat predeterminado cambiado al chat §aGlobal§f.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        p.sendMessage(plugin.getChatHandler().getPrefix() + "Ha ocurrido un error en la base de datos.");
                        event.closeGUI();
                    }
                },
                null, null, null
        );

        for (Country country : plugin.getCountryManager().getAllCountries()) {

            Chat countryChat = plugin.getChatHandler().getChat(country.getName());

            gui.addPaginated(
                    countryChat.getHead(),
                    event -> {
                        try {
                            if (manager.getDefaultChatName().equals(country.getName())) {
                                p.sendMessage(plugin.getChatHandler().getPrefix() + "Tu chat predeterminado ya es el chat de §a" + country.getDisplayName() + "§f.");
                                event.closeGUI();
                                return;
                            }

                            manager.setDefaultChat(countryChat);
                            event.closeGUI();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Chat predeterminado cambiado al chat de §a" + country.getDisplayName() + "§f.");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Ha ocurrido un error en la base de datos.");
                            event.closeGUI();
                        }
                    },
                    null, null, null
            );
        }

        ResultSet set = plugin.getSqlManager().select(
                "projects",
                new SQLColumnSet(
                        "id", "name", "tag"
                ),
                new SQLORConditionSet(
                        new SQLOperatorCondition(
                                "owner", "=", p.getUniqueId()
                        ),
                        new SQLJSONArrayCondition(
                                "members", p.getUniqueId()
                        )
                )
        ).retrieve();

        while (set.next()) {

            String tagString = set.getString("tag");
            ProjectTag tag = (tagString != null ? ProjectTag.valueOf(tagString) : null);

            String name = set.getString("name");

            String id = set.getString("id");

            List<String> lore = new ArrayList<>();
            if (plugin.getChatHandler().isLoaded("project_" + id)) {
                Chat chat = plugin.getChatHandler().getChat("project_" + id);
                lore.add("Jugadores: §7" + chat.getPlayers().size());
            } else {
                lore.add("Jugadores: §70");
            }

            gui.addPaginated(
                    (tag != null ?
                            ItemBuilder.head(
                                    tag.getHeadValue(),
                                    "§aProyecto " + (name != null ? name : set.getString("id".toUpperCase())),
                                    lore
                            ) :
                            ItemBuilder.of(Material.MAP)
                                    .name("§aProyecto " + (name != null ? name : set.getString("id".toUpperCase())))
                                    .lore(lore)
                                    .build()
                    ),
                    event -> {
                        try {
                            Project project = plugin.getProjectRegistry().get(id);

                            if (manager.getDefaultChatName().equals("project_" + id)) {
                                p.sendMessage(plugin.getChatHandler().getPrefix() + "Tu chat predeterminado ya es el chat del proyecto §a" + project.getDisplayName() + "§f.");
                                event.closeGUI();
                                return;
                            }


                            if (!plugin.getChatHandler().isLoaded("project_" + id)) {
                                plugin.getChatHandler().registerChat(
                                        new ProjectChat(project, plugin.getChatHandler())
                                );
                            }

                            Chat projectChat = plugin.getChatHandler().getChat("project_" + id);

                            manager.setDefaultChat(projectChat);
                            event.closeGUI();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Chat predeterminado cambiado al chat del proyecto §a" + project.getDisplayName() + "§f.");
                        } catch (SQLException e) {
                            e.printStackTrace();
                            p.sendMessage(plugin.getChatHandler().getPrefix() + "Ha ocurrido un error en la base de datos.");
                            event.closeGUI();
                        }
                    },
                    null, null, null
            );

        }

        gui.openTo(p, plugin);

    }

    @Override
    public String getPrefix() {
        return "§f[§aCHAT§f] §7>> §f";
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(
                    Arrays.asList(
                            "invite", "default", "set", "toggle"
                    )
            );
        } else if (args.length == 2 && args[0].equals("invite")) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                completions.add(player.getName());
            }
        } else if (args.length == 2 && args[0].equals("default")) {
            completions.add("set");
        }

        List<String> finalCompletions = new ArrayList<>();
        for (String completion : completions) {
            if (completion.startsWith(args[args.length - 1])) {
                finalCompletions.add(completion);
            }
        }
        Collections.sort(finalCompletions);
        return finalCompletions;
    }
}
