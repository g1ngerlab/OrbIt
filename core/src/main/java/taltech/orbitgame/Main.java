package taltech.orbitgame;

import com.badlogic.gdx.Game;
import taltech.orbitgame.client.GameClient;
import taltech.orbitgame.screen.GameScreen;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class Main extends Game {

    @Override
    public void create() {
        // Initialize network client
        GameClient gameClient = new GameClient();
        gameClient.connect();

        // Display the game screen and pass network client
        setScreen(new GameScreen(gameClient));
    }
}
