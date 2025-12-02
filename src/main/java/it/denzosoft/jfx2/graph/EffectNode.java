package it.denzosoft.jfx2.graph;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.Parameter;

import java.util.List;

/**
 * Graph node that wraps an AudioEffect.
 *
 * <p>Bridges the effects system with the signal graph.</p>
 */
public class EffectNode extends AbstractNode {

    private final AudioEffect effect;
    private final Port inputPort;
    private final Port outputPort;

    // Internal buffer for effect processing
    private float[] effectBuffer;

    // Monitor listener for waveform display
    private volatile EffectMonitorListener monitorListener;

    /**
     * Listener for monitoring effect input/output during processing.
     * Used for waveform visualization.
     */
    public interface EffectMonitorListener {
        /**
         * Called after effect processing with copies of input and output samples.
         * Note: This is called from the audio processing thread - implementations
         * should be lightweight and thread-safe.
         *
         * @param input Input samples (may be null for source effects)
         * @param output Output samples (may be null for sink effects)
         * @param frameCount Number of frames processed
         */
        void onProcessed(float[] input, float[] output, int frameCount);
    }

    /**
     * Create an effect node.
     *
     * @param id     Unique identifier
     * @param effect The audio effect to wrap
     */
    public EffectNode(String id, AudioEffect effect) {
        super(id, effect.getMetadata().name(), NodeType.EFFECT);
        this.effect = effect;

        // Create ports based on effect category
        EffectCategory category = effect.getMetadata().category();

        if (category == EffectCategory.INPUT_SOURCE) {
            // Input sources (AudioInput, Oscillator) only have output port
            this.inputPort = null;
            this.outputPort = addOutputPort("out", "Output", PortType.AUDIO_MONO);
        } else if (category == EffectCategory.OUTPUT_SINK) {
            // Output sinks (AudioOutput) only have input port
            this.inputPort = addInputPort("in", "Input", PortType.AUDIO_MONO);
            this.outputPort = null;
        } else {
            // All other effects have both input and output ports
            this.inputPort = addInputPort("in", "Input", PortType.AUDIO_MONO);
            this.outputPort = addOutputPort("out", "Output", PortType.AUDIO_MONO);
        }
    }

    /**
     * Create an effect node with auto-generated ID.
     *
     * @param effect The audio effect to wrap
     */
    public EffectNode(AudioEffect effect) {
        this(effect.getMetadata().id() + "_" + System.currentTimeMillis(), effect);
    }

    @Override
    public void prepare(int sampleRate, int maxFrameCount) {
        super.prepare(sampleRate, maxFrameCount);
        effect.prepare(sampleRate, maxFrameCount);
        effectBuffer = new float[maxFrameCount];
    }

    @Override
    public void process(int frameCount) {
        float[] input = inputPort != null ? inputPort.getBuffer() : null;
        float[] output = outputPort != null ? outputPort.getBuffer() : null;

        // Ensure effect buffer exists for processing
        if (effectBuffer == null || effectBuffer.length < frameCount) {
            effectBuffer = new float[frameCount];
        }

        // Handle different port configurations
        EffectCategory category = effect.getMetadata().category();

        if (category == EffectCategory.INPUT_SOURCE) {
            // Input sources: no input port, generate to output
            if (output != null) {
                if (bypassed || effect.isBypassed()) {
                    java.util.Arrays.fill(output, 0, Math.min(frameCount, output.length), 0.0f);
                } else {
                    effect.process(effectBuffer, output, frameCount);
                }
                // Check for clipping on output
                checkClipping(output, frameCount);
            }
        } else if (category == EffectCategory.OUTPUT_SINK) {
            // Output sinks: input port only, consume input
            if (input != null) {
                if (bypassed || effect.isBypassed()) {
                    // Bypassed - still need to process to write to audio device
                    effect.process(input, effectBuffer, frameCount);
                } else {
                    effect.process(input, effectBuffer, frameCount);
                }
                // Check for clipping on input (the final signal going to output device)
                checkClipping(input, frameCount);
            }
        } else {
            // Normal effects: both input and output
            if (input == null || output == null) {
                return;
            }

            if (bypassed || effect.isBypassed()) {
                // Bypass - copy input to output
                System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
            } else {
                // Process through effect
                effect.process(input, output, frameCount);
            }
            // Check for clipping on output
            checkClipping(output, frameCount);
        }

        // Notify monitor listener if set
        EffectMonitorListener listener = monitorListener;
        if (listener != null) {
            listener.onProcessed(input, output, frameCount);
        }
    }

    @Override
    public void reset() {
        super.reset();
        effect.reset();
    }

    @Override
    public void release() {
        super.release();
        effect.release();
    }

    /**
     * Get the wrapped audio effect.
     */
    public AudioEffect getEffect() {
        return effect;
    }

    /**
     * Get effect parameters.
     */
    public List<Parameter> getParameters() {
        return effect.getParameters();
    }

    /**
     * Get a parameter by ID.
     */
    public Parameter getParameter(String id) {
        return effect.getParameter(id);
    }

    /**
     * Get the input port.
     */
    public Port getInput() {
        return inputPort;
    }

    /**
     * Get the output port.
     */
    public Port getOutput() {
        return outputPort;
    }

    @Override
    public void setBypassed(boolean bypassed) {
        super.setBypassed(bypassed);
        effect.setBypassed(bypassed);
    }

    /**
     * Set the monitor listener to receive input/output samples during processing.
     * Set to null to disable monitoring.
     *
     * @param listener The listener, or null to remove
     */
    public void setMonitorListener(EffectMonitorListener listener) {
        this.monitorListener = listener;
    }

    /**
     * Get the current monitor listener.
     *
     * @return The listener, or null if none set
     */
    public EffectMonitorListener getMonitorListener() {
        return monitorListener;
    }

    @Override
    public int getLatency() {
        return effect.getLatency();
    }
}
