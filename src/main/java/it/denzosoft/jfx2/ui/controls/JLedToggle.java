package it.denzosoft.jfx2.ui.controls;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;

/**
 * Toggle button with LED indicator.
 *
 * Features:
 * - LED that glows when active
 * - Label displayed next to LED
 * - Click to toggle
 */
public class JLedToggle extends JComponent {

    // ==================== DIMENSIONS ====================
    private static final int LED_SIZE = 16;
    private static final int PADDING = 4;
    private static final int GAP = 8;

    // ==================== STATE ====================
    private boolean selected;
    private String label;
    private String onText = "ON";
    private String offText = "OFF";

    // ==================== COLORS ====================
    private Color ledOnColor = DarkTheme.ACCENT_SUCCESS;
    private Color ledOffColor = DarkTheme.BG_LIGHT;
    private Color glowColor = new Color(ledOnColor.getRed(), ledOnColor.getGreen(), ledOnColor.getBlue(), 100);

    // ==================== INTERACTION ====================
    private boolean hovered = false;
    private boolean pressed = false;

    public JLedToggle() {
        this("", false);
    }

    public JLedToggle(String label) {
        this(label, false);
    }

    public JLedToggle(String label, boolean selected) {
        this.label = label;
        this.selected = selected;

        setOpaque(false);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        calculatePreferredSize();
        setupListeners();
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

    private void setupListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressed && contains(e.getPoint())) {
                    setSelected(!selected);
                }
                pressed = false;
                repaint();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    setSelected(!selected);
                }
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
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

        // Draw glow when selected
        if (selected) {
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
        Color ledColor = selected ? ledOnColor : ledOffColor;
        if (hovered) {
            ledColor = ledColor.brighter();
        }
        if (pressed) {
            ledColor = ledColor.darker();
        }

        // Gradient for 3D effect
        GradientPaint gradient = new GradientPaint(
                ledX, ledY, ledColor.brighter(),
                ledX, ledY + LED_SIZE, ledColor
        );
        g2d.setPaint(gradient);
        g2d.fill(new Ellipse2D.Double(ledX, ledY, LED_SIZE, LED_SIZE));

        // Highlight spot on LED
        if (selected) {
            g2d.setColor(new Color(255, 255, 255, 80));
            g2d.fill(new Ellipse2D.Double(ledX + 3, ledY + 3, 5, 5));
        }

        // Draw LED border
        g2d.setColor(DarkTheme.BG_LIGHT.darker());
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new Ellipse2D.Double(ledX, ledY, LED_SIZE, LED_SIZE));

        // Draw label
        if (label != null && !label.isEmpty()) {
            g2d.setFont(DarkTheme.FONT_REGULAR);
            g2d.setColor(hovered ? DarkTheme.TEXT_PRIMARY : DarkTheme.TEXT_SECONDARY);
            FontMetrics fm = g2d.getFontMetrics();
            int textX = ledX + LED_SIZE + GAP;
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(label, textX, textY);
        }

        // Draw focus indicator
        if (hasFocus()) {
            g2d.setColor(DarkTheme.ACCENT_PRIMARY);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10, new float[]{2, 2}, 0));
            g2d.drawRect(1, 1, w - 3, h - 3);
        }

        g2d.dispose();
    }

    // ==================== GETTERS AND SETTERS ====================

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        boolean oldValue = this.selected;
        this.selected = selected;
        if (oldValue != selected) {
            repaint();
            firePropertyChange("selected", oldValue, selected);
        }
    }

    public void toggle() {
        setSelected(!selected);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        calculatePreferredSize();
        repaint();
    }

    public String getOnText() {
        return onText;
    }

    public void setOnText(String onText) {
        this.onText = onText;
    }

    public String getOffText() {
        return offText;
    }

    public void setOffText(String offText) {
        this.offText = offText;
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
