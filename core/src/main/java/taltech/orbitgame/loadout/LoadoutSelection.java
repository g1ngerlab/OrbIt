package taltech.orbitgame.loadout;

import taltech.orbitgame.inventory.InventoryLayout;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.network.Network;

import java.util.ArrayList;

/**
 * Temporary pre-match loadout draft that can be sent to the server.
 */
public class LoadoutSelection {
    private final ItemStack[] slots = new ItemStack[InventoryLayout.TOTAL_SLOT_COUNT];

    public LoadoutSelection() {
        ensureStarterWeapon();
    }

    public ItemStack[] getSlots() {
        return slots;
    }

    public boolean isLockedSlot(int slotIndex) {
        return slotIndex == InventoryLayout.MAIN_SLOT_COUNT;
    }

    public boolean canPlaceInSlot(int itemTypeId, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.length) {
            return false;
        }
        if (!InventoryLayout.isWeaponSlot(slotIndex)) {
            return true;
        }
        return ItemRegistry.isWeapon(itemTypeId);
    }

    public void ensureStarterWeapon() {
        int starterSlot = InventoryLayout.MAIN_SLOT_COUNT;
        if (starterSlot < 0 || starterSlot >= slots.length) {
            return;
        }
        if (slots[starterSlot] == null || slots[starterSlot].isEmpty()) {
            slots[starterSlot] = new ItemStack(ItemKind.STARTER_SHOTGUN.getId(), 1);
        }
    }

    public int addItem(int itemTypeId, int amount, int preferredSlotIndex) {
        if (amount <= 0 || itemTypeId == ItemKind.NONE.getId()) {
            return 0;
        }

        int maxStack = Math.max(1, ItemRegistry.getMaxStack(itemTypeId));
        int remaining = amount;

        if (preferredSlotIndex >= 0 && preferredSlotIndex < slots.length && canPlaceInSlot(itemTypeId, preferredSlotIndex)) {
            remaining = addToSlot(preferredSlotIndex, itemTypeId, remaining, maxStack);
        }

        remaining = fillMatchingStacks(itemTypeId, remaining, maxStack, preferredSlotIndex);
        remaining = fillEmptySlots(itemTypeId, remaining, maxStack, preferredSlotIndex);

        return amount - remaining;
    }

    public ItemStack removeStack(int slotIndex) {
        if (isLockedSlot(slotIndex) || slotIndex < 0 || slotIndex >= slots.length) {
            return null;
        }

        ItemStack stack = slots[slotIndex];
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        slots[slotIndex] = null;
        return new ItemStack(stack.getItemTypeId(), stack.getAmount());
    }

    public ArrayList<Network.InventorySlotState> toNetworkSlots() {
        ArrayList<Network.InventorySlotState> result = new ArrayList<>(slots.length);
        for (ItemStack stack : slots) {
            int itemType = stack == null ? Network.ItemType.NONE : stack.getItemTypeId();
            int amount = stack == null ? 0 : Math.max(0, stack.getAmount());
            result.add(new Network.InventorySlotState(itemType, amount));
        }
        return result;
    }

    private int addToSlot(int slotIndex, int itemTypeId, int remaining, int maxStack) {
        if (remaining <= 0 || slotIndex < 0 || slotIndex >= slots.length) {
            return remaining;
        }
        if (!canPlaceInSlot(itemTypeId, slotIndex)) {
            return remaining;
        }

        ItemStack slot = slots[slotIndex];
        if (slot == null || slot.isEmpty()) {
            int put = Math.min(remaining, maxStack);
            slots[slotIndex] = new ItemStack(itemTypeId, put);
            return remaining - put;
        }

        if (slot.getItemTypeId() != itemTypeId || slot.getAmount() >= maxStack) {
            return remaining;
        }

        int canAdd = Math.min(remaining, maxStack - slot.getAmount());
        slot.add(canAdd);
        return remaining - canAdd;
    }

    private int fillMatchingStacks(int itemTypeId, int remaining, int maxStack, int preferredSlotIndex) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (i == preferredSlotIndex) {
                continue;
            }
            ItemStack slot = slots[i];
            if (slot == null || slot.isEmpty() || slot.getItemTypeId() != itemTypeId || slot.getAmount() >= maxStack) {
                continue;
            }
            if (!canPlaceInSlot(itemTypeId, i)) {
                continue;
            }
            int canAdd = Math.min(remaining, maxStack - slot.getAmount());
            slot.add(canAdd);
            remaining -= canAdd;
        }
        return remaining;
    }

    private int fillEmptySlots(int itemTypeId, int remaining, int maxStack, int preferredSlotIndex) {
        for (int i = 0; i < slots.length && remaining > 0; i++) {
            if (i == preferredSlotIndex) {
                continue;
            }
            if (!canPlaceInSlot(itemTypeId, i)) {
                continue;
            }
            ItemStack slot = slots[i];
            if (slot != null && !slot.isEmpty()) {
                continue;
            }
            int put = Math.min(remaining, maxStack);
            slots[i] = new ItemStack(itemTypeId, put);
            remaining -= put;
        }
        return remaining;
    }
}

