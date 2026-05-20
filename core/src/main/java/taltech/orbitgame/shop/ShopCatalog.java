package taltech.orbitgame.shop;

import taltech.orbitgame.item.ItemKind;

/**
 * Shared collectible shop catalog.
 */
public final class ShopCatalog {
    public static final int GOLDEN_FROG_PRICE_ORB = 100;
    public static final Item[] ITEMS = {
        new Item(ItemKind.WL, "WL", "loot/wl10.png", 0),
        new Item(ItemKind.DL, "DL", "loot/dl100.png", 0),
        new Item(ItemKind.BGL, "BGL", "loot/bgl1000.png", 0),
        new Item(ItemKind.VBUCK, "VBuck", "loot/vbuck322.png", 0),
        new Item(ItemKind.KANES, "Kanes", "loot/kanes333.png", 0),
        new Item(ItemKind.PAPAGOI, "Papagoi", "loot/papagoi399.png", 0),
        new Item(ItemKind.HAZBIK, "Hazbik", "loot/hazbik777.png", 0),
        new Item(ItemKind.GOLDEN_FROG, "Golden Frog", "loot/golden_frog.png", GOLDEN_FROG_PRICE_ORB),
        new Item(ItemKind.SHREX, "Shrex", "loot/shrex400.png", 0),
        new Item(ItemKind.BADABUM, "Badabum", "loot/badabum500.png", 0),
        new Item(ItemKind.STICKERS, "Stickers", "loot/stickers2014.png", 0),
        new Item(ItemKind.KOHUKE, "Kohuke", "loot/kohuke10000.png", 0)
    };

    private ShopCatalog() {
    }

    public static final class Item {
        private final ItemKind kind;
        private final String displayName;
        private final String iconPath;
        private final int priceOrb;

        private Item(ItemKind kind, String displayName, String iconPath, int fallbackPriceOrb) {
            this.kind = kind;
            this.displayName = displayName;
            this.iconPath = iconPath;
            this.priceOrb = parsePriceFromFilename(iconPath, fallbackPriceOrb);
        }

        public ItemKind getKind() {
            return kind;
        }

        public String getCode() {
            return kind.getCode();
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getIconPath() {
            return iconPath;
        }

        public int getPriceOrb() {
            return priceOrb;
        }

        private static int parsePriceFromFilename(String path, int fallbackPriceOrb) {
            if (path == null) {
                return Math.max(1, fallbackPriceOrb);
            }

            String filename = path;
            int slashIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
            if (slashIndex >= 0) {
                filename = filename.substring(slashIndex + 1);
            }
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                filename = filename.substring(0, dotIndex);
            }

            int digitStart = filename.length();
            while (digitStart > 0 && Character.isDigit(filename.charAt(digitStart - 1))) {
                digitStart--;
            }
            if (digitStart == filename.length()) {
                return Math.max(1, fallbackPriceOrb);
            }
            try {
                return Math.max(1, Integer.parseInt(filename.substring(digitStart)));
            } catch (NumberFormatException e) {
                return Math.max(1, fallbackPriceOrb);
            }
        }
    }
}
