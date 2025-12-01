package it.denzosoft.jfx2.audio;

/**
 * Callback interface for audio processing.
 *
 * <p>Implementations of this interface are called from the audio processing thread.
 * The process method must be real-time safe:</p>
 * <ul>
 *   <li>No memory allocations</li>
 *   <li>No I/O operations</li>
 *   <li>No synchronization/locks</li>
 *   <li>No exceptions</li>
 * </ul>
 */
@FunctionalInterface
public interface AudioCallback {

    /**
     * Process audio samples.
     *
     * <p>This method is called from the audio thread and must complete quickly.
     * Input and output buffers are pre-allocated and reused.</p>
     *
     * @param input      Input samples as float array (normalized -1.0 to 1.0).
     *                   For mono: [s0, s1, s2, ...].
     *                   For stereo: [L0, R0, L1, R1, ...] (interleaved).
     * @param output     Output samples buffer to fill (same format as input).
     *                   For mono input to stereo output, output has 2x the frames.
     * @param frameCount Number of frames to process.
     *                   For mono: frameCount == input.length.
     *                   For stereo: frameCount == input.length / 2.
     */
    void process(float[] input, float[] output, int frameCount);
}
