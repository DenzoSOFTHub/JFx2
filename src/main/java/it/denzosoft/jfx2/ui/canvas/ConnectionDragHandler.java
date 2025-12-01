package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Handles connection creation via drag from output port to input port.
 */
public class ConnectionDragHandler {

    private final SignalFlowPanel canvas;
    private final CanvasController controller;

    // Drag state
    private Port sourcePort;
    private Point dragEndPoint;
    private Port targetPort;
    private boolean isValidConnection;

    public ConnectionDragHandler(SignalFlowPanel canvas, CanvasController controller) {
        this.canvas = canvas;
        this.controller = controller;
    }

    /**
     * Start dragging a connection from a port.
     * Only allows starting from OUTPUT ports.
     *
     * @return true if drag started successfully
     */
    public boolean startDrag(Port port, Point worldPoint) {
        if (port == null) return false;

        // Only allow dragging from output ports
        if (port.getDirection() != PortDirection.OUTPUT) {
            return false;
        }

        sourcePort = port;
        dragEndPoint = worldPoint;
        targetPort = null;
        isValidConnection = false;

        return true;
    }

    /**
     * Update drag position.
     */
    public void updateDrag(Point worldPoint) {
        if (sourcePort == null) return;

        dragEndPoint = worldPoint;

        // Check for target port under cursor
        Port hoveredPort = controller.getPortAt(worldPoint.x, worldPoint.y);
        updateTargetPort(hoveredPort);
    }

    /**
     * Update the target port and validation state.
     */
    private void updateTargetPort(Port port) {
        targetPort = port;

        if (port == null) {
            isValidConnection = false;
            return;
        }

        // Validate connection
        isValidConnection = isValidConnectionTarget(port);
    }

    /**
     * Check if a port is a valid connection target.
     */
    private boolean isValidConnectionTarget(Port port) {
        if (port == null || sourcePort == null) return false;

        // Must be an input port
        if (port.getDirection() != PortDirection.INPUT) return false;

        // Can't connect to same node
        if (port.getOwner() == sourcePort.getOwner()) return false;

        // Must be compatible types
        if (!areTypesCompatible(sourcePort.getType(), port.getType())) return false;

        // Can't connect to already connected input (single connection per input)
        if (port.isConnected()) return false;

        // Check for cycles (would create feedback loop)
        SignalGraph graph = canvas.getSignalGraph();
        if (graph != null && wouldCreateCycle(graph, sourcePort.getOwner(), port.getOwner())) {
            return false;
        }

        return true;
    }

    /**
     * Check if port types are compatible.
     */
    private boolean areTypesCompatible(PortType sourceType, PortType targetType) {
        // For now, all audio types are compatible
        return true;
    }

    /**
     * Check if connecting source to target would create a cycle.
     */
    private boolean wouldCreateCycle(SignalGraph graph, ProcessingNode source, ProcessingNode target) {
        // Simple check: see if target can reach source through existing connections
        return canReach(graph, target, source, new java.util.HashSet<>());
    }

    private boolean canReach(SignalGraph graph, ProcessingNode from, ProcessingNode to,
                             java.util.Set<String> visited) {
        if (from == to) return true;
        if (visited.contains(from.getId())) return false;

        visited.add(from.getId());

        for (Port outputPort : from.getOutputPorts()) {
            for (Connection conn : graph.getConnections()) {
                if (conn.getSourcePort() == outputPort) {
                    ProcessingNode nextNode = conn.getTargetPort().getOwner();
                    if (canReach(graph, nextNode, to, visited)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * End the drag and create connection if valid.
     *
     * @return true if connection was created
     */
    public boolean endDrag() {
        if (sourcePort == null) return false;

        boolean created = false;

        if (isValidConnection && targetPort != null) {
            // Create the connection
            SignalGraph graph = canvas.getSignalGraph();
            if (graph != null) {
                graph.connect(sourcePort, targetPort);
                created = true;
            }
        }

        // Reset state
        sourcePort = null;
        dragEndPoint = null;
        targetPort = null;
        isValidConnection = false;

        return created;
    }

    /**
     * Cancel the drag.
     */
    public void cancelDrag() {
        sourcePort = null;
        dragEndPoint = null;
        targetPort = null;
        isValidConnection = false;
    }

    /**
     * Check if currently dragging a connection.
     */
    public boolean isDragging() {
        return sourcePort != null;
    }

    /**
     * Render the drag preview.
     */
    public void render(Graphics2D g2d, ConnectionRenderer renderer) {
        if (sourcePort == null || dragEndPoint == null) return;

        Point startPoint = controller.getPortPosition(sourcePort);
        Point endPoint;

        if (targetPort != null && isValidConnection) {
            // Snap to target port
            endPoint = controller.getPortPosition(targetPort);
        } else {
            endPoint = dragEndPoint;
        }

        renderer.renderDragConnection(g2d, startPoint, endPoint, isValidConnection);
    }

    /**
     * Generate a unique connection ID.
     */
    private String generateConnectionId() {
        SignalGraph graph = canvas.getSignalGraph();
        int counter = 1;
        String id;
        do {
            id = "conn_" + counter++;
        } while (connectionExists(graph, id));
        return id;
    }

    private boolean connectionExists(SignalGraph graph, String id) {
        if (graph == null) return false;
        for (Connection conn : graph.getConnections()) {
            if (conn.getId().equals(id)) return true;
        }
        return false;
    }

    // Getters for rendering hints

    public Port getSourcePort() {
        return sourcePort;
    }

    public Port getTargetPort() {
        return targetPort;
    }

    public boolean isValidConnection() {
        return isValidConnection;
    }

    public Point getDragEndPoint() {
        return dragEndPoint;
    }
}
