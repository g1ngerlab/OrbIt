package taltech.orbitgame;

import com.badlogic.gdx.Game;
import taltech.orbitgame.audio.AudioManager;
import taltech.orbitgame.screen.ConnectionScreen;

/**
 * Extending the Game class in here is very important for multiple screen
 * support (title screen, lobby screen, etc.) in the game.
 */
public class Main extends Game {
    private final Runnable introVideoPlayer;

    public Main() {
        this(null);
    }

    public Main(Runnable introVideoPlayer) {
        this.introVideoPlayer = introVideoPlayer;
    }

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

    public void playIntroVideoBeforeMatch() {
        if (introVideoPlayer != null) {
            introVideoPlayer.run();
        } else {
            System.err.println("[IntroVideo] No intro video player is configured for this platform");
        }
    }
}
