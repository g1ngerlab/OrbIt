package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import taltech.orbitgame.rendering.BackgroundRenderer;

public class IntroVideoScreen implements Screen {
    private static final int FRAME_COUNT = 253;
    private static final float FRAME_RATE = 24f;
    private static final float DURATION = FRAME_COUNT / FRAME_RATE;

    private final Runnable afterIntro;
    private final SpriteBatch batch;
    private final Texture[] frames;

    private Music audio;
    private float elapsedTime;
    private boolean completed;

    public IntroVideoScreen(Runnable afterIntro) {
        this.afterIntro = afterIntro;
        this.batch = new SpriteBatch();
        this.frames = new Texture[FRAME_COUNT];
    }

    @Override
    public void show() {
        for (int i = 0; i < FRAME_COUNT; i++) {
            frames[i] = new Texture(Gdx.files.internal(String.format("introvideo/frames/frame_%04d.jpg", i + 1)));
        }
        audio = Gdx.audio.newMusic(Gdx.files.internal("introvideo/intro.ogg"));
        audio.play();
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.justTouched()) {
            complete();
            return;
        }
        if (elapsedTime >= DURATION) {
            complete();
            return;
        }

        int frameIndex = Math.min((int) (elapsedTime * FRAME_RATE), FRAME_COUNT - 1);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawFit(batch, frames[frameIndex]);
        batch.end();
    }

    private void complete() {
        if (completed) {
            return;
        }
        completed = true;
        if (audio != null) {
            audio.stop();
        }
        dispose();
        afterIntro.run();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
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
        if (audio != null) {
            audio.dispose();
            audio = null;
        }
        for (int i = 0; i < frames.length; i++) {
            if (frames[i] != null) {
                frames[i].dispose();
                frames[i] = null;
            }
        }
    }
}
