package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.dsp.LFO;
import it.denzosoft.jfx2.effects.*;

/**
 * Uni-Vibe effect.
 *
 * <p>Emulates the classic Shin-ei Uni-Vibe, a photocell-based phase/chorus
 * effect popularized by Jimi Hendrix. Creates a swirling, psychedelic
 * sound that's between a phaser and a chorus.</p>
 *
 * <p>Characteristics:
 * - 4-stage variable phase shift
 * - Asymmetric LFO sweep (photocell response)
 * - Mix between vibrato and chorus modes
 * - Warm, organic modulation</p>
 */
public class UniVibeEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "univibe",
            "Uni-Vibe",
            "Classic photocell phaser/chorus effect",
            EffectCategory.MODULATION
    );

    private static final String[] MODE_NAMES = {
            "Chorus",
            "Vibrato"
    };

    // 4 all-pass filter stages
    private static final int NUM_STAGES = 4;

    // Center frequencies for each stage (Hz) - asymmetric spacing
    private static final float[] STAGE_FREQS = {200.0f, 400.0f, 900.0f, 2000.0f};

    // Parameters
    private final Parameter speedParam;
    private final Parameter intensityParam;
    private final Parameter modeParam;
    private final Parameter volumeParam;

    // DSP components
    private LFO lfo;

    // All-pass filter states - Left
    private float[] apStateL;
    // All-pass filter states - Right
    private float[] apStateR;

    public UniVibeEffect() {
        super(METADATA);

        // Speed: 0.5 Hz to 10 Hz, default 3 Hz
        speedParam = addFloatParameter("speed", "Speed",
                "Rate of the modulation sweep.",
                0.5f, 10.0f, 3.0f, "Hz");

        // Intensity: 0% to 100%, default 70%
        intensityParam = addFloatParameter("intensity", "Intensity",
                "Depth of the effect. Higher = more pronounced sweep.",
                0.0f, 100.0f, 70.0f, "%");

        // Mode: Chorus or Vibrato
        modeParam = addChoiceParameter("mode", "Mode",
                "Chorus = mixed with dry, Vibrato = wet only for pitch wobble.",
                MODE_NAMES, 0);

        // Volume: -12 dB to +6 dB, default 0 dB
        volumeParam = addFloatParameter("volume", "Volume",
                "Output level adjustment.",
                -12.0f, 6.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        lfo = new LFO(LFO.Waveform.SINE, speedParam.getValue(), sampleRate);

        apStateL = new float[NUM_STAGES];
        apStateR = new float[NUM_STAGES];
    }

    /**
     * Process through an all-pass filter stage.
     * Uses first-order all-pass: y[n] = a * x[n] + x[n-1] - a * y[n-1]
     * Simplified to: y = a * (x - y_prev) + x_prev
     */
    private float allPassProcess(float input, int stage, float[] state, float coefficient) {
        float output = coefficient * (input - state[stage]) + state[stage];
        state[stage] = input + coefficient * output;
        return output;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float speed = speedParam.getValue();
        float intensity = intensityParam.getValue() / 100.0f;
        boolean vibratoMode = modeParam.getValue() > 0.5f;
        float volumeLin = dbToLinear(volumeParam.getValue());

        lfo.setFrequency(speed);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Get LFO value with photocell-like asymmetric curve
            float lfoRaw = lfo.tick();
            // Apply asymmetric shaping (photocell response)
            float lfoShaped = (lfoRaw + 1.0f) * 0.5f; // 0 to 1
            lfoShaped = (float) Math.pow(lfoShaped, 1.5); // Asymmetric curve
            lfoShaped = lfoShaped * 2.0f - 1.0f; // Back to -1 to 1

            // Process through all-pass stages
            float phased = sample;
            for (int stage = 0; stage < NUM_STAGES; stage++) {
                // Calculate frequency modulation
                float baseFreq = STAGE_FREQS[stage];
                float modFreq = baseFreq * (1.0f + lfoShaped * intensity);

                // Calculate all-pass coefficient from frequency
                float w0 = (float) (2.0 * Math.PI * modFreq / sampleRate);
                float tanW0 = (float) Math.tan(w0 / 2.0);
                float coefficient = (tanW0 - 1.0f) / (tanW0 + 1.0f);

                phased = allPassProcess(phased, stage, apStateL, coefficient);
            }

            // Mix output
            float wet;
            if (vibratoMode) {
                // Vibrato mode: wet only
                wet = phased;
            } else {
                // Chorus mode: mix dry and wet
                wet = (sample + phased) * 0.5f;
            }

            output[i] = wet * volumeLin;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float speed = speedParam.getValue();
        float intensity = intensityParam.getValue() / 100.0f;
        boolean vibratoMode = modeParam.getValue() > 0.5f;
        float volumeLin = dbToLinear(volumeParam.getValue());

        lfo.setFrequency(speed);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            float lfoRaw = lfo.tick();
            float lfoShaped = (lfoRaw + 1.0f) * 0.5f;
            lfoShaped = (float) Math.pow(lfoShaped, 1.5);
            lfoShaped = lfoShaped * 2.0f - 1.0f;

            float phasedL = sampleL;
            float phasedR = sampleR;

            for (int stage = 0; stage < NUM_STAGES; stage++) {
                float baseFreq = STAGE_FREQS[stage];
                float modFreq = baseFreq * (1.0f + lfoShaped * intensity);

                float w0 = (float) (2.0 * Math.PI * modFreq / sampleRate);
                float tanW0 = (float) Math.tan(w0 / 2.0);
                float coefficient = (tanW0 - 1.0f) / (tanW0 + 1.0f);

                phasedL = allPassProcess(phasedL, stage, apStateL, coefficient);
                phasedR = allPassProcess(phasedR, stage, apStateR, coefficient);
            }

            float wetL, wetR;
            if (vibratoMode) {
                wetL = phasedL;
                wetR = phasedR;
            } else {
                wetL = (sampleL + phasedL) * 0.5f;
                wetR = (sampleR + phasedR) * 0.5f;
            }

            outputL[i] = wetL * volumeLin;
            outputR[i] = wetR * volumeLin;
        }
    }

    @Override
    protected void onReset() {
        if (lfo != null) lfo.reset();
        if (apStateL != null) java.util.Arrays.fill(apStateL, 0);
        if (apStateR != null) java.util.Arrays.fill(apStateR, 0);
    }

    // Convenience setters
    public void setSpeed(float hz) {
        speedParam.setValue(hz);
    }

    public void setIntensity(float percent) {
        intensityParam.setValue(percent);
    }

    public void setMode(int mode) {
        modeParam.setValue(mode);
    }

    public void setVolume(float dB) {
        volumeParam.setValue(dB);
    }
}
