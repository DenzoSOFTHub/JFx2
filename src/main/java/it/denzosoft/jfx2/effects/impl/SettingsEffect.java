package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

/**
 * Global settings block for the SignalPath.
 *
 * <p>Provides global parameters that can be used by other effects in the signal chain.
 * Currently supports:
 * <ul>
 *   <li>Tap Tempo - sets the quarter note duration in BPM</li>
 * </ul>
 * </p>
 *
 * <p>Future additions may include:
 * <ul>
 *   <li>Global tuning reference (A4 frequency)</li>
 *   <li>Time signature</li>
 *   <li>Key/Scale for harmonizers</li>
 * </ul>
 * </p>
 */
public class SettingsEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "settings",
            "Settings",
            "Global settings for the SignalPath (tap tempo, etc.)",
            EffectCategory.UTILITY
    );

    // Tap tempo state
    private static final int TAP_TEMPO_MAX_TAPS = 8;
    private static final long TAP_TEMPO_TIMEOUT_MS = 2000;
    private final java.util.LinkedList<Long> tapTimes = new java.util.LinkedList<>();

    // Parameters
    private final Parameter bpmParam;
    private final Parameter tapParam;

    // Singleton instance for global access
    private static SettingsEffect globalInstance;

    public SettingsEffect() {
        super(METADATA);

        // BPM parameter (read-only display, set via tap)
        bpmParam = addFloatParameter("bpm", "BPM",
                "Tempo in beats per minute. Set by tapping the TAP button.",
                40.0f, 240.0f, 120.0f, "");

        // Tap button (boolean that triggers on press)
        tapParam = addBooleanParameter("tap", "TAP",
                "Tap repeatedly to set tempo. Average of last " + TAP_TEMPO_MAX_TAPS + " taps.",
                false);

        // Register as global instance
        globalInstance = this;
    }

    /**
     * Get the global settings instance.
     * Returns null if no settings block exists in the signal path.
     */
    public static SettingsEffect getGlobalInstance() {
        return globalInstance;
    }

    /**
     * Handle a tap event for tap tempo.
     * Call this when the TAP button is pressed.
     */
    public void tap() {
        long now = System.currentTimeMillis();

        // Check if we should reset (timeout since last tap)
        if (!tapTimes.isEmpty()) {
            long lastTap = tapTimes.getLast();
            if (now - lastTap > TAP_TEMPO_TIMEOUT_MS) {
                tapTimes.clear();
            }
        }

        // Add the new tap time
        tapTimes.add(now);

        // Keep only the last N taps
        while (tapTimes.size() > TAP_TEMPO_MAX_TAPS) {
            tapTimes.removeFirst();
        }

        // Need at least 2 taps to calculate BPM
        if (tapTimes.size() < 2) {
            return;
        }

        // Calculate average interval between taps
        long totalInterval = 0;
        int intervalCount = 0;

        Long previousTime = null;
        for (Long tapTime : tapTimes) {
            if (previousTime != null) {
                totalInterval += (tapTime - previousTime);
                intervalCount++;
            }
            previousTime = tapTime;
        }

        if (intervalCount == 0) return;

        double avgIntervalMs = (double) totalInterval / intervalCount;

        // Convert to BPM: 60000 ms / interval = beats per minute
        double bpm = 60000.0 / avgIntervalMs;

        // Clamp to valid range
        bpm = Math.max(40.0, Math.min(240.0, bpm));

        // Update BPM parameter
        bpmParam.setValue((float) bpm);
    }

    /**
     * Get the current tempo in BPM.
     */
    public float getBpm() {
        return bpmParam.getTargetValue();
    }

    /**
     * Set the tempo in BPM directly.
     */
    public void setBpm(float bpm) {
        bpmParam.setValue(Math.max(40.0f, Math.min(240.0f, bpm)));
    }

    /**
     * Get the duration of a quarter note in milliseconds.
     */
    public float getQuarterNoteMs() {
        return 60000.0f / bpmParam.getTargetValue();
    }

    /**
     * Get the duration of an eighth note in milliseconds.
     */
    public float getEighthNoteMs() {
        return getQuarterNoteMs() / 2.0f;
    }

    /**
     * Get the duration of a sixteenth note in milliseconds.
     */
    public float getSixteenthNoteMs() {
        return getQuarterNoteMs() / 4.0f;
    }

    /**
     * Get the duration of a dotted quarter note in milliseconds.
     */
    public float getDottedQuarterNoteMs() {
        return getQuarterNoteMs() * 1.5f;
    }

    /**
     * Get the duration of a dotted eighth note in milliseconds.
     */
    public float getDottedEighthNoteMs() {
        return getEighthNoteMs() * 1.5f;
    }

    /**
     * Get a note duration based on subdivision.
     *
     * @param subdivision 1=whole, 2=half, 4=quarter, 8=eighth, 16=sixteenth
     * @param dotted true for dotted notes (1.5x duration)
     * @return Duration in milliseconds
     */
    public float getNoteDurationMs(int subdivision, boolean dotted) {
        float quarterMs = getQuarterNoteMs();
        float durationMs = quarterMs * 4.0f / subdivision;
        if (dotted) {
            durationMs *= 1.5f;
        }
        return durationMs;
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        // Register as global instance when prepared
        globalInstance = this;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Pass-through - this is a utility block
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));
    }

    @Override
    protected void onReset() {
        tapTimes.clear();
    }

    @Override
    public void release() {
        super.release();
        // Clear global instance if this was it
        if (globalInstance == this) {
            globalInstance = null;
        }
    }

    @Override
    public int[] getParameterRowSizes() {
        // Single row with BPM display and TAP button
        return new int[] {2};
    }
}
