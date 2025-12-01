package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Chorus effect with 4 voices.
 *
 * <p>Creates a rich, shimmering sound by mixing the dry signal with
 * multiple delayed copies modulated by LFOs at different phases.</p>
 */
public class ChorusEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "chorus",
            "Chorus",
            "4-voice chorus with rich stereo spread",
            EffectCategory.MODULATION
    );

    private static final int NUM_VOICES = 4;
    private static final float BASE_DELAY_MS = 7.0f;  // Base delay time
    private static final float MAX_DEPTH_MS = 5.0f;   // Max modulation depth

    // Parameters
    private final Parameter rateParam;
    private final Parameter depthParam;
    private final Parameter mixParam;
    private final Parameter spreadParam;

    // DSP components - Left channel
    private DelayLine[] delayLinesL;
    private LFO[] lfosL;

    // DSP components - Right channel
    private DelayLine[] delayLinesR;
    private LFO[] lfosR;

    public ChorusEffect() {
        super(METADATA);

        // Rate: 0.1 Hz to 5 Hz, default 0.8 Hz
        rateParam = addFloatParameter("rate", "Rate",
                "Speed of the modulation. Slower rates are subtle and lush, faster creates vibrato effect.",
                0.1f, 5.0f, 0.8f, "Hz");

        // Depth: 0% to 100%, default 50%
        depthParam = addFloatParameter("depth", "Depth",
                "Amount of pitch modulation. Higher values create more pronounced swirling effect.",
                0.0f, 100.0f, 50.0f, "%");

        // Mix: 0% (dry) to 100% (wet), default 50%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and chorused signal. 50% gives classic ensemble sound.",
                0.0f, 100.0f, 50.0f, "%");

        // Stereo spread: 0% (mono) to 100% (full spread), default 80%
        spreadParam = addFloatParameter("spread", "Spread",
                "Stereo width of the chorus voices. Higher values create wider, more immersive sound.",
                0.0f, 100.0f, 80.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Create delay lines (max ~20ms for chorus) - Left channel
        delayLinesL = new DelayLine[NUM_VOICES];
        for (int i = 0; i < NUM_VOICES; i++) {
            delayLinesL[i] = new DelayLine(20.0f, sampleRate);
        }

        // Create delay lines - Right channel
        delayLinesR = new DelayLine[NUM_VOICES];
        for (int i = 0; i < NUM_VOICES; i++) {
            delayLinesR[i] = new DelayLine(20.0f, sampleRate);
        }

        // Create LFOs with spread phase - Left channel
        lfosL = LFO.createSpread(NUM_VOICES, LFO.Waveform.SINE, rateParam.getValue(), sampleRate);

        // Create LFOs with spread phase - Right channel (offset phase for stereo)
        lfosR = LFO.createSpread(NUM_VOICES, LFO.Waveform.SINE, rateParam.getValue(), sampleRate);
        // Offset right channel LFOs for stereo spread
        for (int i = 0; i < NUM_VOICES; i++) {
            lfosR[i].setPhase((float) (i + 0.5f) / NUM_VOICES);
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;

        // Update LFO frequencies
        for (LFO lfo : lfosL) {
            lfo.setFrequency(rate);
        }

        // Depth in samples
        float depthSamples = delayLinesL[0].msToSamples(MAX_DEPTH_MS * depth);
        float baseSamples = delayLinesL[0].msToSamples(BASE_DELAY_MS);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Sum of all chorus voices
            float wet = 0.0f;

            for (int v = 0; v < NUM_VOICES; v++) {
                // Get modulated delay time
                float lfoValue = lfosL[v].tick();
                float delaySamples = baseSamples + lfoValue * depthSamples;

                // Write to delay line
                delayLinesL[v].write(dry);

                // Read with cubic interpolation for smooth modulation
                float delayed = delayLinesL[v].readCubic(delaySamples);
                wet += delayed;
            }

            // Average the voices
            wet /= NUM_VOICES;

            // Mix dry and wet
            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float rate = rateParam.getValue();
        float depth = depthParam.getValue() / 100.0f;
        float mix = mixParam.getValue() / 100.0f;
        float spread = spreadParam.getValue() / 100.0f;

        // Update LFO frequencies
        for (int i = 0; i < NUM_VOICES; i++) {
            lfosL[i].setFrequency(rate);
            lfosR[i].setFrequency(rate);
        }

        // Depth in samples
        float depthSamples = delayLinesL[0].msToSamples(MAX_DEPTH_MS * depth);
        float baseSamples = delayLinesL[0].msToSamples(BASE_DELAY_MS);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Sum of all chorus voices for each channel
            float wetL = 0.0f;
            float wetR = 0.0f;

            for (int v = 0; v < NUM_VOICES; v++) {
                // Left channel
                float lfoValueL = lfosL[v].tick();
                float delaySamplesL = baseSamples + lfoValueL * depthSamples;
                delayLinesL[v].write(dryL);
                float delayedL = delayLinesL[v].readCubic(delaySamplesL);
                wetL += delayedL;

                // Right channel
                float lfoValueR = lfosR[v].tick();
                float delaySamplesR = baseSamples + lfoValueR * depthSamples;
                delayLinesR[v].write(dryR);
                float delayedR = delayLinesR[v].readCubic(delaySamplesR);
                wetR += delayedR;
            }

            // Average the voices
            wetL /= NUM_VOICES;
            wetR /= NUM_VOICES;

            // Apply stereo spread (cross-mix wet signals)
            float wetLFinal = wetL * (1.0f - spread * 0.3f) + wetR * spread * 0.3f;
            float wetRFinal = wetR * (1.0f - spread * 0.3f) + wetL * spread * 0.3f;

            // Mix dry and wet
            outputL[i] = dryL * (1.0f - mix) + wetLFinal * mix;
            outputR[i] = dryR * (1.0f - mix) + wetRFinal * mix;
        }
    }

    @Override
    protected void onReset() {
        if (delayLinesL != null) {
            for (DelayLine dl : delayLinesL) {
                dl.clear();
            }
        }
        if (delayLinesR != null) {
            for (DelayLine dl : delayLinesR) {
                dl.clear();
            }
        }
        if (lfosL != null) {
            for (LFO lfo : lfosL) {
                lfo.reset();
            }
        }
        if (lfosR != null) {
            for (LFO lfo : lfosR) {
                lfo.reset();
            }
        }
    }

    // Convenience setters
    public void setRate(float hz) {
        rateParam.setValue(hz);
    }

    public void setDepth(float percent) {
        depthParam.setValue(percent);
    }

    public void setMix(float percent) {
        mixParam.setValue(percent);
    }

    public void setSpread(float percent) {
        spreadParam.setValue(percent);
    }
}
