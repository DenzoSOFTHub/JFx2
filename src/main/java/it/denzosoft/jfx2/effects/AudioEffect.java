package it.denzosoft.jfx2.effects;

import java.util.List;

/**
 * Interface for all audio effects.
 *
 * <p>Effects process audio buffers and can be controlled via parameters.</p>
 */
public interface AudioEffect {

    /**
     * Get the effect metadata.
     */
    EffectMetadata getMetadata();

    /**
     * Get all parameters for this effect.
     */
    List<Parameter> getParameters();

    /**
     * Get a parameter by ID.
     *
     * @param id Parameter identifier
     * @return The parameter, or null if not found
     */
    Parameter getParameter(String id);

    /**
     * Prepare the effect for processing.
     * Called when sample rate or buffer size changes.
     *
     * @param sampleRate    Sample rate in Hz
     * @param maxFrameCount Maximum frames per process call
     */
    void prepare(int sampleRate, int maxFrameCount);

    /**
     * Set the number of input channels.
     * Used by AUTO stereo mode to determine mono vs stereo processing.
     *
     * @param channels Number of input channels (1 = mono, 2 = stereo)
     */
    void setInputChannels(int channels);

    /**
     * Get the number of input channels.
     *
     * @return Number of input channels (1 = mono, 2 = stereo)
     */
    int getInputChannels();

    /**
     * Get the number of output channels after last processing.
     * This is set by processStereo based on the stereo mode:
     * - MONO: always 1 (L == R)
     * - STEREO: always 2 (L may differ from R)
     * - AUTO: matches input channels
     *
     * @return Number of output channels (1 = mono, 2 = stereo)
     */
    int getOutputChannels();

    /**
     * Process audio through the effect (mono).
     *
     * @param input      Input audio buffer
     * @param output     Output audio buffer
     * @param frameCount Number of frames to process
     */
    void process(float[] input, float[] output, int frameCount);

    /**
     * Process audio through the effect (stereo).
     * This method handles stereo mode routing based on the effect's stereo mode setting.
     *
     * @param inputL     Left input audio buffer
     * @param inputR     Right input audio buffer
     * @param outputL    Left output audio buffer
     * @param outputR    Right output audio buffer
     * @param frameCount Number of frames to process
     */
    void processStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount);

    /**
     * Get the current stereo mode.
     */
    StereoMode getStereoMode();

    /**
     * Set the stereo mode.
     */
    void setStereoMode(StereoMode mode);

    /**
     * Check if the effect is bypassed.
     */
    boolean isBypassed();

    /**
     * Set bypass state.
     *
     * @param bypassed true to bypass the effect
     */
    void setBypassed(boolean bypassed);

    /**
     * Reset the effect state (clear delay lines, envelopes, etc.).
     */
    void reset();

    /**
     * Release any resources.
     */
    void release();

    /**
     * Get the processing latency in samples.
     * Override if the effect introduces latency (e.g., convolution, lookahead).
     *
     * @return Latency in samples (default 0)
     */
    default int getLatency() {
        return 0;
    }

    /**
     * Get the number of parameters per row for UI layout.
     * Override to organize parameters into multiple rows.
     *
     * <p>Example: {5, 5, 5, 5, 5} means 5 rows with 5 parameters each.
     * Return null or empty array for default single-row layout.</p>
     *
     * @return Array of parameter counts per row, or null for default layout
     */
    default int[] getParameterRowSizes() {
        return null;
    }
}
