package taltech.orbitgame.inventory;

import taltech.orbitgame.network.Network;

/**
 * Applies server inventory snapshots to local state.
 */
public class InventoryNetworkSync {
    public int applyUpdate(Network.InventoryStateUpdate update, int localClientId, InventoryState state) {
        return applyUpdateForItemType(update, localClientId, state, -1);
    }

    public int applyUpdateForItemType(Network.InventoryStateUpdate update,
                                      int localClientId,
                                      InventoryState state,
                                      int itemTypeId) {
        if (update == null || state == null || update.clientId != localClientId) {
            return 0;
        }

        int before = itemTypeId < 0 ? state.getTotalItems() : state.getTotalItemsByType(itemTypeId);
        state.applyNetworkSlots(update.slots);
        int after = itemTypeId < 0 ? state.getTotalItems() : state.getTotalItemsByType(itemTypeId);
        return Math.max(0, after - before);
    }
}

