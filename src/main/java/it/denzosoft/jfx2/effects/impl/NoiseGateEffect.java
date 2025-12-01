package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Noise Gate effect with lookahead.
 *
 * <p>Attenuates signal when it falls below the threshold.
 * Uses 5ms lookahead to preserve attack transients.</p>
 */
public class NoiseGateEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "noisegate",
            "Noise Gate",
            "Reduces noise by attenuating signal below threshold",
            EffectCategory.DYNAMICS
    );

    // Parameters
    private final Parameter thresholdParam;
    private final Parameter attackParam;
    private final Parameter holdParam;
    private final Parameter releaseParam;
    private final Parameter rangeParam;

    // State - Left channel
    private float envelopeL;
    private float gateGainL;
    private int holdCounterL;
    private float[] lookaheadBufferL;
    private int lookaheadWritePosL;

    // State - Right channel
    private float envelopeR;
    private float gateGainR;
    private int holdCounterR;
    private float[] lookaheadBufferR;
    private int lookaheadWritePosR;

    // Lookahead (5ms)
    private static final float LOOKAHEAD_MS = 5.0f;
    private int lookaheadSamples;

    // Metering (for UI visualization)
    private volatile float inputLevelDb = -80f;
    private volatile float outputLevelDb = -80f;
    private volatile float gateReduction = 0f;  // Current gain reduction in dB

    public NoiseGateEffect() {
        super(METADATA);

        // Threshold: -80 dB to 0 dB, default -40 dB
        thresholdParam = addFloatParameter("threshold", "Threshold",
                "Level below which the gate closes. Set just above the noise floor.",
                -80.0f, 0.0f, -40.0f, "dB");

        // Attack: 0.1 ms to 50 ms, default 1 ms
        attackParam = addFloatParameter("attack", "Attack",
                "How quickly the gate opens when signal exceeds threshold. Fast for tight response.",
                0.1f, 50.0f, 1.0f, "ms");

        // Hold: 0 ms to 500 ms, default 50 ms
        holdParam = addFloatParameter("hold", "Hold",
                "Time the gate stays open after signal drops below threshold. Prevents chatter.",
                0.0f, 500.0f, 50.0f, "ms");

        // Release: 5 ms to 500 ms, default 100 ms
        releaseParam = addFloatParameter("release", "Release",
                "How quickly the gate closes after hold time. Longer for smoother fade-out.",
                5.0f, 500.0f, 100.0f, "ms");

        // Range: -80 dB to 0 dB, default -80 dB (full attenuation)
        rangeParam = addFloatParameter("range", "Range",
                "Maximum attenuation when gate is closed. 0 dB = no attenuation, -80 dB = silence.",
                -80.0f, 0.0f, -80.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Calculate lookahead buffer size
        lookaheadSamples = (int) (LOOKAHEAD_MS * sampleRate / 1000.0f);

        // Initialize Left channel
        lookaheadBufferL = new float[lookaheadSamples + maxFrameCount];
        lookaheadWritePosL = 0;
        envelopeL = 0.0f;
        gateGainL = 0.0f;  // Start closed
        holdCounterL = 0;

        // Initialize Right channel
        lookaheadBufferR = new float[lookaheadSamples + maxFrameCount];
        lookaheadWritePosR = 0;
        envelopeR = 0.0f;
        gateGainR = 0.0f;
        holdCounterR = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float thresholdDb = thresholdParam.getValue();
        float thresholdLinear = dbToLinear(thresholdDb);
        float rangeLinear = dbToLinear(rangeParam.getValue());

        // Calculate attack/release coefficients
        float attackMs = attackParam.getValue();
        float releaseMs = releaseParam.getValue();
        float holdMs = holdParam.getValue();

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
        int holdSamples = (int) (holdMs * sampleRate / 1000.0);

        // Envelope follower coefficient (fast for detection)
        float envCoeff = (float) Math.exp(-1.0 / (0.5 * sampleRate / 1000.0));  // 0.5 ms

        // Metering accumulators
        float inputPeak = 0f;
        float outputPeak = 0f;
        float minGain = 1f;

        for (int i = 0; i < frameCount && i < input.length; i++) {
            float sample = input[i];

            // Write to lookahead buffer
            lookaheadBufferL[lookaheadWritePosL % lookaheadBufferL.length] = sample;

            // Envelope follower (peak detection)
            float absInput = Math.abs(sample);
            if (absInput > envelopeL) {
                envelopeL = absInput;  // Instant attack
            } else {
                envelopeL = envCoeff * envelopeL + (1.0f - envCoeff) * absInput;
            }

            // Track input peak for metering
            if (absInput > inputPeak) {
                inputPeak = absInput;
            }

            // Gate logic
            boolean gateOpen = envelopeL > thresholdLinear;

            if (gateOpen) {
                holdCounterL = holdSamples;
                gateGainL = attackCoeff * gateGainL + (1.0f - attackCoeff) * 1.0f;
            } else if (holdCounterL > 0) {
                holdCounterL--;
                gateGainL = attackCoeff * gateGainL + (1.0f - attackCoeff) * 1.0f;
            } else {
                gateGainL = releaseCoeff * gateGainL + (1.0f - releaseCoeff) * rangeLinear;
            }

            // Track minimum gain for reduction metering
            if (gateGainL < minGain) {
                minGain = gateGainL;
            }

            // Read from lookahead buffer
            int readPos = (lookaheadWritePosL - lookaheadSamples + lookaheadBufferL.length) % lookaheadBufferL.length;
            float delayedSample = lookaheadBufferL[readPos];

            float outputSample = delayedSample * gateGainL;
            if (i < output.length) {
                output[i] = outputSample;
            }

            // Track output peak
            float absOutput = Math.abs(outputSample);
            if (absOutput > outputPeak) {
                outputPeak = absOutput;
            }

            lookaheadWritePosL++;
        }

        // Update metering values (convert to dB)
        inputLevelDb = inputPeak > 0 ? (float) (20 * Math.log10(inputPeak)) : -80f;
        outputLevelDb = outputPeak > 0 ? (float) (20 * Math.log10(outputPeak)) : -80f;
        gateReduction = minGain > 0 ? (float) (20 * Math.log10(minGain)) : -80f;
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float thresholdDb = thresholdParam.getValue();
        float thresholdLinear = dbToLinear(thresholdDb);
        float rangeLinear = dbToLinear(rangeParam.getValue());

        float attackMs = attackParam.getValue();
        float releaseMs = releaseParam.getValue();
        float holdMs = holdParam.getValue();

        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));
        int holdSamples = (int) (holdMs * sampleRate / 1000.0);
        float envCoeff = (float) Math.exp(-1.0 / (0.5 * sampleRate / 1000.0));

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        // Metering accumulators
        float inputPeak = 0f;
        float outputPeak = 0f;
        float minGain = 1f;

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // Write to lookahead buffers
            lookaheadBufferL[lookaheadWritePosL % lookaheadBufferL.length] = sampleL;
            lookaheadBufferR[lookaheadWritePosR % lookaheadBufferR.length] = sampleR;

            // Envelope follower - Left
            float absInputLVal = Math.abs(sampleL);
            if (absInputLVal > envelopeL) {
                envelopeL = absInputLVal;
            } else {
                envelopeL = envCoeff * envelopeL + (1.0f - envCoeff) * absInputLVal;
            }

            // Envelope follower - Right
            float absInputRVal = Math.abs(sampleR);
            if (absInputRVal > envelopeR) {
                envelopeR = absInputRVal;
            } else {
                envelopeR = envCoeff * envelopeR + (1.0f - envCoeff) * absInputRVal;
            }

            // Track input peak (max of both channels)
            float inputMax = Math.max(absInputLVal, absInputRVal);
            if (inputMax > inputPeak) {
                inputPeak = inputMax;
            }

            // Gate logic - Left
            boolean gateOpenL = envelopeL > thresholdLinear;
            if (gateOpenL) {
                holdCounterL = holdSamples;
                gateGainL = attackCoeff * gateGainL + (1.0f - attackCoeff) * 1.0f;
            } else if (holdCounterL > 0) {
                holdCounterL--;
                gateGainL = attackCoeff * gateGainL + (1.0f - attackCoeff) * 1.0f;
            } else {
                gateGainL = releaseCoeff * gateGainL + (1.0f - releaseCoeff) * rangeLinear;
            }

            // Gate logic - Right
            boolean gateOpenR = envelopeR > thresholdLinear;
            if (gateOpenR) {
                holdCounterR = holdSamples;
                gateGainR = attackCoeff * gateGainR + (1.0f - attackCoeff) * 1.0f;
            } else if (holdCounterR > 0) {
                holdCounterR--;
                gateGainR = attackCoeff * gateGainR + (1.0f - attackCoeff) * 1.0f;
            } else {
                gateGainR = releaseCoeff * gateGainR + (1.0f - releaseCoeff) * rangeLinear;
            }

            // Track minimum gain (max reduction)
            float currentMinGain = Math.min(gateGainL, gateGainR);
            if (currentMinGain < minGain) {
                minGain = currentMinGain;
            }

            // Read from lookahead buffers
            int readPosL = (lookaheadWritePosL - lookaheadSamples + lookaheadBufferL.length) % lookaheadBufferL.length;
            int readPosR = (lookaheadWritePosR - lookaheadSamples + lookaheadBufferR.length) % lookaheadBufferR.length;

            float outL = lookaheadBufferL[readPosL] * gateGainL;
            float outR = lookaheadBufferR[readPosR] * gateGainR;
            outputL[i] = outL;
            outputR[i] = outR;

            // Track output peak
            float outputMax = Math.max(Math.abs(outL), Math.abs(outR));
            if (outputMax > outputPeak) {
                outputPeak = outputMax;
            }

            lookaheadWritePosL++;
            lookaheadWritePosR++;
        }

        // Update metering values (convert to dB)
        inputLevelDb = inputPeak > 0 ? (float) (20 * Math.log10(inputPeak)) : -80f;
        outputLevelDb = outputPeak > 0 ? (float) (20 * Math.log10(outputPeak)) : -80f;
        gateReduction = minGain > 0 ? (float) (20 * Math.log10(minGain)) : -80f;
    }

    @Override
    protected void onReset() {
        envelopeL = 0.0f;
        envelopeR = 0.0f;
        gateGainL = 0.0f;
        gateGainR = 0.0f;
        holdCounterL = 0;
        holdCounterR = 0;
        if (lookaheadBufferL != null) {
            java.util.Arrays.fill(lookaheadBufferL, 0.0f);
        }
        if (lookaheadBufferR != null) {
            java.util.Arrays.fill(lookaheadBufferR, 0.0f);
        }
        lookaheadWritePosL = 0;
        lookaheadWritePosR = 0;
    }

    // Convenience setters
    public void setThresholdDb(float dB) {
        thresholdParam.setValue(dB);
    }

    public void setAttackMs(float ms) {
        attackParam.setValue(ms);
    }

    public void setHoldMs(float ms) {
        holdParam.setValue(ms);
    }

    public void setReleaseMs(float ms) {
        releaseParam.setValue(ms);
    }

    public void setRangeDb(float dB) {
        rangeParam.setValue(dB);
    }

    // Metering getters for UI visualization
    /**
     * Get the current input level in dB (-80 to 0).
     */
    public float getInputLevelDb() {
        return inputLevelDb;
    }

    /**
     * Get the current output level in dB (-80 to 0).
     */
    public float getOutputLevelDb() {
        return outputLevelDb;
    }

    /**
     * Get the current gain reduction in dB (-80 to 0).
     * 0 dB = gate fully open, -80 dB = gate fully closed.
     */
    public float getGateReductionDb() {
        return gateReduction;
    }

    /**
     * Get the threshold value for visualization.
     */
    public float getThresholdDb() {
        return thresholdParam.getValue();
    }
}
