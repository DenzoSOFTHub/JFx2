package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;
import it.denzosoft.jfx2.nn.NeuralNetwork;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Neural Amp Modeling effect.
 *
 * <p>Uses a trained neural network to process audio, emulating
 * the characteristics of amplifiers, pedals, or other audio
 * transformations learned from dry/wet audio pairs.</p>
 *
 * <p>The neural network model must be trained separately using
 * the NeuralNetworkTrainer tool and saved to a .jfxnn file.</p>
 */
public class NeuralAmpEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "neuralamp",
            "Neural Amp",
            "AI-powered amp/effect modeling from trained neural network",
            EffectCategory.AMP_SIM
    );

    // Parameters
    private final Parameter inputGainParam;
    private final Parameter outputGainParam;
    private final Parameter mixParam;

    // Neural network
    private NeuralNetwork network;
    private boolean networkLoaded = false;
    private String currentModelPath;

    // Input buffer for sliding window
    private float[] inputBuffer;
    private int inputBufferPos;
    private int windowSize;

    public NeuralAmpEffect() {
        super(METADATA);

        // Input gain: -12 to +12 dB
        inputGainParam = addFloatParameter("inputGain", "Input Gain",
                "Adjust input level before neural network processing",
                -12.0f, 12.0f, 0.0f, "dB");

        // Output gain: -12 to +12 dB
        outputGainParam = addFloatParameter("outputGain", "Output Gain",
                "Adjust output level after neural network processing",
                -12.0f, 12.0f, 0.0f, "dB");

        // Mix: 0-100%
        mixParam = addFloatParameter("mix", "Mix",
                "Blend between dry and processed signal",
                0.0f, 100.0f, 100.0f, "%");
    }

    /**
     * Load a trained neural network model.
     *
     * @param modelPath Path to the .jfxnn model file
     * @return true if loaded successfully
     */
    public boolean loadModel(String modelPath) {
        return loadModel(Path.of(modelPath));
    }

    /**
     * Load a trained neural network model.
     *
     * @param modelPath Path to the .jfxnn model file
     * @return true if loaded successfully
     */
    public boolean loadModel(Path modelPath) {
        try {
            network = NeuralNetwork.load(modelPath);
            windowSize = network.getInputWindowSize();
            currentModelPath = modelPath.toString();
            networkLoaded = true;

            System.out.println("Neural Amp: Loaded model from " + modelPath);
            System.out.println(network.getSummary());

            // Reinitialize buffer if already prepared
            if (inputBuffer != null) {
                inputBuffer = new float[windowSize];
                inputBufferPos = 0;
            }

            return true;
        } catch (IOException e) {
            System.err.println("Neural Amp: Failed to load model: " + e.getMessage());
            networkLoaded = false;
            return false;
        }
    }

    /**
     * Check if a model is loaded.
     */
    public boolean isModelLoaded() {
        return networkLoaded;
    }

    /**
     * Get the current model path.
     */
    public String getModelPath() {
        return currentModelPath;
    }

    /**
     * Get the loaded network (for inspection).
     */
    public NeuralNetwork getNetwork() {
        return network;
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Initialize input buffer based on network window size
        if (networkLoaded && network != null) {
            windowSize = network.getInputWindowSize();
        } else {
            windowSize = 64;  // Default
        }

        inputBuffer = new float[windowSize];
        inputBufferPos = 0;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float inputGainLinear = dbToLinear(inputGainParam.getValue());
        float outputGainLinear = dbToLinear(outputGainParam.getValue());
        float mix = mixParam.getValue() / 100.0f;

        // If no model loaded, pass through with gain
        if (!networkLoaded || network == null) {
            for (int i = 0; i < frameCount && i < output.length; i++) {
                float dry = input[i];
                output[i] = dry * outputGainLinear;
            }
            return;
        }

        // Process each sample
        float[] windowBuffer = new float[windowSize];

        for (int i = 0; i < frameCount && i < output.length && i < input.length; i++) {
            float dry = input[i];
            float scaledInput = dry * inputGainLinear;

            // Add to circular buffer
            inputBuffer[inputBufferPos] = scaledInput;

            // Build window for network (oldest to newest)
            for (int j = 0; j < windowSize; j++) {
                int bufIdx = (inputBufferPos + 1 + j) % windowSize;
                windowBuffer[j] = inputBuffer[bufIdx];
            }

            // Advance buffer position
            inputBufferPos = (inputBufferPos + 1) % windowSize;

            // Process through neural network
            float wet = network.processSample(windowBuffer);

            // Apply output gain and mix
            wet *= outputGainLinear;
            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float inputGainLinear = dbToLinear(inputGainParam.getValue());
        float outputGainLinear = dbToLinear(outputGainParam.getValue());
        float mix = mixParam.getValue() / 100.0f;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));

        // If no model loaded, pass through with gain
        if (!networkLoaded || network == null) {
            for (int i = 0; i < len; i++) {
                outputL[i] = inputL[i] * outputGainLinear;
                outputR[i] = inputR[i] * outputGainLinear;
            }
            return;
        }

        // Process mono (mix L+R) through network, then split back to stereo
        float[] windowBuffer = new float[windowSize];

        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Mix to mono for processing
            float dryMono = (dryL + dryR) * 0.5f;
            float scaledInput = dryMono * inputGainLinear;

            // Add to circular buffer
            inputBuffer[inputBufferPos] = scaledInput;

            // Build window for network
            for (int j = 0; j < windowSize; j++) {
                int bufIdx = (inputBufferPos + 1 + j) % windowSize;
                windowBuffer[j] = inputBuffer[bufIdx];
            }

            inputBufferPos = (inputBufferPos + 1) % windowSize;

            // Process through neural network
            float wet = network.processSample(windowBuffer);
            wet *= outputGainLinear;

            // Output: blend with original stereo
            outputL[i] = dryL * (1.0f - mix) + wet * mix;
            outputR[i] = dryR * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onReset() {
        if (inputBuffer != null) {
            java.util.Arrays.fill(inputBuffer, 0);
        }
        inputBufferPos = 0;
    }

    // Convenience setters
    public void setInputGain(float dB) { inputGainParam.setValue(dB); }
    public void setOutputGain(float dB) { outputGainParam.setValue(dB); }
    public void setMix(float percent) { mixParam.setValue(percent); }
}
