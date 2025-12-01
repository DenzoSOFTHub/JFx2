package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Dynamic range compressor with RMS detection.
 *
 * <p>Reduces dynamic range by attenuating signals above the threshold.
 * Uses RMS detection for a smooth, musical response.</p>
 */
public class CompressorEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "compressor",
            "Compressor",
            "Dynamic range compressor with RMS detection",
            EffectCategory.DYNAMICS
    );

    // Parameters
    private final Parameter thresholdParam;
    private final Parameter ratioParam;
    private final Parameter attackParam;
    private final Parameter releaseParam;
    private final Parameter kneeParam;
    private final Parameter makeupParam;

    // State - Left channel
    private float rmsLevelL;
    private float gainReductionL;

    // State - Right channel
    private float rmsLevelR;
    private float gainReductionR;

    // RMS window
    private static final float RMS_WINDOW_MS = 50.0f;
    private float rmsCoeff;

    public CompressorEffect() {
        super(METADATA);

        // Threshold: -60 dB to 0 dB, default -20 dB
        thresholdParam = addFloatParameter("threshold", "Threshold",
                "Level above which compression begins. Lower values compress more of the signal.",
                -60.0f, 0.0f, -20.0f, "dB");

        // Ratio: 1:1 to 20:1, default 4:1
        ratioParam = addFloatParameter("ratio", "Ratio",
                "Amount of compression. Higher ratios mean more aggressive limiting of loud signals.",
                1.0f, 20.0f, 4.0f, ":1");

        // Attack: 0.1 ms to 100 ms, default 10 ms
        attackParam = addFloatParameter("attack", "Attack",
                "How quickly compression engages. Faster attack catches transients, slower preserves punch.",
                0.1f, 100.0f, 10.0f, "ms");

        // Release: 10 ms to 1000 ms, default 100 ms
        releaseParam = addFloatParameter("release", "Release",
                "How quickly compression releases. Faster can cause pumping, slower sounds more natural.",
                10.0f, 1000.0f, 100.0f, "ms");

        // Knee: 0 dB (hard) to 12 dB (soft), default 3 dB
        kneeParam = addFloatParameter("knee", "Knee",
                "Transition smoothness around threshold. Soft knee (higher) sounds more transparent.",
                0.0f, 12.0f, 3.0f, "dB");

        // Makeup gain: 0 dB to 24 dB, default 0 dB
        makeupParam = addFloatParameter("makeup", "Makeup",
                "Compensates for volume lost during compression. Increase to match original loudness.",
                0.0f, 24.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // RMS averaging coefficient
        rmsCoeff = (float) Math.exp(-1.0 / (RMS_WINDOW_MS * sampleRate / 1000.0));

        // Initialize state - Left
        rmsLevelL = 0.0f;
        gainReductionL = 1.0f;

        // Initialize state - Right
        rmsLevelR = 0.0f;
        gainReductionR = 1.0f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float thresholdDb = thresholdParam.getValue();
        float ratio = ratioParam.getValue();
        float attackMs = attackParam.getValue();
        float releaseMs = releaseParam.getValue();
        float kneeDb = kneeParam.getValue();
        float makeupDb = makeupParam.getValue();

        float makeupLinear = dbToLinear(makeupDb);

        // Attack/release coefficients
        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i];

            // RMS level detection
            float squared = sample * sample;
            rmsLevelL = rmsCoeff * rmsLevelL + (1.0f - rmsCoeff) * squared;
            float rmsDb = linearToDb((float) Math.sqrt(rmsLevelL));

            // Calculate gain reduction
            float targetGainDb = calculateGainReduction(rmsDb, thresholdDb, ratio, kneeDb);
            float targetGainLinear = dbToLinear(targetGainDb);

            // Smooth gain changes (attack/release)
            if (targetGainLinear < gainReductionL) {
                // Attack (gain decreasing)
                gainReductionL = attackCoeff * gainReductionL + (1.0f - attackCoeff) * targetGainLinear;
            } else {
                // Release (gain increasing)
                gainReductionL = releaseCoeff * gainReductionL + (1.0f - releaseCoeff) * targetGainLinear;
            }

            // Apply compression and makeup gain
            output[i] = sample * gainReductionL * makeupLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float thresholdDb = thresholdParam.getValue();
        float ratio = ratioParam.getValue();
        float attackMs = attackParam.getValue();
        float releaseMs = releaseParam.getValue();
        float kneeDb = kneeParam.getValue();
        float makeupDb = makeupParam.getValue();

        float makeupLinear = dbToLinear(makeupDb);

        // Attack/release coefficients
        float attackCoeff = (float) Math.exp(-1.0 / (attackMs * sampleRate / 1000.0));
        float releaseCoeff = (float) Math.exp(-1.0 / (releaseMs * sampleRate / 1000.0));

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i];
            float sampleR = inputR[i];

            // RMS level detection - Left
            float squaredL = sampleL * sampleL;
            rmsLevelL = rmsCoeff * rmsLevelL + (1.0f - rmsCoeff) * squaredL;
            float rmsDbL = linearToDb((float) Math.sqrt(rmsLevelL));

            // RMS level detection - Right
            float squaredR = sampleR * sampleR;
            rmsLevelR = rmsCoeff * rmsLevelR + (1.0f - rmsCoeff) * squaredR;
            float rmsDbR = linearToDb((float) Math.sqrt(rmsLevelR));

            // Calculate gain reduction - Left
            float targetGainDbL = calculateGainReduction(rmsDbL, thresholdDb, ratio, kneeDb);
            float targetGainLinearL = dbToLinear(targetGainDbL);

            // Calculate gain reduction - Right
            float targetGainDbR = calculateGainReduction(rmsDbR, thresholdDb, ratio, kneeDb);
            float targetGainLinearR = dbToLinear(targetGainDbR);

            // Smooth gain changes - Left
            if (targetGainLinearL < gainReductionL) {
                gainReductionL = attackCoeff * gainReductionL + (1.0f - attackCoeff) * targetGainLinearL;
            } else {
                gainReductionL = releaseCoeff * gainReductionL + (1.0f - releaseCoeff) * targetGainLinearL;
            }

            // Smooth gain changes - Right
            if (targetGainLinearR < gainReductionR) {
                gainReductionR = attackCoeff * gainReductionR + (1.0f - attackCoeff) * targetGainLinearR;
            } else {
                gainReductionR = releaseCoeff * gainReductionR + (1.0f - releaseCoeff) * targetGainLinearR;
            }

            // Apply compression and makeup gain
            outputL[i] = sampleL * gainReductionL * makeupLinear;
            outputR[i] = sampleR * gainReductionR * makeupLinear;
        }
    }

    /**
     * Calculate gain reduction in dB using soft knee.
     */
    private float calculateGainReduction(float inputDb, float thresholdDb, float ratio, float kneeDb) {
        if (ratio <= 1.0f) return 0.0f;  // No compression

        float halfKnee = kneeDb / 2.0f;
        float overshoot = inputDb - thresholdDb;

        if (overshoot <= -halfKnee) {
            // Below knee - no compression
            return 0.0f;
        } else if (overshoot >= halfKnee) {
            // Above knee - full compression
            return -(overshoot * (1.0f - 1.0f / ratio));
        } else {
            // In knee region - smooth transition
            float x = overshoot + halfKnee;
            return -(x * x * (1.0f - 1.0f / ratio)) / (2.0f * kneeDb);
        }
    }

    @Override
    protected void onReset() {
        rmsLevelL = 0.0f;
        rmsLevelR = 0.0f;
        gainReductionL = 1.0f;
        gainReductionR = 1.0f;
    }

    /**
     * Get current gain reduction in dB (average of L and R).
     */
    public float getGainReductionDb() {
        return linearToDb((gainReductionL + gainReductionR) / 2.0f);
    }

    // Convenience setters
    public void setThresholdDb(float dB) {
        thresholdParam.setValue(dB);
    }

    public void setRatio(float ratio) {
        ratioParam.setValue(ratio);
    }

    public void setAttackMs(float ms) {
        attackParam.setValue(ms);
    }

    public void setReleaseMs(float ms) {
        releaseParam.setValue(ms);
    }

    public void setKneeDb(float dB) {
        kneeParam.setValue(dB);
    }

    public void setMakeupDb(float dB) {
        makeupParam.setValue(dB);
    }
}
