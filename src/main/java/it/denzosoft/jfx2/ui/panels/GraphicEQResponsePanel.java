package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.effects.impl.GraphicEQEffect;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Panel that displays the frequency response curve of a GraphicEQEffect.
 * Shows a logarithmic frequency scale (20Hz to 20kHz) and dB scale (-15 to +15 dB).
 */
public class GraphicEQResponsePanel extends JPanel {

    private static final int NUM_POINTS = 256;
    private static final double MIN_FREQ = 20.0;
    private static final double MAX_FREQ = 20000.0;
    private static final double MIN_DB = -15.0;
    private static final double MAX_DB = 15.0;

    // Frequency grid lines
    private static final double[] FREQ_LINES = {
            31.25, 62.5, 125, 250, 500, 1000, 2000, 4000, 8000, 16000
    };
    private static final String[] FREQ_LABELS = {
            "31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"
    };

    // dB grid lines
    private static final double[] DB_LINES = {-12, -6, 0, 6, 12};

    private GraphicEQEffect graphicEQ;
    private double[] responseCurve;

    public GraphicEQResponsePanel() {
        setBackground(DarkTheme.BG_DARK);
        setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1));
        setPreferredSize(new Dimension(300, 120));
    }

    /**
     * Set the GraphicEQEffect to display.
     */
    public void setGraphicEQ(GraphicEQEffect effect) {
        this.graphicEQ = effect;
        repaint();
    }

    /**
     * Update the response curve from current EQ settings.
     */
    private void updateResponseCurve() {
        if (graphicEQ != null) {
            responseCurve = graphicEQ.getFrequencyResponseCurve(NUM_POINTS);
        } else {
            responseCurve = null;
        }
    }

    /**
     * Convert frequency to x position (logarithmic scale).
     */
    private int freqToX(double freq, int width) {
        double logMin = Math.log10(MIN_FREQ);
        double logMax = Math.log10(MAX_FREQ);
        double logFreq = Math.log10(freq);
        return (int) ((logFreq - logMin) / (logMax - logMin) * width);
    }

    /**
     * Convert dB to y position.
     */
    private int dbToY(double db, int height, int topMargin, int bottomMargin) {
        int usableHeight = height - topMargin - bottomMargin;
        double normalized = (db - MIN_DB) / (MAX_DB - MIN_DB);
        return height - bottomMargin - (int) (normalized * usableHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Update response curve from current EQ settings
        updateResponseCurve();

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int topMargin = 15;
        int bottomMargin = 15;
        int leftMargin = 5;
        int rightMargin = 5;

        // Draw background grid
        drawGrid(g2d, w, h, topMargin, bottomMargin, leftMargin, rightMargin);

        // Draw frequency response curve
        if (responseCurve != null && responseCurve.length > 0) {
            drawResponseCurve(g2d, w, h, topMargin, bottomMargin);
        }

        // Draw title
        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(9f));
        g2d.setColor(DarkTheme.TEXT_SECONDARY);
        g2d.drawString("EQ RESPONSE", 5, 10);

        g2d.dispose();
    }

    /**
     * Draw the background grid with frequency and dB labels.
     */
    private void drawGrid(Graphics2D g2d, int w, int h, int topMargin, int bottomMargin, int leftMargin, int rightMargin) {
        g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(8f));
        FontMetrics fm = g2d.getFontMetrics();

        // Draw horizontal dB lines
        for (double db : DB_LINES) {
            int y = dbToY(db, h, topMargin, bottomMargin);

            // Line color: 0dB line brighter
            if (db == 0) {
                g2d.setColor(new Color(255, 255, 255, 60));
            } else {
                g2d.setColor(new Color(255, 255, 255, 25));
            }
            g2d.drawLine(leftMargin, y, w - rightMargin, y);

            // dB label on right
            g2d.setColor(new Color(DarkTheme.TEXT_SECONDARY.getRGB() & 0x00FFFFFF | 0x80000000, true));
            String label = (db > 0 ? "+" : "") + (int) db;
            g2d.drawString(label, w - fm.stringWidth(label) - 2, y + 3);
        }

        // Draw vertical frequency lines at band frequencies
        for (int i = 0; i < FREQ_LINES.length; i++) {
            double freq = FREQ_LINES[i];
            int x = freqToX(freq, w);

            // Highlight band positions
            g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                    DarkTheme.ACCENT_PRIMARY.getGreen(),
                    DarkTheme.ACCENT_PRIMARY.getBlue(), 30));
            g2d.drawLine(x, topMargin, x, h - bottomMargin);

            // Frequency label at bottom
            g2d.setColor(new Color(DarkTheme.TEXT_SECONDARY.getRGB() & 0x00FFFFFF | 0x80000000, true));
            String label = FREQ_LABELS[i];
            int labelX = x - fm.stringWidth(label) / 2;
            g2d.drawString(label, labelX, h - 3);
        }
    }

    /**
     * Draw the frequency response curve.
     */
    private void drawResponseCurve(Graphics2D g2d, int w, int h, int topMargin, int bottomMargin) {
        Path2D path = new Path2D.Double();

        double logMin = Math.log10(MIN_FREQ);
        double logMax = Math.log10(MAX_FREQ);

        boolean first = true;

        for (int i = 0; i < responseCurve.length; i++) {
            // Calculate frequency for this point
            double logFreq = logMin + (double) i / (responseCurve.length - 1) * (logMax - logMin);
            double freq = Math.pow(10.0, logFreq);

            int x = freqToX(freq, w);

            // Clamp dB value
            double db = Math.max(MIN_DB, Math.min(MAX_DB, responseCurve[i]));
            int y = dbToY(db, h, topMargin, bottomMargin);

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        // Draw filled area under curve
        Path2D fillPath = new Path2D.Double(path);
        int zeroY = dbToY(0, h, topMargin, bottomMargin);

        // Close the path for filling
        double lastLogFreq = logMax;
        int lastX = freqToX(Math.pow(10.0, lastLogFreq), w);
        fillPath.lineTo(lastX, zeroY);

        double firstLogFreq = logMin;
        int firstX = freqToX(Math.pow(10.0, firstLogFreq), w);
        fillPath.lineTo(firstX, zeroY);
        fillPath.closePath();

        // Fill with gradient
        g2d.setColor(new Color(DarkTheme.ACCENT_SUCCESS.getRed(),
                DarkTheme.ACCENT_SUCCESS.getGreen(),
                DarkTheme.ACCENT_SUCCESS.getBlue(), 40));
        g2d.fill(fillPath);

        // Draw the curve line
        g2d.setColor(DarkTheme.ACCENT_SUCCESS);
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(path);

        // Draw band markers
        if (graphicEQ != null) {
            drawBandMarkers(g2d, w, h, topMargin, bottomMargin);
        }
    }

    /**
     * Draw markers for each band showing current gain.
     */
    private void drawBandMarkers(Graphics2D g2d, int w, int h, int topMargin, int bottomMargin) {
        for (int i = 0; i < GraphicEQEffect.NUM_BANDS; i++) {
            float freq = graphicEQ.getBandFrequency(i);
            float gain = graphicEQ.getBandGain(i);

            int x = freqToX(freq, w);

            // Get the response at this frequency
            double db = graphicEQ.getFrequencyResponseDb(freq);
            db = Math.max(MIN_DB, Math.min(MAX_DB, db));
            int y = dbToY(db, h, topMargin, bottomMargin);

            // Draw band marker - color based on boost/cut
            if (gain > 0.5) {
                g2d.setColor(new Color(0x4ECDC4)); // Teal for boost
            } else if (gain < -0.5) {
                g2d.setColor(new Color(0xFF6B6B)); // Red for cut
            } else {
                g2d.setColor(DarkTheme.TEXT_SECONDARY); // Grey for flat
            }

            g2d.fillOval(x - 3, y - 3, 6, 6);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(x - 3, y - 3, 6, 6);
        }
    }
}
