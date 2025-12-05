package it.denzosoft.jfx2.effects.acoustic;

import it.denzosoft.jfx2.effects.AbstractEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.EffectMetadata;
import it.denzosoft.jfx2.effects.Parameter;

/**
 * Acoustic Compressor
 *
 * <p>Compressor optimized for acoustic guitar. Gentle ratio,
 * program-dependent attack/release, and transparent character
 * that preserves the natural dynamics of acoustic playing.</p>
 */
public class AcousticCompressorEffect extends AbstractEffect {

    private Parameter thresholdParam;
    private Parameter ratioParam;
    private Parameter attackParam;
    private Parameter releaseParam;
    private Parameter makeupParam;
    private Parameter blendParam;  // Parallel compression

    private float envState = 0;
    private float gainState = 1f;

    public AcousticCompressorEffect() {
        super(EffectMetadata.of("acousticcomp", "Acoustic Compressor",
                "Transparent compression for acoustic guitar", EffectCategory.ACOUSTIC));

        thresholdParam = addFloatParameter("threshold", "Threshold", "Compression threshold", 0f, 1f, 0.5f, "");
        ratioParam = addFloatParameter("ratio", "Ratio", "Compression ratio", 0f, 1f, 0.3f, "");
        attackParam = addFloatParameter("attack", "Attack", "Attack time", 0f, 1f, 0.3f, "");
        releaseParam = addFloatParameter("release", "Release", "Release time", 0f, 1f, 0.5f, "");
        makeupParam = addFloatParameter("makeup", "Makeup", "Makeup gain", 0f, 1f, 0.5f, "");
        blendParam = addFloatParameter("blend", "Blend", "Dry/compressed blend", 0f, 1f, 0.7f, "");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {}

    @Override
    protected void onReset() {
        envState = 0;
        gainState = 1f;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float threshold = 0.1f + (1f - thresholdParam.getValue()) * 0.8f;  // -20dB to -2dB
        float ratio = 1.5f + ratioParam.getValue() * 3.5f;  // 1.5:1 to 5:1 (gentle for acoustic)
        float attackMs = 5f + attackParam.getValue() * 45f;  // 5-50ms
        float releaseMs = 50f + releaseParam.getValue() * 450f;  // 50-500ms
        float makeup = 1f + makeupParam.getValue() * 2f;  // 0-6dB
        float blend = blendParam.getValue();

        float attackCoef = (float) Math.exp(-1.0 / (sampleRate * attackMs / 1000f));
        float releaseCoef = (float) Math.exp(-1.0 / (sampleRate * releaseMs / 1000f));

        for (int i = 0; i < frameCount; i++) {
            float s = input[i];
            float dry = s;

            // Envelope detection (peak)
            float envelope = Math.abs(s);

            // Smooth envelope
            if (envelope > envState) {
                envState = envState * attackCoef + envelope * (1f - attackCoef);
            } else {
                envState = envState * releaseCoef + envelope * (1f - releaseCoef);
            }

            // Calculate gain reduction
            float targetGain = 1f;
            if (envState > threshold) {
                float overThreshold = envState / threshold;
                float compressedLevel = (float) Math.pow(overThreshold, 1f / ratio - 1f);
                targetGain = compressedLevel;
            }

            // Smooth gain changes
            if (targetGain < gainState) {
                gainState = gainState * attackCoef + targetGain * (1f - attackCoef);
            } else {
                gainState = gainState * releaseCoef + targetGain * (1f - releaseCoef);
            }

            // Apply compression
            float compressed = s * gainState * makeup;

            // Soft knee at extremes
            if (Math.abs(compressed) > 0.9f) {
                compressed = (float) Math.tanh(compressed) * 0.95f;
            }

            // Parallel compression blend
            output[i] = dry * (1f - blend) + compressed * blend;
        }
    }
}
