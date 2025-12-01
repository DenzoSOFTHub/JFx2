package it.denzosoft.jfx2.graph;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.effects.StereoMode;

import java.util.*;

/**
 * Analyzes signal flow through the graph to determine channel count at each connection.
 *
 * <p>Propagates channel information from input sources through the graph,
 * determining whether each connection carries a mono or stereo signal.</p>
 */
public class SignalFlowAnalyzer {

    /**
     * Signal type for a connection.
     */
    public enum SignalType {
        MONO(1),
        STEREO(2),
        UNKNOWN(0);

        private final int channels;

        SignalType(int channels) {
            this.channels = channels;
        }

        public int getChannels() {
            return channels;
        }

        public static SignalType fromChannels(int channels) {
            return switch (channels) {
                case 1 -> MONO;
                case 2 -> STEREO;
                default -> UNKNOWN;
            };
        }
    }

    // Map of connection ID to signal type
    private final Map<String, SignalType> connectionSignalTypes = new HashMap<>();

    // Map of port to signal type (for output ports)
    private final Map<String, SignalType> portSignalTypes = new HashMap<>();

    /**
     * Analyze the signal flow in a graph.
     *
     * @param graph The signal graph to analyze
     */
    public void analyze(SignalGraph graph) {
        connectionSignalTypes.clear();
        portSignalTypes.clear();

        // Get processing order (topologically sorted)
        List<ProcessingNode> processingOrder = graph.getProcessingOrder();

        // Process each node in order
        for (ProcessingNode node : processingOrder) {
            analyzeNode(node, graph);
        }
    }

    /**
     * Analyze a single node and propagate signal types.
     */
    private void analyzeNode(ProcessingNode node, SignalGraph graph) {
        // Determine input signal type (from incoming connections)
        SignalType inputType = getNodeInputSignalType(node, graph);

        // Determine output signal type based on node type
        SignalType outputType = determineOutputSignalType(node, inputType);

        // Store output signal type for all output ports
        for (Port port : node.getOutputPorts()) {
            String portKey = getPortKey(port);
            portSignalTypes.put(portKey, outputType);

            // Update all connections from this port
            for (Connection conn : graph.getConnections()) {
                if (conn.getSourcePort() == port) {
                    connectionSignalTypes.put(conn.getId(), outputType);
                }
            }
        }
    }

    /**
     * Get the input signal type for a node by checking incoming connections.
     */
    private SignalType getNodeInputSignalType(ProcessingNode node, SignalGraph graph) {
        SignalType maxType = SignalType.MONO; // Default to mono if no inputs

        for (Port inputPort : node.getInputPorts()) {
            // Find connection to this input port
            for (Connection conn : graph.getConnections()) {
                if (conn.getTargetPort() == inputPort) {
                    String sourcePortKey = getPortKey(conn.getSourcePort());
                    SignalType sourceType = portSignalTypes.getOrDefault(sourcePortKey, SignalType.MONO);

                    // Use the maximum channel count from inputs
                    if (sourceType.getChannels() > maxType.getChannels()) {
                        maxType = sourceType;
                    }
                }
            }
        }

        return maxType;
    }

    /**
     * Determine the output signal type based on node type and settings.
     */
    private SignalType determineOutputSignalType(ProcessingNode node, SignalType inputType) {
        // Handle different node types
        if (node instanceof EffectNode effectNode) {
            return determineEffectOutputType(effectNode, inputType);
        } else if (node instanceof MixerNode mixerNode) {
            return determineMixerOutputType(mixerNode);
        } else if (node instanceof SplitterNode) {
            // Splitter passes through the same signal type
            return inputType;
        } else if (node instanceof InputNode) {
            // Legacy InputNode - mono
            return SignalType.MONO;
        } else if (node instanceof OutputNode) {
            // Output node doesn't have outputs in the graph
            return inputType;
        }

        // Default: pass through input type
        return inputType;
    }

    /**
     * Determine output type for an effect node.
     */
    private SignalType determineEffectOutputType(EffectNode effectNode, SignalType inputType) {
        AudioEffect effect = effectNode.getEffect();
        if (effect == null) {
            return inputType;
        }

        EffectCategory category = effect.getMetadata().category();

        // Input sources always output mono
        if (category == EffectCategory.INPUT_SOURCE) {
            return SignalType.MONO;
        }

        // Output sinks don't have graph outputs
        if (category == EffectCategory.OUTPUT_SINK) {
            return SignalType.UNKNOWN;
        }

        // Check effect's stereo mode
        StereoMode stereoMode = effect.getStereoMode();

        switch (stereoMode) {
            case MONO:
                // MONO mode: output is always mono
                return SignalType.MONO;
            case STEREO:
                // STEREO mode: output is stereo
                return SignalType.STEREO;
            case AUTO:
            default:
                // AUTO mode: output matches input
                return inputType;
        }
    }

    /**
     * Determine output type for a mixer node.
     */
    private SignalType determineMixerOutputType(MixerNode mixerNode) {
        // MixerNode outputs based on stereo mode setting
        MixerNode.StereoMode mode = mixerNode.getStereoMode();
        return switch (mode) {
            case STEREO -> SignalType.STEREO;
            case MONO -> SignalType.MONO;
        };
    }

    /**
     * Get the signal type for a connection.
     *
     * @param connectionId The connection ID
     * @return The signal type (MONO, STEREO, or UNKNOWN)
     */
    public SignalType getConnectionSignalType(String connectionId) {
        return connectionSignalTypes.getOrDefault(connectionId, SignalType.UNKNOWN);
    }

    /**
     * Get the signal type for an output port.
     *
     * @param port The output port
     * @return The signal type
     */
    public SignalType getPortSignalType(Port port) {
        return portSignalTypes.getOrDefault(getPortKey(port), SignalType.UNKNOWN);
    }

    /**
     * Check if a connection carries a stereo signal.
     */
    public boolean isStereo(String connectionId) {
        return getConnectionSignalType(connectionId) == SignalType.STEREO;
    }

    /**
     * Check if a connection carries a mono signal.
     */
    public boolean isMono(String connectionId) {
        return getConnectionSignalType(connectionId) == SignalType.MONO;
    }

    /**
     * Create a unique key for a port.
     */
    private String getPortKey(Port port) {
        return port.getOwner().getId() + "." + port.getId();
    }
}
