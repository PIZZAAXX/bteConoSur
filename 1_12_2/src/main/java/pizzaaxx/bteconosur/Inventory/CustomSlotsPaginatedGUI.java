package pizzaaxx.bteconosur.Inventory;

import com.google.common.collect.Lists;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pizzaaxx.bteconosur.BTEConoSur;

import java.util.*;

public class CustomSlotsPaginatedGUI {

    private final String title;
    private final int rows;
    private final Integer[] paginatedSlotIndexes;
    private boolean draggable;
    private final List<PaginatedInventoryGUI.InventoryGUISlot> paginatedSlots;

    private final int backArrowSlotIndex;
    private final int nextArrowSlotIndex;

    private final Map<Integer, PaginatedInventoryGUI.InventoryGUISlot> staticSlots;

    public CustomSlotsPaginatedGUI(String title, int rows, Integer[] paginatedSlotIndexes, int backArrowSlotIndex, int nextArrowSlotIndex) {
        this.title = title;
        this.rows = rows;
        this.paginatedSlotIndexes = paginatedSlotIndexes;
        this.backArrowSlotIndex = backArrowSlotIndex;
        this.nextArrowSlotIndex = nextArrowSlotIndex;

        this.draggable = false;
        this.paginatedSlots = new ArrayList<>();
        this.staticSlots = new HashMap<>();
    }

    public void setDraggable(boolean draggable) {
        this.draggable = draggable;
    }

    public void addPaginated(
            ItemStack itemStack,
            InventoryAction leftClick,
            InventoryAction leftClickShift,
            InventoryAction rightClick,
            InventoryAction rightClickShift
    ) {
        paginatedSlots.add(
                new PaginatedInventoryGUI.InventoryGUISlot(
                        itemStack,
                        leftClick,
                        leftClickShift,
                        rightClick,
                        rightClickShift
                )
        );
    }

    public void setStatic(
            int slot, ItemStack itemStack
    ) {
        this.setStatic(
                slot,
                itemStack,
                null,
                null,
                null,
                null
        );
    }

    public void setStatic(
            int slot, ItemStack itemStack, InventoryAction leftClick
    ) {
        this.setStatic(
                slot,
                itemStack,
                leftClick,
                null,
                null,
                null
        );
    }

    public void setStatic(
            int slot,
            ItemStack itemStack,
            InventoryAction leftClick,
            InventoryAction leftClickShift,
            InventoryAction rightClick,
            InventoryAction rightClickShift
    ) {
        staticSlots.put(
                slot,
                new PaginatedInventoryGUI.InventoryGUISlot(
                        itemStack,
                        leftClick,
                        leftClickShift,
                        rightClick,
                        rightClickShift
                )
        );
    }

    public void openTo(Player player, BTEConoSur plugin) {
        final List<InventoryGUI> guis = new ArrayList<>();

        List<List<PaginatedInventoryGUI.InventoryGUISlot>> lists;
        if (paginatedSlots.isEmpty()) {
            lists = new ArrayList<>();
            lists.add(new ArrayList<>());
        } else {
            lists = Lists.partition(paginatedSlots, this.paginatedSlotIndexes.length);
        }

        int total = lists.size();
        int counter = 1;
        List<Integer> paginatedSlotsIndexes = Arrays.asList(this.paginatedSlotIndexes);
        for (List<PaginatedInventoryGUI.InventoryGUISlot> slots : lists) {
            InventoryGUI gui = new InventoryGUI(
                    this.rows,
                    this.title + " (" + counter + "/" + total + ")",
                    null
            );

            for (int i = 0; i < rows * 9; i++) {
                if (!paginatedSlotsIndexes.contains(i)) {
                    gui.setItem(
                            new ItemBuilder(Material.STAINED_GLASS_PANE, 1, 15)
                                    .name(" ")
                                    .build(),
                            i
                    );
                }
            }

            for (int slotIndex : this.staticSlots.keySet()) {
                PaginatedInventoryGUI.InventoryGUISlot slot = this.staticSlots.get(slotIndex);
                gui.setItem(
                        slot.getItem(),
                        slotIndex
                );

                if (slot.getLeftClick() != null) {
                    gui.setLCAction(
                            slot.getLeftClick(),
                            slotIndex
                    );
                }

                if (slot.getShiftLeftClick() != null) {
                    gui.setShiftLCAction(
                            slot.getShiftLeftClick(),
                            slotIndex
                    );
                }

                if (slot.getRightClick() != null) {
                    gui.setRCAction(
                            slot.getRightClick(),
                            slotIndex
                    );
                }

                if (slot.getShiftRightClick() != null) {
                    gui.setShiftRCAction(
                            slot.getShiftRightClick(),
                            slotIndex
                    );
                }
            }

            int paginatedCounter = 0;
            for (PaginatedInventoryGUI.InventoryGUISlot slot : slots) {
                int slotIndex = this.paginatedSlotIndexes[paginatedCounter];

                gui.setItem(
                        slot.getItem(),
                        slotIndex
                );

                if (slot.getLeftClick() != null) {
                    gui.setLCAction(
                            slot.getLeftClick(),
                            slotIndex
                    );
                }

                if (slot.getShiftLeftClick() != null) {
                    gui.setShiftLCAction(
                            slot.getShiftLeftClick(),
                            slotIndex
                    );
                }

                if (slot.getRightClick() != null) {
                    gui.setRCAction(
                            slot.getRightClick(),
                            slotIndex
                    );
                }

                if (slot.getShiftRightClick() != null) {
                    gui.setShiftRCAction(
                            slot.getShiftRightClick(),
                            slotIndex
                    );
                }

                paginatedCounter++;
            }

            int finalCounter = counter;
            if (counter - 2 >= 0) {
                gui.setItem(
                        ItemBuilder.head(
                                ItemBuilder.BACK_HEAD,
                                "Anterior " + (counter - 1) + "/" + total + ")",
                                null
                        ),
                        backArrowSlotIndex
                );
                gui.setLCAction(
                        event -> plugin.getInventoryHandler().open(
                                player,
                                guis.get(finalCounter - 2)
                        ),
                        backArrowSlotIndex
                );
            }

            if (counter < total) {
                gui.setItem(
                        ItemBuilder.head(
                                ItemBuilder.NEXT_HEAD,
                                "Siguiente (" + counter + 1 + "/" + total + ")",
                                null
                        ),
                        nextArrowSlotIndex
                );
                gui.setLCAction(
                        event -> plugin.getInventoryHandler().open(
                                player,
                                guis.get(finalCounter)
                        ),
                        nextArrowSlotIndex
                );
            }

            guis.add(gui);

            counter++;
        }

        plugin.getInventoryHandler().open(
                player,
                guis.get(0)
        );
    }
}
