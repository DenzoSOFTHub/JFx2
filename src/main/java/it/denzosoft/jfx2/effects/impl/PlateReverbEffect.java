package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Plate Reverb effect - simulates vintage plate reverb units.
 *
 * <p>Plate reverbs use a large metal plate to create reverberations.
 * They are known for their bright, shimmery character and dense diffusion.</p>
 *
 * <p>This implementation uses:
 * <ul>
 *   <li>All-pass diffusor network for initial diffusion</li>
 *   <li>Feedback delay network for tail</li>
 *   <li>High-frequency damping for natural decay</li>
 *   <li>Modulation for shimmer effect</li>
 * </ul>
 * </p>
 */
public class PlateReverbEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "platereverb",
            "Plate Reverb",
            "Classic plate reverb with bright, shimmery character",
            EffectCategory.REVERB
    );

    // Parameters
    private final Parameter mixParam;
    private final Parameter decayParam;
    private final Parameter predelayParam;
    private final Parameter dampingParam;
    private final Parameter sizeParam;
    private final Parameter brightnessParam;
    private final Parameter modulationParam;
    private final Parameter widthParam;

    // Predelay buffer
    private float[] predelayBufferL;
    private float[] predelayBufferR;
    private int predelayWritePos;
    private static final int MAX_PREDELAY_SAMPLES = 4800; // 100ms @ 48kHz

    // All-pass diffusors (4 stages)
    private static final int NUM_ALLPASS = 4;
    private float[][] allpassBufferL;
    private float[][] allpassBufferR;
    private int[] allpassWritePos;
    private static final int[] ALLPASS_TIMES = {142, 107, 379, 277}; // Prime-ish numbers in samples
    private static final float ALLPASS_GAIN = 0.5f;

    // Feedback delay network (4 delays)
    private static final int NUM_DELAYS = 4;
    private float[][] delayBufferL;
    private float[][] delayBufferR;
    private int[] delayWritePos;
    // Delay times based on plate simulation (in samples @ 48kHz)
    private static final int[] BASE_DELAY_TIMES = {1557, 1617, 1491, 1422};

    // Damping filters (one-pole lowpass per delay)
    private float[] dampStateL;
    private float[] dampStateR;

    // Modulation LFOs
    private double[] lfoPhase;
    private static final double[] LFO_RATES = {0.5, 0.7, 0.6, 0.8}; // Hz
    private static final int[] MOD_DEPTH = {12, 14, 11, 13}; // samples

    // Tank feedback
    private float tankFeedback;

    public PlateReverbEffect() {
        super(METADATA);
        setStereoMode(StereoMode.STEREO);

        mixParam = addFloatParameter("mix", "Mix",
                "Dry/wet balance",
                0.0f, 100.0f, 35.0f, "%");

        decayParam = addFloatParameter("decay", "Decay",
                "Reverb tail length",
                0.1f, 10.0f, 2.5f, "s");

        predelayParam = addFloatParameter("predelay", "Pre-Delay",
                "Initial delay before reverb",
                0.0f, 100.0f, 10.0f, "ms");

        dampingParam = addFloatParameter("damping", "Damping",
                "High frequency absorption (darker sound)",
                0.0f, 100.0f, 30.0f, "%");

        sizeParam = addFloatParameter("size", "Size",
                "Simulated plate size",
                20.0f, 150.0f, 100.0f, "%");

        brightnessParam = addFloatParameter("bright", "Brightness",
                "High frequency content",
                0.0f, 100.0f, 70.0f, "%");

        modulationParam = addFloatParameter("mod", "Modulation",
                "Pitch modulation for shimmer",
                0.0f, 100.0f, 20.0f, "%");

        widthParam = addFloatParameter("width", "Width",
                "Stereo width",
                0.0f, 150.0f, 100.0f, "%");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Scale all delay times by sample rate
        float sampleRateRatio = sampleRate / 48000.0f;

        // Predelay buffers
        predelayBufferL = new float[MAX_PREDELAY_SAMPLES];
        predelayBufferR = new float[MAX_PREDELAY_SAMPLES];
        predelayWritePos = 0;

        // All-pass diffusors
        allpassBufferL = new float[NUM_ALLPASS][];
        allpassBufferR = new float[NUM_ALLPASS][];
        allpassWritePos = new int[NUM_ALLPASS];

        for (int i = 0; i < NUM_ALLPASS; i++) {
            int size = (int) (ALLPASS_TIMES[i] * sampleRateRatio * 2); // Extra room for size param
            allpassBufferL[i] = new float[size];
            allpassBufferR[i] = new float[size];
            allpassWritePos[i] = 0;
        }

        // Feedback delay network
        delayBufferL = new float[NUM_DELAYS][];
        delayBufferR = new float[NUM_DELAYS][];
        delayWritePos = new int[NUM_DELAYS];

        for (int i = 0; i < NUM_DELAYS; i++) {
            int size = (int) (BASE_DELAY_TIMES[i] * sampleRateRatio * 2); // Extra room
            delayBufferL[i] = new float[size];
            delayBufferR[i] = new float[size];
            delayWritePos[i] = 0;
        }

        // Damping filter state
        dampStateL = new float[NUM_DELAYS];
        dampStateR = new float[NUM_DELAYS];

        // LFO phases
        lfoPhase = new double[NUM_DELAYS];
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Mono to stereo
        float[] tempR = new float[frameCount];
        System.arraycopy(input, 0, tempR, 0, frameCount);
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];

        processInternal(input, tempR, outL, outR, frameCount);

        // Mix to mono
        for (int i = 0; i < frameCount; i++) {
            output[i] = (outL[i] + outR[i]) * 0.5f;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR,
                                   float[] outputL, float[] outputR, int frameCount) {
        processInternal(inputL, inputR, outputL, outputR, frameCount);
    }

    private void processInternal(float[] inputL, float[] inputR,
                                  float[] outputL, float[] outputR, int frameCount) {
        float mix = mixParam.getValue() / 100.0f;
        float dry = 1.0f - mix;
        float wet = mix;

        float decay = decayParam.getValue();
        float predelayMs = predelayParam.getValue();
        float damping = dampingParam.getValue() / 100.0f;
        float size = sizeParam.getValue() / 100.0f;
        float brightness = brightnessParam.getValue() / 100.0f;
        float modulation = modulationParam.getValue() / 100.0f;
        float width = widthParam.getValue() / 100.0f;

        // Calculate predelay in samples
        int predelaySamples = (int) (predelayMs * sampleRate / 1000.0f);
        predelaySamples = Math.min(predelaySamples, MAX_PREDELAY_SAMPLES - 1);

        // Calculate damping coefficient (higher = more damping = darker)
        float dampCoef = 0.3f + 0.6f * damping;

        // Calculate tank feedback based on decay time
        // Approximate formula: feedback = exp(-3 * avgDelayTime / decayTime / sampleRate)
        float avgDelayTime = 0;
        for (int d : BASE_DELAY_TIMES) avgDelayTime += d;
        avgDelayTime /= NUM_DELAYS;
        tankFeedback = (float) Math.exp(-3.0 * avgDelayTime / (decay * sampleRate));
        tankFeedback = Math.min(0.98f, tankFeedback);

        // Brightness affects input filter
        float inputLpCoef = 0.1f + 0.8f * brightness;

        for (int i = 0; i < frameCount; i++) {
            float inL = inputL[i];
            float inR = inputR[i];

            // === PREDELAY ===
            predelayBufferL[predelayWritePos] = inL;
            predelayBufferR[predelayWritePos] = inR;

            int predelayReadPos = (predelayWritePos - predelaySamples + MAX_PREDELAY_SAMPLES) % MAX_PREDELAY_SAMPLES;
            float delayedL = predelayBufferL[predelayReadPos];
            float delayedR = predelayBufferR[predelayReadPos];

            predelayWritePos = (predelayWritePos + 1) % MAX_PREDELAY_SAMPLES;

            // === INPUT DIFFUSION (All-pass cascade) ===
            float diffL = delayedL;
            float diffR = delayedR;

            for (int ap = 0; ap < NUM_ALLPASS; ap++) {
                int apTime = (int) (ALLPASS_TIMES[ap] * size);
                apTime = Math.min(apTime, allpassBufferL[ap].length - 1);

                int readPos = (allpassWritePos[ap] - apTime + allpassBufferL[ap].length) % allpassBufferL[ap].length;

                float apOutL = allpassBufferL[ap][readPos];
                float apOutR = allpassBufferR[ap][readPos];

                float apInL = diffL + ALLPASS_GAIN * apOutL;
                float apInR = diffR + ALLPASS_GAIN * apOutR;

                allpassBufferL[ap][allpassWritePos[ap]] = apInL;
                allpassBufferR[ap][allpassWritePos[ap]] = apInR;

                diffL = apOutL - ALLPASS_GAIN * apInL;
                diffR = apOutR - ALLPASS_GAIN * apInR;

                allpassWritePos[ap] = (allpassWritePos[ap] + 1) % allpassBufferL[ap].length;
            }

            // === FEEDBACK DELAY NETWORK (Tank) ===
            float tankOutL = 0;
            float tankOutR = 0;

            for (int d = 0; d < NUM_DELAYS; d++) {
                // Calculate modulated delay time
                int baseTime = (int) (BASE_DELAY_TIMES[d] * size);
                int modOffset = (int) (MOD_DEPTH[d] * modulation * Math.sin(2 * Math.PI * lfoPhase[d]));
                int delayTime = baseTime + modOffset;
                delayTime = Math.max(1, Math.min(delayTime, delayBufferL[d].length - 1));

                // Read from delay
                int readPos = (delayWritePos[d] - delayTime + delayBufferL[d].length) % delayBufferL[d].length;
                float delOutL = delayBufferL[d][readPos];
                float delOutR = delayBufferR[d][readPos];

                // Apply damping (one-pole lowpass)
                dampStateL[d] = dampStateL[d] + dampCoef * (delOutL - dampStateL[d]);
                dampStateR[d] = dampStateR[d] + dampCoef * (delOutR - dampStateR[d]);

                // Mix damped and original based on brightness
                delOutL = delOutL * brightness + dampStateL[d] * (1 - brightness);
                delOutR = delOutR * brightness + dampStateR[d] * (1 - brightness);

                // Accumulate outputs with Hadamard-like mixing
                if (d % 2 == 0) {
                    tankOutL += delOutL;
                    tankOutR += delOutR;
                } else {
                    tankOutL -= delOutL;
                    tankOutR -= delOutR;
                }

                // Update LFO
                lfoPhase[d] += LFO_RATES[d] / sampleRate;
                if (lfoPhase[d] >= 1.0) lfoPhase[d] -= 1.0;
            }

            // Normalize tank output
            tankOutL *= 0.5f;
            tankOutR *= 0.5f;

            // Write to delays with feedback and input
            for (int d = 0; d < NUM_DELAYS; d++) {
                // Cross-couple for more diffusion
                float fbL = tankFeedback * (d < 2 ? tankOutL : tankOutR);
                float fbR = tankFeedback * (d < 2 ? tankOutR : tankOutL);

                // Add diffused input
                float inputMix = (d == 0 || d == 2) ? diffL : diffR;

                delayBufferL[d][delayWritePos[d]] = inputMix + fbL;
                delayBufferR[d][delayWritePos[d]] = inputMix + fbR;

                delayWritePos[d] = (delayWritePos[d] + 1) % delayBufferL[d].length;
            }

            // === OUTPUT MIX ===
            // Apply width
            float mid = (tankOutL + tankOutR) * 0.5f;
            float side = (tankOutL - tankOutR) * 0.5f * width;
            float wetL = mid + side;
            float wetR = mid - side;

            // Final mix
            outputL[i] = inL * dry + wetL * wet;
            outputR[i] = inR * dry + wetR * wet;
        }
    }

    @Override
    protected void onReset() {
        // Clear all buffers
        if (predelayBufferL != null) {
            java.util.Arrays.fill(predelayBufferL, 0);
            java.util.Arrays.fill(predelayBufferR, 0);
        }
        predelayWritePos = 0;

        for (int i = 0; i < NUM_ALLPASS; i++) {
            if (allpassBufferL[i] != null) {
                java.util.Arrays.fill(allpassBufferL[i], 0);
                java.util.Arrays.fill(allpassBufferR[i], 0);
            }
            allpassWritePos[i] = 0;
        }

        for (int i = 0; i < NUM_DELAYS; i++) {
            if (delayBufferL[i] != null) {
                java.util.Arrays.fill(delayBufferL[i], 0);
                java.util.Arrays.fill(delayBufferR[i], 0);
            }
            delayWritePos[i] = 0;
            dampStateL[i] = 0;
            dampStateR[i] = 0;
            lfoPhase[i] = 0;
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1: Mix, Decay, Pre-Delay, Size
        // Row 2: Damping, Brightness, Modulation, Width
        return new int[] {4, 4};
    }
}
