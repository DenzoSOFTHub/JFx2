package it.denzosoft.jfx2.nam;

/**
 * Interface for NAM (Neural Amp Modeler) models.
 *
 * <p>Implementations include WaveNet and LSTM architectures.</p>
 */
public interface NAMModel {

    /**
     * Process a single sample.
     *
     * @param sample Input sample
     * @return Processed sample
     */
    float process(float sample);

    /**
     * Process a buffer of samples.
     *
     * @param input Input buffer
     * @param output Output buffer
     * @param numSamples Number of samples to process
     */
    void process(float[] input, float[] output, int numSamples);

    /**
     * Reset internal state (buffers, etc).
     */
    void reset();

    /**
     * Get the receptive field size in samples.
     */
    int getReceptiveField();

    /**
     * Get the expected sample rate.
     */
    int getSampleRate();

    /**
     * Get the architecture name.
     */
    String getArchitecture();

    /**
     * Check if the model has processed enough samples to produce valid output.
     */
    boolean isPrewarmed();
}
