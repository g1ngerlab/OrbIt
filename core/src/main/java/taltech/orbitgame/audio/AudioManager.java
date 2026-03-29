package taltech.orbitgame.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import java.util.Random;

/**
 * Singleton for managing background music and sound effects across the game.
 * Handles loading, playing, and disposing of music tracks and SFX.
 */
public class AudioManager {
    private static AudioManager instance;
    private Music currentMusic;
    private float volume = 0.2f;
    private float sfxVolume = 0.5f;
    private Random random;

    // Music tracks
    private Music[] lobbyTracks;
    private Music[] gameSessionTracks;

    // Sound effects
    private Sound shotgunSound;
    private Sound stepSound;
    private Sound npcDeathSound;
    private Sound npcHitSound;
    private Sound orbSound;
    private Sound playerDeathSound;
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
        loadLobbyMusic();
        loadGameSessionMusic();
        loadSoundEffects();
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
        // Only start new music if nothing is currently playing
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
        // Always stop current music and switch to game music
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
        // Always stop current music and switch to game music
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

    /**
     * Load all sound effects
     */
    private void loadSoundEffects() {
        try {
            shotgunSound = Gdx.audio.newSound(Gdx.files.internal("sounds/shotgun.mp3"));
        } catch (Exception e) {
            System.err.println("Failed to load shotgun sound effect");
            e.printStackTrace();
        }

        try {
            stepSound = Gdx.audio.newSound(Gdx.files.internal("sounds/step.mp3"));
        } catch (Exception e) {
            System.err.println("Failed to load step sound effect");
            e.printStackTrace();
        }

        try {
            npcDeathSound = Gdx.audio.newSound(Gdx.files.internal("sounds/npc_death.mp3"));
        } catch (Exception e) {
            System.err.println("Failed to load NPC death sound effect");
            e.printStackTrace();
        }

        try {
            npcHitSound = Gdx.audio.newSound(Gdx.files.internal("sounds/hit.mp3"));
        } catch (Exception e) {
            System.err.println("Failed to load NPC hit sound effect");
            e.printStackTrace();
        }

        try {
            orbSound = Gdx.audio.newSound(Gdx.files.internal("sounds/orb.mp3"));
        } catch (Exception e) {
            System.err.println("Failed to load orb pickup sound effect");
            e.printStackTrace();
        }

        try {
            playerDeathSound = Gdx.audio.newSound(Gdx.files.internal("sounds/death.mp3"));
        } catch (Exception e) {
            System.err.println("Failed to load player death sound effect");
            e.printStackTrace();
        }
    }

    /**
     * Play shotgun sound with volume based on distance
     * @param sourceX X position of the shooter
     * @param sourceY Y position of the shooter
     * @param listenerX X position of the listener (player)
     * @param listenerY Y position of the listener (player)
     * @param isOwnShot True if the player is the one shooting (always play at full volume)
     */
    public void playShotgunShot(float sourceX, float sourceY, float listenerX, float listenerY, boolean isOwnShot) {
        if (shotgunSound == null) {
            return;
        }

        if (isOwnShot) {
            // Own shot - always play at full volume
            shotgunSound.play(sfxVolume);
        } else {
            // Other player's shot - calculate volume based on distance
            float distanceX = sourceX - listenerX;
            float distanceY = sourceY - listenerY;
            float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

            // Maximum hearing distance in pixels (adjust as needed)
            float maxDistance = 500f;

            // Calculate volume: full at 0 distance, silent at max distance
            float calculatedVolume = Math.max(0f, 1f - (distance / maxDistance)) * sfxVolume;

            if (calculatedVolume > 0.01f) {
                shotgunSound.play(calculatedVolume);
            }
        }
    }

    /**
     * Play player step sound with volume based on distance
     * @param sourceX X position of the player taking a step
     * @param sourceY Y position of the player taking a step
     * @param listenerX X position of the listener (player)
     * @param listenerY Y position of the listener (player)
     * @param isOwnStep True if the player is taking the step (always play at reduced volume)
     */
    public void playStep(float sourceX, float sourceY, float listenerX, float listenerY, boolean isOwnStep) {
        if (stepSound == null) {
            return;
        }

        if (isOwnStep) {
            // Own step - play at lower volume (not distracting)
            stepSound.play(sfxVolume * 0.3f);
        } else {
            // Other player's step - calculate volume based on distance
            float distanceX = sourceX - listenerX;
            float distanceY = sourceY - listenerY;
            float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

            // Maximum hearing distance in pixels
            float maxDistance = 300f;

            // Calculate volume: full at 0 distance, silent at max distance
            float calculatedVolume = Math.max(0f, 1f - (distance / maxDistance)) * sfxVolume * 0.5f;

            if (calculatedVolume > 0.01f) {
                stepSound.play(calculatedVolume);
            }
        }
    }

    /**
     * Play NPC death sound with volume based on distance
     * @param npcX X position of the NPC that died
     * @param npcY Y position of the NPC that died
     * @param listenerX X position of the listener (player)
     * @param listenerY Y position of the listener (player)
     */
    public void playNpcDeath(float npcX, float npcY, float listenerX, float listenerY) {
        if (npcDeathSound == null) {
            return;
        }

        float distanceX = npcX - listenerX;
        float distanceY = npcY - listenerY;
        float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        // Maximum hearing distance in pixels
        float maxDistance = 600f;

        // Calculate volume: full at 0 distance, silent at max distance
        float calculatedVolume = Math.max(0f, 1f - (distance / maxDistance)) * sfxVolume;

        if (calculatedVolume > 0.01f) {
            npcDeathSound.play(calculatedVolume);
        }
    }

    /**
     * Play NPC hit sound with volume based on distance
     * @param npcX X position of the NPC that hit
     * @param npcY Y position of the NPC that hit
     * @param listenerX X position of the listener (player)
     * @param listenerY Y position of the listener (player)
     */
    public void playNpcHit(float npcX, float npcY, float listenerX, float listenerY) {
        if (npcHitSound == null) {
            return;
        }

        float distanceX = npcX - listenerX;
        float distanceY = npcY - listenerY;
        float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        // Maximum hearing distance in pixels
        float maxDistance = 400f;

        // Calculate volume: full at 0 distance, silent at max distance
        float calculatedVolume = Math.max(0f, 1f - (distance / maxDistance)) * sfxVolume;

        if (calculatedVolume > 0.01f) {
            npcHitSound.play(calculatedVolume);
        }
    }

    /**
     * Play orb pickup sound
     * Always plays at full volume when collected by any player
     */
    public void playOrbPickup() {
        if (orbSound == null) {
            return;
        }
        orbSound.play(sfxVolume);
    }

    /**
     * Play player death sound with volume based on distance
     * @param deathX X position where player died
     * @param deathY Y position where player died
     * @param listenerX X position of the listener (player)
     * @param listenerY Y position of the listener (player)
     */
    public void playPlayerDeath(float deathX, float deathY, float listenerX, float listenerY) {
        if (playerDeathSound == null) {
            return;
        }

        float distanceX = deathX - listenerX;
        float distanceY = deathY - listenerY;
        float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);

        // Maximum hearing distance in pixels
        float maxDistance = 700f;

        // Calculate volume: full at 0 distance, silent at max distance
        float calculatedVolume = Math.max(0f, 1f - (distance / maxDistance)) * sfxVolume;

        if (calculatedVolume > 0.01f) {
            playerDeathSound.play(calculatedVolume);
        }
    }

    /**
     * Set volume for sound effects (0.0 to 1.0)
     */
    public void setSfxVolume(float newVolume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, newVolume));
    }

    /**
     * Get current SFX volume
     */
    public float getSfxVolume() {
        return sfxVolume;
    }

    /**
     * Dispose all music resources (call this when game closes)
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
        if (shotgunSound != null) {
            shotgunSound.dispose();
        }
        if (stepSound != null) {
            stepSound.dispose();
        }
        if (npcDeathSound != null) {
            npcDeathSound.dispose();
        }
        if (npcHitSound != null) {
            npcHitSound.dispose();
        }
        if (orbSound != null) {
            orbSound.dispose();
        }
        if (playerDeathSound != null) {
            playerDeathSound.dispose();
        }
    }
}
