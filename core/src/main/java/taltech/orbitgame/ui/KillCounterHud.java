package taltech.orbitgame.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders local player kill counters in the HUD.
 */
public class KillCounterHud {
    private static final float PADDING = 16f;
    private static final float TOP_OFFSET = 42f;
    private static final float LINE_GAP = 6f;
    private static final float FONT_SCALE = 0.9f;

    private int robotKills;
    private int playerKills;
    private final Set<Integer> countedNpcIds = new HashSet<>();
    private final Set<Integer> countedPlayerIds = new HashSet<>();
    private final GlyphLayout layout = new GlyphLayout();

    public void onNpcKilled(int npcId) {
        if (countedNpcIds.add(npcId)) {
            robotKills++;
        }
    }

    public void onPlayerKilled(int playerId) {
        if (countedPlayerIds.add(playerId)) {
            playerKills++;
        }
    }

    public void reset() {
        robotKills = 0;
        playerKills = 0;
        countedNpcIds.clear();
        countedPlayerIds.clear();
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (batch == null || font == null) {
            return;
        }

        String robotText = "Robots: " + robotKills;
        String playerText = "Players: " + playerKills;

        batch.begin();
        font.getData().setScale(FONT_SCALE);
        font.setColor(Color.WHITE);

        float lineHeight = font.getLineHeight();
        float yTop = Gdx.graphics.getHeight() - PADDING - TOP_OFFSET;

        layout.setText(font, robotText);
        float xRobot = Gdx.graphics.getWidth() - layout.width - PADDING;
        font.draw(batch, robotText, xRobot, yTop);

        layout.setText(font, playerText);
        float xPlayer = Gdx.graphics.getWidth() - layout.width - PADDING;
        float yPlayer = yTop - lineHeight - LINE_GAP;
        font.draw(batch, playerText, xPlayer, yPlayer);

        font.getData().setScale(1f);
        font.setColor(Color.WHITE);
        batch.end();
    }
}

