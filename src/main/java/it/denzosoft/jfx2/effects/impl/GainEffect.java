package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Gain/volume effect with optional soft clipping.
 *
 * <p>Provides gain adjustment in dB with smooth transitions and
 * multiple saturation curves to handle signal overflow gracefully.</p>
 */
public class GainEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "gain",
            "Gain",
            "Volume control with optional soft clipping/saturation",
            EffectCategory.GAIN
    );

    // Saturation mode names
    private static final String[] SATURATION_MODES = {
            "None",                // 0: No clipping, signal can exceed -1/+1
            "Hard Clip",           // 1: Simple hard clipping at -1/+1
            "Soft (Tanh)",         // 2: Hyperbolic tangent - very smooth
            "Warm (Arctan)",       // 3: Arctangent - warm character
            "Cubic",               // 4: Polynomial soft clip - transparent
            "Tube",                // 5: Asymmetric tube-style saturation
            "Tape"                 // 6: Tape-style saturation with compression
    };

    private final Parameter gainParam;
    private final Parameter saturationParam;
    private final Parameter kneeParam;
    private final Parameter levelParam;

    public GainEffect() {
        super(METADATA);

        // Gain from -60 dB to +24 dB, default 0 dB (unity)
        gainParam = addFloatParameter("gain", "Gain",
                "Input gain. Boost signal into saturation for more harmonics.",
                -60.0f, 24.0f, 0.0f, "dB");

        // Saturation mode
        saturationParam = addChoiceParameter("saturation", "Saturation",
                "Clipping behavior when signal exceeds -1/+1 range. Soft modes add harmonic warmth.",
                SATURATION_MODES, 0);

        // Knee hardness (0% = soft/wide knee, 100% = hard/tight knee)
        kneeParam = addFloatParameter("knee", "Knee",
                "Controls saturation curve sharpness. Low = gradual transition, High = abrupt transition.",
                0.0f, 100.0f, 50.0f, "%");

        // Output level from -60 dB to 0 dB, default 0 dB (unity)
        levelParam = addFloatParameter("level", "Level",
                "Output level. Reduce after saturation to avoid downstream clipping.",
                -60.0f, 0.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Nothing special to prepare
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float gainLinear = dbToLinear(gainParam.getValue());
        float levelLinear = dbToLinear(levelParam.getValue());
        int satMode = saturationParam.getChoiceIndex();
        float knee = kneeParam.getValue() / 100.0f;  // Normalize to 0-1

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float sample = input[i] * gainLinear;
            sample = applySaturation(sample, satMode, knee);
            output[i] = sample * levelLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float gainLinear = dbToLinear(gainParam.getValue());
        float levelLinear = dbToLinear(levelParam.getValue());
        int satMode = saturationParam.getChoiceIndex();
        float knee = kneeParam.getValue() / 100.0f;  // Normalize to 0-1

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            float sampleL = inputL[i] * gainLinear;
            float sampleR = inputR[i] * gainLinear;
            sampleL = applySaturation(sampleL, satMode, knee);
            sampleR = applySaturation(sampleR, satMode, knee);
            outputL[i] = sampleL * levelLinear;
            outputR[i] = sampleR * levelLinear;
        }
    }

    /**
     * Apply saturation curve to a sample.
     * @param sample Input sample
     * @param mode Saturation mode (0-6)
     * @param knee Knee hardness (0 = soft/wide, 1 = hard/tight)
     */
    private float applySaturation(float sample, int mode, float knee) {
        return switch (mode) {
            case 0 -> sample;                       // None - no clipping
            case 1 -> hardClip(sample);             // Hard Clip
            case 2 -> tanhSoftClip(sample, knee);
            case 3 -> arctanSoftClip(sample, knee);
            case 4 -> cubicSoftClip(sample, knee);
            case 5 -> tubeSaturation(sample, knee);
            case 6 -> tapeSaturation(sample, knee);
            default -> sample;
        };
    }

    /**
     * Hard clipping - simple limit at -1/+1.
     */
    private float hardClip(float x) {
        return Math.max(-1.0f, Math.min(1.0f, x));
    }

    /**
     * Tanh soft clipping - very smooth, symmetric saturation.
     * Knee controls the curve steepness (higher = harder saturation).
     */
    private float tanhSoftClip(float x, float knee) {
        // Drive factor: 0.5 (very soft) to 3.0 (very hard)
        float drive = 0.5f + knee * 2.5f;
        return (float) Math.tanh(x * drive);
    }

    /**
     * Arctangent soft clipping - warm, musical character.
     * Knee controls the curve steepness.
     */
    private float arctanSoftClip(float x, float knee) {
        // Drive factor affects steepness: 0.5 to 4.0
        float drive = 0.5f + knee * 3.5f;
        return (float) (Math.atan(x * drive) * (2.0 / Math.PI));
    }

    /**
     * Cubic soft clipping - transparent, minimal harmonic addition.
     * Knee controls where the soft region starts.
     */
    private float cubicSoftClip(float x, float knee) {
        // Threshold where soft clipping begins: 0.3 (soft) to 0.9 (hard)
        float threshold = 0.3f + knee * 0.6f;

        if (x > 1.0f) {
            return 1.0f;
        } else if (x < -1.0f) {
            return -1.0f;
        } else if (x > threshold) {
            // Soft knee region (positive)
            float range = 1.0f - threshold;
            float t = (x - threshold) / range;
            // Quadratic blend to ceiling
            return threshold + range * t * (2.0f - t);
        } else if (x < -threshold) {
            // Soft knee region (negative)
            float range = 1.0f - threshold;
            float t = (-x - threshold) / range;
            return -threshold - range * t * (2.0f - t);
        } else {
            // Linear region
            return x;
        }
    }

    /**
     * Tube-style saturation - asymmetric, even harmonics.
     * Knee controls the saturation aggressiveness.
     */
    private float tubeSaturation(float x, float knee) {
        // Exponential factor: 0.8 (soft) to 3.0 (hard)
        float factor = 0.8f + knee * 2.2f;

        if (x >= 0) {
            // Positive half: more compression (simulates tube grid limiting)
            float shaped = (float) (1.0 - Math.exp(-x * factor));
            return shaped * 0.95f;
        } else {
            // Negative half: slightly less compression
            float shaped = (float) (-1.0 + Math.exp(x * factor * 0.8f));
            return shaped;
        }
    }

    /**
     * Tape-style saturation - soft compression with subtle odd harmonics.
     * Knee controls the compression threshold.
     */
    private float tapeSaturation(float x, float knee) {
        float sign = Math.signum(x);
        float abs = Math.abs(x);

        // Linear region threshold: 0.7 (soft) to 0.2 (hard)
        float linearThreshold = 0.7f - knee * 0.5f;
        // Saturation drive: 1.0 (soft) to 4.0 (hard)
        float drive = 1.0f + knee * 3.0f;

        if (abs < linearThreshold) {
            // Linear region with slight boost
            return x * (1.0f + knee * 0.15f);
        } else {
            // Saturation region
            float excess = abs - linearThreshold;
            float saturated = linearThreshold + (float) Math.tanh(excess * drive) * (1.0f - linearThreshold);
            return sign * Math.min(saturated, 0.98f);
        }
    }

    /**
     * Set gain in dB.
     */
    public void setGainDb(float dB) {
        gainParam.setValue(dB);
    }

    /**
     * Get current gain in dB.
     */
    public float getGainDb() {
        return gainParam.getValue();
    }

    /**
     * Set saturation mode by index.
     */
    public void setSaturationMode(int mode) {
        saturationParam.setChoice(mode);
    }

    /**
     * Get current saturation mode index.
     */
    public int getSaturationMode() {
        return saturationParam.getChoiceIndex();
    }
}
