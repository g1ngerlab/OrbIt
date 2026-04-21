package taltech.orbitgame.item;

/**
 * Test shop loot item available for ORB currency.
 */
public final class GoldenFrogItemDefinition extends ItemDefinition {
    public GoldenFrogItemDefinition() {
        super(ItemKind.GOLDEN_FROG, ItemCategory.GENERIC, "Golden Frog", "loot/golden_frog.png", 100, true);
    }
}

