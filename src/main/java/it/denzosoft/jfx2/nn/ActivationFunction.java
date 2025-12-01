package it.denzosoft.jfx2.nn;

/**
 * Activation functions for neural network layers.
 */
public enum ActivationFunction {

    LINEAR {
        @Override
        public float activate(float x) {
            return x;
        }

        @Override
        public float derivative(float x) {
            return 1.0f;
        }
    },

    RELU {
        @Override
        public float activate(float x) {
            return x > 0 ? x : 0;
        }

        @Override
        public float derivative(float x) {
            return x > 0 ? 1.0f : 0.0f;
        }
    },

    LEAKY_RELU {
        private static final float ALPHA = 0.01f;

        @Override
        public float activate(float x) {
            return x > 0 ? x : ALPHA * x;
        }

        @Override
        public float derivative(float x) {
            return x > 0 ? 1.0f : ALPHA;
        }
    },

    TANH {
        @Override
        public float activate(float x) {
            return (float) Math.tanh(x);
        }

        @Override
        public float derivative(float x) {
            float tanh = (float) Math.tanh(x);
            return 1.0f - tanh * tanh;
        }
    },

    SIGMOID {
        @Override
        public float activate(float x) {
            return 1.0f / (1.0f + (float) Math.exp(-x));
        }

        @Override
        public float derivative(float x) {
            float sig = activate(x);
            return sig * (1.0f - sig);
        }
    },

    /**
     * Softplus - smooth approximation of ReLU.
     * Good for audio processing as it's continuously differentiable.
     */
    SOFTPLUS {
        @Override
        public float activate(float x) {
            return (float) Math.log(1.0 + Math.exp(x));
        }

        @Override
        public float derivative(float x) {
            return 1.0f / (1.0f + (float) Math.exp(-x));
        }
    };

    /**
     * Apply the activation function.
     */
    public abstract float activate(float x);

    /**
     * Compute the derivative of the activation function.
     */
    public abstract float derivative(float x);

    /**
     * Apply activation to an entire array in-place.
     */
    public void activate(float[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] = activate(values[i]);
        }
    }

    /**
     * Compute derivatives for an entire array.
     */
    public float[] derivatives(float[] preActivations) {
        float[] result = new float[preActivations.length];
        for (int i = 0; i < preActivations.length; i++) {
            result[i] = derivative(preActivations[i]);
        }
        return result;
    }
}
