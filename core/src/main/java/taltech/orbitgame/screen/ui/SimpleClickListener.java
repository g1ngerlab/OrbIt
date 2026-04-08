package taltech.orbitgame.screen.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

/**
 * Wraps TextButton.ClickListener creation to simplify button callbacks.
 */
public class SimpleClickListener extends ClickListener {
    private final Runnable onClicked;

    public SimpleClickListener(Runnable onClicked) {
        this.onClicked = onClicked;
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        onClicked.run();
    }
}

