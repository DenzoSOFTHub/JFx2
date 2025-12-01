package it.denzosoft.jfx2.audio;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Audio engine using Java Sound API.
 *
 * <p>Coordinates audio processing between InputNodes and OutputNodes.
 * Input and output devices are now managed by their respective nodes,
 * allowing multiple inputs and outputs with different devices.</p>
 *
 * <p>The engine provides:
 * <ul>
 *   <li>Processing thread management</li>
 *   <li>Audio metrics collection</li>
 *   <li>Device enumeration for node configuration</li>
 * </ul>
 * </p>
 */
public class AudioEngine {

    private AudioConfig config;
    private AudioCallback callback;
    private AudioMetrics metrics;

    // Processing thread
    private Thread processingThread;
    private volatile boolean running;

    // Pre-allocated buffers (for callback)
    private float[] inputFloatBuffer;
    private float[] outputFloatBuffer;

    // State
    private volatile boolean initialized;

    public AudioEngine() {
        this.metrics = new AudioMetrics();
        this.initialized = false;
        this.running = false;
    }

    /**
     * Initialize the audio engine with the given configuration.
     *
     * @param config Audio configuration
     * @throws AudioEngineException if initialization fails
     */
    public void initialize(AudioConfig config) throws AudioEngineException {
        if (initialized) {
            throw new AudioEngineException("Audio engine already initialized");
        }

        config.validate();
        this.config = config;

        // Pre-allocate buffers for callback
        inputFloatBuffer = new float[config.bufferSize() * config.inputChannels()];
        outputFloatBuffer = new float[config.bufferSize() * config.outputChannels()];

        initialized = true;
        System.out.println("Audio engine initialized: " + config);
    }

    /**
     * Start audio processing with the given callback.
     *
     * @param callback Callback to process audio
     */
    public void start(AudioCallback callback) {
        if (!initialized) {
            throw new IllegalStateException("Audio engine not initialized");
        }
        if (running) {
            throw new IllegalStateException("Audio engine already running");
        }

        this.callback = callback;
        this.running = true;
        metrics.reset();

        // Start processing thread
        processingThread = new Thread(this::processingLoop, "AudioProcessingThread");
        processingThread.setPriority(Thread.MAX_PRIORITY);
        processingThread.start();

        System.out.println("Audio engine started");
    }

    /**
     * Stop audio processing.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;

        // Wait for processing thread to finish
        if (processingThread != null) {
            try {
                processingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            processingThread = null;
        }

        System.out.println("Audio engine stopped");
    }

    /**
     * Shutdown the audio engine and release resources.
     */
    public void shutdown() {
        stop();
        initialized = false;
        System.out.println("Audio engine shutdown");
    }

    /**
     * Main processing loop - runs in dedicated thread.
     *
     * <p>This loop runs at the configured buffer rate.
     * InputNodes read from their devices, the callback processes the graph,
     * and OutputNodes write to their devices.</p>
     *
     * <p>Timing is controlled by the blocking write in AudioOutputEffect.
     * No sleep is needed as the SourceDataLine.write() blocks when buffer is full.</p>
     */
    private void processingLoop() {
        // Calculate timing for metrics
        long bufferTimeNanos = (long) ((config.bufferSize() * 1_000_000_000L) / config.sampleRate());

        while (running) {
            try {
                metrics.beginProcessing();

                int frameCount = config.bufferSize();

                // Process audio through callback
                // The callback is responsible for:
                // 1. Reading from InputNodes (InputNode.readFromDevice)
                // 2. Processing the signal graph
                // 3. Writing to OutputNodes (OutputNode.writeToDevice)
                // Note: AudioOutputEffect.write() is blocking, which controls timing
                if (callback != null) {
                    callback.process(inputFloatBuffer, outputFloatBuffer, frameCount);
                }

                // Update metrics
                metrics.endProcessing(frameCount);
                metrics.updateCpuLoad(bufferTimeNanos);
                metrics.updateLevels(inputFloatBuffer, outputFloatBuffer, frameCount);

            } catch (Exception e) {
                System.err.println("Error in audio processing: " + e.getMessage());
                metrics.recordDropout();
            }
        }
    }

    /**
     * Get list of available input devices.
     *
     * @return List of input device Mixer.Info
     */
    public List<Mixer.Info> getInputDevices() {
        List<Mixer.Info> devices = new ArrayList<>();
        AudioFormat testFormat = new AudioFormat(44100, 16, 1, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(targetInfo)) {
                    devices.add(mixerInfo);
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }
        return devices;
    }

    /**
     * Get list of available output devices.
     *
     * @return List of output device Mixer.Info
     */
    public List<Mixer.Info> getOutputDevices() {
        List<Mixer.Info> devices = new ArrayList<>();
        AudioFormat testFormat = new AudioFormat(44100, 16, 2, true, false);
        DataLine.Info sourceInfo = new DataLine.Info(SourceDataLine.class, testFormat);

        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(sourceInfo)) {
                    devices.add(mixerInfo);
                }
            } catch (Exception e) {
                // Skip problematic devices
            }
        }
        return devices;
    }

    /**
     * Get current audio metrics.
     */
    public AudioMetrics getMetrics() {
        return metrics;
    }

    /**
     * Get current configuration.
     */
    public AudioConfig getConfig() {
        return config;
    }

    /**
     * Check if the engine is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Check if the engine is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Get the configured sample rate.
     */
    public int getSampleRate() {
        return config != null ? config.sampleRate() : 44100;
    }

    /**
     * Get the configured buffer size.
     */
    public int getBufferSize() {
        return config != null ? config.bufferSize() : 512;
    }

    /**
     * Exception thrown by AudioEngine operations.
     */
    public static class AudioEngineException extends Exception {
        public AudioEngineException(String message) {
            super(message);
        }

        public AudioEngineException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
