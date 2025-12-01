package it.denzosoft.jfx2.graph;

/**
 * Simple gain/volume node for testing.
 *
 * <p>Applies a gain factor to the input signal.</p>
 */
public class GainNode extends AbstractNode {

    private final Port inputPort;
    private final Port outputPort;

    private volatile float gain;

    /**
     * Create a gain node.
     *
     * @param id   Unique identifier
     * @param name Display name
     */
    public GainNode(String id, String name) {
        super(id, name, NodeType.EFFECT);
        this.inputPort = addInputPort("in", "Input", PortType.AUDIO_MONO);
        this.outputPort = addOutputPort("out", "Output", PortType.AUDIO_MONO);
        this.gain = 1.0f;
    }

    /**
     * Create a gain node with default name.
     *
     * @param id Unique identifier
     */
    public GainNode(String id) {
        this(id, "Gain");
    }

    @Override
    public void process(int frameCount) {
        float[] input = inputPort.getBuffer();
        float[] output = outputPort.getBuffer();

        if (input == null || output == null) {
            return;
        }

        if (bypassed) {
            // Bypass - copy input to output unchanged
            System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
        } else {
            // Apply gain
            float g = this.gain;
            for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
                output[i] = input[i] * g;
            }
        }
    }

    /**
     * Get the current gain value.
     */
    public float getGain() {
        return gain;
    }

    /**
     * Set the gain value.
     *
     * @param gain Gain factor (0.0 = silence, 1.0 = unity, 2.0 = +6dB)
     */
    public void setGain(float gain) {
        this.gain = gain;
    }

    /**
     * Set gain in decibels.
     *
     * @param dB Gain in dB (0 = unity, -6 = half, +6 = double)
     */
    public void setGainDb(float dB) {
        this.gain = (float) Math.pow(10.0, dB / 20.0);
    }

    /**
     * Get gain in decibels.
     */
    public float getGainDb() {
        return (float) (20.0 * Math.log10(gain));
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
}
