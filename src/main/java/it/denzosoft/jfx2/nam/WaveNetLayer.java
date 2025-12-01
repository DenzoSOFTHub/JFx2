package it.denzosoft.jfx2.nam;

/**
 * Single WaveNet layer with dilated convolution and gated activation.
 *
 * <p>Architecture:
 * <pre>
 *                    input
 *                      │
 *              ┌───────┴───────┐
 *              │               │
 *        DilatedConv      (condition)
 *              │               │
 *              └───────┬───────┘
 *                      │
 *                   (add)
 *                      │
 *              ┌───────┴───────┐ (if gated)
 *              │               │
 *           tanh(z1)     sigmoid(z2)
 *              │               │
 *              └───────*───────┘
 *                      │
 *              ┌───────┴───────┐
 *              │               │
 *           1x1conv         1x1conv
 *           (skip)        (residual)
 *              │               │
 *              ↓               +
 *         skip_out          input
 *                              │
 *                          out (next layer)
 * </pre></p>
 */
public class WaveNetLayer {

    private final int channels;
    private final int headChannels;
    private final boolean gated;
    private final String activation;

    // Dilated convolution: channels -> (gated ? 2*channels : channels)
    private final DilatedConv dilatedConv;

    // Condition mixing: 1 -> (gated ? 2*channels : channels) via 1x1 conv
    private final Conv1x1 conditionConv;

    // Output 1x1 convolutions
    private final Conv1x1 residualConv;  // channels -> channels
    private final Conv1x1 skipConv;      // channels -> headChannels

    // Temporary buffers
    private final float[] convOut;
    private final float[] condOut;
    private final float[] activated;

    public WaveNetLayer(int channels, int kernelSize, int dilation, int conditionSize,
                        int headChannels, boolean gated, String activation) {
        this.channels = channels;
        this.headChannels = headChannels;
        this.gated = gated;
        this.activation = activation;

        int convOutChannels = gated ? channels * 2 : channels;

        this.dilatedConv = new DilatedConv(channels, convOutChannels, kernelSize, dilation, true);
        this.conditionConv = new Conv1x1(conditionSize, convOutChannels, true);
        this.residualConv = new Conv1x1(channels, channels, true);
        this.skipConv = new Conv1x1(channels, headChannels, true);

        this.convOut = new float[convOutChannels];
        this.condOut = new float[convOutChannels];
        this.activated = new float[channels];
    }

    /**
     * Process a single sample.
     *
     * @param input Input [channels]
     * @param condition Condition signal [conditionSize]
     * @param skipOut Skip output to accumulate [headChannels]
     * @param residualOut Residual output [channels]
     */
    public void process(float[] input, float[] condition, float[] skipOut, float[] residualOut) {
        // Dilated convolution
        dilatedConv.process(input, convOut);

        // Add condition
        conditionConv.process(condition, condOut);
        for (int i = 0; i < convOut.length; i++) {
            convOut[i] += condOut[i];
        }

        // Apply activation (gated or standard)
        if (gated) {
            // Split into two halves, apply tanh and sigmoid, multiply
            for (int i = 0; i < channels; i++) {
                float z1 = convOut[i];
                float z2 = convOut[i + channels];
                float a1 = Activations.tanh(z1);
                float a2 = Activations.sigmoid(z2);
                activated[i] = a1 * a2;
            }
        } else {
            // Standard activation
            System.arraycopy(convOut, 0, activated, 0, channels);
            Activations.applyInPlace(activation, activated);
        }

        // Residual connection: 1x1 conv + input
        residualConv.process(activated, residualOut);
        for (int i = 0; i < channels; i++) {
            residualOut[i] += input[i];
        }

        // Skip connection: 1x1 conv, accumulate
        skipConv.processAdd(activated, skipOut);
    }

    /**
     * Load weights from flat array.
     * @return number of weights consumed
     */
    public int loadWeights(float[] weights, int offset) {
        int pos = offset;

        // Dilated conv weights and bias
        dilatedConv.setWeights(weights, pos);
        pos += dilatedConv.getWeightCount();
        dilatedConv.setBiases(weights, pos);
        pos += dilatedConv.getBiasCount();

        // Condition conv weights and bias
        conditionConv.setWeights(weights, pos);
        pos += conditionConv.getWeightCount();
        conditionConv.setBiases(weights, pos);
        pos += conditionConv.getBiasCount();

        // Residual conv weights and bias
        residualConv.setWeights(weights, pos);
        pos += residualConv.getWeightCount();
        residualConv.setBiases(weights, pos);
        pos += residualConv.getBiasCount();

        // Skip conv weights and bias
        skipConv.setWeights(weights, pos);
        pos += skipConv.getWeightCount();
        skipConv.setBiases(weights, pos);
        pos += skipConv.getBiasCount();

        return pos - offset;
    }

    public void reset() {
        dilatedConv.reset();
    }

    public int getReceptiveField() {
        return dilatedConv.getReceptiveField();
    }
}
