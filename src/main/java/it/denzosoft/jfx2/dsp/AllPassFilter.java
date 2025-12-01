package it.denzosoft.jfx2.dsp;

/**
 * All-pass filter for reverb diffusion.
 *
 * <p>Schroeder all-pass filter: passes all frequencies at equal amplitude
 * but changes the phase. Used in reverb algorithms for diffusion.</p>
 *
 * <p>Transfer function: H(z) = (-g + z^-N) / (1 - g*z^-N)</p>
 */
public class AllPassFilter {

    private float[] buffer;
    private int bufferSize;
    private int writeIndex;
    private float feedback;

    /**
     * Create an all-pass filter.
     *
     * @param delaySamples Delay length in samples
     * @param feedback     Feedback coefficient (typically 0.5-0.7)
     */
    public AllPassFilter(int delaySamples, float feedback) {
        this.bufferSize = delaySamples;
        this.buffer = new float[bufferSize];
        this.writeIndex = 0;
        this.feedback = feedback;
    }

    /**
     * Process a sample through the all-pass filter.
     */
    public float process(float input) {
        // Read from buffer
        float bufferOut = buffer[writeIndex];

        // Calculate output
        float output = -input + bufferOut;

        // Write to buffer (input + feedback * bufferOut)
        buffer[writeIndex] = input + feedback * bufferOut;

        // Advance write position
        writeIndex = (writeIndex + 1) % bufferSize;

        return output;
    }

    /**
     * Set feedback coefficient.
     */
    public void setFeedback(float feedback) {
        this.feedback = feedback;
    }

    /**
     * Get feedback coefficient.
     */
    public float getFeedback() {
        return feedback;
    }

    /**
     * Clear the buffer.
     */
    public void clear() {
        java.util.Arrays.fill(buffer, 0.0f);
        writeIndex = 0;
    }

    /**
     * Get delay length in samples.
     */
    public int getDelaySamples() {
        return bufferSize;
    }
}
