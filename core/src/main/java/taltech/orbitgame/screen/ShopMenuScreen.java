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
import taltech.orbitgame.loot.LootEntry;
import taltech.orbitgame.loot.PlayerLootService;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

import java.util.List;

/**
 * Main menu shop screen.
 */
public class ShopMenuScreen implements Screen {
    private static final int GOLDEN_FROG_PRICE_ORB = 100;
    private static final int TILE_SIZE = 64;
    private static final int TILE_COLUMN = 2;
    private static final int TILE_ROW_FROM_TOP = 6;
    private static final int WHITE_CUTOFF = 245;

    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private final PlayerLootService lootService;

    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture inventoryAtlas;
    private Texture frogTexture;
    private Texture screenGrayOverlay;
    private Texture panelBgTexture;
    private Texture cardBgTexture;
    private Label orbBalanceLabel;
    private Label statusLabel;
    private TextButton buyButton;
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
        frogTexture = loadTextureWithoutWhiteBackground("loot/golden_frog.png");
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
        root.add(panelOuter).width(640f).pad(10f);

        Table panel = new Table();
        panel.setBackground(new TextureRegionDrawable(panelBgTexture));
        panel.pad(18f);
        panelOuter.add(panel).expand().fill();

        Table header = new Table();
        Label title = new Label("SHOP", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        header.add(title).left().expandX();
        orbBalanceLabel = new Label("ORB: ...", new Label.LabelStyle(FontManager.getMenuFont(), Color.CYAN));
        header.add(orbBalanceLabel).right();
        panel.add(header).expandX().fillX().padBottom(16f).row();

        Table itemFrame = new Table();
        itemFrame.setBackground(frameDrawable);
        itemFrame.pad(8f);

        Table itemCard = new Table();
        itemCard.setBackground(new TextureRegionDrawable(cardBgTexture));
        itemCard.pad(12f);
        itemCard.add(new Image(frogTexture)).size(96f, 96f).padRight(14f);
        Label frogLabel = new Label("Golden Frog\nPrice: " + GOLDEN_FROG_PRICE_ORB + " ORB",
            new Label.LabelStyle(FontManager.getMenuFont(), Color.WHITE));
        itemCard.add(frogLabel).left();
        itemFrame.add(itemCard).expand().fill();
        panel.add(itemFrame).expandX().fillX().padBottom(16f).row();

        buyButton = createButton("BUY");
        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                handleBuyGoldenFrog();
            }
        });
        panel.add(buyButton).width(300f).height(86f).padBottom(12f).row();

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
            buyButton.setDisabled(true);
            return;
        }

        statusLabel.setText("Loading wallet...");
        buyButton.setDisabled(true);
        lootService.loadPlayerLoot(new PlayerLootService.LoadCallback() {
            @Override
            public void onSuccess(List<LootEntry> entries) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    orbBalance = extractQuantity(entries, ItemKind.ORB.getCode());
                    updateOrbBalanceLabel();
                    statusLabel.setText(orbBalance >= GOLDEN_FROG_PRICE_ORB
                        ? "You can buy Golden Frog"
                        : "Not enough ORB");
                    buyButton.setDisabled(false);
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
                    buyButton.setDisabled(true);
                });
            }
        });
    }

    private void handleBuyGoldenFrog() {
        if (purchaseInProgress) {
            return;
        }
        if (orbBalance < GOLDEN_FROG_PRICE_ORB) {
            statusLabel.setText("Not enough ORB");
            return;
        }

        purchaseInProgress = true;
        buyButton.setDisabled(true);
        statusLabel.setText("Purchasing...");

        lootService.syncShopPurchase(
            ItemKind.ORB.getCode(),
            GOLDEN_FROG_PRICE_ORB,
            ItemKind.GOLDEN_FROG.getCode(),
            1,
            new PlayerLootService.SaveCallback() {
                @Override
                public void onSuccess() {
                    Gdx.app.postRunnable(() -> {
                        if (disposed) {
                            return;
                        }
                        purchaseInProgress = false;
                        orbBalance = Math.max(0, orbBalance - GOLDEN_FROG_PRICE_ORB);
                        updateOrbBalanceLabel();
                        statusLabel.setText("Golden Frog purchased");
                        buyButton.setDisabled(false);
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
                        buyButton.setDisabled(false);
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
        stage.dispose();
        batch.dispose();
        background.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        inventoryAtlas.dispose();
        frogTexture.dispose();
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

