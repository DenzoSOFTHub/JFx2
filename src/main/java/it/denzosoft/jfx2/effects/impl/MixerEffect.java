package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Mixer effect - mixes multiple inputs into one output.
 *
 * <p>This is a utility effect that takes multiple inputs and
 * combines them into a single output, typically used to merge
 * parallel processing chains back together.</p>
 */
public class MixerEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "mixer",
            "Mixer",
            "Mixes multiple signals into one output",
            EffectCategory.UTILITY
    );

    private final Parameter inputCountParam;
    private final Parameter gainParam;

    public MixerEffect() {
        super(METADATA);

        // Number of inputs (2-4)
        inputCountParam = addFloatParameter("inputs", "Inputs", 2.0f, 4.0f, 2.0f, "");

        // Output gain to compensate for summing
        gainParam = addFloatParameter("gain", "Output Gain", -12.0f, 6.0f, 0.0f, "dB");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Nothing special to prepare
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Simple pass-through with gain - actual mixing is handled by node connections
        float gainDb = gainParam.getValue();
        float gainLinear = dbToLinear(gainDb);

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            output[i] = input[i] * gainLinear;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float gainDb = gainParam.getValue();
        float gainLinear = dbToLinear(gainDb);

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));

        for (int i = 0; i < len; i++) {
            outputL[i] = inputL[i] * gainLinear;
            outputR[i] = inputR[i] * gainLinear;
        }
    }

    /**
     * Get the number of inputs.
     */
    public int getInputCount() {
        return (int) inputCountParam.getValue();
    }

    /**
     * Set the number of inputs.
     */
    public void setInputCount(int count) {
        inputCountParam.setValue(Math.max(2, Math.min(4, count)));
    }

    /**
     * Get output gain in dB.
     */
    public float getGainDb() {
        return gainParam.getValue();
    }

    /**
     * Set output gain in dB.
     */
    public void setGainDb(float dB) {
        gainParam.setValue(dB);
    }
}
