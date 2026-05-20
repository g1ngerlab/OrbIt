package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.shop.ShopCatalog;
import taltech.orbitgame.util.FontManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-game shop overlay where players can buy loot for ORB currency.
 */
public class InGameShopScreen implements Screen {
    private static final int SHOP_COLUMNS = 3;

    private final Main game;
    private final GameScreen gameScreen;
    private final LobbyInfoClient lobbyInfoClient;

    private Stage stage;
    private Texture overlay;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture cardBackground;
    private Map<String, Texture> itemTextures;
    private List<TextButton> buyButtons;
    private Label orbLabel;
    private Label statusLabel;
    private ScrollPane itemScroll;
    private Slider scrollSlider;
    private Table previewOverlay;

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
        cardBackground = createSolidTexture(0.18f, 0.18f, 0.21f, 0.92f);
        itemTextures = new LinkedHashMap<>();
        buyButtons = new ArrayList<>();
        for (ShopCatalog.Item item : ShopCatalog.ITEMS) {
            itemTextures.put(item.getCode(), new Texture(Gdx.files.internal(item.getIconPath())));
        }

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
        card.add(title).padBottom(4f).row();

        Label subtitle = new Label("COLLECTIBLES", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        subtitle.setFontScale(0.82f);
        card.add(subtitle).padBottom(10f).row();

        orbLabel = new Label("ORB: 0", new Label.LabelStyle(FontManager.getMenuFont(), Color.CYAN));
        card.add(orbLabel).padBottom(10f).row();

        Table itemsGrid = new Table();
        for (int i = 0; i < ShopCatalog.ITEMS.length; i++) {
            itemsGrid.add(createShopItemCard(ShopCatalog.ITEMS[i], buttonStyle)).width(206f).height(178f).pad(5f);
            if ((i + 1) % SHOP_COLUMNS == 0) {
                itemsGrid.row();
            }
        }

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        itemScroll = new ScrollPane(itemsGrid, scrollStyle);
        itemScroll.setFadeScrollBars(false);
        itemScroll.setScrollingDisabled(true, false);
        itemScroll.setSmoothScrolling(true);
        itemScroll.setOverscroll(false, false);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = new TextureRegionDrawable(overlay);
        sliderStyle.knob = new TextureRegionDrawable(cardBackground);
        sliderStyle.knobBefore = new TextureRegionDrawable(cardBackground);
        scrollSlider = new Slider(0f, 1f, 0.001f, true, sliderStyle);

        Table scrollRow = new Table();
        scrollRow.add(itemScroll).width(668f).height(392f).padRight(10f);
        scrollRow.add(scrollSlider).width(24f).height(392f);
        card.add(scrollRow).padBottom(8f).row();
        bindShopScrolling();

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

        root.add(card).width(720f);
    }

    private Table createShopItemCard(ShopCatalog.Item item, TextButton.TextButtonStyle buttonStyle) {
        Table itemCard = new Table();
        itemCard.setBackground(new TextureRegionDrawable(cardBackground));
        itemCard.pad(8f);

        Texture texture = itemTextures.get(item.getCode());
        if (texture != null) {
            itemCard.add(new Image(texture)).size(64f, 64f).padBottom(4f).row();
        }

        Label nameLabel = new Label(item.getDisplayName(), new Label.LabelStyle(FontManager.getMenuFont(), Color.WHITE));
        nameLabel.setFontScale(0.58f);
        itemCard.add(nameLabel).padBottom(2f).row();

        Label priceLabel = new Label(item.getPriceOrb() + " ORB", new Label.LabelStyle(FontManager.getMenuFont(), Color.CYAN));
        priceLabel.setFontScale(0.54f);
        itemCard.add(priceLabel).padBottom(4f).row();

        TextButton buyButton = new TextButton("BUY", buttonStyle);
        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleBuy(item);
            }
        });
        buyButtons.add(buyButton);
        itemCard.add(buyButton).width(124f).height(48f);

        Stack cardStack = new Stack();
        cardStack.add(itemCard);
        cardStack.add(createInfoButtonLayer(item, buttonStyle));
        Table wrapper = new Table();
        wrapper.add(cardStack).expand().fill();
        return wrapper;
    }

    private Table createInfoButtonLayer(ShopCatalog.Item item, TextButton.TextButtonStyle buttonStyle) {
        Table layer = new Table();
        layer.top().right();
        TextButton infoButton = new TextButton("i", buttonStyle);
        infoButton.getLabel().setFontScale(0.52f);
        infoButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showItemPreview(item);
            }
        });
        layer.add(infoButton).size(36f, 36f).pad(4f);
        return layer;
    }

    private void showItemPreview(ShopCatalog.Item item) {
        closeItemPreview();

        previewOverlay = new Table();
        previewOverlay.setFillParent(true);
        previewOverlay.setBackground(new TextureRegionDrawable(overlay));
        previewOverlay.center();

        Table previewPanel = new Table();
        previewPanel.setBackground(new TextureRegionDrawable(cardBackground));
        previewPanel.pad(18f);

        Label nameLabel = new Label(item.getDisplayName(), new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        nameLabel.setFontScale(0.76f);
        previewPanel.add(nameLabel).padBottom(12f).row();

        Texture texture = itemTextures.get(item.getCode());
        if (texture != null) {
            float maxWidth = Gdx.graphics.getWidth() * 0.72f;
            float maxHeight = Gdx.graphics.getHeight() * 0.62f;
            float scale = Math.min(maxWidth / texture.getWidth(), maxHeight / texture.getHeight());
            scale = Math.max(0.1f, scale);
            previewPanel.add(new Image(texture))
                .size(texture.getWidth() * scale, texture.getHeight() * scale)
                .padBottom(12f)
                .row();
        }

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = FontManager.getMenuFont();
        buttonStyle.fontColor = Color.WHITE;

        TextButton closeButton = new TextButton("CLOSE", buttonStyle);
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeItemPreview();
            }
        });
        previewPanel.add(closeButton).width(240f).height(64f);

        previewOverlay.add(previewPanel).width(Gdx.graphics.getWidth() * 0.82f);
        stage.addActor(previewOverlay);
    }

    private void closeItemPreview() {
        if (previewOverlay != null) {
            previewOverlay.remove();
            previewOverlay = null;
        }
    }

    private void handleBuy(ShopCatalog.Item item) {
        if (gameScreen.isShopPurchaseInProgress()) {
            statusLabel.setText("Purchase already in progress...");
            return;
        }

        boolean sent = gameScreen.requestShopPurchase(item.getKind().getId(), 1, item.getPriceOrb());
        if (!sent) {
            String status = gameScreen.pollShopStatusMessage();
            statusLabel.setText(status == null ? "Purchase failed" : status);
            refreshOrbLabel();
        }
    }

    private void bindShopScrolling() {
        itemScroll.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float maxY = Math.max(0f, itemScroll.getMaxY());
                float ratio = maxY <= 0f ? 0f : itemScroll.getScrollY() / maxY;
                scrollSlider.setValue(ratio);
            }
        });

        scrollSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float maxY = Math.max(0f, itemScroll.getMaxY());
                itemScroll.setScrollY(maxY * scrollSlider.getValue());
            }
        });
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && previewOverlay != null) {
            closeItemPreview();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            closeToEsc();
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        gameScreen.resize(width, height);
        if (itemScroll != null) {
            itemScroll.layout();
        }
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        closeItemPreview();
        stage.dispose();
        overlay.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        cardBackground.dispose();
        if (itemTextures != null) {
            for (Texture texture : itemTextures.values()) {
                texture.dispose();
            }
            itemTextures.clear();
        }
    }

    private Texture createSolidTexture(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}

