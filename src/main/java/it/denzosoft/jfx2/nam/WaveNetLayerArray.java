package it.denzosoft.jfx2.nam;

/**
 * Array of WaveNet layers with increasing dilation.
 *
 * <p>Typically uses dilations [1, 2, 4, 8, 16, 32] to build
 * a large receptive field with few parameters.</p>
 */
public class WaveNetLayerArray {

    private final WaveNetLayer[] layers;
    private final int channels;
    private final int headChannels;
    private final int receptiveField;

    // Temporary buffers
    private final float[] residualBuffer;
    private final float[] inputBuffer;

    public WaveNetLayerArray(int inputSize, int conditionSize, int headSize,
                             int channels, int kernelSize, int[] dilations,
                             String activation, boolean gated) {
        this.channels = channels;
        this.headChannels = headSize;
        this.layers = new WaveNetLayer[dilations.length];

        int rf = 0;
        for (int i = 0; i < dilations.length; i++) {
            layers[i] = new WaveNetLayer(
                    channels,
                    kernelSize,
                    dilations[i],
                    conditionSize,
                    headSize,
                    gated,
                    activation
            );
            rf += layers[i].getReceptiveField() - 1;
        }
        this.receptiveField = rf + 1;

        this.residualBuffer = new float[channels];
        this.inputBuffer = new float[channels];
    }

    /**
     * Process a single sample through all layers.
     *
     * @param input Input from previous layer array [channels]
     * @param condition Condition signal [conditionSize]
     * @param skipOut Accumulated skip outputs [headChannels]
     * @param output Output for next layer array [channels]
     */
    public void process(float[] input, float[] condition, float[] skipOut, float[] output) {
        // Copy input to working buffer
        System.arraycopy(input, 0, inputBuffer, 0, channels);

        // Process through each layer
        for (WaveNetLayer layer : layers) {
            layer.process(inputBuffer, condition, skipOut, residualBuffer);
            // Residual output becomes input to next layer
            System.arraycopy(residualBuffer, 0, inputBuffer, 0, channels);
        }

        // Final output
        System.arraycopy(inputBuffer, 0, output, 0, channels);
    }

    /**
     * Load weights for all layers.
     * @return total weights consumed
     */
    public int loadWeights(float[] weights, int offset) {
        int pos = offset;
        for (WaveNetLayer layer : layers) {
            pos += layer.loadWeights(weights, pos);
        }
        return pos - offset;
    }

    public void reset() {
        for (WaveNetLayer layer : layers) {
            layer.reset();
        }
    }

    public int getReceptiveField() {
        return receptiveField;
    }

    public int getLayerCount() {
        return layers.length;
    }
}
