package server;

import server.model.LobbyPlayer;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.world.MapCollisionGrid;

import java.util.Map;

/**
 * Manages inventory operations: adding, removing, moving items in player inventories.
 * Delegates to LobbyItemDropManager for dropping/spawning orbs.
 */
public class LobbyInventoryManager {
    private static final int INVENTORY_SLOTS = 12;
    private static final float PLAYER_WIDTH = 32f;
    private static final float PLAYER_HEIGHT = 48f;
    private static final float PLAYER_DROP_MIN_DISTANCE = 44f;
    private static final float PLAYER_DROP_MAX_DISTANCE = 72f;

    private final Map<Integer, LobbyPlayer> players;
    private final Map<Integer, ItemStack[]> playerInventories;
    private final LobbyBroadcaster broadcaster;
    private final LobbyItemDropManager itemDropManager;
    private final MapCollisionGrid collisionGrid;

    public LobbyInventoryManager(Map<Integer, LobbyPlayer> players,
                                  Map<Integer, ItemStack[]> playerInventories,
                                  LobbyBroadcaster broadcaster,
                                  LobbyItemDropManager itemDropManager,
                                  MapCollisionGrid collisionGrid) {
        this.players = players;
        this.playerInventories = playerInventories;
        this.broadcaster = broadcaster;
        this.itemDropManager = itemDropManager;
        this.collisionGrid = collisionGrid;
    }

    public ItemStack[] createEmptyInventory() {
        return new ItemStack[INVENTORY_SLOTS];
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

        for (int i = 0; i < inventory.length; i++) {
            ItemStack slot = inventory[i];
            if (slot == null || slot.getItemTypeId() != itemTypeId || slot.getAmount() >= maxStack) {
                continue;
            }

            int canAdd = Math.min(remaining, maxStack - slot.getAmount());
            slot.add(canAdd);
            remaining -= canAdd;
            if (remaining <= 0) {
                sendInventoryUpdate(clientId);
                return true;
            }
        }

        for (int i = 0; i < inventory.length; i++) {
            if (inventory[i] == null || inventory[i].isEmpty()) {
                int put = Math.min(remaining, maxStack);
                inventory[i] = new ItemStack(itemTypeId, put);
                remaining -= put;
                if (remaining <= 0) {
                    sendInventoryUpdate(clientId);
                    return true;
                }
            }
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

        if (to == null || to.isEmpty()) {
            inventory[toSlotIndex] = from;
            inventory[fromSlotIndex] = null;
            sendInventoryUpdate(clientId);
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

        return false;
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
}

