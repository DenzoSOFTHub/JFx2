package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

/**
 * Controller for the signal flow canvas.
 * Manages block positions, selection state, and coordinates model-view synchronization.
 */
public class CanvasController {

    // ==================== SIGNAL FLOW ANALYZER ====================
    private final SignalFlowAnalyzer signalFlowAnalyzer = new SignalFlowAnalyzer();

    // ==================== BLOCK DIMENSIONS (Pedal-style 1:2 ratio) ====================
    public static final int BLOCK_WIDTH = 90;
    public static final int BLOCK_HEIGHT = 180;
    public static final int BLOCK_HEIGHT_MIN = 180;
    public static final int BLOCK_HEIGHT_PER_PORT = 24;
    public static final int BLOCK_HEADER_HEIGHT = 28;
    public static final int BLOCK_CORNER_RADIUS = 10;
    public static final int PORT_RADIUS = 7;
    public static final int PORT_SPACING = 24;

    // ==================== LAYOUT CONSTANTS ====================
    public static final int LAYOUT_MARGIN = 80;
    public static final int LAYOUT_H_SPACING = 120;
    public static final int LAYOUT_V_SPACING = 200;

    // ==================== MODEL ====================
    private SignalGraph signalGraph;

    // ==================== VIEW STATE ====================
    private final Map<String, Point> nodePositions = new HashMap<>();
    private final Set<String> selectedNodes = new HashSet<>();
    private final Set<String> selectedConnections = new HashSet<>();
    private final Set<String> hoveredPorts = new HashSet<>();
    private String hoveredNode = null;
    private String hoveredConnection = null;

    // ==================== LISTENERS ====================
    private final List<CanvasListener> listeners = new ArrayList<>();

    public CanvasController() {
    }

    // ==================== MODEL BINDING ====================

    /**
     * Set the signal graph model.
     */
    public void setSignalGraph(SignalGraph graph) {
        this.signalGraph = graph;
        clearSelection();

        if (graph != null) {
            // Initialize positions for nodes that don't have them
            for (ProcessingNode node : graph.getNodes()) {
                if (!nodePositions.containsKey(node.getId())) {
                    nodePositions.put(node.getId(), new Point(0, 0));
                }
            }
            // Perform auto-layout
            performAutoLayout();
        }

        fireCanvasChanged();
    }

    public SignalGraph getSignalGraph() {
        return signalGraph;
    }

    // ==================== POSITION MANAGEMENT ====================

    /**
     * Get the position of a node.
     */
    public Point getNodePosition(String nodeId) {
        return nodePositions.getOrDefault(nodeId, new Point(0, 0));
    }

    /**
     * Set the position of a node.
     */
    public void setNodePosition(String nodeId, int x, int y) {
        nodePositions.put(nodeId, new Point(x, y));
        fireCanvasChanged();
    }

    /**
     * Set the position of a node (Point version).
     */
    public void setNodePosition(String nodeId, Point position) {
        setNodePosition(nodeId, position.x, position.y);
    }

    /**
     * Remove the position of a node.
     */
    public void removeNodePosition(String nodeId) {
        nodePositions.remove(nodeId);
        fireCanvasChanged();
    }

    /**
     * Move a node by delta.
     */
    public void moveNode(String nodeId, int dx, int dy) {
        Point pos = getNodePosition(nodeId);
        setNodePosition(nodeId, pos.x + dx, pos.y + dy);
    }

    /**
     * Move all selected nodes by delta.
     */
    public void moveSelectedNodes(int dx, int dy) {
        for (String nodeId : selectedNodes) {
            Point pos = getNodePosition(nodeId);
            nodePositions.put(nodeId, new Point(pos.x + dx, pos.y + dy));
        }
        fireCanvasChanged();
    }

    /**
     * Get the bounding rectangle of a node.
     */
    public Rectangle getNodeBounds(String nodeId) {
        Point pos = getNodePosition(nodeId);
        ProcessingNode node = signalGraph != null ? signalGraph.getNode(nodeId) : null;
        int height = getBlockHeight(node);
        return new Rectangle(pos.x, pos.y, BLOCK_WIDTH, height);
    }

    /**
     * Get the center point of a node.
     */
    public Point getNodeCenter(String nodeId) {
        Point pos = getNodePosition(nodeId);
        ProcessingNode node = signalGraph != null ? signalGraph.getNode(nodeId) : null;
        int height = getBlockHeight(node);
        return new Point(pos.x + BLOCK_WIDTH / 2, pos.y + height / 2);
    }

    /**
     * Calculate the block height based on number of ports.
     * Blocks with more ports are taller.
     */
    public static int getBlockHeight(ProcessingNode node) {
        if (node == null) return BLOCK_HEIGHT;

        int maxPorts = Math.max(node.getInputPorts().size(), node.getOutputPorts().size());
        if (maxPorts <= 2) {
            return BLOCK_HEIGHT_MIN;
        }
        // Add extra height for each port beyond 2
        return BLOCK_HEIGHT_MIN + (maxPorts - 2) * BLOCK_HEIGHT_PER_PORT;
    }

    // ==================== PORT POSITIONS ====================

    /**
     * Get the position of a port on its node.
     */
    public Point getPortPosition(Port port) {
        ProcessingNode node = port.getOwner();
        Point nodePos = getNodePosition(node.getId());

        int portIndex;
        int portCount;
        int x, y;

        if (port.getDirection() == PortDirection.INPUT) {
            portIndex = node.getInputPorts().indexOf(port);
            portCount = node.getInputPorts().size();
            x = nodePos.x;  // Left side
        } else {
            portIndex = node.getOutputPorts().indexOf(port);
            portCount = node.getOutputPorts().size();
            x = nodePos.x + BLOCK_WIDTH;  // Right side
        }

        // Distribute ports vertically using dynamic block height
        int blockHeight = getBlockHeight(node);
        int availableHeight = blockHeight - BLOCK_HEADER_HEIGHT;
        int startY = nodePos.y + BLOCK_HEADER_HEIGHT;

        if (portCount == 1) {
            y = startY + availableHeight / 2;
        } else {
            int spacing = availableHeight / (portCount + 1);
            y = startY + spacing * (portIndex + 1);
        }

        return new Point(x, y);
    }

    /**
     * Get the port at a given position, if any.
     */
    public Port getPortAt(int x, int y) {
        if (signalGraph == null) return null;

        for (ProcessingNode node : signalGraph.getNodes()) {
            for (Port port : node.getInputPorts()) {
                Point pos = getPortPosition(port);
                if (isPointNearPort(x, y, pos)) {
                    return port;
                }
            }
            for (Port port : node.getOutputPorts()) {
                Point pos = getPortPosition(port);
                if (isPointNearPort(x, y, pos)) {
                    return port;
                }
            }
        }
        return null;
    }

    private boolean isPointNearPort(int px, int py, Point portPos) {
        int hitRadius = PORT_RADIUS + 4;
        return Math.abs(px - portPos.x) <= hitRadius && Math.abs(py - portPos.y) <= hitRadius;
    }

    // ==================== SELECTION MANAGEMENT ====================

    /**
     * Check if a node is selected.
     */
    public boolean isNodeSelected(String nodeId) {
        return selectedNodes.contains(nodeId);
    }

    /**
     * Check if a connection is selected.
     */
    public boolean isConnectionSelected(String connectionId) {
        return selectedConnections.contains(connectionId);
    }

    /**
     * Select a single node (deselect others).
     */
    public void selectNode(String nodeId) {
        selectedNodes.clear();
        selectedConnections.clear();
        if (nodeId != null) {
            selectedNodes.add(nodeId);
        }
        fireSelectionChanged();
    }

    /**
     * Toggle node selection (for Shift+click).
     */
    public void toggleNodeSelection(String nodeId) {
        if (selectedNodes.contains(nodeId)) {
            selectedNodes.remove(nodeId);
        } else {
            selectedNodes.add(nodeId);
        }
        fireSelectionChanged();
    }

    /**
     * Add node to selection.
     */
    public void addToSelection(String nodeId) {
        selectedNodes.add(nodeId);
        fireSelectionChanged();
    }

    /**
     * Add node to selection (alias for addToSelection).
     */
    public void addNodeToSelection(String nodeId) {
        selectedNodes.add(nodeId);
        fireSelectionChanged();
    }

    /**
     * Select a single connection.
     */
    public void selectConnection(String connectionId) {
        selectedNodes.clear();
        selectedConnections.clear();
        if (connectionId != null) {
            selectedConnections.add(connectionId);
        }
        fireSelectionChanged();
    }

    /**
     * Select all nodes.
     */
    public void selectAll() {
        if (signalGraph != null) {
            for (ProcessingNode node : signalGraph.getNodes()) {
                selectedNodes.add(node.getId());
            }
        }
        fireSelectionChanged();
    }

    /**
     * Clear all selection.
     */
    public void clearSelection() {
        selectedNodes.clear();
        selectedConnections.clear();
        fireSelectionChanged();
    }

    /**
     * Select nodes within a rectangle (marquee selection).
     */
    public void selectNodesInRect(Rectangle2D rect, boolean addToSelection) {
        if (!addToSelection) {
            selectedNodes.clear();
            selectedConnections.clear();
        }

        if (signalGraph != null) {
            for (ProcessingNode node : signalGraph.getNodes()) {
                Rectangle bounds = getNodeBounds(node.getId());
                if (rect.intersects(bounds)) {
                    selectedNodes.add(node.getId());
                }
            }
        }
        fireSelectionChanged();
    }

    /**
     * Get all selected node IDs.
     */
    public Set<String> getSelectedNodes() {
        return Collections.unmodifiableSet(selectedNodes);
    }

    /**
     * Get all selected connection IDs.
     */
    public Set<String> getSelectedConnections() {
        return Collections.unmodifiableSet(selectedConnections);
    }

    /**
     * Check if there is any selection.
     */
    public boolean hasSelection() {
        return !selectedNodes.isEmpty() || !selectedConnections.isEmpty();
    }

    /**
     * Get the first selected node (for parameter panel).
     */
    public ProcessingNode getFirstSelectedNode() {
        if (signalGraph == null || selectedNodes.isEmpty()) {
            return null;
        }
        String nodeId = selectedNodes.iterator().next();
        return signalGraph.getNode(nodeId);
    }

    /**
     * Get the first selected connection (for connection info panel).
     */
    public Connection getFirstSelectedConnection() {
        if (signalGraph == null || selectedConnections.isEmpty()) {
            return null;
        }
        String connectionId = selectedConnections.iterator().next();
        return signalGraph.getConnection(connectionId);
    }

    /**
     * Check if a connection is selected.
     */
    public boolean hasConnectionSelection() {
        return !selectedConnections.isEmpty();
    }

    // ==================== HOVER STATE ====================

    public void setHoveredNode(String nodeId) {
        if (!Objects.equals(this.hoveredNode, nodeId)) {
            this.hoveredNode = nodeId;
            fireCanvasChanged();
        }
    }

    public String getHoveredNode() {
        return hoveredNode;
    }

    public void setHoveredConnection(String connectionId) {
        if (!Objects.equals(this.hoveredConnection, connectionId)) {
            this.hoveredConnection = connectionId;
            fireCanvasChanged();
        }
    }

    public String getHoveredConnection() {
        return hoveredConnection;
    }

    public void setHoveredPort(String portKey, boolean hovered) {
        if (hovered) {
            hoveredPorts.add(portKey);
        } else {
            hoveredPorts.remove(portKey);
        }
    }

    /**
     * Set the single hovered port (clears others).
     */
    public void setHoveredPort(String portKey) {
        hoveredPorts.clear();
        if (portKey != null) {
            hoveredPorts.add(portKey);
        }
        fireCanvasChanged();
    }

    public boolean isPortHovered(String portKey) {
        return hoveredPorts.contains(portKey);
    }

    /**
     * Clear connection selection only.
     */
    public void clearConnectionSelection() {
        selectedConnections.clear();
        fireSelectionChanged();
    }

    // ==================== HIT TESTING ====================

    /**
     * Get the node at a given position.
     */
    public ProcessingNode getNodeAt(int x, int y) {
        if (signalGraph == null) return null;

        // Check in reverse order (top-most first)
        List<ProcessingNode> nodes = new ArrayList<>(signalGraph.getNodes());
        Collections.reverse(nodes);

        for (ProcessingNode node : nodes) {
            Rectangle bounds = getNodeBounds(node.getId());
            if (bounds.contains(x, y)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Check if a point is on the footswitch area of a node.
     * Used for click-to-toggle-bypass functionality.
     *
     * @param node The node to check
     * @param x    X coordinate in world space
     * @param y    Y coordinate in world space
     * @return true if the point is within the footswitch hit area
     */
    public boolean isPointOnFootswitch(ProcessingNode node, int x, int y) {
        if (node == null) return false;

        Point pos = getNodePosition(node.getId());
        int nodeHeight = getBlockHeight(node);

        // Footswitch dimensions (must match BlockRenderer)
        int switchSize = 28;
        int bezelSize = switchSize + 8;  // Include bezel for easier clicking
        int pedalBevel = 3;

        // Calculate footswitch hit area (centered horizontally, near bottom)
        int hitX = pos.x + (BLOCK_WIDTH - bezelSize) / 2;
        int hitY = pos.y + nodeHeight - bezelSize - 10 - pedalBevel;
        int hitW = bezelSize;
        int hitH = bezelSize;

        return x >= hitX && x <= hitX + hitW && y >= hitY && y <= hitY + hitH;
    }

    /**
     * Get the connection at a given position.
     */
    public Connection getConnectionAt(int x, int y) {
        if (signalGraph == null) return null;

        for (Connection conn : signalGraph.getConnections()) {
            if (isPointOnConnection(x, y, conn)) {
                return conn;
            }
        }
        return null;
    }

    /**
     * Check if a point is on a connection (within tolerance).
     */
    private boolean isPointOnConnection(int px, int py, Connection conn) {
        Point start = getPortPosition(conn.getSourcePort());
        Point end = getPortPosition(conn.getTargetPort());

        // Use simple distance-to-bezier approximation
        // Split curve into segments and check distance to each
        int segments = 20;
        double tolerance = 8.0;

        Point2D prev = start;
        for (int i = 1; i <= segments; i++) {
            double t = i / (double) segments;
            Point2D curr = getBezierPoint(start, end, t);

            double dist = distanceToSegment(px, py, prev, curr);
            if (dist <= tolerance) {
                return true;
            }
            prev = curr;
        }
        return false;
    }

    /**
     * Get a point on the bezier curve at parameter t.
     */
    private Point2D getBezierPoint(Point start, Point end, double t) {
        int dx = Math.abs(end.x - start.x) / 2;
        dx = Math.max(dx, 50);

        double x0 = start.x;
        double y0 = start.y;
        double x1 = start.x + dx;
        double y1 = start.y;
        double x2 = end.x - dx;
        double y2 = end.y;
        double x3 = end.x;
        double y3 = end.y;

        double u = 1 - t;
        double x = u * u * u * x0 + 3 * u * u * t * x1 + 3 * u * t * t * x2 + t * t * t * x3;
        double y = u * u * u * y0 + 3 * u * u * t * y1 + 3 * u * t * t * y2 + t * t * t * y3;

        return new Point2D.Double(x, y);
    }

    /**
     * Calculate distance from point to line segment.
     */
    private double distanceToSegment(double px, double py, Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();

        if (dx == 0 && dy == 0) {
            return Math.hypot(px - a.getX(), py - a.getY());
        }

        double t = Math.max(0, Math.min(1, ((px - a.getX()) * dx + (py - a.getY()) * dy) / (dx * dx + dy * dy)));
        double projX = a.getX() + t * dx;
        double projY = a.getY() + t * dy;

        return Math.hypot(px - projX, py - projY);
    }

    // ==================== AUTO LAYOUT ====================

    /**
     * Perform automatic layout based on signal flow.
     */
    public void performAutoLayout() {
        if (signalGraph == null) return;

        // Group nodes by "depth" (distance from input)
        Map<String, Integer> depths = calculateNodeDepths();
        if (depths.isEmpty()) return;

        // Find max depth
        int maxDepth = depths.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Group nodes by depth
        Map<Integer, List<ProcessingNode>> nodesByDepth = new HashMap<>();
        for (ProcessingNode node : signalGraph.getNodes()) {
            int depth = depths.getOrDefault(node.getId(), 0);
            nodesByDepth.computeIfAbsent(depth, k -> new ArrayList<>()).add(node);
        }

        // Position nodes
        for (int depth = 0; depth <= maxDepth; depth++) {
            List<ProcessingNode> nodesAtDepth = nodesByDepth.getOrDefault(depth, Collections.emptyList());
            int count = nodesAtDepth.size();

            int x = LAYOUT_MARGIN + depth * LAYOUT_H_SPACING;

            for (int i = 0; i < count; i++) {
                ProcessingNode node = nodesAtDepth.get(i);
                int y;
                if (count == 1) {
                    y = LAYOUT_MARGIN + LAYOUT_V_SPACING;
                } else {
                    y = LAYOUT_MARGIN + i * LAYOUT_V_SPACING;
                }
                nodePositions.put(node.getId(), new Point(x, y));
            }
        }

        fireCanvasChanged();
    }

    /**
     * Calculate the depth of each node (distance from input).
     */
    private Map<String, Integer> calculateNodeDepths() {
        Map<String, Integer> depths = new HashMap<>();

        if (signalGraph == null) return depths;

        // Find input node
        InputNode inputNode = signalGraph.getInputNode();
        if (inputNode == null) return depths;

        // BFS from input
        Queue<ProcessingNode> queue = new LinkedList<>();
        queue.add(inputNode);
        depths.put(inputNode.getId(), 0);

        while (!queue.isEmpty()) {
            ProcessingNode current = queue.poll();
            int currentDepth = depths.get(current.getId());

            // Find connected nodes
            for (Connection conn : signalGraph.getConnections()) {
                if (conn.getSourceNode().getId().equals(current.getId())) {
                    ProcessingNode target = conn.getTargetNode();
                    if (!depths.containsKey(target.getId())) {
                        depths.put(target.getId(), currentDepth + 1);
                        queue.add(target);
                    }
                }
            }
        }

        return depths;
    }

    /**
     * Get the bounding rectangle of all nodes.
     */
    public Rectangle2D getAllNodesBounds() {
        if (signalGraph == null || signalGraph.getNodeCount() == 0) {
            return new Rectangle2D.Double(0, 0, 100, 100);
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (ProcessingNode node : signalGraph.getNodes()) {
            Point pos = getNodePosition(node.getId());
            int nodeHeight = getBlockHeight(node);
            minX = Math.min(minX, pos.x);
            minY = Math.min(minY, pos.y);
            maxX = Math.max(maxX, pos.x + BLOCK_WIDTH);
            maxY = Math.max(maxY, pos.y + nodeHeight);
        }

        return new Rectangle2D.Double(minX - 50, minY - 50, maxX - minX + 100, maxY - minY + 100);
    }

    // ==================== LISTENERS ====================

    public void addCanvasListener(CanvasListener listener) {
        listeners.add(listener);
    }

    public void removeCanvasListener(CanvasListener listener) {
        listeners.remove(listener);
    }

    private void fireCanvasChanged() {
        // Re-analyze signal flow when canvas changes
        analyzeSignalFlow();

        for (CanvasListener listener : listeners) {
            listener.canvasChanged();
        }
    }

    /**
     * Analyze signal flow through the graph.
     * Updates connection signal types (mono/stereo).
     */
    public void analyzeSignalFlow() {
        if (signalGraph != null) {
            signalFlowAnalyzer.analyze(signalGraph);
        }
    }

    /**
     * Get the signal type for a connection.
     *
     * @param connectionId The connection ID
     * @return The signal type (MONO, STEREO, or UNKNOWN)
     */
    public SignalFlowAnalyzer.SignalType getConnectionSignalType(String connectionId) {
        return signalFlowAnalyzer.getConnectionSignalType(connectionId);
    }

    /**
     * Check if a connection carries a stereo signal.
     */
    public boolean isConnectionStereo(String connectionId) {
        return signalFlowAnalyzer.isStereo(connectionId);
    }

    /**
     * Check if a connection carries a mono signal.
     */
    public boolean isConnectionMono(String connectionId) {
        return signalFlowAnalyzer.isMono(connectionId);
    }

    private void fireSelectionChanged() {
        for (CanvasListener listener : listeners) {
            listener.selectionChanged();
        }
    }

    /**
     * Listener interface for canvas changes.
     */
    public interface CanvasListener {
        void canvasChanged();
        void selectionChanged();
    }
}
