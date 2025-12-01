package it.denzosoft.jfx2.dsp;

/**
 * Biquad IIR filter implementation.
 *
 * <p>Standard second-order filter (biquad) supporting various filter types.
 * Uses Direct Form II Transposed for numerical stability.</p>
 *
 * <p>Transfer function:
 * H(z) = (b0 + b1*z^-1 + b2*z^-2) / (1 + a1*z^-1 + a2*z^-2)</p>
 */
public class BiquadFilter {

    // Coefficients
    private float b0, b1, b2;
    private float a1, a2;

    // State (Direct Form II Transposed)
    private float z1, z2;

    private int sampleRate;
    private FilterType type;
    private float frequency;
    private float q;
    private float gainDb;

    /**
     * Create a biquad filter.
     */
    public BiquadFilter() {
        this.sampleRate = 44100;
        this.type = FilterType.LOWPASS;
        this.frequency = 1000.0f;
        this.q = 0.707f;  // Butterworth
        this.gainDb = 0.0f;
        reset();
        calculateCoefficients();
    }

    /**
     * Set sample rate and recalculate coefficients.
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        calculateCoefficients();
    }

    /**
     * Configure filter parameters.
     *
     * @param type      Filter type
     * @param frequency Cutoff/center frequency in Hz
     * @param q         Q factor (bandwidth for bandpass, resonance for LP/HP)
     * @param gainDb    Gain in dB (for peak/shelf filters)
     */
    public void configure(FilterType type, float frequency, float q, float gainDb) {
        this.type = type;
        this.frequency = frequency;
        this.q = q;
        this.gainDb = gainDb;
        calculateCoefficients();
    }

    /**
     * Set frequency only.
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
        calculateCoefficients();
    }

    /**
     * Set Q only.
     */
    public void setQ(float q) {
        this.q = q;
        calculateCoefficients();
    }

    /**
     * Set gain in dB (for peak/shelf filters).
     */
    public void setGainDb(float gainDb) {
        this.gainDb = gainDb;
        calculateCoefficients();
    }

    /**
     * Set filter type.
     */
    public void setType(FilterType type) {
        this.type = type;
        calculateCoefficients();
    }

    /**
     * Calculate filter coefficients based on current parameters.
     * Based on Audio EQ Cookbook by Robert Bristow-Johnson.
     */
    private void calculateCoefficients() {
        float w0 = (float) (2.0 * Math.PI * frequency / sampleRate);
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float alpha = sinW0 / (2.0f * q);

        // For shelving filters
        float A = (float) Math.pow(10.0, gainDb / 40.0);

        float a0;  // Normalizing coefficient

        switch (type) {
            case LOWPASS:
                b0 = (1.0f - cosW0) / 2.0f;
                b1 = 1.0f - cosW0;
                b2 = (1.0f - cosW0) / 2.0f;
                a0 = 1.0f + alpha;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha;
                break;

            case HIGHPASS:
                b0 = (1.0f + cosW0) / 2.0f;
                b1 = -(1.0f + cosW0);
                b2 = (1.0f + cosW0) / 2.0f;
                a0 = 1.0f + alpha;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha;
                break;

            case BANDPASS:
                b0 = alpha;
                b1 = 0.0f;
                b2 = -alpha;
                a0 = 1.0f + alpha;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha;
                break;

            case NOTCH:
                b0 = 1.0f;
                b1 = -2.0f * cosW0;
                b2 = 1.0f;
                a0 = 1.0f + alpha;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha;
                break;

            case PEAK:
                b0 = 1.0f + alpha * A;
                b1 = -2.0f * cosW0;
                b2 = 1.0f - alpha * A;
                a0 = 1.0f + alpha / A;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha / A;
                break;

            case LOWSHELF: {
                float sqrtA = (float) Math.sqrt(A);
                float sqrtA2Alpha = 2.0f * sqrtA * alpha;
                b0 = A * ((A + 1.0f) - (A - 1.0f) * cosW0 + sqrtA2Alpha);
                b1 = 2.0f * A * ((A - 1.0f) - (A + 1.0f) * cosW0);
                b2 = A * ((A + 1.0f) - (A - 1.0f) * cosW0 - sqrtA2Alpha);
                a0 = (A + 1.0f) + (A - 1.0f) * cosW0 + sqrtA2Alpha;
                a1 = -2.0f * ((A - 1.0f) + (A + 1.0f) * cosW0);
                a2 = (A + 1.0f) + (A - 1.0f) * cosW0 - sqrtA2Alpha;
                break;
            }

            case HIGHSHELF: {
                float sqrtA = (float) Math.sqrt(A);
                float sqrtA2Alpha = 2.0f * sqrtA * alpha;
                b0 = A * ((A + 1.0f) + (A - 1.0f) * cosW0 + sqrtA2Alpha);
                b1 = -2.0f * A * ((A - 1.0f) + (A + 1.0f) * cosW0);
                b2 = A * ((A + 1.0f) + (A - 1.0f) * cosW0 - sqrtA2Alpha);
                a0 = (A + 1.0f) - (A - 1.0f) * cosW0 + sqrtA2Alpha;
                a1 = 2.0f * ((A - 1.0f) - (A + 1.0f) * cosW0);
                a2 = (A + 1.0f) - (A - 1.0f) * cosW0 - sqrtA2Alpha;
                break;
            }

            case ALLPASS:
                b0 = 1.0f - alpha;
                b1 = -2.0f * cosW0;
                b2 = 1.0f + alpha;
                a0 = 1.0f + alpha;
                a1 = -2.0f * cosW0;
                a2 = 1.0f - alpha;
                break;

            default:
                // Passthrough
                b0 = 1.0f;
                b1 = 0.0f;
                b2 = 0.0f;
                a0 = 1.0f;
                a1 = 0.0f;
                a2 = 0.0f;
        }

        // Normalize coefficients
        b0 /= a0;
        b1 /= a0;
        b2 /= a0;
        a1 /= a0;
        a2 /= a0;
    }

    /**
     * Process a single sample.
     */
    public float process(float input) {
        // Direct Form II Transposed
        float output = b0 * input + z1;
        z1 = b1 * input - a1 * output + z2;
        z2 = b2 * input - a2 * output;
        return output;
    }

    /**
     * Process a buffer of samples in place.
     */
    public void process(float[] buffer, int frameCount) {
        for (int i = 0; i < frameCount && i < buffer.length; i++) {
            buffer[i] = process(buffer[i]);
        }
    }

    /**
     * Process from input buffer to output buffer.
     */
    public void process(float[] input, float[] output, int frameCount) {
        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            output[i] = process(input[i]);
        }
    }

    /**
     * Reset filter state.
     */
    public void reset() {
        z1 = 0.0f;
        z2 = 0.0f;
    }

    // Getters
    public FilterType getType() {
        return type;
    }

    public float getFrequency() {
        return frequency;
    }

    public float getQ() {
        return q;
    }

    public float getGainDb() {
        return gainDb;
    }
}
