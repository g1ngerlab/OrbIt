package taltech.orbitgame.player;

public final class PlayerProfile {
    private static int selectedCharacterId = CharacterType.MATH.getId();

    private PlayerProfile() {
    }

    public static int getSelectedCharacterId() {
        return selectedCharacterId;
    }

    public static void setSelectedCharacterId(int selectedCharacterId) {
        PlayerProfile.selectedCharacterId = CharacterType.fromId(selectedCharacterId).getId();
    }
}
