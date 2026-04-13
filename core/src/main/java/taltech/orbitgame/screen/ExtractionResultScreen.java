package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import taltech.orbitgame.Main;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.audio.AudioManager;
import com.badlogic.gdx.utils.Align;

/**
 * Screen shown when player successfully extracts from a game session
 */
public class ExtractionResultScreen implements Screen {
    private final Main game;
    private final String playerName;
    private final int killCount;
    private final Runnable onContinue;

    private SpriteBatch batch;
    private BitmapFont titleFont;
    private BitmapFont statsFont;
    private GlyphLayout layout;
    private ShapeRenderer shapeRenderer;

    private float elapsedTime = 0f;
    private final float FADE_IN_DURATION = 0.5f;

    public ExtractionResultScreen(Main game, String playerName, int killCount, Runnable onContinue) {
        this.game = game;
        this.playerName = playerName;
        this.killCount = killCount;
        this.onContinue = onContinue;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        titleFont = FontManager.getPixelFont(32);
        statsFont = FontManager.getPixelFont(20);
        layout = new GlyphLayout();
        AudioManager.getInstance().stopCurrentMusic();
        AudioManager.getInstance().playRandomLobbyMusic();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        elapsedTime += delta;

        // Draw semi-transparent overlay
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();

        batch.begin();

        // Fade in effect
        float alpha = Math.min(1f, elapsedTime / FADE_IN_DURATION);

        // Draw title
        titleFont.setColor(0.2f, 0.8f, 0.2f, alpha); // Green
        String title = "EXTRACTION SUCCESSFUL";
        layout.setText(titleFont, title);
        float titleX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float titleY = Gdx.graphics.getHeight() * 0.75f;
        titleFont.draw(batch, title, titleX, titleY);

        // Draw player name
        statsFont.setColor(1f, 1f, 1f, alpha);
        String playerNameText = playerName + " extracted safely";
        layout.setText(statsFont, playerNameText);
        float playerX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float playerY = Gdx.graphics.getHeight() * 0.6f;
        statsFont.draw(batch, playerNameText, playerX, playerY);

        // Draw kill count
        String killText = "Kills: " + killCount;
        layout.setText(statsFont, killText);
        float killX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float killY = Gdx.graphics.getHeight() * 0.5f;
        statsFont.draw(batch, killText, killX, killY);

        // Draw continue prompt
        statsFont.setColor(1f, 1f, 1f, (float)Math.sin(elapsedTime * 2f) * 0.3f + 0.7f);
        String continueText = "Press SPACE to continue";
        layout.setText(statsFont, continueText);
        float continueX = (Gdx.graphics.getWidth() - layout.width) * 0.5f;
        float continueY = Gdx.graphics.getHeight() * 0.2f;
        statsFont.draw(batch, continueText, continueX, continueY);

        batch.end();

        // Handle input
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched()) {
            proceed();
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (titleFont != null) titleFont.dispose();
        if (statsFont != null) statsFont.dispose();
    }

    private void proceed() {
        if (onContinue != null) {
            onContinue.run();
        }
    }
}
