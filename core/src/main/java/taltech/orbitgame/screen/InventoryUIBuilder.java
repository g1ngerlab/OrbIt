package taltech.orbitgame.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import taltech.orbitgame.util.FontManager;

/**
 * Builds and manages the UI layout for InventoryMenuScreen
 * Extracted to keep InventoryMenuScreen focused on game logic
 */
public class InventoryUIBuilder {
    private final Stage stage;
    private final Texture buttonTexture;
    private final Texture buttonPressedTexture;
    private final TextureRegion slotRegion;
    private final BitmapFont font;

    private Label statusLabel;
    private Table inventoryGridTable;
    private ScrollPane gridScrollPane;
    private Slider scrollSlider;
    private TextButton deleteButton;
    private Table deleteDialogOverlay;
    private TextField deleteAmountField;

    public InventoryUIBuilder(Stage stage, Texture buttonTexture, Texture buttonPressedTexture,
                              TextureRegion slotRegion, BitmapFont font) {
        this.stage = stage;
        this.buttonTexture = buttonTexture;
        this.buttonPressedTexture = buttonPressedTexture;
        this.slotRegion = slotRegion;
        this.font = font;
    }

    /**
     * Build the main UI for inventory screen
     */
    public void buildMainUI() {
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

        buildInventoryGridUI(scrollRow);

        Table actionsRow = new Table();
        deleteButton = createButton("DELETE");
        deleteButton.setDisabled(true);
        deleteButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (inventoryGridTable.getChildren().size > 0) {
                    // Will be handled by listener in screen
                }
            }
        });
        actionsRow.add(deleteButton).size(300f, 60f).padRight(20f);

        TextButton backButton = createButton("BACK");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Will be handled by listener in screen
            }
        });
        actionsRow.add(backButton).size(300f, 60f);

        root.add(actionsRow).padTop(20f).row();
    }

    /**
     * Build the grid UI for inventory items
     */
    private void buildInventoryGridUI(Table scrollRow) {
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
    }

    /**
     * Build the delete amount dialog
     */
    public void buildDeleteAmountDialog() {
        if (deleteDialogOverlay != null) {
            deleteDialogOverlay.remove();
        }

        deleteDialogOverlay = new Table();
        deleteDialogOverlay.setFillParent(true);

        // Create a semi-transparent overlay
        Pixmap dimPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        dimPixmap.setColor(0f, 0f, 0f, 0.5f);
        dimPixmap.fill();
        Texture dimTexture = new Texture(dimPixmap);
        dimPixmap.dispose();

        deleteDialogOverlay.setBackground(new TextureRegionDrawable(new TextureRegion(dimTexture)));
        stage.addActor(deleteDialogOverlay);

        Table dialogBox = new Table();
        deleteDialogOverlay.add(dialogBox).width(400f).height(250f);

        Label prompt = new Label("Delete amount:", new Label.LabelStyle(font, Color.WHITE));
        dialogBox.add(prompt).padBottom(15f).row();

        deleteAmountField = new TextField("1", new TextField.TextFieldStyle(font, Color.WHITE,
            new TextureRegionDrawable(slotRegion), new TextureRegionDrawable(slotRegion),
            new TextureRegionDrawable(slotRegion)));
        deleteAmountField.setMaxLength(5);
        dialogBox.add(deleteAmountField).width(200f).height(40f).padBottom(20f).row();

        TextButton confirmButton = createButton("CONFIRM");
        TextButton cancelButton = createButton("CANCEL");
        Table buttonRow = new Table();
        buttonRow.add(confirmButton).width(150f).height(50f).padRight(10f);
        buttonRow.add(cancelButton).width(150f).height(50f);
        dialogBox.add(buttonRow).padTop(20f).row();
    }

    /**
     * Close the delete amount dialog
     */
    public void closeDeleteAmountDialog() {
        if (deleteDialogOverlay != null) {
            deleteDialogOverlay.remove();
            deleteDialogOverlay = null;
        }
    }

    /**
     * Create a styled button
     */
    private TextButton createButton(String text) {
        TextButton.TextButtonStyle style = new TextButton.TextButtonStyle();
        style.font = font;
        style.fontColor = Color.WHITE;
        style.up = new TextureRegionDrawable(new TextureRegion(buttonTexture));
        style.down = new TextureRegionDrawable(new TextureRegion(buttonPressedTexture));
        return new TextButton(text, style);
    }

    // Getters
    public Label getStatusLabel() {
        return statusLabel;
    }

    public Table getInventoryGridTable() {
        return inventoryGridTable;
    }

    public ScrollPane getGridScrollPane() {
        return gridScrollPane;
    }

    public Slider getScrollSlider() {
        return scrollSlider;
    }

    public TextButton getDeleteButton() {
        return deleteButton;
    }

    public Table getDeleteDialogOverlay() {
        return deleteDialogOverlay;
    }

    public TextField getDeleteAmountField() {
        return deleteAmountField;
    }

    public void setStatusLabel(Label label) {
        this.statusLabel = label;
    }

    public void setDeleteDialogOverlay(Table overlay) {
        this.deleteDialogOverlay = overlay;
    }
}
