package taltech.orbitgame.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Singleton for managing background music across the game.
 * Sound effects are delegated to SoundEffectManager.
 */
public class AudioManager {
    private enum PlaylistType {
        NONE,
        LOBBY,
        GAME_SESSION
    }

    private static final String LOBBY_MUSIC_DIR = "LobbyMusic";
    private static final String GAME_SESSION_MUSIC_DIR = "GameSeshMusic";
    private static final String[] SUPPORTED_MUSIC_EXTENSIONS = {"mp3", "ogg", "wav"};
    private static final String[] FALLBACK_LOBBY_TRACK_PATHS = {
        "LobbyMusic/Cyberia lyr2-132369550.mp3",
        "LobbyMusic/I Need To Feel Loved-1935759142.mp3",
        "LobbyMusic/joon-1493758854.mp3",
        "LobbyMusic/fontanchik.mp3",
        "LobbyMusic/lobby.mp3",
        "LobbyMusic/lobbygood.mp3",
        "LobbyMusic/onon.mp3",
        "LobbyMusic/outcast.mp3",
        "LobbyMusic/practisemode.mp3"
    };
    private static final String[] FALLBACK_GAME_SESSION_TRACK_PATHS = {
        "GameSeshMusic/Knife Party - 'Bonfire'-419194644.mp3",
        "GameSeshMusic/NOISE MOTHER-233127447.mp3",
        "GameSeshMusic/SHOPLIFTERS-1869278771.mp3",
        "GameSeshMusic/viki_show.mp3",
        "GameSeshMusic/ukradet_i_pozovet.mp3",
        "GameSeshMusic/somewhereibelong.mp3",
        "GameSeshMusic/rightpassword.mp3",
        "GameSeshMusic/kyoto.mp3",
        "GameSeshMusic/blast.mp3",
        "GameSeshMusic/aesthetics of self-destruction.mp3"
    };

    private static AudioManager instance;

    private final SoundEffectManager soundEffectManager;
    private final Random random;

    private final String[] lobbyTrackPaths;
    private final String[] gameSessionTrackPaths;
    private final Music[] lobbyTracks;
    private final Music[] gameSessionTracks;
    private final float[] lobbyTrackDurationsSeconds;
    private final float[] gameSessionTrackDurationsSeconds;

    private Music currentMusic;
    private PlaylistType currentPlaylist = PlaylistType.NONE;
    private int currentTrackIndex = -1;
    private float currentTrackDurationSeconds = 0f;
    private float volume = 0.75f;

    private AudioManager() {
        this.random = new Random();
        this.soundEffectManager = new SoundEffectManager();
        this.lobbyTrackPaths = discoverTrackPaths(LOBBY_MUSIC_DIR);
        this.gameSessionTrackPaths = discoverTrackPaths(GAME_SESSION_MUSIC_DIR);
        this.lobbyTracks = loadTracks(lobbyTrackPaths);
        this.gameSessionTracks = loadTracks(gameSessionTrackPaths);
        this.lobbyTrackDurationsSeconds = loadTrackDurations(lobbyTrackPaths);
        this.gameSessionTrackDurationsSeconds = loadTrackDurations(gameSessionTrackPaths);
    }

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public void playRandomLobbyMusic() {
        playRandomFromPlaylist(PlaylistType.LOBBY);
    }

    public void playRandomGameSessionMusic() {
        playRandomFromPlaylist(PlaylistType.GAME_SESSION);
    }

    public void playNextLobbyMusicTrack() {
        playNextInPlaylist(PlaylistType.LOBBY);
    }

    public void playNextGameSessionMusicTrack() {
        playNextInPlaylist(PlaylistType.GAME_SESSION);
    }

    public void playPreviousLobbyMusicTrack() {
        playPreviousInPlaylist(PlaylistType.LOBBY);
    }

    public void playPreviousGameSessionMusicTrack() {
        playPreviousInPlaylist(PlaylistType.GAME_SESSION);
    }

    public void playNextTrack() {
        if (currentPlaylist == PlaylistType.GAME_SESSION) {
            playNextGameSessionMusicTrack();
        } else {
            playNextLobbyMusicTrack();
        }
    }

    public void playPreviousTrack() {
        if (currentPlaylist == PlaylistType.GAME_SESSION) {
            playPreviousGameSessionMusicTrack();
        } else {
            playPreviousLobbyMusicTrack();
        }
    }

    public void togglePauseResumeCurrentMusic() {
        if (currentMusic == null) {
            if (currentPlaylist == PlaylistType.GAME_SESSION) {
                playRandomGameSessionMusic();
            } else {
                playRandomLobbyMusic();
            }
            return;
        }

        if (currentMusic.isPlaying()) {
            currentMusic.pause();
        } else {
            currentMusic.play();
        }
    }

    public boolean hasCurrentTrack() {
        return currentMusic != null;
    }

    public float getCurrentTrackPositionSeconds() {
        if (currentMusic == null) {
            return 0f;
        }
        return Math.max(0f, currentMusic.getPosition());
    }

    public float getCurrentTrackDurationSeconds() {
        if (currentTrackDurationSeconds > 0f) {
            return currentTrackDurationSeconds;
        }
        return Math.max(300f, getCurrentTrackPositionSeconds() + 30f);
    }

    public void seekCurrentTrackSeconds(float seconds) {
        if (currentMusic == null) {
            return;
        }
        float clamped = Math.max(0f, seconds);
        if (currentTrackDurationSeconds > 0f) {
            clamped = Math.min(clamped, currentTrackDurationSeconds);
        }
        currentMusic.setPosition(clamped);
    }

    public void playLobbyMusicTrack(int trackIndex) {
        playSpecificTrack(PlaylistType.LOBBY, trackIndex);
    }

    public void playGameSessionMusicTrack(int trackIndex) {
        playSpecificTrack(PlaylistType.GAME_SESSION, trackIndex);
    }

    public void stopCurrentMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic.setOnCompletionListener(null);
        }
    }

    public void pauseCurrentMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    public void resumeCurrentMusic() {
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    public void setVolume(float newVolume) {
        this.volume = Math.max(0f, Math.min(1f, newVolume));
        if (currentMusic != null) {
            currentMusic.setVolume(this.volume);
        }
    }

    public float getVolume() {
        return volume;
    }

    public boolean isPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    public void setSfxVolume(float newVolume) {
        soundEffectManager.setVolume(newVolume);
    }

    public float getSfxVolume() {
        return soundEffectManager.getVolume();
    }

    public void setMusicVolume(float newVolume) {
        setVolume(newVolume);
    }

    public float getMusicVolume() {
        return getVolume();
    }

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

    public void playMeleeHit(float sourceX, float sourceY, float listenerX, float listenerY, boolean isOwnHit) {
        soundEffectManager.playMeleeHit(sourceX, sourceY, listenerX, listenerY, isOwnHit);
    }

    public void playOrbPickup() {
        soundEffectManager.playOrbPickup();
    }

    public void playPlayerDeath(float deathX, float deathY, float listenerX, float listenerY) {
        soundEffectManager.playPlayerDeath(deathX, deathY, listenerX, listenerY);
    }

    public void dispose() {
        stopCurrentMusic();
        disposeTracks(lobbyTracks);
        disposeTracks(gameSessionTracks);
        soundEffectManager.dispose();
    }

    private void playRandomFromPlaylist(PlaylistType playlistType) {
        if (currentPlaylist == playlistType && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }
        Music[] tracks = tracksFor(playlistType);
        if (tracks.length == 0) {
            return;
        }

        int candidate = random.nextInt(tracks.length);
        int playableIndex = resolvePlayableIndex(tracks, candidate);
        if (playableIndex >= 0) {
            playTrack(playlistType, playableIndex);
        }
    }

    private void playNextInPlaylist(PlaylistType playlistType) {
        Music[] tracks = tracksFor(playlistType);
        if (tracks.length == 0) {
            return;
        }

        int startIndex;
        if (currentPlaylist == playlistType && currentTrackIndex >= 0) {
            startIndex = (currentTrackIndex + 1) % tracks.length;
        } else {
            startIndex = random.nextInt(tracks.length);
        }

        int playableIndex = resolvePlayableIndex(tracks, startIndex);
        if (playableIndex >= 0) {
            playTrack(playlistType, playableIndex);
        }
    }

    private void playPreviousInPlaylist(PlaylistType playlistType) {
        Music[] tracks = tracksFor(playlistType);
        if (tracks.length == 0) {
            return;
        }

        int startIndex;
        if (currentPlaylist == playlistType && currentTrackIndex >= 0) {
            startIndex = (currentTrackIndex - 1 + tracks.length) % tracks.length;
        } else {
            startIndex = random.nextInt(tracks.length);
        }

        int playableIndex = resolvePlayableIndexBackward(tracks, startIndex);
        if (playableIndex >= 0) {
            playTrack(playlistType, playableIndex);
        }
    }

    private void playSpecificTrack(PlaylistType playlistType, int trackIndex) {
        Music[] tracks = tracksFor(playlistType);
        if (trackIndex < 0 || trackIndex >= tracks.length) {
            System.err.println("[AudioManager] Invalid track index: " + trackIndex);
            return;
        }
        int playableIndex = resolvePlayableIndex(tracks, trackIndex);
        if (playableIndex >= 0) {
            playTrack(playlistType, playableIndex);
        }
    }

    private void playTrack(PlaylistType playlistType, int trackIndex) {
        Music[] tracks = tracksFor(playlistType);
        String[] paths = pathsFor(playlistType);
        if (trackIndex < 0 || trackIndex >= tracks.length) {
            return;
        }

        Music nextTrack = tracks[trackIndex];
        if (nextTrack == null) {
            int fallbackIndex = resolvePlayableIndex(tracks, trackIndex + 1);
            if (fallbackIndex < 0) {
                return;
            }
            nextTrack = tracks[fallbackIndex];
            trackIndex = fallbackIndex;
        }

        stopCurrentMusic();
        currentMusic = nextTrack;
        currentPlaylist = playlistType;
        currentTrackIndex = trackIndex;
        currentTrackDurationSeconds = durationsFor(playlistType)[trackIndex];

        final PlaylistType completionPlaylist = playlistType;
        currentMusic.setLooping(false);
        currentMusic.setVolume(volume);
        currentMusic.setOnCompletionListener(new Music.OnCompletionListener() {
            @Override
            public void onCompletion(Music music) {
                playNextInPlaylist(completionPlaylist);
            }
        });
        currentMusic.play();

        String playlistLabel = playlistType == PlaylistType.LOBBY ? "lobby" : "game-session";
        String trackPath = trackIndex < paths.length ? paths[trackIndex] : "unknown";
        System.out.println("[AudioManager] Playing " + playlistLabel + " music track: " + trackPath);
    }

    private Music[] tracksFor(PlaylistType playlistType) {
        return playlistType == PlaylistType.LOBBY ? lobbyTracks : gameSessionTracks;
    }

    private float[] durationsFor(PlaylistType playlistType) {
        return playlistType == PlaylistType.LOBBY ? lobbyTrackDurationsSeconds : gameSessionTrackDurationsSeconds;
    }

    private String[] pathsFor(PlaylistType playlistType) {
        return playlistType == PlaylistType.LOBBY ? lobbyTrackPaths : gameSessionTrackPaths;
    }

    private int resolvePlayableIndex(Music[] tracks, int startIndex) {
        if (tracks.length == 0) {
            return -1;
        }
        int normalizedStart = ((startIndex % tracks.length) + tracks.length) % tracks.length;
        for (int offset = 0; offset < tracks.length; offset++) {
            int index = (normalizedStart + offset) % tracks.length;
            if (tracks[index] != null) {
                return index;
            }
        }
        return -1;
    }

    private int resolvePlayableIndexBackward(Music[] tracks, int startIndex) {
        if (tracks.length == 0) {
            return -1;
        }
        int normalizedStart = ((startIndex % tracks.length) + tracks.length) % tracks.length;
        for (int offset = 0; offset < tracks.length; offset++) {
            int index = (normalizedStart - offset + tracks.length) % tracks.length;
            if (tracks[index] != null) {
                return index;
            }
        }
        return -1;
    }

    private String[] discoverTrackPaths(String directory) {
        FileHandle dir = Gdx.files.internal(directory);
        if (!dir.exists() || !dir.isDirectory()) {
            return fallbackTrackPaths(directory);
        }

        List<String> discovered = new ArrayList<>();
        FileHandle[] files = dir.list();
        Arrays.sort(files, (a, b) -> a.name().compareToIgnoreCase(b.name()));
        for (FileHandle file : files) {
            if (file == null || file.isDirectory()) {
                continue;
            }
            String extension = file.extension().toLowerCase();
            if (isSupportedMusicExtension(extension)) {
                discovered.add(directory + "/" + file.name());
            }
        }
        if (discovered.isEmpty()) {
            return fallbackTrackPaths(directory);
        }
        return discovered.toArray(new String[0]);
    }

    private boolean isSupportedMusicExtension(String extension) {
        for (String supported : SUPPORTED_MUSIC_EXTENSIONS) {
            if (supported.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    private Music[] loadTracks(String[] paths) {
        Music[] tracks = new Music[paths.length];
        for (int i = 0; i < paths.length; i++) {
            try {
                FileHandle handle = Gdx.files.internal(paths[i]);
                if (!handle.exists()) {
                    System.err.println("[AudioManager] Missing music file: " + paths[i]);
                    continue;
                }
                tracks[i] = Gdx.audio.newMusic(handle);
            } catch (Exception e) {
                System.err.println("[AudioManager] Failed to load music: " + paths[i]);
                e.printStackTrace();
            }
        }
        return tracks;
    }

    private float[] loadTrackDurations(String[] paths) {
        float[] durations = new float[paths.length];
        for (int i = 0; i < paths.length; i++) {
            durations[i] = readDurationSeconds(paths[i]);
        }
        return durations;
    }

    private float readDurationSeconds(String path) {
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            return 0f;
        }
        try (BufferedInputStream stream = new BufferedInputStream(handle.read())) {
            AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(stream);
            Object durationMicros = fileFormat.properties().get("duration");
            if (durationMicros instanceof Number) {
                float seconds = ((Number) durationMicros).longValue() / 1_000_000f;
                if (seconds > 0f) {
                    return seconds;
                }
            }

            AudioFormat format = fileFormat.getFormat();
            int frameLength = fileFormat.getFrameLength();
            if (frameLength > 0 && format != null && format.getFrameRate() > 0f) {
                return frameLength / format.getFrameRate();
            }
        } catch (Exception ignored) {
            // Metadata extraction is best-effort and may not be available for all codecs.
        }
        return 0f;
    }

    private String[] fallbackTrackPaths(String directory) {
        if (LOBBY_MUSIC_DIR.equals(directory)) {
            return Arrays.copyOf(FALLBACK_LOBBY_TRACK_PATHS, FALLBACK_LOBBY_TRACK_PATHS.length);
        }
        return Arrays.copyOf(FALLBACK_GAME_SESSION_TRACK_PATHS, FALLBACK_GAME_SESSION_TRACK_PATHS.length);
    }

    private void disposeTracks(Music[] tracks) {
        for (Music track : tracks) {
            if (track != null) {
                track.dispose();
            }
        }
    }
}
