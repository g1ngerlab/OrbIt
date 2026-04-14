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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.auth.AuthState;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.item.ItemDefinition;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.item.ItemStack;
import taltech.orbitgame.loot.LootEntry;
import taltech.orbitgame.loot.PlayerLootService;
import taltech.orbitgame.loadout.LoadoutSelection;
import taltech.orbitgame.network.Network;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-match loadout selection screen.
 */
public class ChooseLoadoutScreen implements Screen {
    private static final float CELL_SIZE = 84f;
    private static final float CELL_SPACING = 12f;
    private static final float SOURCE_CELL_SIZE = 82f;
    private static final int SOURCE_COLUMNS = 5;

    private final Game game;
    private final LobbyInfoClient lobbyInfoClient;
    private final int lobbyId;
    private final Screen returnScreen;
    private final PlayerLootService lootService = new PlayerLootService();
    private final LoadoutSelection loadoutSelection = new LoadoutSelection();

    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture atlasTexture;
    private Texture dimOverlay;
    private BitmapFont font;
    private Drawable slotDrawable;
    private Map<Integer, Texture> itemIcons;

    private Table rootTable;
    private Table sourceContainer;
    private Table loadoutContainer;
    private Table sourceGridTable;
    private Table loadoutGridTable;
    private Table loadoutWeaponTable;
    private ScrollPane sourceScrollPane;
    private Label titleLabel;
    private Label statusLabel;
    private Label selectionLabel;

    private final List<LootEntry> sourceEntries = new ArrayList<>();
    private int selectedSourceIndex = -1;
    private int selectedLoadoutSlot = -1;
    private boolean loading = false;
    private boolean startingMatch = false;
    private boolean disposed = false;

    public ChooseLoadoutScreen(Game game, LobbyInfoClient lobbyInfoClient, int lobbyId, Screen returnScreen) {
        this.game = game;
        this.lobbyInfoClient = lobbyInfoClient;
        this.lobbyId = lobbyId;
        this.returnScreen = returnScreen;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        batch = new SpriteBatch();
        Gdx.input.setInputProcessor(stage);

        AudioManager.getInstance().playRandomLobbyMusic();

        background = new Texture(Gdx.files.internal("LobbyBg.png"));
        buttonTexture = new Texture(Gdx.files.internal("Buttons/button.png"));
        buttonPressedTexture = new Texture(Gdx.files.internal("Buttons/buttonPressed.png"));
        atlasTexture = new Texture(Gdx.files.internal("UI/inventory_border.png"));
        Pixmap dimPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPixmap.setColor(0f, 0f, 0f, 0.35f);
        dimPixmap.fill();
        dimOverlay = new Texture(dimPixmap);
        dimPixmap.dispose();
        slotDrawable = new TextureRegionDrawable(
            new com.badlogic.gdx.graphics.g2d.TextureRegion(atlasTexture, 64, 320, 64, 64)
        );
        font = FontManager.getMenuFont();
        itemIcons = loadItemIcons();

        buildUi();
        loadPlayerInventory();
    }

    private void buildUi() {
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.top();
        rootTable.padTop(28f);
        stage.addActor(rootTable);

        titleLabel = new Label("Choose Loadout", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        rootTable.add(titleLabel).padBottom(10f).row();

        statusLabel = new Label("Loading player inventory...", new Label.LabelStyle(font, Color.YELLOW));
        rootTable.add(statusLabel).padBottom(6f).row();

        selectionLabel = new Label("Click an item, then click a destination slot.", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        rootTable.add(selectionLabel).padBottom(18f).row();

        Table content = new Table();
        rootTable.add(content).expand().fill().padLeft(24f).padRight(24f).row();

        sourceContainer = new Table();
        loadoutContainer = new Table();
        content.add(sourceContainer).expand().fill().padRight(30f);
        content.add(loadoutContainer).expand().fill();

        Table buttons = new Table();
        TextButton confirmButton = createButton("CONFIRM");
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirmLoadout();
            }
        });

        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                backToReturnScreen();
            }
        });

        buttons.add(confirmButton).width(280f).height(92f).padRight(16f);
        buttons.add(backButton).width(280f).height(92f);
        rootTable.add(buttons).padBottom(24f).row();
    }

    private void loadPlayerInventory() {
        loading = true;
        sourceEntries.clear();
        if (!lootService.isConfigured()) {
            statusLabel.setText("Supabase is not configured");
            refreshPanels();
            loading = false;
            return;
        }

        lootService.loadPlayerLoot(new PlayerLootService.LoadCallback() {
            @Override
            public void onSuccess(List<LootEntry> entries) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    sourceEntries.clear();
                    if (entries != null) {
                        sourceEntries.addAll(entries);
                    }
                    statusLabel.setText(sourceEntries.isEmpty() ? "Player inventory is empty" : "Select items for the match");
                    loading = false;
                    refreshPanels();
                });
            }

            @Override
            public void onError(String message) {
                Gdx.app.postRunnable(() -> {
                    if (disposed) {
                        return;
                    }
                    statusLabel.setText(message);
                    loading = false;
                    refreshPanels();
                });
            }
        });
    }

    private void refreshPanels() {
        rebuildSourcePanel();
        rebuildLoadoutPanel();
    }

    private void rebuildSourcePanel() {
        sourceContainer.clear();
        sourceContainer.top();

        Label header = new Label("Player inventory", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        sourceContainer.add(header).padBottom(10f).row();

        sourceGridTable = new Table();
        for (int i = 0; i < sourceEntries.size(); i++) {
            LootEntry entry = sourceEntries.get(i);
            Table cell = buildSlotCell(resolveItemIcon(entry.getItemCode()), entry.getQuantity(), selectedSourceIndex == i, true);
            final int slotIndex = i;
            final LootEntry slotEntry = entry;
            cell.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    handleSourceCellClick(slotIndex, slotEntry);
                }
            });
            sourceGridTable.add(cell).size(SOURCE_CELL_SIZE, SOURCE_CELL_SIZE).pad(6f);
            if ((i + 1) % SOURCE_COLUMNS == 0) {
                sourceGridTable.row();
            }
        }

        if (sourceEntries.isEmpty()) {
            Label empty = new Label("No saved items yet", new Label.LabelStyle(font, Color.GRAY));
            sourceGridTable.add(empty).padTop(16f);
        }

        sourceScrollPane = new ScrollPane(sourceGridTable);
        sourceScrollPane.setFadeScrollBars(false);
        sourceScrollPane.setScrollingDisabled(true, false);
        sourceScrollPane.setOverscroll(false, false);
        sourceScrollPane.setSmoothScrolling(true);
        sourceContainer.add(sourceScrollPane).width(640f).height(520f).row();
    }

    private void rebuildLoadoutPanel() {
        loadoutContainer.clear();
        loadoutContainer.top();

        Label header = new Label("Loadout", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        loadoutContainer.add(header).padBottom(10f).row();

        Label hint = new Label("Starter weapon is free and always available.", new Label.LabelStyle(font, Color.LIGHT_GRAY));
        loadoutContainer.add(hint).padBottom(12f).row();

        Table mainSection = new Table();
        for (int row = 0; row < InventorySelectionLayoutHelper.MAIN_ROWS; row++) {
            for (int col = 0; col < InventorySelectionLayoutHelper.MAIN_COLUMNS; col++) {
                int slotIndex = row * InventorySelectionLayoutHelper.MAIN_COLUMNS + col;
                ItemStack stack = loadoutSelection.getSlots()[slotIndex];
                Table cell = buildSlotCell(resolveItemIcon(stack), stack == null ? 0 : stack.getAmount(), selectedLoadoutSlot == slotIndex, false);
                final int index = slotIndex;
                cell.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        handleLoadoutCellClick(index);
                    }
                });
                mainSection.add(cell).size(CELL_SIZE, CELL_SIZE).pad(6f);
            }
            mainSection.row();
        }

        Table weaponSection = new Table();
        Label weaponsLabel = new Label("Weapons", new Label.LabelStyle(FontManager.getMenuFont(), Color.WHITE));
        weaponSection.add(weaponsLabel).left().colspan(2).padBottom(6f).row();

        loadoutWeaponTable = new Table();
        for (int i = 0; i < InventorySelectionLayoutHelper.WEAPON_SLOT_COUNT; i++) {
            int slotIndex = InventorySelectionLayoutHelper.MAIN_SLOT_COUNT + i;
            ItemStack stack = loadoutSelection.getSlots()[slotIndex];
            Table cell = buildSlotCell(resolveItemIcon(stack), stack == null ? 0 : stack.getAmount(), selectedLoadoutSlot == slotIndex, loadoutSelection.isLockedSlot(slotIndex));
            final int index = slotIndex;
            cell.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    handleLoadoutCellClick(index);
                }
            });
            loadoutWeaponTable.add(cell).size(CELL_SIZE, CELL_SIZE).pad(6f);
        }
        weaponSection.add(loadoutWeaponTable).padTop(4f).row();

        loadoutContainer.add(mainSection).row();
        loadoutContainer.add(weaponSection).padTop(14f).row();
    }

    private Table buildSlotCell(Texture icon, int amount, boolean selected, boolean locked) {
        Table cell = new Table();
        cell.setBackground(slotDrawable);
        cell.setColor(1f, 1f, 1f, 1f);

        Stack stack = new Stack();
        Image bg = new Image(slotDrawable);
        if (selected) {
            bg.setColor(0.75f, 0.95f, 1f, 1f);
        } else if (locked) {
            bg.setColor(0.9f, 0.9f, 1f, 1f);
        } else {
            bg.setColor(1f, 1f, 1f, 1f);
        }
        stack.add(bg);

        if (icon != null) {
            Image itemImage = new Image(icon);
            itemImage.setScaling(Scaling.fit);
            stack.add(itemImage);
        }

        if (amount > 1) {
            Table overlay = new Table();
            overlay.bottom().right();
            Label qty = new Label(String.valueOf(amount), new Label.LabelStyle(font, Color.WHITE));
            qty.setAlignment(Align.right);
            overlay.add(qty).expand().bottom().right().pad(5f);
            stack.add(overlay);
        }

        cell.add(stack).size(CELL_SIZE, CELL_SIZE);
        return cell;
    }

    private void handleSourceCellClick(int index, LootEntry entry) {
        if (index < 0 || index >= sourceEntries.size() || entry == null) {
            return;
        }

        if (selectedSourceIndex == index && selectedLoadoutSlot < 0) {
            clearSelection();
            return;
        }

        if (selectedLoadoutSlot >= 0) {
            transferLoadoutToSource(selectedLoadoutSlot);
            clearSelection();
            refreshPanels();
            return;
        }

        selectedSourceIndex = index;
        selectedLoadoutSlot = -1;
        selectionLabel.setText(formatSelectionText(entry.getItemCode(), entry.getQuantity(), true));
        refreshPanels();
    }

    private void handleLoadoutCellClick(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= loadoutSelection.getSlots().length) {
            return;
        }

        if (selectedLoadoutSlot == slotIndex && selectedSourceIndex < 0) {
            if (loadoutSelection.isLockedSlot(slotIndex)) {
                statusLabel.setText("Starter weapon cannot be removed");
                return;
            }
            clearSelection();
            refreshPanels();
            return;
        }

        if (selectedSourceIndex >= 0) {
            transferSourceToLoadout(selectedSourceIndex, slotIndex);
            clearSelection();
            refreshPanels();
            return;
        }

        selectedSourceIndex = -1;
        selectedLoadoutSlot = slotIndex;
        ItemStack stack = loadoutSelection.getSlots()[slotIndex];
        if (stack != null && !stack.isEmpty()) {
            selectionLabel.setText(formatSelectionText(stack.getItemTypeId(), stack.getAmount(), false));
        } else {
            selectionLabel.setText("Selected empty loadout slot " + (slotIndex + 1));
        }
        refreshPanels();
    }

    private void transferSourceToLoadout(int sourceIndex, int preferredLoadoutSlot) {
        if (sourceIndex < 0 || sourceIndex >= sourceEntries.size()) {
            return;
        }

        LootEntry entry = sourceEntries.get(sourceIndex);
        ItemKind kind = ItemKind.fromCode(entry.getItemCode());
        if (kind == ItemKind.NONE) {
            statusLabel.setText("Unknown item cannot be moved");
            return;
        }

        int accepted = loadoutSelection.addItem(kind.getId(), entry.getQuantity(), preferredLoadoutSlot);
        if (accepted <= 0) {
            statusLabel.setText("No room in loadout for " + resolveItemName(kind.getId()));
            return;
        }

        int remaining = entry.getQuantity() - accepted;
        if (remaining > 0) {
            sourceEntries.set(sourceIndex, new LootEntry(entry.getItemCode(), remaining));
        } else {
            sourceEntries.remove(sourceIndex);
        }

        selectedSourceIndex = -1;
        selectedLoadoutSlot = -1;
        statusLabel.setText("Added " + resolveItemName(kind.getId()) + " x" + accepted + " to loadout");
    }

    private void transferLoadoutToSource(int slotIndex) {
        ItemStack removed = loadoutSelection.removeStack(slotIndex);
        if (removed == null || removed.isEmpty()) {
            return;
        }

        ItemKind kind = ItemKind.fromId(removed.getItemTypeId());
        if (kind == ItemKind.NONE) {
            statusLabel.setText("Unknown item cannot be returned");
            return;
        }

        addSourceItem(kind.getCode(), removed.getAmount());
        selectedSourceIndex = -1;
        selectedLoadoutSlot = -1;
        statusLabel.setText("Returned " + resolveItemName(kind.getId()) + " x" + removed.getAmount() + " to inventory");
    }

    private void addSourceItem(String itemCode, int amount) {
        if (amount <= 0 || itemCode == null || itemCode.trim().isEmpty()) {
            return;
        }

        String normalized = itemCode.trim();
        for (int i = 0; i < sourceEntries.size(); i++) {
            LootEntry entry = sourceEntries.get(i);
            if (normalized.equalsIgnoreCase(entry.getItemCode())) {
                sourceEntries.set(i, new LootEntry(entry.getItemCode(), entry.getQuantity() + amount));
                return;
            }
        }
        sourceEntries.add(new LootEntry(normalized, amount));
    }

    private void clearSelection() {
        selectedSourceIndex = -1;
        selectedLoadoutSlot = -1;
        selectionLabel.setText("Click an item, then click a destination slot.");
    }

    private void confirmLoadout() {
        if (startingMatch) {
            return;
        }
        startingMatch = true;
        loadoutSelection.ensureStarterWeapon();
        statusLabel.setText("Starting match...");

        try {
            GameClient gameClient = new GameClient(lobbyId, AuthState.getCurrentUsername(), loadoutSelection.toNetworkSlots());
            game.setScreen(new GameScreen((Main) game, gameClient, lobbyInfoClient));
            gameClient.connect();
        } catch (Exception e) {
            startingMatch = false;
            statusLabel.setText("Failed to start match: " + e.getMessage());
        }
    }

    private void backToReturnScreen() {
        if (startingMatch) {
            return;
        }
        if (returnScreen != null) {
            game.setScreen(returnScreen);
        }
    }

    private TextButton createButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.up = new TextureRegionDrawable(buttonTexture);
        style.down = new TextureRegionDrawable(buttonPressedTexture);
        TextButton button = new TextButton(text, style);
        button.getLabel().setAlignment(Align.center);
        return button;
    }

    private Texture resolveItemIcon(LootEntry entry) {
        if (entry == null) {
            return null;
        }
        return resolveItemIcon(entry.getItemCode());
    }

    private Texture resolveItemIcon(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return itemIcons.get(stack.getItemTypeId());
    }

    private Texture resolveItemIcon(String itemCode) {
        ItemKind kind = ItemKind.fromCode(itemCode);
        if (kind == ItemKind.NONE) {
            return null;
        }
        ItemDefinition definition = ItemRegistry.get(kind.getId());
        return definition == null ? null : itemIcons.get(definition.getTypeId());
    }

    private String resolveItemName(int itemTypeId) {
        ItemDefinition definition = ItemRegistry.get(itemTypeId);
        return definition == null ? ItemKind.fromId(itemTypeId).getCode() : definition.getDisplayName();
    }

    private String formatSelectionText(String itemCode, int amount, boolean source) {
        ItemKind kind = ItemKind.fromCode(itemCode);
        String name = kind == ItemKind.NONE ? itemCode : resolveItemName(kind.getId());
        return (source ? "Selected inventory item: " : "Selected loadout item: ") + name + " x" + amount;
    }

    private String formatSelectionText(int itemTypeId, int amount, boolean source) {
        String name = resolveItemName(itemTypeId);
        return (source ? "Selected inventory item: " : "Selected loadout item: ") + name + " x" + amount;
    }

    private Map<Integer, Texture> loadItemIcons() {
        Map<Integer, Texture> icons = new HashMap<>();
        for (ItemDefinition definition : ItemRegistry.all()) {
            String path = definition.getIconPath();
            if (path == null || path.trim().isEmpty()) {
                continue;
            }
            icons.put(definition.getTypeId(), new Texture(Gdx.files.internal(path)));
        }
        return icons;
    }

    @Override
    public void render(float delta) {
        if (disposed) {
            return;
        }

        Gdx.gl.glClearColor(0.2f, 0.2f, 0.3f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        BackgroundRenderer.drawCover(batch, background);
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(dimOverlay, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        batch.end();

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
    @Override public void hide() {}

    @Override
    public void dispose() {
        disposed = true;
        stage.dispose();
        batch.dispose();
        background.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        atlasTexture.dispose();
        dimOverlay.dispose();
        for (Texture icon : itemIcons.values()) {
            icon.dispose();
        }
    }

    /**
     * Helper used to keep the loadout slot layout aligned with the game inventory layout.
     */
    private static final class InventorySelectionLayoutHelper {
        private static final int MAIN_COLUMNS = taltech.orbitgame.inventory.InventoryLayout.MAIN_COLUMNS;
        private static final int MAIN_ROWS = taltech.orbitgame.inventory.InventoryLayout.MAIN_ROWS;
        private static final int MAIN_SLOT_COUNT = taltech.orbitgame.inventory.InventoryLayout.MAIN_SLOT_COUNT;
        private static final int WEAPON_SLOT_COUNT = taltech.orbitgame.inventory.InventoryLayout.WEAPON_SLOT_COUNT;

        private InventorySelectionLayoutHelper() {
        }
    }
}

