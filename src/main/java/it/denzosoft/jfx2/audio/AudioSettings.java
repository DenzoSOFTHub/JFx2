package it.denzosoft.jfx2.audio;

import java.util.prefs.Preferences;

/**
 * Global audio settings.
 * Persisted using Java Preferences API.
 */
public class AudioSettings {

    private static final String PREF_INPUT_CHANNELS = "audio.inputChannels";
    private static final String PREF_SAMPLE_RATE = "audio.sampleRate";
    private static final String PREF_BUFFER_SIZE = "audio.bufferSize";
    private static final String PREF_TUNER_REFERENCE_FREQ = "tuner.referenceFrequency";

    private static final Preferences prefs = Preferences.userNodeForPackage(AudioSettings.class);

    // Singleton instance
    private static AudioSettings instance;

    private int inputChannels;  // 1=mono, 2=stereo
    private int sampleRate;
    private int bufferSize;
    private double tunerReferenceFrequency;  // A4 reference (default 440 Hz)

    private AudioSettings() {
        load();
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized AudioSettings getInstance() {
        if (instance == null) {
            instance = new AudioSettings();
        }
        return instance;
    }

    /**
     * Load settings from preferences.
     */
    public void load() {
        inputChannels = prefs.getInt(PREF_INPUT_CHANNELS, 1);  // Default: mono
        sampleRate = prefs.getInt(PREF_SAMPLE_RATE, 44100);
        bufferSize = prefs.getInt(PREF_BUFFER_SIZE, 2048);
        tunerReferenceFrequency = prefs.getDouble(PREF_TUNER_REFERENCE_FREQ, 440.0);  // Default: A4 = 440 Hz
    }

    /**
     * Save settings to preferences.
     */
    public void save() {
        prefs.putInt(PREF_INPUT_CHANNELS, inputChannels);
        prefs.putInt(PREF_SAMPLE_RATE, sampleRate);
        prefs.putInt(PREF_BUFFER_SIZE, bufferSize);
        prefs.putDouble(PREF_TUNER_REFERENCE_FREQ, tunerReferenceFrequency);

        try {
            prefs.flush();
        } catch (Exception e) {
            System.err.println("[AudioSettings] Failed to save preferences: " + e.getMessage());
        }
    }

    /**
     * Get the number of input channels (1=mono, 2=stereo).
     */
    public int getInputChannels() {
        return inputChannels;
    }

    /**
     * Set the number of input channels.
     * @param channels 1 for mono, 2 for stereo
     */
    public void setInputChannels(int channels) {
        if (channels < 1 || channels > 2) {
            throw new IllegalArgumentException("Input channels must be 1 or 2");
        }
        this.inputChannels = channels;
    }

    /**
     * Check if input is stereo.
     */
    public boolean isStereoInput() {
        return inputChannels == 2;
    }

    /**
     * Set stereo or mono input.
     */
    public void setStereoInput(boolean stereo) {
        this.inputChannels = stereo ? 2 : 1;
    }

    /**
     * Get the sample rate.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Set the sample rate.
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * Get the buffer size.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Set the buffer size.
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Create an AudioConfig based on current settings.
     */
    public AudioConfig toAudioConfig() {
        return new AudioConfig(sampleRate, bufferSize, inputChannels, 2);
    }

    /**
     * Get a description of the input mode.
     */
    public String getInputModeDescription() {
        return inputChannels == 1 ? "Mono" : "Stereo";
    }

    /**
     * Get the tuner reference frequency (A4).
     * Default is 440 Hz.
     */
    public double getTunerReferenceFrequency() {
        return tunerReferenceFrequency;
    }

    /**
     * Set the tuner reference frequency (A4).
     * @param frequency Reference frequency in Hz (typically 432-446)
     */
    public void setTunerReferenceFrequency(double frequency) {
        if (frequency < 400 || frequency > 480) {
            throw new IllegalArgumentException("Reference frequency must be between 400 and 480 Hz");
        }
        this.tunerReferenceFrequency = frequency;
    }

    @Override
    public String toString() {
        return "AudioSettings{" +
                "inputChannels=" + inputChannels +
                ", sampleRate=" + sampleRate +
                ", bufferSize=" + bufferSize +
                '}';
    }
}
