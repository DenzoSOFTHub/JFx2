package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Brickwall Limiter effect.
 *
 * <p>Prevents audio from exceeding a threshold level, providing
 * transparent peak control and protection from clipping.</p>
 *
 * <p>Features:
 * - Look-ahead limiting for transparent operation
 * - Adjustable threshold and release
 * - Soft knee option for smoother limiting
 * - Auto makeup gain option</p>
 */
public class LimiterEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "limiter",
            "Limiter",
            "Brickwall limiter for peak control",
            EffectCategory.DYNAMICS
    );

    // Parameters
    private final Parameter thresholdParam;
    private final Parameter releaseParam;
    private final Parameter ceilingParam;
    private final Parameter kneeParam;

    // Envelope state
    private float envelopeL = 0;
    private float envelopeR = 0;

    // Look-ahead buffer
    private float[] lookAheadL;
    private float[] lookAheadR;
    private int lookAheadPos;
    private int lookAheadSamples;

    public LimiterEffect() {
        super(METADATA);

        // Threshold: -24 dB to 0 dB, default -6 dB
        thresholdParam = addFloatParameter("threshold", "Threshold",
                "Level above which limiting begins. Lower = more limiting.",
                -24.0f, 0.0f, -6.0f, "dB");

        // Release: 10 ms to 1000 ms, default 100 ms
        releaseParam = addFloatParameter("release", "Release",
                "How quickly the limiter recovers after peaks.",
                10.0f, 1000.0f, 100.0f, "ms");

        // Ceiling: -6 dB to 0 dB, default -0.3 dB
        ceilingParam = addFloatParameter("ceiling", "Ceiling",
                "Maximum output level. Set below 0 dB to prevent inter-sample peaks.",
                -6.0f, 0.0f, -0.3f, "dB");

        // Knee: 0 dB to 6 dB, default 0 dB (hard knee)
        kneeParam = addFloatParameter("knee", "Knee",
                "Softness of limiting. 0 = hard knee, higher = softer transition.",
                0.0f, 6.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // 5ms look-ahead
        lookAheadSamples = (int) (0.005 * sampleRate);
        lookAheadL = new float[lookAheadSamples];
        lookAheadR = new float[lookAheadSamples];
        lookAheadPos = 0;

        envelopeL = 0;
        envelopeR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float thresholdLin = dbToLinear(thresholdParam.getValue());
        float ceilingLin = dbToLinear(ceilingParam.getValue());
        float kneeDb = kneeParam.getValue();
        float releaseMs = releaseParam.getValue();

        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
        float attackCoeff = (float) Math.exp(-1.0 / (0.001 * sampleRate)); // 1ms attack

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // Store in look-ahead buffer
            float delayed = lookAheadL[lookAheadPos];
            lookAheadL[lookAheadPos] = sample;
            lookAheadPos = (lookAheadPos + 1) % lookAheadSamples;

            // Envelope follower
            float absInput = Math.abs(sample);
            if (absInput > envelopeL) {
                envelopeL = attackCoeff * envelopeL + (1 - attackCoeff) * absInput;
            } else {
                envelopeL = releaseCoeff * envelopeL;
            }

            // Calculate gain reduction
            float gainReduction = 1.0f;
            if (envelopeL > thresholdLin) {
                // Apply soft knee if enabled
                float overDb = linearToDb(envelopeL / thresholdLin);
                if (kneeDb > 0 && overDb < kneeDb) {
                    // Soft knee region
                    float kneeGain = overDb * overDb / (2 * kneeDb);
                    gainReduction = dbToLinear(-kneeGain);
                } else {
                    // Full limiting
                    gainReduction = thresholdLin / envelopeL;
                }
            }

            // Apply gain reduction with ceiling
            float limited = delayed * gainReduction;

            // Hard clip at ceiling (safety)
            if (limited > ceilingLin) limited = ceilingLin;
            if (limited < -ceilingLin) limited = -ceilingLin;

            output[i] = limited;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float thresholdLin = dbToLinear(thresholdParam.getValue());
        float ceilingLin = dbToLinear(ceilingParam.getValue());
        float kneeDb = kneeParam.getValue();
        float releaseMs = releaseParam.getValue();

        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
        float attackCoeff = (float) Math.exp(-1.0 / (0.001 * sampleRate));

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                  Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Store in look-ahead buffer
            float delayedL = lookAheadL[lookAheadPos];
            float delayedR = lookAheadR[lookAheadPos];
            lookAheadL[lookAheadPos] = sampleL;
            lookAheadR[lookAheadPos] = sampleR;
            lookAheadPos = (lookAheadPos + 1) % lookAheadSamples;

            // Linked stereo envelope (use max of both channels)
            float absInput = Math.max(Math.abs(sampleL), Math.abs(sampleR));
            if (absInput > envelopeL) {
                envelopeL = attackCoeff * envelopeL + (1 - attackCoeff) * absInput;
            } else {
                envelopeL = releaseCoeff * envelopeL;
            }

            // Calculate gain reduction
            float gainReduction = 1.0f;
            if (envelopeL > thresholdLin) {
                float overDb = linearToDb(envelopeL / thresholdLin);
                if (kneeDb > 0 && overDb < kneeDb) {
                    float kneeGain = overDb * overDb / (2 * kneeDb);
                    gainReduction = dbToLinear(-kneeGain);
                } else {
                    gainReduction = thresholdLin / envelopeL;
                }
            }

            // Apply gain reduction
            float limitedL = delayedL * gainReduction;
            float limitedR = delayedR * gainReduction;

            // Hard clip at ceiling
            limitedL = Math.max(-ceilingLin, Math.min(ceilingLin, limitedL));
            limitedR = Math.max(-ceilingLin, Math.min(ceilingLin, limitedR));

            outputL[i] = limitedL;
            outputR[i] = limitedR;
        }
    }

    @Override
    protected void onReset() {
        envelopeL = 0;
        envelopeR = 0;
        if (lookAheadL != null) java.util.Arrays.fill(lookAheadL, 0);
        if (lookAheadR != null) java.util.Arrays.fill(lookAheadR, 0);
        lookAheadPos = 0;
    }

    @Override
    public int getLatency() {
        return lookAheadSamples;
    }

    // Convenience setters
    public void setThreshold(float dB) {
        thresholdParam.setValue(dB);
    }

    public void setRelease(float ms) {
        releaseParam.setValue(ms);
    }

    public void setCeiling(float dB) {
        ceilingParam.setValue(dB);
    }

    public void setKnee(float dB) {
        kneeParam.setValue(dB);
    }
}
