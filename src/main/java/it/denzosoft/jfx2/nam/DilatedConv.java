package it.denzosoft.jfx2.nam;

/**
 * 1D Dilated Causal Convolution.
 *
 * <p>Implements dilated convolution for WaveNet, where the kernel
 * samples are spaced apart by the dilation factor.</p>
 *
 * <p>For kernel size 3 and dilation 4:
 * <pre>
 * output[t] = sum(weight[k] * input[t - k*dilation]) for k in [0, kernel_size)
 *           = w[0]*x[t] + w[1]*x[t-4] + w[2]*x[t-8]
 * </pre></p>
 */
public class DilatedConv {

    private final int inChannels;
    private final int outChannels;
    private final int kernelSize;
    private final int dilation;
    private final boolean bias;

    // Weights: [outChannels][inChannels][kernelSize]
    private final float[][][] weights;
    // Bias: [outChannels]
    private final float[] biases;

    // Input buffer for causal convolution
    private final float[][] buffer;  // [inChannels][bufferSize]
    private final int bufferSize;
    private int bufferPos;

    // Receptive field (number of past samples needed)
    private final int receptiveField;

    public DilatedConv(int inChannels, int outChannels, int kernelSize, int dilation, boolean bias) {
        this.inChannels = inChannels;
        this.outChannels = outChannels;
        this.kernelSize = kernelSize;
        this.dilation = dilation;
        this.bias = bias;

        this.weights = new float[outChannels][inChannels][kernelSize];
        this.biases = bias ? new float[outChannels] : null;

        // Receptive field = (kernelSize - 1) * dilation + 1
        this.receptiveField = (kernelSize - 1) * dilation + 1;

        // Buffer needs to hold receptive field worth of samples
        this.bufferSize = receptiveField;
        this.buffer = new float[inChannels][bufferSize];
        this.bufferPos = 0;
    }

    /**
     * Process a single sample.
     *
     * @param input Input vector [inChannels]
     * @param output Output vector [outChannels] (will be filled)
     */
    public void process(float[] input, float[] output) {
        // Store input in buffer
        for (int c = 0; c < inChannels; c++) {
            buffer[c][bufferPos] = input[c];
        }

        // Compute convolution
        for (int oc = 0; oc < outChannels; oc++) {
            float sum = bias ? biases[oc] : 0;

            for (int ic = 0; ic < inChannels; ic++) {
                for (int k = 0; k < kernelSize; k++) {
                    // Index into past: current - k*dilation
                    int idx = (bufferPos - k * dilation + bufferSize) % bufferSize;
                    sum += weights[oc][ic][k] * buffer[ic][idx];
                }
            }

            output[oc] = sum;
        }

        // Advance buffer position
        bufferPos = (bufferPos + 1) % bufferSize;
    }

    /**
     * Set weights from flat array.
     * Order: [out][in][kernel] flattened
     */
    public void setWeights(float[] flatWeights, int offset) {
        int idx = offset;
        for (int oc = 0; oc < outChannels; oc++) {
            for (int ic = 0; ic < inChannels; ic++) {
                for (int k = 0; k < kernelSize; k++) {
                    weights[oc][ic][k] = flatWeights[idx++];
                }
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

    /**
     * Reset internal buffers.
     */
    public void reset() {
        for (int c = 0; c < inChannels; c++) {
            java.util.Arrays.fill(buffer[c], 0);
        }
        bufferPos = 0;
    }

    /**
     * Get number of weight parameters.
     */
    public int getWeightCount() {
        return outChannels * inChannels * kernelSize;
    }

    /**
     * Get number of bias parameters.
     */
    public int getBiasCount() {
        return bias ? outChannels : 0;
    }

    public int getReceptiveField() {
        return receptiveField;
    }

    public int getOutChannels() {
        return outChannels;
    }

    public int getInChannels() {
        return inChannels;
    }
}
