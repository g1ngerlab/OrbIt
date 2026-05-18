package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import taltech.orbitgame.network.Network;

import java.util.Map;

public class CorpseRenderer {
    private static final float PLAYER_CORPSE_SCALE = 2.2f;

    private final Texture playerTexture;
    private final Texture robotTexture;
    private final TextureRegion playerFrame;
    private final TextureRegion robotFrame;
    private final Texture bossTexture;
    private final TextureRegion bossFrame;

    public CorpseRenderer() {
        this.playerTexture = new Texture(Gdx.files.internal("player/without_run.png"));
        this.robotTexture = new Texture(Gdx.files.internal("npc/robotbutler.png"));

        // Use a single frame from each spritesheet for static corpses.
        this.playerFrame = new TextureRegion(
            playerTexture,
            0,
            0,
            Math.max(1, playerTexture.getWidth() / 10),
            playerTexture.getHeight()
        );

        this.robotFrame = new TextureRegion(
            robotTexture,
            0,
            0,
            Math.max(1, robotTexture.getHeight()),
            robotTexture.getHeight()
        );

        this.bossTexture = new Texture(Gdx.files.internal("npc/BossN/boss_death.png"));
        int bossFrameSize = bossTexture.getHeight();
        int bossFrameCount = Math.max(1, bossTexture.getWidth() / bossFrameSize);
        this.bossFrame = new TextureRegion(
            bossTexture,
            (bossFrameCount - 1) * bossFrameSize,
            0,
            bossFrameSize,
            bossFrameSize
        );
    }

    public void render(SpriteBatch batch, Map<Integer, Network.CorpseState> corpses) {
        for (Network.CorpseState corpse : corpses.values()) {
            TextureRegion region = getTextureForCorpse(corpse.kind);

            float scale = corpse.kind == Network.CorpseState.KIND_PLAYER
                ? PLAYER_CORPSE_SCALE
                : 1f;
            float drawWidth = corpse.width * scale;
            float drawHeight = corpse.height * scale;
            float drawX = corpse.x - drawWidth * 0.5f;
            float drawY = corpse.y - drawHeight * 0.5f;

            batch.draw(
                region,
                drawX,
                drawY,
                drawWidth * 0.5f,
                drawHeight * 0.5f,
                drawWidth,
                drawHeight,
                1f,
                1f,
                corpse.rotationDeg
            );
        }
    }

    private TextureRegion getTextureForCorpse(int kind) {
        if (kind == Network.CorpseState.KIND_ROBOT_BOSS) {
            return bossFrame;
        }
        if (kind == Network.CorpseState.KIND_ROBOT_BUTLER) {
            return robotFrame;
        }
        return playerFrame;
    }

    public void dispose() {
        playerTexture.dispose();
        robotTexture.dispose();
        bossTexture.dispose();
    }
}

