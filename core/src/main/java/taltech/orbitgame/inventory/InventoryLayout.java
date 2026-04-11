package taltech.orbitgame.inventory;

/**
 * Shared inventory slot layout for client UI and server validation.
 */
public final class InventoryLayout {
    public static final int MAIN_COLUMNS = 4;
    public static final int MAIN_ROWS = 3;
    public static final int MAIN_SLOT_COUNT = MAIN_COLUMNS * MAIN_ROWS;

    public static final int WEAPON_SLOT_COUNT = 2;
    public static final int TOTAL_SLOT_COUNT = MAIN_SLOT_COUNT + WEAPON_SLOT_COUNT;

    private InventoryLayout() {
    }

    public static boolean isMainSlot(int slotIndex) {
        return slotIndex >= 0 && slotIndex < MAIN_SLOT_COUNT;
    }

    public static boolean isWeaponSlot(int slotIndex) {
        return slotIndex >= MAIN_SLOT_COUNT && slotIndex < TOTAL_SLOT_COUNT;
    }

    public static InventorySlotType getSlotType(int slotIndex) {
        if (isWeaponSlot(slotIndex)) {
            return InventorySlotType.WEAPON;
        }
        return InventorySlotType.MAIN;
    }
}

