package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Vibrato effect - pitch modulation.
 *
 * <p>Modulates the pitch of the signal using an LFO. Unlike tremolo
 * which modulates volume, vibrato creates a pitch wobble effect.</p>
 *
 * <p>Features:
 * - Variable LFO rate and depth
 * - Multiple waveform shapes
 * - Smooth pitch transitions
 * - Optional rise time for gradual onset</p>
 */
public class VibratoEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "vibrato",
            "Vibrato",
            "Pitch modulation for expressive vibrato",
            EffectCategory.MODULATION
    );

    private static final String[] WAVEFORM_NAMES = {
            "Sine",
            "Triangle"
    };

    // Parameters
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter waveformParam;
    private final Parameter riseParam;

    // DSP components
    private DelayLine delayLineL;
    private DelayLine delayLineR;
    private LFO lfo;

    // Rise time state
    private float riseGain = 1.0f;
    private int riseSamples = 0;
    private int riseCounter = 0;

    public VibratoEffect() {
        super(METADATA);

        // Rate: 0.5 Hz to 10 Hz, default 5 Hz
        rateParam = addFloatParameter("rate", "Rate",
                "Speed of the vibrato. 4-7 Hz is typical for guitar.",
                0.5f, 10.0f, 5.0f, "Hz");

        // Depth: 0 to 100 cents, default 30 cents
        depthParam = addFloatParameter("depth", "Depth",
                "Amount of pitch variation in cents. Subtle (10-20) or wide (50+).",
                0.0f, 100.0f, 30.0f, "cents");

        // Waveform
        waveformParam = addChoiceParameter("waveform", "Wave",
                "Modulation shape. Sine = smooth, Triangle = more pronounced.",
                WAVEFORM_NAMES, 0);

        // Rise time: 0 to 1000 ms, default 0 (instant)
        riseParam = addFloatParameter("rise", "Rise",
                "Time for vibrato to reach full depth. Simulates natural finger vibrato.",
                0.0f, 1000.0f, 0.0f, "ms");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Maximum delay for 100 cents at lowest frequency
        // At 100 cents, we need about ±0.06 * period delay
        float maxDelayMs = 20.0f;
        delayLineL = new DelayLine(maxDelayMs, sampleRate);
        delayLineR = new DelayLine(maxDelayMs, sampleRate);

        lfo = new LFO(LFO.Waveform.SINE, rateParam.getValue(), sampleRate);

        riseGain = 1.0f;
        riseSamples = 0;
        riseCounter = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float rate = rateParam.getValue();
        float depthCents = depthParam.getValue();
        int waveformIdx = (int) waveformParam.getValue();
        float riseMs = riseParam.getValue();

        lfo.setFrequency(rate);
        lfo.setWaveform(waveformIdx == 1 ? LFO.Waveform.TRIANGLE : LFO.Waveform.SINE);

        // Calculate delay modulation depth
        // cents to delay: delay_ratio = 2^(cents/1200) - 1
        // For small values, approximately cents/1200 * ln(2) ≈ cents/1731
        float maxDelayMs = depthCents / 17.31f; // Approximate max delay variation

        // Base delay (center point)
        float baseDelayMs = maxDelayMs + 1.0f; // Add 1ms to ensure positive delay

        // Rise time handling
        if (riseMs > 0) {
            riseSamples = (int) (riseMs * sampleRate / 1000.0f);
        }

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Update rise gain
            if (riseCounter < riseSamples) {
                riseGain = (float) riseCounter / riseSamples;
                riseCounter++;
            } else {
                riseGain = 1.0f;
            }

            // Get LFO value (-1 to 1)
            float lfoValue = lfo.tick();

            // Calculate modulated delay
            float modulatedDelayMs = baseDelayMs + lfoValue * maxDelayMs * riseGain;
            float delaySamples = delayLineL.msToSamples(modulatedDelayMs);

            // Write to delay line
            delayLineL.write(sample);

            // Read with interpolation
            output[i] = delayLineL.readCubic(delaySamples);
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float rate = rateParam.getValue();
        float depthCents = depthParam.getValue();
        int waveformIdx = (int) waveformParam.getValue();
        float riseMs = riseParam.getValue();

        lfo.setFrequency(rate);
        lfo.setWaveform(waveformIdx == 1 ? LFO.Waveform.TRIANGLE : LFO.Waveform.SINE);

        float maxDelayMs = depthCents / 17.31f;
        float baseDelayMs = maxDelayMs + 1.0f;

        if (riseMs > 0) {
            riseSamples = (int) (riseMs * sampleRate / 1000.0f);
        }

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            if (riseCounter < riseSamples) {
                riseGain = (float) riseCounter / riseSamples;
                riseCounter++;
            } else {
                riseGain = 1.0f;
            }

            float lfoValue = lfo.tick();
            float modulatedDelayMs = baseDelayMs + lfoValue * maxDelayMs * riseGain;
            float delaySamples = delayLineL.msToSamples(modulatedDelayMs);

            delayLineL.write(inputL[i]);
            delayLineR.write(inputR[i]);

            outputL[i] = delayLineL.readCubic(delaySamples);
            outputR[i] = delayLineR.readCubic(delaySamples);
        }
    }

    @Override
    protected void onReset() {
        if (delayLineL != null) delayLineL.clear();
        if (delayLineR != null) delayLineR.clear();
        if (lfo != null) lfo.reset();
        riseGain = 1.0f;
        riseCounter = 0;
    }

    // Convenience setters
    public void setRate(float hz) {
        rateParam.setValue(hz);
    }

    public void setDepth(float cents) {
        depthParam.setValue(cents);
    }

    public void setWaveform(int index) {
        waveformParam.setValue(index);
    }

    public void setRise(float ms) {
        riseParam.setValue(ms);
    }
}
