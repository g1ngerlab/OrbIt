package taltech.orbitgame.loot;

/**
 * One persistent loot record stored in Supabase.
 */
public class LootEntry {
    private final String itemCode;
    private final int quantity;

    public LootEntry(String itemCode, int quantity) {
        this.itemCode = itemCode == null ? "" : itemCode;
        this.quantity = Math.max(0, quantity);
    }

    public String getItemCode() {
        return itemCode;
    }

    public int getQuantity() {
        return quantity;
    }
}

