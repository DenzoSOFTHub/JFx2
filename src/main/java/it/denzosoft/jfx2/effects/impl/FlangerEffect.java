package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Flanger effect with feedback.
 *
 * <p>Creates a characteristic "jet plane" or "whooshing" sound by mixing
 * the dry signal with a short, modulated delay and feeding the output
 * back into the input. The feedback creates comb filtering that sweeps
 * through the frequency spectrum.</p>
 *
 * <p>Key differences from chorus:
 * - Shorter delay time (0.5-10ms vs 7-20ms)
 * - Feedback creates the metallic/jet sound
 * - Single voice with deeper modulation
 * - More pronounced frequency sweeping effect</p>
 */
public class FlangerEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "flanger",
            "Flanger",
            "Classic flanger with feedback for jet-plane sweeps",
            EffectCategory.MODULATION
    );

    private static final float MIN_DELAY_MS = 0.5f;   // Minimum delay
    private static final float MAX_DELAY_MS = 10.0f;  // Maximum delay

    // Parameters
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter delayParam;
    private final Parameter feedbackParam;
    private final Parameter mixParam;

    // DSP components
    private DelayLine delayLineL;
    private DelayLine delayLineR;
    private LFO lfoL;
    private LFO lfoR;

    // Feedback state
    private float feedbackL = 0.0f;
    private float feedbackR = 0.0f;

    public FlangerEffect() {
        super(METADATA);

        // Rate: 0.05 Hz to 5 Hz, default 0.5 Hz
        rateParam = addFloatParameter("rate", "Rate",
                "Speed of the sweep. Slow rates (0.1-0.5 Hz) for classic jet sound, faster for vibrato.",
                0.05f, 5.0f, 0.5f, "Hz");

        // Depth: 0% to 100%, default 70%
        depthParam = addFloatParameter("depth", "Depth",
                "Intensity of the sweep. Higher values create more dramatic frequency changes.",
                0.0f, 100.0f, 70.0f, "%");

        // Base delay: 0.5ms to 10ms, default 2ms
        delayParam = addFloatParameter("delay", "Delay",
                "Base delay time. Shorter delays (1-3ms) for metallic sound, longer for chorus-like.",
                MIN_DELAY_MS, MAX_DELAY_MS, 2.0f, "ms");

        // Feedback: -90% to +90%, default 50%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Amount fed back. Positive = resonant peaks, Negative = resonant notches. Higher = more intense.",
                -90.0f, 90.0f, 50.0f, "%");

        // Mix: 0% (dry) to 100% (wet), default 50%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and flanged signal.",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create delay lines (max delay + modulation depth)
        float maxDelayNeeded = MAX_DELAY_MS * 2;
        delayLineL = new DelayLine(maxDelayNeeded, sampleRate);
        delayLineR = new DelayLine(maxDelayNeeded, sampleRate);

        // Create LFOs - triangle wave for classic flanger sweep
        lfoL = new LFO(LFO.Waveform.TRIANGLE, rateParam.getValue(), sampleRate);
        lfoR = new LFO(LFO.Waveform.TRIANGLE, rateParam.getValue(), sampleRate);

        // Offset right channel LFO for stereo spread
        lfoR.setPhase(0.5f);

        // Reset feedback
        feedbackL = 0.0f;
        feedbackR = 0.0f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float baseDelay = delayParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        // Update LFO frequency
        lfoL.setFrequency(rate);

        // Calculate delay range
        float minDelaySamples = delayLineL.msToSamples(MIN_DELAY_MS);
        float baseDelaySamples = delayLineL.msToSamples(baseDelay);
        float modulationRange = baseDelaySamples - minDelaySamples;

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Get modulated delay time (LFO output is -1 to 1, we want 0 to 1)
            float lfoValue = (lfoL.tick() + 1.0f) * 0.5f;
            float delaySamples = minDelaySamples + lfoValue * modulationRange * depth
                               + baseDelaySamples * (1.0f - depth);

            // Write input + feedback to delay line
            float inputWithFeedback = dry + feedbackL * feedback;
            // Soft clip to prevent feedback runaway
            inputWithFeedback = softClip(inputWithFeedback);
            delayLineL.write(inputWithFeedback);

            // Read delayed signal with cubic interpolation
            float wet = delayLineL.readCubic(delaySamples);

            // Store for next feedback
            feedbackL = wet;

            // Mix dry and wet
            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float baseDelay = delayParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        // Update LFO frequencies
        lfoL.setFrequency(rate);
        lfoR.setFrequency(rate);

        // Calculate delay range
        float minDelaySamples = delayLineL.msToSamples(MIN_DELAY_MS);
        float baseDelaySamples = delayLineL.msToSamples(baseDelay);
        float modulationRange = baseDelaySamples - minDelaySamples;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Left channel
            float lfoValueL = (lfoL.tick() + 1.0f) * 0.5f;
            float delaySamplesL = minDelaySamples + lfoValueL * modulationRange * depth
                                + baseDelaySamples * (1.0f - depth);

            float inputWithFeedbackL = dryL + feedbackL * feedback;
            inputWithFeedbackL = softClip(inputWithFeedbackL);
            delayLineL.write(inputWithFeedbackL);

            float wetL = delayLineL.readCubic(delaySamplesL);
            feedbackL = wetL;

            // Right channel (phase-offset LFO for stereo)
            float lfoValueR = (lfoR.tick() + 1.0f) * 0.5f;
            float delaySamplesR = minDelaySamples + lfoValueR * modulationRange * depth
                                + baseDelaySamples * (1.0f - depth);

            float inputWithFeedbackR = dryR + feedbackR * feedback;
            inputWithFeedbackR = softClip(inputWithFeedbackR);
            delayLineR.write(inputWithFeedbackR);

            float wetR = delayLineR.readCubic(delaySamplesR);
            feedbackR = wetR;

            // Mix dry and wet
            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    /**
     * Soft clip to prevent feedback runaway.
     */
    private float softClip(float x) {
        if (x > 1.0f) {
            return 1.0f - 1.0f / (x + 1.0f);
        } else if (x < -1.0f) {
            return -1.0f + 1.0f / (-x + 1.0f);
        }
        return x;
    }

    @Override
    protected void onReset() {
        if (delayLineL != null) delayLineL.clear();
        if (delayLineR != null) delayLineR.clear();
        if (lfoL != null) lfoL.reset();
        if (lfoR != null) lfoR.reset();
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

    public void setDelay(float ms) {
        delayParam.setValue(ms);
    }

    public void setFeedback(float percent) {
        feedbackParam.setValue(percent);
    }

    public void setMix(float percent) {
        mixParam.setValue(percent);
    }
}
