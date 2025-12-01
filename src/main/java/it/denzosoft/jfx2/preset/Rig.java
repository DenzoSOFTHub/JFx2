package it.denzosoft.jfx2.preset;

import java.util.*;

/**
 * A rig preset containing the complete signal chain configuration.
 *
 * <p>Stores:
 * - Metadata (name, author, category, etc.)
 * - Node definitions (effect type, ID, parameters)
 * - Connection definitions (source -> target)
 * - Mixer/Splitter configurations
 * </p>
 */
public class Rig {

    private RigMetadata metadata;
    private final List<NodeDefinition> nodes;
    private final List<ConnectionDefinition> connections;

    /**
     * Definition of a node in the rig.
     */
    public record NodeDefinition(
            String id,
            String type,           // Effect type ID or "splitter"/"mixer"
            String name,           // Display name
            boolean bypassed,
            int x,                 // Canvas position X
            int y,                 // Canvas position Y
            Map<String, Object> parameters,  // Parameter ID -> value
            Map<String, Object> config       // Extra config (e.g., numOutputs for splitter)
    ) {
        public static NodeDefinition effect(String id, String type, Map<String, Object> parameters) {
            return new NodeDefinition(id, type, type, false, 0, 0, parameters, Map.of());
        }

        public static NodeDefinition effect(String id, String type, int x, int y, Map<String, Object> parameters) {
            return new NodeDefinition(id, type, type, false, x, y, parameters, Map.of());
        }

        public static NodeDefinition splitter(String id, int numOutputs) {
            return new NodeDefinition(id, "splitter", "Splitter", false, 0, 0, Map.of(), Map.of("numOutputs", numOutputs));
        }

        public static NodeDefinition splitter(String id, int x, int y, int numOutputs) {
            return new NodeDefinition(id, "splitter", "Splitter", false, x, y, Map.of(), Map.of("numOutputs", numOutputs));
        }

        public static NodeDefinition mixer(String id, int numInputs, Map<String, Object> mixerConfig) {
            return new NodeDefinition(id, "mixer", "Mixer", false, 0, 0, Map.of(), mixerConfig);
        }

        public static NodeDefinition mixer(String id, int x, int y, int numInputs, Map<String, Object> mixerConfig) {
            return new NodeDefinition(id, "mixer", "Mixer", false, x, y, Map.of(), mixerConfig);
        }
    }

    /**
     * Definition of a connection between nodes.
     */
    public record ConnectionDefinition(
            String sourceNodeId,
            String sourcePortId,
            String targetNodeId,
            String targetPortId
    ) {
        /**
         * Simple connection using default ports.
         */
        public static ConnectionDefinition of(String sourceNode, String targetNode) {
            return new ConnectionDefinition(sourceNode, "out", targetNode, "in");
        }

        /**
         * Connection with specific ports.
         */
        public static ConnectionDefinition of(String sourceNode, String sourcePort, String targetNode, String targetPort) {
            return new ConnectionDefinition(sourceNode, sourcePort, targetNode, targetPort);
        }
    }

    /**
     * Create an empty rig.
     */
    public Rig() {
        this.metadata = RigMetadata.of("New Rig", "Custom");
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    /**
     * Create a rig with metadata.
     */
    public Rig(RigMetadata metadata) {
        this.metadata = metadata;
        this.nodes = new ArrayList<>();
        this.connections = new ArrayList<>();
    }

    // Metadata

    public RigMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(RigMetadata metadata) {
        this.metadata = metadata;
    }

    public String getName() {
        return metadata.name();
    }

    // Nodes

    public void addNode(NodeDefinition node) {
        nodes.add(node);
    }

    public void removeNode(String nodeId) {
        nodes.removeIf(n -> n.id().equals(nodeId));
        // Also remove connections to/from this node
        connections.removeIf(c -> c.sourceNodeId().equals(nodeId) || c.targetNodeId().equals(nodeId));
    }

    public NodeDefinition getNode(String nodeId) {
        return nodes.stream()
                .filter(n -> n.id().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    public List<NodeDefinition> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    // Connections

    public void addConnection(ConnectionDefinition connection) {
        connections.add(connection);
    }

    public void removeConnection(String sourceNodeId, String targetNodeId) {
        connections.removeIf(c -> c.sourceNodeId().equals(sourceNodeId) && c.targetNodeId().equals(targetNodeId));
    }

    public List<ConnectionDefinition> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    // Utility

    /**
     * Clear all nodes and connections.
     */
    public void clear() {
        nodes.clear();
        connections.clear();
    }

    /**
     * Get nodes in topological order (for processing).
     */
    public List<String> getProcessingOrder() {
        // Simple implementation - return nodes in order added
        // (assumes user adds them in correct order)
        List<String> order = new ArrayList<>();
        for (NodeDefinition node : nodes) {
            order.add(node.id());
        }
        return order;
    }

    @Override
    public String toString() {
        return String.format("Rig[%s, %d nodes, %d connections]",
                metadata.name(), nodes.size(), connections.size());
    }
}
