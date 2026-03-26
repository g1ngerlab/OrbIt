package taltech.orbitgame;

import com.badlogic.gdx.Game;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.screen.ConnectionScreen;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class Main extends Game {

    @Override
    public void create() {
        // Start with connection screen to connect to server
        setScreen(new ConnectionScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
        // Properly dispose audio resources
        AudioManager.getInstance().dispose();
    }
}
