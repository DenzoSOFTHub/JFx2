package it.denzosoft.jfx2.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Splitter node - splits signal to multiple parallel outputs.
 *
 * <p>Takes a single input and copies it to 2-8 outputs for parallel processing.</p>
 */
public class SplitterNode extends AbstractNode {

    private final Port inputPort;
    private final List<Port> outputPorts;
    private final int numOutputs;

    /**
     * Create a splitter node with specified number of outputs.
     *
     * @param id         Unique identifier
     * @param numOutputs Number of outputs (2-8)
     */
    public SplitterNode(String id, int numOutputs) {
        super(id, "Splitter", NodeType.SPLITTER);

        // Clamp to valid range
        this.numOutputs = Math.max(2, Math.min(8, numOutputs));

        // Create input port
        this.inputPort = addInputPort("in", "Input", PortType.AUDIO_MONO);

        // Create output ports
        this.outputPorts = new ArrayList<>();
        for (int i = 0; i < this.numOutputs; i++) {
            Port out = addOutputPort("out" + (i + 1), "Output " + (i + 1), PortType.AUDIO_MONO);
            outputPorts.add(out);
        }
    }

    /**
     * Create a splitter with 2 outputs (default).
     */
    public SplitterNode(String id) {
        this(id, 2);
    }

    @Override
    public void process(int frameCount) {
        float[] input = inputPort.getBuffer();

        if (input == null) {
            return;
        }

        // Copy input to all outputs
        for (Port outputPort : outputPorts) {
            float[] output = outputPort.getBuffer();
            if (output != null) {
                System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
            }
        }
    }

    /**
     * Get the input port.
     */
    public Port getInput() {
        return inputPort;
    }

    /**
     * Get output port by index (0-based).
     */
    public Port getOutput(int index) {
        if (index < 0 || index >= outputPorts.size()) {
            throw new IndexOutOfBoundsException("Output index " + index + " out of range (0-" + (outputPorts.size() - 1) + ")");
        }
        return outputPorts.get(index);
    }

    /**
     * Get all output ports.
     */
    public List<Port> getOutputs() {
        return new ArrayList<>(outputPorts);
    }

    /**
     * Get number of outputs.
     */
    public int getNumOutputs() {
        return numOutputs;
    }
}
