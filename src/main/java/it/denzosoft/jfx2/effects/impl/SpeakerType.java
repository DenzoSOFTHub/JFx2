package it.denzosoft.jfx2.effects.impl;

/**
 * Guitar speaker types with their frequency response characteristics.
 *
 * <p>Each speaker type has a unique tonal signature based on cone material,
 * magnet size, and construction that affects frequency response.</p>
 */
public enum SpeakerType {

    /**
     * Celestion Vintage 30 - Modern high-gain standard.
     * Strong upper mids, tight bass, aggressive attack.
     * Used in: Mesa, Marshall, Orange, high-gain amps.
     */
    CELESTION_V30(
            "Celestion V30",
            12,             // Size in inches
            60.0f,          // Low frequency resonance Hz
            5500.0f,        // High frequency rolloff Hz
            1.15f,          // Upper mid emphasis (2-4kHz)
            0.9f,           // Low mid (200-500Hz)
            0.85f,          // Bass response
            1.1f,           // Presence (4-6kHz)
            0.8f,           // Air (8-10kHz)
            0.7f            // Compression/breakup
    ),

    /**
     * Celestion G12M Greenback - Classic British crunch.
     * Warm, compressed, vocal mids, smooth highs.
     * Used in: Marshall Bluesbreaker, Plexi, classic rock.
     */
    CELESTION_GREENBACK(
            "Celestion Greenback",
            12,
            75.0f,          // Higher resonance
            5000.0f,        // Earlier rolloff - smoother
            1.0f,           // Balanced upper mids
            1.1f,           // Rich low mids
            0.9f,           // Warm bass
            0.85f,          // Smooth presence
            0.7f,           // Rolled off air
            0.9f            // More compression
    ),

    /**
     * Celestion G12H - Heavy magnet, tight response.
     * Neutral, extended response, less compression.
     * Used in: Marshall, versatile applications.
     */
    CELESTION_G12H(
            "Celestion G12H",
            12,
            55.0f,          // Lower resonance - tighter bass
            5500.0f,
            1.05f,          // Slight upper mid emphasis
            0.95f,          // Neutral low mids
            1.0f,           // Extended bass
            1.0f,           // Neutral presence
            0.85f,          // More air than Greenback
            0.6f            // Less compression - stiffer
    ),

    /**
     * Celestion Blue - Vintage British chime.
     * Bright, chimey, early breakup, Vox sound.
     * Used in: Vox AC15/AC30, British invasion.
     */
    CELESTION_BLUE(
            "Celestion Blue",
            12,
            75.0f,
            6000.0f,        // Extended highs - chimey
            1.1f,           // Present upper mids
            0.9f,           // Slightly scooped low mids
            0.8f,           // Less bass
            1.2f,           // Bright presence
            1.0f,           // Airy highs
            0.85f           // Musical compression
    ),

    /**
     * Jensen P12R - Vintage American clean.
     * Smooth, warm, classic Fender sound.
     * Used in: Fender Deluxe, Princeton, vintage.
     */
    JENSEN_P12R(
            "Jensen P12R",
            12,
            90.0f,          // Higher resonance
            4500.0f,        // Earlier rolloff - warm
            0.9f,           // Relaxed upper mids
            1.0f,           // Full low mids
            0.85f,          // Warm bass
            0.8f,           // Smooth presence
            0.6f,           // Dark air
            0.75f           // Moderate compression
    ),

    /**
     * Jensen C12N - American all-rounder.
     * Balanced, clear, versatile.
     * Used in: Fender Twin, Deluxe Reverb.
     */
    JENSEN_C12N(
            "Jensen C12N",
            12,
            80.0f,
            5000.0f,
            0.95f,          // Balanced upper mids
            1.0f,
            0.95f,          // Good bass
            0.9f,
            0.75f,
            0.65f           // Clean headroom
    ),

    /**
     * Eminence Cannabis Rex - Warm, hemp cone.
     * Smooth, warm, great for blues/jazz.
     */
    EMINENCE_CANNABIS_REX(
            "Eminence Cannabis Rex",
            12,
            70.0f,
            4000.0f,        // Early rolloff - very warm
            0.85f,          // Relaxed upper mids
            1.1f,           // Rich low mids
            1.0f,           // Full bass
            0.7f,           // Smooth presence
            0.5f,           // Dark
            0.8f            // Good compression
    ),

    /**
     * Eminence Governor - Modern British voice.
     * V30-like but smoother highs.
     */
    EMINENCE_GOVERNOR(
            "Eminence Governor",
            12,
            80.0f,
            5000.0f,
            1.1f,
            0.95f,
            0.9f,
            0.95f,
            0.75f,
            0.7f
    ),

    /**
     * Celestion G12T-75 - Modern Marshall.
     * Scooped mids, tight bass, bright.
     * Used in: Marshall JCM800, 900, modern.
     */
    CELESTION_G12T75(
            "Celestion G12T-75",
            12,
            75.0f,
            6000.0f,
            0.9f,           // Scooped upper mids
            0.85f,          // Scooped low mids
            0.95f,          // Tight bass
            1.15f,          // Bright presence
            0.9f,           // Extended air
            0.55f           // Very stiff - low compression
    ),

    /**
     * Weber Blue Dog - Alnico vintage.
     * Smooth, warm, vintage American.
     */
    WEBER_BLUE_DOG(
            "Weber Blue Dog",
            12,
            85.0f,
            4500.0f,
            0.9f,
            1.05f,
            0.9f,
            0.8f,
            0.65f,
            0.8f
    ),

    /**
     * Generic 10" speaker - Smaller, focused.
     */
    GENERIC_10(
            "Generic 10\"",
            10,
            100.0f,         // Higher resonance
            6000.0f,
            1.1f,           // More mids
            0.9f,
            0.7f,           // Less bass
            1.1f,
            0.9f,
            0.6f
    ),

    /**
     * Generic 15" speaker - Extended bass.
     */
    GENERIC_15(
            "Generic 15\"",
            15,
            45.0f,          // Lower resonance
            4500.0f,        // Earlier treble rolloff
            0.85f,          // Less focused mids
            1.0f,
            1.2f,           // More bass
            0.8f,
            0.7f,
            0.7f
    );

    private final String displayName;
    private final int sizeInches;
    private final float resonanceHz;
    private final float rolloffHz;
    private final float upperMids;      // 2-4kHz
    private final float lowMids;        // 200-500Hz
    private final float bass;           // <200Hz
    private final float presence;       // 4-6kHz
    private final float air;            // 8-10kHz
    private final float compression;

    SpeakerType(String displayName, int sizeInches, float resonanceHz, float rolloffHz,
                float upperMids, float lowMids, float bass, float presence, float air, float compression) {
        this.displayName = displayName;
        this.sizeInches = sizeInches;
        this.resonanceHz = resonanceHz;
        this.rolloffHz = rolloffHz;
        this.upperMids = upperMids;
        this.lowMids = lowMids;
        this.bass = bass;
        this.presence = presence;
        this.air = air;
        this.compression = compression;
    }

    public String getDisplayName() { return displayName; }
    public int getSizeInches() { return sizeInches; }
    public float getResonanceHz() { return resonanceHz; }
    public float getRolloffHz() { return rolloffHz; }
    public float getUpperMids() { return upperMids; }
    public float getLowMids() { return lowMids; }
    public float getBass() { return bass; }
    public float getPresence() { return presence; }
    public float getAir() { return air; }
    public float getCompression() { return compression; }
}
