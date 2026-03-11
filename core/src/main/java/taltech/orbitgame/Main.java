package taltech.orbitgame;

import com.badlogic.gdx.Game;
import taltech.orbitgame.screen.MainMenuScreen;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class Main extends Game {

    @Override
    public void create() {
        // Start with the main menu screen
        setScreen(new MainMenuScreen(this));
    }
}
