package it.denzosoft.jfx2.nam;

import it.denzosoft.jfx2.nam.json.JsonValue;

/**
 * LSTM model for Neural Amp Modeler.
 *
 * <p>Architecture:
 * <pre>
 *      input (1 sample)
 *            │
 *        ┌───┴───┐
 *        │ LSTM  │  inputSize -> hiddenSize
 *        └───┬───┘
 *            │
 *        ┌───┴───┐
 *        │ Dense │  hiddenSize -> 1
 *        └───┬───┘
 *            │
 *         output (1 sample)
 * </pre></p>
 */
public class LSTM implements NAMModel {

    private final int sampleRate;
    private final int inputSize;
    private final int hiddenSize;

    // LSTM cell
    private final LSTMCell lstmCell;

    // Output layer: hiddenSize -> 1
    private final Conv1x1 outputLayer;

    // Buffers
    private final float[] inputBuffer;
    private int inputBufferPos;
    private final float[] lstmInput;
    private final float[] lstmOutput;

    private int samplesProcessed = 0;

    public LSTM(JsonValue config, float[] weights, int sampleRate) {
        this.sampleRate = sampleRate;

        // Parse config
        this.inputSize = config.getInt("input_size", 1);
        this.hiddenSize = config.getInt("num_hidden", 24);

        // Create LSTM cell
        this.lstmCell = new LSTMCell(inputSize, hiddenSize);

        // Create output layer
        this.outputLayer = new Conv1x1(hiddenSize, 1, true);

        // Buffers
        this.inputBuffer = new float[inputSize];
        this.inputBufferPos = 0;
        this.lstmInput = new float[inputSize];
        this.lstmOutput = new float[hiddenSize];

        // Load weights
        loadWeights(weights);
    }

    private void loadWeights(float[] weights) {
        int pos = 0;

        // LSTM weights
        // NAM stores: input-hidden weights, hidden-hidden weights, then biases
        // Combined into our format
        int lstmWeightCount = lstmCell.getWeightCount();
        int lstmBiasCount = lstmCell.getBiasCount();

        // In NAM format, LSTM weights are stored differently
        // We need to rearrange them to match our format
        // NAM: weight_ih [4*hiddenSize, inputSize], weight_hh [4*hiddenSize, hiddenSize], bias [4*hiddenSize]

        // For now, assume weights are already in the right order
        // This may need adjustment based on actual NAM file structure
        pos += lstmCell.setWeights(weights, pos);
        pos += lstmCell.setBiases(weights, pos);

        // Output layer weights and bias
        outputLayer.setWeights(weights, pos);
        pos += outputLayer.getWeightCount();
        outputLayer.setBiases(weights, pos);
        pos += outputLayer.getBiasCount();
    }

    @Override
    public float process(float sample) {
        // Add sample to input buffer
        inputBuffer[inputBufferPos] = sample;
        inputBufferPos = (inputBufferPos + 1) % inputSize;

        // Build LSTM input (for inputSize > 1, this provides context)
        for (int i = 0; i < inputSize; i++) {
            int idx = (inputBufferPos + i) % inputSize;
            lstmInput[i] = inputBuffer[idx];
        }

        // Process through LSTM
        lstmCell.process(lstmInput, lstmOutput);

        // Output layer
        float[] output = new float[1];
        outputLayer.process(lstmOutput, output);

        samplesProcessed++;
        return output[0];
    }

    @Override
    public void process(float[] input, float[] output, int numSamples) {
        for (int i = 0; i < numSamples; i++) {
            output[i] = process(input[i]);
        }
    }

    @Override
    public void reset() {
        lstmCell.reset();
        java.util.Arrays.fill(inputBuffer, 0);
        inputBufferPos = 0;
        samplesProcessed = 0;
    }

    @Override
    public int getReceptiveField() {
        // LSTM theoretically has infinite receptive field due to recurrence
        // Return input size as practical value
        return inputSize;
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public String getArchitecture() {
        return "LSTM";
    }

    @Override
    public boolean isPrewarmed() {
        return samplesProcessed >= inputSize;
    }

    /**
     * Get a summary of the model.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("LSTM Model:\n");
        sb.append("  Input size: ").append(inputSize).append("\n");
        sb.append("  Hidden size: ").append(hiddenSize).append("\n");
        sb.append("  Sample rate: ").append(sampleRate).append(" Hz\n");
        return sb.toString();
    }
}
