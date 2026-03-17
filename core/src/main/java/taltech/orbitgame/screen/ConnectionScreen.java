package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.util.FontManager;

/**
 * Initial splash screen that shows while connecting to the server.
 * Connects once and waits until connected before proceeding.
 */
public class ConnectionScreen implements Screen {

    private final Main game;
    private final SpriteBatch batch;
    private final BitmapFont titleFont;
    private final BitmapFont statusFont;
    private LobbyInfoClient lobbyInfoClient;

    private float elapsedTime = 0;
    private static final float MIN_DISPLAY_TIME = 1.5f;

    public ConnectionScreen(Main game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.titleFont = FontManager.getMenuFont();
        this.statusFont = FontManager.getMenuFont();
    }

    @Override
    public void show() {
        System.out.println("[ConnectionScreen] Starting connection to server...");
        lobbyInfoClient = new LobbyInfoClient();
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // Draw "AAA Studios"
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(Color.WHITE);
        String title = "AAA Studios";
        float titleWidth = titleFont.draw(batch, title, 0, 0).width;
        float titleX = (Gdx.graphics.getWidth() - titleWidth) / 2;
        float titleY = Gdx.graphics.getHeight() * 0.6f;
        titleFont.draw(batch, title, titleX, titleY);

        // Draw animated "Connecting to server..."
        statusFont.getData().setScale(1.2f);
        statusFont.setColor(Color.LIGHT_GRAY);
        int dots = (int) (elapsedTime * 2) % 4;
        String status = "Connecting to server" + ".".repeat(dots);
        float statusWidth = statusFont.draw(batch, status, 0, 0).width;
        float statusX = (Gdx.graphics.getWidth() - statusWidth) / 2;
        float statusY = Gdx.graphics.getHeight() * 0.4f;
        statusFont.draw(batch, status, statusX, statusY);

        batch.end();

        // Only proceed when actually connected and minimum display time passed
        if (elapsedTime >= MIN_DISPLAY_TIME && lobbyInfoClient != null && lobbyInfoClient.isConnected()) {
            System.out.println("[ConnectionScreen] Connection successful!");
            game.setScreen(new MainMenuScreen(game, lobbyInfoClient));
        }
    }

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
    }
}
