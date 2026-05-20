package taltech.orbitgame.item;

/**
 * Persistent cosmetic item bought for collection only.
 */
public final class CollectibleItemDefinition extends ItemDefinition {
    public CollectibleItemDefinition(ItemKind kind, String displayName, String iconPath) {
        super(kind, ItemCategory.GENERIC, displayName, iconPath, 100, true);
    }
}
