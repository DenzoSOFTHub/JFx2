package it.denzosoft.jfx2.dsp;

import java.util.Random;

/**
 * Low Frequency Oscillator for modulation effects.
 *
 * <p>Supports multiple waveform types:
 * - Sine: Smooth, classic modulation
 * - Triangle: Linear sweep, good for vibrato
 * - Square: Abrupt changes, tremolo effect
 * - Random (S&H): Sample and hold noise
 * </p>
 */
public class LFO {

    /**
     * LFO waveform types.
     */
    public enum Waveform {
        SINE,
        TRIANGLE,
        SQUARE,
        RANDOM,
        SAWTOOTH_UP,
        SAWTOOTH_DOWN
    }

    private Waveform waveform;
    private float frequency;
    private int sampleRate;
    private double phase;
    private double phaseIncrement;

    // Random generator for S&H
    private Random random;
    private float randomValue;
    private double lastRandomPhase;

    /**
     * Create an LFO.
     *
     * @param waveform   Waveform type
     * @param frequency  Frequency in Hz
     * @param sampleRate Sample rate in Hz
     */
    public LFO(Waveform waveform, float frequency, int sampleRate) {
        this.waveform = waveform;
        this.sampleRate = sampleRate;
        this.phase = 0.0;
        this.random = new Random();
        this.randomValue = random.nextFloat() * 2.0f - 1.0f;
        this.lastRandomPhase = 0.0;
        setFrequency(frequency);
    }

    /**
     * Create a sine LFO.
     */
    public LFO(float frequency, int sampleRate) {
        this(Waveform.SINE, frequency, sampleRate);
    }

    /**
     * Set the oscillator frequency.
     *
     * @param frequency Frequency in Hz
     */
    public void setFrequency(float frequency) {
        this.frequency = frequency;
        this.phaseIncrement = (2.0 * Math.PI * frequency) / sampleRate;
    }

    /**
     * Get the current frequency.
     */
    public float getFrequency() {
        return frequency;
    }

    /**
     * Set the waveform type.
     */
    public void setWaveform(Waveform waveform) {
        this.waveform = waveform;
    }

    /**
     * Get the waveform type.
     */
    public Waveform getWaveform() {
        return waveform;
    }

    /**
     * Set sample rate and recalculate phase increment.
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        setFrequency(frequency);
    }

    /**
     * Get the next sample from the LFO.
     *
     * @return Value in range -1.0 to +1.0
     */
    public float tick() {
        float output;

        switch (waveform) {
            case SINE:
                output = (float) Math.sin(phase);
                break;

            case TRIANGLE:
                // Convert phase (0 to 2*PI) to triangle (-1 to +1)
                double normalizedPhase = phase / (2.0 * Math.PI);
                if (normalizedPhase < 0.5) {
                    output = (float) (4.0 * normalizedPhase - 1.0);
                } else {
                    output = (float) (3.0 - 4.0 * normalizedPhase);
                }
                break;

            case SQUARE:
                output = phase < Math.PI ? 1.0f : -1.0f;
                break;

            case RANDOM:
                // Sample and hold: new random value each cycle
                if (phase < lastRandomPhase) {
                    randomValue = random.nextFloat() * 2.0f - 1.0f;
                }
                lastRandomPhase = phase;
                output = randomValue;
                break;

            case SAWTOOTH_UP:
                output = (float) (phase / Math.PI - 1.0);
                break;

            case SAWTOOTH_DOWN:
                output = (float) (1.0 - phase / Math.PI);
                break;

            default:
                output = 0.0f;
        }

        // Advance phase
        phase += phaseIncrement;
        while (phase >= 2.0 * Math.PI) {
            phase -= 2.0 * Math.PI;
        }

        return output;
    }

    /**
     * Get the next sample scaled to a range.
     *
     * @param min Minimum output value
     * @param max Maximum output value
     * @return Scaled LFO value
     */
    public float tick(float min, float max) {
        float normalized = (tick() + 1.0f) * 0.5f;  // 0 to 1
        return min + normalized * (max - min);
    }

    /**
     * Get the next sample with depth modulation.
     *
     * @param center Center value
     * @param depth  Modulation depth (0 to 1)
     * @return Modulated value: center +/- (center * depth)
     */
    public float tickWithDepth(float center, float depth) {
        return center + tick() * center * depth;
    }

    /**
     * Reset the phase to zero.
     */
    public void reset() {
        phase = 0.0;
        lastRandomPhase = 0.0;
    }

    /**
     * Set the phase directly.
     *
     * @param phase Phase in radians (0 to 2*PI)
     */
    public void setPhase(double phase) {
        this.phase = phase % (2.0 * Math.PI);
        if (this.phase < 0) this.phase += 2.0 * Math.PI;
    }

    /**
     * Get the current phase.
     */
    public double getPhase() {
        return phase;
    }

    /**
     * Create multiple LFOs with spread phase for stereo/multi-voice effects.
     *
     * @param count      Number of LFOs
     * @param waveform   Waveform type
     * @param frequency  Frequency in Hz
     * @param sampleRate Sample rate
     * @return Array of LFOs with evenly distributed phases
     */
    public static LFO[] createSpread(int count, Waveform waveform, float frequency, int sampleRate) {
        LFO[] lfos = new LFO[count];
        double phaseSpread = 2.0 * Math.PI / count;

        for (int i = 0; i < count; i++) {
            lfos[i] = new LFO(waveform, frequency, sampleRate);
            lfos[i].setPhase(i * phaseSpread);
        }

        return lfos;
    }
}
