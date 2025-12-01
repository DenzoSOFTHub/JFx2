package it.denzosoft.jfx2.graph;

import java.util.List;

/**
 * Interface for all processing nodes in the signal graph.
 *
 * <p>A processing node can be an effect, a utility node (splitter/mixer),
 * or a system node (input/output).</p>
 */
public interface ProcessingNode {

    /**
     * Get the unique identifier of this node.
     */
    String getId();

    /**
     * Get the display name of this node.
     */
    String getName();

    /**
     * Set the display name of this node.
     *
     * @param name The new display name
     */
    void setName(String name);

    /**
     * Get the type/category of this node.
     */
    NodeType getNodeType();

    /**
     * Get all input ports.
     */
    List<Port> getInputPorts();

    /**
     * Get all output ports.
     */
    List<Port> getOutputPorts();

    /**
     * Get an input port by ID.
     *
     * @param portId Port identifier
     * @return The port, or null if not found
     */
    Port getInputPort(String portId);

    /**
     * Get an output port by ID.
     *
     * @param portId Port identifier
     * @return The port, or null if not found
     */
    Port getOutputPort(String portId);

    /**
     * Prepare the node for processing.
     * Called once before processing starts.
     *
     * @param sampleRate    Sample rate in Hz
     * @param maxFrameCount Maximum frames per process call
     */
    void prepare(int sampleRate, int maxFrameCount);

    /**
     * Process audio through this node.
     *
     * <p>Input data should be read from input port buffers.
     * Output data should be written to output port buffers.</p>
     *
     * @param frameCount Number of frames to process
     */
    void process(int frameCount);

    /**
     * Release resources when processing stops.
     */
    void release();

    /**
     * Reset internal state (clear delay lines, etc.).
     */
    void reset();

    /**
     * Check if this node is bypassed.
     */
    boolean isBypassed();

    /**
     * Set bypass state.
     *
     * @param bypassed true to bypass processing
     */
    void setBypassed(boolean bypassed);

    /**
     * Check if output is currently clipping (samples exceeding -1 to +1 range).
     * The clipping state has a hold time so the indicator remains visible briefly.
     *
     * @return true if clipping was detected recently
     */
    default boolean isClipping() {
        return false;
    }

    /**
     * Mark that clipping has been detected.
     * Called by processing code when samples exceed -1 to +1 range.
     */
    default void setClipping() {
        // Default implementation does nothing
    }
}
