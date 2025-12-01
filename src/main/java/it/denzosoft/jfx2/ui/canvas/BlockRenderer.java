package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.icons.IconFactory;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Renders effect blocks on the canvas.
 */
public class BlockRenderer {

    private static final int ICON_SIZE = 14;
    private static final int LED_SIZE = 8;
    private static final int LED_MARGIN = 6;
    private final PortRenderer portRenderer;
    private static final JComponent ICON_COMPONENT = new JComponent() {};

    public BlockRenderer() {
        this.portRenderer = new PortRenderer();
    }

    /**
     * Render a single block.
     */
    public void render(Graphics2D g2d, ProcessingNode node, CanvasController controller) {
        Point pos = controller.getNodePosition(node.getId());
        boolean selected = controller.isNodeSelected(node.getId());
        boolean hovered = node.getId().equals(controller.getHoveredNode());
        boolean bypassed = node.isBypassed();

        int x = pos.x;
        int y = pos.y;
        int w = CanvasController.BLOCK_WIDTH;
        int h = CanvasController.getBlockHeight(node);
        int headerH = CanvasController.BLOCK_HEADER_HEIGHT;
        int radius = CanvasController.BLOCK_CORNER_RADIUS;

        // Get category color
        Color categoryColor = getCategoryColor(node);

        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw shadow
        if (!bypassed) {
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fill(new RoundRectangle2D.Double(x + 3, y + 3, w, h, radius, radius));
        }

        // Draw block background with gradient
        GradientPaint gradient = new GradientPaint(
                x, y, DarkTheme.BLOCK_BG_GRADIENT_TOP,
                x, y + h, DarkTheme.BLOCK_BG_GRADIENT_BOTTOM
        );
        g2d.setPaint(gradient);
        g2d.fill(new RoundRectangle2D.Double(x, y, w, h, radius, radius));

        // Draw header background
        g2d.setColor(categoryColor.darker().darker());
        g2d.fillRoundRect(x, y, w, headerH + radius, radius, radius);
        g2d.fillRect(x, y + headerH, w, radius);

        // Draw category color strip
        g2d.setColor(categoryColor);
        g2d.fillRect(x, y, 4, headerH);

        // Draw border
        if (selected) {
            // Selected: blue glow effect
            g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                    DarkTheme.ACCENT_PRIMARY.getGreen(),
                    DarkTheme.ACCENT_PRIMARY.getBlue(), 100));
            g2d.setStroke(new BasicStroke(4));
            g2d.draw(new RoundRectangle2D.Double(x - 2, y - 2, w + 4, h + 4, radius + 2, radius + 2));

            g2d.setColor(DarkTheme.BLOCK_BORDER_SELECTED);
            g2d.setStroke(new BasicStroke(2));
        } else if (hovered) {
            g2d.setColor(DarkTheme.ACCENT_SECONDARY);
            g2d.setStroke(new BasicStroke(1.5f));
        } else {
            g2d.setColor(DarkTheme.BLOCK_BORDER);
            g2d.setStroke(new BasicStroke(1));
        }
        g2d.draw(new RoundRectangle2D.Double(x, y, w, h, radius, radius));

        // Draw header icon
        Icon icon = getIcon(node);
        int iconX = x + 6;
        int iconY = y + (headerH - ICON_SIZE) / 2;
        icon.paintIcon(ICON_COMPONENT, g2d, iconX, iconY);

        // Draw header text
        g2d.setFont(DarkTheme.FONT_BOLD);
        g2d.setColor(DarkTheme.TEXT_PRIMARY);

        String name = truncateName(node.getName(), w - ICON_SIZE - 14);

        FontMetrics fm = g2d.getFontMetrics();
        int textX = iconX + ICON_SIZE + 4;
        int textY = y + (headerH + fm.getAscent() - fm.getDescent()) / 2;
        g2d.drawString(name, textX, textY);

        // Draw clipping LED in top-right corner
        boolean clipping = node.isClipping();
        int ledX = x + w - LED_SIZE - LED_MARGIN;
        int ledY = y + (headerH - LED_SIZE) / 2;

        if (clipping) {
            // Clipping: bright red LED with glow
            g2d.setColor(new Color(255, 0, 0, 80));
            g2d.fillOval(ledX - 3, ledY - 3, LED_SIZE + 6, LED_SIZE + 6);
            g2d.setColor(new Color(255, 50, 50));
            g2d.fillOval(ledX, ledY, LED_SIZE, LED_SIZE);
            // Highlight
            g2d.setColor(new Color(255, 150, 150));
            g2d.fillOval(ledX + 1, ledY + 1, LED_SIZE / 2, LED_SIZE / 2);
        } else {
            // No clipping: dim LED (dark red/gray)
            g2d.setColor(new Color(60, 20, 20));
            g2d.fillOval(ledX, ledY, LED_SIZE, LED_SIZE);
            g2d.setColor(new Color(80, 30, 30));
            g2d.fillOval(ledX + 1, ledY + 1, LED_SIZE / 2, LED_SIZE / 2);
        }
        // LED border
        g2d.setColor(new Color(40, 40, 40));
        g2d.setStroke(new BasicStroke(1));
        g2d.drawOval(ledX, ledY, LED_SIZE, LED_SIZE);

        // Draw bypass indicator
        if (bypassed) {
            // Draw "BYPASS" overlay
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fill(new RoundRectangle2D.Double(x, y, w, h, radius, radius));

            g2d.setFont(DarkTheme.FONT_SMALL);
            g2d.setColor(DarkTheme.ACCENT_WARNING);
            String bypassText = "BYPASSED";
            fm = g2d.getFontMetrics();
            int bypassX = x + (w - fm.stringWidth(bypassText)) / 2;
            int bypassY = y + h / 2 + fm.getAscent() / 2;
            g2d.drawString(bypassText, bypassX, bypassY);
        }

        // Draw ports
        for (Port port : node.getInputPorts()) {
            portRenderer.render(g2d, port, controller);
        }
        for (Port port : node.getOutputPorts()) {
            portRenderer.render(g2d, port, controller);
        }
    }

    /**
     * Get the icon for a node type.
     */
    private Icon getIcon(ProcessingNode node) {
        NodeType type = node.getNodeType();

        switch (type) {
            case INPUT -> { return IconFactory.getIcon("input", ICON_SIZE); }
            case OUTPUT -> { return IconFactory.getIcon("output", ICON_SIZE); }
            case SPLITTER -> {
                Color color = DarkTheme.CATEGORY_UTILITY;
                return IconFactory.getEffectIcon("splitter", color, ICON_SIZE);
            }
            case MIXER -> {
                Color color = DarkTheme.CATEGORY_UTILITY;
                return IconFactory.getEffectIcon("mixer", color, ICON_SIZE);
            }
            case EFFECT -> {
                // Use effect-specific icon with category color
                if (node instanceof EffectNode effectNode) {
                    var effect = effectNode.getEffect();
                    if (effect != null && effect.getMetadata() != null) {
                        String effectId = effect.getMetadata().id();
                        Color categoryColor = DarkTheme.getCategoryColor(effect.getMetadata().category().name());
                        return IconFactory.getEffectIcon(effectId, categoryColor, ICON_SIZE);
                    }
                }
                return IconFactory.getIcon("effect", ICON_SIZE);
            }
            default -> { return IconFactory.getIcon("effect", ICON_SIZE); }
        }
    }

    /**
     * Get the category color for a node.
     */
    private Color getCategoryColor(ProcessingNode node) {
        NodeType type = node.getNodeType();

        switch (type) {
            case INPUT -> { return DarkTheme.CATEGORY_INPUT; }
            case OUTPUT -> { return DarkTheme.CATEGORY_OUTPUT; }
            case SPLITTER, MIXER -> { return DarkTheme.CATEGORY_UTILITY; }
            case EFFECT -> {
                if (node instanceof EffectNode effectNode) {
                    var effect = effectNode.getEffect();
                    if (effect != null && effect.getMetadata() != null) {
                        return DarkTheme.getCategoryColor(effect.getMetadata().category().name());
                    }
                }
                return DarkTheme.CATEGORY_UTILITY;
            }
            default -> { return DarkTheme.CATEGORY_UTILITY; }
        }
    }

    /**
     * Truncate name to fit in available width.
     */
    private String truncateName(String name, int maxWidth) {
        if (name == null) return "";
        if (name.length() <= 12) return name;
        return name.substring(0, 10) + "...";
    }

    public PortRenderer getPortRenderer() {
        return portRenderer;
    }
}
