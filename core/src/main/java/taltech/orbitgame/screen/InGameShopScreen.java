package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
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
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.util.FontManager;

/**
 * In-game shop overlay where players can buy loot for ORB currency.
 */
public class InGameShopScreen implements Screen {
    private static final int GOLDEN_FROG_PRICE = 100;

    private final Main game;
    private final GameScreen gameScreen;
    private final LobbyInfoClient lobbyInfoClient;

    private Stage stage;
    private Texture overlay;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture frogTexture;
    private Label orbLabel;
    private Label statusLabel;

    public InGameShopScreen(Main game, GameScreen gameScreen, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.gameScreen = gameScreen;
        this.lobbyInfoClient = lobbyInfoClient;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Pixmap dim = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dim.setColor(0f, 0f, 0f, 0.55f);
        dim.fill();
        overlay = new Texture(dim);
        dim.dispose();

        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        frogTexture = new Texture(Gdx.files.internal("loot/golden_frog.png"));

        buildUi();
        refreshOrbLabel();
    }

    private void buildUi() {
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = FontManager.getMenuFont();
        buttonStyle.fontColor = Color.WHITE;

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        Table card = new Table();
        card.setBackground(new TextureRegionDrawable(overlay));
        card.pad(18f);

        Label title = new Label("SHOP", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        card.add(title).padBottom(12f).row();

        orbLabel = new Label("ORB: 0", new Label.LabelStyle(FontManager.getMenuFont(), Color.CYAN));
        card.add(orbLabel).padBottom(14f).row();

        Table itemRow = new Table();
        itemRow.add(new Image(frogTexture)).size(78f, 78f).padRight(12f);
        Label itemLabel = new Label("Golden Frog\nPrice: 100 ORB", new Label.LabelStyle(FontManager.getMenuFont(), Color.WHITE));
        itemRow.add(itemLabel).left();
        card.add(itemRow).padBottom(16f).row();

        TextButton buyButton = new TextButton("BUY", buttonStyle);
        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleBuy();
            }
        });
        card.add(buyButton).width(240f).height(64f).padBottom(8f).row();

        statusLabel = new Label("", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        card.add(statusLabel).padBottom(10f).row();

        TextButton backButton = new TextButton("BACK", buttonStyle);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeToEsc();
            }
        });
        card.add(backButton).width(240f).height(64f).row();

        root.add(card).width(420f);
    }

    private void handleBuy() {
        if (gameScreen.isShopPurchaseInProgress()) {
            statusLabel.setText("Purchase already in progress...");
            return;
        }

        boolean sent = gameScreen.requestShopPurchase(ItemKind.GOLDEN_FROG.getId(), 1, GOLDEN_FROG_PRICE);
        if (!sent) {
            String status = gameScreen.pollShopStatusMessage();
            statusLabel.setText(status == null ? "Purchase failed" : status);
            refreshOrbLabel();
        }
    }

    private void refreshOrbLabel() {
        orbLabel.setText("ORB: " + gameScreen.getCurrentOrbBalance());
    }

    private void closeToEsc() {
        game.setScreen(new GameEscScreen(game, gameScreen, lobbyInfoClient));
    }

    @Override
    public void render(float delta) {
        if (!gameScreen.isDisposed()) {
            gameScreen.render(delta);
        }

        stage.getBatch().begin();
        stage.getBatch().draw(overlay, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();

        String status = gameScreen.pollShopStatusMessage();
        if (status != null) {
            statusLabel.setText(status);
            refreshOrbLabel();
        }

        stage.act(delta);
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeToEsc();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        gameScreen.resize(width, height);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        overlay.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        frogTexture.dispose();
    }
}

