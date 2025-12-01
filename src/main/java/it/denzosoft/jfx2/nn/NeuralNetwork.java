package it.denzosoft.jfx2.nn;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Feedforward neural network for audio processing.
 *
 * <p>Designed for learning audio transformations from dry to wet signals.
 * Uses a sliding window of input samples to predict output samples.</p>
 */
public class NeuralNetwork {

    private final List<DenseLayer> layers;
    private final int inputWindowSize;
    private final int outputSize;

    // Training state
    private float learningRate = 0.001f;
    private int batchSize = 32;
    private boolean useAdam = true;

    /**
     * Create a neural network with specified architecture.
     *
     * @param inputWindowSize Number of input samples in the sliding window
     * @param hiddenSizes Array of hidden layer sizes
     * @param outputSize Number of output samples (typically 1)
     * @param hiddenActivation Activation function for hidden layers
     */
    public NeuralNetwork(int inputWindowSize, int[] hiddenSizes, int outputSize,
                         ActivationFunction hiddenActivation) {
        this.inputWindowSize = inputWindowSize;
        this.outputSize = outputSize;
        this.layers = new ArrayList<>();

        int prevSize = inputWindowSize;

        // Create hidden layers
        for (int hiddenSize : hiddenSizes) {
            layers.add(new DenseLayer(prevSize, hiddenSize, hiddenActivation));
            prevSize = hiddenSize;
        }

        // Output layer with linear activation (for regression)
        layers.add(new DenseLayer(prevSize, outputSize, ActivationFunction.LINEAR));
    }

    /**
     * Create a network with default architecture suitable for amp modeling.
     */
    public static NeuralNetwork createDefault() {
        // Input: 64 samples context
        // Hidden: 32 -> 16 -> 8 neurons with tanh
        // Output: 1 sample
        return new NeuralNetwork(64, new int[]{32, 16, 8}, 1, ActivationFunction.TANH);
    }

    /**
     * Create a larger network for more complex transformations.
     */
    public static NeuralNetwork createLarge() {
        // Input: 128 samples context
        // Hidden: 64 -> 32 -> 16 neurons with tanh
        // Output: 1 sample
        return new NeuralNetwork(128, new int[]{64, 32, 16}, 1, ActivationFunction.TANH);
    }

    /**
     * Forward pass through the network.
     *
     * @param input Input window of samples
     * @return Output samples
     */
    public float[] forward(float[] input) {
        float[] current = input;
        for (DenseLayer layer : layers) {
            current = layer.forward(current);
        }
        return current;
    }

    /**
     * Process a single sample using a sliding window.
     * Maintains internal state for the window.
     *
     * @param inputWindow The current input window (must be inputWindowSize)
     * @return The predicted output sample
     */
    public float processSample(float[] inputWindow) {
        float[] output = forward(inputWindow);
        return output[0];
    }

    /**
     * Backward pass - compute gradients and accumulate.
     *
     * @param target Target output values
     * @return Mean squared error for this sample
     */
    public float backward(float[] target) {
        // Get the output from the last forward pass
        DenseLayer outputLayer = layers.get(layers.size() - 1);
        float[] output = outputLayer.getLastOutput();

        // Compute output gradient (MSE loss derivative)
        float[] outputGradient = new float[outputSize];
        float mse = 0;
        for (int i = 0; i < outputSize; i++) {
            float error = output[i] - target[i];
            outputGradient[i] = 2.0f * error / outputSize;  // d(MSE)/d(output)
            mse += error * error;
        }
        mse /= outputSize;

        // Backpropagate through layers
        float[] gradient = outputGradient;
        for (int i = layers.size() - 1; i >= 0; i--) {
            gradient = layers.get(i).backward(gradient);
        }

        return mse;
    }

    /**
     * Update weights after accumulating gradients.
     */
    public void updateWeights() {
        for (DenseLayer layer : layers) {
            if (useAdam) {
                layer.updateWeightsAdam(learningRate, batchSize);
            } else {
                layer.updateWeightsSGD(learningRate, batchSize);
            }
        }
    }

    /**
     * Clear all accumulated gradients.
     */
    public void clearGradients() {
        for (DenseLayer layer : layers) {
            layer.clearGradients();
        }
    }

    /**
     * Train on a single input-target pair.
     *
     * @param input Input window
     * @param target Target output
     * @return MSE for this sample
     */
    public float trainStep(float[] input, float[] target) {
        forward(input);
        return backward(target);
    }

    // Getters and setters
    public int getInputWindowSize() { return inputWindowSize; }
    public int getOutputSize() { return outputSize; }
    public float getLearningRate() { return learningRate; }
    public void setLearningRate(float learningRate) { this.learningRate = learningRate; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public void setUseAdam(boolean useAdam) { this.useAdam = useAdam; }
    public List<DenseLayer> getLayers() { return layers; }

    /**
     * Get total number of trainable parameters.
     */
    public int getParameterCount() {
        int count = 0;
        for (DenseLayer layer : layers) {
            count += layer.getInputSize() * layer.getOutputSize();  // weights
            count += layer.getOutputSize();  // biases
        }
        return count;
    }

    /**
     * Save network to file.
     */
    public void save(Path filePath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            // Header
            writer.write("JFXNN1\n");  // Magic number + version

            // Architecture
            writer.write(inputWindowSize + "\n");
            writer.write(outputSize + "\n");
            writer.write(layers.size() + "\n");

            // Each layer
            for (DenseLayer layer : layers) {
                writer.write(layer.getInputSize() + "," + layer.getOutputSize() + "," +
                        layer.getActivation().name() + "\n");

                // Weights
                float[][] weights = layer.getWeights();
                for (int i = 0; i < layer.getOutputSize(); i++) {
                    StringBuilder sb = new StringBuilder();
                    for (int j = 0; j < layer.getInputSize(); j++) {
                        if (j > 0) sb.append(",");
                        sb.append(weights[i][j]);
                    }
                    writer.write(sb.toString() + "\n");
                }

                // Biases
                float[] biases = layer.getBiases();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < layer.getOutputSize(); i++) {
                    if (i > 0) sb.append(",");
                    sb.append(biases[i]);
                }
                writer.write(sb.toString() + "\n");
            }
        }
    }

    /**
     * Load network from file.
     */
    public static NeuralNetwork load(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            // Check magic
            String magic = reader.readLine();
            if (!"JFXNN1".equals(magic)) {
                throw new IOException("Invalid neural network file format");
            }

            // Read architecture
            int inputWindowSize = Integer.parseInt(reader.readLine().trim());
            int outputSize = Integer.parseInt(reader.readLine().trim());
            int numLayers = Integer.parseInt(reader.readLine().trim());

            // Read layer configs
            List<int[]> layerSizes = new ArrayList<>();
            List<ActivationFunction> activations = new ArrayList<>();

            for (int l = 0; l < numLayers; l++) {
                String[] parts = reader.readLine().split(",");
                int inSize = Integer.parseInt(parts[0].trim());
                int outSize = Integer.parseInt(parts[1].trim());
                ActivationFunction act = ActivationFunction.valueOf(parts[2].trim());

                layerSizes.add(new int[]{inSize, outSize});
                activations.add(act);

                // Skip weights and biases for now (we'll read them after creating network)
                for (int i = 0; i < outSize; i++) {
                    reader.readLine();  // weight row
                }
                reader.readLine();  // biases
            }

            // Reconstruct hidden sizes
            int[] hiddenSizes = new int[numLayers - 1];
            ActivationFunction hiddenActivation = ActivationFunction.TANH;
            for (int i = 0; i < numLayers - 1; i++) {
                hiddenSizes[i] = layerSizes.get(i)[1];
                hiddenActivation = activations.get(i);
            }

            // Create network
            NeuralNetwork network = new NeuralNetwork(inputWindowSize, hiddenSizes, outputSize, hiddenActivation);

            // Now re-read the file to load weights
            try (BufferedReader reader2 = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                reader2.readLine();  // magic
                reader2.readLine();  // inputWindowSize
                reader2.readLine();  // outputSize
                reader2.readLine();  // numLayers

                for (int l = 0; l < numLayers; l++) {
                    DenseLayer layer = network.layers.get(l);
                    reader2.readLine();  // layer config

                    // Read weights
                    float[][] weights = new float[layer.getOutputSize()][layer.getInputSize()];
                    for (int i = 0; i < layer.getOutputSize(); i++) {
                        String[] values = reader2.readLine().split(",");
                        for (int j = 0; j < layer.getInputSize(); j++) {
                            weights[i][j] = Float.parseFloat(values[j].trim());
                        }
                    }
                    layer.setWeights(weights);

                    // Read biases
                    String[] biasValues = reader2.readLine().split(",");
                    float[] biases = new float[layer.getOutputSize()];
                    for (int i = 0; i < layer.getOutputSize(); i++) {
                        biases[i] = Float.parseFloat(biasValues[i].trim());
                    }
                    layer.setBiases(biases);
                }
            }

            return network;
        }
    }

    /**
     * Get a summary string of the network architecture.
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Neural Network Summary:\n");
        sb.append("  Input window: ").append(inputWindowSize).append(" samples\n");
        sb.append("  Output: ").append(outputSize).append(" sample(s)\n");
        sb.append("  Layers:\n");

        for (int i = 0; i < layers.size(); i++) {
            DenseLayer layer = layers.get(i);
            sb.append(String.format("    [%d] %d -> %d (%s)\n",
                    i, layer.getInputSize(), layer.getOutputSize(),
                    layer.getActivation().name()));
        }

        sb.append("  Total parameters: ").append(getParameterCount()).append("\n");
        return sb.toString();
    }
}
