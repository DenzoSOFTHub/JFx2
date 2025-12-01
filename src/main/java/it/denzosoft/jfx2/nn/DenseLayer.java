package it.denzosoft.jfx2.nn;

import java.util.Random;

/**
 * Fully connected (dense) layer for neural network.
 *
 * <p>Implements forward and backward propagation with
 * configurable activation function.</p>
 */
public class DenseLayer {

    private final int inputSize;
    private final int outputSize;
    private final ActivationFunction activation;

    // Weights matrix [outputSize x inputSize]
    private final float[][] weights;
    // Bias vector [outputSize]
    private final float[] biases;

    // Gradients for backpropagation
    private final float[][] weightGradients;
    private final float[] biasGradients;

    // Cached values for backpropagation
    private float[] lastInput;
    private float[] lastPreActivation;
    private float[] lastOutput;

    // Adam optimizer state
    private final float[][] weightM;  // First moment
    private final float[][] weightV;  // Second moment
    private final float[] biasM;
    private final float[] biasV;
    private int adamStep = 0;

    public DenseLayer(int inputSize, int outputSize, ActivationFunction activation) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
        this.activation = activation;

        this.weights = new float[outputSize][inputSize];
        this.biases = new float[outputSize];
        this.weightGradients = new float[outputSize][inputSize];
        this.biasGradients = new float[outputSize];

        // Adam optimizer state
        this.weightM = new float[outputSize][inputSize];
        this.weightV = new float[outputSize][inputSize];
        this.biasM = new float[outputSize];
        this.biasV = new float[outputSize];

        // Initialize with Xavier/Glorot initialization
        initializeWeights();
    }

    /**
     * Initialize weights using Xavier/Glorot initialization.
     * Good for tanh activation, also works reasonably for ReLU.
     */
    private void initializeWeights() {
        Random random = new Random(42);
        float scale = (float) Math.sqrt(2.0 / (inputSize + outputSize));

        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weights[i][j] = (float) (random.nextGaussian() * scale);
            }
            biases[i] = 0.0f;
        }
    }

    /**
     * Forward pass through the layer.
     */
    public float[] forward(float[] input) {
        if (input.length != inputSize) {
            throw new IllegalArgumentException(
                    "Input size mismatch: expected " + inputSize + ", got " + input.length);
        }

        // Cache input for backpropagation
        this.lastInput = input.clone();

        // Compute pre-activation: z = Wx + b
        float[] preActivation = new float[outputSize];
        for (int i = 0; i < outputSize; i++) {
            float sum = biases[i];
            for (int j = 0; j < inputSize; j++) {
                sum += weights[i][j] * input[j];
            }
            preActivation[i] = sum;
        }

        // Cache pre-activation for backpropagation
        this.lastPreActivation = preActivation.clone();

        // Apply activation function
        float[] output = preActivation.clone();
        activation.activate(output);

        // Cache output
        this.lastOutput = output;

        return output;
    }

    /**
     * Backward pass - compute gradients.
     *
     * @param outputGradient Gradient from the next layer (dL/da)
     * @return Gradient to pass to the previous layer (dL/dx)
     */
    public float[] backward(float[] outputGradient) {
        if (outputGradient.length != outputSize) {
            throw new IllegalArgumentException(
                    "Output gradient size mismatch: expected " + outputSize + ", got " + outputGradient.length);
        }

        // Compute delta = dL/dz = dL/da * da/dz
        float[] delta = new float[outputSize];
        float[] activationDerivatives = activation.derivatives(lastPreActivation);
        for (int i = 0; i < outputSize; i++) {
            delta[i] = outputGradient[i] * activationDerivatives[i];
        }

        // Compute weight gradients: dL/dW = delta * input^T
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weightGradients[i][j] += delta[i] * lastInput[j];
            }
            biasGradients[i] += delta[i];
        }

        // Compute input gradient: dL/dx = W^T * delta
        float[] inputGradient = new float[inputSize];
        for (int j = 0; j < inputSize; j++) {
            float sum = 0;
            for (int i = 0; i < outputSize; i++) {
                sum += weights[i][j] * delta[i];
            }
            inputGradient[j] = sum;
        }

        return inputGradient;
    }

    /**
     * Update weights using Adam optimizer.
     *
     * @param learningRate Learning rate
     * @param batchSize Batch size for gradient averaging
     */
    public void updateWeightsAdam(float learningRate, int batchSize) {
        adamStep++;

        final float beta1 = 0.9f;
        final float beta2 = 0.999f;
        final float epsilon = 1e-8f;

        // Bias correction
        float beta1Correction = 1.0f - (float) Math.pow(beta1, adamStep);
        float beta2Correction = 1.0f - (float) Math.pow(beta2, adamStep);

        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                float grad = weightGradients[i][j] / batchSize;

                // Update moments
                weightM[i][j] = beta1 * weightM[i][j] + (1 - beta1) * grad;
                weightV[i][j] = beta2 * weightV[i][j] + (1 - beta2) * grad * grad;

                // Bias-corrected moments
                float mHat = weightM[i][j] / beta1Correction;
                float vHat = weightV[i][j] / beta2Correction;

                // Update weight
                weights[i][j] -= learningRate * mHat / ((float) Math.sqrt(vHat) + epsilon);
            }

            // Update bias
            float biasGrad = biasGradients[i] / batchSize;
            biasM[i] = beta1 * biasM[i] + (1 - beta1) * biasGrad;
            biasV[i] = beta2 * biasV[i] + (1 - beta2) * biasGrad * biasGrad;

            float mHat = biasM[i] / beta1Correction;
            float vHat = biasV[i] / beta2Correction;

            biases[i] -= learningRate * mHat / ((float) Math.sqrt(vHat) + epsilon);
        }

        // Clear gradients
        clearGradients();
    }

    /**
     * Update weights using simple SGD.
     */
    public void updateWeightsSGD(float learningRate, int batchSize) {
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                weights[i][j] -= learningRate * weightGradients[i][j] / batchSize;
            }
            biases[i] -= learningRate * biasGradients[i] / batchSize;
        }

        clearGradients();
    }

    /**
     * Clear accumulated gradients.
     */
    public void clearGradients() {
        for (int i = 0; i < outputSize; i++) {
            java.util.Arrays.fill(weightGradients[i], 0);
        }
        java.util.Arrays.fill(biasGradients, 0);
    }

    // Getters for serialization
    public int getInputSize() { return inputSize; }
    public int getOutputSize() { return outputSize; }
    public ActivationFunction getActivation() { return activation; }
    public float[][] getWeights() { return weights; }
    public float[] getBiases() { return biases; }

    /**
     * Set weights from loaded data.
     */
    public void setWeights(float[][] weights) {
        for (int i = 0; i < outputSize; i++) {
            System.arraycopy(weights[i], 0, this.weights[i], 0, inputSize);
        }
    }

    /**
     * Set biases from loaded data.
     */
    public void setBiases(float[] biases) {
        System.arraycopy(biases, 0, this.biases, 0, outputSize);
    }

    /**
     * Get the sum of squared weights (for L2 regularization).
     */
    public float getWeightSquaredSum() {
        float sum = 0;
        for (int i = 0; i < outputSize; i++) {
            for (int j = 0; j < inputSize; j++) {
                sum += weights[i][j] * weights[i][j];
            }
        }
        return sum;
    }

    /**
     * Get the last output from forward pass.
     */
    public float[] getLastOutput() {
        return lastOutput;
    }
}
