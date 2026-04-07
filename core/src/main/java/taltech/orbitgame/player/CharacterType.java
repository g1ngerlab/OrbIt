package taltech.orbitgame.player;

public enum CharacterType {
    MATH(
        1,
        "Math",
        "player/Math_character.png",
        "player/run_side.png",
        "player/run_up.png",
        "player/run_down.png",
        "player/without_run.png",
        7,
        6,
        8,
        10,
        0.07f,
        0.09f,
        null,
        0,
        0f,
        true,
        false,
        1f / 1.5f
    ),
    PUPU(
        2,
        "Pupu",
        "pupu/Monkey_pupu.png",
        "pupu/side_pupu.png",
        "pupu/back_pupu.png",
        "pupu/down_pupu.png",
        "pupu/MyPupu.png",
        11,
        5,
        6,
        14,
        0.07f,
        0.09f,
        "pupu/attack_pupu.png",
        8,
        0.06f,
        false,
        true,
        0.55f
    ),
    EBANIBUNNY(
        3,
        "EbaniBunny",
        "bunny/BUNNY_CHARACTER.png",
        "bunny/right_bunny.png",
        "bunny/back_bunny.png",
        "bunny/forward_bunny.png",
        "bunny/idle_bunny.png",
        6,
        6,
        7,
        8,
        0.07f,
        0.09f,
        "bunny/attack_bunny.png",
        11,
        0.06f,
        false,
        false,
        0.55f
    );

    private final int id;
    private final String displayName;
    private final String previewTexturePath;
    private final String runSideTexturePath;
    private final String runUpTexturePath;
    private final String runDownTexturePath;
    private final String idleTexturePath;
    private final int runSideFrames;
    private final int runUpFrames;
    private final int runDownFrames;
    private final int idleFrames;
    private final float runFrameTime;
    private final float idleFrameTime;
    private final String attackTexturePath;
    private final int attackFrames;
    private final float attackFrameTime;
    private final boolean baseFacingLeft;
    private final boolean attackFlipInverted;
    private final float drawScale;

    CharacterType(int id,
                  String displayName,
                  String previewTexturePath,
                  String runSideTexturePath,
                  String runUpTexturePath,
                  String runDownTexturePath,
                  String idleTexturePath,
                  int runSideFrames,
                  int runUpFrames,
                  int runDownFrames,
                  int idleFrames,
                  float runFrameTime,
                  float idleFrameTime,
                  String attackTexturePath,
                  int attackFrames,
                  float attackFrameTime,
                  boolean baseFacingLeft,
                  boolean attackFlipInverted,
                  float drawScale) {
        this.id = id;
        this.displayName = displayName;
        this.previewTexturePath = previewTexturePath;
        this.runSideTexturePath = runSideTexturePath;
        this.runUpTexturePath = runUpTexturePath;
        this.runDownTexturePath = runDownTexturePath;
        this.idleTexturePath = idleTexturePath;
        this.runSideFrames = runSideFrames;
        this.runUpFrames = runUpFrames;
        this.runDownFrames = runDownFrames;
        this.idleFrames = idleFrames;
        this.runFrameTime = runFrameTime;
        this.idleFrameTime = idleFrameTime;
        this.attackTexturePath = attackTexturePath;
        this.attackFrames = attackFrames;
        this.attackFrameTime = attackFrameTime;
        this.baseFacingLeft = baseFacingLeft;
        this.attackFlipInverted = attackFlipInverted;
        this.drawScale = drawScale;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPreviewTexturePath() {
        return previewTexturePath;
    }

    public String getRunSideTexturePath() {
        return runSideTexturePath;
    }

    public String getRunUpTexturePath() {
        return runUpTexturePath;
    }

    public String getRunDownTexturePath() {
        return runDownTexturePath;
    }

    public String getIdleTexturePath() {
        return idleTexturePath;
    }

    public int getRunSideFrames() {
        return runSideFrames;
    }

    public int getRunUpFrames() {
        return runUpFrames;
    }

    public int getRunDownFrames() {
        return runDownFrames;
    }

    public int getIdleFrames() {
        return idleFrames;
    }

    public float getRunFrameTime() {
        return runFrameTime;
    }

    public float getIdleFrameTime() {
        return idleFrameTime;
    }

    public String getAttackTexturePath() {
        return attackTexturePath;
    }

    public int getAttackFrames() {
        return attackFrames;
    }

    public float getAttackFrameTime() {
        return attackFrameTime;
    }

    public boolean isBaseFacingLeft() {
        return baseFacingLeft;
    }

    public boolean isAttackFlipInverted() {
        return attackFlipInverted;
    }

    public float getDrawScale() {
        return drawScale;
    }

    public boolean hasAttackAnimation() {
        return attackTexturePath != null && !attackTexturePath.isEmpty() && attackFrames > 0;
    }

    public static CharacterType fromId(int id) {
        for (CharacterType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return MATH;
    }
}
