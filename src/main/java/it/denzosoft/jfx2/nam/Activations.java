package it.denzosoft.jfx2.nam;

/**
 * Activation functions for NAM neural networks.
 */
public final class Activations {

    private Activations() {}

    /**
     * ReLU activation: max(0, x)
     */
    public static float relu(float x) {
        return x > 0 ? x : 0;
    }

    /**
     * Tanh activation
     */
    public static float tanh(float x) {
        return (float) Math.tanh(x);
    }

    /**
     * Sigmoid activation: 1 / (1 + exp(-x))
     */
    public static float sigmoid(float x) {
        return 1.0f / (1.0f + (float) Math.exp(-x));
    }

    /**
     * Fast tanh approximation (Pade approximant)
     */
    public static float fastTanh(float x) {
        if (x < -3) return -1;
        if (x > 3) return 1;
        float x2 = x * x;
        return x * (27 + x2) / (27 + 9 * x2);
    }

    /**
     * Fast sigmoid approximation
     */
    public static float fastSigmoid(float x) {
        return 0.5f * (fastTanh(x * 0.5f) + 1.0f);
    }

    /**
     * Apply activation by name
     */
    public static float apply(String name, float x) {
        return switch (name.toLowerCase()) {
            case "relu" -> relu(x);
            case "tanh" -> tanh(x);
            case "sigmoid" -> sigmoid(x);
            default -> x; // Linear
        };
    }

    /**
     * Apply activation to array in-place
     */
    public static void applyInPlace(String name, float[] arr) {
        switch (name.toLowerCase()) {
            case "relu" -> {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = relu(arr[i]);
                }
            }
            case "tanh" -> {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = tanh(arr[i]);
                }
            }
            case "sigmoid" -> {
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = sigmoid(arr[i]);
                }
            }
            // Linear: do nothing
        }
    }
}
