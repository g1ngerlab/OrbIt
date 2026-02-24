package taltech.orbitgame.client;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import taltech.orbitgame.network.Network.InputState;

public class InputHandler implements InputProcessor {
    private final InputState state = new InputState();

    public InputHandler() {
    }

    public InputState getState() {
        return state;
    }

    /** Returns true if any key or mouse button is currently held down. */
    public boolean hasActiveInput() {
        return state.w || state.a || state.s || state.d
            || state.leftClick || state.rightClick || state.mouseClicked;
    }

    public boolean stateGetter(String key) {
        switch (key.toLowerCase()) {
            case "w": return state.w;
            case "a": return state.a;
            case "s": return state.s;
            case "d": return state.d;
            case "mouseclicked": return state.mouseClicked;
            default: return false;
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.W: state.w = true; break;
            case Input.Keys.A: state.a = true; break;
            case Input.Keys.S: state.s = true; break;
            case Input.Keys.D: state.d = true; break;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        switch (keycode) {
            case Input.Keys.W: state.w = false; break;
            case Input.Keys.A: state.a = false; break;
            case Input.Keys.S: state.s = false; break;
            case Input.Keys.D: state.d = false; break;
        }
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        state.mouseClicked = true;
        state.mouseX = screenX;
        state.mouseY = screenY;
        if (button == Input.Buttons.LEFT) state.leftClick = true;
        if (button == Input.Buttons.RIGHT) state.rightClick = true;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        state.mouseClicked = false;
        if (button == Input.Buttons.LEFT) state.leftClick = false;
        if (button == Input.Buttons.RIGHT) state.rightClick = false;
        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        state.mouseClicked = false;
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        state.mouseX = screenX;
        state.mouseY = screenY;
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        state.mouseX = screenX;
        state.mouseY = screenY;
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }
}
