package it.denzosoft.jfx2.effects;

/**
 * Categories for organizing effects.
 */
public enum EffectCategory {
    INPUT_SOURCE("Input Source", "Audio generators and file players"),
    OUTPUT_SINK("Output", "Audio recorders and file writers"),
    DYNAMICS("Dynamics", "Compressor, Gate, Limiter"),
    GAIN("Gain", "Volume, Boost, Gain stages"),
    DISTORTION("Distortion", "Overdrive, Distortion, Fuzz"),
    MODULATION("Modulation", "Chorus, Phaser, Tremolo, Flanger"),
    DELAY("Delay", "Delay, Echo"),
    REVERB("Reverb", "Room, Hall, Plate reverbs"),
    EQ("EQ", "Equalizers, Tone controls"),
    FILTER("Filter", "Wah, Auto-wah, Envelope filters"),
    AMP_SIM("Amp Simulation", "Amp models, Cabinet simulation"),
    PITCH("Pitch", "Pitch shifter, Octaver, Harmonizer"),
    UTILITY("Utility", "Splitter, Mixer, Volume");

    private final String displayName;
    private final String description;

    EffectCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
