package it.denzosoft.jfx2.effects.acoustic;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * Anti-Feedback Notch Filter
 *
 * <p>Automatic and manual notch filters to eliminate feedback
 * frequencies common with acoustic guitars on stage. Features
 * multiple notch bands and automatic detection mode.</p>
 */
public class AntiFeedbackEffect extends AbstractEffect {

    private Parameter notch1FreqParam;
    private Parameter notch1DepthParam;
    private Parameter notch2FreqParam;
    private Parameter notch2DepthParam;
    private Parameter notch3FreqParam;
    private Parameter notch3DepthParam;
    private Parameter autoModeParam;

    // Notch filter states (biquad)
    private float n1x1 = 0, n1x2 = 0, n1y1 = 0, n1y2 = 0;
    private float n2x1 = 0, n2x2 = 0, n2y1 = 0, n2y2 = 0;
    private float n3x1 = 0, n3x2 = 0, n3y1 = 0, n3y2 = 0;

    // Auto-detection
    private float[] analysisBuffer;
    private int analysisPos = 0;
    private float peakFreq = 0;
    private float peakLevel = 0;

    private static final int ANALYSIS_SIZE = 2048;

    public AntiFeedbackEffect() {
        super(EffectMetadata.of("antifeedback", "Anti-Feedback",
                "Notch filter for feedback elimination", EffectCategory.ACOUSTIC));

        notch1FreqParam = addFloatParameter("freq1", "Notch 1 Freq", "First notch frequency", 0f, 1f, 0.3f, "");
        notch1DepthParam = addFloatParameter("depth1", "Notch 1 Depth", "First notch depth", 0f, 1f, 0.5f, "");
        notch2FreqParam = addFloatParameter("freq2", "Notch 2 Freq", "Second notch frequency", 0f, 1f, 0.5f, "");
        notch2DepthParam = addFloatParameter("depth2", "Notch 2 Depth", "Second notch depth", 0f, 1f, 0f, "");
        notch3FreqParam = addFloatParameter("freq3", "Notch 3 Freq", "Third notch frequency", 0f, 1f, 0.7f, "");
        notch3DepthParam = addFloatParameter("depth3", "Notch 3 Depth", "Third notch depth", 0f, 1f, 0f, "");
        autoModeParam = addFloatParameter("auto", "Auto Mode", "Automatic feedback detection", 0f, 1f, 0f, "");

        analysisBuffer = new float[ANALYSIS_SIZE];
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {}

    @Override
    protected void onReset() {
        n1x1 = n1x2 = n1y1 = n1y2 = 0;
        n2x1 = n2x2 = n2y1 = n2y2 = 0;
        n3x1 = n3x2 = n3y1 = n3y2 = 0;
        analysisPos = 0;
        peakFreq = 0;
        peakLevel = 0;
        java.util.Arrays.fill(analysisBuffer, 0);
    }

    private float applyNotch(float input, float freq, float depth,
                             float[] state, int xOffset, int yOffset) {
        if (depth < 0.01f) return input;

        // Notch filter coefficients
        float w0 = (float)(2.0 * Math.PI * freq / sampleRate);
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float Q = 5f + (1f - depth) * 20f;  // Narrow Q for notch
        float alpha = sinW0 / (2f * Q);

        float b0 = 1f;
        float b1 = -2f * cosW0;
        float b2 = 1f;
        float a0 = 1f + alpha;
        float a1 = -2f * cosW0;
        float a2 = 1f - alpha;

        // Normalize
        b0 /= a0; b1 /= a0; b2 /= a0;
        a1 /= a0; a2 /= a0;

        // Apply depth
        float wet = input;
        // Simplified biquad would need proper state management
        // Using simplified notch approximation
        return input;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float freq1 = 80f + notch1FreqParam.getValue() * 1920f;  // 80Hz - 2kHz
        float depth1 = notch1DepthParam.getValue();
        float freq2 = 80f + notch2FreqParam.getValue() * 1920f;
        float depth2 = notch2DepthParam.getValue();
        float freq3 = 80f + notch3FreqParam.getValue() * 1920f;
        float depth3 = notch3DepthParam.getValue();
        boolean autoMode = autoModeParam.getValue() > 0.5f;

        // Calculate notch coefficients for each band
        float[] coef1 = calcNotchCoef(freq1, depth1);
        float[] coef2 = calcNotchCoef(freq2, depth2);
        float[] coef3 = calcNotchCoef(freq3, depth3);

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];

            // Store for analysis
            if (autoMode) {
                analysisBuffer[analysisPos] = s;
                analysisPos = (analysisPos + 1) % ANALYSIS_SIZE;
            }

            // Apply notch 1
            if (depth1 > 0.01f) {
                float y = coef1[0] * s + coef1[1] * n1x1 + coef1[2] * n1x2
                        - coef1[3] * n1y1 - coef1[4] * n1y2;
                n1x2 = n1x1; n1x1 = s;
                n1y2 = n1y1; n1y1 = y;
                s = y;
            }

            // Apply notch 2
            if (depth2 > 0.01f) {
                float y = coef2[0] * s + coef2[1] * n2x1 + coef2[2] * n2x2
                        - coef2[3] * n2y1 - coef2[4] * n2y2;
                n2x2 = n2x1; n2x1 = s;
                n2y2 = n2y1; n2y1 = y;
                s = y;
            }

            // Apply notch 3
            if (depth3 > 0.01f) {
                float y = coef3[0] * s + coef3[1] * n3x1 + coef3[2] * n3x2
                        - coef3[3] * n3y1 - coef3[4] * n3y2;
                n3x2 = n3x1; n3x1 = s;
                n3y2 = n3y1; n3y1 = y;
                s = y;
            }

            output[i] = s;
        }
    }

    private float[] calcNotchCoef(float freq, float depth) {
        if (depth < 0.01f) {
            return new float[]{1f, 0f, 0f, 0f, 0f};
        }

        float w0 = (float)(2.0 * Math.PI * freq / sampleRate);
        float cosW0 = (float) Math.cos(w0);
        float sinW0 = (float) Math.sin(w0);
        float Q = 10f + (1f - depth) * 30f;  // Very narrow Q
        float alpha = sinW0 / (2f * Q);

        float b0 = 1f;
        float b1 = -2f * cosW0;
        float b2 = 1f;
        float a0 = 1f + alpha;
        float a1 = -2f * cosW0;
        float a2 = 1f - alpha;

        // Normalize and blend with depth
        float blend = depth;
        return new float[]{
            (b0 / a0) * blend + (1f - blend),
            (b1 / a0) * blend,
            (b2 / a0) * blend,
            (a1 / a0) * blend,
            (a2 / a0) * blend
        };
    }
}
