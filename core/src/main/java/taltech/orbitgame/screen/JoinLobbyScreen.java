package taltech.orbitgame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.auth.AuthState;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.audio.AudioManager;

import java.util.List;

/**
 * Screen for joining an existing lobby
 */
public class JoinLobbyScreen implements Screen {
    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Table lobbiesTable;
    private Label statusLabel;
    private boolean connecting = false;
    private boolean requestSent = false;
    private float timeSinceLastUpdate = 0;
    private static final float UPDATE_INTERVAL = 2.0f;

    public JoinLobbyScreen(Game game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        // Start lobby music
        AudioManager.getInstance().playRandomLobbyMusic();

        background = new Texture(Gdx.files.internal("LobbyBg.png"));

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        Label titleLabel = new Label("Join Lobby", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        mainTable.add(titleLabel).padTop(30).padBottom(30).row();

        // Status label
        statusLabel = new Label("Loading lobbies...", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        mainTable.add(statusLabel).padBottom(20).row();

        // Lobbies table with scroll pane
        lobbiesTable = new Table();
        ScrollPane scrollPane = new ScrollPane(lobbiesTable);
        scrollPane.setFadeScrollBars(false);
        mainTable.add(scrollPane).width(600).height(300).padBottom(20).row();

        // Back button
        TextButton backButton = createButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LobbyChoiceScreen(game, lobbyInfoClient));
            }
        });
        mainTable.add(backButton).size(400, 100).row();

        // Set up callback for lobby list
        if (lobbyInfoClient != null) {
            lobbyInfoClient.setAllLobbiesListener(lobbies ->
                Gdx.app.postRunnable(() -> updateLobbyList(lobbies))
            );

            // Request immediately if connected, otherwise wait for connection in render()
            if (lobbyInfoClient.isConnected()) {
                requestSent = true;
                lobbyInfoClient.requestAllLobbies();
            }
        }
    }

    private void updateLobbyList(List<Network.LobbyListItem> lobbies) {
        lobbiesTable.clear();

        if (lobbies == null || lobbies.isEmpty()) {
            statusLabel.setText("No lobbies available. Create one!");
            Label emptyLabel = new Label("No lobbies found", new Label.LabelStyle(FontManager.getMenuFont(), Color.GRAY));
            lobbiesTable.add(emptyLabel).pad(20);
        } else {
            statusLabel.setText("Select a lobby to join:");
            for (Network.LobbyListItem lobby : lobbies) {
                String buttonText = lobby.lobbyName + " (" + lobby.playerCount + " players)";
                TextButton lobbyButton = createButton(buttonText);

                final int lobbyId = lobby.lobbyId;
                lobbyButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        if (!connecting) {
                            joinLobby(lobbyId);
                        }
                    }
                });

                lobbiesTable.add(lobbyButton).width(550).height(80).pad(5).row();
            }
        }
    }

    private void joinLobby(int lobbyId) {
        if (connecting) {
            return;
        }

        connecting = true;
        statusLabel.setText("Joining lobby...");

        GameClient gameClient = new GameClient(lobbyId, AuthState.getCurrentUsername());
        // Pass lobbyInfoClient to preserve connection when exiting game
        game.setScreen(new GameScreen((Main) game, gameClient, lobbyInfoClient));
        gameClient.connect();
    }

    private TextButton createButton(String text) {
        Texture buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        Texture buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        BitmapFont font = FontManager.getMenuFont();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.up = new TextureRegionDrawable(buttonTexture);
        style.down = new TextureRegionDrawable(buttonPressedTexture);

        return new TextButton(text, style);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
        batch.end();

        if (lobbyInfoClient != null) {
            if (!requestSent) {
                // Keep waiting until the LobbyInfoClient connects, then send request
                if (lobbyInfoClient.isConnected()) {
                    requestSent = true;
                    statusLabel.setText("Loading lobbies...");
                    lobbyInfoClient.requestAllLobbies();
                }
                // else: still connecting, statusLabel already says "Loading lobbies..."
            } else {
                // Periodically refresh lobby list
                timeSinceLastUpdate += delta;
                if (timeSinceLastUpdate >= UPDATE_INTERVAL && !connecting) {
                    timeSinceLastUpdate = 0;
                    lobbyInfoClient.requestAllLobbies();
                }
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        if (lobbyInfoClient != null) {
            lobbyInfoClient.setAllLobbiesListener(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        background.dispose();
    }
}
