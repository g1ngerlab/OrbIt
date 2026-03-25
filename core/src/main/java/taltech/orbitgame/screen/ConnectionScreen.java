package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.Texture;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

/**
 * Initial splash screen that shows while connecting to the server.
 * Retries connection in background until connected, then proceeds.
 */
public class ConnectionScreen implements Screen {

    private final Main game;
    private final SpriteBatch batch;
    private final BitmapFont titleFont;
    private final BitmapFont statusFont;
    private LobbyInfoClient lobbyInfoClient;
    private Texture background;

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
        background = new Texture(Gdx.files.internal("LobbyBg.png"));
    }

    @Override
    public void render(float delta) {
        elapsedTime += delta;

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);

        // Draw "AAA Studios"
        titleFont.getData().setScale(2.5f);
        titleFont.setColor(Color.WHITE);
        String title = "AAA Studios";
        float titleWidth = titleFont.draw(batch, title, 0, 0).width;
        float titleX = (Gdx.graphics.getWidth() - titleWidth) / 2;
        float titleY = Gdx.graphics.getHeight() * 0.6f;
        titleFont.draw(batch, title, titleX, titleY);

        // Draw animated connection status based on current retry state.
        statusFont.getData().setScale(1.2f);
        statusFont.setColor(Color.LIGHT_GRAY);
        int dots = (int) (elapsedTime * 2) % 4;
        String baseStatus = lobbyInfoClient == null ? "Connecting to server" : lobbyInfoClient.getConnectionStatusMessage();
        String status = baseStatus + buildDots(dots);
        float statusWidth = statusFont.draw(batch, status, 0, 0).width;
        float statusX = (Gdx.graphics.getWidth() - statusWidth) / 2;
        float statusY = Gdx.graphics.getHeight() * 0.4f;
        statusFont.draw(batch, status, statusX, statusY);

        batch.end();

        // Only proceed when actually connected and minimum display time passed
        if (elapsedTime >= MIN_DISPLAY_TIME && lobbyInfoClient != null && lobbyInfoClient.isConnected()) {
            System.out.println("[ConnectionScreen] Connection successful!");
            game.setScreen(new AuthScreen(game, lobbyInfoClient));
        }
    }

    private String buildDots(int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append('.');
        }
        return builder.toString();
    }

    @Override
    public void resize(int width, int height) {
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void pause() {
        // No-op: this screen has no pause-specific state.
    }

    @Override
    public void resume() {
        // No-op: this screen restores itself automatically.
    }

    @Override
    public void hide() {
        // No-op: resources are released in dispose().
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (background != null) {
            background.dispose();
        }
    }
}
