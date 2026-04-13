package taltech.orbitgame.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

/**
 * Manages all sound effects in the game with distance-based volume calculation
 */
public class SoundEffectManager {
    private Sound shotgunSound;
    private Sound stepSound;
    private Sound npcDeathSound;
    private Sound npcHitSound;
    private Sound orbSound;
    private Sound playerDeathSound;
    private float sfxVolume = 0.5f;

    public SoundEffectManager() {
        loadSoundEffects();
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
            float calculatedVolume = calculateVolumeFromDistance(sourceX, sourceY, listenerX, listenerY, 500f);
            if (calculatedVolume > 0.01f) {
                shotgunSound.play(calculatedVolume * sfxVolume);
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
            float calculatedVolume = calculateVolumeFromDistance(sourceX, sourceY, listenerX, listenerY, 300f);
            if (calculatedVolume > 0.01f) {
                stepSound.play(calculatedVolume * sfxVolume * 0.5f);
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

        float calculatedVolume = calculateVolumeFromDistance(npcX, npcY, listenerX, listenerY, 600f);
        if (calculatedVolume > 0.01f) {
            npcDeathSound.play(calculatedVolume * sfxVolume);
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

        float calculatedVolume = calculateVolumeFromDistance(npcX, npcY, listenerX, listenerY, 400f);
        if (calculatedVolume > 0.01f) {
            npcHitSound.play(calculatedVolume * sfxVolume);
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

        float calculatedVolume = calculateVolumeFromDistance(deathX, deathY, listenerX, listenerY, 700f);
        if (calculatedVolume > 0.01f) {
            playerDeathSound.play(calculatedVolume * sfxVolume);
        }
    }

    /**
     * Calculate volume based on distance from sound source to listener
     * @param sourceX X position of sound source
     * @param sourceY Y position of sound source
     * @param listenerX X position of listener
     * @param listenerY Y position of listener
     * @param maxDistance Maximum distance at which sound can be heard
     * @return Volume multiplier (0.0 to 1.0)
     */
    private float calculateVolumeFromDistance(float sourceX, float sourceY, float listenerX, float listenerY, float maxDistance) {
        float distanceX = sourceX - listenerX;
        float distanceY = sourceY - listenerY;
        float distance = (float) Math.sqrt(distanceX * distanceX + distanceY * distanceY);
        return Math.max(0f, 1f - (distance / maxDistance));
    }

    /**
     * Set volume for sound effects (0.0 to 1.0)
     */
    public void setVolume(float newVolume) {
        this.sfxVolume = Math.max(0.0f, Math.min(1.0f, newVolume));
    }

    /**
     * Get current SFX volume
     */
    public float getVolume() {
        return sfxVolume;
    }

    /**
     * Dispose all sound effect resources
     */
    public void dispose() {
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
