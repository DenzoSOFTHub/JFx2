package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Panel that displays 20-band spectrum analyzer for input and output signals.
 * Input spectrum on the LEFT, Output spectrum on the RIGHT.
 * Similar to the footer spectrum analyzer but larger with more detail.
 */
public class SpectrumBandsPanel extends JPanel {

    // Display settings
    private static final int DISPLAY_WIDTH = 400;
    private static final int PANEL_HEIGHT = 160;
    private static final int NUM_BANDS = 20;
    private static final Color INPUT_COLOR = new Color(0x00d9ff);   // Cyan
    private static final Color OUTPUT_COLOR = new Color(0x00ff88);  // Green
    private static final Color GRID_COLOR = new Color(0x2a2a4e);

    // FFT settings
    private static final int FFT_SIZE = 2048;
    private static final float DECAY_RATE = 0.88f;
    private static final long PEAK_HOLD_MS = 500;

    // FFT buffers
    private final float[] inputBuffer = new float[FFT_SIZE];
    private final float[] outputBuffer = new float[FFT_SIZE];
    private int inputBufferPos = 0;
    private int outputBufferPos = 0;

    // Band values
    private final float[] inputBands = new float[NUM_BANDS];
    private final float[] outputBands = new float[NUM_BANDS];
    private final float[] inputPeaks = new float[NUM_BANDS];
    private final float[] outputPeaks = new float[NUM_BANDS];
    private final long[] inputPeakTimes = new long[NUM_BANDS];
    private final long[] outputPeakTimes = new long[NUM_BANDS];

    // Band frequency boundaries (logarithmic 20Hz - 20kHz)
    private final float[] bandFrequencies = new float[NUM_BANDS + 1];

    // Band labels
    private static final String[] BAND_LABELS = {
        "25", "40", "63", "100", "160", "250", "400", "630", "1k", "1.6k",
        "2.5k", "4k", "6.3k", "10k", "16k", "", "", "", "", ""
    };

    // Sample rate
    private int sampleRate = 44100;

    // Update timer
    private Timer updateTimer;

    public SpectrumBandsPanel() {
        setBackground(DarkTheme.BG_DARK);
        setPreferredSize(new Dimension(DISPLAY_WIDTH * 2 + 60, PANEL_HEIGHT + 40));

        // Calculate band frequencies (logarithmic 20Hz - 20kHz)
        for (int i = 0; i <= NUM_BANDS; i++) {
            bandFrequencies[i] = (float) (20 * Math.pow(1000, i / (float) NUM_BANDS));
        }

        clearBuffers();
    }

    /**
     * Set sample rate.
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Start updates.
     */
    public void startUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        updateTimer = new Timer(33, e -> repaint());  // ~30 FPS
        updateTimer.start();
    }

    /**
     * Stop updates.
     */
    public void stopUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    /**
     * Add samples for analysis.
     */
    public void addSamples(float[] input, float[] output, int count) {
        // Accumulate input samples
        if (input != null) {
            for (int i = 0; i < count && i < input.length; i++) {
                inputBuffer[inputBufferPos++] = input[i];
                if (inputBufferPos >= FFT_SIZE) {
                    computeFFT(inputBuffer, inputBands, inputPeaks, inputPeakTimes);
                    inputBufferPos = 0;
                }
            }
        }

        // Accumulate output samples
        if (output != null) {
            for (int i = 0; i < count && i < output.length; i++) {
                outputBuffer[outputBufferPos++] = output[i];
                if (outputBufferPos >= FFT_SIZE) {
                    computeFFT(outputBuffer, outputBands, outputPeaks, outputPeakTimes);
                    outputBufferPos = 0;
                }
            }
        }
    }

    /**
     * Compute FFT and extract band values.
     */
    private void computeFFT(float[] samples, float[] bands, float[] peaks, long[] peakTimes) {
        // Apply Hann window
        float[] real = new float[FFT_SIZE];
        float[] imag = new float[FFT_SIZE];

        for (int i = 0; i < FFT_SIZE; i++) {
            float window = 0.5f * (1 - (float) Math.cos(2 * Math.PI * i / (FFT_SIZE - 1)));
            real[i] = samples[i] * window;
        }

        // Cooley-Tukey FFT
        int bits = (int) (Math.log(FFT_SIZE) / Math.log(2));

        // Bit-reversal permutation
        for (int i = 0; i < FFT_SIZE; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (j > i) {
                float temp = real[i];
                real[i] = real[j];
                real[j] = temp;
            }
        }

        // FFT butterfly operations
        for (int size = 2; size <= FFT_SIZE; size *= 2) {
            int halfSize = size / 2;
            float angle = (float) (-2 * Math.PI / size);

            for (int i = 0; i < FFT_SIZE; i += size) {
                for (int j = 0; j < halfSize; j++) {
                    float cos = (float) Math.cos(angle * j);
                    float sin = (float) Math.sin(angle * j);

                    int idx1 = i + j;
                    int idx2 = i + j + halfSize;

                    float tReal = real[idx2] * cos - imag[idx2] * sin;
                    float tImag = real[idx2] * sin + imag[idx2] * cos;

                    real[idx2] = real[idx1] - tReal;
                    imag[idx2] = imag[idx1] - tImag;
                    real[idx1] = real[idx1] + tReal;
                    imag[idx1] = imag[idx1] + tImag;
                }
            }
        }

        // Calculate band magnitudes
        float binFrequency = (float) sampleRate / FFT_SIZE;
        int numFFTBins = FFT_SIZE / 2;
        long now = System.currentTimeMillis();

        for (int band = 0; band < NUM_BANDS; band++) {
            float freqLo = bandFrequencies[band];
            float freqHi = bandFrequencies[band + 1];

            int binLo = Math.max(1, (int) (freqLo / binFrequency));
            int binHi = Math.min(numFFTBins - 1, (int) (freqHi / binFrequency));

            // Average magnitudes in range
            float sumMag = 0;
            int binCount = 0;
            for (int b = binLo; b <= binHi && b < numFFTBins; b++) {
                float mag = (float) Math.sqrt(real[b] * real[b] + imag[b] * imag[b]);
                sumMag += mag;
                binCount++;
            }

            float avgMag = (binCount > 0) ? sumMag / binCount : 0;

            // Normalize
            avgMag = avgMag / (FFT_SIZE / 4);
            avgMag = Math.min(1.0f, avgMag * 3);  // Scale up

            // Smoothing with decay
            if (avgMag > bands[band]) {
                bands[band] = avgMag;
            } else {
                bands[band] = bands[band] * DECAY_RATE + avgMag * (1 - DECAY_RATE);
            }

            // Peak hold
            if (bands[band] > peaks[band]) {
                peaks[band] = bands[band];
                peakTimes[band] = now;
            } else if (now - peakTimes[band] > PEAK_HOLD_MS) {
                peaks[band] *= 0.95f;
            }
        }
    }

    /**
     * Clear all buffers.
     */
    public void clearBuffers() {
        Arrays.fill(inputBuffer, 0);
        Arrays.fill(outputBuffer, 0);
        Arrays.fill(inputBands, 0);
        Arrays.fill(outputBands, 0);
        Arrays.fill(inputPeaks, 0);
        Arrays.fill(outputPeaks, 0);
        inputBufferPos = 0;
        outputBufferPos = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int margin = 10;
        int gap = 20;

        int panelWidth = (width - gap - margin * 2) / 2;
        int panelHeight = height - margin * 2 - 25;

        // Input bands on LEFT
        drawBandsPanel(g2d, "INPUT", inputBands, inputPeaks,
                margin, margin + 20, panelWidth, panelHeight, INPUT_COLOR);

        // Output bands on RIGHT
        drawBandsPanel(g2d, "OUTPUT", outputBands, outputPeaks,
                margin + panelWidth + gap, margin + 20, panelWidth, panelHeight, OUTPUT_COLOR);

        g2d.dispose();
    }

    /**
     * Draw a bands panel.
     */
    private void drawBandsPanel(Graphics2D g, String title, float[] bands, float[] peaks,
                                 int x, int y, int width, int height, Color color) {
        // Draw title
        g.setFont(DarkTheme.FONT_BOLD.deriveFont(12f));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, x + (width - fm.stringWidth(title)) / 2, y - 5);

        // Draw background
        g.setColor(DarkTheme.BG_MEDIUM);
        g.fillRoundRect(x, y, width, height, 6, 6);

        // Draw horizontal grid lines (dB levels)
        g.setColor(GRID_COLOR);
        for (int i = 1; i < 4; i++) {
            int gridY = y + (height * i) / 4;
            g.drawLine(x + 2, gridY, x + width - 2, gridY);
        }

        // Calculate bar dimensions
        int barGap = 3;
        int totalGaps = (NUM_BANDS - 1) * barGap;
        int barWidth = (width - 8 - totalGaps) / NUM_BANDS;
        int startX = x + 4;

        // Draw bars
        for (int band = 0; band < NUM_BANDS; band++) {
            int barX = startX + band * (barWidth + barGap);
            int barH = (int) (bands[band] * (height - 8));
            barH = Math.max(2, Math.min(height - 8, barH));

            // Color gradient based on level
            float level = bands[band];
            Color barColor;
            if (level < 0.4f) {
                barColor = interpolateColor(color.darker(), color, level / 0.4f);
            } else if (level < 0.7f) {
                barColor = interpolateColor(color, DarkTheme.ACCENT_WARNING, (level - 0.4f) / 0.3f);
            } else {
                barColor = interpolateColor(DarkTheme.ACCENT_WARNING, DarkTheme.ACCENT_ERROR, (level - 0.7f) / 0.3f);
            }

            // Draw bar with gradient
            GradientPaint gradient = new GradientPaint(
                    barX, y + height - 4 - barH, barColor.brighter(),
                    barX, y + height - 4, barColor.darker()
            );
            g.setPaint(gradient);
            g.fillRect(barX, y + height - 4 - barH, barWidth, barH);

            // Draw peak indicator
            int peakY = y + height - 4 - (int) (peaks[band] * (height - 8));
            peakY = Math.max(y + 4, peakY);
            g.setColor(Color.WHITE);
            g.drawLine(barX, peakY, barX + barWidth - 1, peakY);
        }

        // Draw frequency labels (every 4th band)
        g.setFont(DarkTheme.FONT_SMALL.deriveFont(7f));
        g.setColor(DarkTheme.TEXT_DISABLED);
        fm = g.getFontMetrics();

        int[] labelBands = {0, 4, 8, 12, 16};
        String[] labels = {"25", "160", "1k", "6.3k", "16k"};

        for (int i = 0; i < labelBands.length; i++) {
            int band = labelBands[i];
            if (band < NUM_BANDS) {
                int barX = startX + band * (barWidth + barGap);
                g.drawString(labels[i], barX + barWidth / 2 - fm.stringWidth(labels[i]) / 2, y + height + 12);
            }
        }

        // Draw border
        g.setColor(new Color(0x4a4a7e));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, width, height, 6, 6);
    }

    /**
     * Interpolate between two colors.
     */
    private Color interpolateColor(Color c1, Color c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * t);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
        return new Color(r, g, b);
    }
}
