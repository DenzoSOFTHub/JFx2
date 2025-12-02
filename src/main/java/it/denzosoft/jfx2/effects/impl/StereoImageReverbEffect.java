package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Stereo Image Reverb - creates spacious stereo imaging through reverb.
 *
 * <p>This reverb focuses on creating a wide, immersive stereo field using:
 * <ul>
 *   <li>Haas effect delays for spatial positioning</li>
 *   <li>Decorrelated early reflections panned across stereo field</li>
 *   <li>Independent L/R late reverb with cross-feed</li>
 *   <li>Mid/Side processing for width control</li>
 *   <li>Modulated delays for movement and depth</li>
 * </ul>
 * </p>
 */
public class StereoImageReverbEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "stereoimagereverb",
            "Stereo Image Reverb",
            "Reverb with advanced stereo imaging and spatial positioning",
            EffectCategory.REVERB
    );

    // Parameters
    private final Parameter mixParam;
    private final Parameter decayParam;
    private final Parameter predelayParam;
    private final Parameter widthParam;
    private final Parameter depthParam;
    private final Parameter spreadParam;
    private final Parameter crossfeedParam;
    private final Parameter haasDelayParam;
    private final Parameter modulationParam;
    private final Parameter dampingParam;
    private final Parameter diffusionParam;
    private final Parameter balanceParam;

    // Haas effect delays (for stereo positioning)
    private float[] haasBufferL;
    private float[] haasBufferR;
    private int haasWritePos;
    private static final int MAX_HAAS_SAMPLES = 2400; // 50ms @ 48kHz

    // Predelay buffers
    private float[] predelayBufferL;
    private float[] predelayBufferR;
    private int predelayWritePos;
    private static final int MAX_PREDELAY_SAMPLES = 4800; // 100ms @ 48kHz

    // Early reflections (8 taps per channel, decorrelated)
    private static final int NUM_EARLY_TAPS = 8;
    private float[][] earlyBufferL;
    private float[][] earlyBufferR;
    private int[] earlyWritePos;
    // Delay times in ms (different for L/R to decorrelate)
    private static final float[] EARLY_TIMES_L = {7.3f, 13.7f, 21.2f, 29.8f, 41.5f, 53.1f, 67.9f, 79.4f};
    private static final float[] EARLY_TIMES_R = {9.1f, 15.9f, 24.6f, 33.2f, 44.7f, 58.3f, 71.2f, 83.6f};
    // Pan positions for early reflections (-1 to +1)
    private static final float[] EARLY_PAN_L = {-0.9f, 0.3f, -0.6f, 0.7f, -0.4f, 0.8f, -0.2f, 0.5f};
    private static final float[] EARLY_PAN_R = {0.9f, -0.3f, 0.6f, -0.7f, 0.4f, -0.8f, 0.2f, -0.5f};
    private static final float[] EARLY_GAINS = {0.85f, 0.75f, 0.65f, 0.55f, 0.45f, 0.38f, 0.30f, 0.22f};

    // Late reverb FDN (4 delays per channel)
    private static final int NUM_LATE_DELAYS = 4;
    private float[][] lateBufferL;
    private float[][] lateBufferR;
    private int[] lateWritePosL;
    private int[] lateWritePosR;
    // Different delay times for L/R (in samples @ 48kHz)
    private static final int[] LATE_TIMES_L = {1427, 1637, 1823, 2011};
    private static final int[] LATE_TIMES_R = {1531, 1709, 1907, 2099};

    // All-pass diffusors (2 per channel)
    private static final int NUM_ALLPASS = 2;
    private float[][] allpassBufferL;
    private float[][] allpassBufferR;
    private int[] allpassWritePosL;
    private int[] allpassWritePosR;
    private static final int[] ALLPASS_TIMES_L = {113, 199};
    private static final int[] ALLPASS_TIMES_R = {127, 211};
    private static final float ALLPASS_GAIN = 0.6f;

    // Damping state
    private float[] dampStateL;
    private float[] dampStateR;

    // Modulation LFOs
    private double[] lfoPhaseL;
    private double[] lfoPhaseR;
    private static final double[] LFO_RATES = {0.37, 0.53, 0.71, 0.89};
    private static final int[] MOD_DEPTH = {8, 10, 12, 14};

    // Cross-feed state
    private float crossFeedL;
    private float crossFeedR;

    public StereoImageReverbEffect() {
        super(METADATA);
        setStereoMode(StereoMode.STEREO);

        // === Row 1: REVERB parameters ===
        mixParam = addFloatParameter("mix", "Mix",
                "Dry/wet balance",
                0.0f, 100.0f, 35.0f, "%");

        decayParam = addFloatParameter("decay", "Decay",
                "Reverb tail length",
                0.2f, 15.0f, 3.0f, "s");

        predelayParam = addFloatParameter("predelay", "Pre-Delay",
                "Initial delay before reverb",
                0.0f, 100.0f, 15.0f, "ms");

        dampingParam = addFloatParameter("damping", "Damping",
                "High frequency absorption",
                0.0f, 100.0f, 40.0f, "%");

        diffusionParam = addFloatParameter("diffusion", "Diffusion",
                "Reverb density and smoothness",
                0.0f, 100.0f, 70.0f, "%");

        modulationParam = addFloatParameter("mod", "Modulation",
                "Pitch modulation for movement",
                0.0f, 100.0f, 25.0f, "%");

        // === Row 2: STEREO imaging parameters ===
        widthParam = addFloatParameter("width", "Width",
                "Overall stereo width (0=mono, 100=normal, 200=extra wide)",
                0.0f, 200.0f, 120.0f, "%");

        depthParam = addFloatParameter("depth", "Depth",
                "Front-to-back depth perception",
                0.0f, 100.0f, 60.0f, "%");

        spreadParam = addFloatParameter("spread", "Spread",
                "Early reflection spread across stereo field",
                0.0f, 100.0f, 80.0f, "%");

        haasDelayParam = addFloatParameter("haas", "Haas",
                "Stereo positioning delay (Haas effect)",
                0.0f, 35.0f, 0.0f, "ms");

        crossfeedParam = addFloatParameter("crossfeed", "X-Feed",
                "Channel cross-mixing for cohesion",
                0.0f, 50.0f, 15.0f, "%");

        balanceParam = addFloatParameter("balance", "Balance",
                "Left/right balance (-100=L, +100=R)",
                -100.0f, 100.0f, 0.0f, "");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        float sampleRateRatio = sampleRate / 48000.0f;

        // Haas effect buffers
        haasBufferL = new float[MAX_HAAS_SAMPLES];
        haasBufferR = new float[MAX_HAAS_SAMPLES];
        haasWritePos = 0;

        // Predelay buffers
        predelayBufferL = new float[MAX_PREDELAY_SAMPLES];
        predelayBufferR = new float[MAX_PREDELAY_SAMPLES];
        predelayWritePos = 0;

        // Early reflection buffers
        int maxEarlySize = (int) (100.0f * sampleRate / 1000.0f); // 100ms max
        earlyBufferL = new float[NUM_EARLY_TAPS][];
        earlyBufferR = new float[NUM_EARLY_TAPS][];
        earlyWritePos = new int[NUM_EARLY_TAPS];

        for (int i = 0; i < NUM_EARLY_TAPS; i++) {
            earlyBufferL[i] = new float[maxEarlySize];
            earlyBufferR[i] = new float[maxEarlySize];
            earlyWritePos[i] = 0;
        }

        // Late reverb FDN
        lateBufferL = new float[NUM_LATE_DELAYS][];
        lateBufferR = new float[NUM_LATE_DELAYS][];
        lateWritePosL = new int[NUM_LATE_DELAYS];
        lateWritePosR = new int[NUM_LATE_DELAYS];

        for (int i = 0; i < NUM_LATE_DELAYS; i++) {
            int sizeL = (int) (LATE_TIMES_L[i] * sampleRateRatio * 1.5f);
            int sizeR = (int) (LATE_TIMES_R[i] * sampleRateRatio * 1.5f);
            lateBufferL[i] = new float[sizeL];
            lateBufferR[i] = new float[sizeR];
            lateWritePosL[i] = 0;
            lateWritePosR[i] = 0;
        }

        // All-pass diffusors
        allpassBufferL = new float[NUM_ALLPASS][];
        allpassBufferR = new float[NUM_ALLPASS][];
        allpassWritePosL = new int[NUM_ALLPASS];
        allpassWritePosR = new int[NUM_ALLPASS];

        for (int i = 0; i < NUM_ALLPASS; i++) {
            int sizeL = (int) (ALLPASS_TIMES_L[i] * sampleRateRatio * 2);
            int sizeR = (int) (ALLPASS_TIMES_R[i] * sampleRateRatio * 2);
            allpassBufferL[i] = new float[sizeL];
            allpassBufferR[i] = new float[sizeR];
        }

        // Damping state
        dampStateL = new float[NUM_LATE_DELAYS];
        dampStateR = new float[NUM_LATE_DELAYS];

        // LFOs
        lfoPhaseL = new double[NUM_LATE_DELAYS];
        lfoPhaseR = new double[NUM_LATE_DELAYS];
        // Start with different phases for decorrelation
        for (int i = 0; i < NUM_LATE_DELAYS; i++) {
            lfoPhaseL[i] = i * 0.25;
            lfoPhaseR[i] = i * 0.25 + 0.125;
        }

        crossFeedL = 0;
        crossFeedR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Convert mono to stereo
        float[] outL = new float[frameCount];
        float[] outR = new float[frameCount];

        processInternal(input, input, outL, outR, frameCount);

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
        float width = widthParam.getValue() / 100.0f;
        float depth = depthParam.getValue() / 100.0f;
        float spread = spreadParam.getValue() / 100.0f;
        float haasMs = haasDelayParam.getValue();
        float crossfeed = crossfeedParam.getValue() / 100.0f;
        float diffusion = diffusionParam.getValue() / 100.0f;
        float modulation = modulationParam.getValue() / 100.0f;
        float balance = balanceParam.getValue() / 100.0f;

        // Calculate delay times in samples
        int predelaySamples = (int) (predelayMs * sampleRate / 1000.0f);
        predelaySamples = Math.min(predelaySamples, MAX_PREDELAY_SAMPLES - 1);

        int haasSamples = (int) (haasMs * sampleRate / 1000.0f);
        haasSamples = Math.min(haasSamples, MAX_HAAS_SAMPLES - 1);

        // Damping coefficient
        float dampCoef = 0.2f + 0.7f * damping;

        // Calculate tank feedback from decay
        float avgDelayTime = 0;
        for (int d : LATE_TIMES_L) avgDelayTime += d;
        avgDelayTime /= NUM_LATE_DELAYS;
        float tankFeedback = (float) Math.exp(-3.0 * avgDelayTime / (decay * sampleRate));
        tankFeedback = Math.min(0.97f, tankFeedback);

        // Effective all-pass gain based on diffusion
        float apGain = ALLPASS_GAIN * diffusion;

        // Balance gains
        float balanceL = balance < 0 ? 1.0f : 1.0f - balance;
        float balanceR = balance > 0 ? 1.0f : 1.0f + balance;

        for (int i = 0; i < frameCount; i++) {
            float inL = inputL[i];
            float inR = inputR[i];

            // === HAAS EFFECT (stereo positioning) ===
            haasBufferL[haasWritePos] = inL;
            haasBufferR[haasWritePos] = inR;

            float haasOutL, haasOutR;
            if (haasSamples > 0) {
                // Positive Haas: delay right channel (sound appears from left)
                int readPosR = (haasWritePos - haasSamples + MAX_HAAS_SAMPLES) % MAX_HAAS_SAMPLES;
                haasOutL = inL;
                haasOutR = haasBufferR[readPosR];
            } else {
                haasOutL = inL;
                haasOutR = inR;
            }
            haasWritePos = (haasWritePos + 1) % MAX_HAAS_SAMPLES;

            // === PREDELAY ===
            predelayBufferL[predelayWritePos] = haasOutL;
            predelayBufferR[predelayWritePos] = haasOutR;

            int predelayReadPos = (predelayWritePos - predelaySamples + MAX_PREDELAY_SAMPLES) % MAX_PREDELAY_SAMPLES;
            float delayedL = predelayBufferL[predelayReadPos];
            float delayedR = predelayBufferR[predelayReadPos];

            predelayWritePos = (predelayWritePos + 1) % MAX_PREDELAY_SAMPLES;

            // === EARLY REFLECTIONS (decorrelated and spread) ===
            float earlyL = 0;
            float earlyR = 0;

            for (int t = 0; t < NUM_EARLY_TAPS; t++) {
                // Write to early buffers
                earlyBufferL[t][earlyWritePos[t]] = delayedL;
                earlyBufferR[t][earlyWritePos[t]] = delayedR;

                // Calculate delay time in samples
                int earlyTimeL = (int) (EARLY_TIMES_L[t] * sampleRate / 1000.0f);
                int earlyTimeR = (int) (EARLY_TIMES_R[t] * sampleRate / 1000.0f);

                earlyTimeL = Math.min(earlyTimeL, earlyBufferL[t].length - 1);
                earlyTimeR = Math.min(earlyTimeR, earlyBufferR[t].length - 1);

                int readPosL = (earlyWritePos[t] - earlyTimeL + earlyBufferL[t].length) % earlyBufferL[t].length;
                int readPosR = (earlyWritePos[t] - earlyTimeR + earlyBufferR[t].length) % earlyBufferR[t].length;

                float tapL = earlyBufferL[t][readPosL] * EARLY_GAINS[t];
                float tapR = earlyBufferR[t][readPosR] * EARLY_GAINS[t];

                // Pan each tap with spread control
                float panL = EARLY_PAN_L[t] * spread;
                float panR = EARLY_PAN_R[t] * spread;

                // Constant power panning
                float gainLL = (float) Math.cos((panL + 1) * Math.PI / 4);
                float gainLR = (float) Math.sin((panL + 1) * Math.PI / 4);
                float gainRL = (float) Math.cos((panR + 1) * Math.PI / 4);
                float gainRR = (float) Math.sin((panR + 1) * Math.PI / 4);

                earlyL += tapL * gainLL + tapR * gainRL;
                earlyR += tapL * gainLR + tapR * gainRR;

                earlyWritePos[t] = (earlyWritePos[t] + 1) % earlyBufferL[t].length;
            }

            // Normalize early reflections
            earlyL *= 0.25f;
            earlyR *= 0.25f;

            // === INPUT DIFFUSION (all-pass per channel) ===
            float diffL = delayedL + earlyL * depth;
            float diffR = delayedR + earlyR * depth;

            for (int ap = 0; ap < NUM_ALLPASS; ap++) {
                // Left channel
                int apTimeL = ALLPASS_TIMES_L[ap];
                apTimeL = Math.min(apTimeL, allpassBufferL[ap].length - 1);

                int readPosL = (allpassWritePosL[ap] - apTimeL + allpassBufferL[ap].length) % allpassBufferL[ap].length;
                float apOutL = allpassBufferL[ap][readPosL];
                float apInL = diffL + apGain * apOutL;
                allpassBufferL[ap][allpassWritePosL[ap]] = apInL;
                diffL = apOutL - apGain * apInL;
                allpassWritePosL[ap] = (allpassWritePosL[ap] + 1) % allpassBufferL[ap].length;

                // Right channel (different times for decorrelation)
                int apTimeR = ALLPASS_TIMES_R[ap];
                apTimeR = Math.min(apTimeR, allpassBufferR[ap].length - 1);

                int readPosR = (allpassWritePosR[ap] - apTimeR + allpassBufferR[ap].length) % allpassBufferR[ap].length;
                float apOutR = allpassBufferR[ap][readPosR];
                float apInR = diffR + apGain * apOutR;
                allpassBufferR[ap][allpassWritePosR[ap]] = apInR;
                diffR = apOutR - apGain * apInR;
                allpassWritePosR[ap] = (allpassWritePosR[ap] + 1) % allpassBufferR[ap].length;
            }

            // === LATE REVERB FDN (independent L/R with cross-feed) ===
            float lateOutL = 0;
            float lateOutR = 0;

            for (int d = 0; d < NUM_LATE_DELAYS; d++) {
                // Modulated delay times
                int baseTimeL = (int) (LATE_TIMES_L[d] * sampleRate / 48000.0f);
                int baseTimeR = (int) (LATE_TIMES_R[d] * sampleRate / 48000.0f);

                int modOffsetL = (int) (MOD_DEPTH[d] * modulation * Math.sin(2 * Math.PI * lfoPhaseL[d]));
                int modOffsetR = (int) (MOD_DEPTH[d] * modulation * Math.sin(2 * Math.PI * lfoPhaseR[d]));

                int delayTimeL = Math.max(1, Math.min(baseTimeL + modOffsetL, lateBufferL[d].length - 1));
                int delayTimeR = Math.max(1, Math.min(baseTimeR + modOffsetR, lateBufferR[d].length - 1));

                // Read from delays
                int readPosL = (lateWritePosL[d] - delayTimeL + lateBufferL[d].length) % lateBufferL[d].length;
                int readPosR = (lateWritePosR[d] - delayTimeR + lateBufferR[d].length) % lateBufferR[d].length;

                float delOutL = lateBufferL[d][readPosL];
                float delOutR = lateBufferR[d][readPosR];

                // Apply damping
                dampStateL[d] = dampStateL[d] + dampCoef * (delOutL - dampStateL[d]);
                dampStateR[d] = dampStateR[d] + dampCoef * (delOutR - dampStateR[d]);

                delOutL = delOutL * (1 - damping) + dampStateL[d] * damping;
                delOutR = delOutR * (1 - damping) + dampStateR[d] * damping;

                // Hadamard-like mixing
                if (d % 2 == 0) {
                    lateOutL += delOutL;
                    lateOutR += delOutR;
                } else {
                    lateOutL -= delOutL;
                    lateOutR -= delOutR;
                }

                // Update LFOs
                lfoPhaseL[d] += LFO_RATES[d] / sampleRate;
                lfoPhaseR[d] += LFO_RATES[d] * 1.1 / sampleRate; // Slightly different rate
                if (lfoPhaseL[d] >= 1.0) lfoPhaseL[d] -= 1.0;
                if (lfoPhaseR[d] >= 1.0) lfoPhaseR[d] -= 1.0;
            }

            // Normalize
            lateOutL *= 0.5f;
            lateOutR *= 0.5f;

            // Cross-feed for cohesion
            float crossL = lateOutL + crossfeed * crossFeedR;
            float crossR = lateOutR + crossfeed * crossFeedL;
            crossFeedL = lateOutL;
            crossFeedR = lateOutR;

            // Write to delay buffers
            for (int d = 0; d < NUM_LATE_DELAYS; d++) {
                // Cross-couple between channels
                float fbL = tankFeedback * (d < 2 ? crossL : crossR);
                float fbR = tankFeedback * (d < 2 ? crossR : crossL);

                // Add diffused input
                float inputMixL = (d == 0 || d == 2) ? diffL : diffR;
                float inputMixR = (d == 0 || d == 2) ? diffR : diffL;

                lateBufferL[d][lateWritePosL[d]] = inputMixL + fbL;
                lateBufferR[d][lateWritePosR[d]] = inputMixR + fbR;

                lateWritePosL[d] = (lateWritePosL[d] + 1) % lateBufferL[d].length;
                lateWritePosR[d] = (lateWritePosR[d] + 1) % lateBufferR[d].length;
            }

            // === COMBINE EARLY + LATE ===
            float reverbL = earlyL + crossL;
            float reverbR = earlyR + crossR;

            // === WIDTH PROCESSING (Mid/Side) ===
            float mid = (reverbL + reverbR) * 0.5f;
            float side = (reverbL - reverbR) * 0.5f * width;

            float wetL = mid + side;
            float wetR = mid - side;

            // Apply balance
            wetL *= balanceL;
            wetR *= balanceR;

            // === FINAL MIX ===
            outputL[i] = inL * dry + wetL * wet;
            outputR[i] = inR * dry + wetR * wet;
        }
    }

    @Override
    protected void onReset() {
        // Clear Haas buffers
        if (haasBufferL != null) {
            java.util.Arrays.fill(haasBufferL, 0);
            java.util.Arrays.fill(haasBufferR, 0);
        }
        haasWritePos = 0;

        // Clear predelay
        if (predelayBufferL != null) {
            java.util.Arrays.fill(predelayBufferL, 0);
            java.util.Arrays.fill(predelayBufferR, 0);
        }
        predelayWritePos = 0;

        // Clear early reflections
        for (int i = 0; i < NUM_EARLY_TAPS; i++) {
            if (earlyBufferL[i] != null) {
                java.util.Arrays.fill(earlyBufferL[i], 0);
                java.util.Arrays.fill(earlyBufferR[i], 0);
            }
            earlyWritePos[i] = 0;
        }

        // Clear all-pass
        for (int i = 0; i < NUM_ALLPASS; i++) {
            if (allpassBufferL[i] != null) {
                java.util.Arrays.fill(allpassBufferL[i], 0);
                java.util.Arrays.fill(allpassBufferR[i], 0);
            }
            allpassWritePosL[i] = 0;
            allpassWritePosR[i] = 0;
        }

        // Clear late reverb
        for (int i = 0; i < NUM_LATE_DELAYS; i++) {
            if (lateBufferL[i] != null) {
                java.util.Arrays.fill(lateBufferL[i], 0);
                java.util.Arrays.fill(lateBufferR[i], 0);
            }
            lateWritePosL[i] = 0;
            lateWritePosR[i] = 0;
            dampStateL[i] = 0;
            dampStateR[i] = 0;
            lfoPhaseL[i] = i * 0.25;
            lfoPhaseR[i] = i * 0.25 + 0.125;
        }

        crossFeedL = 0;
        crossFeedR = 0;
    }

    @Override
    public int[] getParameterRowSizes() {
        // Row 1 (Reverb): Mix, Decay, Pre-Delay, Damping, Diffusion, Modulation
        // Row 2 (Stereo): Width, Depth, Spread, Haas, X-Feed, Balance
        return new int[] {6, 6};
    }
}
