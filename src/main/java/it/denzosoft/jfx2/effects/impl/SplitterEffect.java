package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Splitter effect - splits one input into multiple outputs.
 *
 * <p>This is a utility effect that takes a single input and
 * copies it to multiple outputs for parallel processing chains.</p>
 */
public class SplitterEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "splitter",
            "Splitter",
            "Splits signal into multiple parallel paths",
            EffectCategory.UTILITY
    );

    private final Parameter outputCountParam;

    public SplitterEffect() {
        super(METADATA);

        // Number of outputs (2-4)
        outputCountParam = addFloatParameter("outputs", "Outputs", 2.0f, 4.0f, 2.0f, "");
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Nothing special to prepare
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Simple pass-through - the actual splitting is handled by the node connections
        // This effect just copies input to output
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Stereo pass-through
        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));
        System.arraycopy(inputL, 0, outputL, 0, len);
        System.arraycopy(inputR, 0, outputR, 0, len);
    }

    /**
     * Get the number of outputs.
     */
    public int getOutputCount() {
        return (int) outputCountParam.getValue();
    }

    /**
     * Set the number of outputs.
     */
    public void setOutputCount(int count) {
        outputCountParam.setValue(Math.max(2, Math.min(4, count)));
    }
}
