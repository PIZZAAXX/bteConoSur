package pizzaaxx.bteconosur.WorldEdit.Assets.Commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.regions.RegionSelector;
import com.sk89q.worldedit.regions.selector.CuboidRegionSelector;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Inventory.InventoryAction;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Inventory.PaginatedInventoryGUI;
import pizzaaxx.bteconosur.Player.Managers.WorldEditManager;
import pizzaaxx.bteconosur.Player.ServerPlayer;
import pizzaaxx.bteconosur.WorldEdit.Assets.Asset;
import pizzaaxx.bteconosur.WorldEdit.Assets.AssetGroup;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class AssetsCommand implements CommandExecutor, TabCompleter {

    private final BTEConoSur plugin;
    private final String prefix;
    private final Map<UUID, Vector> origins = new HashMap<>();

    public AssetsCommand(@NotNull BTEConoSur plugin) {
        this.plugin = plugin;
        this.prefix = plugin.getWorldEdit().getPrefix();
    }

    private final Set<UUID> onCooldown = new HashSet<>();

    private void startCooldown(@NotNull Player player) {

        ServerPlayer s = plugin.getPlayerRegistry().get(player.getUniqueId());
        if (s.getSecondaryRoles().contains(ServerPlayer.SecondaryRoles.ADMIN)) {
            return;
        }

        onCooldown.add(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                onCooldown.remove(player.getUniqueId());
            }
        }.runTaskLaterAsynchronously(plugin, 200);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;
        ServerPlayer s = plugin.getPlayerRegistry().get(p.getUniqueId());

        if (args.length < 1) {
            p.sendMessage(prefix + "Introduce un subcomando.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setorigin": {

                if (!s.isBuilder()) {
                    p.sendMessage(prefix + "Debes haber terminado un proyecto para poder crear §oassets§r.");
                    return true;
                }

                RegionSelector selector = plugin.getWorldEdit().getLocalSession(p).getRegionSelector(plugin.getWorldEditWorld());

                if (!(selector instanceof CuboidRegionSelector)) {
                    p.sendMessage(prefix + "Selecciona la posición 1 (click izquierdo) de un área cúbica.");
                    return true;
                }

                CuboidRegionSelector cuboidSelector = (CuboidRegionSelector) selector;

                Vector pos1 = cuboidSelector.getIncompleteRegion().getPos1();

                if (pos1 == null) {
                    p.sendMessage(prefix + "Selecciona la posición 1 (click izquierdo) de un área cúbica.");
                    return true;
                }

                origins.put(p.getUniqueId(), new Vector(pos1));
                p.sendMessage(prefix + "Punto de origen establecido en las coordenadas §a" + pos1.getBlockX() + " " + pos1.getBlockY() + " " + pos1.getBlockZ() + "§f.");

                break;
            }
            case "create": {

                if (!s.isBuilder()) {
                    p.sendMessage(prefix + "Deber haber terminado un proyecto para poder crear §oassets§r.");
                    return true;
                }

                if (!origins.containsKey(p.getUniqueId())) {
                    p.sendMessage(prefix + "Debes seleccionar un punto de origen para el §oasset§r. Usa §a/asset setOrigin§f.");
                    return true;
                }

                if (onCooldown.contains(p.getUniqueId())) {
                    p.sendMessage(prefix + "Solo puedes crear §oassets§f cada 5 minutos.");
                    return true;
                }

                if (args.length < 2) {
                    p.sendMessage(prefix + "Introduce un nombre para el §oasset§r.");
                    return true;
                }

                String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

                if (!name.matches("[a-zA-Z0-9_ñÑ\\s]{1,32}")) {
                    p.sendMessage(prefix + "Introduce un nombre válido.");
                    return true;
                }

                if (plugin.getAssetsRegistry().getNames().contains(name)) {
                    p.sendMessage(prefix + "Ya existe un §oasset§r con ese nombre.");
                    return true;
                }

                try {
                    Region region = plugin.getWorldEdit().getSelection(p);
                    if (!(region instanceof CuboidRegion)) {
                        p.sendMessage(prefix + "Selecciona una región cúbica.");
                        return true;
                    }
                    CuboidRegion cuboidRegion = (CuboidRegion) region;

                    if (!cuboidRegion.contains(origins.get(p.getUniqueId()))) {
                        p.sendMessage(prefix + "El punto de origen seleccionado no está dentro del área seleccionada.");
                        return true;
                    }

                    Vector max = cuboidRegion.getMaximumPoint();
                    Vector min = cuboidRegion.getMinimumPoint();

                    int xDif = max.getBlockX() - min.getBlockX();
                    int yDif = max.getBlockY() - min.getBlockY();
                    int zDif = max.getBlockZ() - min.getBlockZ();
                    if (xDif > 20 || yDif > 80 || zDif > 20) {
                        p.sendMessage(prefix + "Los §oassets§r tienen un tamaño máximo de §a20x20x40§f bloques. Actual: §a" + xDif + "x" + zDif + "x" + yDif + "§f.");
                        return true;
                    }

                    BlockArrayClipboard clipboard = new BlockArrayClipboard(cuboidRegion);

                    ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(
                            plugin.getWorldEditWorld(), cuboidRegion, clipboard, region.getMinimumPoint()
                    );
                    Operations.complete(forwardExtentCopy);

                    try {
                        String id = plugin.getAssetsRegistry().create(name, clipboard, origins.get(p.getUniqueId()), p.getUniqueId());
                        this.origins.remove(p.getUniqueId());
                        this.startCooldown(p);
                        p.sendMessage(prefix + "§oAsset§r §a" + name + "§f creado correctamente con la ID §a" + id + "§f.");
                    } catch (SQLException e) {
                        p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                    } catch (IOException e) {
                        p.sendMessage(prefix + "Ha ocurrido un error al escribir el archivo.");
                    }
                } catch (IncompleteRegionException e) {
                    p.sendMessage(prefix + "Selecciona una región cúbica.");
                } catch (WorldEditException e) {
                    p.sendMessage(prefix + "Algo ha salido mal.");
                }

                break;
            }
            case "setname": {

                if (!s.isBuilder()) {
                    p.sendMessage(prefix + "Deber haber terminado un proyecto para poder crear §oassets§r.");
                    return true;
                }

                if (args.length < 2) {
                    p.sendMessage(prefix + "Introduce una ID.");
                    return true;
                }

                String id = args[1];

                if (args.length < 3) {
                    p.sendMessage(prefix + "Introduce un nuevo nombre.");
                    return true;
                }

                String name = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                if (!name.matches("[a-zA-Z0-9_ñÑ\\s]{1,32}")) {
                    p.sendMessage(prefix + "Introduce un nuevo nombre válido.");
                    return true;
                }

                if (plugin.getAssetsRegistry().getNames().contains(name)) {
                    p.sendMessage(prefix + "Ya existe un §oasset§r con ese nombre.");
                    return true;
                }

                if (!plugin.getAssetsRegistry().exists(id)) {
                    p.sendMessage(prefix + "No existe un §oasset§r con esa ID.");
                    return true;
                }

                Asset asset = plugin.getAssetsRegistry().get(id);
                String oldName = asset.getName();

                if (!asset.getCreator().equals(p.getUniqueId())) {
                    p.sendMessage(prefix + "No eres el creador de este §oasset§r por lo que no puedes editarlo.");
                    return true;
                }

                try {
                    asset.setName(name);
                    p.sendMessage(prefix + "Nombre del §oasset§r cambiado de §a" + oldName + "§f a §a" + name + "§f.");
                } catch (SQLException e) {
                    p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                }

                break;
            }
            case "setautorotate": {

                if (!s.isBuilder()) {
                    p.sendMessage(prefix + "Deber haber terminado un proyecto para poder crear §oassets§r.");
                    return true;
                }

                if (args.length < 2) {
                    p.sendMessage(prefix + "Introduce una ID.");
                    return true;
                }

                String id = args[1];

                if (args.length < 3) {
                    p.sendMessage(prefix + "Introduce §atrue§f o §cfalse§f.");
                    return true;
                }

                if (!args[2].equals("true") && !args[2].equals("false")) {
                    p.sendMessage(prefix + "Introduce §atrue§f o §cfalse§f.");
                    return true;
                }

                boolean autoRotate = args[2].equals("true");

                if (!plugin.getAssetsRegistry().exists(id)) {
                    p.sendMessage(prefix + "No existe un §oasset§r con esa ID.");
                    return true;
                }

                Asset asset = plugin.getAssetsRegistry().get(id);

                if (!asset.getCreator().equals(p.getUniqueId())) {
                    p.sendMessage(prefix + "No eres el creador de este §oasset§r por lo que no puedes editarlo.");
                    return true;
                }

                try {
                    asset.setAutoRotate(autoRotate);
                    p.sendMessage(prefix + "Rotación automática del §oasset§r establecida en " + (autoRotate ? "§averdadero" : "§cfalso") + "§f.");
                } catch (SQLException | JsonProcessingException e) {
                    p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                }

                break;
            }
            case "settags": {
                if (!s.isBuilder()) {
                    p.sendMessage(prefix + "Deber haber terminado un proyecto para poder crear §oassets§r.");
                    return true;
                }

                if (args.length < 2) {
                    p.sendMessage(prefix + "Introduce una ID.");
                    return true;
                }

                String id = args[1];

                Set<String> tags = new HashSet<>(
                        Arrays.asList(
                                Arrays.copyOfRange(args, 2, args.length)
                        )
                );

                if (!plugin.getAssetsRegistry().exists(id)) {
                    p.sendMessage(prefix + "No existe un §oasset§r con esa ID.");
                    return true;
                }

                Asset asset = plugin.getAssetsRegistry().get(id);

                if (!asset.getCreator().equals(p.getUniqueId())) {
                    p.sendMessage(prefix + "No eres el creador de este §oasset§r por lo que no puedes editarlo.");
                    return true;
                }

                try {
                    asset.setTags(tags);
                    if (tags.isEmpty()) {
                        p.sendMessage(prefix + "Has eliminado las etiquetas del §oasset§r han sido eliminadas.");
                    } else {
                        p.sendMessage(prefix + "Has establecido las etiquetas del §oasset§r en §a#" + String.join("§f, §a#", tags) + "§f.");
                    }
                } catch (SQLException e) {
                    p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                }

                break;
            }
            case "delete": {

                if (!s.isBuilder()) {
                    p.sendMessage(prefix + "Deber haber terminado un proyecto para poder crear §oassets§r.");
                    return true;
                }

                if (args.length < 2) {
                    p.sendMessage(prefix + "Introduce una ID.");
                    return true;
                }

                String id = args[1];

                if (!plugin.getAssetsRegistry().exists(id)) {
                    p.sendMessage(prefix + "No existe un §oasset§r con esa ID.");
                    return true;
                }

                Asset asset = plugin.getAssetsRegistry().get(id);

                if (!asset.getCreator().equals(p.getUniqueId())) {
                    p.sendMessage(prefix + "No eres el creador de este §oasset§r por lo que no puedes eliminarlo.");
                    return true;
                }

                try {
                    plugin.getAssetsRegistry().delete(id);
                    p.sendMessage(prefix + "Asset §a" + id + "§f eliminado correctamente.");
                } catch (SQLException | JsonProcessingException e) {
                    p.sendMessage(prefix + "Ha ocurrido un error en la base de datos.");
                }

                break;

            }
            case "search": {}
            case "fav": {
                List<String> ids;
                WorldEditManager manager = s.getWorldEditManager();
                if (args[0].equalsIgnoreCase("search")) {
                    String input;
                    if (args.length < 2) {
                        input = null;
                    } else {
                        input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    }
                    ids = plugin.getAssetsRegistry().getSearch(input);
                } else {
                    ids = new ArrayList<>(manager.getFavAssets());
                }
                if (ids.isEmpty()) {
                    p.sendMessage(prefix + "No tienes assets favoritos. Selecciona algunos en §a/asset search§f.");
                    return true;
                }

                PaginatedInventoryGUI gui = new PaginatedInventoryGUI(
                        6,
                        (args[0].equals("search") ? "Resultados de búsqueda" : "Assets favoritos")
                );
                gui.setDraggable(true);
                for (String id : ids) {
                    Asset asset = plugin.getAssetsRegistry().get(id);
                    ItemStack head = ItemBuilder.head(
                            this.getAssetHeadValue(asset.getId()),
                            (s.getWorldEditManager().isFavourite(asset.getId()) ? "§6" + asset.getName() + " ⭐" : "§a" + asset.getName()),
                            new ArrayList<>(
                                    Arrays.asList(
                                            "§fID: §7" + asset.getId(),
                                            "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                            "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                            (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                            "",
                                            "§7Haz click derecho para " + (manager.isFavourite(asset.getId()) ? "§celiminar§7 de" : "§aagregar§f a") + " favoritos.",
                                            (asset.isAutoRotate() ? "§7Haz click derecho más shift para agregar el §oasset§7 a un grupo." : "§8No puedes agregar §oassets§8 con rotación manual a grupos.")
                                    )
                            )
                    );
                    InventoryAction changeFavAction = event -> {
                                try {
                                    if (manager.isFavourite(asset.getId())) {
                                        manager.removeFavAsset(asset.getId());
                                        event.updateSlot("§a" + asset.getName());
                                        event.updateSlot(
                                                new ArrayList<>(
                                                        Arrays.asList(
                                                                "§fID: §7" + asset.getId(),
                                                                "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                "",
                                                                "§7Haz click derecho para §aagregar§7 a favoritos.",
                                                                (asset.isAutoRotate() ? "§7Haz click derecho más shift para agregar el §oasset§7 a un grupo." : "§8No puedes agregar §oassets§8 con rotación manual a grupos.")
                                                        )
                                                )
                                        );
                                    } else {
                                        manager.addFavAsset(asset.getId());
                                        event.updateSlot("§6" + asset.getName() + " ⭐");
                                        event.updateSlot(
                                                new ArrayList<>(
                                                        Arrays.asList(
                                                                "§fID: §7" + asset.getId(),
                                                                "§fCreador: §7" + plugin.getPlayerRegistry().get(asset.getCreator()).getName(),
                                                                "§fRotación: §7" + (asset.isAutoRotate() ? "Automática" : "Manual"),
                                                                (!asset.getTags().isEmpty() ? "§7#" + String.join(" #", asset.getTags()):""),
                                                                "",
                                                                "§7Haz click derecho para §celiminar§7 de favoritos.",
                                                                (asset.isAutoRotate() ? "§7Haz click derecho más shift para agregar el §oasset§7 a un grupo." : "§8No puedes agregar §oassets§8 con rotación manual a grupos.")
                                                        )
                                                )
                                        );
                                    }
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            };
                    InventoryAction addToGroupAction;
                    if (asset.isAutoRotate()) {
                        addToGroupAction = event -> {
                            PaginatedInventoryGUI groupsGUI = new PaginatedInventoryGUI(
                                    6,
                                    "Selecciona un grupo para agregar el §oasset"
                            );
                            for (String name : manager.getAssetGroups().keySet()) {
                                AssetGroup group = manager.getAssetGroup(name);
                                ItemStack groupHead = ItemBuilder.head(
                                        this.getGroupHeadValue(name),
                                        "§7Grupo §a" + name,
                                        Arrays.asList(
                                                (group.getIds().isEmpty() ? "§8Este grupo no tiene §oassets§8 aún." : "§a§oAssets§a: §7" + String.join(", ", group.getNames())),
                                                " ",
                                                (group.isPart(id) ? "§8El §oasset§8 ya es parte de este grupo" : "§a[+] §7Haz click para agregar el §oasset§7 §a" + asset.getName() + "§7 al grupo")
                                        )
                                );
                                InventoryAction action;
                                if (!group.isPart(id)) {
                                    action = groupClickEvent -> {
                                        try {
                                            manager.addAssetToGroup(name, id);
                                            p.sendMessage(prefix + "§oAsset§r §a" + asset.getName() + "§f agregado al grupo §a" + name + "§f.");
                                            gui.openTo(p, plugin);
                                        } catch (SQLException e) {
                                            throw new RuntimeException(e);
                                        }
                                    };
                                } else {
                                    action = null;
                                }
                                groupsGUI.add(
                                        groupHead,
                                        action,
                                        null,
                                        null,
                                        null
                                );
                            }
                            groupsGUI.openTo(p, plugin);
                        };
                    } else {
                        addToGroupAction = null;
                    }
                    gui.add(
                            head,
                            null,
                            null,
                            changeFavAction,
                            addToGroupAction
                    );
                }
                gui.addRemovedLoreLine(4);
                gui.addRemovedLoreLine(5);
                gui.addRemovedLoreLine(6);
                gui.openTo(p, plugin);
                break;
            }
        }
        return true;
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

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(
                    Arrays.asList(
                            "setorigin", "create", "setname", "setautorotate", "settags", "delete", "search", "fav"
                    )
            );
        } else if (args.length == 2 && args[0].equals("setautorotate")) {
            completions.addAll(
                    Arrays.asList("true", "false")
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
