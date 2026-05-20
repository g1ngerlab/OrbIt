package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.loot.LootEntry;
import taltech.orbitgame.loot.PlayerLootService;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.shop.ShopCatalog;
import taltech.orbitgame.util.FontManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main menu shop screen.
 */
public class ShopMenuScreen implements Screen {
    private static final int TILE_SIZE = 64;
    private static final int TILE_COLUMN = 2;
    private static final int TILE_ROW_FROM_TOP = 6;
    private static final int WHITE_CUTOFF = 245;
    private static final int SHOP_COLUMNS = 3;

    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private final PlayerLootService lootService;

    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture inventoryAtlas;
    private Map<String, Texture> itemTextures;
    private List<TextButton> buyButtons;
    private Texture screenGrayOverlay;
    private Texture panelBgTexture;
    private Texture cardBgTexture;
    private Label orbBalanceLabel;
    private Label statusLabel;
    private ScrollPane itemScroll;
    private Slider scrollSlider;
    private Table previewOverlay;
    private int orbBalance;
    private boolean purchaseInProgress;
    private boolean disposed;

    public ShopMenuScreen(Main game, LobbyInfoClient lobbyInfoClient) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
        this.lootService = new PlayerLootService();
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        background = new Texture(Gdx.files.internal("LobbyBg.png"));
        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        inventoryAtlas = new Texture(Gdx.files.internal("UI/inventory_border.png"));
        itemTextures = new LinkedHashMap<>();
        buyButtons = new ArrayList<>();
        for (ShopCatalog.Item item : ShopCatalog.ITEMS) {
            itemTextures.put(item.getCode(), loadTextureWithoutWhiteBackground(item.getIconPath()));
        }
        screenGrayOverlay = createSolidTexture(0.14f, 0.14f, 0.16f, 0.5f);
        panelBgTexture = createSolidTexture(0.25f, 0.25f, 0.28f, 0.92f);
        cardBgTexture = createSolidTexture(0.2f, 0.2f, 0.23f, 0.95f);

        Table root = new Table();
        root.setFillParent(true);
        root.center();
        stage.addActor(root);

        TextureRegion slotRegion = new TextureRegion(
            inventoryAtlas,
            (TILE_COLUMN - 1) * TILE_SIZE,
            (TILE_ROW_FROM_TOP - 1) * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        TextureRegionDrawable frameDrawable = new TextureRegionDrawable(slotRegion);

        Table panelOuter = new Table();
        panelOuter.setBackground(frameDrawable);
        panelOuter.pad(10f);
        root.add(panelOuter).width(880f).height(680f).pad(10f);

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(panelBgTexture));
        panel.pad(18f);
        panelOuter.add(panel).expand().fill();

        Table header = new Table();
        Label title = new Label("SHOP", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        header.add(title).left().expandX();
        orbBalanceLabel = new Label("ORB: ...", new Label.LabelStyle(FontManager.getMenuFont(), Color.CYAN));
        header.add(orbBalanceLabel).right();
        panel.add(header).expandX().fillX().padBottom(6f).row();

        Label subtitle = new Label("COLLECTIBLES", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        subtitle.setFontScale(0.86f);
        panel.add(subtitle).left().padBottom(10f).row();

        Table itemsGrid = new Table();
        for (int i = 0; i < ShopCatalog.ITEMS.length; i++) {
            itemsGrid.add(createShopItemCard(ShopCatalog.ITEMS[i], frameDrawable)).width(252f).height(210f).pad(6f);
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
        sliderStyle.background = frameDrawable;
        sliderStyle.knob = frameDrawable;
        sliderStyle.knobBefore = new TextureRegionDrawable(cardBgTexture);
        scrollSlider = new Slider(0f, 1f, 0.001f, true, sliderStyle);

        Table scrollRow = new Table();
        scrollRow.add(itemScroll).expand().fill().padRight(10f);
        scrollRow.add(scrollSlider).width(34f).fillY();
        panel.add(scrollRow).expand().fill().padBottom(10f).row();
        bindShopScrolling();

        statusLabel = new Label("", new Label.LabelStyle(FontManager.getMenuFont(), Color.YELLOW));
        panel.add(statusLabel).padBottom(12f).row();

        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game, lobbyInfoClient));
            }
        });
        panel.add(backButton).width(300f).height(86f);

        loadOrbBalance();
    }

    private void loadOrbBalance() {
        if (!lootService.isConfigured()) {
            orbBalance = 0;
            updateOrbBalanceLabel();
            statusLabel.setText("Supabase is not configured");
            updateBuyButtonsDisabled();
            return;
        }

        statusLabel.setText("Loading wallet...");
        updateBuyButtonsDisabled();
        lootService.loadPlayerLoot(new PlayerLootService.LoadCallback() {
            @Override
            public void onSuccess(List<LootEntry> entries) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    orbBalance = extractQuantity(entries, ItemKind.ORB.getCode());
                    updateOrbBalanceLabel();
                    statusLabel.setText("Collectibles are cosmetic inventory items");
                    updateBuyButtonsDisabled();
                });
            }

            @Override
            public void onError(String message) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    orbBalance = 0;
                    updateOrbBalanceLabel();
                    statusLabel.setText(message == null ? "Failed to load wallet" : message);
                    updateBuyButtonsDisabled();
                });
            }
        });
    }

    private Table createShopItemCard(ShopCatalog.Item item, TextureRegionDrawable frameDrawable) {
        Table itemFrame = new Table();
        itemFrame.setBackground(frameDrawable);
        itemFrame.pad(7f);

        Table itemCard = new Table();
        itemCard.setBackground(new TextureRegionDrawable(cardBgTexture));
        itemCard.pad(10f);

        Texture itemTexture = itemTextures.get(item.getCode());
        if (itemTexture != null) {
            itemCard.add(new Image(itemTexture)).size(76f, 76f).padBottom(5f).row();
        }

        Label nameLabel = new Label(item.getDisplayName(), new Label.LabelStyle(FontManager.getMenuFont(), Color.WHITE));
        nameLabel.setFontScale(0.68f);
        itemCard.add(nameLabel).padBottom(1f).row();

        Label priceLabel = new Label(item.getPriceOrb() + " ORB", new Label.LabelStyle(FontManager.getMenuFont(), Color.CYAN));
        priceLabel.setFontScale(0.6f);
        itemCard.add(priceLabel).padBottom(4f).row();

        TextButton buyItemButton = createButton("BUY");
        buyItemButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleBuyItem(item);
            }
        });
        buyButtons.add(buyItemButton);
        itemCard.add(buyItemButton).width(142f).height(54f);

        Stack cardStack = new Stack();
        cardStack.add(itemCard);
        cardStack.add(createInfoButtonLayer(item));

        itemFrame.add(cardStack).expand().fill();
        return itemFrame;
    }

    private Table createInfoButtonLayer(ShopCatalog.Item item) {
        Table layer = new Table();
        layer.top().right();
        TextButton infoButton = createInfoButton();
        infoButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showItemPreview(item);
            }
        });
        layer.add(infoButton).size(38f, 38f).pad(4f);
        return layer;
    }

    private TextButton createInfoButton() {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = new TextureRegionDrawable(cardBgTexture);
        style.down = new TextureRegionDrawable(panelBgTexture);
        style.font = FontManager.getMenuFont();
        style.fontColor = Color.CYAN;
        TextButton button = new TextButton("i", style);
        button.getLabel().setFontScale(0.58f);
        return button;
    }

    private void showItemPreview(ShopCatalog.Item item) {
        closeItemPreview();

        previewOverlay = new Table();
        previewOverlay.setFillParent(true);
        previewOverlay.setBackground(new TextureRegionDrawable(screenGrayOverlay));
        previewOverlay.center();

        Table previewPanel = new Table();
        previewPanel.setBackground(new TextureRegionDrawable(panelBgTexture));
        previewPanel.pad(18f);

        Label nameLabel = new Label(item.getDisplayName(), new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        nameLabel.setFontScale(0.82f);
        previewPanel.add(nameLabel).padBottom(12f).row();

        Texture texture = itemTextures.get(item.getCode());
        if (texture != null) {
            float maxWidth = Gdx.graphics.getWidth() * 0.72f;
            float maxHeight = Gdx.graphics.getHeight() * 0.64f;
            float scale = Math.min(maxWidth / texture.getWidth(), maxHeight / texture.getHeight());
            scale = Math.max(0.1f, scale);
            previewPanel.add(new Image(texture))
                .size(texture.getWidth() * scale, texture.getHeight() * scale)
                .padBottom(14f)
                .row();
        }

        TextButton closeButton = createButton("CLOSE");
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeItemPreview();
            }
        });
        previewPanel.add(closeButton).width(260f).height(76f);

        previewOverlay.add(previewPanel).width(Gdx.graphics.getWidth() * 0.82f);
        stage.addActor(previewOverlay);
    }

    private void closeItemPreview() {
        if (previewOverlay != null) {
            previewOverlay.remove();
            previewOverlay = null;
        }
    }

    private void handleBuyItem(ShopCatalog.Item item) {
        if (purchaseInProgress) {
            return;
        }
        if (orbBalance < item.getPriceOrb()) {
            statusLabel.setText("Not enough ORB");
            return;
        }

        purchaseInProgress = true;
        updateBuyButtonsDisabled();
        statusLabel.setText("Purchasing...");

        lootService.syncShopPurchase(
            ItemKind.ORB.getCode(),
            item.getPriceOrb(),
            item.getCode(),
            1,
            new PlayerLootService.SaveCallback() {
                @Override
                public void onSuccess() {
                    Gdx.app.postRunnable(() -> {
                        if (disposed) {
                            return;
                        }
                        purchaseInProgress = false;
                        orbBalance = Math.max(0, orbBalance - item.getPriceOrb());
                        updateOrbBalanceLabel();
                        statusLabel.setText(item.getDisplayName() + " purchased");
                        updateBuyButtonsDisabled();
                    });
                }

                @Override
                public void onError(String message) {
                    Gdx.app.postRunnable(() -> {
                        if (disposed) {
                            return;
                        }
                        purchaseInProgress = false;
                        statusLabel.setText(message == null ? "Purchase failed" : message);
                        updateBuyButtonsDisabled();
                    });
                }
            }
        );
    }

    private int extractQuantity(List<LootEntry> entries, String itemCode) {
        if (entries == null || itemCode == null) {
            return 0;
        }
        for (LootEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (itemCode.equalsIgnoreCase(entry.getItemCode())) {
                return Math.max(0, entry.getQuantity());
            }
        }
        return 0;
    }

    private void updateOrbBalanceLabel() {
        if (orbBalanceLabel != null) {
            orbBalanceLabel.setText("ORB: " + orbBalance);
        }
    }

    private void updateBuyButtonsDisabled() {
        if (buyButtons == null) {
            return;
        }
        for (TextButton button : buyButtons) {
            button.setDisabled(purchaseInProgress || !lootService.isConfigured());
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

    private TextButton createButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.up = new TextureRegionDrawable(buttonTexture);
        style.down = new TextureRegionDrawable(buttonPressedTexture);
        style.font = FontManager.getMenuFont();
        style.fontColor = Color.WHITE;
        return new TextButton(text, style);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
        batch.draw(screenGrayOverlay, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        batch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
        if (itemScroll != null) {
            itemScroll.layout();
        }
    }

    @Override
    public void pause() {
        // No-op.
    }

    @Override
    public void resume() {
        // No-op.
    }

    @Override
    public void hide() {
        // No-op.
    }

    @Override
    public void dispose() {
        disposed = true;
        closeItemPreview();
        stage.dispose();
        batch.dispose();
        background.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        inventoryAtlas.dispose();
        if (itemTextures != null) {
            for (Texture texture : itemTextures.values()) {
                texture.dispose();
            }
            itemTextures.clear();
        }
        screenGrayOverlay.dispose();
        panelBgTexture.dispose();
        cardBgTexture.dispose();
    }

    private Texture createSolidTexture(float r, float g, float b, float a) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(r, g, b, a);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private Texture loadTextureWithoutWhiteBackground(String path) {
        Pixmap pixmap = new Pixmap(Gdx.files.internal(path));
        int transparentWhite = 0xFFFFFF00;
        for (int y = 0; y < pixmap.getHeight(); y++) {
            for (int x = 0; x < pixmap.getWidth(); x++) {
                int pixel = pixmap.getPixel(x, y);
                int r = (pixel >>> 24) & 0xFF;
                int g = (pixel >>> 16) & 0xFF;
                int b = (pixel >>> 8) & 0xFF;
                int a = pixel & 0xFF;
                if (a > 0 && r >= WHITE_CUTOFF && g >= WHITE_CUTOFF && b >= WHITE_CUTOFF) {
                    pixmap.drawPixel(x, y, transparentWhite);
                }
            }
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

}

