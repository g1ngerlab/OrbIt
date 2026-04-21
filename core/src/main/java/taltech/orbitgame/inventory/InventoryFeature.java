package taltech.orbitgame.inventory;

import taltech.orbitgame.network.Network;

/**
 * Facade that coordinates input + network sync for inventory.
 */
public class InventoryFeature {
    private final InventoryState state = new InventoryState();
    private final InventoryInputController inputController = new InventoryInputController();
    private final InventoryNetworkSync networkSync = new InventoryNetworkSync();

    public InventoryState getState() {
        return state;
    }

    public void handleInput(boolean isInLobby,
                            boolean isPaused,
                            InventoryInputController.SlotResolver slotResolver,
                            InventoryInputController.DropAction dropAction,
                            InventoryInputController.MoveAction moveAction) {
        inputController.update(isInLobby, isPaused, state, slotResolver, dropAction, moveAction);
    }

    public int applyNetworkUpdate(Network.InventoryStateUpdate update, int localClientId) {
        return networkSync.applyUpdate(update, localClientId, state);
    }

    public int applyNetworkUpdateForItemType(Network.InventoryStateUpdate update,
                                             int localClientId,
                                             int itemTypeId) {
        return networkSync.applyUpdateForItemType(update, localClientId, state, itemTypeId);
    }
}

