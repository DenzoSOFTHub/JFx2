package it.denzosoft.jfx2.effects.impl;

/**
 * Common preamp tube types with their characteristics.
 *
 * <p>Each tube type has different gain factors, harmonic content,
 * and tonal characteristics that affect the sound.</p>
 */
public enum TubeType {

    /**
     * 12AX7 / ECC83 - The most common preamp tube.
     * High gain (100), rich harmonics, classic rock/blues tone.
     * Used in: Fender, Marshall, Mesa, Vox, almost all guitar amps.
     */
    ECC83_12AX7(
            "12AX7 / ECC83",
            100.0f,     // Gain factor (mu)
            1.2f,       // Plate resistance (relative)
            0.7f,       // Even harmonic content
            0.5f,       // Odd harmonic content
            1.0f,       // Compression amount
            0.8f,       // Bass response
            1.0f,       // Mid response
            0.9f        // Treble response
    ),

    /**
     * 12AT7 / ECC81 - Medium gain tube.
     * Lower gain (60), cleaner, more headroom, used in reverb drivers.
     * Used in: Phase inverters, reverb circuits, hi-fi preamps.
     */
    ECC81_12AT7(
            "12AT7 / ECC81",
            60.0f,      // Gain factor
            0.8f,       // Plate resistance
            0.5f,       // Even harmonics
            0.3f,       // Odd harmonics
            0.7f,       // Compression
            0.9f,       // Bass
            1.0f,       // Mid
            1.0f        // Treble
    ),

    /**
     * 12AU7 / ECC82 - Low gain tube.
     * Lowest gain (20), very clean, excellent dynamics.
     * Used in: Phase inverters, hi-fi, studio equipment.
     */
    ECC82_12AU7(
            "12AU7 / ECC82",
            20.0f,      // Gain factor
            0.6f,       // Plate resistance
            0.3f,       // Even harmonics
            0.2f,       // Odd harmonics
            0.5f,       // Compression
            1.0f,       // Bass - more controlled
            1.0f,       // Mid
            1.0f        // Treble
    ),

    /**
     * 12AY7 - Vintage Fender tube.
     * Medium-low gain (45), sweet breakup, vintage tone.
     * Used in: Fender Tweed amps, vintage designs.
     */
    V_12AY7(
            "12AY7",
            45.0f,      // Gain factor
            0.9f,       // Plate resistance
            0.6f,       // Even harmonics - sweet
            0.25f,      // Odd harmonics - low
            0.8f,       // Compression
            0.85f,      // Bass - slightly rolled off
            1.0f,       // Mid
            0.95f       // Treble
    ),

    /**
     * 5751 - Military spec 12AX7.
     * Lower gain (70), tighter, less noise, more headroom.
     * Used in: High-gain amps for less fizz, studio applications.
     */
    V_5751(
            "5751",
            70.0f,      // Gain factor - 70% of 12AX7
            1.0f,       // Plate resistance
            0.6f,       // Even harmonics
            0.4f,       // Odd harmonics
            0.85f,      // Compression
            0.9f,       // Bass
            1.0f,       // Mid
            0.95f       // Treble - slightly tamed
    ),

    /**
     * EF86 - Pentode tube.
     * Very high gain, complex harmonics, distinctive chime.
     * Used in: Vox AC15/AC30, some boutique amps.
     */
    EF86(
            "EF86 Pentode",
            200.0f,     // Very high gain
            1.5f,       // Higher plate resistance
            0.4f,       // Even harmonics
            0.8f,       // Odd harmonics - more aggressive
            1.2f,       // Higher compression
            0.7f,       // Bass - tighter
            1.1f,       // Mid - prominent
            1.1f        // Treble - chimey
    ),

    /**
     * 6SL7 - Octal preamp tube.
     * High gain (70), warm, vintage character.
     * Used in: Vintage amps, hi-fi equipment.
     */
    V_6SL7(
            "6SL7",
            70.0f,      // Gain factor
            1.3f,       // Higher plate resistance
            0.8f,       // Even harmonics - warm
            0.3f,       // Odd harmonics - smooth
            0.9f,       // Compression
            0.95f,      // Bass
            1.0f,       // Mid
            0.85f       // Treble - rolled off
    ),

    /**
     * 6SN7 - Octal medium gain tube.
     * Medium gain (20), very linear, excellent dynamics.
     * Used in: Hi-fi preamps, phase inverters.
     */
    V_6SN7(
            "6SN7",
            20.0f,      // Lower gain
            0.7f,       // Plate resistance
            0.4f,       // Even harmonics
            0.2f,       // Odd harmonics
            0.6f,       // Compression
            1.0f,       // Bass
            1.0f,       // Mid
            1.0f        // Treble - flat
    );

    private final String displayName;
    private final float gainFactor;
    private final float plateResistance;
    private final float evenHarmonics;
    private final float oddHarmonics;
    private final float compression;
    private final float bassResponse;
    private final float midResponse;
    private final float trebleResponse;

    TubeType(String displayName, float gainFactor, float plateResistance,
             float evenHarmonics, float oddHarmonics, float compression,
             float bassResponse, float midResponse, float trebleResponse) {
        this.displayName = displayName;
        this.gainFactor = gainFactor;
        this.plateResistance = plateResistance;
        this.evenHarmonics = evenHarmonics;
        this.oddHarmonics = oddHarmonics;
        this.compression = compression;
        this.bassResponse = bassResponse;
        this.midResponse = midResponse;
        this.trebleResponse = trebleResponse;
    }

    public String getDisplayName() { return displayName; }
    public float getGainFactor() { return gainFactor; }
    public float getPlateResistance() { return plateResistance; }
    public float getEvenHarmonics() { return evenHarmonics; }
    public float getOddHarmonics() { return oddHarmonics; }
    public float getCompression() { return compression; }
    public float getBassResponse() { return bassResponse; }
    public float getMidResponse() { return midResponse; }
    public float getTrebleResponse() { return trebleResponse; }

    /**
     * Get normalized gain (0-1 range based on max gain tube).
     */
    public float getNormalizedGain() {
        return gainFactor / 200.0f;  // EF86 has max gain
    }
}
