package pizzaaxx.bteconosur.Inventory;

import com.sk89q.worldguard.bukkit.listener.debounce.legacy.InventoryMoveItemEventDebounce;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

import java.util.*;

public class InventoryHandler implements Listener {

    private final BTEConoSur plugin;

    private final Map<UUID, InventoryGUI> openedInventories = new HashMap<>();

    public InventoryHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void open(@NotNull Player p, @NotNull InventoryGUI inventoryGUI) {

        p.openInventory(inventoryGUI.buildInventory());
        openedInventories.put(p.getUniqueId(), inventoryGUI);

    }

    @EventHandler
    public void onInventoryClick(@NotNull InventoryClickEvent event) {
        if (openedInventories.containsKey(event.getWhoClicked().getUniqueId())) {
            InventoryGUI gui = openedInventories.get(event.getWhoClicked().getUniqueId());
            if (event.getClickedInventory() == null) {
                return;
            }
            if (event.getClickedInventory().getName().equals(gui.getTitle())) {
                if (event.getClick().isKeyboardClick() && !event.isShiftClick()) {
                    if (!gui.isDraggable(event.getSlot())) {
                        event.setCancelled(true);
                    }
                    return;
                }
                InventoryAction action;
                if (event.isLeftClick()) {
                    if (!gui.isDraggable(event.getSlot())) {
                        event.setCancelled(true);
                    }
                    if (event.isShiftClick()) {
                        action = gui.getShiftLCAction(event.getSlot());
                        if (action == null) {
                            action = gui.getLCAction(event.getSlot());
                        }
                    } else {
                        action = gui.getLCAction(event.getSlot());
                    }
                } else {
                    event.setCancelled(true);
                    if (event.isShiftClick()) {
                        action = gui.getShiftRCAction(event.getSlot());
                        if (action == null) {
                            action = gui.getRCAction(event.getSlot());
                        }
                    } else {
                        action = gui.getRCAction(event.getSlot());
                    }
                }
                if (action != null) {
                    InventoryGUIClickEvent clickEvent = new InventoryGUIClickEvent(
                            (Player) event.getWhoClicked(),
                            gui,
                            event.getSlot(),
                            event.getClickedInventory());
                    action.exec(clickEvent);
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(@NotNull InventoryCloseEvent event) {
        openedInventories.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryItemMove(@NotNull InventoryMoveItemEvent event) {
        InventoryHolder holder = event.getSource().getHolder();
        if (holder instanceof Player) {
            Player p = (Player) holder;
            if (openedInventories.containsKey(p.getUniqueId())) {
                InventoryGUI gui = openedInventories.get(p.getUniqueId());
                if (event.getSource().getName().equals(gui.getTitle())) {
                    if (!gui.getRemovedLoreLines().isEmpty()) {
                        ItemStack finalStack = new ItemStack(event.getItem());
                        ItemMeta meta = finalStack.getItemMeta();
                        if (meta.hasLore()) {
                            List<String> lore = new ArrayList<>(meta.getLore());
                            for (int index : gui.getRemovedLoreLines()) {
                                lore.remove(index);
                            }
                            meta.setLore(lore);
                            finalStack.setItemMeta(meta);
                            event.setItem(finalStack);
                        }
                    }
                }
            }
        }
    }

}
