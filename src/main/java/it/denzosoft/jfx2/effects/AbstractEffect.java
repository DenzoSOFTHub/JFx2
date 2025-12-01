package it.denzosoft.jfx2.effects;

import java.util.*;

/**
 * Base class for all audio effects.
 *
 * <p>Provides common functionality for parameter management,
 * bypass handling, and preparation.</p>
 */
public abstract class AbstractEffect implements AudioEffect {

    protected final EffectMetadata metadata;
    protected final Map<String, Parameter> parameters;
    protected final List<Parameter> parameterList;

    protected int sampleRate;
    protected int maxFrameCount;
    protected volatile boolean bypassed;

    // Stereo mode
    protected StereoMode stereoMode = StereoMode.AUTO;
    protected Parameter stereoModeParam;

    // Number of input channels (set by audio engine)
    protected int inputChannels = 1;  // Default to mono

    // Number of output channels (computed after processing)
    protected int outputChannels = 1;  // Default to mono

    /**
     * Create an effect with the given metadata.
     */
    protected AbstractEffect(EffectMetadata metadata) {
        this.metadata = metadata;
        this.parameters = new LinkedHashMap<>();
        this.parameterList = new ArrayList<>();
        this.bypassed = false;

        // Add stereo mode parameter as the first parameter
        stereoModeParam = addChoiceParameter("stereoMode", "Stereo",
                "Processing mode: Mono (L only), Stereo (L+R independent), Auto (detect from input)",
                StereoMode.getDisplayNames(), StereoMode.AUTO.ordinal());
    }

    /**
     * Add a parameter to this effect.
     * Subclasses should call this in their constructor.
     */
    protected Parameter addParameter(Parameter parameter) {
        parameters.put(parameter.getId(), parameter);
        parameterList.add(parameter);
        return parameter;
    }

    /**
     * Create and add a float parameter without description.
     */
    protected Parameter addFloatParameter(String id, String name, float min, float max, float defaultValue, String unit) {
        return addParameter(new Parameter(id, name, min, max, defaultValue, unit));
    }

    /**
     * Create and add a float parameter with description.
     */
    protected Parameter addFloatParameter(String id, String name, String description, float min, float max, float defaultValue, String unit) {
        return addParameter(new Parameter(id, name, description, min, max, defaultValue, unit));
    }

    /**
     * Create and add a boolean parameter without description.
     */
    protected Parameter addBooleanParameter(String id, String name, boolean defaultValue) {
        return addParameter(new Parameter(id, name, defaultValue));
    }

    /**
     * Create and add a boolean parameter with description.
     */
    protected Parameter addBooleanParameter(String id, String name, String description, boolean defaultValue) {
        return addParameter(new Parameter(id, name, description, defaultValue));
    }

    /**
     * Create and add a choice parameter without description.
     */
    protected Parameter addChoiceParameter(String id, String name, String[] choices, int defaultIndex) {
        return addParameter(new Parameter(id, name, choices, defaultIndex));
    }

    /**
     * Create and add a choice parameter with description.
     */
    protected Parameter addChoiceParameter(String id, String name, String description, String[] choices, int defaultIndex) {
        return addParameter(new Parameter(id, name, description, choices, defaultIndex));
    }

    /**
     * Create and add an integer parameter with description.
     */
    protected Parameter addIntParameter(String id, String name, String description, int min, int max, int defaultValue, String unit) {
        return addParameter(new Parameter(id, name, description, min, max, defaultValue, unit));
    }

    @Override
    public EffectMetadata getMetadata() {
        return metadata;
    }

    @Override
    public List<Parameter> getParameters() {
        return Collections.unmodifiableList(parameterList);
    }

    @Override
    public Parameter getParameter(String id) {
        return parameters.get(id);
    }

    @Override
    public void prepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;
        this.maxFrameCount = maxFrameCount;

        // Prepare all parameters with sample rate for smoothing
        for (Parameter param : parameterList) {
            param.prepare(sampleRate);
        }

        // Call subclass preparation
        onPrepare(sampleRate, maxFrameCount);
    }

    /**
     * Called after base preparation.
     * Override to allocate buffers, initialize filters, etc.
     */
    protected abstract void onPrepare(int sampleRate, int maxFrameCount);

    @Override
    public void process(float[] input, float[] output, int frameCount) {
        // Update all parameter smoothing for the whole buffer
        for (Parameter param : parameterList) {
            param.smoothBuffer(frameCount);
        }

        if (bypassed) {
            // Bypass: copy input to output
            System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
        } else {
            // Process through effect
            onProcess(input, output, frameCount);
        }
    }

    /**
     * Called to process audio (mono).
     * Override to implement the actual effect processing.
     */
    protected abstract void onProcess(float[] input, float[] output, int frameCount);

    @Override
    public void processStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Update all parameter smoothing for the whole buffer
        for (Parameter param : parameterList) {
            param.smoothBuffer(frameCount);
        }

        // Update stereo mode from parameter
        stereoMode = StereoMode.values()[stereoModeParam.getChoiceIndex()];

        if (bypassed) {
            // Bypass: copy input to output
            int len = Math.min(frameCount, Math.min(inputL.length, outputL.length));
            System.arraycopy(inputL, 0, outputL, 0, len);
            len = Math.min(frameCount, Math.min(inputR.length, outputR.length));
            System.arraycopy(inputR, 0, outputR, 0, len);
            return;
        }

        // Determine effective stereo mode
        StereoMode effectiveMode = stereoMode;
        if (effectiveMode == StereoMode.AUTO) {
            // AUTO: match input channel count
            effectiveMode = (inputChannels >= 2) ? StereoMode.STEREO : StereoMode.MONO;
        }

        // Process based on mode
        if (effectiveMode == StereoMode.MONO) {
            // MONO: process left channel only, copy result to both outputs
            onProcess(inputL, outputL, frameCount);
            System.arraycopy(outputL, 0, outputR, 0, Math.min(frameCount, Math.min(outputL.length, outputR.length)));
            outputChannels = 1;  // Output is mono (L == R)
        } else {
            // STEREO mode - output is always stereo
            if (inputChannels >= 2) {
                // Input is stereo: process L and R independently
                onProcessStereo(inputL, inputR, outputL, outputR, frameCount);
            } else {
                // Input is mono: use stereo processing on duplicated mono signal
                // This allows effects to add spatiality (phase-offset LFOs, etc.)
                onProcessStereo(inputL, inputL, outputL, outputR, frameCount);
            }
            outputChannels = 2;  // Output is stereo (L may differ from R)
        }
    }

    /**
     * Called to process audio in stereo mode.
     * Default implementation processes L and R channels independently using onProcess.
     * Override for effects that need true stereo processing (e.g., stereo widener, panner).
     */
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Default: process each channel independently
        onProcess(inputL, outputL, frameCount);

        // For right channel, we need to temporarily reset any stateful components
        // This is a simple approach - complex effects may need to override this
        onProcess(inputR, outputR, frameCount);
    }

    @Override
    public void setInputChannels(int channels) {
        this.inputChannels = Math.max(1, Math.min(2, channels));
    }

    @Override
    public int getInputChannels() {
        return inputChannels;
    }

    @Override
    public int getOutputChannels() {
        return outputChannels;
    }

    @Override
    public StereoMode getStereoMode() {
        return StereoMode.values()[stereoModeParam.getChoiceIndex()];
    }

    @Override
    public void setStereoMode(StereoMode mode) {
        stereoModeParam.setChoice(mode.ordinal());
        this.stereoMode = mode;
    }

    @Override
    public boolean isBypassed() {
        return bypassed;
    }

    @Override
    public void setBypassed(boolean bypassed) {
        this.bypassed = bypassed;
    }

    @Override
    public void reset() {
        for (Parameter param : parameterList) {
            param.reset();
        }
        onReset();
    }

    /**
     * Called when effect is reset.
     * Override to clear delay lines, reset envelopes, etc.
     */
    protected void onReset() {
        // Default: nothing to reset
    }

    @Override
    public void release() {
        // Default: nothing to release
    }

    /**
     * Utility: Convert dB to linear gain.
     */
    protected static float dbToLinear(float dB) {
        return (float) Math.pow(10.0, dB / 20.0);
    }

    /**
     * Utility: Convert linear gain to dB.
     */
    protected static float linearToDb(float linear) {
        if (linear <= 0) return -100.0f;
        return (float) (20.0 * Math.log10(linear));
    }

    /**
     * Utility: Soft clamp value to range.
     */
    protected static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
