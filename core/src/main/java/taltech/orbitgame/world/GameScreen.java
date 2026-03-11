package taltech.orbitgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GameScreen implements Screen {

    private SpriteBatch batch;
    private OrthographicCamera camera;

    private MapManager map;


    private final float speed = 120;

    private Player player;

    public GameScreen() {
        player = new Player();
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        map = new MapManager();

    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        map.render(batch);

        player.render(batch);

        batch.end();
    }

    private void update(float delta) {

        boolean moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            player.x += speed * delta;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            player.x -= speed * delta;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            player.y += speed * delta;
            moving = true;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            player.y -= speed * delta;
            moving = true;
        }

        player.update(delta, moving);
    }

    @Override
    public void dispose() {
        batch.dispose();
        map.dispose();
        player.dispose();
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
