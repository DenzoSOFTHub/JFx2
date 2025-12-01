package it.denzosoft.jfx2.audio;

/**
 * Real-time audio processing metrics.
 */
public class AudioMetrics {

    private volatile long processedFrames;
    private volatile long processedCallbacks;
    private volatile long dropouts;
    private volatile double cpuLoad;
    private volatile double peakInputLevel;
    private volatile double peakOutputLevel;
    private volatile long lastUpdateTimeNanos;

    // For CPU load calculation
    private long lastProcessingTimeNanos;
    private long lastCallbackTimeNanos;

    public AudioMetrics() {
        reset();
    }

    /**
     * Reset all metrics to initial values.
     */
    public void reset() {
        processedFrames = 0;
        processedCallbacks = 0;
        dropouts = 0;
        cpuLoad = 0;
        peakInputLevel = 0;
        peakOutputLevel = 0;
        lastUpdateTimeNanos = System.nanoTime();
        lastProcessingTimeNanos = 0;
        lastCallbackTimeNanos = 0;
    }

    /**
     * Called before processing starts.
     */
    public void beginProcessing() {
        lastCallbackTimeNanos = System.nanoTime();
    }

    /**
     * Called after processing completes.
     *
     * @param frameCount Number of frames processed
     */
    public void endProcessing(int frameCount) {
        long now = System.nanoTime();
        lastProcessingTimeNanos = now - lastCallbackTimeNanos;
        processedFrames += frameCount;
        processedCallbacks++;
        lastUpdateTimeNanos = now;
    }

    /**
     * Update CPU load estimate based on processing time vs available time.
     *
     * @param availableTimeNanos Time available for processing (buffer duration)
     */
    public void updateCpuLoad(long availableTimeNanos) {
        if (availableTimeNanos > 0) {
            double load = (double) lastProcessingTimeNanos / availableTimeNanos;
            // Exponential moving average
            cpuLoad = cpuLoad * 0.9 + load * 0.1;
        }
    }

    /**
     * Record a dropout (buffer underrun/overrun).
     */
    public void recordDropout() {
        dropouts++;
    }

    /**
     * Update peak levels from audio buffers.
     */
    public void updateLevels(float[] input, float[] output, int frameCount) {
        float maxIn = 0;
        float maxOut = 0;

        for (int i = 0; i < Math.min(frameCount, input.length); i++) {
            float absIn = Math.abs(input[i]);
            if (absIn > maxIn) maxIn = absIn;
        }

        for (int i = 0; i < Math.min(frameCount * 2, output.length); i++) {
            float absOut = Math.abs(output[i]);
            if (absOut > maxOut) maxOut = absOut;
        }

        // Peak hold with decay
        peakInputLevel = Math.max(maxIn, peakInputLevel * 0.99);
        peakOutputLevel = Math.max(maxOut, peakOutputLevel * 0.99);
    }

    // Getters

    public long getProcessedFrames() {
        return processedFrames;
    }

    public long getProcessedCallbacks() {
        return processedCallbacks;
    }

    public long getDropouts() {
        return dropouts;
    }

    /**
     * Get CPU load as percentage (0-100).
     */
    public double getCpuLoadPercent() {
        return cpuLoad * 100.0;
    }

    /**
     * Get peak input level in dB.
     */
    public double getPeakInputLevelDb() {
        return linearToDb(peakInputLevel);
    }

    /**
     * Get peak output level in dB.
     */
    public double getPeakOutputLevelDb() {
        return linearToDb(peakOutputLevel);
    }

    /**
     * Get peak input level as linear value (0-1).
     */
    public double getPeakInputLevel() {
        return peakInputLevel;
    }

    /**
     * Get peak output level as linear value (0-1).
     */
    public double getPeakOutputLevel() {
        return peakOutputLevel;
    }

    private static double linearToDb(double linear) {
        if (linear <= 0) return -96.0;
        return 20.0 * Math.log10(linear);
    }

    @Override
    public String toString() {
        return String.format("AudioMetrics[CPU: %.1f%%, In: %.1fdB, Out: %.1fdB, Dropouts: %d]",
            getCpuLoadPercent(), getPeakInputLevelDb(), getPeakOutputLevelDb(), dropouts);
    }
}
