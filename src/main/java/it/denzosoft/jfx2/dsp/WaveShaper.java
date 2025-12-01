package it.denzosoft.jfx2.dsp;

/**
 * Wave shaping functions for distortion effects.
 *
 * <p>Provides various clipping/saturation algorithms.</p>
 */
public class WaveShaper {

    /**
     * Soft clipping using hyperbolic tangent (tanh).
     * Classic warm overdrive sound.
     *
     * @param input Input sample
     * @param drive Drive amount (1.0 = unity, higher = more saturation)
     * @return Shaped output
     */
    public static float tanhClip(float input, float drive) {
        return (float) Math.tanh(input * drive);
    }

    /**
     * Hard clipping - simple digital clipping.
     *
     * @param input     Input sample
     * @param threshold Clipping threshold (0.0 to 1.0)
     * @return Clipped output
     */
    public static float hardClip(float input, float threshold) {
        if (input > threshold) return threshold;
        if (input < -threshold) return -threshold;
        return input;
    }

    /**
     * Soft clipping using cubic function.
     * Softer than tanh, good for mild overdrive.
     *
     * @param input Input sample (-1 to 1 range expected)
     * @return Shaped output
     */
    public static float cubicClip(float input) {
        if (input > 1.0f) return 2.0f / 3.0f;
        if (input < -1.0f) return -2.0f / 3.0f;
        return input - (input * input * input) / 3.0f;
    }

    /**
     * Asymmetric soft clipping (tube-like).
     * Different saturation on positive and negative half-waves.
     *
     * @param input Input sample
     * @param drive Drive amount
     * @return Shaped output with even harmonics
     */
    public static float asymmetricClip(float input, float drive) {
        float driven = input * drive;
        if (driven >= 0) {
            return (float) Math.tanh(driven);
        } else {
            // Softer clipping on negative side
            return (float) Math.tanh(driven * 0.7f);
        }
    }

    /**
     * Foldback distortion.
     * When signal exceeds threshold, it folds back on itself.
     *
     * @param input     Input sample
     * @param threshold Fold threshold
     * @return Folded output
     */
    public static float foldback(float input, float threshold) {
        if (threshold <= 0) return 0;

        while (input > threshold || input < -threshold) {
            if (input > threshold) {
                input = 2.0f * threshold - input;
            } else if (input < -threshold) {
                input = -2.0f * threshold - input;
            }
        }
        return input;
    }

    /**
     * Exponential soft clipping.
     * Very smooth saturation curve.
     *
     * @param input Input sample
     * @param drive Drive amount
     * @return Shaped output
     */
    public static float exponentialClip(float input, float drive) {
        float driven = input * drive;
        if (driven >= 0) {
            return 1.0f - (float) Math.exp(-driven);
        } else {
            return -1.0f + (float) Math.exp(driven);
        }
    }

    /**
     * Arctangent soft clipping.
     * Similar to tanh but with different harmonic content.
     *
     * @param input Input sample
     * @param drive Drive amount
     * @return Shaped output
     */
    public static float atanClip(float input, float drive) {
        return (float) (Math.atan(input * drive) * (2.0 / Math.PI));
    }

    /**
     * Sine wave shaping.
     * Creates rich harmonics with a distinctive character.
     *
     * @param input Input sample
     * @param drive Drive amount (affects harmonic content)
     * @return Shaped output
     */
    public static float sineShape(float input, float drive) {
        return (float) Math.sin(input * drive * Math.PI / 2.0);
    }

    /**
     * Bit crusher - reduces bit depth for lo-fi distortion.
     *
     * @param input Input sample
     * @param bits  Effective bit depth (1-16)
     * @return Quantized output
     */
    public static float bitCrush(float input, int bits) {
        if (bits >= 16) return input;
        if (bits < 1) bits = 1;

        float levels = (float) Math.pow(2, bits) - 1;
        return Math.round(input * levels) / levels;
    }

    /**
     * Mix dry and wet signals with drive compensation.
     * Higher drive typically reduces output level, this compensates.
     *
     * @param dry      Original signal
     * @param wet      Processed signal
     * @param mix      Mix amount (0.0 = dry, 1.0 = wet)
     * @param drive    Drive amount (for output compensation)
     * @param maxDrive Maximum expected drive value
     * @return Mixed output
     */
    public static float mixWithCompensation(float dry, float wet, float mix, float drive, float maxDrive) {
        // Simple compensation curve
        float compensation = 1.0f + (drive / maxDrive) * 0.5f;
        return (1.0f - mix) * dry + mix * wet * compensation;
    }
}
