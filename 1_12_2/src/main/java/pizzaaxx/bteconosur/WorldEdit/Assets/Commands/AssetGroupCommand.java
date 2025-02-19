package pizzaaxx.bteconosur.WorldEdit.Assets.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Inventory.InventoryAction;
import pizzaaxx.bteconosur.Inventory.InventoryGUIClickEvent;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Inventory.PaginatedInventoryGUI;
import pizzaaxx.bteconosur.Player.Managers.WorldEditManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.WorldEdit.Assets.Asset;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetGroup;

import java.sql.SQLException;
import java.util.*;

public class AssetGroupCommand implements CommandExecutor, TabCompleter {

    private final BTEConoSur plugin;
    private final String prefix;

    public AssetGroupCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getWorldEdit().getPrefix();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());
        WorldEditManager worldEditManager = s.getWorldEditManager();

        if (args.length < 1) {
            p.performCommand("assetgroup list");
        } else {
            switch (args[0]) {
                case "create": {

                    if (args.length < 2) {
                        p.sendMessage(prefix + "Introduce un nombre para el grupo.");
                        return true;
                    }

                    final String name = args[1];

                    if (!name.matches("[a-zA-Z0-9_]{1,32}")) {
                        p.sendMessage(prefix + "Introduce un nombre válido para el grupo.");
                        return true;
                    }

                    if (worldEditManager.existsAssetGroup(name)) {
                        p.sendMessage(prefix + "Ya existe un grupo con este nombre.");
                        return true;
                    }

                    try {
                        worldEditManager.createAssetGroup(name);
                        p.sendMessage(prefix + "Grupo §a" + name + "§f creado correctamente.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                    }
                    break;
                }
                case "delete": {

                    if (args.length < 2) {
                        p.sendMessage(prefix + "Introduce el nombre del grupo que quieres eliminar.");
                        return true;
                    }

                    final String name = args[1];

                    if (!name.matches("[a-zA-Z0-9_]{1,32}")) {
                        p.sendMessage(prefix + "Introduce un nombre de grupo válido.");
                        return true;
                    }

                    if (!worldEditManager.existsAssetGroup(name)) {
                        p.sendMessage(prefix + "No existe un grupo con este nombre.");
                        return true;
                    }

                    try {
                        worldEditManager.deleteAssetGroup(name);
                        p.sendMessage(prefix + "Grupo §a" + name + "§f eliminado correctamente.");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                    }
                    break;
                }
                case "list": {

                    Map<String, List<String>> groups = worldEditManager.getAssetGroups();
                    if (groups.isEmpty()) {
                        p.sendMessage(prefix + "No tienes grupos de §oassets§r creados aún.");
                        return true;
                    }

                    if (args.length == 1) {

                        PaginatedInventoryGUI gui = new PaginatedInventoryGUI(
                                6,
                                "Tus grupos de §oassets§r"
                        );
                        gui.setDraggable(true);
                        for (String name : groups.keySet()) {
                            AssetGroup group = worldEditManager.getAssetGroup(name);
                            gui.add(
                                    ItemBuilder.head(
                                            this.getGroupHeadValue(name),
                                            "§7Grupo §a" + name,
                                            Arrays.asList(
                                                    (group.getIds().isEmpty() ? "§8Este grupo no tiene §oassets§8 aún." : "§a§oAssets§a: §7" + String.join(", ", group.getNames())),
                                                    " ",
                                                    "§7Haz click derecho para ver los §oassets§7 de este grupo"
                                            )
                                    ),
                                    null,
                                    null,
                                    event -> {

                                        PaginatedInventoryGUI groupGUI = new PaginatedInventoryGUI(
                                                6,
                                                "§oAssets§r del grupo " + name
                                        );
                                        groupGUI.setDraggable(true);
                                        for (String id : group.getIds()) {
                                            Asset asset = plugin.getAssetsRegistry().get(id);
                                            ItemStack head = ItemBuilder.head(
                                                    this.getAssetHeadValue(asset.getId()),
                                                    "§a" + asset.getName(),
                                                    new ArrayList<>(
                                                            Arrays.asList(
                                                                    "§fID: §7" + asset.getId(),
                                                                    "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                    "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                    (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                    "",
                                                                    "§c[-] §7Haz click derecho para eliminar este §oasset§7 del grupo"
                                                            )
                                                    )
                                            );
                                            InventoryAction action = new InventoryAction() {
                                                @Override
                                                public void exec(InventoryGUIClickEvent event) {
                                                    try {
                                                        if (group.isPart(id)) {
                                                            worldEditManager.removeAssetFromGroup(name, id);
                                                            event.updateSlot(
                                                                    new ArrayList<>(
                                                                            Arrays.asList(
                                                                                    "§fID: §7" + asset.getId(),
                                                                                    "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                                    "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                                    (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                                    "",
                                                                                    "§a[+] §7Haz click derecho para volver a agregar este §oasset§7 al grupo"
                                                                            )
                                                                    )
                                                            );
                                                        } else {
                                                            worldEditManager.addAssetToGroup(name, id);
                                                            event.updateSlot(
                                                                    new ArrayList<>(
                                                                            Arrays.asList(
                                                                                    "§fID: §7" + asset.getId(),
                                                                                    "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                                    "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                                    (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                                    "",
                                                                                    "§c[-] §7Haz click derecho para eliminar este §oasset§7 del grupo"
                                                                            )
                                                                    )
                                                            );
                                                        }
                                                    } catch (SQLException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                            };
                                            groupGUI.add(
                                                    head,
                                                    null,
                                                    null,
                                                    action,
                                                    null
                                            );
                                        }
                                        groupGUI.openTo(p, plugin);
                                    },
                                    null
                            );
                        }
                        gui.openTo(p, plugin);
                    } else {

                        String name = args[1];

                        if (!worldEditManager.existsAssetGroup(name)) {
                            p.sendMessage(prefix + "El grupo introducido no existe.");
                            return true;
                        }

                        AssetGroup group = worldEditManager.getAssetGroup(name);

                        PaginatedInventoryGUI groupGUI = new PaginatedInventoryGUI(
                                6,
                                "§oAssets§r del grupo " + name
                        );
                        groupGUI.setDraggable(true);
                        for (String id : group.getIds()) {
                            Asset asset = plugin.getAssetsRegistry().get(id);
                            ItemStack head = ItemBuilder.head(
                                    this.getAssetHeadValue(asset.getId()),
                                    "§a" + asset.getName(),
                                    new ArrayList<>(
                                            Arrays.asList(
                                                    "§fID: §7" + asset.getId(),
                                                    "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                    "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                    (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                    "",
                                                    "§c[-] §7Haz click derecho para eliminar este §oasset§7 del grupo"
                                            )
                                    )
                            );
                            InventoryAction action = new InventoryAction() {
                                @Override
                                public void exec(InventoryGUIClickEvent event) {
                                    try {
                                        if (group.isPart(id)) {
                                            worldEditManager.removeAssetFromGroup(name, id);
                                            event.updateSlot(
                                                    new ArrayList<>(
                                                            Arrays.asList(
                                                                    "§fID: §7" + asset.getId(),
                                                                    "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                    "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                    (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                    "",
                                                                    "§a[+] §7Haz click derecho para volver a agregar este §oasset§7 al grupo"
                                                            )
                                                    )
                                            );
                                        } else {
                                            worldEditManager.addAssetToGroup(name, id);
                                            event.updateSlot(
                                                    new ArrayList<>(
                                                            Arrays.asList(
                                                                    "§fID: §7" + asset.getId(),
                                                                    "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                    "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                    (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                    "",
                                                                    "§c[-] §7Haz click derecho para eliminar este §oasset§7 del grupo"
                                                            )
                                                    )
                                            );
                                        }
                                    } catch (SQLException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            };
                            groupGUI.add(
                                    head,
                                    null,
                                    null,
                                    action,
                                    null
                            );
                        }
                        groupGUI.openTo(p, plugin);
                    }
                    break;
                }
                default: {
                    p.performCommand("assetgroup list " + args[0]);
                }
            }
        }

        return true;
    }

    private final List<String> groupHeadOptions = new ArrayList<>(
            Arrays.asList(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODAzNzYyNTRkNTczNWFiMTU2N2IzZjg3YzdmYmRlNDFkZmM0MTYyMmI2NTEwYjY2N2Q4MmM5ZWZlOGE1Y2VkMSJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNGYwZWYxNjc0YjcxZjhkYWQ3ZjU4ODJmMTNjZjI0MTE2NTNmNmEyZjllOTBlNDViMTgxZjQ0YjllZWYyNzhmZiJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTNjMjQ3YjBkODJkZDg3MTQ4Yzk2NTZhNDJlMDI0MDcwYzQ1OTcwZTExNDlmZGM1NTNlZjYzNjBmMjc5OWM2YyJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNDhlZGYwYzMxODYyNDM2M2MzMzI2YTY3NzQ1MmY5ZTk0MGNiNjk3NjMwNWY3NTg3NWFlMzMwZDE1ZDE1N2FlNCJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODBjNDUyOGU2MjJiZDMxODcyMGQzOGUwZTQ1OTllNjliZjIzMzA4Zjg5NjkzOTIwZTBlNGVjYjU1ZDFjMGJhYyJ9fX0="
            )
    );

    private String getGroupHeadValue(@NotNull String name) {
        int sum = 0;
        for (char character : name.toCharArray()) {
            sum += character;
        }
        int option = sum % 5;
        return groupHeadOptions.get(option);
    }

    private final List<String> assetHeadOptions = new ArrayList<>(
            Arrays.asList(
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjZjY2Q0MzRlMWQxNTYzNTUzNjNmNDg2ODkyYTkzOTIxNTQ5ZGI1MGEzNDkzMTM1YjRkYjQ5MjM3ZWE0NDVlMiJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTViYWNkMWQ3ZTY3NDZjN2YyNjg4ZDE4NDE3OGM2MjVlNzFjOWFhMTczMWUwMmQ3ZGZlNzg3NDlhNzU0YWY0MSJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzExMDFjNDk4YWFhYzkzZTc2YjhmMTZkMjljYmZhNDczZWQyNGQ5YzZjNzU1NjNjMjdlZWRkNDljOTYzZTk4YiJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjIyMTAwNTFmZjRmZjVhNzM5ZGY5NDUxYzc5YWYwZmQwYTNiMDU1NmMxMzg5M2Y3Yzk2YWY1OTk3ZDAxNjY1MiJ9fX0=",
                    "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWQ3OGNjMzkxYWZmYjgwYjJiMzVlYjczNjRmZjc2MmQzODQyNGMwN2U3MjRiOTkzOTZkZWU5MjFmYmJjOWNmIn19fQ=="
            )
    );

    private String getAssetHeadValue(@NotNull String id) {
        int sum = 0;
        for (char character : id.toCharArray()) {
            sum += character;
        }
        int option = sum % 5;
        return assetHeadOptions.get(option);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(
                    Arrays.asList(
                            "create", "delete", "list"
                    )
            );
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
