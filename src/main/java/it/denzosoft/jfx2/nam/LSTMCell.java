package it.denzosoft.jfx2.nam;

/**
 * LSTM (Long Short-Term Memory) cell.
 *
 * <p>Standard LSTM with input, forget, and output gates:
 * <pre>
 * f_t = sigmoid(W_f * [h_{t-1}, x_t] + b_f)   // Forget gate
 * i_t = sigmoid(W_i * [h_{t-1}, x_t] + b_i)   // Input gate
 * o_t = sigmoid(W_o * [h_{t-1}, x_t] + b_o)   // Output gate
 * c̃_t = tanh(W_c * [h_{t-1}, x_t] + b_c)     // Candidate cell
 * c_t = f_t * c_{t-1} + i_t * c̃_t            // Cell state
 * h_t = o_t * tanh(c_t)                       // Hidden state
 * </pre></p>
 */
public class LSTMCell {

    private final int inputSize;
    private final int hiddenSize;

    // Combined weights for efficiency: [hiddenSize * 4][inputSize + hiddenSize]
    // Gates order: input, forget, cell, output (i, f, c, o)
    private final float[][] weights;
    private final float[] biases;  // [hiddenSize * 4]

    // State
    private final float[] hiddenState;  // h
    private final float[] cellState;    // c

    // Temporary buffers
    private final float[] gates;        // [hiddenSize * 4]
    private final float[] combined;     // [inputSize + hiddenSize]

    public LSTMCell(int inputSize, int hiddenSize) {
        this.inputSize = inputSize;
        this.hiddenSize = hiddenSize;

        int totalInputSize = inputSize + hiddenSize;
        int gateSize = hiddenSize * 4;

        this.weights = new float[gateSize][totalInputSize];
        this.biases = new float[gateSize];

        this.hiddenState = new float[hiddenSize];
        this.cellState = new float[hiddenSize];

        this.gates = new float[gateSize];
        this.combined = new float[totalInputSize];
    }

    /**
     * Process a single time step.
     *
     * @param input Input vector [inputSize]
     * @param output Output vector [hiddenSize] (will be filled with hidden state)
     */
    public void process(float[] input, float[] output) {
        // Combine input and hidden state: [x_t, h_{t-1}]
        System.arraycopy(input, 0, combined, 0, inputSize);
        System.arraycopy(hiddenState, 0, combined, inputSize, hiddenSize);

        // Compute all gates at once
        for (int g = 0; g < hiddenSize * 4; g++) {
            float sum = biases[g];
            for (int i = 0; i < combined.length; i++) {
                sum += weights[g][i] * combined[i];
            }
            gates[g] = sum;
        }

        // Apply activations and compute new states
        for (int i = 0; i < hiddenSize; i++) {
            float inputGate = Activations.sigmoid(gates[i]);                     // i
            float forgetGate = Activations.sigmoid(gates[i + hiddenSize]);       // f
            float cellCandidate = Activations.tanh(gates[i + hiddenSize * 2]);   // c̃
            float outputGate = Activations.sigmoid(gates[i + hiddenSize * 3]);   // o

            // Update cell state
            cellState[i] = forgetGate * cellState[i] + inputGate * cellCandidate;

            // Update hidden state
            hiddenState[i] = outputGate * Activations.tanh(cellState[i]);
        }

        // Output is the hidden state
        System.arraycopy(hiddenState, 0, output, 0, hiddenSize);
    }

    /**
     * Set weights from flat array.
     * NAM format: weights are stored as [4][hiddenSize][inputSize + hiddenSize]
     */
    public int setWeights(float[] flatWeights, int offset) {
        int pos = offset;

        // Weights are stored transposed in NAM format
        // NAM: [4 * hiddenSize][inputSize + hiddenSize]
        for (int g = 0; g < hiddenSize * 4; g++) {
            for (int i = 0; i < inputSize + hiddenSize; i++) {
                weights[g][i] = flatWeights[pos++];
            }
        }

        return pos - offset;
    }

    /**
     * Set biases from flat array.
     */
    public int setBiases(float[] flatBiases, int offset) {
        System.arraycopy(flatBiases, offset, biases, 0, hiddenSize * 4);
        return hiddenSize * 4;
    }

    /**
     * Reset state to zeros.
     */
    public void reset() {
        java.util.Arrays.fill(hiddenState, 0);
        java.util.Arrays.fill(cellState, 0);
    }

    public int getInputSize() {
        return inputSize;
    }

    public int getHiddenSize() {
        return hiddenSize;
    }

    public int getWeightCount() {
        return hiddenSize * 4 * (inputSize + hiddenSize);
    }

    public int getBiasCount() {
        return hiddenSize * 4;
    }
}
