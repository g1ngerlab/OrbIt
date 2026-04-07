package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import taltech.orbitgame.Main;
import taltech.orbitgame.client.LobbyInfoClient;
import taltech.orbitgame.item.ItemDefinition;
import taltech.orbitgame.item.ItemKind;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.loot.LootEntry;
import taltech.orbitgame.loot.PlayerLootService;
import taltech.orbitgame.rendering.BackgroundRenderer;
import taltech.orbitgame.util.FontManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main-menu screen for account inventory loaded from Supabase.
 */
public class InventoryMenuScreen implements Screen {
    private static final int GRID_COLUMNS = 5;
    private static final int GRID_ROWS = 20;
    private static final int SLOT_COUNT = GRID_COLUMNS * GRID_ROWS;
    private static final int TILE_SIZE = 64;
    private static final int TILE_COLUMN = 2;
    private static final int TILE_ROW_FROM_TOP = 6;
    private static final float CELL_SIZE = 98f;

    private final Main game;
    private final LobbyInfoClient lobbyInfoClient;
    private final PlayerLootService lootService;

    private Stage stage;
    private SpriteBatch batch;
    private Texture background;
    private Texture buttonTexture;
    private Texture buttonPressedTexture;
    private Texture inventoryAtlas;
    private Texture dimOverlay;
    private BitmapFont font;

    private Label statusLabel;
    private Table inventoryGridTable;
    private ScrollPane gridScrollPane;
    private Slider scrollSlider;
    private TextureRegion slotRegion;
    private Map<Integer, Texture> itemIcons;
    private List<LootEntry> currentEntries;
    private int selectedSlotIndex = -1;
    private TextButton deleteButton;
    private Table deleteDialogOverlay;
    private TextField deleteAmountField;
    private LootEntry pendingDeleteEntry;

    public InventoryMenuScreen(Main game, LobbyInfoClient lobbyInfoClient) {
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
        Pixmap dimPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPixmap.setColor(0f, 0f, 0f, 0.55f);
        dimPixmap.fill();
        dimOverlay = new Texture(dimPixmap);
        dimPixmap.dispose();
        slotRegion = new TextureRegion(
            inventoryAtlas,
            (TILE_COLUMN - 1) * TILE_SIZE,
            (TILE_ROW_FROM_TOP - 1) * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        itemIcons = loadItemIcons();
        font = FontManager.getMenuFont();

        buildUi();
        loadLootFromSupabase();
    }

    private void buildUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.top();
        root.padTop(40f);
        stage.addActor(root);

        Label title = new Label("Inventory", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        root.add(title).padBottom(20f).row();

        statusLabel = new Label("Loading items...", new Label.LabelStyle(font, Color.YELLOW));
        root.add(statusLabel).padBottom(18f).row();

        Table scrollRow = new Table();
        root.add(scrollRow).width(760f).height(560f).padBottom(24f).row();

        inventoryGridTable = new Table();

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        gridScrollPane = new ScrollPane(inventoryGridTable, scrollStyle);
        gridScrollPane.setFadeScrollBars(false);
        gridScrollPane.setScrollingDisabled(true, false);
        gridScrollPane.setSmoothScrolling(true);
        gridScrollPane.setOverscroll(false, false);

        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = new TextureRegionDrawable(slotRegion);
        sliderStyle.knob = new TextureRegionDrawable(slotRegion);
        sliderStyle.knobBefore = new TextureRegionDrawable(slotRegion);
        scrollSlider = new Slider(0f, 1f, 0.001f, true, sliderStyle);
        scrollSlider.setVisible(false);

        scrollRow.add(gridScrollPane).width(700f).height(560f).padRight(12f);
        scrollRow.add(scrollSlider).width(24f).height(560f);

        Table actionsRow = new Table();

        deleteButton = createButton("DELETE");
        deleteButton.setDisabled(true);
        deleteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                deleteSelectedItem();
            }
        });

        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game, lobbyInfoClient));
            }
        });

        actionsRow.add(deleteButton).width(280f).height(98f).padRight(18f);
        actionsRow.add(backButton).width(280f).height(98f);
        root.add(actionsRow);

        bindScrolling();
        stage.setScrollFocus(gridScrollPane);
    }

    private void loadLootFromSupabase() {
        lootService.loadPlayerLoot(new PlayerLootService.LoadCallback() {
            @Override
            public void onSuccess(List<LootEntry> entries) {
                currentEntries = new ArrayList<>(entries == null ? new ArrayList<>() : entries);
                selectedSlotIndex = -1;
                renderLootRows(currentEntries);
                refreshDeleteButtonState();
            }

            @Override
            public void onError(String message) {
                statusLabel.setText(message);
                inventoryGridTable.clearChildren();
            }
        });
    }

    private void renderLootRows(List<LootEntry> entries) {
        inventoryGridTable.clearChildren();

        int itemSlotsUsed = entries == null ? 0 : Math.min(entries.size(), SLOT_COUNT);
        if (itemSlotsUsed == 0) {
            statusLabel.setText("Inventory is empty");
        } else {
            statusLabel.setText("Saved items: " + itemSlotsUsed + " / " + SLOT_COUNT);
        }

        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            LootEntry entry = (entries != null && slot < entries.size()) ? entries.get(slot) : null;
            Table slotCell = createSlotCell(slot, entry);
            inventoryGridTable.add(slotCell).size(CELL_SIZE, CELL_SIZE).pad(7f);
            if ((slot + 1) % GRID_COLUMNS == 0) {
                inventoryGridTable.row();
            }
        }

        gridScrollPane.layout();
        updateSliderRange();
    }

    private String resolveItemName(String itemCode) {
        ItemKind kind = ItemKind.fromCode(itemCode);
        if (kind == ItemKind.NONE) {
            return itemCode;
        }
        ItemDefinition definition = ItemRegistry.get(kind.getId());
        return definition == null ? itemCode : definition.getDisplayName();
    }

    private Table createSlotCell(int slotIndex, LootEntry entry) {
        Table slot = new Table();
        slot.setBackground(new TextureRegionDrawable(slotRegion));

        if (slotIndex == selectedSlotIndex) {
            slot.setColor(0.75f, 0.95f, 1f, 1f);
        } else {
            slot.setColor(1f, 1f, 1f, 1f);
        }

        slot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                selectedSlotIndex = slotIndex;
                renderLootRows(currentEntries);
                refreshDeleteButtonState();
                if (selectedSlotIndex >= 0 && currentEntries != null && selectedSlotIndex < currentEntries.size()) {
                    LootEntry selected = currentEntries.get(selectedSlotIndex);
                    statusLabel.setText("Selected: " + resolveItemName(selected.getItemCode()) + " x" + selected.getQuantity());
                }
            }
        });

        if (entry == null) {
            return slot;
        }

        ItemKind kind = ItemKind.fromCode(entry.getItemCode());
        Texture icon = itemIcons.get(kind.getId());
        if (icon != null) {
            slot.add(new Image(icon)).size(CELL_SIZE * 0.64f, CELL_SIZE * 0.64f).padTop(4f).row();
        } else {
            Label unknown = new Label("?", new Label.LabelStyle(font, Color.WHITE));
            slot.add(unknown).padTop(8f).row();
        }

        Label qty = new Label("x" + Math.max(0, entry.getQuantity()), new Label.LabelStyle(font, Color.WHITE));
        qty.setFontScale(0.55f);
        slot.add(qty).padTop(-2f);
        return slot;
    }

    private void deleteSelectedItem() {
        if (deleteButton == null || deleteButton.isDisabled()) {
            return;
        }
        if (currentEntries == null || selectedSlotIndex < 0 || selectedSlotIndex >= currentEntries.size()) {
            return;
        }

        openDeleteAmountDialog(currentEntries.get(selectedSlotIndex));
    }

    private void openDeleteAmountDialog(LootEntry entry) {
        closeDeleteAmountDialog();
        pendingDeleteEntry = entry;

        deleteDialogOverlay = new Table();
        deleteDialogOverlay.setFillParent(true);
        deleteDialogOverlay.setBackground(new TextureRegionDrawable(new TextureRegion(dimOverlay)));

        Table dialog = new Table();
        dialog.setBackground(new TextureRegionDrawable(slotRegion));
        dialog.pad(26f);

        String itemName = resolveItemName(entry.getItemCode());
        Label title = new Label("Delete items", new Label.LabelStyle(FontManager.getMenuFontLarge(), Color.WHITE));
        title.setFontScale(0.7f);
        dialog.add(title).padBottom(10f).row();

        Label info = new Label(itemName + " (max " + entry.getQuantity() + ")", new Label.LabelStyle(font, Color.WHITE));
        info.setFontScale(0.75f);
        dialog.add(info).padBottom(12f).row();

        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = font;
        fieldStyle.fontColor = Color.WHITE;
        TextureRegionDrawable inputBg = new TextureRegionDrawable(slotRegion);
        inputBg.setLeftWidth(16f);
        inputBg.setRightWidth(10f);
        fieldStyle.background = inputBg;
        deleteAmountField = new TextField("1", fieldStyle);
        deleteAmountField.setAlignment(Align.center);
        deleteAmountField.setMessageText("Amount");
        deleteAmountField.setMaxLength(6);
        dialog.add(deleteAmountField).width(220f).height(74f).padBottom(14f).row();

        Table buttons = new Table();
        TextButton confirmButton = createButton("CONFIRM");
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirmDeleteAmount();
            }
        });

        TextButton cancelButton = createButton("CANCEL");
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeDeleteAmountDialog();
            }
        });

        buttons.add(confirmButton).width(200f).height(86f).padRight(10f);
        buttons.add(cancelButton).width(200f).height(86f);
        dialog.add(buttons);

        deleteDialogOverlay.add(dialog).width(540f);
        stage.addActor(deleteDialogOverlay);
        stage.setKeyboardFocus(deleteAmountField);
    }

    private void confirmDeleteAmount() {
        if (pendingDeleteEntry == null || deleteAmountField == null) {
            return;
        }

        int max = Math.max(1, pendingDeleteEntry.getQuantity());
        int requestedAmount;
        try {
            requestedAmount = Integer.parseInt(deleteAmountField.getText().trim());
        } catch (Exception e) {
            statusLabel.setText("Enter a valid number");
            return;
        }

        int amount = Math.max(1, Math.min(max, requestedAmount));
        statusLabel.setText("Deleting " + amount + " x " + resolveItemName(pendingDeleteEntry.getItemCode()) + "...");
        deleteButton.setDisabled(true);

        String itemCode = pendingDeleteEntry.getItemCode();
        int localIndex = selectedSlotIndex;
        lootService.deleteLootItemAmount(itemCode, amount, new PlayerLootService.SaveCallback() {
            @Override
            public void onSuccess() {
                applyLocalDelete(localIndex, amount);
                closeDeleteAmountDialog();
                renderLootRows(currentEntries);
                refreshDeleteButtonState();
                statusLabel.setText("Deleted " + amount + " item(s)");
            }

            @Override
            public void onError(String message) {
                refreshDeleteButtonState();
                statusLabel.setText(message);
            }
        });
    }

    private void applyLocalDelete(int index, int amount) {
        if (currentEntries == null || index < 0 || index >= currentEntries.size()) {
            return;
        }

        LootEntry entry = currentEntries.get(index);
        int remaining = Math.max(0, entry.getQuantity() - Math.max(0, amount));
        if (remaining <= 0) {
            currentEntries.remove(index);
            selectedSlotIndex = -1;
        } else {
            currentEntries.set(index, new LootEntry(entry.getItemCode(), remaining));
        }
    }

    private void closeDeleteAmountDialog() {
        if (deleteDialogOverlay != null) {
            deleteDialogOverlay.remove();
            deleteDialogOverlay = null;
        }
        pendingDeleteEntry = null;
        deleteAmountField = null;
        stage.setKeyboardFocus(null);
    }

    private void refreshDeleteButtonState() {
        if (deleteButton == null) {
            return;
        }
        boolean canDelete = currentEntries != null
            && selectedSlotIndex >= 0
            && selectedSlotIndex < currentEntries.size();
        deleteButton.setDisabled(!canDelete);
    }

    private void bindScrolling() {
        gridScrollPane.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent event,
                                com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (!scrollSlider.isVisible()) {
                    return;
                }
                float maxY = Math.max(0f, gridScrollPane.getMaxY());
                float ratio = maxY <= 0f ? 0f : gridScrollPane.getScrollY() / maxY;
                scrollSlider.setValue(ratio);
            }
        });

        scrollSlider.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            @Override
            public void changed(com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent event,
                                com.badlogic.gdx.scenes.scene2d.Actor actor) {
                float maxY = Math.max(0f, gridScrollPane.getMaxY());
                gridScrollPane.setScrollY(maxY * scrollSlider.getValue());
            }
        });
    }

    private void updateSliderRange() {
        float maxY = Math.max(0f, gridScrollPane.getMaxY());
        boolean scrollable = maxY > 0.5f;
        scrollSlider.setVisible(scrollable);
        if (!scrollable) {
            scrollSlider.setValue(0f);
            gridScrollPane.setScrollY(0f);
        }
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

    private TextButton createButton(String text) {
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = new TextureRegionDrawable(buttonTexture);
        buttonStyle.down = new TextureRegionDrawable(buttonPressedTexture);
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        return new TextButton(text, buttonStyle);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
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
        stage.setScrollFocus(gridScrollPane);
        updateSliderRange();
    }

    @Override
    public void pause() {
        // No-op: no pause-specific logic is needed for this menu screen.
    }

    @Override
    public void resume() {
        // No-op: UI state is static and restored automatically by Scene2D.
    }

    @Override
    public void hide() {
        // No-op: all resources are released in dispose().
    }

    @Override
    public void dispose() {
        stage.dispose();
        batch.dispose();
        background.dispose();
        buttonTexture.dispose();
        buttonPressedTexture.dispose();
        if (inventoryAtlas != null) {
            inventoryAtlas.dispose();
        }
        if (dimOverlay != null) {
            dimOverlay.dispose();
        }
        if (itemIcons != null) {
            for (Texture icon : itemIcons.values()) {
                icon.dispose();
            }
            itemIcons.clear();
        }
    }
}

