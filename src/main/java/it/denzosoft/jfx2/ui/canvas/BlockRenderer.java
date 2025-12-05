package it.denzosoft.jfx2.ui.canvas;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.EffectCategory;
import it.denzosoft.jfx2.graph.*;
import it.denzosoft.jfx2.ui.icons.IconFactory;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Renders effect blocks on the canvas as realistic guitar pedals seen from above.
 *
 * <p>Inspired by Headrush Prime's visual style with:
 * <ul>
 *   <li>3D pedal enclosure appearance</li>
 *   <li>Metallic/powder-coated finish</li>
 *   <li>Footswitch with LED indicator</li>
 *   <li>Mini knob indicators</li>
 *   <li>Clear bypass state visualization</li>
 * </ul>
 * </p>
 */
public class BlockRenderer {

    // Pedal dimensions
    private static final int PEDAL_CORNER_RADIUS = 8;
    private static final int PEDAL_BEVEL = 3;

    // LED dimensions
    private static final int LED_SIZE = 10;
    private static final int LED_GLOW_SIZE = 16;

    // Screw dimensions
    private static final int SCREW_SIZE = 6;

    private final PortRenderer portRenderer;
    private static final JComponent ICON_COMPONENT = new JComponent() {};

    public BlockRenderer() {
        this.portRenderer = new PortRenderer();
    }

    /**
     * Render a single block as a guitar pedal.
     */
    public void render(Graphics2D g2d, ProcessingNode node, CanvasController controller) {
        Point pos = controller.getNodePosition(node.getId());
        boolean selected = controller.isNodeSelected(node.getId());
        boolean hovered = node.getId().equals(controller.getHoveredNode());
        boolean bypassed = node.isBypassed();
        boolean clipping = node.isClipping();

        int x = pos.x;
        int y = pos.y;
        int w = CanvasController.BLOCK_WIDTH;
        int h = CanvasController.getBlockHeight(node);

        // Get category color for pedal finish
        Color categoryColor = getCategoryColor(node);

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw pedal enclosure
        drawPedalEnclosure(g2d, x, y, w, h, categoryColor, selected, hovered, bypassed);

        // Draw clipping LED in top-right corner
        drawLED(g2d, x, y, w, h, clipping, bypassed);

        // Draw effect name plate (multi-line)
        drawNamePlate(g2d, x, y, w, h, node.getName(), bypassed);

        // Draw footswitch area
        drawFootswitch(g2d, x, y, w, h, bypassed);

        // Draw corner screws (decorative)
        drawCornerScrews(g2d, x, y, w, h);

        // Draw selection glow
        if (selected) {
            drawSelectionGlow(g2d, x, y, w, h);
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
     * Draw the pedal enclosure with 3D metallic appearance.
     */
    private void drawPedalEnclosure(Graphics2D g2d, int x, int y, int w, int h,
                                     Color baseColor, boolean selected, boolean hovered, boolean bypassed) {

        // Adjust color for bypass
        Color pedalColor = bypassed ? desaturate(baseColor, 0.3f) : baseColor;

        // === SHADOW ===
        g2d.setColor(new Color(0, 0, 0, bypassed ? 30 : 80));
        g2d.fill(new RoundRectangle2D.Double(x + 4, y + 4, w, h, PEDAL_CORNER_RADIUS, PEDAL_CORNER_RADIUS));

        // === PEDAL BASE (darker bottom edge for 3D effect) ===
        Color darkEdge = pedalColor.darker().darker();
        g2d.setColor(darkEdge);
        g2d.fill(new RoundRectangle2D.Double(x, y + PEDAL_BEVEL, w, h, PEDAL_CORNER_RADIUS, PEDAL_CORNER_RADIUS));

        // === MAIN PEDAL BODY ===
        // Gradient from top-left (lighter) to bottom-right (darker) for metallic look
        GradientPaint metalGradient = new GradientPaint(
                x, y, brighten(pedalColor, 1.2f),
                x + w, y + h, pedalColor.darker()
        );
        g2d.setPaint(metalGradient);
        g2d.fill(new RoundRectangle2D.Double(x, y, w, h - PEDAL_BEVEL, PEDAL_CORNER_RADIUS, PEDAL_CORNER_RADIUS));

        // === TOP HIGHLIGHT (simulates lighting) ===
        GradientPaint highlight = new GradientPaint(
                x, y, new Color(255, 255, 255, bypassed ? 20 : 50),
                x, y + h * 0.3f, new Color(255, 255, 255, 0)
        );
        g2d.setPaint(highlight);
        g2d.fill(new RoundRectangle2D.Double(x + 2, y + 2, w - 4, h * 0.3f, PEDAL_CORNER_RADIUS - 2, PEDAL_CORNER_RADIUS - 2));

        // === BORDER ===
        g2d.setColor(selected ? DarkTheme.ACCENT_PRIMARY : (hovered ? DarkTheme.ACCENT_SECONDARY : darkEdge));
        g2d.setStroke(new BasicStroke(selected ? 2.5f : (hovered ? 2f : 1f)));
        g2d.draw(new RoundRectangle2D.Double(x, y, w, h - PEDAL_BEVEL, PEDAL_CORNER_RADIUS, PEDAL_CORNER_RADIUS));

        // === INNER BEVEL LINE ===
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new RoundRectangle2D.Double(x + 2, y + 2, w - 4, h - PEDAL_BEVEL - 4, PEDAL_CORNER_RADIUS - 2, PEDAL_CORNER_RADIUS - 2));
    }

    /**
     * Draw the effect name plate with multi-line support (up to 3 lines).
     */
    private void drawNamePlate(Graphics2D g2d, int x, int y, int w, int h, String name, boolean bypassed) {
        int plateY = y + 18;
        int plateH = 50; // Taller for 3 lines
        int plateMargin = 6;

        // Name plate background (darker inset area)
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.fillRoundRect(x + plateMargin, plateY, w - plateMargin * 2, plateH, 4, 4);

        // Inner highlight
        g2d.setColor(new Color(255, 255, 255, 20));
        g2d.drawRoundRect(x + plateMargin + 1, plateY + 1, w - plateMargin * 2 - 2, plateH - 2, 3, 3);

        // Effect name (multi-line, centered) - always white for readability
        g2d.setFont(DarkTheme.FONT_BOLD);
        g2d.setColor(DarkTheme.TEXT_PRIMARY);

        FontMetrics fm = g2d.getFontMetrics();
        int lineHeight = fm.getHeight();
        int maxWidth = w - plateMargin * 2 - 8;

        // Split name into lines
        String[] lines = wrapText(name, fm, maxWidth);
        int numLines = Math.min(lines.length, 3);

        // Calculate starting Y to center the text block
        int totalTextHeight = numLines * lineHeight;
        int startTextY = plateY + (plateH - totalTextHeight) / 2 + fm.getAscent();

        for (int i = 0; i < numLines; i++) {
            String line = lines[i];
            int textX = x + (w - fm.stringWidth(line)) / 2;
            int textY = startTextY + i * lineHeight;
            g2d.drawString(line, textX, textY);
        }
    }

    /**
     * Wrap text into multiple lines based on max width.
     */
    private String[] wrapText(String text, FontMetrics fm, int maxWidth) {
        if (text == null || text.isEmpty()) return new String[]{""};

        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split("(?<=.)(?=\\p{Upper})|\\s+|(?<=-)"); // Split on camelCase, spaces, hyphens

        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + word;
            if (fm.stringWidth(testLine) <= maxWidth) {
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder(word);
                } else {
                    // Word is too long, truncate it
                    lines.add(truncateWord(word, fm, maxWidth));
                    currentLine = new StringBuilder();
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }

        return lines.toArray(new String[0]);
    }

    /**
     * Truncate a single word to fit max width.
     */
    private String truncateWord(String word, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(word) <= maxWidth) return word;
        for (int i = word.length() - 1; i > 0; i--) {
            String truncated = word.substring(0, i) + "..";
            if (fm.stringWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        return "..";
    }

    /**
     * Draw a realistic chrome footswitch with 3D dome appearance.
     */
    private void drawFootswitch(Graphics2D g2d, int x, int y, int w, int h, boolean bypassed) {
        int switchSize = 28;
        int switchX = x + (w - switchSize) / 2;
        int switchY = y + h - switchSize - 14 - PEDAL_BEVEL;

        // 3D effect - switch is "up" when bypassed, "down" when active
        int pressOffset = bypassed ? 0 : 3;

        // === OUTER RING (mounting bezel) ===
        int bezelSize = switchSize + 8;
        int bezelX = switchX - 4;
        int bezelY = switchY - 4;

        // Bezel shadow (depth)
        g2d.setColor(new Color(10, 10, 12));
        g2d.fillOval(bezelX, bezelY + 2, bezelSize, bezelSize);

        // Bezel body - brushed metal look
        GradientPaint bezelGradient = new GradientPaint(
                bezelX, bezelY, new Color(70, 72, 75),
                bezelX + bezelSize, bezelY + bezelSize, new Color(40, 42, 45)
        );
        g2d.setPaint(bezelGradient);
        g2d.fillOval(bezelX, bezelY, bezelSize, bezelSize);

        // Bezel inner edge highlight
        g2d.setColor(new Color(90, 92, 95));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(bezelX + 1, bezelY + 1, bezelSize - 2, bezelSize - 2);

        // === RECESSED AREA (hole for the switch) ===
        g2d.setColor(new Color(15, 15, 18));
        g2d.fillOval(switchX - 2, switchY - 2, switchSize + 4, switchSize + 4);

        // === SWITCH DOME (chrome button) ===
        int domeX = switchX;
        int domeY = switchY + pressOffset;

        // Chrome base - dark to light gradient for 3D dome effect
        RadialGradientPaint chromeGradient = new RadialGradientPaint(
                domeX + switchSize * 0.35f, domeY + switchSize * 0.3f,
                switchSize * 0.8f,
                new float[]{0f, 0.3f, 0.6f, 1f},
                new Color[]{
                        new Color(200, 205, 210),  // Bright highlight
                        new Color(140, 145, 150),  // Mid chrome
                        new Color(80, 85, 90),     // Darker edge
                        new Color(50, 55, 60)      // Edge shadow
                }
        );
        g2d.setPaint(chromeGradient);
        g2d.fillOval(domeX, domeY, switchSize, switchSize);

        // === DOME HIGHLIGHT (specular reflection) ===
        // Main bright spot (top-left, simulating light source)
        RadialGradientPaint highlightGradient = new RadialGradientPaint(
                domeX + switchSize * 0.3f, domeY + switchSize * 0.25f,
                switchSize * 0.3f,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                        new Color(255, 255, 255, bypassed ? 120 : 200),
                        new Color(255, 255, 255, bypassed ? 40 : 80),
                        new Color(255, 255, 255, 0)
                }
        );
        g2d.setPaint(highlightGradient);
        g2d.fillOval(domeX + 2, domeY + 2, switchSize - 4, switchSize - 4);

        // === DOME EDGE RING (rim highlight) ===
        g2d.setColor(new Color(120, 125, 130));
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawOval(domeX, domeY, switchSize, switchSize);

        // Inner edge shadow
        g2d.setColor(new Color(40, 42, 45));
        g2d.drawOval(domeX + 1, domeY + 1, switchSize - 2, switchSize - 2);

        // === CONCENTRIC RIDGES (grip texture on dome) ===
        g2d.setStroke(new BasicStroke(0.5f));
        g2d.setColor(new Color(0, 0, 0, 30));
        for (int r = 6; r < switchSize / 2 - 2; r += 3) {
            g2d.drawOval(domeX + switchSize / 2 - r, domeY + switchSize / 2 - r + pressOffset / 2, r * 2, r * 2);
        }

        // === SECONDARY HIGHLIGHT (bottom right reflection) ===
        g2d.setColor(new Color(255, 255, 255, bypassed ? 15 : 30));
        g2d.fillOval(domeX + switchSize / 2 + 2, domeY + switchSize / 2 + 2,
                switchSize / 4, switchSize / 4);
    }

    /**
     * Draw the LED indicator in top-right corner.
     */
    private void drawLED(Graphics2D g2d, int x, int y, int w, int h, boolean clipping, boolean bypassed) {
        // LED positioned in top-right corner (offset left to avoid screw overlap)
        int ledX = x + w - LED_SIZE - 16;
        int ledY = y + 8;

        if (!bypassed) {
            if (clipping) {
                // === CLIPPING: Bright red LED with glow ===
                // Outer glow
                RadialGradientPaint glow = new RadialGradientPaint(
                        ledX + LED_SIZE / 2f, ledY + LED_SIZE / 2f,
                        LED_GLOW_SIZE,
                        new float[]{0f, 0.5f, 1f},
                        new Color[]{new Color(255, 0, 0, 150), new Color(255, 0, 0, 50), new Color(255, 0, 0, 0)}
                );
                g2d.setPaint(glow);
                g2d.fillOval(ledX - 3, ledY - 3, LED_SIZE + 6, LED_SIZE + 6);

                // LED body
                GradientPaint ledGradient = new GradientPaint(
                        ledX, ledY, new Color(255, 100, 100),
                        ledX, ledY + LED_SIZE, new Color(220, 0, 0)
                );
                g2d.setPaint(ledGradient);
                g2d.fillOval(ledX, ledY, LED_SIZE, LED_SIZE);

                // LED highlight
                g2d.setColor(new Color(255, 200, 200));
                g2d.fillOval(ledX + 2, ledY + 2, LED_SIZE / 3, LED_SIZE / 3);
            } else {
                // === ACTIVE: Green LED ===
                // Outer glow
                RadialGradientPaint glow = new RadialGradientPaint(
                        ledX + LED_SIZE / 2f, ledY + LED_SIZE / 2f,
                        LED_GLOW_SIZE,
                        new float[]{0f, 0.5f, 1f},
                        new Color[]{new Color(0, 255, 0, 100), new Color(0, 255, 0, 30), new Color(0, 255, 0, 0)}
                );
                g2d.setPaint(glow);
                g2d.fillOval(ledX - 3, ledY - 3, LED_SIZE + 6, LED_SIZE + 6);

                // LED body
                GradientPaint ledGradient = new GradientPaint(
                        ledX, ledY, new Color(100, 255, 100),
                        ledX, ledY + LED_SIZE, new Color(0, 200, 0)
                );
                g2d.setPaint(ledGradient);
                g2d.fillOval(ledX, ledY, LED_SIZE, LED_SIZE);

                // LED highlight
                g2d.setColor(new Color(200, 255, 200));
                g2d.fillOval(ledX + 2, ledY + 2, LED_SIZE / 3, LED_SIZE / 3);
            }
        } else {
            // === BYPASSED: Dim LED ===
            g2d.setColor(new Color(40, 30, 30));
            g2d.fillOval(ledX, ledY, LED_SIZE, LED_SIZE);
            g2d.setColor(new Color(60, 40, 40));
            g2d.fillOval(ledX + 2, ledY + 2, LED_SIZE / 3, LED_SIZE / 3);
        }

        // LED bezel
        g2d.setColor(new Color(30, 30, 30));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(ledX - 1, ledY - 1, LED_SIZE + 2, LED_SIZE + 2);
    }

    /**
     * Draw decorative corner screws.
     */
    private void drawCornerScrews(Graphics2D g2d, int x, int y, int w, int h) {
        int margin = 6;
        int[][] screwPositions = {
                {x + margin, y + margin},
                {x + w - margin - SCREW_SIZE, y + margin},
                {x + margin, y + h - margin - SCREW_SIZE - PEDAL_BEVEL},
                {x + w - margin - SCREW_SIZE, y + h - margin - SCREW_SIZE - PEDAL_BEVEL}
        };

        for (int[] pos : screwPositions) {
            // Screw hole
            g2d.setColor(new Color(20, 20, 20));
            g2d.fillOval(pos[0], pos[1], SCREW_SIZE, SCREW_SIZE);

            // Screw head
            GradientPaint screwGradient = new GradientPaint(
                    pos[0], pos[1], new Color(100, 100, 105),
                    pos[0], pos[1] + SCREW_SIZE, new Color(60, 60, 65)
            );
            g2d.setPaint(screwGradient);
            g2d.fillOval(pos[0] + 1, pos[1] + 1, SCREW_SIZE - 2, SCREW_SIZE - 2);

            // Screw slot (Phillips-style cross)
            g2d.setColor(new Color(40, 40, 40));
            g2d.setStroke(new BasicStroke(1));
            int cx = pos[0] + SCREW_SIZE / 2;
            int cy = pos[1] + SCREW_SIZE / 2;
            g2d.drawLine(cx - 2, cy, cx + 2, cy);
            g2d.drawLine(cx, cy - 2, cx, cy + 2);
        }
    }

    /**
     * Draw selection glow effect.
     */
    private void drawSelectionGlow(Graphics2D g2d, int x, int y, int w, int h) {
        // Outer glow
        g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                DarkTheme.ACCENT_PRIMARY.getGreen(),
                DarkTheme.ACCENT_PRIMARY.getBlue(), 60));
        g2d.setStroke(new BasicStroke(6));
        g2d.draw(new RoundRectangle2D.Double(x - 3, y - 3, w + 6, h - PEDAL_BEVEL + 6,
                PEDAL_CORNER_RADIUS + 3, PEDAL_CORNER_RADIUS + 3));
    }

    /**
     * Get the icon for a node type.
     */
    private Icon getIcon(ProcessingNode node) {
        NodeType type = node.getNodeType();

        switch (type) {
            case INPUT -> { return IconFactory.getIcon("input", 14); }
            case OUTPUT -> { return IconFactory.getIcon("output", 14); }
            case SPLITTER -> {
                Color color = DarkTheme.CATEGORY_UTILITY;
                return IconFactory.getEffectIcon("splitter", color, 14);
            }
            case MIXER -> {
                Color color = DarkTheme.CATEGORY_UTILITY;
                return IconFactory.getEffectIcon("mixer", color, 14);
            }
            case EFFECT -> {
                if (node instanceof EffectNode effectNode) {
                    var effect = effectNode.getEffect();
                    if (effect != null && effect.getMetadata() != null) {
                        String effectId = effect.getMetadata().id();
                        Color categoryColor = DarkTheme.getCategoryColor(effect.getMetadata().category().name());
                        return IconFactory.getEffectIcon(effectId, categoryColor, 14);
                    }
                }
                return IconFactory.getIcon("effect", 14);
            }
            default -> { return IconFactory.getIcon("effect", 14); }
        }
    }

    /**
     * Get the category color for a node.
     */
    private Color getCategoryColor(ProcessingNode node) {
        NodeType type = node.getNodeType();

        switch (type) {
            case INPUT -> { return DarkTheme.CATEGORY_INPUT_SOURCE; }
            case OUTPUT -> { return DarkTheme.CATEGORY_OUTPUT_SINK; }
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
     * Truncate name to fit in narrow pedal.
     */
    private String truncateName(String name, int maxWidth) {
        if (name == null) return "";
        if (name.length() <= 10) return name;
        return name.substring(0, 8) + "..";
    }

    /**
     * Brighten a color.
     */
    private Color brighten(Color c, float factor) {
        int r = Math.min(255, (int)(c.getRed() * factor));
        int g = Math.min(255, (int)(c.getGreen() * factor));
        int b = Math.min(255, (int)(c.getBlue() * factor));
        return new Color(r, g, b);
    }

    /**
     * Desaturate a color (move toward gray).
     */
    private Color desaturate(Color c, float amount) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[1] *= amount; // Reduce saturation
        hsb[2] *= 0.7f;   // Also darken slightly
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }

    public PortRenderer getPortRenderer() {
        return portRenderer;
    }
}
