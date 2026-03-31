package taltech.orbitgame.item;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry for item definitions.
 */
public final class ItemRegistry {
    private static final Map<Integer, ItemDefinition> BY_ID = new HashMap<>();

    static {
        register(new OrbItemDefinition());
    }

    private ItemRegistry() {
    }

    public static void register(ItemDefinition definition) {
        if (definition == null) {
            return;
        }
        BY_ID.put(definition.getTypeId(), definition);
    }

    public static ItemDefinition get(int itemTypeId) {
        return BY_ID.get(itemTypeId);
    }

    public static int getMaxStack(int itemTypeId) {
        ItemDefinition definition = get(itemTypeId);
        return definition == null ? 1 : definition.getMaxStack();
    }

    public static boolean isDroppableInWorld(int itemTypeId) {
        ItemDefinition definition = get(itemTypeId);
        return definition != null && definition.isDroppableInWorld();
    }

    public static Collection<ItemDefinition> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}


