package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Phaser effect with 6 all-pass filter stages.
 *
 * <p>Creates a sweeping, swirling effect by modulating the cutoff
 * frequencies of a series of all-pass filters.</p>
 */
public class PhaserEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "phaser",
            "Phaser",
            "6-stage phaser with deep modulation",
            EffectCategory.MODULATION
    );

    private static final int NUM_STAGES = 6;

    // Frequency range for the all-pass filters
    private static final float MIN_FREQ = 100.0f;
    private static final float MAX_FREQ = 4000.0f;

    // Parameters
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter feedbackParam;
    private final Parameter mixParam;
    private final Parameter centerParam;

    // All-pass filter state - Left channel
    private float[] apStateL;
    private float feedbackL;
    private LFO lfoL;

    // All-pass filter state - Right channel
    private float[] apStateR;
    private float feedbackR;
    private LFO lfoR;

    public PhaserEffect() {
        super(METADATA);

        // Rate: 0.05 Hz to 5 Hz, default 0.5 Hz
        rateParam = addFloatParameter("rate", "Rate",
                "Speed of the filter sweep. Slow rates create hypnotic sweeps, fast rates add movement.",
                0.05f, 5.0f, 0.5f, "Hz");

        // Depth: 0% to 100%, default 70%
        depthParam = addFloatParameter("depth", "Depth",
                "Range of the frequency sweep. Higher values create more dramatic swooshing effect.",
                0.0f, 100.0f, 70.0f, "%");

        // Feedback: -90% to +90%, default 40%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Intensity of the effect. Negative values invert phase for different character.",
                -90.0f, 90.0f, 40.0f, "%");

        // Mix: 0% (dry) to 100% (wet), default 50%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and phased signal. Higher values intensify the effect.",
                0.0f, 100.0f, 50.0f, "%");

        // Center frequency: 200 Hz to 2000 Hz, default 800 Hz
        centerParam = addFloatParameter("center", "Center",
                "Center frequency of the sweep. Lower values affect bass, higher affects treble.",
                200.0f, 2000.0f, 800.0f, "Hz");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Initialize all-pass filter state - Left
        apStateL = new float[NUM_STAGES];
        feedbackL = 0.0f;
        lfoL = new LFO(LFO.Waveform.SINE, rateParam.getValue(), sampleRate);

        // Initialize all-pass filter state - Right (offset phase for stereo)
        apStateR = new float[NUM_STAGES];
        feedbackR = 0.0f;
        lfoR = new LFO(LFO.Waveform.SINE, rateParam.getValue(), sampleRate);
        lfoR.setPhase(0.5f);  // 180 degrees out of phase for stereo width
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float fb = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float centerFreq = centerParam.getValue();

        // Update LFO frequency
        lfoL.setFrequency(rate);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Get LFO value and calculate sweep frequency
            float lfoValue = lfoL.tick();

            // Calculate frequency sweep (logarithmic)
            float freqRatio = MAX_FREQ / MIN_FREQ;
            float sweepRange = depth * 2.0f;  // Full sweep range

            float freq = centerFreq * (float) Math.pow(freqRatio, lfoValue * sweepRange * 0.5f);
            freq = Math.max(MIN_FREQ, Math.min(MAX_FREQ, freq));

            // Calculate all-pass coefficient from frequency
            float w = (float) Math.tan(Math.PI * freq / sampleRate);
            float coef = (w - 1.0f) / (w + 1.0f);

            // Process through all-pass stages with feedback
            float sample = dry + feedbackL * fb;

            for (int stage = 0; stage < NUM_STAGES; stage++) {
                float newState = sample - coef * apStateL[stage];
                sample = coef * newState + apStateL[stage];
                apStateL[stage] = newState;
            }

            // Store feedback sample
            feedbackL = sample;

            // Mix dry and wet
            output[i] = dry * (1.0f - mix) + sample * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float fb = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float centerFreq = centerParam.getValue();

        // Update LFO frequencies
        lfoL.setFrequency(rate);
        lfoR.setFrequency(rate);

        float freqRatio = MAX_FREQ / MIN_FREQ;
        float sweepRange = depth * 2.0f;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Get LFO values
            float lfoValueL = lfoL.tick();
            float lfoValueR = lfoR.tick();

            // Calculate frequencies
            float freqL = centerFreq * (float) Math.pow(freqRatio, lfoValueL * sweepRange * 0.5f);
            freqL = Math.max(MIN_FREQ, Math.min(MAX_FREQ, freqL));
            float freqR = centerFreq * (float) Math.pow(freqRatio, lfoValueR * sweepRange * 0.5f);
            freqR = Math.max(MIN_FREQ, Math.min(MAX_FREQ, freqR));

            // Calculate coefficients
            float wL = (float) Math.tan(Math.PI * freqL / sampleRate);
            float coefL = (wL - 1.0f) / (wL + 1.0f);
            float wR = (float) Math.tan(Math.PI * freqR / sampleRate);
            float coefR = (wR - 1.0f) / (wR + 1.0f);

            // Process Left channel
            float sampleL = dryL + feedbackL * fb;
            for (int stage = 0; stage < NUM_STAGES; stage++) {
                float newState = sampleL - coefL * apStateL[stage];
                sampleL = coefL * newState + apStateL[stage];
                apStateL[stage] = newState;
            }
            feedbackL = sampleL;

            // Process Right channel
            float sampleR = dryR + feedbackR * fb;
            for (int stage = 0; stage < NUM_STAGES; stage++) {
                float newState = sampleR - coefR * apStateR[stage];
                sampleR = coefR * newState + apStateR[stage];
                apStateR[stage] = newState;
            }
            feedbackR = sampleR;

            // Mix
            outputL[i] = dryL * (1.0f - mix) + sampleL * mix;
            outputR[i] = dryR * (1.0f - mix) + sampleR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (apStateL != null) {
            java.util.Arrays.fill(apStateL, 0.0f);
        }
        if (apStateR != null) {
            java.util.Arrays.fill(apStateR, 0.0f);
        }
        if (lfoL != null) {
            lfoL.reset();
        }
        if (lfoR != null) {
            lfoR.reset();
            lfoR.setPhase(0.5f);  // Restore stereo offset
        }
        feedbackL = 0.0f;
        feedbackR = 0.0f;
    }

    // Convenience setters
    public void setRate(float hz) {
        rateParam.setValue(hz);
    }

    public void setDepth(float percent) {
        depthParam.setValue(percent);
    }

    public void setFeedback(float percent) {
        feedbackParam.setValue(percent);
    }

    public void setMix(float percent) {
        mixParam.setValue(percent);
    }

    public void setCenter(float hz) {
        centerParam.setValue(hz);
    }
}
