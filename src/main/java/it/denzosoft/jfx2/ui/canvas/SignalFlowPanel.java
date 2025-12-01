package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.ArrayList;

/**
 * Main canvas panel for the signal flow editor.
 * Supports zoom, pan, drag & drop, and connection creation.
 */
public class SignalFlowPanel extends JPanel {

    // ==================== ZOOM CONSTANTS ====================
    public static final double MIN_ZOOM = 0.25;
    public static final double MAX_ZOOM = 4.0;
    public static final double DEFAULT_ZOOM = 1.0;
    public static final double ZOOM_STEP = 0.1;

    // ==================== VIEW STATE ====================
    private double zoom = DEFAULT_ZOOM;
    private double panX = 0;
    private double panY = 0;

    // ==================== COMPONENTS ====================
    private final GridRenderer gridRenderer;
    private final CanvasController controller;
    private final BlockRenderer blockRenderer;
    private final ConnectionRenderer connectionRenderer;
    private final ConnectionDragHandler connectionDragHandler;
    private final BlockContextMenu blockContextMenu;
    private final ConnectionContextMenu connectionContextMenu;
    private SignalGraph signalGraph;

    // ==================== INTERACTION STATE ====================
    private Point lastMousePoint;
    private Point dragStartPoint;
    private boolean isPanning = false;
    private boolean spacePressed = false;
    private boolean isDraggingNode = false;
    private boolean isDraggingSelection = false;
    private boolean isDraggingConnection = false;
    private Rectangle2D selectionRect = null;
    private Point dropPreviewLocation = null;
    private String hoveredPortKey = null;

    // ==================== TRANSFORM ====================
    private AffineTransform viewTransform = new AffineTransform();
    private AffineTransform inverseTransform = new AffineTransform();

    public SignalFlowPanel() {
        this.gridRenderer = new GridRenderer();
        this.controller = new CanvasController();
        this.blockRenderer = new BlockRenderer();
        this.connectionRenderer = new ConnectionRenderer();
        this.connectionDragHandler = new ConnectionDragHandler(this, controller);
        this.blockContextMenu = new BlockContextMenu(this);
        this.connectionContextMenu = new ConnectionContextMenu(this);

        // Listen to controller changes
        controller.addCanvasListener(new CanvasController.CanvasListener() {
            @Override
            public void canvasChanged() {
                repaint();
            }

            @Override
            public void selectionChanged() {
                repaint();
                // Fire node selection if a node is selected
                if (!controller.hasConnectionSelection()) {
                    firePropertyChange("selection", null, controller.getFirstSelectedNode());
                    firePropertyChange("connectionSelection", null, null);
                } else {
                    // Fire connection selection if a connection is selected
                    firePropertyChange("selection", null, null);
                    firePropertyChange("connectionSelection", null, controller.getFirstSelectedConnection());
                }
            }
        });

        setBackground(DarkTheme.CANVAS_BG);
        setDoubleBuffered(true);
        setFocusable(true);

        setupMouseListeners();
        setupKeyListeners();

        // Setup drag & drop handler
        new CanvasDropHandler(this);

        // Center the view initially
        SwingUtilities.invokeLater(this::centerView);
    }

    // ==================== PAINTING ====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Set rendering hints
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            // Update transforms
            updateTransform();

            // Calculate visible area in world coordinates
            Rectangle2D visibleArea = getVisibleWorldArea();

            // Apply view transform
            g2d.transform(viewTransform);

            // Render grid
            gridRenderer.render(g2d, visibleArea, zoom);

            // Render graph elements (will be implemented in Phase 9)
            renderGraph(g2d);

            // Render overlays (selection, drag indicators, etc.)
            renderOverlays(g2d);

        } finally {
            g2d.dispose();
        }

        // Render HUD elements (zoom indicator, etc.) - not transformed
        renderHUD((Graphics2D) g);
    }

    /**
     * Update the view transform based on zoom and pan.
     */
    private void updateTransform() {
        viewTransform.setToIdentity();
        viewTransform.translate(getWidth() / 2.0, getHeight() / 2.0);
        viewTransform.scale(zoom, zoom);
        viewTransform.translate(-panX, -panY);

        try {
            inverseTransform = viewTransform.createInverse();
        } catch (NoninvertibleTransformException e) {
            inverseTransform.setToIdentity();
        }
    }

    /**
     * Get the visible area in world coordinates.
     */
    private Rectangle2D getVisibleWorldArea() {
        Point2D topLeft = screenToWorld(0, 0);
        Point2D bottomRight = screenToWorld(getWidth(), getHeight());

        return new Rectangle2D.Double(
                topLeft.getX(),
                topLeft.getY(),
                bottomRight.getX() - topLeft.getX(),
                bottomRight.getY() - topLeft.getY()
        );
    }

    /**
     * Render the signal graph.
     */
    private void renderGraph(Graphics2D g2d) {
        if (signalGraph == null || signalGraph.getNodeCount() == 0) {
            // Render placeholder message
            g2d.setColor(DarkTheme.TEXT_SECONDARY);
            g2d.setFont(DarkTheme.FONT_LARGE);

            String message = "Signal Flow Canvas";
            String hint = "Drag effects from palette to start";

            FontMetrics fm = g2d.getFontMetrics();
            int messageWidth = fm.stringWidth(message);
            g2d.drawString(message, -messageWidth / 2, -20);

            g2d.setFont(DarkTheme.FONT_SMALL);
            g2d.setColor(DarkTheme.TEXT_DISABLED);
            fm = g2d.getFontMetrics();
            int hintWidth = fm.stringWidth(hint);
            g2d.drawString(hint, -hintWidth / 2, 10);
            return;
        }

        // Render connections first (behind blocks)
        for (Connection conn : signalGraph.getConnections()) {
            connectionRenderer.render(g2d, conn, controller);
        }

        // Render blocks
        for (ProcessingNode node : signalGraph.getNodes()) {
            blockRenderer.render(g2d, node, controller);
        }
    }

    /**
     * Render overlays (selection rectangles, connection drag, drop preview).
     */
    private void renderOverlays(Graphics2D g2d) {
        // Draw selection rectangle if dragging
        if (selectionRect != null) {
            g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                    DarkTheme.ACCENT_PRIMARY.getGreen(),
                    DarkTheme.ACCENT_PRIMARY.getBlue(), 30));
            g2d.fill(selectionRect);

            g2d.setColor(DarkTheme.ACCENT_PRIMARY);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{5, 5}, 0));
            g2d.draw(selectionRect);
        }

        // Draw connection being dragged
        if (connectionDragHandler.isDragging()) {
            connectionDragHandler.render(g2d, connectionRenderer);
        }

        // Draw drop preview for effect drag & drop
        if (dropPreviewLocation != null) {
            int x = dropPreviewLocation.x;
            int y = dropPreviewLocation.y;
            int w = CanvasController.BLOCK_WIDTH;
            int h = CanvasController.BLOCK_HEIGHT;
            int r = CanvasController.BLOCK_CORNER_RADIUS;

            // Ghost block preview
            g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                    DarkTheme.ACCENT_PRIMARY.getGreen(),
                    DarkTheme.ACCENT_PRIMARY.getBlue(), 80));
            g2d.fill(new RoundRectangle2D.Double(x, y, w, h, r, r));

            g2d.setColor(DarkTheme.ACCENT_PRIMARY);
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10, new float[]{8, 4}, 0));
            g2d.draw(new RoundRectangle2D.Double(x, y, w, h, r, r));
        }
    }

    /**
     * Render HUD elements (not affected by zoom/pan).
     */
    private void renderHUD(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // Zoom indicator in bottom-right
        String zoomText = String.format("%.0f%%", zoom * 100);
        g2d.setFont(DarkTheme.FONT_SMALL);
        g2d.setColor(DarkTheme.TEXT_SECONDARY);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(zoomText);
        int x = getWidth() - textWidth - 10;
        int y = getHeight() - 10;

        // Background for readability
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(x - 6, y - fm.getAscent() - 2, textWidth + 12, fm.getHeight() + 4, 4, 4);

        g2d.setColor(DarkTheme.TEXT_SECONDARY);
        g2d.drawString(zoomText, x, y);

        // Pan position (for debugging)
        if (false) {  // Set to true for debug
            String panText = String.format("Pan: %.0f, %.0f", panX, panY);
            g2d.drawString(panText, 10, getHeight() - 10);
        }
    }

    // ==================== COORDINATE CONVERSION ====================

    /**
     * Convert screen coordinates to world coordinates.
     */
    public Point2D screenToWorld(int screenX, int screenY) {
        Point2D.Double screen = new Point2D.Double(screenX, screenY);
        Point2D.Double world = new Point2D.Double();
        inverseTransform.transform(screen, world);
        return world;
    }

    /**
     * Convert screen point to world coordinates.
     */
    public Point2D screenToWorld(Point screenPoint) {
        return screenToWorld(screenPoint.x, screenPoint.y);
    }

    /**
     * Convert world coordinates to screen coordinates.
     */
    public Point2D worldToScreen(double worldX, double worldY) {
        Point2D.Double world = new Point2D.Double(worldX, worldY);
        Point2D.Double screen = new Point2D.Double();
        viewTransform.transform(world, screen);
        return screen;
    }

    // ==================== ZOOM CONTROL ====================

    /**
     * Set zoom level, clamped to valid range.
     */
    public void setZoom(double newZoom) {
        this.zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        repaint();
    }

    /**
     * Zoom by a factor, centered on the given screen point.
     */
    public void zoomAt(double factor, int screenX, int screenY) {
        // Convert screen point to world before zoom
        Point2D worldPoint = screenToWorld(screenX, screenY);

        // Apply zoom
        double newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));

        if (newZoom != zoom) {
            zoom = newZoom;

            // Adjust pan to keep world point under cursor
            updateTransform();
            Point2D newScreenPoint = worldToScreen(worldPoint.getX(), worldPoint.getY());

            double dx = (screenX - newScreenPoint.getX()) / zoom;
            double dy = (screenY - newScreenPoint.getY()) / zoom;

            panX -= dx;
            panY -= dy;

            repaint();
            fireZoomChanged();
        }
    }

    /**
     * Zoom in centered on panel.
     */
    public void zoomIn() {
        zoomAt(1.0 + ZOOM_STEP, getWidth() / 2, getHeight() / 2);
    }

    /**
     * Zoom out centered on panel.
     */
    public void zoomOut() {
        zoomAt(1.0 - ZOOM_STEP, getWidth() / 2, getHeight() / 2);
    }

    /**
     * Reset zoom to 100%.
     */
    public void resetZoom() {
        zoom = DEFAULT_ZOOM;
        repaint();
        fireZoomChanged();
    }

    /**
     * Fit all content in view.
     */
    public void fitAll() {
        if (signalGraph == null || signalGraph.getNodeCount() == 0) {
            centerView();
            zoom = DEFAULT_ZOOM;
            repaint();
            fireZoomChanged();
            return;
        }

        // Get bounds of all nodes
        Rectangle2D bounds = controller.getAllNodesBounds();

        // Calculate zoom to fit
        double viewWidth = getWidth() - 100;  // Margin
        double viewHeight = getHeight() - 100;

        if (viewWidth <= 0 || viewHeight <= 0) {
            viewWidth = 800;
            viewHeight = 600;
        }

        double zoomX = viewWidth / bounds.getWidth();
        double zoomY = viewHeight / bounds.getHeight();
        zoom = Math.min(zoomX, zoomY);
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));

        // Center on bounds
        panX = bounds.getCenterX();
        panY = bounds.getCenterY();

        repaint();
        fireZoomChanged();
    }

    // ==================== PAN CONTROL ====================

    /**
     * Pan by delta in world coordinates.
     */
    public void pan(double dx, double dy) {
        panX += dx;
        panY += dy;
        repaint();
    }

    /**
     * Center the view on the origin.
     */
    public void centerView() {
        panX = 0;
        panY = 0;
        repaint();
    }

    /**
     * Center the view on a specific world point.
     */
    public void centerOn(double worldX, double worldY) {
        panX = worldX;
        panY = worldY;
        repaint();
    }

    // ==================== MOUSE LISTENERS ====================

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastMousePoint = e.getPoint();
                dragStartPoint = e.getPoint();

                Point2D worldPoint = screenToWorld(e.getX(), e.getY());
                int wx = (int) worldPoint.getX();
                int wy = (int) worldPoint.getY();

                // Right-click for context menu
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleContextMenu(e, wx, wy);
                    return;
                }

                // Middle button or Space+Left = pan
                if (SwingUtilities.isMiddleMouseButton(e) ||
                        (spacePressed && SwingUtilities.isLeftMouseButton(e))) {
                    isPanning = true;
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }

                // Left click - selection, dragging, and connection creation
                if (SwingUtilities.isLeftMouseButton(e)) {
                    // Check if clicked on a port first (for connection dragging)
                    Port clickedPort = controller.getPortAt(wx, wy);
                    if (clickedPort != null && clickedPort.getDirection() == PortDirection.OUTPUT) {
                        // Start connection drag from output port
                        if (connectionDragHandler.startDrag(clickedPort, new Point(wx, wy))) {
                            isDraggingConnection = true;
                            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                            return;
                        }
                    }

                    // Check if clicked on a node
                    ProcessingNode clickedNode = controller.getNodeAt(wx, wy);

                    if (clickedNode != null) {
                        if (e.isShiftDown()) {
                            // Shift+click: toggle selection
                            controller.toggleNodeSelection(clickedNode.getId());
                        } else if (!controller.isNodeSelected(clickedNode.getId())) {
                            // Click on unselected node: select it
                            controller.selectNode(clickedNode.getId());
                        }
                        // Start dragging
                        isDraggingNode = true;
                        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    } else {
                        // Check if clicked on a connection
                        Connection clickedConn = controller.getConnectionAt(wx, wy);

                        if (clickedConn != null) {
                            controller.selectConnection(clickedConn.getId());
                        } else {
                            // Click on empty space: start marquee selection or clear selection
                            if (!e.isShiftDown()) {
                                controller.clearSelection();
                            }
                            isDraggingSelection = true;
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (isPanning) {
                    isPanning = false;
                    setCursor(spacePressed
                            ? Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                            : Cursor.getDefaultCursor());
                }

                if (isDraggingConnection) {
                    boolean created = connectionDragHandler.endDrag();
                    isDraggingConnection = false;
                    setCursor(Cursor.getDefaultCursor());
                    if (created) {
                        firePropertyChange("connectionCreated", null, true);
                    }
                    repaint();
                }

                if (isDraggingNode) {
                    // Snap to grid on release
                    snapSelectedNodesToGrid();
                    isDraggingNode = false;
                    setCursor(Cursor.getDefaultCursor());
                }

                if (isDraggingSelection && selectionRect != null) {
                    // Select nodes in rectangle
                    controller.selectNodesInRect(selectionRect, e.isShiftDown());
                    selectionRect = null;
                    isDraggingSelection = false;
                    repaint();
                }

                dragStartPoint = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point2D worldPoint = screenToWorld(e.getX(), e.getY());
                int wx = (int) worldPoint.getX();
                int wy = (int) worldPoint.getY();

                if (isPanning && lastMousePoint != null) {
                    int dx = e.getX() - lastMousePoint.x;
                    int dy = e.getY() - lastMousePoint.y;

                    // Convert screen delta to world delta
                    panX -= dx / zoom;
                    panY -= dy / zoom;

                    lastMousePoint = e.getPoint();
                    repaint();
                    return;
                }

                if (isDraggingConnection) {
                    connectionDragHandler.updateDrag(new Point(wx, wy));
                    repaint();
                    return;
                }

                if (isDraggingNode && lastMousePoint != null) {
                    int dx = e.getX() - lastMousePoint.x;
                    int dy = e.getY() - lastMousePoint.y;

                    // Convert screen delta to world delta and move selected nodes
                    int worldDx = (int) (dx / zoom);
                    int worldDy = (int) (dy / zoom);
                    controller.moveSelectedNodes(worldDx, worldDy);

                    lastMousePoint = e.getPoint();
                    return;
                }

                if (isDraggingSelection && dragStartPoint != null) {
                    // Update selection rectangle
                    Point2D startWorld = screenToWorld(dragStartPoint.x, dragStartPoint.y);
                    Point2D endWorld = screenToWorld(e.getX(), e.getY());

                    double x = Math.min(startWorld.getX(), endWorld.getX());
                    double y = Math.min(startWorld.getY(), endWorld.getY());
                    double w = Math.abs(endWorld.getX() - startWorld.getX());
                    double h = Math.abs(endWorld.getY() - startWorld.getY());

                    selectionRect = new Rectangle2D.Double(x, y, w, h);
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                // Update hover state
                Point2D worldPoint = screenToWorld(e.getX(), e.getY());
                int wx = (int) worldPoint.getX();
                int wy = (int) worldPoint.getY();

                ProcessingNode hoveredNode = controller.getNodeAt(wx, wy);
                controller.setHoveredNode(hoveredNode != null ? hoveredNode.getId() : null);

                Connection hoveredConn = controller.getConnectionAt(wx, wy);
                controller.setHoveredConnection(hoveredConn != null ? hoveredConn.getId() : null);

                // Update port hover
                Port hoveredPort = controller.getPortAt(wx, wy);
                String newPortKey = hoveredPort != null ? PortRenderer.getPortKey(hoveredPort) : null;
                if (!java.util.Objects.equals(hoveredPortKey, newPortKey)) {
                    hoveredPortKey = newPortKey;
                    controller.setHoveredPort(hoveredPortKey);

                    // Change cursor when hovering over an output port
                    if (hoveredPort != null && hoveredPort.getDirection() == PortDirection.OUTPUT) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    } else if (!isDraggingConnection && !isDraggingNode && !isPanning) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int rotation = e.getWheelRotation();
                double factor = rotation < 0 ? (1.0 + ZOOM_STEP) : (1.0 - ZOOM_STEP);
                zoomAt(factor, e.getX(), e.getY());
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }

    /**
     * Handle right-click context menu.
     */
    private void handleContextMenu(MouseEvent e, int worldX, int worldY) {
        // Check if clicked on a node
        ProcessingNode clickedNode = controller.getNodeAt(worldX, worldY);
        if (clickedNode != null) {
            // Select node if not already selected
            if (!controller.isNodeSelected(clickedNode.getId())) {
                controller.selectNode(clickedNode.getId());
            }
            blockContextMenu.showForNode(clickedNode, this, e.getX(), e.getY());
            return;
        }

        // Check if clicked on a connection
        Connection clickedConn = controller.getConnectionAt(worldX, worldY);
        if (clickedConn != null) {
            controller.selectConnection(clickedConn.getId());
            connectionContextMenu.showForConnection(clickedConn, this, e.getX(), e.getY());
            return;
        }

        // Clicked on empty space - could show canvas context menu
    }

    /**
     * Snap selected nodes to grid.
     */
    private void snapSelectedNodesToGrid() {
        int gridSize = GridRenderer.GRID_SIZE;
        for (String nodeId : controller.getSelectedNodes()) {
            Point pos = controller.getNodePosition(nodeId);
            if (pos != null) {
                int snappedX = Math.round(pos.x / (float) gridSize) * gridSize;
                int snappedY = Math.round(pos.y / (float) gridSize) * gridSize;
                controller.setNodePosition(nodeId, new Point(snappedX, snappedY));
            }
        }
    }

    // ==================== KEY LISTENERS ====================

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE -> {
                        if (!spacePressed) {
                            spacePressed = true;
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        }
                    }
                    case KeyEvent.VK_DELETE, KeyEvent.VK_BACK_SPACE -> deleteSelectedNodes();
                    case KeyEvent.VK_B -> toggleBypassSelectedNodes();
                    case KeyEvent.VK_ESCAPE -> {
                        if (isDraggingConnection) {
                            connectionDragHandler.cancelDrag();
                            isDraggingConnection = false;
                            setCursor(Cursor.getDefaultCursor());
                            repaint();
                        } else {
                            controller.clearSelection();
                        }
                    }
                    case KeyEvent.VK_F2 -> renameSelectedNode();
                    case KeyEvent.VK_A -> {
                        if (e.isControlDown()) {
                            selectAllNodes();
                        }
                    }
                    case KeyEvent.VK_D -> {
                        if (e.isControlDown()) {
                            duplicateSelectedNodes();
                        }
                    }
                    case KeyEvent.VK_PLUS, KeyEvent.VK_EQUALS -> {
                        if (e.isControlDown()) {
                            zoomIn();
                        }
                    }
                    case KeyEvent.VK_MINUS -> {
                        if (e.isControlDown()) {
                            zoomOut();
                        }
                    }
                    case KeyEvent.VK_0 -> {
                        if (e.isControlDown()) {
                            resetZoom();
                        }
                    }
                    case KeyEvent.VK_1 -> {
                        if (e.isControlDown()) {
                            fitAll();
                        }
                    }
                    case KeyEvent.VK_LEFT -> pan(-50 / zoom, 0);
                    case KeyEvent.VK_RIGHT -> pan(50 / zoom, 0);
                    case KeyEvent.VK_UP -> pan(0, -50 / zoom);
                    case KeyEvent.VK_DOWN -> pan(0, 50 / zoom);
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    spacePressed = false;
                    if (!isPanning) {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        });
    }

    /**
     * Delete selected nodes and connections.
     */
    public void deleteSelectedNodes() {
        if (signalGraph == null) return;

        // Get selected nodes
        List<ProcessingNode> nodesToDelete = new ArrayList<>();
        for (String nodeId : controller.getSelectedNodes()) {
            ProcessingNode node = signalGraph.getNode(nodeId);
            if (node != null) {
                nodesToDelete.add(node);
            }
        }

        // Get selected connections
        List<Connection> connectionsToDelete = new ArrayList<>();
        for (String connId : controller.getSelectedConnections()) {
            for (Connection conn : signalGraph.getConnections()) {
                if (conn.getId().equals(connId)) {
                    connectionsToDelete.add(conn);
                    break;
                }
            }
        }

        // Nothing to delete
        if (nodesToDelete.isEmpty() && connectionsToDelete.isEmpty()) {
            return;
        }

        // Delete connections first
        for (Connection conn : connectionsToDelete) {
            signalGraph.disconnect(conn.getId());
        }

        // Delete nodes (SignalGraph.removeNode handles connections automatically)
        for (ProcessingNode node : nodesToDelete) {
            signalGraph.removeNode(node.getId());
        }

        controller.clearSelection();
        firePropertyChange("nodesDeleted", null, nodesToDelete.size());
        repaint();
    }

    /**
     * Toggle bypass on selected effect nodes.
     */
    private void toggleBypassSelectedNodes() {
        if (signalGraph == null) return;

        for (String nodeId : controller.getSelectedNodes()) {
            ProcessingNode node = signalGraph.getNode(nodeId);
            if (node != null && node.getNodeType() == NodeType.EFFECT) {
                node.setBypassed(!node.isBypassed());
            }
        }
        firePropertyChange("bypassChanged", null, true);
        repaint();
    }

    /**
     * Select all nodes.
     */
    public void selectAllNodes() {
        if (signalGraph == null) return;

        for (ProcessingNode node : signalGraph.getNodes()) {
            controller.addNodeToSelection(node.getId());
        }
    }

    /**
     * Duplicate selected nodes.
     */
    private void duplicateSelectedNodes() {
        if (signalGraph == null) return;

        List<String> selectedIds = new ArrayList<>(controller.getSelectedNodes());
        controller.clearSelection();

        for (String nodeId : selectedIds) {
            ProcessingNode node = signalGraph.getNode(nodeId);
            if (node instanceof EffectNode effectNode) {
                var effect = effectNode.getEffect();
                if (effect != null) {
                    var metadata = effect.getMetadata();
                    var newEffect = it.denzosoft.jfx2.effects.EffectFactory.getInstance().create(metadata.id());
                    if (newEffect != null) {
                        // Copy parameter values
                        for (var param : effect.getParameters()) {
                            var newParam = newEffect.getParameter(param.getId());
                            if (newParam != null) {
                                newParam.setValue(param.getValue());
                            }
                        }

                        // Create new node
                        String newId = generateUniqueId(nodeId);
                        EffectNode newNode = new EffectNode(newId, newEffect);
                        signalGraph.addNode(newNode);

                        // Position slightly offset from original
                        Point pos = controller.getNodePosition(nodeId);
                        Point newPos = new Point(pos.x + 40, pos.y + 40);
                        controller.setNodePosition(newId, newPos);
                        controller.addNodeToSelection(newId);
                    }
                }
            }
        }

        firePropertyChange("nodesDuplicated", null, true);
        repaint();
    }

    /**
     * Generate a unique node ID.
     */
    private String generateUniqueId(String baseId) {
        String id = baseId + "_copy";
        int counter = 1;
        while (signalGraph.getNode(id) != null) {
            id = baseId + "_copy" + counter++;
        }
        return id;
    }

    /**
     * Rename the selected node (F2 shortcut).
     */
    private void renameSelectedNode() {
        if (signalGraph == null) return;

        // Only rename if exactly one node is selected
        if (controller.getSelectedNodes().size() != 1) {
            return;
        }

        String nodeId = controller.getSelectedNodes().iterator().next();
        ProcessingNode node = signalGraph.getNode(nodeId);

        if (node == null) return;

        // Cannot rename Input/Output nodes
        if (node.getNodeType() == NodeType.INPUT || node.getNodeType() == NodeType.OUTPUT) {
            DarkTheme.showMessageDialog(this,
                    "Cannot rename Input/Output nodes",
                    "Rename Not Allowed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Get current display name
        String currentName = node.getName();

        // Show input dialog
        String newName = DarkTheme.showInputDialog(
                this,
                "Enter new name for block:",
                "Rename Block",
                currentName);

        // Update name if changed
        if (newName != null && !newName.trim().isEmpty() && !newName.equals(currentName)) {
            node.setName(newName.trim());
            firePropertyChange("nodeRenamed", currentName, newName);
            repaint();
        }
    }

    // ==================== EVENT FIRING ====================

    private void fireZoomChanged() {
        // Fire property change for zoom indicator updates
        firePropertyChange("zoom", null, zoom);
    }

    // ==================== GETTERS AND SETTERS ====================

    public double getZoom() {
        return zoom;
    }

    public double getPanX() {
        return panX;
    }

    public double getPanY() {
        return panY;
    }

    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
        repaint();
    }

    public SignalGraph getSignalGraph() {
        return signalGraph;
    }

    public void setSignalGraph(SignalGraph signalGraph) {
        this.signalGraph = signalGraph;
        controller.setSignalGraph(signalGraph);

        // Fit view to show all nodes
        if (signalGraph != null && signalGraph.getNodeCount() > 0) {
            SwingUtilities.invokeLater(this::fitAll);
        }
    }

    public GridRenderer getGridRenderer() {
        return gridRenderer;
    }

    public CanvasController getController() {
        return controller;
    }

    /**
     * Get the current view state for persistence.
     */
    public ViewState getViewState() {
        return new ViewState(zoom, panX, panY);
    }

    /**
     * Restore view state.
     */
    public void setViewState(ViewState state) {
        this.zoom = state.zoom();
        this.panX = state.panX();
        this.panY = state.panY();
        repaint();
        fireZoomChanged();
    }

    /**
     * Immutable view state for persistence.
     */
    public record ViewState(double zoom, double panX, double panY) {
        public static ViewState DEFAULT = new ViewState(1.0, 0, 0);
    }

    /**
     * Set the drop preview location for drag & drop.
     */
    public void setDropPreview(Point location) {
        this.dropPreviewLocation = location;
    }

    /**
     * Get the connection renderer (for drag handler).
     */
    public ConnectionRenderer getConnectionRenderer() {
        return connectionRenderer;
    }
}
