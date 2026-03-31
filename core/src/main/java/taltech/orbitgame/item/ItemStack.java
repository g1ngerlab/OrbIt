package taltech.orbitgame.item;

/**
 * Mutable stack of one item type in a single inventory slot.
 */
public class ItemStack {
    private final int itemTypeId;
    private int amount;

    public ItemStack(int itemTypeId, int amount) {
        this.itemTypeId = itemTypeId;
        this.amount = Math.max(0, amount);
    }

    public int getItemTypeId() {
        return itemTypeId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = Math.max(0, amount);
    }

    public boolean isEmpty() {
        return amount <= 0;
    }

    public void add(int delta) {
        amount = Math.max(0, amount + delta);
    }

    public void remove(int delta) {
        amount = Math.max(0, amount - Math.max(0, delta));
    }
}

