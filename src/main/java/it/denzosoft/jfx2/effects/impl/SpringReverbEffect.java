package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.DelayLine;
import it.denzosoft.jfx2.dsp.BiquadFilter;
import it.denzosoft.jfx2.dsp.FilterType;
import it.denzosoft.jfx2.effects.*;

/**
 * Spring Reverb effect.
 *
 * <p>Emulates the distinctive sound of spring reverb tanks found in
 * guitar amplifiers. Features the characteristic "drip" and "boing"
 * sounds of physical springs.</p>
 *
 * <p>Characteristics:
 * - Metallic, "drip" sound on attacks
 * - Multiple spring simulation
 * - Characteristic frequency response (mid-focused)
 * - Feedback for extended decay</p>
 */
public class SpringReverbEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "springreverb",
            "Spring Reverb",
            "Classic amp-style spring reverb",
            EffectCategory.REVERB
    );

    // Number of parallel springs to simulate
    private static final int NUM_SPRINGS = 3;

    // Parameters
    private final Parameter mixParam;
    private final Parameter decayParam;
    private final Parameter toneParam;
    private final Parameter dripParam;
    private final Parameter tensionParam;

    // Delay lines for each spring (different lengths create comb filtering)
    private DelayLine[] springDelaysL;
    private DelayLine[] springDelaysR;

    // All-pass diffusers for metallic quality
    private float[][] apStateL;
    private float[][] apStateR;

    // Filters
    private BiquadFilter inputFilterL, inputFilterR;
    private BiquadFilter outputFilterL, outputFilterR;
    private BiquadFilter lowCutL, lowCutR;

    // Spring delay times (in ms) - slightly different for each spring
    private static final float[] SPRING_TIMES = {35.0f, 41.0f, 47.0f};

    // Feedback state
    private float[] feedbackL;
    private float[] feedbackR;

    public SpringReverbEffect() {
        super(METADATA);

        // Mix: 0% to 100%, default 30%
        mixParam = addFloatParameter("mix", "Mix",
                "Balance between dry and reverb signal.",
                0.0f, 100.0f, 30.0f, "%");

        // Decay: 0.5s to 4s, default 2s
        decayParam = addFloatParameter("decay", "Decay",
                "Length of the reverb tail.",
                0.5f, 4.0f, 2.0f, "s");

        // Tone: 500 Hz to 5000 Hz, default 2000 Hz
        toneParam = addFloatParameter("tone", "Tone",
                "High frequency content. Lower = darker, more vintage.",
                500.0f, 5000.0f, 2000.0f, "Hz");

        // Drip: 0% to 100%, default 50%
        dripParam = addFloatParameter("drip", "Drip",
                "Amount of the characteristic spring 'drip' sound on attacks.",
                0.0f, 100.0f, 50.0f, "%");

        // Tension: 0% to 100%, default 50%
        tensionParam = addFloatParameter("tension", "Tension",
                "Spring tension. Higher = tighter, faster response.",
                0.0f, 100.0f, 50.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Initialize spring delay lines
        springDelaysL = new DelayLine[NUM_SPRINGS];
        springDelaysR = new DelayLine[NUM_SPRINGS];
        feedbackL = new float[NUM_SPRINGS];
        feedbackR = new float[NUM_SPRINGS];

        for (int i = 0; i < NUM_SPRINGS; i++) {
            springDelaysL[i] = new DelayLine(100.0f, sampleRate);
            springDelaysR[i] = new DelayLine(100.0f, sampleRate);
            feedbackL[i] = 0;
            feedbackR[i] = 0;
        }

        // All-pass states for metallic quality
        apStateL = new float[NUM_SPRINGS][2];
        apStateR = new float[NUM_SPRINGS][2];

        // Input bandpass (springs have limited frequency response)
        inputFilterL = new BiquadFilter();
        inputFilterL.setSampleRate(sampleRate);
        inputFilterL.configure(FilterType.PEAK, 1500.0f, 0.7f, 6.0f);

        inputFilterR = new BiquadFilter();
        inputFilterR.setSampleRate(sampleRate);
        inputFilterR.configure(FilterType.PEAK, 1500.0f, 0.7f, 6.0f);

        // Output lowpass
        outputFilterL = new BiquadFilter();
        outputFilterL.setSampleRate(sampleRate);
        outputFilterL.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        outputFilterR = new BiquadFilter();
        outputFilterR.setSampleRate(sampleRate);
        outputFilterR.configure(FilterType.LOWPASS, toneParam.getValue(), 0.707f, 0.0f);

        // Low cut to prevent rumble
        lowCutL = new BiquadFilter();
        lowCutL.setSampleRate(sampleRate);
        lowCutL.configure(FilterType.HIGHPASS, 120.0f, 0.707f, 0.0f);

        lowCutR = new BiquadFilter();
        lowCutR.setSampleRate(sampleRate);
        lowCutR.configure(FilterType.HIGHPASS, 120.0f, 0.707f, 0.0f);
    }

    /**
     * Simple all-pass filter for metallic texture.
     */
    private float allPass(float input, float[] state, float coeff) {
        float output = -coeff * input + state[0] + coeff * state[1];
        state[0] = input;
        state[1] = output;
        return output;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float mix = mixParam.getValue() / 100.0f;
        float decayTime = decayParam.getValue();
        float toneFreq = toneParam.getValue();
        float drip = dripParam.getValue() / 100.0f;
        float tension = tensionParam.getValue() / 100.0f;

        outputFilterL.setFrequency(toneFreq);

        // Calculate feedback based on decay time
        float feedbackAmount = (float) Math.pow(0.001, 1.0 / (decayTime * sampleRate / SPRING_TIMES[0]));
        feedbackAmount = Math.min(0.95f, feedbackAmount);

        // Tension affects delay times
        float tensionMod = 0.7f + tension * 0.6f;

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float dry = input[i];

            // Input filtering (spring transducer response)
            float filtered = inputFilterL.process(dry);

            // Process through all springs
            float wetSum = 0;
            for (int s = 0; s < NUM_SPRINGS; s++) {
                float springTime = SPRING_TIMES[s] * tensionMod;
                float delaySamples = springDelaysL[s].msToSamples(springTime);

                // Write input + feedback
                float springIn = filtered * (0.5f + drip * 0.5f) + feedbackL[s] * feedbackAmount;
                springDelaysL[s].write(springIn);

                // Read delayed
                float delayed = springDelaysL[s].readCubic(delaySamples);

                // Add metallic quality with all-pass
                delayed = allPass(delayed, apStateL[s], 0.6f);

                feedbackL[s] = delayed;
                wetSum += delayed;
            }

            // Average and filter
            float wet = wetSum / NUM_SPRINGS;
            wet = outputFilterL.process(wet);
            wet = lowCutL.process(wet);

            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float mix = mixParam.getValue() / 100.0f;
        float decayTime = decayParam.getValue();
        float toneFreq = toneParam.getValue();
        float drip = dripParam.getValue() / 100.0f;
        float tension = tensionParam.getValue() / 100.0f;

        outputFilterL.setFrequency(toneFreq);
        outputFilterR.setFrequency(toneFreq);

        float feedbackAmount = (float) Math.pow(0.001, 1.0 / (decayTime * sampleRate / SPRING_TIMES[0]));
        feedbackAmount = Math.min(0.95f, feedbackAmount);

        float tensionMod = 0.7f + tension * 0.6f;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            float filteredL = this.inputFilterL.process(dryL);
            float filteredR = this.inputFilterR.process(dryR);

            float wetSumL = 0, wetSumR = 0;

            for (int s = 0; s < NUM_SPRINGS; s++) {
                float springTime = SPRING_TIMES[s] * tensionMod;
                // Slight stereo offset
                float springTimeR = springTime * (1.0f + (s - 1) * 0.02f);

                float delaySamplesL = springDelaysL[s].msToSamples(springTime);
                float delaySamplesR = springDelaysR[s].msToSamples(springTimeR);

                springDelaysL[s].write(filteredL * (0.5f + drip * 0.5f) + feedbackL[s] * feedbackAmount);
                springDelaysR[s].write(filteredR * (0.5f + drip * 0.5f) + feedbackR[s] * feedbackAmount);

                float delayedL = springDelaysL[s].readCubic(delaySamplesL);
                float delayedR = springDelaysR[s].readCubic(delaySamplesR);

                delayedL = allPass(delayedL, apStateL[s], 0.6f);
                delayedR = allPass(delayedR, apStateR[s], 0.6f);

                feedbackL[s] = delayedL;
                feedbackR[s] = delayedR;

                wetSumL += delayedL;
                wetSumR += delayedR;
            }

            float wetL = wetSumL / NUM_SPRINGS;
            float wetR = wetSumR / NUM_SPRINGS;

            wetL = outputFilterL.process(wetL);
            wetR = outputFilterR.process(wetR);
            wetL = lowCutL.process(wetL);
            wetR = lowCutR.process(wetR);

            outputL[i] = dryL * (1.0f - mix) + wetL * mix;
            outputR[i] = dryR * (1.0f - mix) + wetR * mix;
        }
    }

    @Override
    protected void onReset() {
        if (springDelaysL != null) {
            for (DelayLine dl : springDelaysL) if (dl != null) dl.clear();
        }
        if (springDelaysR != null) {
            for (DelayLine dl : springDelaysR) if (dl != null) dl.clear();
        }
        if (feedbackL != null) java.util.Arrays.fill(feedbackL, 0);
        if (feedbackR != null) java.util.Arrays.fill(feedbackR, 0);
        if (apStateL != null) for (float[] a : apStateL) java.util.Arrays.fill(a, 0);
        if (apStateR != null) for (float[] a : apStateR) java.util.Arrays.fill(a, 0);
        if (inputFilterL != null) inputFilterL.reset();
        if (inputFilterR != null) inputFilterR.reset();
        if (outputFilterL != null) outputFilterL.reset();
        if (outputFilterR != null) outputFilterR.reset();
        if (lowCutL != null) lowCutL.reset();
        if (lowCutR != null) lowCutR.reset();
    }

    // Convenience setters
    public void setMix(float percent) { mixParam.setValue(percent); }
    public void setDecay(float seconds) { decayParam.setValue(seconds); }
    public void setTone(float hz) { toneParam.setValue(hz); }
    public void setDrip(float percent) { dripParam.setValue(percent); }
    public void setTension(float percent) { tensionParam.setValue(percent); }
}
