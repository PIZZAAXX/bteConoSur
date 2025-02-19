package pizzaaxx.bteconosur.Inventory;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pizzaaxx.bteconosur.BTEConoSur;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PaginatedInventoryGUI {

    public static class InventoryGUISlot {
        private final ItemStack item;
        private final InventoryAction leftClick;
        private final InventoryAction shiftLeftClick;
        private final InventoryAction rightClick;
        private final InventoryAction shiftRightClick;

        public InventoryGUISlot(
                @NotNull ItemStack item,
                @Nullable InventoryAction leftClick,
                @Nullable InventoryAction shiftLeftClick,
                @Nullable InventoryAction rightClick,
                @Nullable InventoryAction shiftRightClick
        ) {
            this.item = item;
            this.leftClick = leftClick;
            this.shiftLeftClick = shiftLeftClick;
            this.rightClick = rightClick;
            this.shiftRightClick = shiftRightClick;
        }

        public ItemStack getItem() {
            return item;
        }

        public InventoryAction getLeftClick() {
            return leftClick;
        }

        public InventoryAction getShiftLeftClick() {
            return shiftLeftClick;
        }

        public InventoryAction getRightClick() {
            return rightClick;
        }

        public InventoryAction getShiftRightClick() {
            return shiftRightClick;
        }
    }

    private final String title;
    private final int rows;
    private final List<InventoryGUISlot> items;
    private boolean draggable;

    private final Set<Integer> removedLoreLines;

    public PaginatedInventoryGUI(int rows, @NotNull String title) {
        this.title = title;
        this.rows = rows;
        this.items = new ArrayList<>();
        this.draggable = false;
        this.removedLoreLines = new HashSet<>();
    }

    public void add(
            @NotNull ItemStack item,
            @Nullable InventoryAction leftClick,
            @Nullable InventoryAction shiftLeftClick,
            @Nullable InventoryAction rightClick,
            @Nullable InventoryAction shiftRightClick
    ) {
        this.items.add(
                new InventoryGUISlot(
                        item, leftClick, shiftLeftClick, rightClick, shiftRightClick
                )
        );
    }

    public void addRemovedLoreLine(int line) {
        removedLoreLines.add(line);
    }

     public void setDraggable(boolean draggable) {
        this.draggable = draggable;
     }

    /**
     * Creates the necessary GUIs for this pagination.
     * @param player The player to open this GUI to.
     * @param plugin The plugin that opens this GUI.
     */
    public void openTo(Player player, BTEConoSur plugin) {
        List<List<InventoryGUISlot>> slotLists;
        if (items.isEmpty()) {
            slotLists = new ArrayList<>();
            slotLists.add(new ArrayList<>());
        } else {
            slotLists = Lists.partition(items, (rows - 1) * 9);
        }

        final List<InventoryGUI> guis = new ArrayList<>();
        int count = 0;
        int total = slotLists.size();
        for (List<InventoryGUISlot> slots : slotLists) {
            InventoryGUI gui = new InventoryGUI(
                    rows,
                    title + " (" + (count + 1) + "/" + total + ")",
                    null
            );
            gui.setItems(
                    new ItemBuilder(Material.STAINED_GLASS_PANE, 1, 15)
                            .name(" ")
                            .build(),
                    InventoryGUI.getIntInRange(0, 8)
            );
            if (count > 0) {
                gui.setItem(
                        ItemBuilder.head(
                                ItemBuilder.BACK_HEAD,
                                "Anterior (" + count + "/" + total + ")",
                                null
                        ),
                        0
                );
                int finalCount = count;
                gui.setLCAction(
                        event -> plugin.getInventoryHandler().open(player, guis.get(finalCount - 1)),
                        0
                );
            }
            if (count + 1 < total) {
                gui.setItem(
                        ItemBuilder.head(
                                ItemBuilder.NEXT_HEAD,
                                "Siguiente (" + (count + 2) + "/" + total + ")",
                                null
                        ),
                        8
                );
                int finalCount = count;
                gui.setLCAction(
                        event -> plugin.getInventoryHandler().open(player, guis.get(finalCount + 1)),
                        8
                );
            }

            int j = 9;
            for (InventoryGUISlot slot : slots) {

                gui.setItem(
                        slot.item,
                        j
                );

                if (slot.leftClick != null) {
                    gui.setLCAction(
                            slot.leftClick,
                            j
                    );
                }

                if (slot.rightClick != null) {
                    gui.setRCAction(
                            slot.rightClick,
                            j
                    );
                }

                if (slot.shiftLeftClick != null) {
                    gui.setShiftLCAction(
                            slot.shiftLeftClick,
                            j
                    );
                }

                if (slot.shiftRightClick != null) {
                    gui.setShiftRCAction(
                            slot.shiftRightClick,
                            j
                    );
                }

                if (this.draggable) {
                    gui.setDraggable(j);
                }

                j++;
            }
            for (int line : removedLoreLines) {
                gui.addRemoveLoreLine(line);
            }
            guis.add(gui);
            count++;
        }
        plugin.getInventoryHandler().open(player, guis.get(0));
    }
}
