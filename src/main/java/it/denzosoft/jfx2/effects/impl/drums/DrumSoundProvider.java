package it.denzosoft.jfx2.effects.impl.drums;

import it.denzosoft.jfx2.effects.impl.drums.DrumSounds.DrumSound;

/**
 * Interface for drum sound providers.
 *
 * <p>Implementations can generate sounds via synthesis, MIDI rendering,
 * or loading from sample files.</p>
 */
public interface DrumSoundProvider {

    /**
     * Get the samples for a specific drum sound.
     *
     * @param sound The drum sound type
     * @return Array of audio samples (mono, normalized to -1..1)
     */
    float[] getSound(DrumSound sound);

    /**
     * Get the sample rate of the generated sounds.
     */
    int getSampleRate();

    /**
     * Get the name of this provider for display purposes.
     */
    String getName();

    // Convenience methods for specific sounds

    default float[] getKick() { return getSound(DrumSound.KICK); }
    default float[] getSnare() { return getSound(DrumSound.SNARE); }
    default float[] getHihatClosed() { return getSound(DrumSound.HIHAT_CLOSED); }
    default float[] getHihatOpen() { return getSound(DrumSound.HIHAT_OPEN); }
    default float[] getCrash() { return getSound(DrumSound.CRASH); }
    default float[] getRide() { return getSound(DrumSound.RIDE); }
    default float[] getRideBell() { return getSound(DrumSound.RIDE_BELL); }
    default float[] getTomHigh() { return getSound(DrumSound.TOM_HIGH); }
    default float[] getTomMid() { return getSound(DrumSound.TOM_MID); }
    default float[] getTomLow() { return getSound(DrumSound.TOM_LOW); }
    default float[] getRimshot() { return getSound(DrumSound.RIMSHOT); }
    default float[] getClap() { return getSound(DrumSound.CLAP); }
    default float[] getCowbell() { return getSound(DrumSound.COWBELL); }
    default float[] getSticks() { return getSound(DrumSound.STICKS); }
}
