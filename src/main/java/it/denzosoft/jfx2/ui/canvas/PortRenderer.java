package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.graph.Port;
import it.denzosoft.jfx2.graph.PortDirection;
import it.denzosoft.jfx2.graph.PortType;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Renders ports on effect blocks.
 * Ports are color-coded by type (audio = green, control = orange).
 */
public class PortRenderer {

    private static final int PORT_RADIUS = CanvasController.PORT_RADIUS;
    private static final int GLOW_RADIUS = PORT_RADIUS + 4;

    public PortRenderer() {
    }

    /**
     * Render a port.
     */
    public void render(Graphics2D g2d, Port port, CanvasController controller) {
        Point pos = controller.getPortPosition(port);
        boolean hovered = controller.isPortHovered(getPortKey(port));
        boolean connected = port.isConnected();

        int x = pos.x;
        int y = pos.y;

        // Get port color based on type
        Color portColor = getPortColor(port.getType());
        Color hoverColor = getPortHoverColor(port.getType());

        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw glow if hovered
        if (hovered) {
            g2d.setColor(new Color(portColor.getRed(), portColor.getGreen(), portColor.getBlue(), 80));
            g2d.fill(new Ellipse2D.Double(
                    x - GLOW_RADIUS, y - GLOW_RADIUS,
                    GLOW_RADIUS * 2, GLOW_RADIUS * 2
            ));
        }

        // Draw port background
        if (connected) {
            // Filled circle for connected ports
            g2d.setColor(hovered ? hoverColor : portColor);
            g2d.fill(new Ellipse2D.Double(
                    x - PORT_RADIUS, y - PORT_RADIUS,
                    PORT_RADIUS * 2, PORT_RADIUS * 2
            ));
        } else {
            // Empty circle with border for unconnected ports
            g2d.setColor(DarkTheme.BG_LIGHT);
            g2d.fill(new Ellipse2D.Double(
                    x - PORT_RADIUS, y - PORT_RADIUS,
                    PORT_RADIUS * 2, PORT_RADIUS * 2
            ));

            g2d.setColor(hovered ? hoverColor : portColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new Ellipse2D.Double(
                    x - PORT_RADIUS, y - PORT_RADIUS,
                    PORT_RADIUS * 2, PORT_RADIUS * 2
            ));
        }

        // Draw port border
        g2d.setColor(DarkTheme.PORT_BORDER);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new Ellipse2D.Double(
                x - PORT_RADIUS, y - PORT_RADIUS,
                PORT_RADIUS * 2, PORT_RADIUS * 2
        ));

        // Draw direction indicator (small triangle)
        if (hovered) {
            drawDirectionIndicator(g2d, port, x, y, portColor);
        }
    }

    /**
     * Render a port being dragged (for connection creation).
     */
    public void renderDragPort(Graphics2D g2d, int x, int y, PortType type, boolean valid) {
        Color color = valid ? DarkTheme.CONNECTION_VALID : DarkTheme.CONNECTION_INVALID;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Glow
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
        g2d.fill(new Ellipse2D.Double(
                x - GLOW_RADIUS, y - GLOW_RADIUS,
                GLOW_RADIUS * 2, GLOW_RADIUS * 2
        ));

        // Port circle
        g2d.setColor(color);
        g2d.fill(new Ellipse2D.Double(
                x - PORT_RADIUS, y - PORT_RADIUS,
                PORT_RADIUS * 2, PORT_RADIUS * 2
        ));

        // Border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(new Ellipse2D.Double(
                x - PORT_RADIUS, y - PORT_RADIUS,
                PORT_RADIUS * 2, PORT_RADIUS * 2
        ));
    }

    /**
     * Draw a small triangle indicating port direction.
     */
    private void drawDirectionIndicator(Graphics2D g2d, Port port, int cx, int cy, Color color) {
        int size = 4;
        int offset = PORT_RADIUS + 6;

        int[] xPoints, yPoints;

        if (port.getDirection() == PortDirection.INPUT) {
            // Triangle pointing right (into the port)
            int baseX = cx - offset - size;
            xPoints = new int[]{baseX, baseX, baseX + size};
            yPoints = new int[]{cy - size / 2, cy + size / 2, cy};
        } else {
            // Triangle pointing right (out of the port)
            int baseX = cx + offset;
            xPoints = new int[]{baseX, baseX, baseX + size};
            yPoints = new int[]{cy - size / 2, cy + size / 2, cy};
        }

        g2d.setColor(color);
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Get the color for a port type.
     */
    public Color getPortColor(PortType type) {
        return switch (type) {
            case AUDIO_MONO, AUDIO_STEREO -> DarkTheme.PORT_AUDIO;
        };
    }

    /**
     * Get the hover color for a port type.
     */
    public Color getPortHoverColor(PortType type) {
        return switch (type) {
            case AUDIO_MONO, AUDIO_STEREO -> DarkTheme.PORT_AUDIO_HOVER;
        };
    }

    /**
     * Get a unique key for a port (for hover tracking).
     */
    public static String getPortKey(Port port) {
        return port.getOwner().getId() + ":" + port.getId();
    }
}
