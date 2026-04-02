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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.player.CharacterType;
import taltech.orbitgame.player.PlayerProfile;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

import java.util.ArrayList;
import java.util.List;

public class CharacterSelectScreen implements Screen {
    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private final List<Texture> previewTextures = new ArrayList<>();

    public CharacterSelectScreen(Game game, LobbyInfoClient lobbyInfoClient) {
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

        Label titleLabel = new Label("Select Character",
            new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        mainTable.add(titleLabel).padTop(30).padBottom(40).colspan(2).row();

        addCharacterOption(mainTable, CharacterType.MATH);
        addCharacterOption(mainTable, CharacterType.PUPU);
        mainTable.row();

        TextButton backButton = createButton("Back");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen((Main) game, lobbyInfoClient));
            }
        });
        mainTable.add(backButton).padTop(30).size(400, 100).colspan(2).row();
    }

    private void addCharacterOption(Table mainTable, CharacterType characterType) {
        Table characterTable = new Table();
        Texture preview = new Texture(Gdx.files.internal(characterType.getPreviewTexturePath()));
        preview.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        previewTextures.add(preview);

        Image characterImage = new Image(new TextureRegionDrawable(preview));
        characterTable.add(characterImage).size(240, 240).padBottom(15).row();

        TextButton selectButton = createButton(characterType.getDisplayName());
        selectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                PlayerProfile.setSelectedCharacterId(characterType.getId());
                game.setScreen(new MainMenuScreen((Main) game, lobbyInfoClient));
            }
        });
        characterTable.add(selectButton).size(280, 90).row();

        int currentSelection = PlayerProfile.getSelectedCharacterId();
        if (currentSelection == characterType.getId()) {
            Label selectedLabel = new Label("Selected", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
            characterTable.add(selectedLabel).padTop(10).row();
        }

        mainTable.add(characterTable).pad(20);
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
        for (Texture texture : previewTextures) {
            texture.dispose();
        }
        previewTextures.clear();
    }
}
