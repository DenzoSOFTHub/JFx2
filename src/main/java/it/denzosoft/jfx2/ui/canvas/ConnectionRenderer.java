package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.Connection;
import it.denzosoft.jfx2.graph.Port;
import it.denzosoft.jfx2.graph.PortType;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;

/**
 * Renders connections between ports as smooth Bezier curves.
 */
public class ConnectionRenderer {

    private static final float LINE_WIDTH = 3.0f;
    private static final float LINE_WIDTH_SELECTED = 4.0f;
    private static final float LINE_WIDTH_HOVER = 3.5f;

    private static final int ARROW_SIZE = 8;
    private static final int MIN_CONTROL_OFFSET = 50;

    public ConnectionRenderer() {
    }

    /**
     * Render a connection.
     */
    public void render(Graphics2D g2d, Connection connection, CanvasController controller) {
        Point start = controller.getPortPosition(connection.getSourcePort());
        Point end = controller.getPortPosition(connection.getTargetPort());

        boolean selected = controller.isConnectionSelected(connection.getId());
        boolean hovered = connection.getId().equals(controller.getHoveredConnection());

        // Get connection color based on signal type
        Color color;
        float width;

        if (selected) {
            color = DarkTheme.CONNECTION_SELECTED;
            width = LINE_WIDTH_SELECTED;
        } else if (hovered) {
            color = DarkTheme.CONNECTION_SELECTED;
            width = LINE_WIDTH_HOVER;
        } else {
            // Use signal type to determine color (mono vs stereo)
            if (controller.isConnectionStereo(connection.getId())) {
                color = DarkTheme.CONNECTION_STEREO;
            } else if (controller.isConnectionMono(connection.getId())) {
                color = DarkTheme.CONNECTION_MONO;
            } else {
                color = DarkTheme.CONNECTION_NORMAL;
            }
            width = LINE_WIDTH;
        }

        // Draw the curve
        renderCurve(g2d, start, end, color, width, selected || hovered);
    }

    /**
     * Render a connection being dragged (preview).
     */
    public void renderDragConnection(Graphics2D g2d, Point start, Point end, boolean valid) {
        Color color = valid ? DarkTheme.CONNECTION_VALID : DarkTheme.CONNECTION_INVALID;
        renderCurve(g2d, start, end, color, LINE_WIDTH, true);
    }

    /**
     * Render a bezier curve between two points.
     */
    private void renderCurve(Graphics2D g2d, Point start, Point end, Color color, float width, boolean showGlow) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Calculate control points for smooth S-curve
        int dx = Math.abs(end.x - start.x) / 2;
        dx = Math.max(dx, MIN_CONTROL_OFFSET);

        // If end is to the left of start, we need to curve around
        if (end.x < start.x) {
            dx = Math.max(dx, Math.abs(end.x - start.x) + MIN_CONTROL_OFFSET);
        }

        CubicCurve2D curve = new CubicCurve2D.Double(
                start.x, start.y,           // Start point
                start.x + dx, start.y,      // Control point 1
                end.x - dx, end.y,          // Control point 2
                end.x, end.y                // End point
        );

        // Draw glow/shadow if selected or hovered
        if (showGlow) {
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            g2d.setStroke(new BasicStroke(width + 6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(curve);
        }

        // Draw main curve with gradient
        GradientPaint gradient = new GradientPaint(
                start.x, start.y, color,
                end.x, end.y, color.brighter()
        );
        g2d.setPaint(gradient);
        g2d.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(curve);

        // Draw arrow at end point
        drawArrow(g2d, end, curve, color);
    }

    /**
     * Draw an arrow at the end of the connection.
     */
    private void drawArrow(Graphics2D g2d, Point end, CubicCurve2D curve, Color color) {
        // Calculate direction from control point to end
        double dx = end.x - curve.getCtrlX2();
        double dy = end.y - curve.getCtrlY2();
        double len = Math.sqrt(dx * dx + dy * dy);

        if (len == 0) return;

        // Normalize
        dx /= len;
        dy /= len;

        // Arrow points
        int arrowSize = ARROW_SIZE;
        double perpX = -dy;
        double perpY = dx;

        int x1 = (int) (end.x - dx * arrowSize - perpX * arrowSize / 2);
        int y1 = (int) (end.y - dy * arrowSize - perpY * arrowSize / 2);
        int x2 = (int) (end.x - dx * arrowSize + perpX * arrowSize / 2);
        int y2 = (int) (end.y - dy * arrowSize + perpY * arrowSize / 2);

        // Draw arrow
        g2d.setColor(color);
        int[] xPoints = {end.x, x1, x2};
        int[] yPoints = {end.y, y1, y2};
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Render a "potential" connection when hovering over a valid port.
     */
    public void renderPotentialConnection(Graphics2D g2d, Port sourcePort, Point mousePos,
                                           boolean valid, CanvasController controller) {
        Point start = controller.getPortPosition(sourcePort);
        Color color = new Color(255, 255, 255, 100);

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Dashed line
        float[] dashPattern = {8, 8};
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                10, dashPattern, 0));
        g2d.setColor(color);

        int dx = Math.abs(mousePos.x - start.x) / 2;
        dx = Math.max(dx, MIN_CONTROL_OFFSET);

        CubicCurve2D curve = new CubicCurve2D.Double(
                start.x, start.y,
                start.x + dx, start.y,
                mousePos.x - dx, mousePos.y,
                mousePos.x, mousePos.y
        );

        g2d.draw(curve);
    }

    /**
     * Get a cubic curve for a connection (for hit testing).
     */
    public CubicCurve2D getCurve(Point start, Point end) {
        int dx = Math.abs(end.x - start.x) / 2;
        dx = Math.max(dx, MIN_CONTROL_OFFSET);

        if (end.x < start.x) {
            dx = Math.max(dx, Math.abs(end.x - start.x) + MIN_CONTROL_OFFSET);
        }

        return new CubicCurve2D.Double(
                start.x, start.y,
                start.x + dx, start.y,
                end.x - dx, end.y,
                end.x, end.y
        );
    }
}
