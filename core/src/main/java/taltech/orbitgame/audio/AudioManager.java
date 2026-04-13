package taltech.orbitgame.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import java.util.Random;

/**
 * Singleton for managing background music across the game.
 * Sound effects are delegated to SoundEffectManager.
 * Handles loading, playing, and disposing of music tracks.
 */
public class AudioManager {
    private static AudioManager instance;
    private SoundEffectManager soundEffectManager;
    private Music currentMusic;
    private float volume = 0.2f;
    private Random random;

    // Music tracks
    private Music[] lobbyTracks;
    private Music[] gameSessionTracks;

    private static final String[] LOBBY_MUSIC_PATHS = {
        "LobbyMusic/Cyberia lyr2-132369550.mp3",
        "LobbyMusic/I Need To Feel Loved-1935759142.mp3",
        "LobbyMusic/joon-1493758854.mp3"
    };
    private static final String[] GAME_SESSION_MUSIC_PATHS = {
        "GameSeshMusic/Knife Party - 'Bonfire'-419194644.mp3",
        "GameSeshMusic/NOISE MOTHER-233127447.mp3",
        "GameSeshMusic/SHOPLIFTERS-1869278771.mp3"
    };

    private AudioManager() {
        random = new Random();
        soundEffectManager = new SoundEffectManager();
        loadLobbyMusic();
        loadGameSessionMusic();
    }

    /**
     * Get or create the singleton instance
     */
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    /**
     * Load all lobby music tracks
     */
    private void loadLobbyMusic() {
        lobbyTracks = new Music[LOBBY_MUSIC_PATHS.length];
        for (int i = 0; i < LOBBY_MUSIC_PATHS.length; i++) {
            try {
                lobbyTracks[i] = Gdx.audio.newMusic(Gdx.files.internal(LOBBY_MUSIC_PATHS[i]));
            } catch (Exception e) {
                System.err.println("Failed to load music: " + LOBBY_MUSIC_PATHS[i]);
                e.printStackTrace();
            }
        }
    }

    /**
     * Load all game session music tracks
     */
    private void loadGameSessionMusic() {
        gameSessionTracks = new Music[GAME_SESSION_MUSIC_PATHS.length];
        for (int i = 0; i < GAME_SESSION_MUSIC_PATHS.length; i++) {
            try {
                gameSessionTracks[i] = Gdx.audio.newMusic(Gdx.files.internal(GAME_SESSION_MUSIC_PATHS[i]));
            } catch (Exception e) {
                System.err.println("Failed to load music: " + GAME_SESSION_MUSIC_PATHS[i]);
                e.printStackTrace();
            }
        }
    }

    /**
     * Play a random lobby music track (only if no music is currently playing)
     */
    public void playRandomLobbyMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            System.out.println("[AudioManager] Music already playing, continuing...");
            return;
        }

        stopCurrentMusic();
        int randomIndex = random.nextInt(lobbyTracks.length);
        currentMusic = lobbyTracks[randomIndex];
        if (currentMusic != null) {
            currentMusic.setLooping(true);
            currentMusic.setVolume(volume);
            currentMusic.play();
            System.out.println("[AudioManager] Playing lobby music track: " + LOBBY_MUSIC_PATHS[randomIndex]);
        }
    }

    /**
     * Play a specific lobby music track by index (0-2)
     */
    public void playLobbyMusicTrack(int trackIndex) {
        if (trackIndex < 0 || trackIndex >= lobbyTracks.length) {
            System.err.println("[AudioManager] Invalid track index: " + trackIndex);
            return;
        }
        stopCurrentMusic();
        currentMusic = lobbyTracks[trackIndex];
        if (currentMusic != null) {
            currentMusic.setLooping(true);
            currentMusic.setVolume(volume);
            currentMusic.play();
            System.out.println("[AudioManager] Playing lobby music track: " + LOBBY_MUSIC_PATHS[trackIndex]);
        }
    }

    /**
     * Play a random game session music track (stops lobby music and starts game music)
     */
    public void playRandomGameSessionMusic() {
        stopCurrentMusic();

        int randomIndex = random.nextInt(gameSessionTracks.length);
        currentMusic = gameSessionTracks[randomIndex];
        if (currentMusic != null) {
            currentMusic.setLooping(true);
            currentMusic.setVolume(volume);
            currentMusic.play();
            System.out.println("[AudioManager] Playing game session music track: " + GAME_SESSION_MUSIC_PATHS[randomIndex]);
        }
    }

    /**
     * Play a specific game session music track by index (0-2)
     */
    public void playGameSessionMusicTrack(int trackIndex) {
        if (trackIndex < 0 || trackIndex >= gameSessionTracks.length) {
            System.err.println("[AudioManager] Invalid track index: " + trackIndex);
            return;
        }
        stopCurrentMusic();
        currentMusic = gameSessionTracks[trackIndex];
        if (currentMusic != null) {
            currentMusic.setLooping(true);
            currentMusic.setVolume(volume);
            currentMusic.play();
            System.out.println("[AudioManager] Playing game session music track: " + GAME_SESSION_MUSIC_PATHS[trackIndex]);
        }
    }

    /**
     * Stop the current music
     */
    public void stopCurrentMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
    }

    /**
     * Pause the current music
     */
    public void pauseCurrentMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    /**
     * Resume the current music
     */
    public void resumeCurrentMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    /**
     * Set volume for all music (0.0 to 1.0)
     */
    public void setVolume(float newVolume) {
        this.volume = Math.max(0.0f, Math.min(1.0f, newVolume));
        if (currentMusic != null) {
            currentMusic.setVolume(this.volume);
        }
    }

    /**
     * Get current volume
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Check if music is currently playing
     */
    public boolean isPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    // Sound effect delegation methods
    public void playShotgunShot(float sourceX, float sourceY, float listenerX, float listenerY, boolean isOwnShot) {
        soundEffectManager.playShotgunShot(sourceX, sourceY, listenerX, listenerY, isOwnShot);
    }

    public void playStep(float sourceX, float sourceY, float listenerX, float listenerY, boolean isOwnStep) {
        soundEffectManager.playStep(sourceX, sourceY, listenerX, listenerY, isOwnStep);
    }

    public void playNpcDeath(float npcX, float npcY, float listenerX, float listenerY) {
        soundEffectManager.playNpcDeath(npcX, npcY, listenerX, listenerY);
    }

    public void playNpcHit(float npcX, float npcY, float listenerX, float listenerY) {
        soundEffectManager.playNpcHit(npcX, npcY, listenerX, listenerY);
    }

    public void playOrbPickup() {
        soundEffectManager.playOrbPickup();
    }

    public void playPlayerDeath(float deathX, float deathY, float listenerX, float listenerY) {
        soundEffectManager.playPlayerDeath(deathX, deathY, listenerX, listenerY);
    }

    /**
     * Set volume for sound effects (0.0 to 1.0)
     */
    public void setSfxVolume(float newVolume) {
        soundEffectManager.setVolume(newVolume);
    }

    /**
     * Get current SFX volume
     */
    public float getSfxVolume() {
        return soundEffectManager.getVolume();
    }

    /**
     * Set music volume (0.0 to 1.0)
     */
    public void setMusicVolume(float newVolume) {
        setVolume(newVolume);
    }

    /**
     * Get current music volume
     */
    public float getMusicVolume() {
        return getVolume();
    }

    /**
     * Dispose all audio resources (call this when game closes)
     */
    public void dispose() {
        stopCurrentMusic();
        for (Music track : lobbyTracks) {
            if (track != null) {
                track.dispose();
            }
        }
        for (Music track : gameSessionTracks) {
            if (track != null) {
                track.dispose();
            }
        }
        soundEffectManager.dispose();
    }
}
