package taltech.orbitgame.inventory;

import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.network.Network;

import java.util.List;

/**
 * Mutable inventory state for one player session.
 */
public class InventoryState {
    public static final int COLUMNS = 4;
    public static final int ROWS = 3;
    public static final int SLOT_COUNT = COLUMNS * ROWS;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
    private boolean visible;
    private int selectedSlot = -1;
    private int draggedFromSlot = -1;
    private ItemStack draggedStack;

    public ItemStack[] getSlots() {
        return slots;
    }

    public boolean isVisible() {
        return visible;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public int getDraggedFromSlot() {
        return draggedFromSlot;
    }

    public ItemStack getDraggedStack() {
        return draggedStack;
    }

    public boolean isDragging() {
        return draggedFromSlot >= 0 && draggedStack != null && !draggedStack.isEmpty();
    }

    public void toggleVisible() {
        visible = !visible;
        if (!visible) {
            selectedSlot = -1;
            clearDragging();
        }
    }

    public void selectSlot(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < SLOT_COUNT) {
            selectedSlot = slotIndex;
        }
    }

    public boolean hasDroppableSelection() {
        return selectedSlot >= 0
            && selectedSlot < slots.length
            && slots[selectedSlot] != null
            && !slots[selectedSlot].isEmpty();
    }

    public void beginDragging(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.length) {
            return;
        }

        ItemStack slot = slots[slotIndex];
        if (slot == null || slot.isEmpty()) {
            clearDragging();
            return;
        }

        draggedFromSlot = slotIndex;
        draggedStack = new ItemStack(slot.getItemTypeId(), slot.getAmount());
        selectSlot(slotIndex);
    }

    public void clearDragging() {
        draggedFromSlot = -1;
        draggedStack = null;
    }

    public int getTotalItems() {
        int sum = 0;
        for (ItemStack stack : slots) {
            if (stack != null) {
                sum += Math.max(0, stack.getAmount());
            }
        }
        return sum;
    }

    public int getTotalItemsByType(int itemTypeId) {
        int sum = 0;
        for (ItemStack stack : slots) {
            if (stack != null && stack.getItemTypeId() == itemTypeId) {
                sum += Math.max(0, stack.getAmount());
            }
        }
        return sum;
    }

    public void applyNetworkSlots(List<Network.InventorySlotState> slots) {
        for (int i = 0; i < this.slots.length; i++) {
            this.slots[i] = null;
        }
        if (slots == null) {
            return;
        }

        int limit = Math.min(this.slots.length, slots.size());
        for (int i = 0; i < limit; i++) {
            Network.InventorySlotState slot = slots.get(i);
            if (slot != null && slot.itemType != ItemKind.NONE.getId() && slot.amount > 0) {
                this.slots[i] = new ItemStack(slot.itemType, slot.amount);
            }
        }

        if (selectedSlot >= 0
            && selectedSlot < this.slots.length
            && (this.slots[selectedSlot] == null || this.slots[selectedSlot].isEmpty())) {
            selectedSlot = -1;
        }

        if (isDragging()) {
            if (draggedFromSlot < 0 || draggedFromSlot >= this.slots.length) {
                clearDragging();
            } else {
                ItemStack source = this.slots[draggedFromSlot];
                if (source == null || source.isEmpty()) {
                    clearDragging();
                } else {
                    draggedStack = new ItemStack(source.getItemTypeId(), source.getAmount());
                }
            }
        }
    }
}

