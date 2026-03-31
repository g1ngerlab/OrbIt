package taltech.orbitgame.item;

/**
 * Base definition for all item types that can be stored and dropped.
 */
public abstract class ItemDefinition {
    private final ItemKind kind;
    private final String displayName;
    private final String iconPath;
    private final int maxStack;
    private final boolean droppableInWorld;

    protected ItemDefinition(ItemKind kind,
                             String displayName,
                             String iconPath,
                             int maxStack,
                             boolean droppableInWorld) {
        this.kind = kind;
        this.displayName = displayName;
        this.iconPath = iconPath;
        this.maxStack = Math.max(1, maxStack);
        this.droppableInWorld = droppableInWorld;
    }

    public ItemKind getKind() {
        return kind;
    }

    public int getTypeId() {
        return kind.getId();
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconPath() {
        return iconPath;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public boolean isDroppableInWorld() {
        return droppableInWorld;
    }
}


