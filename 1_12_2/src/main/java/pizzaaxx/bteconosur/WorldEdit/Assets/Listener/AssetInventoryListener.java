package pizzaaxx.bteconosur.WorldEdit.Assets.Listener;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

import java.util.ArrayList;
import java.util.List;

public class AssetInventoryListener implements Listener {

    private final BTEConoSur plugin;

    public AssetInventoryListener(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    private final String[] possibleStarts = {
            "Tus grupos de assets",
            "Assets del grupo",
            "Resultados de búsqueda",
            "Assets favoritos"
    };

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        String name = ChatColor.stripColor(inventory.getName());
        for (String option : possibleStarts) {
            if (name.startsWith(option)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        check((Player) event.getWhoClicked());
                    }
                }.runTaskLaterAsynchronously(plugin, 1);
            }
        }
    }

    public void check(Player player) {
        for (int i = 0; i < 45; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType() != Material.SKULL_ITEM || !stack.hasItemMeta()) {
                return;
            }
            ItemMeta meta = stack.getItemMeta();
            if (!meta.hasDisplayName() || !meta.hasLore()) {
                return;
            }

            if (meta.getLore().get(0).startsWith("§8Este grupo no tiene §oassets§8 aún.") || meta.getLore().get(0).startsWith("§a§oAssets§a: §7")) {
                if (meta.getDisplayName().startsWith("§7Grupo §a")) {
                    List<String> lore = new ArrayList<>(meta.getLore());
                    while (lore.size() > 1) {
                        lore.remove(1);
                    }
                    meta.setLore(lore);
                    stack.setItemMeta(meta);
                    player.getInventory().setItem(i, stack);
                }
            } else if (stack.getItemMeta().getLore().get(0).startsWith("§fID: §7") && stack.getItemMeta().getLore().get(1).startsWith("§fCreador: §7")) {
                List<String> lore = new ArrayList<>(meta.getLore());
                while (lore.size() > 4) {
                    lore.remove(4);
                }
                meta.setLore(lore);
                stack.setItemMeta(meta);
                player.getInventory().setItem(i, stack);
            }
        }
    }
}
