package it.denzosoft.jfx2.audio;

/**
 * Configuration for the audio engine.
 *
 * <p>Note: Input and output devices are now configured per-node
 * (InputNode and OutputNode) rather than globally.</p>
 *
 * @param sampleRate     Sample rate in Hz (typically 44100)
 * @param bufferSize     Buffer size in samples (256, 512, 1024)
 * @param inputChannels  Number of input channels (1 for mono guitar)
 * @param outputChannels Number of output channels (2 for stereo)
 */
public record AudioConfig(
    int sampleRate,
    int bufferSize,
    int inputChannels,
    int outputChannels
) {
    /**
     * Default configuration: 44.1kHz, 2048 samples buffer, mono in, stereo out.
     */
    public static final AudioConfig DEFAULT = new AudioConfig(
        44100,  // sampleRate
        2048,   // bufferSize
        1,      // inputChannels (mono guitar)
        2       // outputChannels (stereo)
    );

    /**
     * Low latency configuration: smaller buffer for less delay.
     */
    public static final AudioConfig LOW_LATENCY = new AudioConfig(
        44100,
        256,
        1,
        2
    );

    /**
     * Safe configuration: larger buffer for stability.
     */
    public static final AudioConfig SAFE = new AudioConfig(
        44100,
        1024,
        1,
        2
    );

    /**
     * Calculate the latency in milliseconds for this configuration.
     * This is the buffer latency only, not including hardware latency.
     *
     * @return Latency in milliseconds
     */
    public double getBufferLatencyMs() {
        return (bufferSize * 1000.0) / sampleRate;
    }

    /**
     * Calculate total estimated round-trip latency (input + output buffers).
     *
     * @return Estimated round-trip latency in milliseconds
     */
    public double getEstimatedRoundTripLatencyMs() {
        // Input buffer + output buffer + some overhead
        return getBufferLatencyMs() * 2 + 2.0; // +2ms for hardware overhead estimate
    }

    /**
     * Validate the configuration.
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (sampleRate < 8000 || sampleRate > 192000) {
            throw new IllegalArgumentException("Sample rate must be between 8000 and 192000 Hz");
        }
        if (bufferSize < 64 || bufferSize > 16384) {
            throw new IllegalArgumentException("Buffer size must be between 64 and 16384 samples");
        }
        if (inputChannels < 1 || inputChannels > 2) {
            throw new IllegalArgumentException("Input channels must be 1 or 2");
        }
        if (outputChannels < 1 || outputChannels > 2) {
            throw new IllegalArgumentException("Output channels must be 1 or 2");
        }
    }

    @Override
    public String toString() {
        return String.format("AudioConfig[%dHz, %d samples, %dch->%dch, ~%.1fms latency]",
            sampleRate, bufferSize, inputChannels, outputChannels, getEstimatedRoundTripLatencyMs());
    }
}
