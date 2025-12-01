package it.denzosoft.jfx2.nam;

/**
 * 1x1 Convolution (equivalent to a dense layer per sample).
 *
 * <p>Maps input channels to output channels without temporal context.</p>
 */
public class Conv1x1 {

    private final int inChannels;
    private final int outChannels;
    private final boolean bias;

    // Weights: [outChannels][inChannels]
    private final float[][] weights;
    // Bias: [outChannels]
    private final float[] biases;

    public Conv1x1(int inChannels, int outChannels, boolean bias) {
        this.inChannels = inChannels;
        this.outChannels = outChannels;
        this.bias = bias;

        this.weights = new float[outChannels][inChannels];
        this.biases = bias ? new float[outChannels] : null;
    }

    /**
     * Process a single sample.
     *
     * @param input Input vector [inChannels]
     * @param output Output vector [outChannels] (will be filled)
     */
    public void process(float[] input, float[] output) {
        for (int oc = 0; oc < outChannels; oc++) {
            float sum = bias ? biases[oc] : 0;
            for (int ic = 0; ic < inChannels; ic++) {
                sum += weights[oc][ic] * input[ic];
            }
            output[oc] = sum;
        }
    }

    /**
     * Process and add to existing output.
     */
    public void processAdd(float[] input, float[] output) {
        for (int oc = 0; oc < outChannels; oc++) {
            float sum = bias ? biases[oc] : 0;
            for (int ic = 0; ic < inChannels; ic++) {
                sum += weights[oc][ic] * input[ic];
            }
            output[oc] += sum;
        }
    }

    /**
     * Set weights from flat array.
     */
    public void setWeights(float[] flatWeights, int offset) {
        int idx = offset;
        for (int oc = 0; oc < outChannels; oc++) {
            for (int ic = 0; ic < inChannels; ic++) {
                weights[oc][ic] = flatWeights[idx++];
            }
        }
    }

    /**
     * Set biases from flat array.
     */
    public void setBiases(float[] flatBiases, int offset) {
        if (bias) {
            System.arraycopy(flatBiases, offset, biases, 0, outChannels);
        }
    }

    public int getWeightCount() {
        return outChannels * inChannels;
    }

    public int getBiasCount() {
        return bias ? outChannels : 0;
    }

    public int getOutChannels() {
        return outChannels;
    }

    public int getInChannels() {
        return inChannels;
    }
}
