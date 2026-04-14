package taltech.orbitgame.item;

/**
 * Default starter weapon represented as a regular loot item.
 */
public final class StarterShotgunItemDefinition extends ItemDefinition {
    public StarterShotgunItemDefinition() {
        super(ItemKind.STARTER_SHOTGUN, ItemCategory.WEAPON, "Starter Shotgun", "guns/test_shotgun.png", 1, true);
    }
}

