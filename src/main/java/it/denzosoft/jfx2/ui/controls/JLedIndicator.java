package it.denzosoft.jfx2.ui.controls;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Read-only LED indicator with label.
 *
 * Similar to JLedToggle but not interactive - used for status display.
 */
public class JLedIndicator extends JComponent {

    // ==================== DIMENSIONS ====================
    private static final int LED_SIZE = 16;
    private static final int PADDING = 4;
    private static final int GAP = 8;

    // ==================== STATE ====================
    private boolean active;
    private String label;

    // ==================== COLORS ====================
    private Color ledOnColor = DarkTheme.ACCENT_ERROR;  // Red for clipping
    private Color ledOffColor = new Color(60, 20, 20);  // Dark red when off
    private Color glowColor = new Color(255, 0, 0, 100);

    public JLedIndicator() {
        this("", false);
    }

    public JLedIndicator(String label) {
        this(label, false);
    }

    public JLedIndicator(String label, boolean active) {
        this.label = label;
        this.active = active;

        setOpaque(false);
        calculatePreferredSize();
    }

    private void calculatePreferredSize() {
        FontMetrics fm = getFontMetrics(DarkTheme.FONT_REGULAR);
        int textWidth = 0;
        if (label != null && !label.isEmpty()) {
            textWidth = fm.stringWidth(label) + GAP;
        }
        int width = PADDING * 2 + LED_SIZE + textWidth;
        int height = PADDING * 2 + Math.max(LED_SIZE, fm.getHeight());
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(getPreferredSize());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Calculate LED position
        int ledX = PADDING;
        int ledY = (h - LED_SIZE) / 2;

        // Draw glow when active
        if (active) {
            g2d.setColor(glowColor);
            int glowSize = LED_SIZE + 8;
            g2d.fill(new Ellipse2D.Double(
                    ledX - 4, ledY - 4, glowSize, glowSize
            ));
        }

        // Draw LED background (outer ring)
        g2d.setColor(DarkTheme.BG_DARK);
        g2d.fill(new Ellipse2D.Double(ledX - 1, ledY - 1, LED_SIZE + 2, LED_SIZE + 2));

        // Draw LED
        Color ledColor = active ? ledOnColor : ledOffColor;

        // Gradient for 3D effect
        GradientPaint gradient = new GradientPaint(
                ledX, ledY, ledColor.brighter(),
                ledX, ledY + LED_SIZE, ledColor
        );
        g2d.setPaint(gradient);
        g2d.fill(new Ellipse2D.Double(ledX, ledY, LED_SIZE, LED_SIZE));

        // Highlight spot on LED
        if (active) {
            g2d.setColor(new Color(255, 200, 200, 120));
            g2d.fill(new Ellipse2D.Double(ledX + 3, ledY + 3, 5, 5));
        } else {
            g2d.setColor(new Color(100, 50, 50, 80));
            g2d.fill(new Ellipse2D.Double(ledX + 3, ledY + 3, 4, 4));
        }

        // Draw LED border
        g2d.setColor(DarkTheme.BG_LIGHT.darker());
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new Ellipse2D.Double(ledX, ledY, LED_SIZE, LED_SIZE));

        // Draw label
        if (label != null && !label.isEmpty()) {
            g2d.setFont(DarkTheme.FONT_REGULAR);
            g2d.setColor(active ? ledOnColor : DarkTheme.TEXT_SECONDARY);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = ledX + LED_SIZE + GAP;
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(label, textX, textY);
        }

        g2d.dispose();
    }

    // ==================== GETTERS AND SETTERS ====================

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (this.active != active) {
            this.active = active;
            repaint();
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        calculatePreferredSize();
        repaint();
    }

    public Color getLedOnColor() {
        return ledOnColor;
    }

    public void setLedOnColor(Color color) {
        this.ledOnColor = color;
        this.glowColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 100);
        repaint();
    }

    public Color getLedOffColor() {
        return ledOffColor;
    }

    public void setLedOffColor(Color color) {
        this.ledOffColor = color;
        repaint();
    }
}
