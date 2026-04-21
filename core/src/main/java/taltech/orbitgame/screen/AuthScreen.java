package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import taltech.orbitgame.auth.AuthSession;
import taltech.orbitgame.auth.SupabaseAuthService;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;
import taltech.orbitgame.audio.AudioManager;

/**
 * Login/registration screen shown before the main menu.
 */
public class AuthScreen implements Screen {
    private enum AuthView {
        LOGIN,
        REGISTER
    }

    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private final SupabaseAuthService authService;

    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture dimOverlay;
    private Texture textFieldBg;
    private Texture cursorTexture;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;

    private Table rootTable;
    private TextField.TextFieldStyle fieldStyle;

    private TextField loginField;
    private TextField emailField;
    private TextField nicknameField;
    private TextField passwordField;
    private Label statusLabel;

    private boolean requestInFlight;
    private AuthView currentView = AuthView.LOGIN;

    public AuthScreen(Main game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
        this.authService = new SupabaseAuthService();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        // Start lobby music
        AudioManager.getInstance().playRandomLobbyMusic();

        background = new Texture(Gdx.files.internal("LobbyBg.png"));
        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));

        Pixmap dimPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPixmap.setColor(0f, 0f, 0f, 0.35f);
        dimPixmap.fill();
        dimOverlay = new Texture(dimPixmap);
        dimPixmap.dispose();

        Pixmap fieldPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        fieldPixmap.setColor(0.15f, 0.15f, 0.2f, 0.9f);
        fieldPixmap.fill();
        textFieldBg = new Texture(fieldPixmap);
        fieldPixmap.dispose();

        Pixmap cursorPixmap = new Pixmap(2, 1, Pixmap.Format.RGBA8888);
        cursorPixmap.setColor(Color.WHITE);
        cursorPixmap.fill();
        cursorTexture = new Texture(cursorPixmap);
        cursorPixmap.dispose();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        rootTable = table;

        fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = FontManager.getMenuFont();
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.cursor = new TextureRegionDrawable(cursorTexture);
        fieldStyle.background = new TextureRegionDrawable(textFieldBg);
        fieldStyle.selection = new TextureRegionDrawable(textFieldBg);

        buildCurrentView();
        setLoginHint();
    }

    private void buildCurrentView() {
        rootTable.clearChildren();

        String title = currentView == AuthView.LOGIN ? "Login" : "Register";
        Label titleLabel = new Label(title, new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        rootTable.add(titleLabel).padBottom(36).row();

        if (currentView == AuthView.LOGIN) {
            buildLoginView();
        } else {
            buildRegisterView();
        }
    }

    private void buildLoginView() {
        loginField = new TextField(loginField == null ? "" : loginField.getText(), fieldStyle);
        loginField.setMessageText("Nickname or email");
        loginField.setMaxLength(64);
        rootTable.add(loginField).width(520).height(72).padBottom(12).row();

        passwordField = new TextField("", fieldStyle);
        passwordField.setMessageText("Password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setMaxLength(72);
        rootTable.add(passwordField).width(520).height(72).padBottom(14).row();

        statusLabel = new Label("", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        rootTable.add(statusLabel).padBottom(16).row();

        TextButton loginButton = createButton("LOGIN");
        loginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                submitLogin();
            }
        });
        rootTable.add(loginButton).width(520).height(110).padBottom(8).row();

        TextButton openRegisterButton = createButton("REGISTER");
        openRegisterButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (requestInFlight) {
                    return;
                }
                currentView = AuthView.REGISTER;
                buildCurrentView();
            }
        });
        rootTable.add(openRegisterButton).width(520).height(110).padBottom(8).row();
    }

    private void buildRegisterView() {
        nicknameField = new TextField(nicknameField == null ? "" : nicknameField.getText(), fieldStyle);
        nicknameField.setMessageText("Nickname (3-16, unique)");
        nicknameField.setMaxLength(16);
        rootTable.add(nicknameField).width(520).height(72).padBottom(12).row();

        emailField = new TextField(emailField == null ? "" : emailField.getText(), fieldStyle);
        emailField.setMessageText("Email");
        emailField.setMaxLength(128);
        rootTable.add(emailField).width(520).height(72).padBottom(12).row();

        passwordField = new TextField("", fieldStyle);
        passwordField.setMessageText("Password (8+, at least 1 digit)");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setMaxLength(72);
        rootTable.add(passwordField).width(520).height(72).padBottom(14).row();

        statusLabel = new Label("", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        rootTable.add(statusLabel).padBottom(16).row();

        TextButton registerButton = createButton("CREATE ACCOUNT");
        registerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                submitRegister();
            }
        });
        rootTable.add(registerButton).width(520).height(110).padBottom(8).row();

        TextButton backToLoginButton = createButton("BACK TO LOGIN");
        backToLoginButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (requestInFlight) {
                    return;
                }
                currentView = AuthView.LOGIN;
                buildCurrentView();
                setLoginHint();
            }
        });
        rootTable.add(backToLoginButton).width(520).height(110).padBottom(8).row();
    }

    private void setLoginHint() {
        if (!authService.isConfigured()) {
            statusLabel.setText(authService.getConfigurationError());
            return;
        }

        if (statusLabel != null) {
            statusLabel.setText("Enter your nickname/email and password.");
        }
    }

    private void submitRegister() {
        if (requestInFlight) {
            return;
        }

        requestInFlight = true;
        statusLabel.setText("Creating account and sending confirmation email...");
        authService.register(emailField.getText(), nicknameField.getText(), passwordField.getText(), new SupabaseAuthService.RegisterCallback() {
            @Override
            public void onRegistered(String message) {
                requestInFlight = false;
                statusLabel.setText(message);
                String email = emailField.getText().trim();
                String nickname = nicknameField.getText().trim();
                currentView = AuthView.LOGIN;
                buildCurrentView();
                loginField.setText(email.isEmpty() ? nickname : email);
                statusLabel.setText(message);
            }

            @Override
            public void onError(String message) {
                requestInFlight = false;
                statusLabel.setText(message);
            }
        });
    }

    private void submitLogin() {
        if (requestInFlight) {
            return;
        }

        requestInFlight = true;
        statusLabel.setText("Signing in...");
        authService.login(loginField.getText(), passwordField.getText(), new SupabaseAuthService.AuthCallback() {
            @Override
            public void onSuccess(AuthSession session) {
                // First check if player is already online before proceeding
                statusLabel.setText("Verifying player is not already in game...");
                checkPlayerAvailability(session.getUsername());
            }

            @Override
            public void onError(String message) {
                requestInFlight = false;
                statusLabel.setText(message);
            }
        });
    }

    private void checkPlayerAvailability(String playerName) {
        if (lobbyInfoClient == null) {
            // If no lobby info client available, proceed anyway
            requestInFlight = false;
            statusLabel.setText("Welcome, " + playerName + "!");
            goToMainMenu();
            return;
        }

        statusLabel.setText("Verifying player availability...");

        // Set up listener for availability response first
        lobbyInfoClient.setPlayerAvailabilityListener((name, available, reason) -> {
            Gdx.app.postRunnable(() -> {
                requestInFlight = false;
                if (available) {
                    // Player is available, proceed to main menu
                    statusLabel.setText("Welcome, " + playerName + "!");
                    goToMainMenu();
                } else {
                    // Player is already in game - block login
                    statusLabel.setText("ERROR: " + reason);
                    System.out.println("[AuthScreen] Login blocked: " + reason);
                }
            });
        });

        // Try to send check request
        if (lobbyInfoClient.isConnected()) {
            System.out.println("[AuthScreen] Checking player availability: " + playerName);
            lobbyInfoClient.checkPlayerAvailability(playerName);
        } else {
            // Server not connected, proceed anyway
            System.out.println("[AuthScreen] Server not connected, allowing login anyway");
            requestInFlight = false;
            statusLabel.setText("Welcome, " + playerName + "!");
            goToMainMenu();
        }
    }

    private TextButton createButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = FontManager.getMenuFont();
        style.fontColor = Color.WHITE;
        style.up = new TextureRegionDrawable(buttonTexture);
        style.down = new TextureRegionDrawable(buttonPressedTexture);
        return new TextButton(text, style);
    }

    private void goToMainMenu() {
        game.setScreen(new MainMenuScreen(game, lobbyInfoClient));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
        batch.draw(dimOverlay, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
    }

    @Override
    public void pause() {
        // No-op: this screen has no background tasks that need pause handling.
    }

    @Override
    public void resume() {
        // No-op: rendering/input continue automatically after resume.
    }

    @Override
    public void hide() {
        // No-op: resources are disposed in dispose().
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        background.dispose();
        dimOverlay.dispose();
        textFieldBg.dispose();
        cursorTexture.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
    }
}

