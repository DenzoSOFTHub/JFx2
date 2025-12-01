package it.denzosoft.jfx2.dsp;

/**
 * Lowpass feedback comb filter for reverb.
 *
 * <p>Creates resonant frequencies at harmonics of the fundamental delay time.
 * Includes a lowpass filter in the feedback loop for natural high-frequency damping.</p>
 */
public class CombFilter {

    private float[] buffer;
    private int bufferSize;
    private int writeIndex;
    private float feedback;
    private float damp1;
    private float damp2;
    private float filterStore;

    /**
     * Create a comb filter.
     *
     * @param delaySamples Delay length in samples
     * @param feedback     Feedback coefficient (controls decay time)
     * @param damp         Damping coefficient (0-1, higher = more HF damping)
     */
    public CombFilter(int delaySamples, float feedback, float damp) {
        this.bufferSize = delaySamples;
        this.buffer = new float[bufferSize];
        this.writeIndex = 0;
        this.feedback = feedback;
        setDamp(damp);
        this.filterStore = 0.0f;
    }

    /**
     * Process a sample through the comb filter.
     */
    public float process(float input) {
        // Read from buffer
        float output = buffer[writeIndex];

        // Lowpass filter in feedback loop
        filterStore = output * damp2 + filterStore * damp1;

        // Write to buffer
        buffer[writeIndex] = input + filterStore * feedback;

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
     * Set damping coefficient.
     *
     * @param damp Damping (0-1, higher = more HF damping)
     */
    public void setDamp(float damp) {
        this.damp1 = damp;
        this.damp2 = 1.0f - damp;
    }

    /**
     * Get damping coefficient.
     */
    public float getDamp() {
        return damp1;
    }

    /**
     * Clear the buffer.
     */
    public void clear() {
        java.util.Arrays.fill(buffer, 0.0f);
        filterStore = 0.0f;
        writeIndex = 0;
    }

    /**
     * Get delay length in samples.
     */
    public int getDelaySamples() {
        return bufferSize;
    }
}
