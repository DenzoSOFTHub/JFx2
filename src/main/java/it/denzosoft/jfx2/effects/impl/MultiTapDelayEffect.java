package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Multi-Tap Delay effect.
 *
 * <p>Creates rhythmic delay patterns using multiple delay taps
 * with independent timing and level control.</p>
 *
 * <p>Features:
 * - 4 independent delay taps
 * - Preset rhythmic patterns
 * - Individual tap level control
 * - Feedback on main tap
 * - High-cut filter for analog feel</p>
 */
public class MultiTapDelayEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "multitap",
            "Multi-Tap Delay",
            "Rhythmic delay with 4 taps",
            EffectCategory.DELAY
    );

    private static final int NUM_TAPS = 4;

    private static final String[] PATTERN_NAMES = {
            "Quarter",     // 1/4, 2/4, 3/4, 4/4
            "Eighth",      // 1/8, 2/8, 3/8, 4/8
            "Dotted",      // Dotted rhythm
            "Triplet",     // Triplet feel
            "Random"       // Random-ish pattern
    };

    // Tap ratios for each pattern
    private static final float[][] TAP_RATIOS = {
            {0.25f, 0.5f, 0.75f, 1.0f},    // Quarter
            {0.125f, 0.25f, 0.375f, 0.5f}, // Eighth
            {0.375f, 0.75f, 1.125f, 1.5f}, // Dotted
            {0.333f, 0.667f, 1.0f, 1.333f},// Triplet
            {0.2f, 0.45f, 0.7f, 1.0f}      // Random
    };

    // Tap levels for each pattern
    private static final float[][] TAP_LEVELS = {
            {0.8f, 0.6f, 0.4f, 0.3f},
            {0.9f, 0.7f, 0.5f, 0.3f},
            {0.7f, 0.5f, 0.4f, 0.3f},
            {0.8f, 0.6f, 0.5f, 0.4f},
            {0.6f, 0.8f, 0.5f, 0.7f}
    };

    // Parameters
    private final Parameter timeParam;
    private final Parameter patternParam;
    private final Parameter feedbackParam;
    private final Parameter mixParam;
    private final Parameter toneParam;
    private final Parameter spreadParam;

    // DSP components
    private DelayLine delayLineL;
    private DelayLine delayLineR;
    private BiquadFilter toneFilterL;
    private BiquadFilter toneFilterR;

    // Feedback state
    private float feedbackL = 0;
    private float feedbackR = 0;

    public MultiTapDelayEffect() {
        super(METADATA);

        // Base time: 100 ms to 1500 ms, default 500 ms
        timeParam = addFloatParameter("time", "Time",
                "Base delay time. Taps are calculated relative to this.",
                100.0f, 1500.0f, 500.0f, "ms");

        // Pattern
        patternParam = addChoiceParameter("pattern", "Pattern",
                "Rhythmic pattern for the delay taps.",
                PATTERN_NAMES, 0);

        // Feedback: 0% to 80%, default 30%
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "Amount fed back from the last tap.",
                0.0f, 80.0f, 30.0f, "%");

        // Mix: 0% to 100%, default 40%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and delayed signals.",
                0.0f, 100.0f, 40.0f, "%");

        // Tone: 1000 Hz to 12000 Hz, default 6000 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "High frequency cutoff for the delays.",
                1000.0f, 12000.0f, 6000.0f, "Hz");

        // Spread: 0% to 100%, default 50%
        spreadParam = addFloatParameter("spread", "Spread",
                "Stereo spread of the taps. Higher = wider stereo field.",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Max delay for longest tap (3 seconds)
        delayLineL = new DelayLine(3000.0f, sampleRate);
        delayLineR = new DelayLine(3000.0f, sampleRate);

        toneFilterL = new BiquadFilter();
        toneFilterL.setSampleRate(sampleRate);
        toneFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        toneFilterR = new BiquadFilter();
        toneFilterR.setSampleRate(sampleRate);
        toneFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        feedbackL = 0;
        feedbackR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float baseTimeMs = timeParam.getValue();
        int pattern = (int) patternParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float toneFreq = toneParam.getValue();

        toneFilterL.setFrequency(toneFreq);

        float[] tapRatios = TAP_RATIOS[pattern];
        float[] tapLevels = TAP_LEVELS[pattern];

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Write input + feedback
            delayLineL.write(dry + feedbackL * feedback);

            // Sum all taps
            float wet = 0;
            for (int t = 0; t < NUM_TAPS; t++) {
                float tapTimeMs = baseTimeMs * tapRatios[t];
                float tapSamples = delayLineL.msToSamples(tapTimeMs);
                wet += delayLineL.readCubic(tapSamples) * tapLevels[t];
            }

            // Apply tone filter
            wet = toneFilterL.process(wet);

            // Store feedback from last tap
            float lastTapMs = baseTimeMs * tapRatios[NUM_TAPS - 1];
            feedbackL = delayLineL.readCubic(delayLineL.msToSamples(lastTapMs));

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float baseTimeMs = timeParam.getValue();
        int pattern = (int) patternParam.getValue();
        float feedback = feedbackParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float toneFreq = toneParam.getValue();
        float spread = spreadParam.getValue() / 100.0f;

        toneFilterL.setFrequency(toneFreq);
        toneFilterR.setFrequency(toneFreq);

        float[] tapRatios = TAP_RATIOS[pattern];
        float[] tapLevels = TAP_LEVELS[pattern];

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            delayLineL.write(dryL + feedbackL * feedback);
            delayLineR.write(dryR + feedbackR * feedback);

            float wetL = 0, wetR = 0;

            for (int t = 0; t < NUM_TAPS; t++) {
                float tapTimeMs = baseTimeMs * tapRatios[t];
                float tapSamplesL = delayLineL.msToSamples(tapTimeMs);
                float tapSamplesR = delayLineR.msToSamples(tapTimeMs * (1.0f + spread * 0.1f));

                float tapL = delayLineL.readCubic(tapSamplesL) * tapLevels[t];
                float tapR = delayLineR.readCubic(tapSamplesR) * tapLevels[t];

                // Alternate taps between channels for stereo spread
                float panL = 0.5f + (t % 2 == 0 ? spread * 0.5f : -spread * 0.5f);
                float panR = 1.0f - panL;

                wetL += tapL * panL + tapR * (1 - panL);
                wetR += tapR * panR + tapL * (1 - panR);
            }

            wetL = toneFilterL.process(wetL);
            wetR = toneFilterR.process(wetR);

            float lastTapMs = baseTimeMs * tapRatios[NUM_TAPS - 1];
            feedbackL = delayLineL.readCubic(delayLineL.msToSamples(lastTapMs));
            feedbackR = delayLineR.readCubic(delayLineR.msToSamples(lastTapMs * 1.1f));

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (delayLineL != null) delayLineL.clear();
        if (delayLineR != null) delayLineR.clear();
        if (toneFilterL != null) toneFilterL.reset();
        if (toneFilterR != null) toneFilterR.reset();
        feedbackL = 0;
        feedbackR = 0;
    }

    // Convenience setters
    public void setTime(float ms) { timeParam.setValue(ms); }
    public void setPattern(int pattern) { patternParam.setValue(pattern); }
    public void setFeedback(float percent) { feedbackParam.setValue(percent); }
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setTone(float hz) { toneParam.setValue(hz); }
    public void setSpread(float percent) { spreadParam.setValue(percent); }
}
