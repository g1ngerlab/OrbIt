package taltech.orbitgame.item;

/**
 * Stable item ids used in network payloads and saves.
 */
public enum ItemKind {
    NONE(0, "none"),
    ORB(1, "orb");

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
}

