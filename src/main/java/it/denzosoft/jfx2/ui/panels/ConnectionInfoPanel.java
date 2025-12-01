package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.graph.Connection;
import it.denzosoft.jfx2.graph.Port;
import it.denzosoft.jfx2.graph.SignalFlowAnalyzer;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Arrays;

/**
 * Panel that displays connection information including signal type,
 * real-time waveform, and FFT visualization.
 */
public class ConnectionInfoPanel extends JPanel {

    // ==================== CONSTANTS ====================
    private static final int PADDING = 16;
    private static final int WAVEFORM_HEIGHT = 60;
    private static final int FFT_HEIGHT = 60;
    private static final int FFT_BINS = 256;

    // ==================== CURRENT CONNECTION ====================
    private Connection currentConnection;
    private SignalFlowAnalyzer.SignalType signalType = SignalFlowAnalyzer.SignalType.UNKNOWN;

    // ==================== UI COMPONENTS ====================
    private JPanel headerPanel;
    private JLabel connectionLabel;
    private JLabel signalTypeLabel;
    private WaveformPanel waveformPanel;
    private FFTPanel fftPanel;
    private JPanel placeholderPanel;

    // ==================== AUDIO DATA ====================
    private float[] waveformBufferL;
    private float[] waveformBufferR;
    private float[] fftMagnitudes;
    private boolean isStereo = false;

    // ==================== UPDATE TIMER ====================
    private Timer updateTimer;
    private static final int UPDATE_INTERVAL_MS = 33; // ~30 FPS

    public ConnectionInfoPanel() {
        setLayout(new BorderLayout());
        setBackground(DarkTheme.BG_LIGHT);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DarkTheme.BG_DARK));

        createHeaderPanel();
        createVisualizationPanel();
        createPlaceholder();

        showPlaceholder();

        // Start update timer
        updateTimer = new Timer(UPDATE_INTERVAL_MS, e -> updateVisualization());
        updateTimer.start();
    }

    /**
     * Create the header panel with connection info.
     */
    private void createHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout(16, 0));
        headerPanel.setBackground(DarkTheme.BG_MEDIUM);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, PADDING, 8, PADDING));

        // Left: Connection name
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        connectionLabel = new JLabel("Connection");
        connectionLabel.setFont(DarkTheme.FONT_BOLD.deriveFont(14f));
        connectionLabel.setForeground(DarkTheme.TEXT_PRIMARY);

        signalTypeLabel = new JLabel("");
        signalTypeLabel.setFont(DarkTheme.FONT_SMALL);
        signalTypeLabel.setForeground(DarkTheme.TEXT_SECONDARY);

        leftPanel.add(connectionLabel);
        leftPanel.add(signalTypeLabel);

        headerPanel.add(leftPanel, BorderLayout.CENTER);
    }

    /**
     * Create the visualization panels (waveform and FFT).
     */
    private void createVisualizationPanel() {
        JPanel visualPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        visualPanel.setBackground(DarkTheme.BG_LIGHT);
        visualPanel.setBorder(BorderFactory.createEmptyBorder(8, PADDING, 8, PADDING));

        // Waveform panel
        waveformPanel = new WaveformPanel();
        waveformPanel.setPreferredSize(new Dimension(200, WAVEFORM_HEIGHT));

        // FFT panel
        fftPanel = new FFTPanel();
        fftPanel.setPreferredSize(new Dimension(200, FFT_HEIGHT));

        visualPanel.add(waveformPanel);
        visualPanel.add(fftPanel);

        // Main content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(DarkTheme.BG_LIGHT);
        contentPanel.add(headerPanel, BorderLayout.NORTH);
        contentPanel.add(visualPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Create placeholder panel.
     */
    private void createPlaceholder() {
        placeholderPanel = new JPanel(new GridBagLayout());
        placeholderPanel.setBackground(DarkTheme.BG_LIGHT);

        JLabel label = new JLabel("Select a connection to view signal info");
        label.setFont(DarkTheme.FONT_REGULAR);
        label.setForeground(DarkTheme.TEXT_SECONDARY);
        placeholderPanel.add(label);
    }

    /**
     * Show the placeholder.
     */
    public void showPlaceholder() {
        removeAll();
        add(placeholderPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    /**
     * Show the connection info panels.
     */
    private void showConnectionInfo() {
        removeAll();

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(DarkTheme.BG_LIGHT);
        contentPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel visualPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        visualPanel.setBackground(DarkTheme.BG_LIGHT);
        visualPanel.setBorder(BorderFactory.createEmptyBorder(8, PADDING, 8, PADDING));
        visualPanel.add(waveformPanel);
        visualPanel.add(fftPanel);

        contentPanel.add(visualPanel, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /**
     * Set the connection to display.
     */
    public void setConnection(Connection connection, SignalFlowAnalyzer.SignalType type) {
        this.currentConnection = connection;
        this.signalType = type;

        if (connection == null) {
            showPlaceholder();
            return;
        }

        // Update labels
        String sourceName = connection.getSourceNode().getName();
        String targetName = connection.getTargetNode().getName();
        connectionLabel.setText(sourceName + " → " + targetName);

        // Update signal type
        isStereo = (type == SignalFlowAnalyzer.SignalType.STEREO);
        String typeText = switch (type) {
            case MONO -> "MONO";
            case STEREO -> "STEREO";
            default -> "UNKNOWN";
        };
        signalTypeLabel.setText(" [" + typeText + "]");
        signalTypeLabel.setForeground(isStereo ? DarkTheme.CONNECTION_STEREO : DarkTheme.CONNECTION_MONO);

        showConnectionInfo();
    }

    /**
     * Update visualization from current connection buffer.
     */
    private void updateVisualization() {
        if (currentConnection == null || !isVisible()) {
            return;
        }

        Port sourcePort = currentConnection.getSourcePort();
        if (sourcePort == null) {
            return;
        }

        float[] buffer = sourcePort.getBuffer();
        if (buffer == null || buffer.length == 0) {
            return;
        }

        if (isStereo) {
            // Stereo: buffer is interleaved (L, R, L, R, ...)
            int frameCount = buffer.length / 2;
            int len = Math.min(frameCount, 1024);

            if (waveformBufferL == null || waveformBufferL.length != len) {
                waveformBufferL = new float[len];
                waveformBufferR = new float[len];
                fftMagnitudes = new float[FFT_BINS];
            }

            // Deinterleave stereo buffer
            for (int i = 0; i < len && i * 2 + 1 < buffer.length; i++) {
                waveformBufferL[i] = buffer[i * 2];       // Left channel
                waveformBufferR[i] = buffer[i * 2 + 1];   // Right channel
            }
        } else {
            // Mono: buffer is single channel
            int len = Math.min(buffer.length, 1024);

            if (waveformBufferL == null || waveformBufferL.length != len) {
                waveformBufferL = new float[len];
                waveformBufferR = new float[len];
                fftMagnitudes = new float[FFT_BINS];
            }

            System.arraycopy(buffer, 0, waveformBufferL, 0, len);
            System.arraycopy(buffer, 0, waveformBufferR, 0, len);
        }

        // Compute FFT magnitudes (use left channel for FFT)
        computeFFT(waveformBufferL, fftMagnitudes);

        // Update visualizations
        waveformPanel.setData(waveformBufferL, waveformBufferR, isStereo);
        fftPanel.setData(fftMagnitudes);
    }

    // FFT work arrays (reused to avoid allocations)
    private double[] fftReal;
    private double[] fftImag;
    private double[] fftMagLinear;

    /**
     * Compute FFT and map to logarithmic display.
     * Uses Cooley-Tukey radix-2 FFT algorithm.
     */
    private void computeFFT(float[] input, float[] magnitudes) {
        int n = input.length;
        int sampleRate = 44100;

        // Find next power of 2 for FFT
        int fftSize = 1;
        while (fftSize < n) fftSize *= 2;

        // Allocate work arrays if needed
        if (fftReal == null || fftReal.length != fftSize) {
            fftReal = new double[fftSize];
            fftImag = new double[fftSize];
            fftMagLinear = new double[fftSize / 2];
        }

        // Apply Hann window and copy to FFT buffer
        for (int i = 0; i < fftSize; i++) {
            if (i < n) {
                double window = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
                fftReal[i] = input[i] * window;
            } else {
                fftReal[i] = 0; // Zero padding
            }
            fftImag[i] = 0;
        }

        // Compute FFT in-place (Cooley-Tukey radix-2)
        fftInPlace(fftReal, fftImag, fftSize);

        // Compute magnitude spectrum (linear bins)
        for (int k = 0; k < fftSize / 2; k++) {
            double mag = Math.sqrt(fftReal[k] * fftReal[k] + fftImag[k] * fftImag[k]) / fftSize;
            fftMagLinear[k] = mag;
        }

        // Map linear FFT bins to logarithmic output bins
        double minFreq = 20.0;
        double maxFreq = 20000.0;
        double logMin = Math.log10(minFreq);
        double logMax = Math.log10(maxFreq);
        int numBins = magnitudes.length;

        for (int k = 0; k < numBins; k++) {
            // Map output bin to frequency (logarithmic scale)
            double logFreq = logMin + (double) k / numBins * (logMax - logMin);
            double freq = Math.pow(10.0, logFreq);

            // Convert frequency to linear FFT bin (with interpolation)
            double binFloat = freq * fftSize / sampleRate;
            int bin = (int) binFloat;
            double frac = binFloat - bin;

            // Clamp to valid range
            bin = Math.max(0, Math.min(bin, fftSize / 2 - 2));

            // Linear interpolation between adjacent bins
            double mag = fftMagLinear[bin] * (1 - frac) + fftMagLinear[bin + 1] * frac;

            // Convert to dB, clamp to -60..0 dB range
            double db = 20 * Math.log10(Math.max(mag, 1e-10));
            magnitudes[k] = (float) Math.max(0, (db + 60) / 60); // Normalize to 0..1
        }
    }

    /**
     * In-place Cooley-Tukey radix-2 FFT.
     */
    private void fftInPlace(double[] real, double[] imag, int n) {
        // Bit-reversal permutation
        int j = 0;
        for (int i = 0; i < n - 1; i++) {
            if (i < j) {
                double tempR = real[i];
                double tempI = imag[i];
                real[i] = real[j];
                imag[i] = imag[j];
                real[j] = tempR;
                imag[j] = tempI;
            }
            int k = n / 2;
            while (k <= j) {
                j -= k;
                k /= 2;
            }
            j += k;
        }

        // Cooley-Tukey decimation-in-time
        for (int len = 2; len <= n; len *= 2) {
            double angle = -2 * Math.PI / len;
            double wR = Math.cos(angle);
            double wI = Math.sin(angle);

            for (int i = 0; i < n; i += len) {
                double curR = 1.0;
                double curI = 0.0;

                for (int k = 0; k < len / 2; k++) {
                    int u = i + k;
                    int v = i + k + len / 2;

                    double tR = curR * real[v] - curI * imag[v];
                    double tI = curR * imag[v] + curI * real[v];

                    real[v] = real[u] - tR;
                    imag[v] = imag[u] - tI;
                    real[u] = real[u] + tR;
                    imag[u] = imag[u] + tI;

                    double nextR = curR * wR - curI * wI;
                    double nextI = curR * wI + curI * wR;
                    curR = nextR;
                    curI = nextI;
                }
            }
        }
    }

    /**
     * Stop the update timer when panel is not visible.
     */
    public void stopUpdates() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }

    /**
     * Start the update timer.
     */
    public void startUpdates() {
        if (updateTimer != null && !updateTimer.isRunning()) {
            updateTimer.start();
        }
    }

    // ==================== WAVEFORM PANEL ====================

    /**
     * Panel that displays the waveform.
     */
    private class WaveformPanel extends JPanel {
        private float[] dataL;
        private float[] dataR;
        private boolean stereo = false;

        WaveformPanel() {
            setBackground(DarkTheme.BG_DARK);
            setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1));
        }

        void setData(float[] left, float[] right, boolean isStereo) {
            this.dataL = left;
            this.dataR = right;
            this.stereo = isStereo;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(8f));
            FontMetrics fm = g2d.getFontMetrics();

            if (stereo) {
                // Stereo: draw two halves with their own grid
                int halfH = h / 2;

                // Top half (Left channel)
                drawWaveformGrid(g2d, 0, halfH, w, fm);
                // Bottom half (Right channel)
                drawWaveformGrid(g2d, halfH, halfH, w, fm);

                // Draw waveforms
                g2d.setStroke(new BasicStroke(1.5f));
                if (dataL != null && dataL.length > 0) {
                    drawWaveformHalf(g2d, dataL, 0, halfH, w, DarkTheme.CONNECTION_MONO);
                }
                if (dataR != null && dataR.length > 0) {
                    drawWaveformHalf(g2d, dataR, halfH, halfH, w, DarkTheme.CONNECTION_STEREO);
                }

                // Channel labels
                g2d.setColor(DarkTheme.TEXT_SECONDARY);
                g2d.drawString("L", 3, 10);
                g2d.drawString("R", 3, halfH + 10);
            } else {
                // Mono: single waveform with grid
                drawWaveformGrid(g2d, 0, h, w, fm);

                // Draw waveform
                g2d.setStroke(new BasicStroke(1.5f));
                if (dataL != null && dataL.length > 0) {
                    drawWaveformFull(g2d, dataL, w, h, DarkTheme.CONNECTION_MONO);
                }
            }

            g2d.dispose();
        }

        /**
         * Draw the waveform grid with -1, 0, +1 reference lines.
         */
        private void drawWaveformGrid(Graphics2D g2d, int yOffset, int height, int w, FontMetrics fm) {
            int centerY = yOffset + height / 2;
            int amplitude = height / 2 - 2;
            int yTop = centerY - amplitude;    // +1 line
            int yBottom = centerY + amplitude; // -1 line

            // Draw +1 line
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.drawLine(0, yTop, w, yTop);

            // Draw center (0) line
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.drawLine(0, centerY, w, centerY);

            // Draw -1 line
            g2d.setColor(new Color(255, 255, 255, 40));
            g2d.drawLine(0, yBottom, w, yBottom);

            // Draw labels on the right side
            g2d.setColor(new Color(DarkTheme.TEXT_SECONDARY.getRGB() & 0x00FFFFFF | 0xA0000000, true));
            String labelPlus = "+1";
            String labelZero = "0";
            String labelMinus = "-1";

            int labelX = w - fm.stringWidth(labelMinus) - 3;
            g2d.drawString(labelPlus, labelX, yTop + 4);
            g2d.drawString(labelZero, w - fm.stringWidth(labelZero) - 3, centerY + 3);
            g2d.drawString(labelMinus, labelX, yBottom + 4);
        }

        private void drawWaveformFull(Graphics2D g2d, float[] data, int w, int h, Color color) {
            g2d.setColor(color);
            Path2D path = new Path2D.Float();
            int centerY = h / 2;
            int amplitude = h / 2 - 4;

            int step = Math.max(1, data.length / w);
            boolean first = true;

            for (int x = 0; x < w && x * step < data.length; x++) {
                float sample = data[x * step];
                int y = centerY - (int) (sample * amplitude);
                y = Math.max(2, Math.min(h - 2, y));

                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.draw(path);
        }

        private void drawWaveformHalf(Graphics2D g2d, float[] data, int yOffset, int height, int w, Color color) {
            g2d.setColor(color);
            Path2D path = new Path2D.Float();
            int centerY = yOffset + height / 2;
            int amplitude = height / 2 - 2;

            int step = Math.max(1, data.length / w);
            boolean first = true;

            for (int x = 0; x < w && x * step < data.length; x++) {
                float sample = data[x * step];
                int y = centerY - (int) (sample * amplitude);
                y = Math.max(yOffset + 2, Math.min(yOffset + height - 2, y));

                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.draw(path);
        }
    }

    // ==================== FFT PANEL ====================

    /**
     * Panel that displays the FFT spectrum with musical note grid.
     */
    private class FFTPanel extends JPanel {
        private float[] magnitudes;

        // All natural notes (C, D, E, F, G, A, B) from C1 to C8 with frequencies
        // Only notes within 20Hz-20kHz range will be displayed
        private static final double[] ALL_NATURAL_FREQUENCIES = {
            // Octave 1
            32.70,   // C1
            36.71,   // D1
            41.20,   // E1
            43.65,   // F1
            49.00,   // G1
            55.00,   // A1
            61.74,   // B1
            // Octave 2
            65.41,   // C2
            73.42,   // D2
            82.41,   // E2 (low E string)
            87.31,   // F2
            98.00,   // G2
            110.00,  // A2
            123.47,  // B2
            // Octave 3
            130.81,  // C3
            146.83,  // D3
            164.81,  // E3
            174.61,  // F3
            196.00,  // G3
            220.00,  // A3
            246.94,  // B3
            // Octave 4
            261.63,  // C4 (middle C)
            293.66,  // D4
            329.63,  // E4 (high E string)
            349.23,  // F4
            392.00,  // G4
            440.00,  // A4 (concert pitch)
            493.88,  // B4
            // Octave 5
            523.25,  // C5
            587.33,  // D5
            659.25,  // E5
            698.46,  // F5
            783.99,  // G5
            880.00,  // A5
            987.77,  // B5
            // Octave 6
            1046.50, // C6
            1174.66, // D6
            1318.51, // E6
            1396.91, // F6
            1567.98, // G6
            1760.00, // A6
            1975.53, // B6
            // Octave 7
            2093.00, // C7
            2349.32, // D7
            2637.02, // E7
            2793.83, // F7
            3135.96, // G7
            3520.00, // A7
            3951.07, // B7
            // Octave 8
            4186.01, // C8
            4698.63, // D8
            5274.04, // E8
            5587.65, // F8
            6271.93, // G8
            7040.00, // A8
            7902.13, // B8
            // Octave 9
            8372.02, // C9
            9397.27, // D9
            10548.08,// E9
            11175.30,// F9
            12543.85,// G9
            14080.00,// A9
            15804.27 // B9
        };

        // Note names corresponding to ALL_NATURAL_FREQUENCIES
        private static final String[] ALL_NATURAL_NAMES = {
            "C1", "D1", "E1", "F1", "G1", "A1", "B1",
            "C2", "D2", "E2", "F2", "G2", "A2", "B2",
            "C3", "D3", "E3", "F3", "G3", "A3", "B3",
            "C4", "D4", "E4", "F4", "G4", "A4", "B4",
            "C5", "D5", "E5", "F5", "G5", "A5", "B5",
            "C6", "D6", "E6", "F6", "G6", "A6", "B6",
            "C7", "D7", "E7", "F7", "G7", "A7", "B7",
            "C8", "D8", "E8", "F8", "G8", "A8", "B8",
            "C9", "D9", "E9", "F9", "G9", "A9", "B9"
        };

        // Notes that should show labels (subset for cleaner display)
        // Includes C (Do), E (Mi) and A (La) for reference
        private static final String[] LABELED_NOTES = {
            "C2", "E2", "A2", "C3", "E3", "A3", "C4", "E4", "A4", "C5", "E5", "A5", "C6", "E6", "A6", "C7", "E7"
        };

        FFTPanel() {
            setBackground(DarkTheme.BG_DARK);
            setBorder(BorderFactory.createLineBorder(DarkTheme.BG_LIGHTER, 1));
        }

        private boolean shouldShowLabel(String noteName) {
            for (String labeled : LABELED_NOTES) {
                if (labeled.equals(noteName)) return true;
            }
            return false;
        }

        void setData(float[] mags) {
            this.magnitudes = mags;
            repaint();
        }

        /**
         * Convert frequency to x position using logarithmic scale.
         * FFT range: 1 Hz to 20 kHz (log10(1)=0 to log10(20000)=4.3)
         */
        private int frequencyToX(double freq, int width) {
            double minFreq = 20.0;
            double maxFreq = 20000.0;
            if (freq < minFreq || freq > maxFreq) {
                return -1;
            }
            double logMin = Math.log10(minFreq);
            double logMax = Math.log10(maxFreq);
            double logFreq = Math.log10(freq);
            return (int) ((logFreq - logMin) / (logMax - logMin) * width);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int topMargin = 20;    // Space for note labels and frequencies
            int bottomMargin = 2;  // Small space at bottom
            int barAreaHeight = h - topMargin - bottomMargin;

            if (barAreaHeight <= 0 || w <= 0) {
                g2d.dispose();
                return;
            }

            // Draw FFT bars first (background) - uses full width from x=0 to x=w
            if (magnitudes != null && magnitudes.length > 0) {
                int numBins = magnitudes.length;

                for (int i = 0; i < numBins; i++) {
                    float mag = magnitudes[i];
                    int barH = (int) (mag * barAreaHeight);

                    // Calculate x positions to fill entire width exactly
                    int x1 = (i * w) / numBins;
                    int x2 = ((i + 1) * w) / numBins;
                    int barWidth = x2 - x1;

                    if (barWidth > 0 && barH > 0) {
                        // Color gradient from blue to green based on magnitude
                        Color color = interpolateColor(DarkTheme.CONNECTION_MONO, DarkTheme.CONNECTION_STEREO, mag);
                        g2d.setColor(color);

                        int y = h - bottomMargin - barH;
                        g2d.fillRect(x1, y, barWidth, barH);
                    }
                }
            }

            // Draw note grid lines and labels (on top of bars)
            g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(7f));
            FontMetrics fm = g2d.getFontMetrics();

            for (int i = 0; i < ALL_NATURAL_FREQUENCIES.length; i++) {
                double freq = ALL_NATURAL_FREQUENCIES[i];
                int x = frequencyToX(freq, w);

                if (x >= 0 && x < w) {
                    String noteName = ALL_NATURAL_NAMES[i];
                    boolean showLabel = shouldShowLabel(noteName);

                    // Draw vertical grid line (brighter for labeled notes)
                    if (showLabel) {
                        g2d.setColor(new Color(255, 255, 255, 50));
                    } else {
                        g2d.setColor(new Color(255, 255, 255, 20));
                    }
                    g2d.drawLine(x, topMargin, x, h - bottomMargin);

                    // Draw note name and frequency for labeled notes
                    if (showLabel) {
                        int textWidth = fm.stringWidth(noteName);
                        int labelX = x - textWidth / 2;

                        // Only draw if it fits within bounds
                        if (labelX >= 2 && labelX + textWidth <= w - 2) {
                            // Note name at top
                            g2d.setColor(DarkTheme.TEXT_SECONDARY);
                            g2d.drawString(noteName, labelX, 8);

                            // Frequency below note name
                            String freqLabel = formatFrequency(freq);
                            int freqWidth = fm.stringWidth(freqLabel);
                            int freqX = x - freqWidth / 2;
                            g2d.setColor(new Color(DarkTheme.TEXT_SECONDARY.getRGB() & 0x00FFFFFF | 0x80000000, true));
                            g2d.drawString(freqLabel, freqX, 16);
                        }
                    }
                }
            }

            g2d.dispose();
        }

        /**
         * Format frequency for display (e.g., "440" or "1.3k")
         */
        private String formatFrequency(double freq) {
            if (freq >= 1000) {
                if (freq % 1000 == 0) {
                    return String.format("%.0fk", freq / 1000);
                } else {
                    return String.format("%.1fk", freq / 1000);
                }
            } else {
                return String.format("%.0f", freq);
            }
        }

        private Color interpolateColor(Color c1, Color c2, float t) {
            t = Math.max(0, Math.min(1, t));
            int r = (int) (c1.getRed() + t * (c2.getRed() - c1.getRed()));
            int gg = (int) (c1.getGreen() + t * (c2.getGreen() - c1.getGreen()));
            int b = (int) (c1.getBlue() + t * (c2.getBlue() - c1.getBlue()));
            return new Color(r, gg, b);
        }
    }
}
