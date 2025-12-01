package it.denzosoft.jfx2.ui.controls;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;

/**
 * Custom rotary knob control with arc indicator.
 *
 * Features:
 * - Vertical drag to change value
 * - Shift+drag for fine control
 * - Double-click to reset to default
 * - Mouse wheel support
 * - Value displayed below knob
 */
public class JRotaryKnob extends JComponent {

    // ==================== DIMENSIONS ====================
    private static final int DEFAULT_SIZE = 60;
    private static final int ARC_WIDTH = 6;
    private static final int PADDING = 8;
    private static final int VALUE_HEIGHT = 16;
    private static final int LABEL_HEIGHT = 14;

    // ==================== ARC ANGLES ====================
    private static final double START_ANGLE = 225;  // Bottom-left
    private static final double ARC_EXTENT = 270;   // Sweep clockwise

    // ==================== VALUE ====================
    private double value;
    private double minValue;
    private double maxValue;
    private double defaultValue;
    private String unit;
    private String label;

    // ==================== INTERACTION ====================
    private int lastY;
    private boolean dragging = false;
    private double valueBeforeDrag;  // Value when drag started
    private static final double DRAG_SENSITIVITY = 0.005;      // Normal drag
    private static final double FINE_SENSITIVITY = 0.0005;     // Shift+drag

    // ==================== APPEARANCE ====================
    private Color arcBackgroundColor = DarkTheme.BG_LIGHT;
    private Color arcForegroundColor = DarkTheme.ACCENT_PRIMARY;
    private Color knobColor = DarkTheme.BG_MEDIUM;
    private Color indicatorColor = DarkTheme.TEXT_PRIMARY;
    private boolean showValue = true;
    private DecimalFormat valueFormat;
    private boolean bipolar = false;  // If true, arc starts from center (for pan controls)

    public JRotaryKnob() {
        this(0, 100, 50, "");
    }

    public JRotaryKnob(double min, double max, double defaultVal, String unit) {
        this.minValue = min;
        this.maxValue = max;
        this.defaultValue = defaultVal;
        this.value = defaultVal;
        this.unit = unit;
        this.label = "";

        // Determine format based on range
        double range = max - min;
        if (range <= 1) {
            valueFormat = new DecimalFormat("0.00");
        } else if (range <= 10) {
            valueFormat = new DecimalFormat("0.0");
        } else {
            valueFormat = new DecimalFormat("0");
        }

        setPreferredSize(new Dimension(DEFAULT_SIZE + PADDING * 2,
                DEFAULT_SIZE + PADDING * 2 + VALUE_HEIGHT + LABEL_HEIGHT));
        setMinimumSize(getPreferredSize());

        setOpaque(false);
        setFocusable(true);

        setupMouseListeners();
        setupKeyListeners();

        // Tooltip
        updateTooltip();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                lastY = e.getY();
                dragging = true;
                valueBeforeDrag = value;  // Store value before drag starts
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
                    // Double-click: reset to default
                    setValue(defaultValue);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    int deltaY = lastY - e.getY();  // Inverted: up = increase
                    lastY = e.getY();

                    double sensitivity = e.isShiftDown() ? FINE_SENSITIVITY : DRAG_SENSITIVITY;
                    double range = maxValue - minValue;
                    double delta = deltaY * sensitivity * range;

                    // Update value silently (no event) during drag
                    setValueSilent(value + delta);
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double range = maxValue - minValue;
                double step = range / 100.0;
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
                double step = range / 100.0;
                if (e.isShiftDown()) step /= 10.0;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP, KeyEvent.VK_RIGHT -> setValue(value + step);
                    case KeyEvent.VK_DOWN, KeyEvent.VK_LEFT -> setValue(value - step);
                    case KeyEvent.VK_HOME -> setValue(minValue);
                    case KeyEvent.VK_END -> setValue(maxValue);
                    case KeyEvent.VK_ESCAPE -> setValue(defaultValue);
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        int h = getHeight();

        // Calculate knob center and size
        int knobSize = Math.min(w - PADDING * 2, DEFAULT_SIZE);
        int knobX = (w - knobSize) / 2;
        int knobY = LABEL_HEIGHT;

        // Draw label above
        if (label != null && !label.isEmpty()) {
            g2d.setFont(DarkTheme.FONT_SMALL);
            g2d.setColor(DarkTheme.TEXT_SECONDARY);
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            g2d.drawString(label, (w - labelWidth) / 2, fm.getAscent());
        }

        // Draw background arc
        drawArc(g2d, knobX, knobY, knobSize, arcBackgroundColor, 0, 1);

        // Draw value arc
        double normalizedValue = getNormalizedValue();
        if (bipolar) {
            // Bipolar mode: arc from center (0.5) to current value
            double center = 0.5;
            if (normalizedValue < center) {
                // Value is left of center - draw from value to center
                drawArc(g2d, knobX, knobY, knobSize, arcForegroundColor, normalizedValue, center);
            } else {
                // Value is right of center or at center - draw from center to value
                drawArc(g2d, knobX, knobY, knobSize, arcForegroundColor, center, normalizedValue);
            }
        } else {
            // Normal mode: arc from start to current value
            drawArc(g2d, knobX, knobY, knobSize, arcForegroundColor, 0, normalizedValue);
        }

        // Draw knob center
        int innerSize = knobSize - ARC_WIDTH * 2 - 4;
        int innerX = knobX + (knobSize - innerSize) / 2;
        int innerY = knobY + (knobSize - innerSize) / 2;

        // Gradient for 3D effect
        GradientPaint gradient = new GradientPaint(
                innerX, innerY, knobColor.brighter(),
                innerX, innerY + innerSize, knobColor.darker()
        );
        g2d.setPaint(gradient);
        g2d.fill(new Ellipse2D.Double(innerX, innerY, innerSize, innerSize));

        // Draw border
        g2d.setColor(DarkTheme.BG_LIGHT);
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(new Ellipse2D.Double(innerX, innerY, innerSize, innerSize));

        // Draw indicator line
        drawIndicator(g2d, knobX + knobSize / 2, knobY + knobSize / 2, innerSize / 2 - 4, normalizedValue);

        // Draw value below
        if (showValue) {
            String valueText = formatValue(value);
            g2d.setFont(DarkTheme.FONT_SMALL);
            g2d.setColor(DarkTheme.TEXT_PRIMARY);
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(valueText);
            int textY = knobY + knobSize + fm.getAscent() + 4;
            g2d.drawString(valueText, (w - textWidth) / 2, textY);
        }

        g2d.dispose();
    }

    /**
     * Draw the arc indicator.
     */
    private void drawArc(Graphics2D g2d, int x, int y, int size, Color color, double startNorm, double endNorm) {
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(ARC_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        double startAngle = START_ANGLE - startNorm * ARC_EXTENT;
        double extent = -(endNorm - startNorm) * ARC_EXTENT;

        int arcInset = ARC_WIDTH / 2 + 2;
        Arc2D arc = new Arc2D.Double(
                x + arcInset, y + arcInset,
                size - arcInset * 2, size - arcInset * 2,
                startAngle, extent,
                Arc2D.OPEN
        );
        g2d.draw(arc);
    }

    /**
     * Draw the indicator line on the knob.
     */
    private void drawIndicator(Graphics2D g2d, int cx, int cy, int radius, double normalizedValue) {
        double angle = Math.toRadians(START_ANGLE - normalizedValue * ARC_EXTENT);
        int x2 = cx + (int) (Math.cos(angle) * radius);
        int y2 = cy - (int) (Math.sin(angle) * radius);
        int x1 = cx + (int) (Math.cos(angle) * (radius * 0.3));
        int y1 = cy - (int) (Math.sin(angle) * (radius * 0.3));

        g2d.setColor(indicatorColor);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(x1, y1, x2, y2);
    }

    /**
     * Format the value for display.
     */
    private String formatValue(double val) {
        String formatted = valueFormat.format(val);
        if (unit != null && !unit.isEmpty()) {
            return formatted + " " + unit;
        }
        return formatted;
    }

    private void updateTooltip() {
        setToolTipText(String.format("%s: %s (default: %s)",
                label != null && !label.isEmpty() ? label : "Value",
                formatValue(value),
                formatValue(defaultValue)));
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
        repaint();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
        repaint();
    }

    public void setArcForegroundColor(Color color) {
        this.arcForegroundColor = color;
        repaint();
    }

    public void setShowValue(boolean showValue) {
        this.showValue = showValue;
        repaint();
    }

    public void setValueFormat(DecimalFormat format) {
        this.valueFormat = format;
        repaint();
    }

    /**
     * Set bipolar mode for pan-style controls.
     * In bipolar mode, the arc is drawn from center (12 o'clock) to current position.
     */
    public void setBipolar(boolean bipolar) {
        this.bipolar = bipolar;
        repaint();
    }

    /**
     * Check if knob is in bipolar mode.
     */
    public boolean isBipolar() {
        return bipolar;
    }

    /**
     * Reset to default value.
     */
    public void reset() {
        setValue(defaultValue);
    }
}
