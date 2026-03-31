package taltech.orbitgame.item;

/**
 * Current concrete item implementation used by orb drops.
 */
public final class OrbItemDefinition extends ItemDefinition {
    public OrbItemDefinition() {
        super(ItemKind.ORB, "Orb", "loot/orb_v1.png", 16, true);
    }
}


