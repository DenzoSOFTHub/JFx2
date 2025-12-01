package it.denzosoft.jfx2.effects.impl;

/**
 * Microphone types commonly used for guitar cabinet recording.
 *
 * <p>Each microphone has a unique frequency response and character
 * that affects the recorded tone.</p>
 */
public enum MicrophoneType {

    /**
     * Shure SM57 - The industry standard.
     * Dynamic, cardioid. Presence peak, bass rolloff.
     * Punchy mids, cuts through mix.
     */
    SHURE_SM57(
            "Shure SM57",
            "Dynamic",
            40.0f,          // Low freq response Hz
            15000.0f,       // High freq response Hz
            0.85f,          // Bass (proximity effect compensated)
            0.9f,           // Low mids (slight dip 300-500Hz)
            1.0f,           // Mids
            1.2f,           // Upper mids / presence peak (2-6kHz)
            0.9f,           // Highs (slight rolloff 8-10kHz)
            0.8f,           // Air (top end sparkle)
            1.0f            // Proximity effect strength
    ),

    /**
     * Sennheiser MD421 - Balanced and full.
     * Dynamic, cardioid. More low end than SM57.
     * Balanced, detailed, versatile.
     */
    SENNHEISER_MD421(
            "Sennheiser MD421",
            "Dynamic",
            30.0f,
            17000.0f,
            1.0f,           // Full bass
            1.05f,          // Present low mids
            1.0f,           // Flat mids
            1.1f,           // Slight presence boost
            0.95f,          // Extended highs
            0.85f,
            0.9f            // Less proximity effect
    ),

    /**
     * Royer R-121 - Warm and smooth ribbon.
     * Ribbon, bidirectional. Flat response, warm character.
     * Creamy, smooth, natural.
     */
    ROYER_R121(
            "Royer R-121",
            "Ribbon",
            30.0f,
            15000.0f,
            1.1f,           // Warm bass
            1.1f,           // Rich low mids
            1.0f,           // Flat mids
            0.9f,           // Smooth presence (no peak)
            0.8f,           // Rolled off highs
            0.6f,           // Dark air
            0.7f            // Less proximity effect
    ),

    /**
     * Sennheiser e906 - Flat on cab design.
     * Dynamic, supercardioid. Three-position switch.
     * Versatile, rejects bleed well.
     */
    SENNHEISER_E906(
            "Sennheiser e906",
            "Dynamic",
            40.0f,
            18000.0f,
            0.95f,          // Tight bass
            1.0f,
            1.0f,
            1.15f,          // Presence boost
            1.0f,           // Extended highs
            0.9f,
            0.85f
    ),

    /**
     * Sennheiser e609 - Classic flat design.
     * Dynamic, supercardioid. Predecessor to e906.
     * Bright, cutting.
     */
    SENNHEISER_E609(
            "Sennheiser e609",
            "Dynamic",
            40.0f,
            15000.0f,
            0.9f,
            0.95f,
            1.0f,
            1.2f,           // More presence peak
            0.95f,
            0.85f,
            0.85f
    ),

    /**
     * AKG C414 - Studio condenser.
     * Condenser, multiple patterns. Detailed, accurate.
     * Clear, detailed, studio quality.
     */
    AKG_C414(
            "AKG C414",
            "Condenser",
            20.0f,
            20000.0f,
            1.0f,           // Flat bass
            1.0f,           // Flat low mids
            1.0f,           // Flat mids
            1.05f,          // Slight presence
            1.1f,           // Extended highs
            1.1f,           // Airy top
            1.2f            // Strong proximity effect
    ),

    /**
     * Neumann U87 - Classic studio condenser.
     * Condenser, multi-pattern. Smooth, detailed.
     * Silky, professional.
     */
    NEUMANN_U87(
            "Neumann U87",
            "Condenser",
            20.0f,
            20000.0f,
            0.95f,          // Slightly rolled bass
            1.0f,
            1.0f,
            1.0f,           // Smooth presence
            1.05f,
            1.15f,          // Silky air
            1.15f
    ),

    /**
     * Beyerdynamic M160 - Double ribbon.
     * Ribbon, hypercardioid. Tight pattern.
     * Detailed yet smooth.
     */
    BEYERDYNAMIC_M160(
            "Beyerdynamic M160",
            "Ribbon",
            40.0f,
            18000.0f,
            1.0f,
            1.05f,
            1.0f,
            0.95f,          // Smooth presence
            0.9f,
            0.75f,
            0.75f
    ),

    /**
     * Electro-Voice RE20 - Broadcast dynamic.
     * Dynamic, cardioid. Variable-D for less proximity.
     * Full, consistent.
     */
    EV_RE20(
            "Electro-Voice RE20",
            "Dynamic",
            45.0f,
            18000.0f,
            1.1f,           // Full bass
            1.0f,
            1.0f,
            1.0f,           // Flat presence
            0.95f,
            0.85f,
            0.5f            // Variable-D reduces proximity
    ),

    /**
     * Audio-Technica AT4050 - Modern condenser.
     * Condenser, multi-pattern. Clean, detailed.
     */
    AT_4050(
            "Audio-Technica AT4050",
            "Condenser",
            20.0f,
            18000.0f,
            1.0f,
            1.0f,
            1.0f,
            1.1f,           // Presence peak
            1.05f,
            1.0f,
            1.1f
    ),

    /**
     * Cascade Fathead - Budget ribbon.
     * Ribbon, bidirectional. Warm, smooth.
     */
    CASCADE_FATHEAD(
            "Cascade Fathead",
            "Ribbon",
            30.0f,
            14000.0f,
            1.15f,          // Warm bass
            1.1f,           // Rich low mids
            1.0f,
            0.85f,          // Smooth presence
            0.75f,          // Dark highs
            0.5f,
            0.65f
    );

    private final String displayName;
    private final String type;          // Dynamic, Condenser, Ribbon
    private final float lowFreq;
    private final float highFreq;
    private final float bass;           // <100Hz
    private final float lowMids;        // 200-500Hz
    private final float mids;           // 500-2kHz
    private final float presence;       // 2-6kHz
    private final float highs;          // 6-10kHz
    private final float air;            // 10-20kHz
    private final float proximityEffect;

    MicrophoneType(String displayName, String type, float lowFreq, float highFreq,
                   float bass, float lowMids, float mids, float presence,
                   float highs, float air, float proximityEffect) {
        this.displayName = displayName;
        this.type = type;
        this.lowFreq = lowFreq;
        this.highFreq = highFreq;
        this.bass = bass;
        this.lowMids = lowMids;
        this.mids = mids;
        this.presence = presence;
        this.highs = highs;
        this.air = air;
        this.proximityEffect = proximityEffect;
    }

    public String getDisplayName() { return displayName; }
    public String getType() { return type; }
    public float getLowFreq() { return lowFreq; }
    public float getHighFreq() { return highFreq; }
    public float getBass() { return bass; }
    public float getLowMids() { return lowMids; }
    public float getMids() { return mids; }
    public float getPresence() { return presence; }
    public float getHighs() { return highs; }
    public float getAir() { return air; }
    public float getProximityEffect() { return proximityEffect; }

    /**
     * Check if this is a ribbon microphone (needs special handling).
     */
    public boolean isRibbon() {
        return "Ribbon".equals(type);
    }

    /**
     * Check if this is a condenser microphone.
     */
    public boolean isCondenser() {
        return "Condenser".equals(type);
    }
}
