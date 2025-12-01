package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.canvas.CanvasController;
import it.denzosoft.jfx2.ui.canvas.SignalFlowPanel;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.beans.PropertyChangeListener;

/**
 * Minimap panel showing an overview of the signal graph.
 * Allows quick navigation by clicking or dragging.
 */
public class MinimapPanel extends JPanel {

    private static final int PREFERRED_WIDTH = 180;
    private static final int PREFERRED_HEIGHT = 120;
    private static final int MARGIN = 10;
    private static final int NODE_SIZE = 8;

    private SignalFlowPanel canvas;
    private CanvasController controller;

    // Cached bounds
    private Rectangle2D worldBounds;
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;

    // Interaction
    private boolean isDragging = false;
    private PropertyChangeListener canvasListener;

    public MinimapPanel() {
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        setMinimumSize(new Dimension(120, 80));
        setBackground(DarkTheme.BG_DARK);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DarkTheme.BG_LIGHT),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        setupMouseListeners();
    }

    /**
     * Set the canvas to display in minimap.
     */
    public void setCanvas(SignalFlowPanel canvas) {
        // Remove old listener
        if (this.canvas != null && canvasListener != null) {
            this.canvas.removePropertyChangeListener(canvasListener);
        }

        this.canvas = canvas;
        this.controller = canvas != null ? canvas.getController() : null;

        // Add listener to repaint when canvas changes
        if (canvas != null) {
            canvasListener = e -> repaint();
            canvas.addPropertyChangeListener("zoom", canvasListener);
            canvas.addPropertyChangeListener("selection", canvasListener);

            // Also listen to controller changes
            if (controller != null) {
                controller.addCanvasListener(new CanvasController.CanvasListener() {
                    @Override
                    public void canvasChanged() {
                        repaint();
                    }

                    @Override
                    public void selectionChanged() {
                        repaint();
                    }
                });
            }
        }

        repaint();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (canvas == null) return;
                navigateToPoint(e.getX(), e.getY());
                isDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                isDragging = false;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isDragging && canvas != null) {
                    navigateToPoint(e.getX(), e.getY());
                }
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    /**
     * Navigate the main canvas to center on the clicked point.
     */
    private void navigateToPoint(int screenX, int screenY) {
        if (canvas == null || worldBounds == null) return;

        // Convert screen to minimap-relative coordinates
        int mapX = screenX - MARGIN;
        int mapY = screenY - MARGIN;
        int mapWidth = getWidth() - MARGIN * 2;
        int mapHeight = getHeight() - MARGIN * 2;

        // Convert to world coordinates
        double worldX = worldBounds.getMinX() + (mapX / scale);
        double worldY = worldBounds.getMinY() + (mapY / scale);

        // Center the canvas on this point
        canvas.centerOn(worldX, worldY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int mapWidth = w - MARGIN * 2;
        int mapHeight = h - MARGIN * 2;

        // Background
        g2d.setColor(DarkTheme.BG_MEDIUM);
        g2d.fillRect(MARGIN, MARGIN, mapWidth, mapHeight);

        if (canvas == null || controller == null) {
            // Show placeholder
            g2d.setColor(DarkTheme.TEXT_DISABLED);
            g2d.setFont(DarkTheme.FONT_SMALL);
            String text = "No canvas";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2);
            g2d.dispose();
            return;
        }

        SignalGraph graph = canvas.getSignalGraph();
        if (graph == null || graph.getNodeCount() == 0) {
            g2d.setColor(DarkTheme.TEXT_DISABLED);
            g2d.setFont(DarkTheme.FONT_SMALL);
            String text = "Empty";
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(text, (w - fm.stringWidth(text)) / 2, h / 2);
            g2d.dispose();
            return;
        }

        // Calculate world bounds
        worldBounds = controller.getAllNodesBounds();
        if (worldBounds.getWidth() <= 0 || worldBounds.getHeight() <= 0) {
            worldBounds = new Rectangle2D.Double(-100, -100, 200, 200);
        }

        // Add some padding to world bounds
        double padding = 50;
        worldBounds = new Rectangle2D.Double(
                worldBounds.getMinX() - padding,
                worldBounds.getMinY() - padding,
                worldBounds.getWidth() + padding * 2,
                worldBounds.getHeight() + padding * 2
        );

        // Calculate scale to fit
        double scaleX = mapWidth / worldBounds.getWidth();
        double scaleY = mapHeight / worldBounds.getHeight();
        scale = Math.min(scaleX, scaleY);

        // Calculate offset to center
        double scaledWidth = worldBounds.getWidth() * scale;
        double scaledHeight = worldBounds.getHeight() * scale;
        offsetX = MARGIN + (mapWidth - scaledWidth) / 2;
        offsetY = MARGIN + (mapHeight - scaledHeight) / 2;

        // Transform for drawing
        AffineTransform originalTransform = g2d.getTransform();
        g2d.translate(offsetX, offsetY);
        g2d.scale(scale, scale);
        g2d.translate(-worldBounds.getMinX(), -worldBounds.getMinY());

        // Draw connections
        g2d.setColor(DarkTheme.CONNECTION_NORMAL.darker());
        g2d.setStroke(new BasicStroke((float) (1 / scale)));
        for (Connection conn : graph.getConnections()) {
            Point sourcePos = controller.getNodePosition(conn.getSourcePort().getOwner().getId());
            Point targetPos = controller.getNodePosition(conn.getTargetPort().getOwner().getId());

            if (sourcePos != null && targetPos != null) {
                int x1 = sourcePos.x + CanvasController.BLOCK_WIDTH / 2;
                int y1 = sourcePos.y + CanvasController.BLOCK_HEIGHT / 2;
                int x2 = targetPos.x + CanvasController.BLOCK_WIDTH / 2;
                int y2 = targetPos.y + CanvasController.BLOCK_HEIGHT / 2;
                g2d.drawLine(x1, y1, x2, y2);
            }
        }

        // Draw nodes
        for (ProcessingNode node : graph.getNodes()) {
            Point pos = controller.getNodePosition(node.getId());
            if (pos == null) continue;

            // Determine color based on node type and state
            Color nodeColor = getNodeColor(node);

            // Check if selected
            boolean selected = controller.isNodeSelected(node.getId());
            if (selected) {
                nodeColor = DarkTheme.ACCENT_PRIMARY;
            }

            // Draw node as small rectangle
            int x = pos.x + CanvasController.BLOCK_WIDTH / 2 - (int) (NODE_SIZE / scale / 2);
            int y = pos.y + CanvasController.BLOCK_HEIGHT / 2 - (int) (NODE_SIZE / scale / 2);
            int size = (int) (NODE_SIZE / scale);

            g2d.setColor(nodeColor);
            g2d.fillRect(x, y, size, size);

            if (selected) {
                g2d.setColor(DarkTheme.TEXT_PRIMARY);
                g2d.setStroke(new BasicStroke((float) (1 / scale)));
                g2d.drawRect(x - 1, y - 1, size + 2, size + 2);
            }
        }

        // Reset transform
        g2d.setTransform(originalTransform);

        // Draw viewport rectangle
        drawViewport(g2d, mapWidth, mapHeight);

        // Draw border
        g2d.setColor(DarkTheme.BG_LIGHT);
        g2d.drawRect(MARGIN, MARGIN, mapWidth - 1, mapHeight - 1);

        g2d.dispose();
    }

    /**
     * Draw the viewport rectangle showing the visible area.
     */
    private void drawViewport(Graphics2D g2d, int mapWidth, int mapHeight) {
        if (canvas == null || worldBounds == null) return;

        // Get visible area from canvas
        double zoom = canvas.getZoom();
        double panX = canvas.getPanX();
        double panY = canvas.getPanY();
        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        // Calculate visible world area
        double visibleWidth = canvasWidth / zoom;
        double visibleHeight = canvasHeight / zoom;
        double visibleX = panX - visibleWidth / 2;
        double visibleY = panY - visibleHeight / 2;

        // Convert to minimap coordinates
        double vpX = offsetX + (visibleX - worldBounds.getMinX()) * scale;
        double vpY = offsetY + (visibleY - worldBounds.getMinY()) * scale;
        double vpW = visibleWidth * scale;
        double vpH = visibleHeight * scale;

        // Clip to map area
        double minX = MARGIN;
        double minY = MARGIN;
        double maxX = MARGIN + mapWidth;
        double maxY = MARGIN + mapHeight;

        vpX = Math.max(minX, Math.min(maxX - vpW, vpX));
        vpY = Math.max(minY, Math.min(maxY - vpH, vpY));
        vpW = Math.min(maxX - vpX, vpW);
        vpH = Math.min(maxY - vpY, vpH);

        // Draw viewport rectangle
        g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                DarkTheme.ACCENT_PRIMARY.getGreen(),
                DarkTheme.ACCENT_PRIMARY.getBlue(), 50));
        g2d.fill(new Rectangle2D.Double(vpX, vpY, vpW, vpH));

        g2d.setColor(DarkTheme.ACCENT_PRIMARY);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.draw(new Rectangle2D.Double(vpX, vpY, vpW, vpH));
    }

    /**
     * Get color for a node based on its type.
     */
    private Color getNodeColor(ProcessingNode node) {
        return switch (node.getNodeType()) {
            case INPUT -> DarkTheme.CATEGORY_INPUT;
            case OUTPUT -> DarkTheme.CATEGORY_OUTPUT;
            case EFFECT -> {
                if (node.isBypassed()) {
                    yield DarkTheme.TEXT_DISABLED;
                }
                if (node instanceof EffectNode effectNode) {
                    var effect = effectNode.getEffect();
                    if (effect != null) {
                        EffectCategory cat = effect.getMetadata().category();
                        if (cat == EffectCategory.DISTORTION) yield DarkTheme.CATEGORY_DRIVE;
                        else if (cat == EffectCategory.MODULATION) yield DarkTheme.CATEGORY_MODULATION;
                        else if (cat == EffectCategory.DELAY || cat == EffectCategory.REVERB) yield DarkTheme.CATEGORY_TIME;
                        else if (cat == EffectCategory.DYNAMICS) yield DarkTheme.CATEGORY_DYNAMICS;
                        else if (cat == EffectCategory.EQ || cat == EffectCategory.FILTER) yield DarkTheme.CATEGORY_EQ;
                        else yield DarkTheme.BLOCK_BG;
                    }
                }
                yield DarkTheme.BLOCK_BG;
            }
            case SPLITTER, MIXER -> DarkTheme.CATEGORY_UTILITY;
            default -> DarkTheme.BLOCK_BG;
        };
    }
}
