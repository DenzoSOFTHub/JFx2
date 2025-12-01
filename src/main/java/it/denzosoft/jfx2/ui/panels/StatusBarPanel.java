package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.ui.icons.IconFactory;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * Status bar panel showing audio engine status, CPU usage, signal monitor, and level meters.
 */
public class StatusBarPanel extends JPanel {

    // ==================== METRICS ====================
    private float cpuUsage = 0f;
    private float latencyMs = 0f;
    private float inputLevel = -60f;  // dB
    private float outputLevel = -60f; // dB
    private boolean engineRunning = false;

    // ==================== UI COMPONENTS ====================
    private JLabel statusLabel;
    private JLabel cpuLabel;
    private JLabel latencyLabel;
    private SignalMonitorWidget signalMonitor;
    private TunerWidget tunerWidget;
    private LevelMeter inputMeter;
    private LevelMeter outputMeter;

    // ==================== CONSTANTS ====================
    private static final int HEIGHT = 28;

    public StatusBarPanel() {
        setLayout(new GridBagLayout());
        setBackground(DarkTheme.BG_DARK);
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, DarkTheme.BG_LIGHT));
        setPreferredSize(new Dimension(0, HEIGHT));
        setMinimumSize(new Dimension(600, HEIGHT));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.gridy = 0;

        int col = 0;

        // Status indicator
        gbc.gridx = col++;
        gbc.weightx = 0.05;
        add(createStatusSection(), gbc);

        gbc.gridx = col++;
        gbc.weightx = 0;
        add(createSeparator(), gbc);

        // DSP & Latency
        gbc.gridx = col++;
        gbc.weightx = 0.08;
        add(createMetricsSection(), gbc);

        gbc.gridx = col++;
        gbc.weightx = 0;
        add(createSeparator(), gbc);

        // Signal Monitor (L/R waveforms + spectrum) - larger weight
        gbc.gridx = col++;
        gbc.weightx = 0.35;
        signalMonitor = new SignalMonitorWidget();
        add(signalMonitor, gbc);

        gbc.gridx = col++;
        gbc.weightx = 0;
        add(createSeparator(), gbc);

        // Tuner
        gbc.gridx = col++;
        gbc.weightx = 0.12;
        tunerWidget = new TunerWidget();
        add(tunerWidget, gbc);

        gbc.gridx = col++;
        gbc.weightx = 0;
        add(createSeparator(), gbc);

        // Input meter
        gbc.gridx = col++;
        gbc.weightx = 0.18;
        add(createInputMeterSection(), gbc);

        gbc.gridx = col++;
        gbc.weightx = 0;
        add(createSeparator(), gbc);

        // Output meter
        gbc.gridx = col++;
        gbc.weightx = 0.18;
        add(createOutputMeterSection(), gbc);
    }

    private JPanel createStatusSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        statusLabel = new JLabel("Off", IconFactory.getIcon("stopped", 10), JLabel.CENTER);
        statusLabel.setFont(DarkTheme.FONT_SMALL);
        statusLabel.setForeground(DarkTheme.TEXT_DISABLED);
        statusLabel.setIconTextGap(3);
        panel.add(statusLabel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createMetricsSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
        panel.setOpaque(false);

        cpuLabel = new JLabel("0%", JLabel.CENTER);
        cpuLabel.setFont(DarkTheme.FONT_SMALL);
        cpuLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        cpuLabel.setToolTipText("DSP Load");
        panel.add(cpuLabel);

        latencyLabel = new JLabel("0ms", JLabel.CENTER);
        latencyLabel.setFont(DarkTheme.FONT_SMALL);
        latencyLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        latencyLabel.setToolTipText("Latency");
        panel.add(latencyLabel);

        return panel;
    }

    private JPanel createInputMeterSection() {
        JPanel panel = new JPanel(new BorderLayout(2, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel("I");
        label.setFont(DarkTheme.FONT_SMALL);
        label.setForeground(DarkTheme.TEXT_SECONDARY);
        label.setToolTipText("Input Level");
        panel.add(label, BorderLayout.WEST);

        inputMeter = new LevelMeter();
        panel.add(inputMeter, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createOutputMeterSection() {
        JPanel panel = new JPanel(new BorderLayout(2, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel("O");
        label.setFont(DarkTheme.FONT_SMALL);
        label.setForeground(DarkTheme.TEXT_SECONDARY);
        label.setToolTipText("Output Level");
        panel.add(label, BorderLayout.WEST);

        outputMeter = new LevelMeter();
        panel.add(outputMeter, BorderLayout.CENTER);

        return panel;
    }

    private JSeparator createSeparator() {
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setForeground(DarkTheme.BG_LIGHTER);
        sep.setPreferredSize(new Dimension(1, HEIGHT - 8));
        return sep;
    }

    // ==================== UPDATE METHODS ====================

    /**
     * Set engine running status.
     */
    public void setEngineRunning(boolean running) {
        this.engineRunning = running;
        if (running) {
            statusLabel.setIcon(IconFactory.getIcon("running", 10));
            statusLabel.setText("On");
            statusLabel.setForeground(DarkTheme.ACCENT_SUCCESS);
        } else {
            statusLabel.setIcon(IconFactory.getIcon("stopped", 10));
            statusLabel.setText("Off");
            statusLabel.setForeground(DarkTheme.TEXT_DISABLED);
            // Reset signal monitor when stopped
            if (signalMonitor != null) {
                signalMonitor.reset();
            }
        }
    }

    /**
     * Set DSP load percentage.
     */
    public void setCpuUsage(float percentage) {
        this.cpuUsage = percentage;
        cpuLabel.setText(String.format("%.0f%%", percentage));

        // Color based on usage
        if (percentage > 80) {
            cpuLabel.setForeground(DarkTheme.ACCENT_ERROR);
        } else if (percentage > 50) {
            cpuLabel.setForeground(DarkTheme.ACCENT_WARNING);
        } else {
            cpuLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        }
    }

    /**
     * Set latency in milliseconds.
     */
    public void setLatency(float ms) {
        this.latencyMs = ms;
        latencyLabel.setText(String.format("%.0fms", ms));
    }

    /**
     * Set input level in dB.
     */
    public void setInputLevel(float dbLevel) {
        this.inputLevel = dbLevel;
        inputMeter.setLevel(dbLevel);
    }

    /**
     * Set output level in dB.
     */
    public void setOutputLevel(float dbLevel) {
        this.outputLevel = dbLevel;
        outputMeter.setLevel(dbLevel);
    }

    /**
     * Set temporary message (no-op in compact mode).
     */
    public void setMessage(String message) {
        // No message area in compact status bar
    }

    /**
     * Clear message (no-op in compact mode).
     */
    public void clearMessage() {
        // No message area in compact status bar
    }

    /**
     * Feed input audio samples and update input meter in real-time.
     */
    public void feedInputAudio(float[] samples, int length) {
        // Feed to tuner
        if (tunerWidget != null) {
            tunerWidget.feedAudio(samples, length);
        }
        // Calculate and update input level meter
        if (inputMeter != null && samples != null && length > 0) {
            float rms = 0;
            for (int i = 0; i < length && i < samples.length; i++) {
                rms += samples[i] * samples[i];
            }
            rms = (float) Math.sqrt(rms / length);
            float db = rms > 0 ? (float) (20 * Math.log10(rms)) : -60f;
            inputMeter.setLevel(Math.max(-60f, Math.min(0f, db)));
        }
    }

    /**
     * Feed audio samples to the tuner for pitch detection.
     * @deprecated Use feedInputAudio instead
     */
    @Deprecated
    public void feedTunerAudio(float[] samples, int length) {
        feedInputAudio(samples, length);
    }

    /**
     * Set the sample rate for the tuner.
     */
    public void setTunerSampleRate(int sampleRate) {
        if (tunerWidget != null) {
            tunerWidget.setSampleRate(sampleRate);
        }
        if (signalMonitor != null) {
            signalMonitor.setSampleRate(sampleRate);
        }
    }

    /**
     * Reset the tuner.
     */
    public void resetTuner() {
        if (tunerWidget != null) {
            tunerWidget.reset();
        }
    }

    /**
     * Feed stereo audio samples to the signal monitor.
     */
    public void feedSignalMonitor(float[] left, float[] right, int length) {
        if (signalMonitor != null) {
            signalMonitor.feedAudio(left, right, length);
        }
    }

    /**
     * Feed mono audio samples to the signal monitor.
     */
    public void feedSignalMonitor(float[] mono, int length) {
        if (signalMonitor != null) {
            signalMonitor.feedAudio(mono, null, length);
        }
    }

    /**
     * Feed output audio samples and update output meter in real-time.
     */
    public void feedOutputAudio(float[] samples, int length) {
        // Feed to signal monitor
        if (signalMonitor != null) {
            signalMonitor.feedAudio(samples, null, length);
        }
        // Calculate and update output level meter
        if (outputMeter != null && samples != null && length > 0) {
            float rms = 0;
            for (int i = 0; i < length && i < samples.length; i++) {
                rms += samples[i] * samples[i];
            }
            rms = (float) Math.sqrt(rms / length);
            float db = rms > 0 ? (float) (20 * Math.log10(rms)) : -60f;
            outputMeter.setLevel(Math.max(-60f, Math.min(0f, db)));
        }
    }

    /**
     * Set the sample rate for the signal monitor.
     */
    public void setSignalMonitorSampleRate(int sampleRate) {
        if (signalMonitor != null) {
            signalMonitor.setSampleRate(sampleRate);
        }
    }

    /**
     * Feed FFT data from input audio to the signal monitor.
     */
    public void feedInputFFT(float[] magnitudes, int numBins, int sampleRate, float binFrequency) {
        if (signalMonitor != null) {
            signalMonitor.feedFFT(magnitudes, numBins, sampleRate, binFrequency);
        }
    }

    // ==================== SIGNAL MONITOR WIDGET ====================

    /**
     * Container widget holding L/R waveforms and spectrum analyzer.
     * Resizes to fill available space.
     */
    private static class SignalMonitorWidget extends JPanel {

        private final WaveformWidget waveformL;
        private final WaveformWidget waveformR;
        private final SpectrumWidget spectrum;

        public SignalMonitorWidget() {
            setLayout(new GridBagLayout());
            setOpaque(false);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.BOTH;
            gbc.gridy = 0;
            gbc.weighty = 1.0;
            gbc.insets = new Insets(0, 1, 0, 1);

            // L waveform (25% width)
            gbc.gridx = 0;
            gbc.weightx = 0.25;
            waveformL = new WaveformWidget(DarkTheme.ACCENT_PRIMARY);
            waveformL.setToolTipText("Left Channel");
            add(waveformL, gbc);

            // R waveform (25% width)
            gbc.gridx = 1;
            gbc.weightx = 0.25;
            waveformR = new WaveformWidget(DarkTheme.ACCENT_SECONDARY);
            waveformR.setToolTipText("Right Channel");
            add(waveformR, gbc);

            // Spectrum analyzer (50% width)
            gbc.gridx = 2;
            gbc.weightx = 0.50;
            spectrum = new SpectrumWidget();
            spectrum.setToolTipText("Spectrum Analyzer");
            add(spectrum, gbc);

            // Single refresh timer for all components
            Timer timer = new Timer(33, e -> {
                waveformL.repaint();
                waveformR.repaint();
                spectrum.repaint();
            });
            timer.start();
        }

        public void setSampleRate(int rate) {
            waveformL.setSampleRate(rate);
            waveformR.setSampleRate(rate);
        }

        public void feedAudio(float[] left, float[] right, int length) {
            waveformL.feedSamples(left, length);
            waveformR.feedSamples(right != null ? right : left, length);
        }

        public void feedFFT(float[] magnitudes, int numBins, int sr, float binFrequency) {
            spectrum.feedFFT(magnitudes, numBins, sr, binFrequency);
        }

        public void reset() {
            waveformL.reset();
            waveformR.reset();
            spectrum.reset();
        }
    }

    // ==================== WAVEFORM WIDGET ====================

    /**
     * Single channel waveform display - signal flows from right to left.
     * Resizes to fill available space.
     */
    private static class WaveformWidget extends JComponent {

        private static final int BUFFER_SAMPLES = 512; // ~4 buffers at 128 samples

        private final float[] buffer = new float[BUFFER_SAMPLES];
        private int writePos = 0;
        private final Color waveColor;
        private int sampleRate = 44100;

        public WaveformWidget(Color color) {
            this.waveColor = color;
            setMinimumSize(new Dimension(20, 12));
            setPreferredSize(new Dimension(60, 20));
        }

        public void setSampleRate(int rate) {
            this.sampleRate = rate;
        }

        public void feedSamples(float[] samples, int length) {
            // Write new samples to circular buffer
            for (int i = 0; i < length && i < samples.length; i++) {
                buffer[writePos] = samples[i];
                writePos = (writePos + 1) % buffer.length;
            }
        }

        public void reset() {
            Arrays.fill(buffer, 0);
            writePos = 0;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int midY = h / 2;

            // Background
            g2.setColor(DarkTheme.BG_DARK);
            g2.fillRoundRect(0, 0, w, h, 3, 3);

            // Center line
            g2.setColor(DarkTheme.GRID_LINE);
            g2.drawLine(0, midY, w, midY);

            // Draw waveform - signal enters from RIGHT and scrolls LEFT
            g2.setColor(waveColor);
            int samplesPerPixel = Math.max(1, buffer.length / w);

            int prevY = midY;
            for (int x = 0; x < w; x++) {
                // Calculate buffer index: rightmost pixel (x=w-1) = newest data
                // leftmost pixel (x=0) = oldest data
                int pixelAge = w - 1 - x; // 0 for rightmost, w-1 for leftmost
                int bufferIdx = (writePos - 1 - pixelAge * samplesPerPixel + buffer.length * 2) % buffer.length;

                // Find max amplitude for this pixel
                float maxVal = 0;
                for (int j = 0; j < samplesPerPixel; j++) {
                    int idx = (bufferIdx - j + buffer.length) % buffer.length;
                    maxVal = Math.max(maxVal, Math.abs(buffer[idx]));
                }

                // Draw as filled area from center
                int amplitude = (int) (maxVal * (h / 2 - 1));
                g2.drawLine(x, midY - amplitude, x, midY + amplitude);
            }

            // Border
            g2.setColor(DarkTheme.BG_LIGHTER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 3, 3);

            g2.dispose();
        }
    }

    // ==================== SPECTRUM WIDGET ====================

    /**
     * 16-band spectrum analyzer with colored bars.
     * Resizes to fill available space.
     */
    private static class SpectrumWidget extends JComponent {

        private static final int NUM_BANDS = 16;
        private static final float DECAY_RATE = 0.88f;
        private static final long PEAK_HOLD_MS = 400;

        private final float[] spectrumBands = new float[NUM_BANDS];
        private final float[] spectrumPeaks = new float[NUM_BANDS];
        private final long[] peakTimes = new long[NUM_BANDS];
        private final float[] bandFrequencies = new float[NUM_BANDS + 1];

        public SpectrumWidget() {
            setMinimumSize(new Dimension(32, 12));
            setPreferredSize(new Dimension(100, 20));

            // Calculate band frequencies (logarithmic 20Hz - 20kHz)
            for (int i = 0; i <= NUM_BANDS; i++) {
                bandFrequencies[i] = (float) (20 * Math.pow(1000, i / (float) NUM_BANDS));
            }
        }

        public void feedFFT(float[] magnitudes, int numBins, int sampleRate, float binFrequency) {
            for (int band = 0; band < NUM_BANDS; band++) {
                float freqLo = bandFrequencies[band];
                float freqHi = bandFrequencies[band + 1];

                int binLo = (int) (freqLo / binFrequency);
                int binHi = (int) (freqHi / binFrequency);
                binLo = Math.max(1, Math.min(numBins - 1, binLo));
                binHi = Math.max(binLo + 1, Math.min(numBins, binHi));

                float sumMag = 0;
                for (int k = binLo; k < binHi; k++) {
                    sumMag += magnitudes[k];
                }
                float avgMag = sumMag / (binHi - binLo);

                // Smoothing
                if (avgMag > spectrumBands[band]) {
                    spectrumBands[band] = avgMag;
                } else {
                    spectrumBands[band] = spectrumBands[band] * DECAY_RATE + avgMag * (1 - DECAY_RATE);
                }

                // Peak hold
                long now = System.currentTimeMillis();
                if (spectrumBands[band] > spectrumPeaks[band]) {
                    spectrumPeaks[band] = spectrumBands[band];
                    peakTimes[band] = now;
                } else if (now - peakTimes[band] > PEAK_HOLD_MS) {
                    spectrumPeaks[band] *= 0.95f;
                }
            }
        }

        public void reset() {
            Arrays.fill(spectrumBands, 0);
            Arrays.fill(spectrumPeaks, 0);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            int w = getWidth();
            int h = getHeight();

            // Background
            g2.setColor(DarkTheme.BG_DARK);
            g2.fillRoundRect(0, 0, w, h, 3, 3);

            int barWidth = Math.max(2, (w - 4) / NUM_BANDS - 1);
            int gap = 1;
            int startX = 2;

            for (int band = 0; band < NUM_BANDS; band++) {
                int barX = startX + band * (barWidth + gap);
                int barH = (int) (spectrumBands[band] * (h - 4));
                barH = Math.max(1, Math.min(h - 4, barH));

                // Color based on level (green -> yellow -> red)
                float level = spectrumBands[band];
                Color barColor;
                if (level < 0.4f) {
                    barColor = interpolateColor(DarkTheme.ACCENT_SUCCESS, DarkTheme.ACCENT_WARNING, level / 0.4f);
                } else if (level < 0.7f) {
                    barColor = interpolateColor(DarkTheme.ACCENT_WARNING, DarkTheme.ACCENT_ERROR, (level - 0.4f) / 0.3f);
                } else {
                    barColor = DarkTheme.ACCENT_ERROR;
                }

                // Draw bar from bottom
                g2.setColor(barColor);
                g2.fillRect(barX, h - 2 - barH, barWidth, barH);

                // Draw peak indicator
                int peakY = h - 2 - (int) (spectrumPeaks[band] * (h - 4));
                peakY = Math.max(2, peakY);
                g2.setColor(Color.WHITE);
                g2.drawLine(barX, peakY, barX + barWidth - 1, peakY);
            }

            // Border
            g2.setColor(DarkTheme.BG_LIGHTER);
            g2.drawRoundRect(0, 0, w - 1, h - 1, 3, 3);

            g2.dispose();
        }

        private static Color interpolateColor(Color c1, Color c2, float ratio) {
            ratio = Math.max(0, Math.min(1, ratio));
            int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * ratio);
            int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio);
            int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio);
            return new Color(r, g, b);
        }
    }

    // ==================== LEVEL METER COMPONENT ====================

    /**
     * Simple horizontal level meter.
     * Resizes to fill available space.
     */
    private static class LevelMeter extends JComponent {

        private static final float MIN_DB = -60f;
        private static final float MAX_DB = 0f;

        private float level = MIN_DB;
        private float peakLevel = MIN_DB;
        private long peakHoldTime = 0;
        private static final long PEAK_HOLD_MS = 1000;

        public LevelMeter() {
            setMinimumSize(new Dimension(40, 10));
            setPreferredSize(new Dimension(100, 14));
        }

        public void setLevel(float dbLevel) {
            this.level = Math.max(MIN_DB, Math.min(MAX_DB, dbLevel));

            // Update peak
            if (level > peakLevel) {
                peakLevel = level;
                peakHoldTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - peakHoldTime > PEAK_HOLD_MS) {
                peakLevel = level;
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background
            g2d.setColor(DarkTheme.BG_LIGHT);
            g2d.fillRoundRect(0, 0, w, h, 4, 4);

            // Calculate level position
            float normalized = (level - MIN_DB) / (MAX_DB - MIN_DB);
            int levelWidth = (int) (normalized * (w - 2));

            // Draw level gradient
            if (levelWidth > 0) {
                // Green to yellow to red gradient
                GradientPaint gradient;
                if (level > -6) {
                    gradient = new GradientPaint(
                            0, 0, DarkTheme.ACCENT_WARNING,
                            levelWidth, 0, DarkTheme.ACCENT_ERROR);
                } else if (level > -18) {
                    gradient = new GradientPaint(
                            0, 0, DarkTheme.ACCENT_SUCCESS,
                            levelWidth, 0, DarkTheme.ACCENT_WARNING);
                } else {
                    gradient = new GradientPaint(
                            0, 0, DarkTheme.ACCENT_SUCCESS.darker(),
                            levelWidth, 0, DarkTheme.ACCENT_SUCCESS);
                }

                g2d.setPaint(gradient);
                g2d.fillRoundRect(1, 1, levelWidth, h - 2, 3, 3);
            }

            // Peak indicator
            float peakNorm = (peakLevel - MIN_DB) / (MAX_DB - MIN_DB);
            int peakX = (int) (peakNorm * (w - 2));
            if (peakX > 2) {
                g2d.setColor(peakLevel > -3 ? DarkTheme.ACCENT_ERROR : DarkTheme.TEXT_PRIMARY);
                g2d.fillRect(peakX, 1, 2, h - 2);
            }

            // Border
            g2d.setColor(DarkTheme.BG_DARK);
            g2d.drawRoundRect(0, 0, w - 1, h - 1, 4, 4);

            g2d.dispose();
        }
    }
}
