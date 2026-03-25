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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

/**
 * Screen for choosing between creating a new lobby or joining an existing one
 */
public class LobbyChoiceScreen implements Screen {
    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;

    public LobbyChoiceScreen(Game game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        background = new Texture(Gdx.files.internal("LobbyBg.png"));

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label titleLabel = new Label("Play", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        table.add(titleLabel).padTop(30).padBottom(50).row();

        // Create Lobby button
        TextButton createButton = createButton("Create Lobby");
        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new CreateLobbyScreen(game, lobbyInfoClient));
            }
        });
        table.add(createButton).padBottom(20).size(500, 150).row();

        // Join Lobby button
        TextButton joinButton = createButton("Join Lobby");
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new JoinLobbyScreen(game, lobbyInfoClient));
            }
        });
        table.add(joinButton).padBottom(20).size(500, 150).row();

        // Back button
        TextButton backButton = createButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen((Main) game, lobbyInfoClient));
            }
        });
        table.add(backButton).padBottom(20).size(500, 150).row();
    }

    private TextButton createButton(String text) {
        Texture buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        Texture buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        BitmapFont font = FontManager.getMenuFont();

        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.up = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonTexture);
        style.down = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(buttonPressedTexture);

        return new TextButton(text, style);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
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
    }
}
