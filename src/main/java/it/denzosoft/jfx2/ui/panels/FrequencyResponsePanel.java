package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.effects.impl.FilterEffect;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Panel that displays the frequency response curve of a FilterEffect.
 * Shows a logarithmic frequency scale (20Hz to 20kHz) and dB scale (-24 to +24 dB).
 */
public class FrequencyResponsePanel extends JPanel {

    private static final int NUM_POINTS = 256;
    private static final double MIN_FREQ = 20.0;
    private static final double MAX_FREQ = 20000.0;
    private static final double MIN_DB = -24.0;
    private static final double MAX_DB = 24.0;

    // Note frequencies for grid
    private static final double[] NOTE_FREQUENCIES = {
            100, 200, 500, 1000, 2000, 5000, 10000
    };
    private static final String[] NOTE_LABELS = {
            "100", "200", "500", "1k", "2k", "5k", "10k"
    };

    // dB grid lines
    private static final double[] DB_LINES = {-24, -18, -12, -6, 0, 6, 12, 18, 24};

    private FilterEffect filterEffect;
    private double[] responseCurve;

    public FrequencyResponsePanel() {
        setBackground(DarkTheme.BG_DARK);
        setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1));
        setPreferredSize(new Dimension(300, 120));
    }

    /**
     * Set the FilterEffect to display.
     */
    public void setFilterEffect(FilterEffect effect) {
        this.filterEffect = effect;
        repaint();
    }

    /**
     * Stop updates when panel is hidden.
     */
    public void stopUpdates() {
        // No timer to stop anymore - updates are immediate on repaint
    }

    /**
     * Update the response curve from current filter settings.
     */
    private void updateResponseCurve() {
        if (filterEffect != null) {
            responseCurve = filterEffect.getFrequencyResponseCurve(NUM_POINTS);
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

        // Update response curve from current filter settings
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
        g2d.drawString("FREQUENCY RESPONSE", 5, 10);

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
            if (db == 0 || db == 12 || db == -12 || db == 24 || db == -24) {
                g2d.setColor(new Color(DarkTheme.TEXT_SECONDARY.getRGB() & 0x00FFFFFF | 0x80000000, true));
                String label = (db > 0 ? "+" : "") + (int) db;
                g2d.drawString(label, w - fm.stringWidth(label) - 2, y + 3);
            }
        }

        // Draw vertical frequency lines
        for (int i = 0; i < NOTE_FREQUENCIES.length; i++) {
            double freq = NOTE_FREQUENCIES[i];
            int x = freqToX(freq, w);

            g2d.setColor(new Color(255, 255, 255, 25));
            g2d.drawLine(x, topMargin, x, h - bottomMargin);

            // Frequency label at bottom
            g2d.setColor(new Color(DarkTheme.TEXT_SECONDARY.getRGB() & 0x00FFFFFF | 0x80000000, true));
            String label = NOTE_LABELS[i];
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

        // Fill with gradient based on whether it's boost or cut
        g2d.setColor(new Color(DarkTheme.ACCENT_PRIMARY.getRed(),
                DarkTheme.ACCENT_PRIMARY.getGreen(),
                DarkTheme.ACCENT_PRIMARY.getBlue(), 40));
        g2d.fill(fillPath);

        // Draw the curve line
        g2d.setColor(DarkTheme.ACCENT_PRIMARY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(path);

        // Draw band markers (frequency points for active bands)
        if (filterEffect != null) {
            drawBandMarkers(g2d, w, h, topMargin, bottomMargin);
        }
    }

    /**
     * Draw markers for each active band.
     */
    private void drawBandMarkers(Graphics2D g2d, int w, int h, int topMargin, int bottomMargin) {
        Color[] bandColors = {
                new Color(0xFF6B6B),  // Red
                new Color(0xFFE66D),  // Yellow
                new Color(0x4ECDC4),  // Teal
                new Color(0x95E1D3),  // Mint
                new Color(0xDDA0DD)   // Plum
        };

        for (int i = 0; i < FilterEffect.NUM_BANDS; i++) {
            int type = filterEffect.getBandType(i);
            if (type == FilterEffect.TYPE_NONE) {
                continue;
            }

            float freq = filterEffect.getBandFrequency(i);
            int x = freqToX(freq, w);

            // Determine marker position based on filter type
            double db;
            if (type == FilterEffect.TYPE_PEAK ||
                type == FilterEffect.TYPE_LOWSHELF ||
                type == FilterEffect.TYPE_HIGHSHELF) {
                // For Peak/Shelf filters, marker shows the gain setting
                db = filterEffect.getBandGain(i);
            } else {
                // For LP, HP, BP, Notch, AP - these don't use gain, marker at 0 dB
                db = 0.0;
            }
            db = Math.max(MIN_DB, Math.min(MAX_DB, db));
            int y = dbToY(db, h, topMargin, bottomMargin);

            // Draw band marker
            g2d.setColor(bandColors[i % bandColors.length]);
            g2d.fillOval(x - 4, y - 4, 8, 8);
            g2d.setColor(Color.WHITE);
            g2d.drawOval(x - 4, y - 4, 8, 8);

            // Draw band number
            g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD, 7f));
            String num = String.valueOf(i + 1);
            FontMetrics fm = g2d.getFontMetrics();
            g2d.drawString(num, x - fm.stringWidth(num) / 2, y + 3);
        }
    }
}
