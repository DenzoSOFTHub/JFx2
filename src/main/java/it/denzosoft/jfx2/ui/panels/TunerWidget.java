package it.denzosoft.jfx2.ui.panels;

import it.denzosoft.jfx2.audio.AudioSettings;
import it.denzosoft.jfx2.ui.theme.DarkTheme;

import javax.swing.*;
import java.awt.*;

/**
 * Compact tuner widget for status bar.
 * Shows detected note and tuning deviation with arrows.
 */
public class TunerWidget extends JPanel {

    // Note names
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int A4_MIDI = 69;

    // Pitch detection
    private static final int BUFFER_SIZE = 2048;
    private static final float MIN_FREQ = 60f;   // ~B1
    private static final float MAX_FREQ = 1200f; // ~D6

    // Current state
    private String currentNote = "--";
    private int currentOctave = 0;
    private float centsDeviation = 0;  // -50 to +50 cents
    private boolean signalDetected = false;

    // Audio buffer for analysis
    private final float[] analysisBuffer = new float[BUFFER_SIZE];
    private int bufferIndex = 0;
    private int sampleRate = 44100;

    public TunerWidget() {
        setMinimumSize(new Dimension(50, 14));
        setPreferredSize(new Dimension(80, 20));
        setOpaque(false);
        setToolTipText("Guitar Tuner - Shows detected pitch and tuning");
    }

    /**
     * Set the sample rate for pitch detection.
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Feed audio samples to the tuner for analysis.
     * Call this from the audio thread with input samples.
     */
    public void feedAudio(float[] samples, int length) {
        // Copy samples to analysis buffer
        for (int i = 0; i < length && i < samples.length; i++) {
            analysisBuffer[bufferIndex++] = samples[i];
            if (bufferIndex >= BUFFER_SIZE) {
                // Buffer full, analyze
                analyzeBuffer();
                bufferIndex = 0;
            }
        }
    }

    /**
     * Analyze the buffer and detect pitch.
     */
    private void analyzeBuffer() {
        // Check if there's enough signal
        float rms = calculateRMS(analysisBuffer);
        if (rms < 0.01f) {
            // Too quiet
            signalDetected = false;
            SwingUtilities.invokeLater(this::repaint);
            return;
        }

        // Detect pitch using autocorrelation
        float detectedFreq = detectPitch(analysisBuffer, sampleRate);

        if (detectedFreq > MIN_FREQ && detectedFreq < MAX_FREQ) {
            signalDetected = true;

            // Convert frequency to note (using reference frequency from settings)
            double a4Freq = AudioSettings.getInstance().getTunerReferenceFrequency();
            double midiNote = 12 * Math.log(detectedFreq / a4Freq) / Math.log(2) + A4_MIDI;
            int nearestMidi = (int) Math.round(midiNote);

            // Calculate cents deviation
            double exactMidi = midiNote;
            centsDeviation = (float) ((exactMidi - nearestMidi) * 100);

            // Get note name and octave
            int noteIndex = nearestMidi % 12;
            currentOctave = (nearestMidi / 12) - 1;
            currentNote = NOTE_NAMES[noteIndex];

            SwingUtilities.invokeLater(this::repaint);
        } else {
            signalDetected = false;
            SwingUtilities.invokeLater(this::repaint);
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

        // Find the period using autocorrelation
        int minPeriod = sampleRate / (int) MAX_FREQ;
        int maxPeriod = sampleRate / (int) MIN_FREQ;

        maxPeriod = Math.min(maxPeriod, size / 2);
        minPeriod = Math.max(minPeriod, 2);

        float bestCorrelation = 0;
        int bestPeriod = 0;

        // Normalize the buffer
        float[] normalized = new float[size];
        float maxVal = 0;
        for (float v : buffer) {
            maxVal = Math.max(maxVal, Math.abs(v));
        }
        if (maxVal > 0) {
            for (int i = 0; i < size; i++) {
                normalized[i] = buffer[i] / maxVal;
            }
        }

        // Calculate autocorrelation for each lag
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

        // Need reasonable correlation to be confident
        if (bestCorrelation < 0.3f || bestPeriod == 0) {
            return 0;
        }

        // Parabolic interpolation for better accuracy
        if (bestPeriod > minPeriod && bestPeriod < maxPeriod - 1) {
            float y0 = autocorrelation(normalized, bestPeriod - 1);
            float y1 = autocorrelation(normalized, bestPeriod);
            float y2 = autocorrelation(normalized, bestPeriod + 1);

            float refinedPeriod = bestPeriod + (y0 - y2) / (2 * (y0 - 2 * y1 + y2));
            return sampleRate / refinedPeriod;
        }

        return (float) sampleRate / bestPeriod;
    }

    /**
     * Calculate autocorrelation at a specific lag.
     */
    private float autocorrelation(float[] buffer, int lag) {
        float sum = 0;
        int count = buffer.length - lag;
        for (int i = 0; i < count; i++) {
            sum += buffer[i] * buffer[i + lag];
        }
        return sum / count;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background
        g2d.setColor(DarkTheme.BG_MEDIUM);
        g2d.fillRoundRect(0, 0, w, h, 4, 4);

        // Border
        g2d.setColor(DarkTheme.BG_LIGHTER);
        g2d.drawRoundRect(0, 0, w - 1, h - 1, 4, 4);

        if (!signalDetected) {
            // No signal - show placeholder
            g2d.setFont(DarkTheme.FONT_SMALL.deriveFont(9f));
            g2d.setColor(DarkTheme.TEXT_DISABLED);
            String text = "--";
            FontMetrics fm = g2d.getFontMetrics();
            int textX = (w - fm.stringWidth(text)) / 2;
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(text, textX, textY);
        } else {
            // Draw tuning indicators
            int arrowWidth = 10;

            // Left arrow (flat - need to tune up)
            drawArrow(g2d, arrowWidth / 2 + 2, h / 2, true, centsDeviation < -5);

            // Right arrow (sharp - need to tune down)
            drawArrow(g2d, w - arrowWidth / 2 - 2, h / 2, false, centsDeviation > 5);

            // Note name in center
            String noteText = currentNote + currentOctave;
            g2d.setFont(DarkTheme.FONT_BOLD.deriveFont(9f));

            // Color based on tuning accuracy
            if (Math.abs(centsDeviation) < 5) {
                g2d.setColor(DarkTheme.ACCENT_SUCCESS);  // In tune
            } else if (Math.abs(centsDeviation) < 15) {
                g2d.setColor(DarkTheme.ACCENT_WARNING);  // Close
            } else {
                g2d.setColor(DarkTheme.TEXT_PRIMARY);    // Off
            }

            FontMetrics fm = g2d.getFontMetrics();
            int textX = (w - fm.stringWidth(noteText)) / 2;
            int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2d.drawString(noteText, textX, textY);
        }

        g2d.dispose();
    }

    /**
     * Draw arrow indicator.
     */
    private void drawArrow(Graphics2D g2d, int x, int y, boolean pointLeft, boolean active) {
        int size = 4;

        if (active) {
            // Active arrow - colored and filled
            g2d.setColor(pointLeft ? new Color(0xFF6B6B) : new Color(0x4ECDC4));
        } else {
            // Inactive arrow - dim
            g2d.setColor(DarkTheme.BG_LIGHTER);
        }

        int[] xPoints, yPoints;
        if (pointLeft) {
            xPoints = new int[]{x + size, x - size / 2, x + size};
            yPoints = new int[]{y - size / 2, y, y + size / 2};
        } else {
            xPoints = new int[]{x - size, x + size / 2, x - size};
            yPoints = new int[]{y - size / 2, y, y + size / 2};
        }

        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    /**
     * Reset the tuner state.
     */
    public void reset() {
        signalDetected = false;
        currentNote = "--";
        centsDeviation = 0;
        bufferIndex = 0;
        repaint();
    }
}
