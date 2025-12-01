package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.effects.impl.NoiseGateEffect;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Visual meter panel for NoiseGate effect.
 * Shows input level, threshold line, gate reduction, and output level.
 */
public class NoiseGateMeterPanel extends JPanel {

    private NoiseGateEffect noiseGate;
    private Timer updateTimer;

    // Meter dimensions
    private static final int METER_WIDTH = 30;
    private static final int METER_HEIGHT = 120;
    private static final int METER_GAP = 20;
    private static final int LABEL_HEIGHT = 16;
    private static final int VALUE_HEIGHT = 14;

    // Colors
    private static final Color METER_BG = new Color(0x1a1a2e);
    private static final Color METER_BORDER = new Color(0x3a3a5e);
    private static final Color INPUT_COLOR = new Color(0x00d9ff);  // Cyan
    private static final Color OUTPUT_COLOR = new Color(0x00ff88);  // Green
    private static final Color THRESHOLD_COLOR = new Color(0xe94560);  // Red/pink
    private static final Color REDUCTION_COLOR = new Color(0xffaa00);  // Orange
    private static final Color GRID_COLOR = new Color(0x2a2a4e);

    // dB range for meters
    private static final float MIN_DB = -60f;
    private static final float MAX_DB = 0f;

    public NoiseGateMeterPanel() {
        setOpaque(false);
        setPreferredSize(new Dimension(200, METER_HEIGHT + LABEL_HEIGHT + VALUE_HEIGHT + 20));
    }

    /**
     * Set the noise gate effect to monitor.
     */
    public void setNoiseGate(NoiseGateEffect effect) {
        this.noiseGate = effect;
        startUpdates();
    }

    /**
     * Start periodic UI updates.
     */
    public void startUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        updateTimer = new Timer(50, e -> repaint());  // 20 FPS
        updateTimer.start();
    }

    /**
     * Stop periodic UI updates.
     */
    public void stopUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // Get values from effect
        float inputDb = noiseGate != null ? noiseGate.getInputLevelDb() : MIN_DB;
        float outputDb = noiseGate != null ? noiseGate.getOutputLevelDb() : MIN_DB;
        float thresholdDb = noiseGate != null ? noiseGate.getThresholdDb() : -40f;
        float reductionDb = noiseGate != null ? noiseGate.getGateReductionDb() : 0f;

        // Calculate meter positions
        int totalMetersWidth = METER_WIDTH * 3 + METER_GAP * 2;
        int startX = (width - totalMetersWidth) / 2;
        int meterY = LABEL_HEIGHT + 4;

        // Draw input meter
        drawMeter(g2d, startX, meterY, "INPUT", inputDb, INPUT_COLOR, thresholdDb);

        // Draw reduction meter (center)
        int reductionX = startX + METER_WIDTH + METER_GAP;
        drawReductionMeter(g2d, reductionX, meterY, "GR", reductionDb);

        // Draw output meter
        int outputX = reductionX + METER_WIDTH + METER_GAP;
        drawMeter(g2d, outputX, meterY, "OUTPUT", outputDb, OUTPUT_COLOR, Float.NaN);

        g2d.dispose();
    }

    /**
     * Draw a level meter with optional threshold line.
     */
    private void drawMeter(Graphics2D g2d, int x, int y, String label, float levelDb, Color color, float thresholdDb) {
        // Draw label
        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
        g2d.setColor(DarkTheme.TEXT_SECONDARY);
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        g2d.drawString(label, x + (METER_WIDTH - labelWidth) / 2, y - 4);

        // Draw meter background
        g2d.setColor(METER_BG);
        g2d.fillRoundRect(x, y, METER_WIDTH, METER_HEIGHT, 4, 4);

        // Draw border
        g2d.setColor(METER_BORDER);
        g2d.drawRoundRect(x, y, METER_WIDTH, METER_HEIGHT, 4, 4);

        // Draw grid lines (every 10 dB)
        g2d.setColor(GRID_COLOR);
        for (float db = -10f; db >= MIN_DB; db -= 10f) {
            int lineY = y + dbToY(db);
            g2d.drawLine(x + 2, lineY, x + METER_WIDTH - 2, lineY);
        }

        // Draw level bar
        float clampedDb = Math.max(MIN_DB, Math.min(MAX_DB, levelDb));
        int barHeight = METER_HEIGHT - dbToY(clampedDb);
        if (barHeight > 0) {
            // Gradient from bottom to top
            GradientPaint gradient = new GradientPaint(
                    x, y + METER_HEIGHT, color.darker(),
                    x, y + METER_HEIGHT - barHeight, color
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(x + 3, y + METER_HEIGHT - barHeight, METER_WIDTH - 6, barHeight, 2, 2);
        }

        // Draw threshold line if provided
        if (!Float.isNaN(thresholdDb)) {
            int thresholdY = y + dbToY(thresholdDb);
            g2d.setColor(THRESHOLD_COLOR);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawLine(x - 4, thresholdY, x + METER_WIDTH + 4, thresholdY);

            // Draw threshold marker triangle
            int[] xPoints = {x - 6, x - 2, x - 6};
            int[] yPoints = {thresholdY - 4, thresholdY, thresholdY + 4};
            g2d.fillPolygon(xPoints, yPoints, 3);
        }

        // Draw value below meter
        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(9f));
        g2d.setColor(color);
        String valueStr = levelDb > MIN_DB + 1 ? String.format("%.0f", levelDb) : "-INF";
        int valueWidth = fm.stringWidth(valueStr);
        g2d.drawString(valueStr, x + (METER_WIDTH - valueWidth) / 2, y + METER_HEIGHT + 12);
    }

    /**
     * Draw the gain reduction meter (inverted - shows reduction from top).
     */
    private void drawReductionMeter(Graphics2D g2d, int x, int y, String label, float reductionDb) {
        // Draw label
        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD, 9f));
        g2d.setColor(DarkTheme.TEXT_SECONDARY);
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        g2d.drawString(label, x + (METER_WIDTH - labelWidth) / 2, y - 4);

        // Draw meter background
        g2d.setColor(METER_BG);
        g2d.fillRoundRect(x, y, METER_WIDTH, METER_HEIGHT, 4, 4);

        // Draw border
        g2d.setColor(METER_BORDER);
        g2d.drawRoundRect(x, y, METER_WIDTH, METER_HEIGHT, 4, 4);

        // Draw grid lines
        g2d.setColor(GRID_COLOR);
        for (float db = -10f; db >= MIN_DB; db -= 10f) {
            int lineY = y + dbToY(db);
            g2d.drawLine(x + 2, lineY, x + METER_WIDTH - 2, lineY);
        }

        // Reduction meter shows from top (0 dB) down
        // reductionDb is negative (e.g., -20 dB = 20 dB of reduction)
        float clampedReduction = Math.max(MIN_DB, Math.min(0f, reductionDb));
        int barHeight = dbToY(clampedReduction);  // Height from top

        if (barHeight > 0) {
            // Draw reduction bar from top
            GradientPaint gradient = new GradientPaint(
                    x, y, REDUCTION_COLOR,
                    x, y + barHeight, REDUCTION_COLOR.darker()
            );
            g2d.setPaint(gradient);
            g2d.fillRoundRect(x + 3, y + 2, METER_WIDTH - 6, barHeight - 2, 2, 2);
        }

        // Draw value below meter
        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(9f));
        g2d.setColor(REDUCTION_COLOR);
        String valueStr = reductionDb > MIN_DB + 1 ? String.format("%.0f", reductionDb) : "-INF";
        int valueWidth = fm.stringWidth(valueStr);
        g2d.drawString(valueStr, x + (METER_WIDTH - valueWidth) / 2, y + METER_HEIGHT + 12);
    }

    /**
     * Convert dB value to Y position within meter.
     * 0 dB = top, MIN_DB = bottom
     */
    private int dbToY(float db) {
        float normalized = (db - MIN_DB) / (MAX_DB - MIN_DB);  // 0 to 1
        return (int) ((1f - normalized) * (METER_HEIGHT - 4)) + 2;
    }
}
