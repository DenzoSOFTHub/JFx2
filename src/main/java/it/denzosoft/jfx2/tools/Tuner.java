package it.denzosoft.jfx2.tools;

/**
 * Chromatic tuner using autocorrelation pitch detection.
 *
 * <p>Detects the fundamental frequency of the input signal and
 * converts it to musical note with cents deviation.</p>
 *
 * <p>Features:
 * - Autocorrelation-based pitch detection (robust for guitar)
 * - Configurable reference pitch (A4 = 440Hz default)
 * - Note name with octave (e.g., "E2", "A4")
 * - Cents deviation (-50 to +50)
 * - Noise gate to ignore silence</p>
 */
public class Tuner {

    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};

    // Detection parameters
    private static final float MIN_FREQUENCY = 60.0f;   // ~B1 (lowest guitar note)
    private static final float MAX_FREQUENCY = 1400.0f; // ~F6 (high harmonics)
    private static final float NOISE_THRESHOLD = 0.01f; // RMS threshold for detection

    private int sampleRate;
    private float referenceA4;  // Reference pitch for A4 (default 440Hz)

    // Autocorrelation buffer
    private float[] buffer;
    private float[] autocorr;
    private int bufferSize;
    private int writePos;
    private boolean bufferFilled;

    // Detection results
    private float detectedFrequency;
    private String detectedNote;
    private int detectedOctave;
    private float detectedCents;
    private boolean signalPresent;

    // Smoothing
    private float smoothedFrequency;
    private static final float SMOOTHING = 0.3f;

    public Tuner() {
        this.referenceA4 = 440.0f;
    }

    /**
     * Prepare the tuner for processing.
     *
     * @param sampleRate Sample rate in Hz
     */
    public void prepare(int sampleRate) {
        this.sampleRate = sampleRate;

        // Buffer size for lowest frequency detection
        // Need at least 2 periods of the lowest frequency
        int minPeriod = (int) (sampleRate / MIN_FREQUENCY);
        this.bufferSize = minPeriod * 4;  // 4 periods for good autocorrelation
        this.bufferSize = Math.min(bufferSize, 8192);  // Cap at 8192

        this.buffer = new float[bufferSize];
        this.autocorr = new float[bufferSize];
        this.writePos = 0;
        this.bufferFilled = false;

        this.detectedFrequency = 0;
        this.detectedNote = "-";
        this.detectedOctave = 0;
        this.detectedCents = 0;
        this.signalPresent = false;
        this.smoothedFrequency = 0;
    }

    /**
     * Process audio samples for pitch detection.
     *
     * @param input      Input audio buffer
     * @param frameCount Number of frames
     */
    public void process(float[] input, int frameCount) {
        // Add samples to circular buffer
        for (int i = 0; i < frameCount; i++) {
            buffer[writePos] = input[i];
            writePos = (writePos + 1) % bufferSize;
            if (writePos == 0) {
                bufferFilled = true;
            }
        }

        // Only analyze when we have enough data
        if (!bufferFilled) {
            return;
        }

        // Check signal level
        float rms = calculateRMS();
        signalPresent = rms > NOISE_THRESHOLD;

        if (!signalPresent) {
            detectedFrequency = 0;
            detectedNote = "-";
            detectedCents = 0;
            return;
        }

        // Perform pitch detection
        float frequency = detectPitch();

        if (frequency > 0) {
            // Smooth the frequency
            if (smoothedFrequency > 0) {
                smoothedFrequency = smoothedFrequency * (1 - SMOOTHING) + frequency * SMOOTHING;
            } else {
                smoothedFrequency = frequency;
            }

            detectedFrequency = smoothedFrequency;

            // Convert to note
            frequencyToNote(detectedFrequency);
        }
    }

    /**
     * Detect pitch using NSDF (Normalized Square Difference Function).
     * This is more robust than simple autocorrelation for finding the fundamental.
     */
    private float detectPitch() {
        // Copy buffer to linear array
        float[] x = new float[bufferSize];
        for (int i = 0; i < bufferSize; i++) {
            x[i] = buffer[(writePos + i) % bufferSize];
        }

        int minLag = (int) (sampleRate / MAX_FREQUENCY);
        int maxLag = (int) (sampleRate / MIN_FREQUENCY);
        maxLag = Math.min(maxLag, bufferSize / 2);

        // Calculate NSDF
        float[] nsdf = new float[maxLag + 1];
        for (int tau = minLag; tau <= maxLag; tau++) {
            float acf = 0;  // Autocorrelation
            float m = 0;    // Normalization term

            int windowSize = bufferSize - tau;
            for (int i = 0; i < windowSize; i++) {
                acf += x[i] * x[i + tau];
                m += x[i] * x[i] + x[i + tau] * x[i + tau];
            }

            if (m > 0.0001f) {
                nsdf[tau] = 2.0f * acf / m;  // NSDF formula
            } else {
                nsdf[tau] = 0;
            }
        }

        // Find peaks in NSDF - we want the FIRST peak above threshold
        // This gives us the fundamental, not a sub-harmonic
        float threshold = 0.7f;  // Require 70% correlation
        int bestLag = 0;
        float bestNsdf = 0;

        boolean foundNegative = false;
        for (int tau = minLag; tau <= maxLag; tau++) {
            // Wait for NSDF to go negative first (skip the trivial peak at low lag)
            if (nsdf[tau] < 0) {
                foundNegative = true;
            }

            // After going negative, find the first peak above threshold
            if (foundNegative && nsdf[tau] > threshold) {
                // Check if this is a local maximum
                if (tau > minLag && tau < maxLag &&
                    nsdf[tau] > nsdf[tau - 1] && nsdf[tau] >= nsdf[tau + 1]) {

                    bestLag = tau;
                    bestNsdf = nsdf[tau];
                    break;  // Use first valid peak (fundamental frequency)
                }
            }
        }

        // If no peak above threshold, find the maximum peak
        if (bestLag == 0) {
            for (int tau = minLag; tau <= maxLag; tau++) {
                if (nsdf[tau] > bestNsdf && tau > minLag && tau < maxLag &&
                    nsdf[tau] > nsdf[tau - 1] && nsdf[tau] >= nsdf[tau + 1]) {
                    bestNsdf = nsdf[tau];
                    bestLag = tau;
                }
            }
        }

        // Require minimum correlation
        if (bestNsdf < 0.5f || bestLag == 0) {
            return 0;
        }

        // Parabolic interpolation for sub-sample accuracy
        float frequency = (float) sampleRate / bestLag;

        if (bestLag > minLag && bestLag < maxLag) {
            float prev = nsdf[bestLag - 1];
            float curr = nsdf[bestLag];
            float next = nsdf[bestLag + 1];

            float delta = 0.5f * (prev - next) / (prev - 2 * curr + next);
            if (Math.abs(delta) < 1.0f) {  // Sanity check
                frequency = (float) sampleRate / (bestLag + delta);
            }
        }

        return frequency;
    }

    /**
     * Convert frequency to note name, octave, and cents deviation.
     */
    private void frequencyToNote(float frequency) {
        if (frequency <= 0) {
            detectedNote = "-";
            detectedOctave = 0;
            detectedCents = 0;
            return;
        }

        // Calculate semitones from A4
        float semitones = (float) (12.0 * Math.log(frequency / referenceA4) / Math.log(2));

        // Round to nearest semitone
        int nearestSemitone = Math.round(semitones);

        // Calculate cents deviation
        detectedCents = (semitones - nearestSemitone) * 100.0f;

        // Convert to note and octave (A4 = semitone 0)
        // C4 = semitone -9, so note index = (semitone + 9) % 12
        int noteIndex = ((nearestSemitone % 12) + 12 + 9) % 12;
        detectedNote = NOTE_NAMES[noteIndex];

        // Calculate octave (A4 is in octave 4)
        detectedOctave = 4 + (nearestSemitone + 9) / 12;
        if (nearestSemitone < -9) {
            detectedOctave = 4 + (nearestSemitone - 2) / 12;
        }
    }

    /**
     * Calculate RMS of the buffer.
     */
    private float calculateRMS() {
        float sum = 0;
        for (int i = 0; i < bufferSize; i++) {
            sum += buffer[i] * buffer[i];
        }
        return (float) Math.sqrt(sum / bufferSize);
    }

    // Getters

    /**
     * Get the detected frequency in Hz.
     */
    public float getFrequency() {
        return detectedFrequency;
    }

    /**
     * Get the detected note name (e.g., "A", "C#").
     */
    public String getNoteName() {
        return detectedNote;
    }

    /**
     * Get the detected octave number.
     */
    public int getOctave() {
        return detectedOctave;
    }

    /**
     * Get the full note string (e.g., "A4", "E2").
     */
    public String getNoteString() {
        if (detectedNote.equals("-")) {
            return "-";
        }
        return detectedNote + detectedOctave;
    }

    /**
     * Get cents deviation from the nearest note (-50 to +50).
     */
    public float getCents() {
        return detectedCents;
    }

    /**
     * Check if a signal is present (above noise threshold).
     */
    public boolean isSignalPresent() {
        return signalPresent;
    }

    /**
     * Check if the tuning is accurate (within ±5 cents).
     */
    public boolean isInTune() {
        return signalPresent && Math.abs(detectedCents) < 5.0f;
    }

    /**
     * Get the reference pitch for A4.
     */
    public float getReferenceA4() {
        return referenceA4;
    }

    /**
     * Set the reference pitch for A4.
     *
     * @param frequency Reference frequency (typically 440Hz, range 430-450)
     */
    public void setReferenceA4(float frequency) {
        this.referenceA4 = Math.max(430.0f, Math.min(frequency, 450.0f));
    }

    /**
     * Reset the tuner state.
     */
    public void reset() {
        if (buffer != null) {
            java.util.Arrays.fill(buffer, 0);
        }
        writePos = 0;
        bufferFilled = false;
        detectedFrequency = 0;
        smoothedFrequency = 0;
        detectedNote = "-";
        detectedCents = 0;
        signalPresent = false;
    }

    /**
     * Get a formatted display string for the tuner.
     */
    public String getDisplayString() {
        if (!signalPresent) {
            return "---  ---";
        }
        String centsStr = String.format("%+.0f", detectedCents);
        return String.format("%3s  %s cents", getNoteString(), centsStr);
    }
}
