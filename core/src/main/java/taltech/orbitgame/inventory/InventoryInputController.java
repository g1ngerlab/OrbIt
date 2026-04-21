package taltech.orbitgame.inventory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;

/**
 * Handles inventory-specific input only.
 */
public class InventoryInputController {
    @FunctionalInterface
    public interface SlotResolver {
        int resolve(int screenX, int screenY);
    }

    @FunctionalInterface
    public interface DropAction {
        void dropFromSlot(int slotIndex);
    }

    @FunctionalInterface
    public interface MoveAction {
        void move(int fromSlotIndex, int toSlotIndex);
    }

    private boolean prevLeftMouseDown;

    public void update(boolean isInLobby,
                       boolean isPaused,
                       InventoryState state,
                       SlotResolver slotResolver,
                       DropAction dropAction,
                       MoveAction moveAction) {
        if (state == null || slotResolver == null || dropAction == null || moveAction == null
            || !isInLobby || isPaused) {
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            state.toggleVisible();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            state.selectActiveWeaponSlot(0);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            state.selectActiveWeaponSlot(1);
        }

        boolean leftDown = Gdx.input.isButtonPressed(Input.Buttons.LEFT);
        if (state.isVisible() && leftDown && !prevLeftMouseDown) {
            int slot = slotResolver.resolve(Gdx.input.getX(), Gdx.input.getY());
            if (slot >= 0) {
                state.beginDragging(slot);
            }
        }

        if (state.isVisible() && !leftDown && prevLeftMouseDown && state.isDragging()) {
            int toSlot = slotResolver.resolve(Gdx.input.getX(), Gdx.input.getY());
            int fromSlot = state.getDraggedFromSlot();
            if (fromSlot >= 0 && toSlot >= 0 && toSlot != fromSlot) {
                moveAction.move(fromSlot, toSlot);
            }
            state.clearDragging();
        }

        prevLeftMouseDown = leftDown;

        if (state.isVisible() && state.hasDroppableSelection() && Gdx.input.isKeyJustPressed(Input.Keys.G)) {
            int selectedSlot = state.getSelectedSlot();
            dropAction.dropFromSlot(selectedSlot);
        }
    }
}

