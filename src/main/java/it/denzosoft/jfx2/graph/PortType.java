package it.denzosoft.jfx2.graph;

/**
 * Type of signal that a port carries.
 */
public enum PortType {

    /**
     * Mono audio signal (single channel).
     */
    AUDIO_MONO(1),

    /**
     * Stereo audio signal (two channels, interleaved).
     */
    AUDIO_STEREO(2);

    private final int channelCount;

    PortType(int channelCount) {
        this.channelCount = channelCount;
    }

    /**
     * Get the number of audio channels for this port type.
     */
    public int getChannelCount() {
        return channelCount;
    }

    /**
     * Check if this port type is compatible with another for connection.
     * Mono can connect to stereo (will be duplicated).
     * Stereo can connect to mono (will be downmixed).
     */
    public boolean isCompatibleWith(PortType other) {
        // All audio types are compatible (conversion happens automatically)
        return true;
    }
}
