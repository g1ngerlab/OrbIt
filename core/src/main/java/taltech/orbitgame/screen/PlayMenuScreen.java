package taltech.orbitgame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.auth.AuthState;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

import java.util.HashMap;
import java.util.Map;

public class PlayMenuScreen implements Screen {
    private final Game game;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private BitmapFont font;
    private Label statusLabel;
    private boolean connecting = false;
    private LobbyInfoClient lobbyInfoClient;
    private Map<Integer, TextButton> lobbyButtons;
    private float timeSinceLastUpdate = 0;
    private static final float UPDATE_INTERVAL = 2.0f; // Update every 2 seconds
    private boolean initialRequestSent = false;

    public PlayMenuScreen(Game game) {
        this(game, null);
    }

    public PlayMenuScreen(Game game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        background = new Texture(Gdx.files.internal("LobbyBg.png"));
        font = FontManager.getMenuFont();

        lobbyButtons = new HashMap<>();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Select Lobby", new Label.LabelStyle(FontManager.getMenuFontLarge(), null));
        table.add(titleLabel).padTop(30).row();

        // Create status label (fixes null exception)
        statusLabel = new Label("", new Label.LabelStyle(font, null));
        table.add(statusLabel).padBottom(20).row();

        // Create lobby buttons with initial player count
        for (int i = 1; i <= 2; i++) {
            TextButton lobbyButton = createLobbyButton("Lobby " + i + " - Players: ...", i);
            lobbyButtons.put(i, lobbyButton);
            table.add(lobbyButton).padBottom(20).size(500, 200).row();
        }

        TextButton backButton = createBackButton();
        table.add(backButton).padBottom(20).size(500, 200).row();

        // Use existing lobby info client or create new one
        if (lobbyInfoClient == null) {
            System.out.println("[PlayMenuScreen] Creating new LobbyInfoClient");
            lobbyInfoClient = new LobbyInfoClient();
        } else {
            System.out.println("[PlayMenuScreen] Using existing LobbyInfoClient");
        }

        lobbyInfoClient.setListener(lobbyPlayerCounts -> {
            // Update UI on main thread
            Gdx.app.postRunnable(() -> {
                System.out.println("[PlayMenuScreen] Received lobby player counts: " + lobbyPlayerCounts);
                System.out.println("[PlayMenuScreen] HashMap is null: " + (lobbyPlayerCounts == null));
                System.out.println("[PlayMenuScreen] HashMap is empty: " + (lobbyPlayerCounts != null && lobbyPlayerCounts.isEmpty()));
                System.out.println("[PlayMenuScreen] lobbyButtons map size: " + lobbyButtons.size());

                // Update all lobby buttons with player counts
                for (int lobbyId = 1; lobbyId <= 2; lobbyId++) {
                    TextButton button = lobbyButtons.get(lobbyId);
                    if (button != null) {
                        int playerCount = lobbyPlayerCounts != null ? lobbyPlayerCounts.getOrDefault(lobbyId, 0) : 0;
                        String oldText = button.getText().toString();
                        String newText = "Lobby " + lobbyId + " - Players: " + playerCount;
                        button.setText(newText);
                        System.out.println("[PlayMenuScreen] Updated button " + lobbyId + " from '" + oldText + "' to '" + newText + "'");
                    } else {
                        System.out.println("[PlayMenuScreen] ERROR: Button " + lobbyId + " is null!");
                    }
                }
            });
        });

        System.out.println("[PlayMenuScreen] Connecting to server for lobby info...");
        lobbyInfoClient.connect();
    }

    private TextButton createLobbyButton(String text, final int lobbyId) {
        Texture buttonTexture = new Texture("Buttons/button.png");
        Texture buttonPressedTexture = new Texture("Buttons/buttonPressed.png");

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonTexture);
        style.down = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonPressedTexture);

        TextButton button = new TextButton(text, style);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                connectToLobby(lobbyId);
            }
        });

        return button;
    }

    private void connectToLobby(int lobbyId) {
        if (connecting) {
            System.out.println("[PlayMenuScreen] Already connecting, ignoring click");
            return;
        }

        connecting = true;
        statusLabel.setText("Connecting to Lobby " + lobbyId + "...");
        System.out.println("[PlayMenuScreen] Connecting to Lobby " + lobbyId);

        GameClient gameClient = new GameClient(lobbyId, AuthState.getCurrentUsername());

        // Create GameScreen first so listener is attached before any server packets arrive.
        game.setScreen(new GameScreen((Main) game, gameClient));
        gameClient.connect();
    }

    private TextButton createBackButton() {
        Texture buttonTexture = new Texture("Buttons/button.png");
        Texture buttonPressedTexture = new Texture("Buttons/buttonPressed.png");

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonTexture);
        style.down = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonPressedTexture);

        TextButton button = new TextButton("Back", style);

        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen((Main) game));
            }
        });

        return button;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
        batch.end();

        // Send initial request after connection is established
        if (!initialRequestSent && lobbyInfoClient != null && lobbyInfoClient.isConnected()) {
            System.out.println("[PlayMenuScreen] Sending initial lobby info request");
            lobbyInfoClient.requestLobbyInfo();
            initialRequestSent = true;
            timeSinceLastUpdate = 0;
        }

        // Periodically request lobby info updates
        timeSinceLastUpdate += delta;
        if (timeSinceLastUpdate >= UPDATE_INTERVAL && lobbyInfoClient != null && lobbyInfoClient.isConnected()) {
            System.out.println("[PlayMenuScreen] Requesting lobby info update");
            lobbyInfoClient.requestLobbyInfo();
            timeSinceLastUpdate = 0;
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        background.dispose();
        if (lobbyInfoClient != null) {
            lobbyInfoClient.disconnect();
        }
    }
}
