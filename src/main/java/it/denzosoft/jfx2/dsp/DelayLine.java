package it.denzosoft.jfx2.dsp;

/**
 * Delay line with linear interpolation support.
 *
 * <p>Circular buffer implementation for delay effects.
 * Supports fractional delay times via linear interpolation.</p>
 */
public class DelayLine {

    private float[] buffer;
    private int writeIndex;
    private int bufferSize;
    private int sampleRate;

    /**
     * Create a delay line.
     *
     * @param maxDelayMs Maximum delay time in milliseconds
     * @param sampleRate Sample rate in Hz
     */
    public DelayLine(float maxDelayMs, int sampleRate) {
        this.sampleRate = sampleRate;
        this.bufferSize = (int) (maxDelayMs * sampleRate / 1000.0f) + 2;  // +2 for interpolation
        this.buffer = new float[bufferSize];
        this.writeIndex = 0;
    }

    /**
     * Create a delay line with default 2 second max delay.
     */
    public DelayLine(int sampleRate) {
        this(2000.0f, sampleRate);
    }

    /**
     * Write a sample to the delay line.
     */
    public void write(float sample) {
        buffer[writeIndex] = sample;
        writeIndex = (writeIndex + 1) % bufferSize;
    }

    /**
     * Read a sample from the delay line at a specific delay time.
     *
     * @param delaySamples Delay in samples (can be fractional for interpolation)
     * @return Delayed sample
     */
    public float read(float delaySamples) {
        // Clamp delay to valid range
        if (delaySamples < 0) delaySamples = 0;
        if (delaySamples > bufferSize - 2) delaySamples = bufferSize - 2;

        // Calculate read position
        float readPos = writeIndex - 1 - delaySamples;
        while (readPos < 0) readPos += bufferSize;

        // Linear interpolation
        int index0 = (int) readPos;
        int index1 = (index0 + 1) % bufferSize;
        float frac = readPos - index0;

        return buffer[index0] * (1.0f - frac) + buffer[index1] * frac;
    }

    /**
     * Read a sample at integer delay (no interpolation, faster).
     *
     * @param delaySamples Delay in samples (integer)
     * @return Delayed sample
     */
    public float readNoInterp(int delaySamples) {
        if (delaySamples < 0) delaySamples = 0;
        if (delaySamples >= bufferSize) delaySamples = bufferSize - 1;

        int readIndex = writeIndex - 1 - delaySamples;
        while (readIndex < 0) readIndex += bufferSize;

        return buffer[readIndex];
    }

    /**
     * Read with cubic interpolation (higher quality, for modulated delays).
     *
     * @param delaySamples Delay in samples (can be fractional)
     * @return Delayed sample with cubic interpolation
     */
    public float readCubic(float delaySamples) {
        if (delaySamples < 1) delaySamples = 1;
        if (delaySamples > bufferSize - 3) delaySamples = bufferSize - 3;

        float readPos = writeIndex - 1 - delaySamples;
        while (readPos < 0) readPos += bufferSize;

        int index0 = ((int) readPos - 1 + bufferSize) % bufferSize;
        int index1 = (index0 + 1) % bufferSize;
        int index2 = (index1 + 1) % bufferSize;
        int index3 = (index2 + 1) % bufferSize;

        float frac = readPos - (int) readPos;

        float y0 = buffer[index0];
        float y1 = buffer[index1];
        float y2 = buffer[index2];
        float y3 = buffer[index3];

        // Cubic Hermite interpolation
        float c0 = y1;
        float c1 = 0.5f * (y2 - y0);
        float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
        float c3 = 0.5f * (y3 - y0) + 1.5f * (y1 - y2);

        return ((c3 * frac + c2) * frac + c1) * frac + c0;
    }

    /**
     * Process a sample through the delay line (write and read).
     *
     * @param input        Input sample to write
     * @param delaySamples Delay time in samples
     * @return Delayed output sample
     */
    public float process(float input, float delaySamples) {
        float output = read(delaySamples);
        write(input);
        return output;
    }

    /**
     * Convert milliseconds to samples.
     */
    public float msToSamples(float ms) {
        return ms * sampleRate / 1000.0f;
    }

    /**
     * Convert BPM and note division to samples.
     *
     * @param bpm          Tempo in beats per minute
     * @param noteDivision Note division (1 = whole, 2 = half, 4 = quarter, etc.)
     * @param dotted       If true, add 50% to duration
     * @param triplet      If true, multiply by 2/3
     * @return Delay time in samples
     */
    public float bpmToSamples(float bpm, int noteDivision, boolean dotted, boolean triplet) {
        // Quarter note duration in seconds
        float quarterNoteSec = 60.0f / bpm;

        // Note duration relative to quarter note
        float noteDuration = quarterNoteSec * (4.0f / noteDivision);

        if (dotted) {
            noteDuration *= 1.5f;
        }
        if (triplet) {
            noteDuration *= 2.0f / 3.0f;
        }

        return noteDuration * sampleRate;
    }

    /**
     * Clear the delay buffer.
     */
    public void clear() {
        java.util.Arrays.fill(buffer, 0.0f);
        writeIndex = 0;
    }

    /**
     * Get maximum delay in samples.
     */
    public int getMaxDelaySamples() {
        return bufferSize - 2;
    }

    /**
     * Get maximum delay in milliseconds.
     */
    public float getMaxDelayMs() {
        return (bufferSize - 2) * 1000.0f / sampleRate;
    }

    /**
     * Resize the delay line (clears buffer).
     */
    public void resize(float maxDelayMs, int sampleRate) {
        this.sampleRate = sampleRate;
        this.bufferSize = (int) (maxDelayMs * sampleRate / 1000.0f) + 2;
        this.buffer = new float[bufferSize];
        this.writeIndex = 0;
    }
}
