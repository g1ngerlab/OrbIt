package taltech.orbitgame.item;

/**
 * Stable item ids used in network payloads and saves.
 */
public enum ItemKind {
    NONE(0, "none"),
    ORB(1, "orb"),
    STARTER_SHOTGUN(2, "starter_shotgun"),
    GOLDEN_FROG(3, "golden_frog"),
    WL(4, "wl"),
    DL(5, "dl"),
    BGL(6, "bgl"),
    VBUCK(7, "vbuck"),
    KANES(8, "kanes"),
    PAPAGOI(9, "papagoi"),
    HAZBIK(10, "hazbik"),
    SHREX(11, "shrex"),
    BADABUM(12, "badabum"),
    STICKERS(13, "stickers"),
    KOHUKE(14, "kohuke");

    private final int id;
    private final String code;

    ItemKind(int id, String code) {
        this.id = id;
        this.code = code;
    }

    public int getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public static ItemKind fromId(int id) {
        for (ItemKind kind : values()) {
            if (kind.id == id) {
                return kind;
            }
        }
        return NONE;
    }

    public static ItemKind fromCode(String code) {
        if (code == null) {
            return NONE;
        }
        String normalized = code.trim();
        for (ItemKind kind : values()) {
            if (kind.code.equalsIgnoreCase(normalized)) {
                return kind;
            }
        }
        return NONE;
    }
}

