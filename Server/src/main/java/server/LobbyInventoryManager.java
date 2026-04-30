package server;

import server.model.LobbyPlayer;
import taltech.orbitgame.inventory.InventoryLayout;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.network.Network;

import java.util.List;
import java.util.Map;

/**
 * Manages inventory operations: adding, removing, moving items in player inventories.
 * Delegates to LobbyItemDropManager for dropping/spawning orbs.
 */
public class LobbyInventoryManager {
    private static final int INVENTORY_SLOTS = InventoryLayout.TOTAL_SLOT_COUNT;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 48f;
    private static final float PLAYER_DROP_MIN_DISTANCE = 44f;
    private static final float PLAYER_DROP_MAX_DISTANCE = 72f;
    private static final int STARTER_WEAPON_ITEM_TYPE = ItemKind.STARTER_SHOTGUN.getId();

    private final Map<Integer, LobbyPlayer> players;
    private final Map<Integer, ItemStack[]> playerInventories;
    private final LobbyItemDropManager itemDropManager;

    public LobbyInventoryManager(Map<Integer, LobbyPlayer> players,
                                  Map<Integer, ItemStack[]> playerInventories,
                                  LobbyItemDropManager itemDropManager) {
        this.players = players;
        this.playerInventories = playerInventories;
        this.itemDropManager = itemDropManager;
    }

    public ItemStack[] createEmptyInventory() {
        return new ItemStack[INVENTORY_SLOTS];
    }

    public void applyStartingLoadout(int clientId, List<Network.InventorySlotState> loadoutSlots) {
        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null) {
            return;
        }

        for (int i = 0; i < inventory.length; i++) {
            inventory[i] = null;
        }

        if (loadoutSlots != null) {
            int limit = Math.min(inventory.length, loadoutSlots.size());
            for (int i = 0; i < limit; i++) {
                Network.InventorySlotState slot = loadoutSlots.get(i);
                if (slot == null || slot.itemType == ItemKind.NONE.getId() || slot.amount <= 0) {
                    continue;
                }
                if (!canPlaceInSlot(slot.itemType, i)) {
                    continue;
                }
                int amount = Math.min(slot.amount, Math.max(1, ItemRegistry.getMaxStack(slot.itemType)));
                inventory[i] = new ItemStack(slot.itemType, amount);
            }
        }

        grantStarterWeaponIfMissing(clientId, STARTER_WEAPON_ITEM_TYPE);
        sendInventoryUpdate(clientId);
    }

    public boolean addItemToInventory(int clientId, int itemTypeId, int amount) {
        if (amount <= 0 || itemTypeId == ItemKind.NONE.getId()) {
            return false;
        }

        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null) {
            return false;
        }

        int remaining = amount;
        int maxStack = Math.max(1, ItemRegistry.getMaxStack(itemTypeId));
        boolean weaponItem = ItemRegistry.isWeapon(itemTypeId);

        if (weaponItem) {
            remaining = fillExistingStacks(inventory, itemTypeId, remaining, maxStack,
                InventoryLayout.MAIN_SLOT_COUNT, InventoryLayout.TOTAL_SLOT_COUNT);
            if (remaining <= 0) {
                sendInventoryUpdate(clientId);
                return true;
            }
            remaining = fillExistingStacks(inventory, itemTypeId, remaining, maxStack,
                0, InventoryLayout.MAIN_SLOT_COUNT);
            if (remaining <= 0) {
                sendInventoryUpdate(clientId);
                return true;
            }

            remaining = fillEmptySlots(inventory, itemTypeId, remaining, maxStack,
                InventoryLayout.MAIN_SLOT_COUNT, InventoryLayout.TOTAL_SLOT_COUNT);
            if (remaining <= 0) {
                sendInventoryUpdate(clientId);
                return true;
            }
            remaining = fillEmptySlots(inventory, itemTypeId, remaining, maxStack,
                0, InventoryLayout.MAIN_SLOT_COUNT);
            if (remaining <= 0) {
                sendInventoryUpdate(clientId);
                return true;
            }

            if (remaining < amount) {
                sendInventoryUpdate(clientId);
                return true;
            }
            return false;
        }

        remaining = fillExistingStacks(inventory, itemTypeId, remaining, maxStack,
            0, InventoryLayout.MAIN_SLOT_COUNT);
        if (remaining <= 0) {
            sendInventoryUpdate(clientId);
            return true;
        }

        remaining = fillEmptySlots(inventory, itemTypeId, remaining, maxStack,
            0, InventoryLayout.MAIN_SLOT_COUNT);
        if (remaining <= 0) {
            sendInventoryUpdate(clientId);
            return true;
        }

        if (remaining < amount) {
            sendInventoryUpdate(clientId);
            return true;
        }

        return false;
    }

    public void dropInventoryItem(int clientId, int slotIndex) {
        LobbyPlayer player = players.get(clientId);
        ItemStack[] inventory = playerInventories.get(clientId);
        if (player == null || inventory == null || slotIndex < 0 || slotIndex >= inventory.length) {
            return;
        }
        ItemStack stack = inventory[slotIndex];
        if (stack == null || stack.getAmount() <= 0) {
            return;
        }

        if (!dropStackFromPlayerPosition(player, stack.getItemTypeId(), 1)) {
            return;
        }
        stack.remove(1);
        if (stack.isEmpty()) {
            inventory[slotIndex] = null;
        }
        sendInventoryUpdate(clientId);
    }

    public void moveInventoryItem(int clientId, int fromSlotIndex, int toSlotIndex) {
        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null
            || fromSlotIndex < 0 || fromSlotIndex >= inventory.length
            || toSlotIndex < 0 || toSlotIndex >= inventory.length
            || fromSlotIndex == toSlotIndex) {
            return;
        }

        ItemStack from = inventory[fromSlotIndex];
        ItemStack to = inventory[toSlotIndex];
        if (from == null || from.isEmpty()) {
            return;
        }

        if (!canPlaceInSlot(from.getItemTypeId(), toSlotIndex)) {
            return;
        }

        if (to == null || to.isEmpty()) {
            inventory[toSlotIndex] = from;
            inventory[fromSlotIndex] = null;
            sendInventoryUpdate(clientId);
            return;
        }

        if (!canPlaceInSlot(to.getItemTypeId(), fromSlotIndex)) {
            return;
        }

        if (from.getItemTypeId() == to.getItemTypeId()) {
            int maxStack = Math.max(1, ItemRegistry.getMaxStack(from.getItemTypeId()));
            int canMove = Math.min(from.getAmount(), Math.max(0, maxStack - to.getAmount()));
            if (canMove > 0) {
                to.add(canMove);
                from.remove(canMove);
                if (from.isEmpty()) {
                    inventory[fromSlotIndex] = null;
                }
                sendInventoryUpdate(clientId);
            }
            return;
        }

        inventory[fromSlotIndex] = to;
        inventory[toSlotIndex] = from;
        sendInventoryUpdate(clientId);
    }

    public boolean purchaseItemWithOrbCost(int clientId, int itemTypeId, int amount, int orbCost) {
        if (itemTypeId == ItemKind.NONE.getId() || amount <= 0 || orbCost <= 0) {
            return false;
        }

        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null) {
            return false;
        }

        int totalOrbs = countItemAmount(inventory, ItemKind.ORB.getId());
        if (totalOrbs < orbCost) {
            return false;
        }

        int insertCapacity = calculateInsertCapacity(inventory, itemTypeId);
        if (insertCapacity < amount) {
            return false;
        }

        if (!removeItemAmount(inventory, ItemKind.ORB.getId(), orbCost)) {
            return false;
        }

        boolean added = addItemToInventory(clientId, itemTypeId, amount);
        if (!added) {
            addItemToInventory(clientId, ItemKind.ORB.getId(), orbCost);
            return false;
        }

        return true;
    }

    public void dropAllInventoryItems(LobbyPlayer player) {
        ItemStack[] inventory = playerInventories.get(player.getClientId());
        if (inventory == null) {
            return;
        }

        for (int i = 0; i < inventory.length; i++) {
            ItemStack stack = inventory[i];
            if (stack != null && stack.getAmount() > 0) {
                dropStackFromPlayerPosition(player, stack.getItemTypeId(), stack.getAmount());
            }
            inventory[i] = null;
        }
    }

    public void grantStarterWeaponIfMissing(int clientId, int weaponTypeId) {
        if (!ItemRegistry.isWeapon(weaponTypeId)) {
            return;
        }
        ItemStack[] inventory = playerInventories.get(clientId);
        if (inventory == null) {
            return;
        }

        for (ItemStack slot : inventory) {
            if (slot != null && !slot.isEmpty() && slot.getItemTypeId() == weaponTypeId) {
                return;
            }
        }

        for (int i = InventoryLayout.MAIN_SLOT_COUNT; i < InventoryLayout.TOTAL_SLOT_COUNT; i++) {
            if (inventory[i] == null || inventory[i].isEmpty()) {
                inventory[i] = new ItemStack(weaponTypeId, 1);
                sendInventoryUpdate(clientId);
                return;
            }
        }

        for (int i = 0; i < InventoryLayout.MAIN_SLOT_COUNT; i++) {
            if (inventory[i] == null || inventory[i].isEmpty()) {
                inventory[i] = new ItemStack(weaponTypeId, 1);
                sendInventoryUpdate(clientId);
                return;
            }
        }
    }

    private boolean dropStackFromPlayerPosition(LobbyPlayer player, int itemTypeId, int amount) {
        if (amount <= 0) {
            return false;
        }

        if (!ItemRegistry.isDroppableInWorld(itemTypeId)) {
            return false;
        }

        float centerX = player.getX() + PLAYER_WIDTH * 0.5f;
        float centerY = player.getY() + PLAYER_HEIGHT * 0.5f;
        if (itemTypeId == ItemKind.ORB.getId()) {
            itemDropManager.spawnOrbsAroundPoint(centerX, centerY, amount, PLAYER_DROP_MIN_DISTANCE, PLAYER_DROP_MAX_DISTANCE);
            return true;
        }

        itemDropManager.spawnItemsAroundPoint(itemTypeId, centerX, centerY, amount, PLAYER_DROP_MIN_DISTANCE, PLAYER_DROP_MAX_DISTANCE);
        return true;
    }


    private void sendInventoryUpdate(int clientId) {
        LobbyPlayer player = players.get(clientId);
        if (player == null) {
            return;
        }
        player.getConnection().sendTCP(createInventoryStateUpdate(clientId));
    }

    private taltech.orbitgame.network.Network.InventoryStateUpdate createInventoryStateUpdate(int clientId) {
        taltech.orbitgame.network.Network.InventoryStateUpdate update = new taltech.orbitgame.network.Network.InventoryStateUpdate(clientId);
        ItemStack[] inventory = playerInventories.get(clientId);

        for (int i = 0; i < INVENTORY_SLOTS; i++) {
            ItemStack stack = (inventory != null && i < inventory.length) ? inventory[i] : null;
            int amount = stack == null ? 0 : Math.max(0, stack.getAmount());
            int itemType = stack == null ? taltech.orbitgame.network.Network.ItemType.NONE : stack.getItemTypeId();
            update.slots.add(new taltech.orbitgame.network.Network.InventorySlotState(itemType, amount));
        }

        return update;
    }

    public taltech.orbitgame.network.Network.InventoryStateUpdate getInventoryStateUpdate(int clientId) {
        return createInventoryStateUpdate(clientId);
    }

    private boolean canPlaceInSlot(int itemTypeId, int slotIndex) {
        if (!InventoryLayout.isWeaponSlot(slotIndex)) {
            return true;
        }
        return ItemRegistry.isWeapon(itemTypeId);
    }

    private int fillExistingStacks(ItemStack[] inventory,
                                   int itemTypeId,
                                   int remaining,
                                   int maxStack,
                                   int fromInclusive,
                                   int toExclusive) {
        for (int i = fromInclusive; i < toExclusive && i < inventory.length; i++) {
            ItemStack slot = inventory[i];
            if (slot == null || slot.getItemTypeId() != itemTypeId || slot.getAmount() >= maxStack) {
                continue;
            }
            int canAdd = Math.min(remaining, maxStack - slot.getAmount());
            slot.add(canAdd);
            remaining -= canAdd;
            if (remaining <= 0) {
                return 0;
            }
        }
        return remaining;
    }

    private int fillEmptySlots(ItemStack[] inventory,
                               int itemTypeId,
                               int remaining,
                               int maxStack,
                               int fromInclusive,
                               int toExclusive) {
        for (int i = fromInclusive; i < toExclusive && i < inventory.length; i++) {
            if (inventory[i] != null && !inventory[i].isEmpty()) {
                continue;
            }
            int put = Math.min(remaining, maxStack);
            inventory[i] = new ItemStack(itemTypeId, put);
            remaining -= put;
            if (remaining <= 0) {
                return 0;
            }
        }
        return remaining;
    }

    private int countItemAmount(ItemStack[] inventory, int itemTypeId) {
        int total = 0;
        for (ItemStack slot : inventory) {
            if (slot == null || slot.isEmpty() || slot.getItemTypeId() != itemTypeId) {
                continue;
            }
            total += Math.max(0, slot.getAmount());
        }
        return total;
    }

    private boolean removeItemAmount(ItemStack[] inventory, int itemTypeId, int amount) {
        int remaining = amount;
        for (int i = 0; i < inventory.length && remaining > 0; i++) {
            ItemStack slot = inventory[i];
            if (slot == null || slot.isEmpty() || slot.getItemTypeId() != itemTypeId) {
                continue;
            }
            int take = Math.min(remaining, slot.getAmount());
            slot.remove(take);
            remaining -= take;
            if (slot.isEmpty()) {
                inventory[i] = null;
            }
        }
        return remaining <= 0;
    }

    private int calculateInsertCapacity(ItemStack[] inventory, int itemTypeId) {
        int maxStack = Math.max(1, ItemRegistry.getMaxStack(itemTypeId));
        int capacity = 0;

        for (int i = 0; i < inventory.length; i++) {
            if (!canPlaceInSlot(itemTypeId, i)) {
                continue;
            }
            ItemStack slot = inventory[i];
            if (slot == null || slot.isEmpty()) {
                capacity += maxStack;
                continue;
            }
            if (slot.getItemTypeId() == itemTypeId && slot.getAmount() < maxStack) {
                capacity += maxStack - slot.getAmount();
            }
        }

        return capacity;
    }
}

