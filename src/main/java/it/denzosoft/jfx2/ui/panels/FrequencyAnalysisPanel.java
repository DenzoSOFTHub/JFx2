package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Panel that displays frequency spectrum analysis for input and output signals.
 * Input spectrum on the LEFT, Output spectrum on the RIGHT.
 * Shows FFT-based frequency content from 20Hz to 20kHz.
 */
public class FrequencyAnalysisPanel extends JPanel {

    // Display settings
    private static final int DISPLAY_WIDTH = 400;
    private static final int PANEL_HEIGHT = 160;
    private static final Color INPUT_COLOR = new Color(0x00d9ff);   // Cyan
    private static final Color OUTPUT_COLOR = new Color(0x00ff88);  // Green
    private static final Color GRID_COLOR = new Color(0x2a2a4e);

    // FFT settings
    private static final int FFT_SIZE = 2048;
    private static final int NUM_DISPLAY_BINS = 256;  // Number of display points
    private static final float MIN_FREQ = 20f;
    private static final float MAX_FREQ = 20000f;
    private static final float DECAY_RATE = 0.85f;

    // FFT buffers
    private final float[] inputBuffer = new float[FFT_SIZE];
    private final float[] outputBuffer = new float[FFT_SIZE];
    private int inputBufferPos = 0;
    private int outputBufferPos = 0;

    // Display magnitude arrays (logarithmic frequency scale)
    private final float[] inputMagnitudes = new float[NUM_DISPLAY_BINS];
    private final float[] outputMagnitudes = new float[NUM_DISPLAY_BINS];
    private final float[] inputPeaks = new float[NUM_DISPLAY_BINS];
    private final float[] outputPeaks = new float[NUM_DISPLAY_BINS];

    // Frequency mapping
    private final float[] binFrequencies = new float[NUM_DISPLAY_BINS];

    // Sample rate
    private int sampleRate = 44100;

    // Update timer
    private Timer updateTimer;

    public FrequencyAnalysisPanel() {
        setBackground(DarkTheme.BG_DARK);
        setPreferredSize(new Dimension(DISPLAY_WIDTH * 2 + 60, PANEL_HEIGHT + 40));

        // Calculate logarithmic frequency mapping
        for (int i = 0; i < NUM_DISPLAY_BINS; i++) {
            float t = (float) i / (NUM_DISPLAY_BINS - 1);
            binFrequencies[i] = (float) (MIN_FREQ * Math.pow(MAX_FREQ / MIN_FREQ, t));
        }

        clearBuffers();
    }

    /**
     * Set sample rate for FFT calculations.
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Start updating the display.
     */
    public void startUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        updateTimer = new Timer(33, e -> repaint());  // ~30 FPS
        updateTimer.start();
    }

    /**
     * Stop updating the display.
     */
    public void stopUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    /**
     * Add samples for FFT analysis.
     */
    public void addSamples(float[] input, float[] output, int count) {
        // Accumulate input samples
        if (input != null) {
            for (int i = 0; i < count && i < input.length; i++) {
                inputBuffer[inputBufferPos++] = input[i];
                if (inputBufferPos >= FFT_SIZE) {
                    computeFFT(inputBuffer, inputMagnitudes, inputPeaks);
                    inputBufferPos = 0;
                }
            }
        }

        // Accumulate output samples
        if (output != null) {
            for (int i = 0; i < count && i < output.length; i++) {
                outputBuffer[outputBufferPos++] = output[i];
                if (outputBufferPos >= FFT_SIZE) {
                    computeFFT(outputBuffer, outputMagnitudes, outputPeaks);
                    outputBufferPos = 0;
                }
            }
        }
    }

    /**
     * Compute FFT and map to display bins.
     */
    private void computeFFT(float[] samples, float[] magnitudes, float[] peaks) {
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

        // Map FFT bins to display bins (logarithmic frequency scale)
        float binFrequency = (float) sampleRate / FFT_SIZE;
        int numFFTBins = FFT_SIZE / 2;

        for (int i = 0; i < NUM_DISPLAY_BINS; i++) {
            float targetFreq = binFrequencies[i];
            float nextFreq = (i < NUM_DISPLAY_BINS - 1) ? binFrequencies[i + 1] : MAX_FREQ;

            int binLo = Math.max(1, (int) (targetFreq / binFrequency));
            int binHi = Math.min(numFFTBins - 1, (int) (nextFreq / binFrequency));

            // Average magnitudes in range
            float maxMag = 0;
            for (int b = binLo; b <= binHi && b < numFFTBins; b++) {
                float mag = (float) Math.sqrt(real[b] * real[b] + imag[b] * imag[b]);
                maxMag = Math.max(maxMag, mag);
            }

            // Normalize and convert to dB-like scale
            float normalized = maxMag / (FFT_SIZE / 4);  // Normalize
            normalized = Math.min(1.0f, normalized * 2);  // Scale up weak signals

            // Smoothing with decay
            if (normalized > magnitudes[i]) {
                magnitudes[i] = normalized;
            } else {
                magnitudes[i] = magnitudes[i] * DECAY_RATE + normalized * (1 - DECAY_RATE);
            }

            // Peak hold
            if (magnitudes[i] > peaks[i]) {
                peaks[i] = magnitudes[i];
            } else {
                peaks[i] *= 0.995f;  // Slow decay for peaks
            }
        }
    }

    /**
     * Clear all buffers.
     */
    public void clearBuffers() {
        Arrays.fill(inputBuffer, 0);
        Arrays.fill(outputBuffer, 0);
        Arrays.fill(inputMagnitudes, 0);
        Arrays.fill(outputMagnitudes, 0);
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

        // Input spectrum on LEFT
        drawSpectrumPanel(g2d, "INPUT SPECTRUM", inputMagnitudes, inputPeaks,
                margin, margin + 20, panelWidth, panelHeight, INPUT_COLOR);

        // Output spectrum on RIGHT
        drawSpectrumPanel(g2d, "OUTPUT SPECTRUM", outputMagnitudes, outputPeaks,
                margin + panelWidth + gap, margin + 20, panelWidth, panelHeight, OUTPUT_COLOR);

        g2d.dispose();
    }

    /**
     * Draw a spectrum panel.
     */
    private void drawSpectrumPanel(Graphics2D g, String title, float[] magnitudes, float[] peaks,
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
            g.drawLine(x, gridY, x + width, gridY);
        }

        // Draw vertical frequency markers
        g.setFont(DarkTheme.FONT_SMALL.deriveFont(8f));
        fm = g.getFontMetrics();
        float[] freqMarkers = {100, 1000, 10000};
        String[] freqLabels = {"100", "1k", "10k"};

        for (int i = 0; i < freqMarkers.length; i++) {
            float freq = freqMarkers[i];
            // Find position on logarithmic scale
            float t = (float) (Math.log(freq / MIN_FREQ) / Math.log(MAX_FREQ / MIN_FREQ));
            int markerX = x + (int) (t * width);

            if (markerX > x && markerX < x + width) {
                g.setColor(GRID_COLOR);
                g.drawLine(markerX, y, markerX, y + height);
                g.setColor(DarkTheme.TEXT_DISABLED);
                g.drawString(freqLabels[i], markerX - fm.stringWidth(freqLabels[i]) / 2, y + height + 12);
            }
        }

        // Draw spectrum fill
        int[] xPoints = new int[NUM_DISPLAY_BINS + 2];
        int[] yPoints = new int[NUM_DISPLAY_BINS + 2];

        xPoints[0] = x;
        yPoints[0] = y + height;

        for (int i = 0; i < NUM_DISPLAY_BINS; i++) {
            int drawX = x + (i * width) / NUM_DISPLAY_BINS;
            int drawY = y + height - (int) (magnitudes[i] * (height - 4));
            drawY = Math.max(y + 2, Math.min(y + height - 2, drawY));

            xPoints[i + 1] = drawX;
            yPoints[i + 1] = drawY;
        }

        xPoints[NUM_DISPLAY_BINS + 1] = x + width;
        yPoints[NUM_DISPLAY_BINS + 1] = y + height;

        // Fill
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g.fillPolygon(xPoints, yPoints, NUM_DISPLAY_BINS + 2);

        // Draw spectrum line
        g.setColor(color);
        g.setStroke(new BasicStroke(1.5f));

        for (int i = 1; i < NUM_DISPLAY_BINS; i++) {
            int x1 = x + ((i - 1) * width) / NUM_DISPLAY_BINS;
            int y1 = y + height - (int) (magnitudes[i - 1] * (height - 4));
            int x2 = x + (i * width) / NUM_DISPLAY_BINS;
            int y2 = y + height - (int) (magnitudes[i] * (height - 4));

            y1 = Math.max(y + 2, Math.min(y + height - 2, y1));
            y2 = Math.max(y + 2, Math.min(y + height - 2, y2));

            g.drawLine(x1, y1, x2, y2);
        }

        // Draw peak line
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g.setStroke(new BasicStroke(0.5f));

        for (int i = 1; i < NUM_DISPLAY_BINS; i++) {
            int x1 = x + ((i - 1) * width) / NUM_DISPLAY_BINS;
            int y1 = y + height - (int) (peaks[i - 1] * (height - 4));
            int x2 = x + (i * width) / NUM_DISPLAY_BINS;
            int y2 = y + height - (int) (peaks[i] * (height - 4));

            y1 = Math.max(y + 2, Math.min(y + height - 2, y1));
            y2 = Math.max(y + 2, Math.min(y + height - 2, y2));

            g.drawLine(x1, y1, x2, y2);
        }

        // Draw border
        g.setColor(new Color(0x4a4a7e));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, width, height, 6, 6);
    }
}
