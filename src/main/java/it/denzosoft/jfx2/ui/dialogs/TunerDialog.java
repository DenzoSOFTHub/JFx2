package it.denzosoft.jfx2.ui.dialogs;

import it.denzosoft.jfx2.audio.AudioEngine;
import it.denzosoft.jfx2.audio.AudioSettings;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Tuner dialog with tachometer-style display.
 * Shows detected pitch with a needle indicating cents deviation.
 */
public class TunerDialog extends JDialog {

    // Note names
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int A4_MIDI = 69;

    // Reference frequency from settings
    private double a4Frequency;

    // Pitch detection parameters
    private static final int BUFFER_SIZE = 4096;
    private static final float MIN_FREQ = 60f;   // ~B1
    private static final float MAX_FREQ = 1200f; // ~D6

    // Current tuner state
    private String currentNote = "--";
    private int currentOctave = 0;
    private float currentFrequency = 0;
    private float centsDeviation = 0;
    private boolean signalDetected = false;

    // Audio capture
    private final float[] analysisBuffer = new float[BUFFER_SIZE];
    private int bufferIndex = 0;
    private int sampleRate = 44100;

    // Independent capture (when SignalGraph not active)
    private TargetDataLine captureLine;
    private Thread captureThread;
    private volatile boolean capturing = false;
    private int captureChannels = 1;  // 1=mono, 2=stereo

    // External audio source (from SignalGraph)
    private boolean usingExternalSource = false;

    // UI components
    private final TachometerPanel tachometer;
    private final JComboBox<String> deviceCombo;
    private final JLabel frequencyLabel;
    private final JLabel noteLabel;
    private final JButton startStopButton;

    // Reference to check if SignalGraph is active
    private final AudioEngine audioEngine;

    public TunerDialog(Frame owner, AudioEngine audioEngine) {
        super(owner, "Tuner", false);
        this.audioEngine = audioEngine;
        this.a4Frequency = AudioSettings.getInstance().getTunerReferenceFrequency();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(DarkTheme.BG_DARK);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Tachometer display
        tachometer = new TachometerPanel();
        mainPanel.add(tachometer, BorderLayout.CENTER);

        // Info panel below tachometer
        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        infoPanel.setOpaque(false);

        noteLabel = new JLabel("--", SwingConstants.CENTER);
        noteLabel.setFont(DarkTheme.FONT_BOLD.deriveFont(48f));
        noteLabel.setForeground(DarkTheme.TEXT_PRIMARY);
        infoPanel.add(noteLabel);

        frequencyLabel = new JLabel("--- Hz", SwingConstants.CENTER);
        frequencyLabel.setFont(DarkTheme.FONT_REGULAR.deriveFont(18f));
        frequencyLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        infoPanel.add(frequencyLabel);

        mainPanel.add(infoPanel, BorderLayout.SOUTH);

        // Control panel at top
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setOpaque(false);

        JLabel deviceLabel = new JLabel("Input Device:");
        deviceLabel.setForeground(DarkTheme.TEXT_SECONDARY);
        controlPanel.add(deviceLabel);

        deviceCombo = new JComboBox<>();
        deviceCombo.setPreferredSize(new Dimension(250, 25));
        populateDevices();
        controlPanel.add(deviceCombo);

        startStopButton = new JButton("Start");
        DarkTheme.stylePrimaryButton(startStopButton);
        startStopButton.addActionListener(e -> toggleCapture());
        controlPanel.add(startStopButton);

        mainPanel.add(controlPanel, BorderLayout.NORTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(owner);

        // Stop capture when dialog closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopCapture();
            }
        });

        // Start refresh timer
        Timer refreshTimer = new Timer(50, e -> updateDisplay());
        refreshTimer.start();

        // Check if SignalGraph is running
        checkExternalSource();
    }

    /**
     * Populate the device combo box with available audio input devices.
     */
    private void populateDevices() {
        deviceCombo.removeAllItems();

        // Add "Use SignalGraph" option if available
        deviceCombo.addItem("(Use Signal Graph - if active)");

        // Get available mixers
        Mixer.Info[] mixers = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixers) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                for (Line.Info lineInfo : targetLines) {
                    if (lineInfo instanceof DataLine.Info) {
                        deviceCombo.addItem(mixerInfo.getName());
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Check if we should use external source from SignalGraph.
     */
    private void checkExternalSource() {
        if (audioEngine != null && audioEngine.isRunning()) {
            usingExternalSource = true;
            startStopButton.setText("Using Signal Graph");
            startStopButton.setEnabled(false);
            deviceCombo.setSelectedIndex(0);
            deviceCombo.setEnabled(false);
        }
    }

    /**
     * Toggle audio capture on/off.
     */
    private void toggleCapture() {
        if (capturing) {
            stopCapture();
            startStopButton.setText("Start");
        } else {
            startCapture();
            startStopButton.setText("Stop");
        }
    }

    /**
     * Start capturing audio from selected device.
     */
    private void startCapture() {
        if (capturing) return;

        int selectedIndex = deviceCombo.getSelectedIndex();
        if (selectedIndex == 0) {
            // Use SignalGraph if active
            if (audioEngine != null && audioEngine.isRunning()) {
                usingExternalSource = true;
                capturing = true;
                return;
            }
            // SignalGraph not active, fallback to first real device
            if (deviceCombo.getItemCount() > 1) {
                deviceCombo.setSelectedIndex(1);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Signal Graph is not running and no audio input device available.\n" +
                        "Start the audio engine or select a different input device.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Find the selected mixer
        String selectedDevice = (String) deviceCombo.getSelectedItem();
        if (selectedDevice == null || selectedDevice.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No audio input device selected",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // Find mixer
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            Mixer selectedMixer = null;
            for (Mixer.Info info : mixers) {
                if (info.getName().equals(selectedDevice)) {
                    selectedMixer = AudioSystem.getMixer(info);
                    break;
                }
            }

            if (selectedMixer == null) {
                selectedMixer = AudioSystem.getMixer(mixers[0]);
            }

            // Get input channels setting from AudioSettings
            AudioSettings audioSettings = AudioSettings.getInstance();
            int preferredChannels = audioSettings.getInputChannels();

            AudioFormat stereoFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100, 16, 2, 4, 44100, false);  // 2 channels, 4 bytes/frame
            AudioFormat monoFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    44100, 16, 1, 2, 44100, false);  // 1 channel, 2 bytes/frame

            DataLine.Info stereoLineInfo = new DataLine.Info(TargetDataLine.class, stereoFormat);
            DataLine.Info monoLineInfo = new DataLine.Info(TargetDataLine.class, monoFormat);

            AudioFormat format;
            if (preferredChannels == 2 && selectedMixer.isLineSupported(stereoLineInfo)) {
                // Stereo preferred and supported
                captureLine = (TargetDataLine) selectedMixer.getLine(stereoLineInfo);
                format = stereoFormat;
                captureChannels = 2;
            } else if (selectedMixer.isLineSupported(monoLineInfo)) {
                // Mono preferred or stereo not supported
                captureLine = (TargetDataLine) selectedMixer.getLine(monoLineInfo);
                format = monoFormat;
                captureChannels = 1;
            } else if (selectedMixer.isLineSupported(stereoLineInfo)) {
                // Fallback to stereo if mono not supported
                captureLine = (TargetDataLine) selectedMixer.getLine(stereoLineInfo);
                format = stereoFormat;
                captureChannels = 2;
            } else {
                // Try default system line
                try {
                    if (preferredChannels == 2) {
                        captureLine = AudioSystem.getTargetDataLine(stereoFormat);
                        format = stereoFormat;
                        captureChannels = 2;
                    } else {
                        captureLine = AudioSystem.getTargetDataLine(monoFormat);
                        format = monoFormat;
                        captureChannels = 1;
                    }
                } catch (Exception e) {
                    captureLine = AudioSystem.getTargetDataLine(monoFormat);
                    format = monoFormat;
                    captureChannels = 1;
                }
            }

            captureLine.open(format);
            captureLine.start();
            sampleRate = (int) format.getSampleRate();

            capturing = true;
            usingExternalSource = false;

            // Start capture thread
            captureThread = new Thread(this::captureLoop, "TunerCapture");
            captureThread.setDaemon(true);
            captureThread.start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Error opening audio device: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Stop audio capture.
     */
    private void stopCapture() {
        capturing = false;

        if (captureThread != null) {
            captureThread.interrupt();
            captureThread = null;
        }

        if (captureLine != null) {
            captureLine.stop();
            captureLine.close();
            captureLine = null;
        }

        usingExternalSource = false;
    }

    /**
     * Audio capture loop (for independent capture).
     */
    private void captureLoop() {
        byte[] buffer = new byte[2048];

        while (capturing && captureLine != null) {
            int bytesRead = captureLine.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                int bytesPerSample = 2;  // 16-bit
                int bytesPerFrame = bytesPerSample * captureChannels;
                int frameCount = bytesRead / bytesPerFrame;

                // Extract left channel (or mono) for analysis
                float[] samples = new float[frameCount];
                for (int i = 0; i < frameCount; i++) {
                    int bytePos = i * bytesPerFrame;
                    int lo = buffer[bytePos] & 0xFF;
                    int hi = buffer[bytePos + 1];
                    short sample = (short) (lo | (hi << 8));
                    samples[i] = sample / 32768f;
                }
                feedAudio(samples, samples.length);
            }
        }
    }

    /**
     * Feed audio samples to the tuner (called from SignalGraph or capture thread).
     */
    public void feedAudio(float[] samples, int length) {
        for (int i = 0; i < length && i < samples.length; i++) {
            analysisBuffer[bufferIndex++] = samples[i];
            if (bufferIndex >= BUFFER_SIZE) {
                analyzeBuffer();
                bufferIndex = 0;
            }
        }
    }

    /**
     * Analyze the buffer and detect pitch.
     */
    private void analyzeBuffer() {
        // Check signal level
        float rms = calculateRMS(analysisBuffer);
        if (rms < 0.01f) {
            signalDetected = false;
            return;
        }

        // Detect pitch using autocorrelation
        float detectedFreq = detectPitch(analysisBuffer, sampleRate);

        if (detectedFreq > MIN_FREQ && detectedFreq < MAX_FREQ) {
            signalDetected = true;
            currentFrequency = detectedFreq;

            // Convert frequency to note
            double midiNote = 12 * Math.log(detectedFreq / a4Frequency) / Math.log(2) + A4_MIDI;
            int nearestMidi = (int) Math.round(midiNote);

            // Calculate cents deviation
            centsDeviation = (float) ((midiNote - nearestMidi) * 100);

            // Get note name and octave
            int noteIndex = ((nearestMidi % 12) + 12) % 12;
            currentOctave = (nearestMidi / 12) - 1;
            currentNote = NOTE_NAMES[noteIndex];
        } else {
            signalDetected = false;
        }
    }

    /**
     * Calculate RMS of buffer.
     */
    private float calculateRMS(float[] buffer) {
        float sum = 0;
        for (float sample : buffer) {
            sum += sample * sample;
        }
        return (float) Math.sqrt(sum / buffer.length);
    }

    /**
     * Detect pitch using autocorrelation method.
     */
    private float detectPitch(float[] buffer, int sampleRate) {
        int size = buffer.length;

        int minPeriod = sampleRate / (int) MAX_FREQ;
        int maxPeriod = sampleRate / (int) MIN_FREQ;
        maxPeriod = Math.min(maxPeriod, size / 2);
        minPeriod = Math.max(minPeriod, 2);

        // Normalize
        float maxVal = 0;
        for (float v : buffer) {
            maxVal = Math.max(maxVal, Math.abs(v));
        }
        if (maxVal < 0.001f) return 0;

        float[] normalized = new float[size];
        for (int i = 0; i < size; i++) {
            normalized[i] = buffer[i] / maxVal;
        }

        // Autocorrelation
        float bestCorrelation = 0;
        int bestPeriod = 0;

        for (int period = minPeriod; period < maxPeriod; period++) {
            float correlation = 0;
            for (int i = 0; i < size - period; i++) {
                correlation += normalized[i] * normalized[i + period];
            }
            correlation /= (size - period);

            if (correlation > bestCorrelation) {
                bestCorrelation = correlation;
                bestPeriod = period;
            }
        }

        if (bestCorrelation < 0.3f || bestPeriod == 0) {
            return 0;
        }

        // Parabolic interpolation
        if (bestPeriod > minPeriod && bestPeriod < maxPeriod - 1) {
            float y0 = autocorrelation(normalized, bestPeriod - 1);
            float y1 = autocorrelation(normalized, bestPeriod);
            float y2 = autocorrelation(normalized, bestPeriod + 1);

            float denom = 2 * (y0 - 2 * y1 + y2);
            if (Math.abs(denom) > 0.0001f) {
                float refinedPeriod = bestPeriod + (y0 - y2) / denom;
                return sampleRate / refinedPeriod;
            }
        }

        return (float) sampleRate / bestPeriod;
    }

    private float autocorrelation(float[] buffer, int lag) {
        float sum = 0;
        int count = buffer.length - lag;
        for (int i = 0; i < count; i++) {
            sum += buffer[i] * buffer[i + lag];
        }
        return sum / count;
    }

    /**
     * Update the display.
     */
    private void updateDisplay() {
        tachometer.setCents(signalDetected ? centsDeviation : 0);
        tachometer.setInTune(signalDetected && Math.abs(centsDeviation) < 5);
        tachometer.setSignalDetected(signalDetected);
        tachometer.repaint();

        if (signalDetected) {
            noteLabel.setText(currentNote + currentOctave);
            frequencyLabel.setText(String.format("%.1f Hz  (%+.0f cents)", currentFrequency, centsDeviation));

            // Color based on tuning
            if (Math.abs(centsDeviation) < 5) {
                noteLabel.setForeground(DarkTheme.ACCENT_SUCCESS);
            } else if (Math.abs(centsDeviation) < 15) {
                noteLabel.setForeground(DarkTheme.ACCENT_WARNING);
            } else {
                noteLabel.setForeground(DarkTheme.TEXT_PRIMARY);
            }
        } else {
            noteLabel.setText("--");
            noteLabel.setForeground(DarkTheme.TEXT_DISABLED);
            frequencyLabel.setText("--- Hz");
        }
    }

    /**
     * Set sample rate (when using external source).
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Check if using external audio source.
     */
    public boolean isUsingExternalSource() {
        return usingExternalSource || (audioEngine != null && audioEngine.isRunning());
    }

    // ==================== TACHOMETER PANEL ====================

    /**
     * Tachometer-style display for the tuner.
     */
    private static class TachometerPanel extends JPanel {

        private static final int SIZE = 300;
        private static final int ARC_START = 135;  // Start angle (bottom-left)
        private static final int ARC_EXTENT = 270; // Sweep angle

        private float cents = 0;
        private boolean inTune = false;
        private boolean signalDetected = false;

        // Smoothed needle position
        private float smoothedCents = 0;
        private static final float SMOOTHING = 0.3f;

        public TachometerPanel() {
            setPreferredSize(new Dimension(SIZE, SIZE - 40));
            setOpaque(false);
        }

        public void setCents(float cents) {
            this.cents = Math.max(-50, Math.min(50, cents));
            // Smooth the needle movement
            smoothedCents = smoothedCents + (this.cents - smoothedCents) * SMOOTHING;
        }

        public void setInTune(boolean inTune) {
            this.inTune = inTune;
        }

        public void setSignalDetected(boolean detected) {
            this.signalDetected = detected;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int centerX = w / 2;
            int centerY = h - 30;
            int radius = Math.min(w, h) - 60;

            // Draw arc background
            g2.setStroke(new BasicStroke(20, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(DarkTheme.BG_MEDIUM);
            g2.draw(new Arc2D.Double(centerX - radius / 2.0, centerY - radius / 2.0,
                    radius, radius, ARC_START, ARC_EXTENT, Arc2D.OPEN));

            // Draw colored zones
            drawColoredArc(g2, centerX, centerY, radius, -50, -15, DarkTheme.ACCENT_ERROR);      // Flat zone
            drawColoredArc(g2, centerX, centerY, radius, -15, -5, DarkTheme.ACCENT_WARNING);     // Slightly flat
            drawColoredArc(g2, centerX, centerY, radius, -5, 5, DarkTheme.ACCENT_SUCCESS);       // In tune
            drawColoredArc(g2, centerX, centerY, radius, 5, 15, DarkTheme.ACCENT_WARNING);       // Slightly sharp
            drawColoredArc(g2, centerX, centerY, radius, 15, 50, DarkTheme.ACCENT_ERROR);        // Sharp zone

            // Draw tick marks
            g2.setStroke(new BasicStroke(2));
            g2.setFont(DarkTheme.FONT_SMALL.deriveFont(10f));
            for (int c = -50; c <= 50; c += 10) {
                double angle = centsToAngle(c);
                int tickLength = (c == 0) ? 20 : (c % 25 == 0 ? 15 : 10);

                int x1 = centerX + (int) ((radius / 2.0 - 15) * Math.cos(angle));
                int y1 = centerY - (int) ((radius / 2.0 - 15) * Math.sin(angle));
                int x2 = centerX + (int) ((radius / 2.0 - 15 - tickLength) * Math.cos(angle));
                int y2 = centerY - (int) ((radius / 2.0 - 15 - tickLength) * Math.sin(angle));

                g2.setColor(c == 0 ? DarkTheme.ACCENT_SUCCESS : DarkTheme.TEXT_SECONDARY);
                g2.drawLine(x1, y1, x2, y2);

                // Labels at major ticks
                if (c % 25 == 0 || c == 0) {
                    String label = (c == 0) ? "0" : String.valueOf(c);
                    FontMetrics fm = g2.getFontMetrics();
                    int lx = centerX + (int) ((radius / 2.0 - 45) * Math.cos(angle)) - fm.stringWidth(label) / 2;
                    int ly = centerY - (int) ((radius / 2.0 - 45) * Math.sin(angle)) + fm.getAscent() / 2;
                    g2.setColor(DarkTheme.TEXT_SECONDARY);
                    g2.drawString(label, lx, ly);
                }
            }

            // Draw needle
            if (signalDetected) {
                double needleAngle = centsToAngle(smoothedCents);
                int needleLength = radius / 2 - 25;

                // Needle shadow
                g2.setColor(new Color(0, 0, 0, 80));
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int nx = centerX + (int) (needleLength * Math.cos(needleAngle)) + 2;
                int ny = centerY - (int) (needleLength * Math.sin(needleAngle)) + 2;
                g2.drawLine(centerX + 2, centerY + 2, nx, ny);

                // Needle
                Color needleColor = inTune ? DarkTheme.ACCENT_SUCCESS :
                        (Math.abs(smoothedCents) < 15 ? DarkTheme.ACCENT_WARNING : DarkTheme.ACCENT_ERROR);
                g2.setColor(needleColor);
                g2.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                nx = centerX + (int) (needleLength * Math.cos(needleAngle));
                ny = centerY - (int) (needleLength * Math.sin(needleAngle));
                g2.drawLine(centerX, centerY, nx, ny);

                // Center dot
                g2.setColor(DarkTheme.BG_LIGHTER);
                g2.fillOval(centerX - 8, centerY - 8, 16, 16);
                g2.setColor(needleColor);
                g2.fillOval(centerX - 5, centerY - 5, 10, 10);
            } else {
                // No signal - show grayed needle at center
                g2.setColor(DarkTheme.TEXT_DISABLED);
                g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                double needleAngle = centsToAngle(0);
                int needleLength = radius / 2 - 25;
                int nx = centerX + (int) (needleLength * Math.cos(needleAngle));
                int ny = centerY - (int) (needleLength * Math.sin(needleAngle));
                g2.drawLine(centerX, centerY, nx, ny);

                // Center dot
                g2.setColor(DarkTheme.BG_LIGHTER);
                g2.fillOval(centerX - 6, centerY - 6, 12, 12);
            }

            // Flat/Sharp labels
            g2.setFont(DarkTheme.FONT_BOLD.deriveFont(12f));
            g2.setColor(DarkTheme.TEXT_SECONDARY);
            g2.drawString("FLAT", 20, h - 10);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("SHARP", w - 20 - fm.stringWidth("SHARP"), h - 10);

            g2.dispose();
        }

        /**
         * Convert cents to angle (radians).
         * -50 cents = left (225°), 0 = top (90°), +50 cents = right (-45°)
         */
        private double centsToAngle(float cents) {
            // Map -50..+50 to 135°..225° (in radians, counter-clockwise from right)
            // Actually we want: -50 -> 135°, 0 -> 90°, +50 -> 45° (clockwise visual)
            double normalized = (cents + 50) / 100.0; // 0 to 1
            double angleDegrees = 135 - normalized * 90; // 135° to 45°
            return Math.toRadians(angleDegrees);
        }

        /**
         * Draw a colored arc segment for a cents range.
         */
        private void drawColoredArc(Graphics2D g2, int cx, int cy, int radius,
                                    int centsStart, int centsEnd, Color color) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            g2.setStroke(new BasicStroke(18, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

            // Convert cents to arc angles
            double startNorm = (centsStart + 50) / 100.0;
            double endNorm = (centsEnd + 50) / 100.0;

            double arcStart = ARC_START + startNorm * ARC_EXTENT;
            double arcExtent = (endNorm - startNorm) * ARC_EXTENT;

            g2.draw(new Arc2D.Double(cx - radius / 2.0, cy - radius / 2.0,
                    radius, radius, arcStart, arcExtent, Arc2D.OPEN));
        }
    }
}
