package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Panel that displays scrolling input/output waveforms for an effect.
 * Input waveform on the LEFT, Output waveform on the RIGHT.
 * Displays approximately 10 seconds of signal history, scrolling left.
 */
public class SignalWaveformPanel extends JPanel {

    // Display settings
    private static final int DISPLAY_WIDTH = 400;      // Pixels per waveform panel
    private static final int WAVEFORM_HEIGHT = 120;    // Height per waveform
    private static final Color INPUT_COLOR = new Color(0x00d9ff);   // Cyan
    private static final Color OUTPUT_COLOR = new Color(0x00ff88);  // Green
    private static final Color GRID_COLOR = new Color(0x2a2a4e);
    private static final Color ZERO_LINE_COLOR = new Color(0x3a3a6e);
    private static final Color TIME_MARKER_COLOR = new Color(0x4a4a7e);

    // History settings for ~10 seconds of display
    // At 44100 Hz with 512 buffer size = ~86 buffers/sec
    // We store peak values per column, so we need DISPLAY_WIDTH columns
    // Each column represents (10 seconds / DISPLAY_WIDTH) = 25ms of audio
    private static final int HISTORY_SIZE = DISPLAY_WIDTH;
    private static final float DISPLAY_SECONDS = 10.0f;

    // Peak history buffers (store max absolute value per time slice)
    private final float[] inputPeakHistory = new float[HISTORY_SIZE];
    private final float[] outputPeakHistory = new float[HISTORY_SIZE];
    private int historyWriteIndex = 0;

    // Accumulation buffers for decimation
    private float inputPeakAccum = 0;
    private float outputPeakAccum = 0;
    private int samplesAccumulated = 0;
    private int samplesPerColumn = 1;  // Will be calculated based on sample rate

    // Sample rate for time calculations
    private int sampleRate = 44100;

    // Current effect being monitored
    private AudioEffect effect;

    // Update timer
    private Timer updateTimer;

    public SignalWaveformPanel() {
        setBackground(DarkTheme.BG_DARK);
        setPreferredSize(new Dimension(DISPLAY_WIDTH * 2 + 60, WAVEFORM_HEIGHT + 40));
        setLayout(new BorderLayout());
        clearBuffers();
    }

    /**
     * Set the effect to monitor.
     */
    public void setEffect(AudioEffect effect) {
        this.effect = effect;
    }

    /**
     * Set the sample rate (used to calculate decimation factor).
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        // Calculate samples per column for ~10 seconds of display
        // Total samples in 10 seconds / number of columns
        float totalSamples = sampleRate * DISPLAY_SECONDS;
        samplesPerColumn = Math.max(1, (int) (totalSamples / HISTORY_SIZE));
    }

    /**
     * Start updating the waveform display.
     */
    public void startUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
        // Calculate decimation based on default sample rate
        setSampleRate(sampleRate);
        updateTimer = new Timer(33, e -> repaint());  // ~30 FPS
        updateTimer.start();
    }

    /**
     * Stop updating the waveform display.
     */
    public void stopUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
            updateTimer = null;
        }
    }

    /**
     * Add new samples to the waveform history (stereo version).
     * Called from the audio processing thread.
     */
    public void addSamples(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int count) {
        // For stereo, use max of L and R channels
        for (int i = 0; i < count; i++) {
            float inSample = 0;
            float outSample = 0;

            if (inputL != null && i < inputL.length) {
                inSample = Math.abs(inputL[i]);
            }
            if (inputR != null && i < inputR.length) {
                inSample = Math.max(inSample, Math.abs(inputR[i]));
            }
            if (outputL != null && i < outputL.length) {
                outSample = Math.abs(outputL[i]);
            }
            if (outputR != null && i < outputR.length) {
                outSample = Math.max(outSample, Math.abs(outputR[i]));
            }

            accumulateSample(inSample, outSample);
        }
    }

    /**
     * Add mono samples to the waveform history.
     * Called from the audio processing thread.
     */
    public void addSamplesMono(float[] input, float[] output, int count) {
        for (int i = 0; i < count; i++) {
            float inSample = (input != null && i < input.length) ? Math.abs(input[i]) : 0;
            float outSample = (output != null && i < output.length) ? Math.abs(output[i]) : 0;
            accumulateSample(inSample, outSample);
        }
    }

    /**
     * Accumulate a sample and write to history when enough samples collected.
     */
    private void accumulateSample(float inputAbs, float outputAbs) {
        // Track peak values
        inputPeakAccum = Math.max(inputPeakAccum, inputAbs);
        outputPeakAccum = Math.max(outputPeakAccum, outputAbs);
        samplesAccumulated++;

        // When we have enough samples for one column, write to history
        if (samplesAccumulated >= samplesPerColumn) {
            // Write peak values to history (circular buffer)
            inputPeakHistory[historyWriteIndex] = inputPeakAccum;
            outputPeakHistory[historyWriteIndex] = outputPeakAccum;

            // Advance write index
            historyWriteIndex = (historyWriteIndex + 1) % HISTORY_SIZE;

            // Reset accumulators
            inputPeakAccum = 0;
            outputPeakAccum = 0;
            samplesAccumulated = 0;
        }
    }

    /**
     * Clear all waveform buffers.
     */
    public void clearBuffers() {
        Arrays.fill(inputPeakHistory, 0);
        Arrays.fill(outputPeakHistory, 0);
        historyWriteIndex = 0;
        inputPeakAccum = 0;
        outputPeakAccum = 0;
        samplesAccumulated = 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int margin = 10;
        int gap = 20;  // Gap between input and output panels

        // Calculate panel dimensions
        int panelWidth = (width - gap - margin * 2) / 2;
        int panelHeight = height - margin * 2 - 25;  // Leave room for labels

        // Input panel on LEFT
        int inputX = margin;
        int inputY = margin + 20;
        drawWaveformPanel(g2d, "INPUT", inputPeakHistory, inputX, inputY,
                panelWidth, panelHeight, INPUT_COLOR);

        // Output panel on RIGHT
        int outputX = margin + panelWidth + gap;
        int outputY = margin + 20;
        drawWaveformPanel(g2d, "OUTPUT", outputPeakHistory, outputX, outputY,
                panelWidth, panelHeight, OUTPUT_COLOR);

        g2d.dispose();
    }

    /**
     * Draw a waveform panel with title and time scale.
     */
    private void drawWaveformPanel(Graphics2D g, String title, float[] peakHistory,
                                    int x, int y, int width, int height, Color color) {
        // Draw title
        g.setFont(DarkTheme.FONT_BOLD.deriveFont(12f));
        g.setColor(color);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, x + (width - fm.stringWidth(title)) / 2, y - 5);

        // Draw waveform background
        g.setColor(DarkTheme.BG_MEDIUM);
        g.fillRoundRect(x, y, width, height, 6, 6);

        // Draw horizontal grid lines
        g.setColor(GRID_COLOR);
        int gridLines = 4;
        for (int i = 1; i < gridLines; i++) {
            int gridY = y + (height * i) / gridLines;
            g.drawLine(x, gridY, x + width, gridY);
        }

        // Draw zero/center line
        g.setColor(ZERO_LINE_COLOR);
        int centerY = y + height / 2;
        g.drawLine(x, centerY, x + width, centerY);

        // Draw vertical time markers (every second)
        g.setColor(TIME_MARKER_COLOR);
        g.setFont(DarkTheme.FONT_SMALL.deriveFont(8f));
        fm = g.getFontMetrics();
        float columnsPerSecond = HISTORY_SIZE / DISPLAY_SECONDS;
        for (int sec = 1; sec < (int) DISPLAY_SECONDS; sec++) {
            int markerX = x + width - (int) (sec * columnsPerSecond * width / HISTORY_SIZE);
            if (markerX > x && markerX < x + width) {
                g.drawLine(markerX, y, markerX, y + height);
                String timeLabel = "-" + sec + "s";
                g.drawString(timeLabel, markerX - fm.stringWidth(timeLabel) / 2, y + height + 12);
            }
        }

        // Draw waveform (envelope display - mirrored around center)
        g.setColor(color);
        g.setStroke(new BasicStroke(1.0f));

        // Scale factor to fit width
        float scaleX = (float) width / HISTORY_SIZE;

        // Draw filled envelope
        int[] xPoints = new int[HISTORY_SIZE * 2 + 2];
        int[] yPoints = new int[HISTORY_SIZE * 2 + 2];
        int pointCount = 0;

        // Top half (positive peaks) - from right to left (newest to oldest)
        for (int i = 0; i < HISTORY_SIZE; i++) {
            // Read from oldest to newest (scroll left effect)
            // historyWriteIndex points to next write position (oldest data)
            int histIdx = (historyWriteIndex + i) % HISTORY_SIZE;
            float peak = Math.min(1.0f, peakHistory[histIdx]);

            int drawX = x + (int) (i * scaleX);
            int drawY = centerY - (int) (peak * (height / 2 - 4));

            xPoints[pointCount] = drawX;
            yPoints[pointCount] = drawY;
            pointCount++;
        }

        // Bottom half (mirrored) - from left to right
        for (int i = HISTORY_SIZE - 1; i >= 0; i--) {
            int histIdx = (historyWriteIndex + i) % HISTORY_SIZE;
            float peak = Math.min(1.0f, peakHistory[histIdx]);

            int drawX = x + (int) (i * scaleX);
            int drawY = centerY + (int) (peak * (height / 2 - 4));

            xPoints[pointCount] = drawX;
            yPoints[pointCount] = drawY;
            pointCount++;
        }

        // Fill the envelope
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
        g.fillPolygon(xPoints, yPoints, pointCount);

        // Draw envelope outline
        g.setColor(color);
        g.setStroke(new BasicStroke(1.2f));

        // Draw top line
        for (int i = 1; i < HISTORY_SIZE; i++) {
            int histIdx1 = (historyWriteIndex + i - 1) % HISTORY_SIZE;
            int histIdx2 = (historyWriteIndex + i) % HISTORY_SIZE;

            float peak1 = Math.min(1.0f, peakHistory[histIdx1]);
            float peak2 = Math.min(1.0f, peakHistory[histIdx2]);

            int x1 = x + (int) ((i - 1) * scaleX);
            int y1 = centerY - (int) (peak1 * (height / 2 - 4));
            int x2 = x + (int) (i * scaleX);
            int y2 = centerY - (int) (peak2 * (height / 2 - 4));

            g.drawLine(x1, y1, x2, y2);
        }

        // Draw bottom line (mirrored)
        for (int i = 1; i < HISTORY_SIZE; i++) {
            int histIdx1 = (historyWriteIndex + i - 1) % HISTORY_SIZE;
            int histIdx2 = (historyWriteIndex + i) % HISTORY_SIZE;

            float peak1 = Math.min(1.0f, peakHistory[histIdx1]);
            float peak2 = Math.min(1.0f, peakHistory[histIdx2]);

            int x1 = x + (int) ((i - 1) * scaleX);
            int y1 = centerY + (int) (peak1 * (height / 2 - 4));
            int x2 = x + (int) (i * scaleX);
            int y2 = centerY + (int) (peak2 * (height / 2 - 4));

            g.drawLine(x1, y1, x2, y2);
        }

        // Draw border
        g.setColor(new Color(0x4a4a7e));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, width, height, 6, 6);

        // Draw "NOW" indicator at right edge
        g.setColor(color.brighter());
        g.setFont(DarkTheme.FONT_SMALL.deriveFont(Font.BOLD, 8f));
        g.drawString("NOW", x + width - 22, y + height + 12);
    }
}
