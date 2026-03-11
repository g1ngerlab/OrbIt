package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.util.FontManager;

/**
 * Settings menu screen with two placeholder buttons
 */
public class SettingsMenuScreen implements Screen {
    private Main game;
    private Stage stage;
    private SpriteBatch batch;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private BitmapFont pixelFont;

    public SettingsMenuScreen(Main game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // Load assets
        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        buttonPressedTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        // Load pixel font from assets
        pixelFont = FontManager.getMenuFont();

        // Create UI layout
        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        // Title
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = pixelFont;
        titleStyle.fontColor = Color.WHITE;
        Label titleLabel = new Label("SETTINGS", titleStyle);
        table.add(titleLabel).padBottom(50).row();

        // Create placeholder button 1
        TextButton button1 = createButton("OPTION 1");
        button1.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                System.out.println("Option 1 clicked");
            }
        });
        table.add(button1).padBottom(20).size(button1.getWidth(), button1.getHeight()).row();

        // Create placeholder button 2
        TextButton button2 = createButton("BACK");
        button2.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
        table.add(button2).padBottom(20).size(button2.getWidth(), button2.getHeight()).row();
    }

    private TextButton createButton(String text) {
        // Create button style with background images
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = pixelFont;
        buttonStyle.fontColor = Color.WHITE;

        TextButton button = new TextButton(text, buttonStyle);

        // Make buttons smaller - scale to 60% of original size
        float buttonWidth = buttonTexture.getWidth() * 0.6f;
        float buttonHeight = buttonTexture.getHeight() * 0.6f;
        button.setSize(buttonWidth, buttonHeight);

        // Center text in the button by ensuring the label cell fills the button area
        button.getLabel().setAlignment(Align.center);
        button.getLabelCell().size(buttonWidth, buttonHeight);

        return button;
    }

    @Override
    public void render(float delta) {
        // Clear screen with black background
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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
        stage.dispose();
        batch.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        // Font is managed by FontManager, don't dispose here
    }
}
