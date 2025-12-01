package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;
import it.denzosoft.jfx2.nam.NAMLoader;
import it.denzosoft.jfx2.nam.NAMModel;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Neural Amp Modeler (NAM) effect.
 *
 * <p>Loads and processes audio using NAM model files (.nam).
 * Supports WaveNet and LSTM architectures.</p>
 *
 * <p>NAM models are trained to replicate the sound of real amplifiers
 * and effects pedals using neural networks.</p>
 */
public class NAMEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "nam",
            "NAM",
            "Neural Amp Modeler - loads .nam profiles",
            EffectCategory.AMP_SIM
    );

    // Parameters
    private final Parameter inputGainParam;
    private final Parameter outputGainParam;
    private final Parameter mixParam;

    // NAM model
    private NAMModel model;
    private boolean modelLoaded = false;
    private String currentModelPath;
    private NAMLoader.NAMMetadata metadata;

    // Sample rate handling
    private int effectSampleRate;
    private int modelSampleRate;
    private boolean needsResampling;

    // Resampling state (simple linear interpolation)
    private float resampleRatio;
    private float resamplePhase;
    private float lastInputSample;
    private float lastOutputSample;

    public NAMEffect() {
        super(METADATA);

        // Input gain: -12 to +12 dB
        inputGainParam = addFloatParameter("inputGain", "Input Gain",
                "Adjust input level before NAM processing",
                -12.0f, 12.0f, 0.0f, "dB");

        // Output gain: -12 to +12 dB
        outputGainParam = addFloatParameter("outputGain", "Output Gain",
                "Adjust output level after NAM processing",
                -12.0f, 12.0f, 0.0f, "dB");

        // Mix: 0-100%
        mixParam = addFloatParameter("mix", "Mix",
                "Blend between dry and processed signal",
                0.0f, 100.0f, 100.0f, "%");
    }

    /**
     * Load a NAM model from file.
     *
     * @param modelPath Path to the .nam file
     * @return true if loaded successfully
     */
    public boolean loadModel(String modelPath) {
        return loadModel(Path.of(modelPath));
    }

    /**
     * Load a NAM model from file.
     *
     * @param modelPath Path to the .nam file
     * @return true if loaded successfully
     */
    public boolean loadModel(Path modelPath) {
        try {
            // Get metadata first
            metadata = NAMLoader.getMetadata(modelPath);
            System.out.println("NAM: Loading " + metadata.architecture() + " model from " + modelPath);

            // Load model
            model = NAMLoader.load(modelPath);
            modelSampleRate = model.getSampleRate();
            currentModelPath = modelPath.toString();
            modelLoaded = true;

            // Update resampling if already prepared
            if (effectSampleRate > 0) {
                updateResampling();
            }

            System.out.println(metadata);
            return true;

        } catch (IOException e) {
            System.err.println("NAM: Failed to load model: " + e.getMessage());
            modelLoaded = false;
            return false;
        }
    }

    /**
     * Check if a model is loaded.
     */
    public boolean isModelLoaded() {
        return modelLoaded;
    }

    /**
     * Get the current model path.
     */
    public String getModelPath() {
        return currentModelPath;
    }

    /**
     * Get the loaded NAM model metadata.
     */
    public NAMLoader.NAMMetadata getNAMMetadata() {
        return metadata;
    }

    /**
     * Get the loaded NAM model.
     */
    public NAMModel getModel() {
        return model;
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.effectSampleRate = sampleRate;
        updateResampling();
    }

    private void updateResampling() {
        if (modelLoaded && model != null) {
            needsResampling = (effectSampleRate != modelSampleRate);
            resampleRatio = (float) modelSampleRate / effectSampleRate;
            resamplePhase = 0;
            lastInputSample = 0;
            lastOutputSample = 0;

            if (needsResampling) {
                System.out.println("NAM: Resampling " + effectSampleRate + " Hz -> " +
                        modelSampleRate + " Hz (ratio: " + resampleRatio + ")");
            }
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float inputGainLinear = dbToLinear(inputGainParam.getValue());
        float outputGainLinear = dbToLinear(outputGainParam.getValue());
        float mix = mixParam.getValue() / 100.0f;

        // If no model loaded, pass through with gain
        if (!modelLoaded || model == null) {
            for (int i = 0; i < frameCount && i < output.length; i++) {
                output[i] = input[i] * outputGainLinear;
            }
            return;
        }

        // Process each sample
        for (int i = 0; i < frameCount && i < output.length && i < input.length; i++) {
            float dry = input[i];
            float scaledInput = dry * inputGainLinear;

            float wet;
            if (needsResampling) {
                wet = processWithResampling(scaledInput);
            } else {
                wet = model.process(scaledInput);
            }

            // Apply output gain and mix
            wet *= outputGainLinear;
            output[i] = dry * (1.0f - mix) + wet * mix;
        }
    }

    /**
     * Process with simple resampling (linear interpolation).
     */
    private float processWithResampling(float inputSample) {
        // Accumulate phase
        resamplePhase += resampleRatio;

        // Process model samples as needed
        while (resamplePhase >= 1.0f) {
            // Interpolate input for model
            float t = resamplePhase - (int) resamplePhase;
            float interpolatedInput = lastInputSample * (1 - t) + inputSample * t;

            // Process through model
            lastOutputSample = model.process(interpolatedInput);

            resamplePhase -= 1.0f;
        }

        lastInputSample = inputSample;
        return lastOutputSample;
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        float inputGainLinear = dbToLinear(inputGainParam.getValue());
        float outputGainLinear = dbToLinear(outputGainParam.getValue());
        float mix = mixParam.getValue() / 100.0f;

        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length,
                Math.min(outputL.length, outputR.length))));

        // If no model loaded, pass through with gain
        if (!modelLoaded || model == null) {
            for (int i = 0; i < len; i++) {
                outputL[i] = inputL[i] * outputGainLinear;
                outputR[i] = inputR[i] * outputGainLinear;
            }
            return;
        }

        // Process mono (mix L+R) through model, then apply to stereo
        for (int i = 0; i < len; i++) {
            float dryL = inputL[i];
            float dryR = inputR[i];

            // Mix to mono for processing
            float dryMono = (dryL + dryR) * 0.5f;
            float scaledInput = dryMono * inputGainLinear;

            float wet;
            if (needsResampling) {
                wet = processWithResampling(scaledInput);
            } else {
                wet = model.process(scaledInput);
            }

            wet *= outputGainLinear;

            // Output: blend with original stereo
            outputL[i] = dryL * (1.0f - mix) + wet * mix;
            outputR[i] = dryR * (1.0f - mix) + wet * mix;
        }
    }

    @Override
    protected void onReset() {
        if (model != null) {
            model.reset();
        }
        resamplePhase = 0;
        lastInputSample = 0;
        lastOutputSample = 0;
    }

    // Convenience setters
    public void setInputGain(float dB) { inputGainParam.setValue(dB); }
    public void setOutputGain(float dB) { outputGainParam.setValue(dB); }
    public void setMix(float percent) { mixParam.setValue(percent); }
}
