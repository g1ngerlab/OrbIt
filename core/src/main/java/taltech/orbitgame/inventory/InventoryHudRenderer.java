package taltech.orbitgame.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import taltech.orbitgame.item.ItemDefinition;
import taltech.orbitgame.item.ItemRegistry;
import taltech.orbitgame.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders inventory panel and resolves slot hit-tests.
 */
public class InventoryHudRenderer {
    private static final float CELL_SIZE = 84f;
    private static final float CELL_SPACING = 16f;
    private static final float PAD_X = 18f;
    private static final float PAD_Y = 16f;
    private static final float LEFT_MARGIN = 16f;
    private static final float TOP_MARGIN = 18f;
    private static final float BG_ALPHA = 0.8f;
    private static final float SLIDE_SPEED = 11f;
    private static final float ITEM_ICON_SCALE = 0.74f;
    private static final float HINT_FONT_SCALE = 0.9f;
    private static final float SECTION_GAP = 18f;
    private static final float LABEL_FONT_SCALE = 0.75f;
    private static final int TILE_SIZE = 64;
    private static final int TILE_COLUMN = 2;
    private static final int TILE_ROW_FROM_TOP = 6;

    private final Texture atlasTexture;
    private final TextureRegion cellRegion;
    private final Map<Integer, Texture> itemIcons;

    private float panelY;
    private float lastX;
    private float lastY;
    private float lastWidth;
    private float lastHeight;

    public InventoryHudRenderer() {
        atlasTexture = new Texture(Gdx.files.internal("UI/inventory_border.png"));
        cellRegion = new TextureRegion(
            atlasTexture,
            (TILE_COLUMN - 1) * TILE_SIZE,
            (TILE_ROW_FROM_TOP - 1) * TILE_SIZE,
            TILE_SIZE,
            TILE_SIZE
        );
        itemIcons = loadItemIcons();
        panelY = Gdx.graphics.getHeight() + 220f;
    }

    public void render(SpriteBatch batch,
                       ShapeRenderer shapeRenderer,
                       OrthographicCamera hudCamera,
                       BitmapFont hudFont,
                       float delta,
                       InventoryState state) {
        if (state == null) {
            return;
        }

        float step = CELL_SIZE + CELL_SPACING;
        float mainGridWidth = InventoryLayout.MAIN_COLUMNS * CELL_SIZE
            + (InventoryLayout.MAIN_COLUMNS - 1) * CELL_SPACING;
        float weaponGridWidth = InventoryLayout.WEAPON_SLOT_COUNT * CELL_SIZE
            + (InventoryLayout.WEAPON_SLOT_COUNT - 1) * CELL_SPACING;
        float panelWidth = PAD_X * 2f + Math.max(mainGridWidth, weaponGridWidth);
        float panelHeight = PAD_Y * 2f
            + InventoryLayout.MAIN_ROWS * CELL_SIZE
            + (InventoryLayout.MAIN_ROWS - 1) * CELL_SPACING
            + SECTION_GAP
            + CELL_SIZE;
        float targetY = state.isVisible()
            ? Gdx.graphics.getHeight() - TOP_MARGIN - panelHeight
            : Gdx.graphics.getHeight() + panelHeight + 14f;

        panelY += (targetY - panelY) * Math.min(1f, delta * SLIDE_SPEED);
        float panelX = LEFT_MARGIN;

        lastX = panelX;
        lastY = panelY;
        lastWidth = panelWidth;
        lastHeight = panelHeight;

        batch.begin();

        batch.setColor(0f, 0f, 0f, BG_ALPHA);
        batch.draw(cellRegion, panelX - 8f, panelY - 8f, panelWidth + 16f, panelHeight + 16f);
        batch.setColor(1f, 1f, 1f, 1f);

        ItemStack[] slots = state.getSlots();
        float mainSectionHeight = InventoryLayout.MAIN_ROWS * CELL_SIZE
            + (InventoryLayout.MAIN_ROWS - 1) * CELL_SPACING;
        float weaponSectionY = panelY + PAD_Y + mainSectionHeight + SECTION_GAP;

        for (int row = 0; row < InventoryLayout.MAIN_ROWS; row++) {
            for (int col = 0; col < InventoryLayout.MAIN_COLUMNS; col++) {
                int slotIndex = row * InventoryLayout.MAIN_COLUMNS + col;
                float slotX = panelX + PAD_X + col * step;
                float slotY = panelY + PAD_Y + row * step;

                batch.draw(cellRegion, slotX, slotY, CELL_SIZE, CELL_SIZE);

                ItemStack stack = slots[slotIndex];
                if (state.isDragging() && slotIndex == state.getDraggedFromSlot()) {
                    stack = null;
                }
                if (stack != null && stack.getAmount() > 0) {
                    float itemSize = CELL_SIZE * ITEM_ICON_SCALE;
                    float itemX = slotX + (CELL_SIZE - itemSize) * 0.5f;
                    float itemY = slotY + (CELL_SIZE - itemSize) * 0.5f;
                    Texture icon = itemIcons.get(stack.getItemTypeId());
                    if (icon != null) {
                        batch.draw(icon, itemX, itemY, itemSize, itemSize);
                    } else {
                        hudFont.draw(batch, "?", itemX + itemSize * 0.4f, itemY + itemSize * 0.7f);
                    }

                    if (stack.getAmount() > 1) {
                        hudFont.draw(batch,
                            String.valueOf(stack.getAmount()),
                            slotX + CELL_SIZE - 20f,
                            slotY + 18f);
                    }
                }
            }
        }

        hudFont.getData().setScale(LABEL_FONT_SCALE);
        hudFont.draw(batch, "WEAPON", panelX + PAD_X, weaponSectionY + CELL_SIZE + 12f);
        hudFont.getData().setScale(1f);

        for (int i = 0; i < InventoryLayout.WEAPON_SLOT_COUNT; i++) {
            int slotIndex = InventoryLayout.MAIN_SLOT_COUNT + i;
            float slotX = panelX + PAD_X + i * step;
            float slotY = weaponSectionY;

            batch.draw(cellRegion, slotX, slotY, CELL_SIZE, CELL_SIZE);
            ItemStack stack = slots[slotIndex];
            if (state.isDragging() && slotIndex == state.getDraggedFromSlot()) {
                stack = null;
            }
            if (stack != null && stack.getAmount() > 0) {
                float itemSize = CELL_SIZE * ITEM_ICON_SCALE;
                float itemX = slotX + (CELL_SIZE - itemSize) * 0.5f;
                float itemY = slotY + (CELL_SIZE - itemSize) * 0.5f;
                Texture icon = itemIcons.get(stack.getItemTypeId());
                if (icon != null) {
                    batch.draw(icon, itemX, itemY, itemSize, itemSize);
                } else {
                    hudFont.draw(batch, "?", itemX + itemSize * 0.4f, itemY + itemSize * 0.7f);
                }
                if (stack.getAmount() > 1) {
                    hudFont.draw(batch, String.valueOf(stack.getAmount()), slotX + CELL_SIZE - 20f, slotY + 18f);
                }
            }
        }

        hudFont.getData().setScale(HINT_FONT_SCALE);
        hudFont.draw(batch, "TAB - inventory, 1/2 - weapon slots, G - drop selected", panelX, panelY - 10f);
        hudFont.getData().setScale(1f);

        if (state.isDragging() && state.getDraggedStack() != null) {
            ItemStack dragged = state.getDraggedStack();
            Texture icon = itemIcons.get(dragged.getItemTypeId());
            float itemSize = CELL_SIZE * ITEM_ICON_SCALE;
            float drawX = Gdx.input.getX() - itemSize * 0.5f;
            float drawY = Gdx.graphics.getHeight() - Gdx.input.getY() - itemSize * 0.5f;
            if (icon != null) {
                batch.draw(icon, drawX, drawY, itemSize, itemSize);
            }
            if (dragged.getAmount() > 1) {
                hudFont.draw(batch, String.valueOf(dragged.getAmount()), drawX + itemSize - 16f, drawY + 18f);
            }
        }

        batch.end();

        int selectedSlot = state.getSelectedSlot();
        if (selectedSlot >= 0 && selectedSlot < InventoryState.SLOT_COUNT) {
            float selectedX;
            float selectedY;
            if (InventoryLayout.isWeaponSlot(selectedSlot)) {
                int weaponIndex = selectedSlot - InventoryLayout.MAIN_SLOT_COUNT;
                selectedX = panelX + PAD_X + weaponIndex * step;
                selectedY = weaponSectionY;
            } else {
                int selectedRow = selectedSlot / InventoryLayout.MAIN_COLUMNS;
                int selectedCol = selectedSlot % InventoryLayout.MAIN_COLUMNS;
                selectedX = panelX + PAD_X + selectedCol * step;
                selectedY = panelY + PAD_Y + selectedRow * step;
            }

            shapeRenderer.setProjectionMatrix(hudCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(1f, 0.9f, 0.25f, 1f);
            shapeRenderer.rect(selectedX - 2f, selectedY - 2f, CELL_SIZE + 4f, CELL_SIZE + 4f);
            shapeRenderer.end();
        }
    }

    public int getSlotAtScreenPosition(int screenX, int screenY, InventoryState state) {
        if (state == null || !state.isVisible()) {
            return -1;
        }

        float hudY = (float) Gdx.graphics.getHeight() - screenY;
        if (screenX < lastX || screenX > lastX + lastWidth || hudY < lastY || hudY > lastY + lastHeight) {
            return -1;
        }

        float localX = screenX - (lastX + PAD_X);
        float localY = hudY - (lastY + PAD_Y);
        float step = CELL_SIZE + CELL_SPACING;
        float mainSectionHeight = InventoryLayout.MAIN_ROWS * CELL_SIZE
            + (InventoryLayout.MAIN_ROWS - 1) * CELL_SPACING;

        float weaponLocalYStart = mainSectionHeight + SECTION_GAP;

        int weaponCol = (int) (localX / step);
        if (weaponCol >= 0 && weaponCol < InventoryLayout.WEAPON_SLOT_COUNT) {
            float inCellX = localX - weaponCol * step;
            float inCellY = localY - weaponLocalYStart;
            if (inCellY >= 0f && inCellY <= CELL_SIZE && inCellX >= 0f && inCellX <= CELL_SIZE) {
                return InventoryLayout.MAIN_SLOT_COUNT + weaponCol;
            }
        }

        float mainLocalY = localY;
        int col = (int) (localX / step);
        int row = (int) (mainLocalY / step);
        if (col < 0 || col >= InventoryLayout.MAIN_COLUMNS || row < 0 || row >= InventoryLayout.MAIN_ROWS) {
            return -1;
        }

        float inCellX = localX - col * step;
        float inCellY = mainLocalY - row * step;
        if (inCellX > CELL_SIZE || inCellY > CELL_SIZE || inCellX < 0f || inCellY < 0f) {
            return -1;
        }

        return row * InventoryLayout.MAIN_COLUMNS + col;
    }

    public boolean isInsidePanel(int screenX, int screenY, InventoryState state) {
        if (state == null || !state.isVisible()) {
            return false;
        }

        float hudY = (float) Gdx.graphics.getHeight() - screenY;
        return screenX >= lastX
            && screenX <= lastX + lastWidth
            && hudY >= lastY
            && hudY <= lastY + lastHeight;
    }

    public void dispose() {
        for (Texture icon : itemIcons.values()) {
            icon.dispose();
        }
        atlasTexture.dispose();
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
}

