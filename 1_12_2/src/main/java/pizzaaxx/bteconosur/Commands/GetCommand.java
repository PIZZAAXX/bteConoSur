package pizzaaxx.bteconosur.Commands;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Redstone;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Inventory.InventoryGUI;
import pizzaaxx.bteconosur.Inventory.ItemBuilder;
import pizzaaxx.bteconosur.Player.ServerPlayer;

import java.util.ArrayList;

public class GetCommand implements CommandExecutor, Listener {

    private final BTEConoSur plugin;

    public GetCommand(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        Player p = (Player) sender;

        InventoryGUI gui = new InventoryGUI(
                5,
                "Bloques especiales"
        );

        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGRkMGNkMTU4YzJiYjY2MTg2NTBlMzk1NGIyZDI5MjM3ZjViNGMwZGRjN2QyNThlMTczODBhYjY5NzlmMDcxIn19fQ=="
        , "§a[43:8] §fPiedra Lisa", new ArrayList<>()), 10);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzNmZTI0YTkxZTE0OTQ0ODYxZmYzN2M3YTYwZTE4YzVmYzU0YTQxYTg1MzFmOWJhNjEyNGQxNTI5MjI0OTgyNCJ9fX0="
        , "§a[43:9] §fArenisca Lisa", new ArrayList<>()), 11);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDIzYjczMDcxNDBmOWZkNzU3YmNlY2JlYjg1NzM0ZjFlYWZkMDU3NGFlZWQ5ZTkzNWJiMWRiMzQ5NDlkMzM4OSJ9fX0="
        , "§a[181:9] §fArenisca Roja Lisa", new ArrayList<>()), 12);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmQyZTMxMDg3OWE2NDUwYWY1NjI1YmNkNDUwOTNkZDdlNWQ4ZjgyN2NjYmZlYWM2OWM4MTUzNzc2ODQwNmIifX19"
        , "§a[17:12] §fTronco de Roble Entero", new ArrayList<>()), 13);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDI2ZWVmZDg3MjYzY2M0ODVjMTdjYjVmODE4ZWUzYmFkOTNjYTc2OTEzODVjYjVlNWQ2OThhZmY3MzNhMyJ9fX0="
        , "§a[17:13] §fTronco de Abeto Entero", new ArrayList<>()), 14);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTk5MWYzYjczZWJiOWRlYzkxZWRkYzgzNjFjYTJmZWNmNTI4MGQyYzczM2VkYTllY2I2OTVmODNkMTU4MCJ9fX0="
        , "§a[17:14] §fTronco de Abedul Entero", new ArrayList<>()), 15);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzljYTM1NDA2MjFjMWM3OWMzMmJmNDI0Mzg3MDhmZjFmNWY3ZDBhZjliMTRhMDc0NzMxMTA3ZWRmZWI2OTFjIn19fQ=="
        , "§a[17:15] §fTronco de Jungla Entero", new ArrayList<>()), 16);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTY2NjlmZTJkYmY3ODc5MmEzZTE5MTYyMmE4ZWQxZjllYjgwM2Y4ODI2YzliOTQ5ZDBkYzE1YTUxYzU5MzkxIn19fQ=="
        , "§a[162:12] §fTronco de Acacia Entero", new ArrayList<>()), 19);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjc3ZGM2ZThiNDVhZGNmOTI4ZjJjNjEzOWRmYTJhMzYxYWExMDdiODljOWFkMzVjMzQ3YjY3N2EwN2M3OWY1In19fQ=="
        , "§a[162:13] §fTronco de Roble Oscuro Entero", new ArrayList<>()), 20);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGUzNWYzODkzNGMzNzFmNWQxNWUzNGZhNDQ1ZDY5OGZkMTQxZWQ3ZTc1MjkzMTk5MzkwYjBiZDE2NzhlZiJ9fX0="
        , "§a[100:14] §fChampiñón Rojo", new ArrayList<>()), 21);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZmE0OWVjYTAzNjlkMWUxNThlNTM5ZDc4MTQ5YWNiMTU3Mjk0OWI4OGJhOTIxZDllZTY5NGZlYTRjNzI2YjMifX19"
        , "§a[99:14] §fChampiñón Café", new ArrayList<>()), 22);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2ZhMzljY2Y0Nzg4ZDkxNzlhODc5NWU2YjcyMzgyZDQ5Mjk3YjM5MjE3MTQ2ZWRhNjhhZTc4Mzg0MzU1YjEzIn19fQ=="
        , "§a[99:0 / 100:0] §fChampiñón Interior", new ArrayList<>()), 23);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZjU1ZmE2NDJkNWViY2JhMmM1MjQ2ZmU2NDk5YjFjNGY2ODAzYzEwZjE0ZjUyOTljOGU1OTgxOWQ1ZGMifX19"
        , "§a[99:15 / 100:15] §fTallo de Champiñón Entero", new ArrayList<>()), 24);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3ZDJlODRhZTEwYzk3MWQ5MmNiMDViNmFiNDExNmY3NjUwM2E4N2QzMTc0MjQ5Y2QxZjQ5OTJiYTE4MWRiNCJ9fX0="
        , "§a[167:5] §fTrampilla de Hierro Norte", new ArrayList<>()), 25);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3ZDJlODRhZTEwYzk3MWQ5MmNiMDViNmFiNDExNmY3NjUwM2E4N2QzMTc0MjQ5Y2QxZjQ5OTJiYTE4MWRiNCJ9fX0="
        , "§a[167:4] §fTrampilla de Hierro Sur", new ArrayList<>()), 29);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3ZDJlODRhZTEwYzk3MWQ5MmNiMDViNmFiNDExNmY3NjUwM2E4N2QzMTc0MjQ5Y2QxZjQ5OTJiYTE4MWRiNCJ9fX0="
        , "§a[167:6] §fTrampilla de Hierro Este", new ArrayList<>()), 30);
        gui.setItem(ItemBuilder.head(
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmI3ZDJlODRhZTEwYzk3MWQ5MmNiMDViNmFiNDExNmY3NjUwM2E4N2QzMTc0MjQ5Y2QxZjQ5OTJiYTE4MWRiNCJ9fX0="
        , "§a[167:7] §fTrampilla de Hierro Oeste", new ArrayList<>()), 31);
        gui.setItem(new ItemBuilder(Material.BARRIER).name( "§a[166] §fBarrera").build(), 32);
        gui.setItem(new ItemBuilder(Material.STRUCTURE_VOID).name("§a[217] §fVacío").build(), 33);

        gui.setDraggable(10,11,12,13,14,15,16,19,20,21,22,23,24,25,29,30,31,32,33);

        plugin.getInventoryHandler().open(p, gui);

        return true;
    }

    @EventHandler
    public void onClick(@NotNull PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.SKULL_ITEM && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasDisplayName()) {
                    Material material = null;
                    byte metadata = 0;
                    switch (meta.getDisplayName()) {
                        case "§a[43:9] §fArenisca Lisa":
                            material = Material.DOUBLE_STEP;
                            metadata = 9;
                            break;
                        case "§a[181:9] §fArenisca Roja Lisa":
                            material = Material.DOUBLE_STONE_SLAB2;
                            metadata = 9;
                            break;
                        case "§a[17:12] §fTronco de Roble Entero":
                            material = Material.LOG;
                            metadata = 12;
                            break;
                        case "§a[17:13] §fTronco de Abeto Entero":
                            material = Material.LOG;
                            metadata = 13;
                            break;
                        case "§a[17:14] §fTronco de Abedul Entero":
                            material = Material.LOG;
                            metadata = 14;
                            break;
                        case "§a[17:15] §fTronco de Jungla Entero":
                            material = Material.LOG;
                            metadata = 15;
                            break;
                        case "§a[162:12] §fTronco de Acacia Entero":
                            material = Material.LOG_2;
                            metadata = 12;
                            break;
                        case "§a[162:13] §fTronco de Roble Oscuro Entero":
                            material = Material.LOG_2;
                            metadata = 13;
                            break;
                        case "§a[100:14] §fChampiñón Rojo":
                            material = Material.HUGE_MUSHROOM_2;
                            metadata = 14;
                            break;
                        case "§a[99:14] §fChampiñón Café":
                            material = Material.HUGE_MUSHROOM_1;
                            metadata = 14;
                            break;
                        case "§a[99:0 / 100:0] §fChampiñón Interior":
                            material = Material.HUGE_MUSHROOM_2;
                            metadata = 0;
                            break;
                        case "§a[99:15 / 100:15] §fTallo de Champiñón Entero":
                            material = Material.HUGE_MUSHROOM_2;
                            metadata = 15;
                            break;
                        case "§a[167:5] §fTrampilla de Hierro Norte":
                            material = Material.IRON_TRAPDOOR;
                            metadata = 5;
                            break;
                        case "§a[167:4] §fTrampilla de Hierro Sur":
                            material = Material.IRON_TRAPDOOR;
                            metadata = 4;
                            break;
                        case "§a[167:6] §fTrampilla de Hierro Este":
                            material = Material.IRON_TRAPDOOR;
                            metadata = 6;
                            break;
                        case "§a[167:7] §fTrampilla de Hierro Oeste":
                            material = Material.IRON_TRAPDOOR;
                            metadata = 7;
                            break;
                        case "§a[43:8] §fPiedra Lisa":
                            material = Material.DOUBLE_STEP;
                            metadata = 8;
                            break;
                    }

                    if (material != null) {
                        event.setCancelled(true);

                        ServerPlayer s = plugin.getPlayerRegistry().get(event.getPlayer().getUniqueId());

                        Block targetBlock = event.getClickedBlock().getRelative(event.getBlockFace());
                        if (s.canBuild(targetBlock.getLocation())) {
                            targetBlock.setType(material);
                            targetBlock.setData(metadata);
                        }
                    }

                }
            }
        }
    }
}
