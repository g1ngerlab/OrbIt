package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

public class IntroScreen implements Screen {
    private static final float DURATION_SECONDS = 4.0f;
    private static final float FADE_SECONDS = 0.8f;

    private final Runnable afterIntro;
    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont titleFont;
    private final BitmapFont subtitleFont;
    private final GlyphLayout layout;

    private Texture background;
    private Texture logo;
    private float elapsedTime;
    private boolean completed;

    public IntroScreen(Runnable afterIntro) {
        this.afterIntro = afterIntro;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.titleFont = FontManager.getMenuFontLarge();
        this.subtitleFont = FontManager.getMenuFont();
        this.layout = new GlyphLayout();
    }

    @Override
    public void show() {
        background = new Texture(Gdx.files.internal("LobbyBg.png"));
        logo = new Texture(Gdx.files.internal("MainMenuAssets/orbit.png"));
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.justTouched()) {
            complete();
            return;
        }
        if (elapsedTime >= DURATION_SECONDS) {
            complete();
            return;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float width = Gdx.graphics.getWidth();
        float height = Gdx.graphics.getHeight();
        float introAlpha = Math.min(1f, elapsedTime / FADE_SECONDS);
        float outroAlpha = Math.min(1f, (DURATION_SECONDS - elapsedTime) / FADE_SECONDS);
        float alpha = Math.max(0f, Math.min(introAlpha, outroAlpha));
        float pulse = 1f + 0.04f * (float) Math.sin(elapsedTime * 4f);

        batch.begin();
        batch.setColor(1f, 1f, 1f, alpha);
        BackgroundRenderer.drawCover(batch, background);

        float logoMaxWidth = width * 0.42f;
        float logoScale = logoMaxWidth / logo.getWidth() * pulse;
        float logoWidth = logo.getWidth() * logoScale;
        float logoHeight = logo.getHeight() * logoScale;
        float logoX = (width - logoWidth) / 2f;
        float logoY = height * 0.55f - logoHeight / 2f;
        batch.draw(logo, logoX, logoY, logoWidth, logoHeight);

        drawCentered(titleFont, "AAA Studios", height * 0.38f, alpha);
        drawCentered(subtitleFont, "ORBIT", height * 0.28f, alpha);
        batch.setColor(Color.WHITE);
        batch.end();

        float blackAlpha = 1f - alpha;
        if (blackAlpha > 0f) {
            drawBlackOverlay(blackAlpha);
        }
    }

    private void drawCentered(BitmapFont font, String text, float y, float alpha) {
        font.setColor(1f, 1f, 1f, alpha);
        layout.setText(font, text);
        float x = (Gdx.graphics.getWidth() - layout.width) / 2f;
        font.draw(batch, layout, x, y);
    }

    private void drawBlackOverlay(float alpha) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, Interpolation.fade.apply(alpha));
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void complete() {
        if (completed) {
            return;
        }
        completed = true;
        afterIntro.run();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        shapeRenderer.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
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
        if (background != null) {
            background.dispose();
        }
        if (logo != null) {
            logo.dispose();
        }
    }
}
