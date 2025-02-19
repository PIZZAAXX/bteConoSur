package pizzaaxx.bteconosur.Inventory;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public class InventoryGUI {
    private int rows;
    private String title;
    private final Map<Integer, ItemStack> items;
    private final Set<Integer> draggableSlots = new HashSet<>();
    private ItemStack background;
    private final Map<Integer, InventoryAction> leftClickActions;
    private final Map<Integer, InventoryAction> shiftLeftClickActions;
    private final Map<Integer, InventoryAction> rightClickActions;
    private final Map<Integer, InventoryAction> shiftRightClickActions;
    private final Map<Integer, InventoryDataSet> data;
    private final Set<Integer> removedLoreLinesOnMove;


    /**
     * Creates a new InventoryGUI with a non-default background.
     * @param rows The amount of rows of this inventory. Can be set to a maximum of 6.
     * @param title The title of this inventory.
     * @param background A custom item for all the empty slots. Can be set to null.
     */
    public InventoryGUI(int rows, @NotNull String title, @Nullable ItemStack background) {
        this.rows = rows;
        this.title = title;
        this.items = new HashMap<>();
        this.leftClickActions = new HashMap<>();
        this.shiftLeftClickActions = new HashMap<>();
        this.rightClickActions = new HashMap<>();
        this.shiftRightClickActions = new HashMap<>();
        this.data = new HashMap<>();
        this.background = background;
        this.removedLoreLinesOnMove = new HashSet<>();
    }

    /**
     * Creates a new InventoryGUI with a default background (Black Stained Glass Panes).
     * @param rows The amount of rows of this inventory. Can be set to a maximum of 6.
     * @param title The title of this inventory.
     */
    public InventoryGUI(int rows, @NotNull String title) {
        this.rows = rows;
        this.title = title;
        this.items = new HashMap<>();
        this.leftClickActions = new HashMap<>();
        this.shiftLeftClickActions = new HashMap<>();
        this.rightClickActions = new HashMap<>();
        this.shiftRightClickActions = new HashMap<>();
        this.data = new HashMap<>();
        ItemStack background = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = background.getItemMeta();
        meta.setDisplayName(" ");
        background.setItemMeta(meta);
        this.background = background;
        this.removedLoreLinesOnMove = new HashSet<>();
    }

    /**
     * Modifies the amount of rows of this GUI.
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    /**
     * Modifies the title of this GUI.
     */
    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    /**
     * Modifies the {@link ItemStack} to be used as background on this GUI.
     */
    public void setBackground(@Nullable ItemStack background) {
        this.background = background;
    }

    /**
     * Sets an empty slot. This slot won't have an item nor a background.
     */
    public void setEmptySlot(int slot) {
        items.put(slot, null);
    }

    /**
     * Sets empty slots. These slots won't have an item nor a background.
     */
    public void setEmptySlots(@NotNull int... slots) {
        for (int slot : slots) {
            this.setEmptySlot(slot);
        }
    }

    /**
     * Marks slots as draggable. Draggable slots won't stop the {@link org.bukkit.event.inventory.InventoryClickEvent} and will allow the player to retrieve the item.
     */
    public void setDraggable(@NotNull int... slots) {
        for (int slot : slots) {
            this.setDraggable(slot);
        }
    }

    /**
     * Marks a slot as draggable. Draggable slots won't stop the {@link org.bukkit.event.inventory.InventoryClickEvent} and will allow the player to retrieve the item.
     */
    public void setDraggable(int slot) {
        this.draggableSlots.add(slot);
    }

    /**
     * Sets an item of this GUI.
     */
    public void setItem(ItemStack item, int slot) {
        items.put(slot, item);
    }

    /**
     * Sets the same item to many slots of this GUI.
     */
    public void setItems(ItemStack itemStack, @NotNull int... slots) {
        for (int slot : slots) {
            this.setItem(itemStack, slot);
        }
    }

    /**
     * Sets an item of this GUI based on its {@link Material}.
     */
    public void setItem(Material material, int slot) {
        items.put(slot, new ItemStack(material));
    }

    /**
     * Set the same item to many slots of this GUI based on its {@link Material}.
     */
    public void setItems(Material material, @NotNull int... slots) {
        for (int slot : slots) {
            this.setItem(material, slot);
        }
    }

    /**
     * Defines an {@link InventoryAction} that should be run when a player left-clicks the inventory slot.
     * If there is no shift-left-click action defined on this slot, this action will also be triggered by shift-left-clicks.
     * @param action The action that should be run.
     * @param slot The slot that has to be clicked for this action to run.
     */
    public void setLCAction(InventoryAction action, int slot) {
        leftClickActions.put(slot, action);
    }

    /**
     * Defines an {@link InventoryAction} that should be run when a player left-clicks the inventory slot while pressing shift.
     * @param action The action that should be run.
     * @param slot The slot that has to be clicked for this action to run.
     */
    public void setShiftLCAction(InventoryAction action, int slot) {
        shiftLeftClickActions.put(slot, action);
    }

    /**
     * Defines an {@link InventoryAction} that should be run when a player right-clicks the inventory slot.
     * If there is no shift-right-click action defined on this slot, this action will also be triggered by shift-right-clicks.
     * @param action The action that should be run.
     * @param slot The slot that has to be clicked for this action to run.
     */
    public void setRCAction(InventoryAction action, int slot) {
        rightClickActions.put(slot, action);
    }

    /**
     * Defines an {@link InventoryAction} that should be run when a player right-clicks the inventory slot while pressing shift.
     * @param action The action that should be run.
     * @param slot The slot that has to be clicked for this action to run.
     */
    public void setShiftRCAction(InventoryAction action, int slot) {
        shiftRightClickActions.put(slot, action);
    }

    public void deleteLCAction(int slot) {
        leftClickActions.remove(slot);
    }
    public void deleteRCAction(int slot) {
        rightClickActions.remove(slot);
    }

    public void deleteShiftLCAction(int slot) {
        shiftLeftClickActions.remove(slot);
    }

    public void deleteShiftRCAction(int slot) {
        shiftRightClickActions.remove(slot);
    }

    public void addRemoveLoreLine(int line) {
        this.removedLoreLinesOnMove.add(line);
    }

    public Set<Integer> getRemovedLoreLines() {
        return removedLoreLinesOnMove;
    }

    public void setData(String key, Object value, int slot) {
        this.data.put(
                slot,
                new InventoryDataSet(
                        key, value
                )
        );
    }

    public void setData(Map<String, Object> data, int slot) {
        this.data.put(
                slot,
                new InventoryDataSet(
                        data
                )
        );
    }

    public InventoryDataSet getData(int slot) {
        return data.get(slot);
    }

    /**
     * Shortcut for making the player run a command when left-clicking this slot.
     * @param command The command that should be run.
     * @param slot The slot that has to be clicked.
     */
    public void setCommand(@NotNull String command, int slot) {
        this.setLCAction(
                event -> event.getPlayer().performCommand(command),
                slot
        );
    }

    /**
     * Shortcut for making the player teleport to a location when left-clicking this slot.
     * @param loc The location the player should be teleported to.
     * @param slot The slot that has to be clicked.
     */
    public void setTP(Location loc, int slot) {
        this.setLCAction(
                event -> event.getPlayer().teleport(loc),
                slot
        );
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    @Nullable
    public InventoryAction getLCAction(int slot) {
        return leftClickActions.get(slot);
    }
    public InventoryAction getShiftLCAction(int slot) {
        return shiftLeftClickActions.get(slot);
    }

    @Nullable
    public InventoryAction getRCAction(int slot) {
        return rightClickActions.get(slot);
    }
    public InventoryAction getShiftRCAction(int slot) {
        return shiftRightClickActions.get(slot);
    }

    public boolean isDraggable(int slot) {
        return this.draggableSlots.contains(slot);
    }

    /**
     * Creates an {@link Inventory} representation of this GUI.
     * @return The inventory.
     */
    @NotNull
    public Inventory buildInventory() {
        Inventory inventory = Bukkit.createInventory(
                null,
                rows * 9,
                title
        );

        if (this.background != null) {
            for (int i = 0; i < rows * 9; i++) {
                if (!items.containsKey(i)) {
                    inventory.setItem(i, this.background);
                }
            }
        }

        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue());
        }

        return inventory;
    }

    public static int[] getIntInRange(int from, int to) {
        return IntStream.rangeClosed(from, to).toArray();
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }

        InventoryGUI gui = (InventoryGUI) obj;

        if (!this.title.equals(gui.title)) {
            return false;
        }

        if (this.background != gui.background) {
            return false;
        }

        if (this.draggableSlots != gui.draggableSlots) {
            return false;
        }

        if (!this.items.equals(gui.items)) {
            return false;
        }

        if (!this.leftClickActions.equals(gui.leftClickActions)) {
            return false;
        }

        if (!this.shiftLeftClickActions.equals(gui.shiftLeftClickActions)) {
            return false;
        }

        if (!this.rightClickActions.equals(gui.rightClickActions)) {
            return false;
        }

        if (!this.shiftRightClickActions.equals(gui.shiftRightClickActions)) {
            return false;
        }

        return true;
    }
}
