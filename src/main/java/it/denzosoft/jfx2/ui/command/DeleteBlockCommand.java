package it.denzosoft.jfx2.ui.command;

import it.denzosoft.jfx2.graph.Connection;
import it.denzosoft.jfx2.graph.ProcessingNode;
import it.denzosoft.jfx2.graph.SignalGraph;
import it.denzosoft.jfx2.ui.canvas.CanvasController;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Command for deleting one or more blocks and their connections.
 */
public class DeleteBlockCommand implements Command {

    private final SignalGraph graph;
    private final CanvasController controller;

    // Deleted nodes with their positions
    private final Map<String, ProcessingNode> deletedNodes = new LinkedHashMap<>();
    private final Map<String, Point> deletedPositions = new HashMap<>();

    // Deleted connections (stored as source/target port info)
    private final List<ConnectionInfo> deletedConnections = new ArrayList<>();

    /**
     * Create a delete command for specified node IDs.
     */
    public DeleteBlockCommand(SignalGraph graph, CanvasController controller, Set<String> nodeIds) {
        this.graph = graph;
        this.controller = controller;

        // Store nodes and positions
        for (String nodeId : nodeIds) {
            ProcessingNode node = graph.getNode(nodeId);
            if (node != null) {
                deletedNodes.put(nodeId, node);
                Point pos = controller.getNodePosition(nodeId);
                if (pos != null) {
                    deletedPositions.put(nodeId, new Point(pos));
                }
            }
        }

        // Store connections that will be deleted
        for (Connection conn : graph.getConnections()) {
            String sourceId = conn.getSourcePort().getOwner().getId();
            String targetId = conn.getTargetPort().getOwner().getId();

            // Connection is deleted if either end is being deleted
            if (nodeIds.contains(sourceId) || nodeIds.contains(targetId)) {
                deletedConnections.add(new ConnectionInfo(
                        sourceId,
                        conn.getSourcePort().getName(),
                        targetId,
                        conn.getTargetPort().getName()
                ));
            }
        }
    }

    @Override
    public void execute() {
        // Remove connections first
        for (ConnectionInfo connInfo : deletedConnections) {
            graph.disconnect(connInfo.sourceNodeId, connInfo.sourcePortName,
                    connInfo.targetNodeId, connInfo.targetPortName);
        }

        // Remove nodes
        for (String nodeId : deletedNodes.keySet()) {
            graph.removeNode(nodeId);
            controller.removeNodePosition(nodeId);
        }
    }

    @Override
    public void undo() {
        // Restore nodes first
        for (Map.Entry<String, ProcessingNode> entry : deletedNodes.entrySet()) {
            graph.addNode(entry.getValue());
            Point pos = deletedPositions.get(entry.getKey());
            if (pos != null) {
                controller.setNodePosition(entry.getKey(), pos);
            }
        }

        // Restore connections
        for (ConnectionInfo connInfo : deletedConnections) {
            try {
                graph.connect(connInfo.sourceNodeId, connInfo.sourcePortName,
                        connInfo.targetNodeId, connInfo.targetPortName);
            } catch (Exception e) {
                System.err.println("Could not restore connection: " + e.getMessage());
            }
        }
    }

    @Override
    public String getDescription() {
        if (deletedNodes.size() == 1) {
            return "Delete " + deletedNodes.keySet().iterator().next();
        }
        return "Delete " + deletedNodes.size() + " blocks";
    }

    /**
     * Check if any nodes were actually deleted.
     */
    public boolean hasDeletedNodes() {
        return !deletedNodes.isEmpty();
    }

    /**
     * Get the number of deleted nodes.
     */
    public int getDeletedCount() {
        return deletedNodes.size();
    }

    /**
     * Record to store connection info for restoration.
     */
    private record ConnectionInfo(
            String sourceNodeId,
            String sourcePortName,
            String targetNodeId,
            String targetPortName
    ) {
    }
}
