package it.denzosoft.jfx2.tools;

/**
 * Metronome with tap tempo and time signature support.
 *
 * <p>Features:
 * - BPM range 40-240
 * - Multiple time signatures (4/4, 3/4, 6/8, etc.)
 * - Tap tempo detection
 * - Audio click generation (accented downbeat)
 * - Visual beat indicator</p>
 */
public class Metronome {

    /**
     * Common time signatures.
     */
    public enum TimeSignature {
        FOUR_FOUR(4, 4, "4/4"),
        THREE_FOUR(3, 4, "3/4"),
        TWO_FOUR(2, 4, "2/4"),
        SIX_EIGHT(6, 8, "6/8"),
        FIVE_FOUR(5, 4, "5/4"),
        SEVEN_EIGHT(7, 8, "7/8");

        public final int beats;
        public final int noteValue;
        public final String display;

        TimeSignature(int beats, int noteValue, String display) {
            this.beats = beats;
            this.noteValue = noteValue;
            this.display = display;
        }
    }

    // Tempo settings
    private float bpm;
    private TimeSignature timeSignature;
    private boolean running;

    // Audio generation
    private int sampleRate;
    private double phase;
    private long sampleCounter;
    private int samplesPerBeat;

    // Beat tracking
    private int currentBeat;  // 1-based beat number
    private boolean beatTriggered;

    // Click sound parameters
    private static final float CLICK_FREQUENCY_HIGH = 1500.0f;  // Accented beat
    private static final float CLICK_FREQUENCY_LOW = 1000.0f;   // Normal beat
    private static final float CLICK_DURATION_MS = 10.0f;       // Click length
    private static final float CLICK_VOLUME = 0.5f;

    private int clickSamplesRemaining;
    private float clickFrequency;

    // Tap tempo
    private static final int TAP_HISTORY_SIZE = 4;
    private static final long TAP_TIMEOUT_MS = 2000;
    private long[] tapTimes;
    private int tapCount;
    private long lastTapTime;

    // Volume
    private float volume;

    public Metronome() {
        this.bpm = 120.0f;
        this.timeSignature = TimeSignature.FOUR_FOUR;
        this.running = false;
        this.volume = 0.7f;
        this.tapTimes = new long[TAP_HISTORY_SIZE];
        this.tapCount = 0;
    }

    /**
     * Prepare the metronome for audio generation.
     *
     * @param sampleRate Sample rate in Hz
     */
    public void prepare(int sampleRate) {
        this.sampleRate = sampleRate;
        updateSamplesPerBeat();
        reset();
    }

    /**
     * Start the metronome.
     */
    public void start() {
        if (!running) {
            running = true;
            sampleCounter = 0;
            currentBeat = 1;
            phase = 0;
            clickSamplesRemaining = 0;
        }
    }

    /**
     * Stop the metronome.
     */
    public void stop() {
        running = false;
        clickSamplesRemaining = 0;
    }

    /**
     * Check if the metronome is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Generate metronome audio clicks.
     *
     * @param output     Output buffer to mix clicks into
     * @param frameCount Number of frames
     */
    public void process(float[] output, int frameCount) {
        if (!running) {
            return;
        }

        beatTriggered = false;

        for (int i = 0; i < frameCount; i++) {
            // Check for beat trigger
            if (sampleCounter % samplesPerBeat == 0) {
                triggerBeat();
            }

            // Generate click sound
            if (clickSamplesRemaining > 0) {
                float click = generateClickSample();
                output[i] += click * volume;
                clickSamplesRemaining--;
            }

            sampleCounter++;
        }
    }

    /**
     * Trigger a beat (called at beat boundaries).
     */
    private void triggerBeat() {
        beatTriggered = true;

        // Set click frequency based on beat position
        if (currentBeat == 1) {
            clickFrequency = CLICK_FREQUENCY_HIGH;  // Accented downbeat
        } else {
            clickFrequency = CLICK_FREQUENCY_LOW;   // Normal beat
        }

        // Start click sound
        clickSamplesRemaining = (int) (CLICK_DURATION_MS * sampleRate / 1000.0f);
        phase = 0;

        // Advance beat counter
        currentBeat++;
        if (currentBeat > timeSignature.beats) {
            currentBeat = 1;
        }
    }

    /**
     * Generate a single click sample.
     */
    private float generateClickSample() {
        // Envelope (quick decay)
        float envelope = (float) clickSamplesRemaining / (CLICK_DURATION_MS * sampleRate / 1000.0f);
        envelope = envelope * envelope;  // Exponential decay

        // Sine wave click
        float sample = (float) Math.sin(phase) * envelope * CLICK_VOLUME;

        // Advance phase
        phase += 2.0 * Math.PI * clickFrequency / sampleRate;

        return sample;
    }

    /**
     * Process a tap for tap tempo.
     * Call this when the user taps the tempo button.
     */
    public void tap() {
        long now = System.currentTimeMillis();

        // Reset if too long since last tap
        if (now - lastTapTime > TAP_TIMEOUT_MS) {
            tapCount = 0;
        }

        // Record tap time
        if (tapCount < TAP_HISTORY_SIZE) {
            tapTimes[tapCount] = now;
            tapCount++;
        } else {
            // Shift history
            for (int i = 0; i < TAP_HISTORY_SIZE - 1; i++) {
                tapTimes[i] = tapTimes[i + 1];
            }
            tapTimes[TAP_HISTORY_SIZE - 1] = now;
        }

        lastTapTime = now;

        // Calculate BPM from tap history
        if (tapCount >= 2) {
            long totalInterval = tapTimes[tapCount - 1] - tapTimes[0];
            int intervals = tapCount - 1;
            float avgIntervalMs = (float) totalInterval / intervals;

            if (avgIntervalMs > 0) {
                float newBpm = 60000.0f / avgIntervalMs;
                setBpm(newBpm);
            }
        }
    }

    /**
     * Reset tap tempo history.
     */
    public void resetTap() {
        tapCount = 0;
        lastTapTime = 0;
    }

    // Getters and setters

    /**
     * Get the current BPM.
     */
    public float getBpm() {
        return bpm;
    }

    /**
     * Set the BPM (clamped to 40-240).
     */
    public void setBpm(float bpm) {
        this.bpm = Math.max(40.0f, Math.min(bpm, 240.0f));
        updateSamplesPerBeat();
    }

    /**
     * Get the current time signature.
     */
    public TimeSignature getTimeSignature() {
        return timeSignature;
    }

    /**
     * Set the time signature.
     */
    public void setTimeSignature(TimeSignature timeSignature) {
        this.timeSignature = timeSignature;
        if (currentBeat > timeSignature.beats) {
            currentBeat = 1;
        }
    }

    /**
     * Get the current beat (1-based).
     */
    public int getCurrentBeat() {
        return currentBeat;
    }

    /**
     * Check if a beat was triggered in the last process() call.
     */
    public boolean wasBeatTriggered() {
        return beatTriggered;
    }

    /**
     * Check if the current beat is the downbeat (beat 1).
     */
    public boolean isDownbeat() {
        return currentBeat == 1;
    }

    /**
     * Get the volume (0.0 to 1.0).
     */
    public float getVolume() {
        return volume;
    }

    /**
     * Set the volume (0.0 to 1.0).
     */
    public void setVolume(float volume) {
        this.volume = Math.max(0.0f, Math.min(volume, 1.0f));
    }

    /**
     * Get milliseconds per beat.
     */
    public float getMsPerBeat() {
        return 60000.0f / bpm;
    }

    /**
     * Get samples per beat.
     */
    public int getSamplesPerBeat() {
        return samplesPerBeat;
    }

    /**
     * Update samples per beat based on current BPM.
     */
    private void updateSamplesPerBeat() {
        if (sampleRate > 0) {
            samplesPerBeat = (int) (sampleRate * 60.0f / bpm);
        }
    }

    /**
     * Reset the metronome state.
     */
    public void reset() {
        sampleCounter = 0;
        currentBeat = 1;
        phase = 0;
        clickSamplesRemaining = 0;
        beatTriggered = false;
    }

    /**
     * Get a display string showing current state.
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%.1f BPM  %s  ", bpm, timeSignature.display));

        // Beat indicator
        for (int i = 1; i <= timeSignature.beats; i++) {
            if (i == currentBeat) {
                sb.append("[").append(i).append("]");
            } else {
                sb.append(" ").append(i).append(" ");
            }
        }

        return sb.toString();
    }
}
