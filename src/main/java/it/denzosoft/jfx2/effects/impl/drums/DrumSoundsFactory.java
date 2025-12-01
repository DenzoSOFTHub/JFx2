package it.denzosoft.jfx2.effects.impl.drums;

/**
 * Factory for creating drum sound providers.
 *
 * <p>Supports multiple implementations:</p>
 * <ul>
 *   <li><b>SYNTHESIZED</b>: High-quality synthesized drums (default, always works)</li>
 *   <li><b>SF2</b>: Drums loaded from SF2 soundfont files (portable, no special JVM flags)</li>
 *   <li><b>MIDI</b>: Drums rendered from Java MIDI synthesizer (requires --add-exports)</li>
 * </ul>
 */
public class DrumSoundsFactory {

    /**
     * Available drum sound provider types.
     */
    public enum ProviderType {
        /**
         * Synthesized drums using DSP algorithms.
         * Always available, no external dependencies.
         */
        SYNTHESIZED("Synthesized", "High-quality synthesized drums"),

        /**
         * Drums loaded from SF2 soundfont file.
         * Uses our custom SF2 parser - no special JVM flags needed.
         */
        SF2("SoundFont", "Drums from SF2 soundfont file"),

        /**
         * MIDI-rendered drums using Java's built-in synthesizer.
         * Requires --add-exports java.desktop/com.sun.media.sound=ALL-UNNAMED
         */
        MIDI("MIDI", "Drums from Java MIDI synthesizer");

        private final String displayName;
        private final String description;

        ProviderType(String displayName, String description) {
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

    // Default provider type
    private static ProviderType defaultType = ProviderType.SYNTHESIZED;

    /**
     * Create a drum sound provider with the default type.
     *
     * @param sampleRate Audio sample rate
     * @return A drum sound provider
     */
    public static DrumSoundProvider create(int sampleRate) {
        return create(defaultType, sampleRate);
    }

    /**
     * Create a drum sound provider of the specified type.
     *
     * @param type       The provider type
     * @param sampleRate Audio sample rate
     * @return A drum sound provider
     */
    public static DrumSoundProvider create(ProviderType type, int sampleRate) {
        return switch (type) {
            case SYNTHESIZED -> new DrumSounds(sampleRate);
            case SF2 -> {
                if (SF2DrumSounds.isAvailable()) {
                    yield new SF2DrumSounds(sampleRate);
                } else {
                    yield new DrumSounds(sampleRate);
                }
            }
            case MIDI -> {
                if (MidiDrumSounds.isAvailable()) {
                    yield new MidiDrumSounds(sampleRate);
                } else {
                    yield new DrumSounds(sampleRate);
                }
            }
        };
    }

    /**
     * Get the default provider type.
     */
    public static ProviderType getDefaultType() {
        return defaultType;
    }

    /**
     * Set the default provider type.
     *
     * @param type The new default type
     */
    public static void setDefaultType(ProviderType type) {
        defaultType = type;
    }

    /**
     * Check if a provider type is available on this system.
     *
     * @param type The provider type to check
     * @return true if the provider can be used
     */
    public static boolean isAvailable(ProviderType type) {
        return switch (type) {
            case SYNTHESIZED -> true; // Always available
            case SF2 -> SF2DrumSounds.isAvailable();
            case MIDI -> MidiDrumSounds.isAvailable();
        };
    }

    /**
     * Get all available provider types on this system.
     *
     * @return Array of available provider types
     */
    public static ProviderType[] getAvailableTypes() {
        return java.util.Arrays.stream(ProviderType.values())
                .filter(DrumSoundsFactory::isAvailable)
                .toArray(ProviderType[]::new);
    }

    /**
     * Get display names for all provider types.
     *
     * @return Array of display names
     */
    public static String[] getProviderNames() {
        ProviderType[] types = ProviderType.values();
        String[] names = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            names[i] = types[i].getDisplayName();
            if (!isAvailable(types[i])) {
                names[i] += " (unavailable)";
            }
        }
        return names;
    }
}
