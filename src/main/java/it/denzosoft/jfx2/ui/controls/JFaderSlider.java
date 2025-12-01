package it.denzosoft.jfx2.ui.controls;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom fader slider control styled like professional mixing console faders.
 *
 * Features:
 * - Vertical fader with mixer-style cap/thumb
 * - Drag to change value
 * - Shift+drag for fine control
 * - Double-click to reset to default (center/0)
 * - Mouse wheel support
 * - LED-style track indicator
 */
public class JFaderSlider extends JComponent {

    // ==================== DIMENSIONS ====================
    private static final int DEFAULT_WIDTH = 36;
    private static final int DEFAULT_HEIGHT = 150;
    private static final int TRACK_WIDTH = 6;
    private static final int CAP_WIDTH = 28;
    private static final int CAP_HEIGHT = 18;
    private static final int CAP_CORNER = 3;
    private static final int LABEL_HEIGHT = 14;
    private static final int VALUE_HEIGHT = 12;
    private static final int NOTCH_WIDTH = 12;

    // ==================== VALUE ====================
    private double value;
    private double minValue;
    private double maxValue;
    private double defaultValue;
    private String unit;

    // ==================== INTERACTION ====================
    private int dragStartY;
    private double dragStartValue;
    private boolean dragging = false;
    private double valueBeforeDrag;  // Value when drag started
    private static final double DRAG_SENSITIVITY = 1.0;
    private static final double FINE_SENSITIVITY = 0.1;

    // ==================== APPEARANCE ====================
    private Color trackColor = DarkTheme.BG_DARK;
    private Color trackFillColor = DarkTheme.ACCENT_SUCCESS;
    private Color capColor = new Color(0x3A3A3A);
    private Color capHighlightColor = new Color(0x4A4A4A);
    private Color capShadowColor = new Color(0x2A2A2A);
    private Color notchColor = new Color(0x606060);
    private boolean showValue = true;
    private boolean bipolar = true;  // Show fill from center for EQ

    public JFaderSlider() {
        this(-12, 12, 0, "dB");
    }

    public JFaderSlider(double min, double max, double defaultVal, String unit) {
        this.minValue = min;
        this.maxValue = max;
        this.defaultValue = defaultVal;
        this.value = defaultVal;
        this.unit = unit;

        setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT + LABEL_HEIGHT + VALUE_HEIGHT));
        setMinimumSize(getPreferredSize());
        setOpaque(false);
        setFocusable(true);

        setupMouseListeners();
        setupKeyListeners();
        updateTooltip();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();

                // Check if clicked on the cap
                int capY = getCapY();
                int clickY = e.getY();

                if (clickY >= capY && clickY <= capY + CAP_HEIGHT) {
                    // Clicked on cap - start dragging
                    dragging = true;
                    dragStartY = e.getY();
                    dragStartValue = value;
                    valueBeforeDrag = value;  // Store value before drag
                } else {
                    // Clicked on track - jump to position
                    setValueFromY(e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging) {
                    dragging = false;
                    // Fire event only on release, if value changed
                    if (valueBeforeDrag != value) {
                        firePropertyChange("value", valueBeforeDrag, value);
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    setValue(defaultValue);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int deltaY = dragStartY - e.getY();
                    double sensitivity = e.isShiftDown() ? FINE_SENSITIVITY : DRAG_SENSITIVITY;
                    double range = maxValue - minValue;
                    int trackHeight = getTrackHeight();
                    double delta = (deltaY * sensitivity * range) / trackHeight;
                    // Update value silently during drag
                    setValueSilent(dragStartValue + delta);
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double range = maxValue - minValue;
                double step = range / 48.0;  // Finer steps for faders
                if (e.isShiftDown()) step /= 10.0;
                setValue(value - e.getWheelRotation() * step);
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
        addMouseWheelListener(mouseAdapter);
    }

    private void setupKeyListeners() {
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                double range = maxValue - minValue;
                double step = range / 48.0;
                if (e.isShiftDown()) step /= 10.0;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP -> setValue(value + step);
                    case KeyEvent.VK_DOWN -> setValue(value - step);
                    case KeyEvent.VK_HOME -> setValue(maxValue);
                    case KeyEvent.VK_END -> setValue(minValue);
                    case KeyEvent.VK_ESCAPE -> setValue(defaultValue);
                }
            }
        });
    }

    private void setValueFromY(int y) {
        int trackTop = VALUE_HEIGHT + CAP_HEIGHT / 2;
        int trackHeight = getTrackHeight();
        double normalized = 1.0 - (double)(y - trackTop) / trackHeight;
        normalized = Math.max(0, Math.min(1, normalized));
        setValue(minValue + normalized * (maxValue - minValue));
    }

    private int getTrackHeight() {
        return getHeight() - VALUE_HEIGHT - LABEL_HEIGHT - CAP_HEIGHT;
    }

    private int getCapY() {
        int trackTop = VALUE_HEIGHT + CAP_HEIGHT / 2;
        int trackHeight = getTrackHeight();
        double normalized = (value - minValue) / (maxValue - minValue);
        int capCenterY = trackTop + (int)((1.0 - normalized) * trackHeight);
        return capCenterY - CAP_HEIGHT / 2;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int centerX = w / 2;

        // Calculate positions
        int trackTop = VALUE_HEIGHT + CAP_HEIGHT / 2;
        int trackHeight = getTrackHeight();
        int trackBottom = trackTop + trackHeight;

        // Draw value at top
        if (showValue) {
            drawValue(g2d, centerX);
        }

        // Draw track background
        drawTrack(g2d, centerX, trackTop, trackHeight);

        // Draw track fill (LED indicator)
        drawTrackFill(g2d, centerX, trackTop, trackHeight);

        // Draw tick marks
        drawTickMarks(g2d, centerX, trackTop, trackHeight);

        // Draw fader cap
        drawCap(g2d, centerX);

        g2d.dispose();
    }

    private void drawValue(Graphics2D g2d, int centerX) {
        String valueText;
        if (Math.abs(value) < 0.05) {
            valueText = "0";
        } else if (value > 0) {
            valueText = "+" + String.format("%.0f", value);
        } else {
            valueText = String.format("%.0f", value);
        }

        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
        FontMetrics fm = g2d.getFontMetrics();

        // Color based on value
        if (value > 0.5) {
            g2d.setColor(new Color(0x4ECDC4));  // Teal for boost
        } else if (value < -0.5) {
            g2d.setColor(new Color(0xFF6B6B));  // Red for cut
        } else {
            g2d.setColor(DarkTheme.TEXT_SECONDARY);
        }

        int textX = centerX - fm.stringWidth(valueText) / 2;
        g2d.drawString(valueText, textX, fm.getAscent());
    }

    private void drawTrack(Graphics2D g2d, int centerX, int trackTop, int trackHeight) {
        int trackX = centerX - TRACK_WIDTH / 2;

        // Track slot (recessed look)
        g2d.setColor(new Color(0x1A1A1A));
        g2d.fillRoundRect(trackX - 1, trackTop - 1, TRACK_WIDTH + 2, trackHeight + 2, 3, 3);

        g2d.setColor(trackColor);
        g2d.fillRoundRect(trackX, trackTop, TRACK_WIDTH, trackHeight, 2, 2);
    }

    private void drawTrackFill(Graphics2D g2d, int centerX, int trackTop, int trackHeight) {
        int trackX = centerX - TRACK_WIDTH / 2 + 1;
        int fillWidth = TRACK_WIDTH - 2;

        double normalized = (value - minValue) / (maxValue - minValue);
        int valueY = trackTop + (int)((1.0 - normalized) * trackHeight);

        if (bipolar) {
            // Fill from center
            int centerY = trackTop + trackHeight / 2;

            if (value > 0) {
                // Boost - fill from center upward
                int fillHeight = centerY - valueY;
                if (fillHeight > 0) {
                    // Gradient for LED effect
                    GradientPaint gradient = new GradientPaint(
                            trackX, valueY, new Color(0x4ECDC4),
                            trackX + fillWidth, valueY, new Color(0x3AAA9A)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(trackX, valueY, fillWidth, fillHeight);
                }
            } else if (value < 0) {
                // Cut - fill from center downward
                int fillHeight = valueY - centerY;
                if (fillHeight > 0) {
                    GradientPaint gradient = new GradientPaint(
                            trackX, centerY, new Color(0xFF6B6B),
                            trackX + fillWidth, centerY, new Color(0xCC5555)
                    );
                    g2d.setPaint(gradient);
                    g2d.fillRect(trackX, centerY, fillWidth, fillHeight);
                }
            }

            // Center line marker
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.drawLine(centerX - 8, centerY, centerX + 8, centerY);
        } else {
            // Fill from bottom
            int fillHeight = trackTop + trackHeight - valueY;
            if (fillHeight > 0) {
                g2d.setColor(trackFillColor);
                g2d.fillRect(trackX, valueY, fillWidth, fillHeight);
            }
        }
    }

    private void drawTickMarks(Graphics2D g2d, int centerX, int trackTop, int trackHeight) {
        g2d.setColor(new Color(255, 255, 255, 40));

        // Draw tick marks at regular intervals
        int numTicks = 5;  // -12, -6, 0, +6, +12
        for (int i = 0; i <= numTicks - 1; i++) {
            int y = trackTop + (i * trackHeight) / (numTicks - 1);
            int tickWidth = (i == numTicks / 2) ? 10 : 6;  // Longer tick at center
            g2d.drawLine(centerX - tickWidth, y, centerX - TRACK_WIDTH / 2 - 2, y);
            g2d.drawLine(centerX + TRACK_WIDTH / 2 + 2, y, centerX + tickWidth, y);
        }
    }

    private void drawCap(Graphics2D g2d, int centerX) {
        int capY = getCapY();
        int capX = centerX - CAP_WIDTH / 2;

        // Cap shadow
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fill(new RoundRectangle2D.Double(capX + 2, capY + 2, CAP_WIDTH, CAP_HEIGHT, CAP_CORNER, CAP_CORNER));

        // Cap body with gradient
        GradientPaint capGradient = new GradientPaint(
                capX, capY, capHighlightColor,
                capX, capY + CAP_HEIGHT, capShadowColor
        );
        g2d.setPaint(capGradient);
        g2d.fill(new RoundRectangle2D.Double(capX, capY, CAP_WIDTH, CAP_HEIGHT, CAP_CORNER, CAP_CORNER));

        // Cap border
        g2d.setColor(new Color(0x252525));
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new RoundRectangle2D.Double(capX, capY, CAP_WIDTH, CAP_HEIGHT, CAP_CORNER, CAP_CORNER));

        // Center notch/grip (the indicator line on the fader cap)
        int notchY = capY + CAP_HEIGHT / 2;
        g2d.setColor(notchColor);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(centerX - NOTCH_WIDTH / 2, notchY, centerX + NOTCH_WIDTH / 2, notchY);

        // Highlight lines on cap for 3D effect
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawLine(capX + 2, capY + 2, capX + CAP_WIDTH - 3, capY + 2);
        g2d.drawLine(capX + 2, capY + 2, capX + 2, capY + CAP_HEIGHT - 3);
    }

    private void updateTooltip() {
        String valStr = String.format("%.1f", value);
        if (unit != null && !unit.isEmpty()) {
            valStr += " " + unit;
        }
        setToolTipText(valStr + " (default: " + String.format("%.1f", defaultValue) + ")");
    }

    // ==================== GETTERS AND SETTERS ====================

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        double oldValue = this.value;
        this.value = Math.max(minValue, Math.min(maxValue, value));
        if (oldValue != this.value) {
            updateTooltip();
            repaint();
            firePropertyChange("value", oldValue, this.value);
        }
    }

    /**
     * Set value without firing property change event.
     * Used during dragging for visual feedback only.
     */
    private void setValueSilent(double value) {
        this.value = Math.max(minValue, Math.min(maxValue, value));
        updateTooltip();
        repaint();
    }

    public double getNormalizedValue() {
        if (maxValue == minValue) return 0;
        return (value - minValue) / (maxValue - minValue);
    }

    public void setNormalizedValue(double normalized) {
        setValue(minValue + normalized * (maxValue - minValue));
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
        repaint();
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        repaint();
    }

    public double getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(double defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setTrackFillColor(Color color) {
        this.trackFillColor = color;
        repaint();
    }

    public void setBipolar(boolean bipolar) {
        this.bipolar = bipolar;
        repaint();
    }

    public void setShowValue(boolean showValue) {
        this.showValue = showValue;
        repaint();
    }

    public void reset() {
        setValue(defaultValue);
    }
}
