package it.denzosoft.jfx2.effects;

/**
 * Stereo processing mode for effects.
 */
public enum StereoMode {
    /**
     * Mono processing: process only left channel, copy to right.
     */
    MONO("Mono", "Process left channel only, copy to right"),

    /**
     * Stereo processing: process left and right channels independently.
     */
    STEREO("Stereo", "Process left and right channels independently"),

    /**
     * Auto: detect from input signal.
     * If input is mono (L=R or R is silent), process as mono.
     * If input is stereo (L != R), process as stereo.
     */
    AUTO("Auto", "Automatically detect mono or stereo from input");

    private final String displayName;
    private final String description;

    StereoMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get array of display names for UI combo box.
     */
    public static String[] getDisplayNames() {
        StereoMode[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }
}
