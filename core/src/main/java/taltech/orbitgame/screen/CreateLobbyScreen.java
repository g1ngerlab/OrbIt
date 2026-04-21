package taltech.orbitgame.screen;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.auth.AuthState;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.audio.AudioManager;

/**
 * Screen for creating a new lobby with a custom name
 */
public class CreateLobbyScreen implements Screen {
    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture dimOverlay;
    private Texture labelBgTexture;
    private Texture textFieldBg;
    private Texture cursorTexture;
    private TextField nameField;
    private Label statusLabel;
    private boolean creatingLobby = false;
    private String pendingLobbyName = null;

    public CreateLobbyScreen(Game game, LobbyInfoClient lobbyInfoClient) {
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
        Pixmap dimPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPixmap.setColor(0f, 0f, 0f, 0.25f);
        dimPixmap.fill();
        dimOverlay = new Texture(dimPixmap);
        dimPixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Create Lobby", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        table.add(titleLabel).padTop(30).padBottom(50).row();

        // Lobby name input label with readable background
        Pixmap labelBgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        labelBgPixmap.setColor(0f, 0f, 0f, 0.55f);
        labelBgPixmap.fill();
        labelBgTexture = new Texture(labelBgPixmap);
        labelBgPixmap.dispose();

        Label.LabelStyle nameStyle = new Label.LabelStyle(FontManager.getMenuFont(), Color.WHITE);
        nameStyle.background = new TextureRegionDrawable(labelBgTexture);
        Label nameLabel = new Label("Lobby Name (max 12 characters):", nameStyle);
        table.add(nameLabel).padBottom(10).row();

        // Create simple background for text field
        Pixmap bgPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bgPixmap.setColor(0.2f, 0.2f, 0.3f, 0.9f);
        bgPixmap.fill();
        textFieldBg = new Texture(bgPixmap);
        bgPixmap.dispose();

        // Create cursor (thin white line)
        Pixmap cursorPixmap = new Pixmap(2, 1, Pixmap.Format.RGBA8888);
        cursorPixmap.setColor(Color.WHITE);
        cursorPixmap.fill();
        cursorTexture = new Texture(cursorPixmap);
        cursorPixmap.dispose();

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = FontManager.getMenuFont();
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = new TextureRegionDrawable(textFieldBg);
        textFieldStyle.cursor = new TextureRegionDrawable(cursorTexture);
        textFieldStyle.selection = new TextureRegionDrawable(textFieldBg);

        nameField = new TextField("", textFieldStyle);
        nameField.setMaxLength(12);
        nameField.setMessageText("Enter name...");
        table.add(nameField).width(400).height(60).padBottom(30).row();

        // Status label
        statusLabel = new Label("", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        table.add(statusLabel).padBottom(20).row();

        // Create button
        TextButton createButton = createButton("Create");
        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                createLobby();
            }
        });
        table.add(createButton).padBottom(20).size(400, 100).row();

        // Back button
        TextButton backButton = createButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new LobbyChoiceScreen(game, lobbyInfoClient));
            }
        });
        table.add(backButton).size(400, 100).row();

        // Set up lobby creation callback
        if (lobbyInfoClient != null) {
            lobbyInfoClient.setLobbyCreatedListener(new LobbyInfoClient.LobbyCreatedListener() {
                @Override
                public void onLobbyCreated(int lobbyId, String lobbyName) {
                    Gdx.app.postRunnable(() -> {
                        System.out.println("[CreateLobbyScreen] Lobby created: " + lobbyId + " - " + lobbyName);
                        game.setScreen(new ChooseLoadoutScreen(
                            game,
                            lobbyInfoClient,
                            lobbyId,
                            new CreateLobbyScreen(game, lobbyInfoClient)
                        ));
                    });
                }

                @Override
                public void onLobbyCreateFailed(String reason) {
                    Gdx.app.postRunnable(() -> {
                        statusLabel.setText(reason);
                        creatingLobby = false;
                    });
                }
            });
        }
    }

    private void createLobby() {
        if (creatingLobby) {
            return;
        }

        String lobbyName = nameField.getText().trim();
        if (lobbyName.isEmpty()) {
            statusLabel.setText("Please enter a lobby name!");
            return;
        }

        if (lobbyInfoClient == null) {
            statusLabel.setText("Not connected to server!");
            return;
        }

        creatingLobby = true;

        if (lobbyInfoClient.isConnected()) {
            statusLabel.setText("Creating lobby...");
            lobbyInfoClient.createLobby(lobbyName);
        } else {
            // Not connected yet — wait for connection in render(), then send
            statusLabel.setText("Connecting to server...");
            pendingLobbyName = lobbyName;
        }
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
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(dimOverlay, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        // If we have a pending lobby to create, wait for connection then send
        if (pendingLobbyName != null && lobbyInfoClient != null && lobbyInfoClient.isConnected()) {
            statusLabel.setText("Creating lobby...");
            lobbyInfoClient.createLobby(pendingLobbyName);
            pendingLobbyName = null;
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
            lobbyInfoClient.setLobbyCreatedListener(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        background.dispose();
        if (dimOverlay != null) {
            dimOverlay.dispose();
        }
        if (labelBgTexture != null) {
            labelBgTexture.dispose();
        }
        if (textFieldBg != null) {
            textFieldBg.dispose();
        }
        if (cursorTexture != null) {
            cursorTexture.dispose();
        }
    }
}
