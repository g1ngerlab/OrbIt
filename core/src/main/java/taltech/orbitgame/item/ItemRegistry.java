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
        register(new taltech.orbitgame.item.StarterShotgunItemDefinition());
        register(new GoldenFrogItemDefinition());
        register(new CollectibleItemDefinition(ItemKind.WL, "WL", "loot/wl10.png"));
        register(new CollectibleItemDefinition(ItemKind.DL, "DL", "loot/dl100.png"));
        register(new CollectibleItemDefinition(ItemKind.BGL, "BGL", "loot/bgl1000.png"));
        register(new CollectibleItemDefinition(ItemKind.VBUCK, "VBuck", "loot/vbuck322.png"));
        register(new CollectibleItemDefinition(ItemKind.KANES, "Kanes", "loot/kanes333.png"));
        register(new CollectibleItemDefinition(ItemKind.PAPAGOI, "Papagoi", "loot/papagoi399.png"));
        register(new CollectibleItemDefinition(ItemKind.HAZBIK, "Hazbik", "loot/hazbik777.png"));
        register(new CollectibleItemDefinition(ItemKind.SHREX, "Shrex", "loot/shrex400.png"));
        register(new CollectibleItemDefinition(ItemKind.BADABUM, "Badabum", "loot/badabum500.png"));
        register(new CollectibleItemDefinition(ItemKind.STICKERS, "Stickers", "loot/stickers2014.png"));
        register(new CollectibleItemDefinition(ItemKind.KOHUKE, "Kohuke", "loot/kohuke10000.png"));
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

    public static boolean isWeapon(int itemTypeId) {
        ItemDefinition definition = get(itemTypeId);
        return definition != null && definition.isWeapon();
    }

    public static Collection<ItemDefinition> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}


