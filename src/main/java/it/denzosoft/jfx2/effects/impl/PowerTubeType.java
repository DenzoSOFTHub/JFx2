package it.denzosoft.jfx2.effects.impl;

/**
 * Common power tube types with their characteristics.
 *
 * <p>Each power tube type has different headroom, breakup characteristics,
 * and tonal qualities that significantly affect the amplifier's sound.</p>
 */
public enum PowerTubeType {

    /**
     * EL34 - Classic British rock tube.
     * Pronounced midrange, aggressive breakup, "British" sound.
     * Used in: Marshall, HiWatt, Orange, Sound City.
     */
    EL34(
            "EL34",
            25.0f,      // Watts per pair
            0.9f,       // Headroom (relative)
            1.2f,       // Midrange emphasis
            0.8f,       // Bass tightness (lower = looser)
            0.9f,       // Treble brightness
            0.7f,       // Even harmonics
            0.9f,       // Odd harmonics (more aggressive)
            0.8f,       // Sag amount
            0.7f        // Compression
    ),

    /**
     * 6L6 - Classic American clean tube.
     * Tight bass, scooped mids, "American" clean sound.
     * Used in: Fender Twin, Mesa Boogie, Peavey.
     */
    V_6L6(
            "6L6",
            30.0f,      // Watts per pair
            1.0f,       // High headroom - stays clean
            0.8f,       // Scooped mids
            1.0f,       // Tight bass
            1.0f,       // Bright treble
            0.8f,       // Even harmonics
            0.5f,       // Less odd harmonics - cleaner
            0.5f,       // Less sag
            0.5f        // Less compression
    ),

    /**
     * EL84 - Small British tube with early breakup.
     * Chimey, bright, breaks up early, "Vox" sound.
     * Used in: Vox AC15/AC30, Fender Blues Junior.
     */
    EL84(
            "EL84",
            12.0f,      // Lower wattage
            0.6f,       // Less headroom - early breakup
            1.1f,       // Slight mid emphasis
            0.85f,      // Slightly loose bass
            1.1f,       // Bright/chimey
            0.6f,       // Even harmonics
            0.8f,       // Odd harmonics - snarling
            0.9f,       // More sag
            0.9f        // More compression
    ),

    /**
     * 6V6 - Small American tube, warm breakup.
     * Warm, smooth breakup, "Tweed" sound.
     * Used in: Fender Deluxe, Champ, Princeton.
     */
    V_6V6(
            "6V6",
            14.0f,      // Lower wattage
            0.7f,       // Moderate headroom
            1.0f,       // Balanced mids
            0.9f,       // Warm bass
            0.9f,       // Rolled off treble
            0.9f,       // Sweet even harmonics
            0.4f,       // Low odd harmonics - smooth
            0.85f,      // Good sag
            0.8f        // Nice compression
    ),

    /**
     * KT88 - High power hi-fi tube.
     * Huge headroom, tight, powerful, minimal distortion.
     * Used in: Marshall Major, Hi-Fi amps, high-power amps.
     */
    KT88(
            "KT88",
            50.0f,      // High wattage
            1.2f,       // Very high headroom
            0.9f,       // Flat mids
            1.1f,       // Very tight bass
            0.95f,      // Neutral treble
            0.5f,       // Low harmonics - clean
            0.3f,       // Very low odd harmonics
            0.3f,       // Minimal sag
            0.4f        // Low compression
    ),

    /**
     * KT66 - Vintage British tube.
     * Warm, fat, vintage "Plexi" sound.
     * Used in: Marshall JTM45, vintage HiWatt.
     */
    KT66(
            "KT66",
            25.0f,      // Medium-high wattage
            0.95f,      // Good headroom
            1.0f,       // Balanced mids
            0.95f,      // Full bass
            0.85f,      // Warm treble
            0.85f,      // Nice even harmonics
            0.5f,       // Moderate odd harmonics
            0.6f,       // Moderate sag
            0.6f        // Moderate compression
    ),

    /**
     * 6550 - High power American tube.
     * Tight, powerful, clean headroom, bass amp favorite.
     * Used in: Ampeg SVT, Mesa Boogie, high-power amps.
     */
    V_6550(
            "6550",
            40.0f,      // High wattage
            1.1f,       // Very high headroom
            0.85f,      // Slightly scooped mids
            1.15f,      // Very tight bass - great for bass
            0.9f,       // Clean treble
            0.6f,       // Lower harmonics
            0.4f,       // Clean odd harmonics
            0.35f,      // Low sag
            0.45f       // Low compression
    ),

    /**
     * 5881 - Military spec 6L6.
     * Tighter, more controlled than 6L6.
     * Used in: Vintage Fender, military amps.
     */
    V_5881(
            "5881",
            23.0f,      // Slightly lower than 6L6
            0.9f,       // Good headroom
            0.9f,       // Slight mid scoop
            0.95f,      // Tight bass
            0.95f,      // Neutral treble
            0.75f,      // Even harmonics
            0.55f,      // Low odd harmonics
            0.55f,      // Moderate sag
            0.55f       // Moderate compression
    );

    private final String displayName;
    private final float wattsPerPair;
    private final float headroom;
    private final float midEmphasis;
    private final float bassTightness;
    private final float trebleBrightness;
    private final float evenHarmonics;
    private final float oddHarmonics;
    private final float sagAmount;
    private final float compression;

    PowerTubeType(String displayName, float wattsPerPair, float headroom,
                  float midEmphasis, float bassTightness, float trebleBrightness,
                  float evenHarmonics, float oddHarmonics, float sagAmount, float compression) {
        this.displayName = displayName;
        this.wattsPerPair = wattsPerPair;
        this.headroom = headroom;
        this.midEmphasis = midEmphasis;
        this.bassTightness = bassTightness;
        this.trebleBrightness = trebleBrightness;
        this.evenHarmonics = evenHarmonics;
        this.oddHarmonics = oddHarmonics;
        this.sagAmount = sagAmount;
        this.compression = compression;
    }

    public String getDisplayName() { return displayName; }
    public float getWattsPerPair() { return wattsPerPair; }
    public float getHeadroom() { return headroom; }
    public float getMidEmphasis() { return midEmphasis; }
    public float getBassTightness() { return bassTightness; }
    public float getTrebleBrightness() { return trebleBrightness; }
    public float getEvenHarmonics() { return evenHarmonics; }
    public float getOddHarmonics() { return oddHarmonics; }
    public float getSagAmount() { return sagAmount; }
    public float getCompression() { return compression; }
}
