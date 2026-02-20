package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.InputHandler;

/**
 * Main game screen that handles input and renders the game
 */
public class GameScreen implements Screen {
    private InputHandler inputHandler;
    private OrthographicCamera camera;
    private GameClient gameClient;

    public GameScreen(GameClient gameClient) {
        this.gameClient = gameClient;
        this.inputHandler = new InputHandler();
        Gdx.input.setInputProcessor(this.inputHandler);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);

        System.out.println("[GameScreen] Screen initialized, waiting for connection...");
    }

    @Override
    public void show() {
        System.out.println("[GameScreen] Screen shown");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // Send input state to server
        if (gameClient != null) {
            gameClient.sendInput(inputHandler.getState());
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
    }
}
