package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-layer looper effect with recording, overdubbing, and export.
 *
 * <p>Features:
 * <ul>
 *   <li>Record initial loop (up to 5 minutes)</li>
 *   <li>Load WAV file as initial loop with autoplay</li>
 *   <li>Overdub additional layers</li>
 *   <li>Delete last layer (undo)</li>
 *   <li>Pause/resume playback</li>
 *   <li>Mute without stopping</li>
 *   <li>Export all layers to WAV</li>
 * </ul>
 */
public class LooperEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "looper",
            "Looper",
            "Multi-layer looper with overdub and export",
            EffectCategory.UTILITY
    );

    // Maximum loop duration: 300 seconds (5 minutes)
    private static final float MAX_LOOP_SECONDS = 300.0f;

    // Looper states
    public enum LooperState {
        IDLE,        // Nothing recorded, waiting
        RECORDING,   // Recording initial loop
        PLAYING,     // Playing back loop
        OVERDUBBING, // Playing + recording new layer
        PAUSED       // Playback paused
    }

    // Parameters (used as action triggers)
    private final Parameter recParam;
    private final Parameter stopParam;
    private final Parameter overdubParam;
    private final Parameter pauseParam;
    private final Parameter deleteParam;
    private final Parameter muteParam;
    private final Parameter saveParam;
    private final Parameter levelParam;
    private final Parameter feedbackParam;

    // File loading parameters
    private String wavFilePath = "";
    private boolean autoPlay = false;
    private boolean fileLoaded = false;
    private float[] pendingFileData = null;
    private int pendingFileSampleRate = 0;

    // State
    private LooperState state = LooperState.IDLE;
    private boolean muted = false;

    // Audio buffers
    private List<float[]> layers;        // Recorded layers
    private float[] recordBuffer;        // Current recording buffer
    private int loopLength;              // Length of the loop in samples
    private int playPosition;            // Current playback position
    private int recordPosition;          // Current record position
    private int sampleRate;
    private int maxLoopSamples;

    // Previous button states for edge detection
    private boolean prevRec = false;
    private boolean prevStop = false;
    private boolean prevOverdub = false;
    private boolean prevPause = false;
    private boolean prevDelete = false;
    private boolean prevMute = false;
    private boolean prevSave = false;

    public LooperEffect() {
        super(METADATA);

        // Action buttons (boolean parameters used as triggers)
        recParam = addBooleanParameter("rec", "Rec",
                "Start recording a new loop. Press Stop when done.", false);

        stopParam = addBooleanParameter("stop", "Stop",
                "Stop recording and start playback.", false);

        overdubParam = addBooleanParameter("overdub", "Overdub",
                "Record a new layer while playing. Press again to finish.", false);

        pauseParam = addBooleanParameter("pause", "Pause",
                "Pause/resume loop playback.", false);

        deleteParam = addBooleanParameter("delete", "Delete",
                "Delete the last recorded layer.", false);

        muteParam = addBooleanParameter("mute", "Mute",
                "Mute loop output (loop continues running).", false);

        saveParam = addBooleanParameter("save", "Save",
                "Export all layers mixed to a WAV file.", false);

        // Level control
        levelParam = addFloatParameter("level", "Level",
                "Output level of the loop playback.",
                0.0f, 100.0f, 80.0f, "%");

        // Feedback for overdub
        feedbackParam = addFloatParameter("feedback", "Feedback",
                "How much of existing layers to keep when overdubbing.",
                0.0f, 100.0f, 100.0f, "%");
    }

    /**
     * Set a WAV file to load as the initial loop.
     * Call this before prepare() or it will be loaded on next prepare().
     *
     * @param filePath Path to the WAV file
     * @param autoPlay If true, start playing automatically when audio starts
     */
    public void setWavFile(String filePath, boolean autoPlay) {
        this.wavFilePath = filePath != null ? filePath : "";
        this.autoPlay = autoPlay;
        this.fileLoaded = false;

        // If already prepared, load the file now
        if (sampleRate > 0 && !wavFilePath.isEmpty()) {
            loadWavFile();
        }
    }

    /**
     * Get the current WAV file path.
     */
    public String getWavFilePath() {
        return wavFilePath;
    }

    /**
     * Check if autoplay is enabled.
     */
    public boolean isAutoPlay() {
        return autoPlay;
    }

    /**
     * Load WAV file and prepare it as the first layer.
     */
    private void loadWavFile() {
        if (wavFilePath.isEmpty()) {
            return;
        }

        File file = new File(wavFilePath);
        if (!file.exists() || !file.canRead()) {
            System.err.println("[Looper] Cannot read file: " + wavFilePath);
            return;
        }

        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            AudioFormat format = audioStream.getFormat();

            // Convert to mono float if needed
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    format.getSampleRate(),
                    16,
                    1,  // mono
                    2,
                    format.getSampleRate(),
                    false
            );

            AudioInputStream convertedStream = audioStream;
            if (!format.matches(targetFormat)) {
                // Try to convert
                if (AudioSystem.isConversionSupported(targetFormat, format)) {
                    convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
                } else {
                    // Manual conversion for stereo to mono
                    convertedStream = audioStream;
                }
            }

            // Read all bytes
            byte[] audioBytes = convertedStream.readAllBytes();
            int channels = format.getChannels();
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int frameSize = channels * bytesPerSample;
            int numFrames = audioBytes.length / frameSize;

            // Limit to max loop length
            int maxFrames = (int) (MAX_LOOP_SECONDS * format.getSampleRate());
            numFrames = Math.min(numFrames, maxFrames);

            // Convert to float array (mix to mono if stereo)
            float[] samples = new float[numFrames];

            for (int i = 0; i < numFrames; i++) {
                float sum = 0;
                for (int ch = 0; ch < channels; ch++) {
                    int offset = i * frameSize + ch * bytesPerSample;
                    if (offset + 1 < audioBytes.length) {
                        // Assume 16-bit little-endian
                        short sample = (short) ((audioBytes[offset] & 0xFF) | (audioBytes[offset + 1] << 8));
                        sum += sample / 32768.0f;
                    }
                }
                samples[i] = sum / channels;
            }

            // Store for use in prepare or apply immediately
            pendingFileData = samples;
            pendingFileSampleRate = (int) format.getSampleRate();
            fileLoaded = true;

            System.out.println("[Looper] Loaded WAV file: " + wavFilePath +
                    " (" + numFrames + " samples, " +
                    String.format("%.2f", numFrames / format.getSampleRate()) + "s)");

            audioStream.close();

            // Apply if already prepared
            if (this.sampleRate > 0) {
                applyLoadedFile();
            }

        } catch (Exception e) {
            System.err.println("[Looper] Error loading WAV file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Apply the loaded file data as the first layer.
     */
    private void applyLoadedFile() {
        if (pendingFileData == null || pendingFileData.length == 0) {
            return;
        }

        // Resample if needed
        float[] data = pendingFileData;
        if (pendingFileSampleRate != sampleRate && pendingFileSampleRate > 0) {
            data = resample(pendingFileData, pendingFileSampleRate, sampleRate);
        }

        // Limit to max loop samples
        int length = Math.min(data.length, maxLoopSamples);

        // Set as first layer
        layers.clear();
        float[] layer = new float[length];
        System.arraycopy(data, 0, layer, 0, length);
        layers.add(layer);

        loopLength = length;
        playPosition = 0;

        System.out.println("[Looper] Applied WAV as loop: " + length + " samples (" +
                String.format("%.2f", (float) length / sampleRate) + "s)");

        // Auto-play if enabled
        if (autoPlay) {
            state = LooperState.PLAYING;
            System.out.println("[Looper] Auto-play started");
        } else {
            state = LooperState.PAUSED;
        }

        pendingFileData = null;
    }

    /**
     * Simple linear resampling.
     */
    private float[] resample(float[] input, int fromRate, int toRate) {
        if (fromRate == toRate) {
            return input;
        }

        double ratio = (double) toRate / fromRate;
        int newLength = (int) (input.length * ratio);
        float[] output = new float[newLength];

        for (int i = 0; i < newLength; i++) {
            double srcIndex = i / ratio;
            int idx0 = (int) srcIndex;
            int idx1 = Math.min(idx0 + 1, input.length - 1);
            double frac = srcIndex - idx0;
            output[i] = (float) (input[idx0] * (1 - frac) + input[idx1] * frac);
        }

        return output;
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.sampleRate = sampleRate;
        this.maxLoopSamples = (int) (MAX_LOOP_SECONDS * sampleRate);

        layers = new ArrayList<>();
        recordBuffer = new float[maxLoopSamples];
        loopLength = 0;
        playPosition = 0;
        recordPosition = 0;
        state = LooperState.IDLE;
        muted = false;

        // Load WAV file if specified
        if (!wavFilePath.isEmpty() && !fileLoaded) {
            loadWavFile();
        }

        // Apply loaded file if pending
        if (pendingFileData != null) {
            applyLoadedFile();
        }
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        float level = levelParam.getValue() / 100.0f;

        // Detect button presses (rising edge)
        detectButtonPresses();

        for (int i = 0; i < frameCount && i < input.length && i < output.length; i++) {
            float in = input[i];
            float loopOut = 0.0f;

            switch (state) {
                case IDLE:
                    // Pass through input
                    output[i] = in;
                    break;

                case RECORDING:
                    // Record input, pass through
                    if (recordPosition < maxLoopSamples) {
                        recordBuffer[recordPosition++] = in;
                    }
                    output[i] = in;
                    break;

                case PLAYING:
                    // Play back layers
                    if (loopLength > 0) {
                        loopOut = getLayersMixed(playPosition) * level;
                        playPosition = (playPosition + 1) % loopLength;
                    }
                    output[i] = in + (muted ? 0 : loopOut);
                    break;

                case OVERDUBBING:
                    // Play + record
                    if (loopLength > 0) {
                        loopOut = getLayersMixed(playPosition) * level;

                        // Record input to buffer
                        recordBuffer[playPosition] = in;

                        playPosition = (playPosition + 1) % loopLength;
                    }
                    output[i] = in + (muted ? 0 : loopOut);
                    break;

                case PAUSED:
                    // Pass through, don't advance position
                    output[i] = in;
                    break;
            }
        }
    }

    /**
     * Get mixed output from all layers at given position.
     */
    private float getLayersMixed(int position) {
        float sum = 0;
        for (float[] layer : layers) {
            if (position < layer.length) {
                sum += layer[position];
            }
        }
        return sum;
    }

    /**
     * Detect button presses and handle state transitions.
     */
    private void detectButtonPresses() {
        // REC button
        boolean rec = recParam.getBooleanValue();
        if (rec && !prevRec) {
            onRecPressed();
        }
        prevRec = rec;

        // STOP button
        boolean stop = stopParam.getBooleanValue();
        if (stop && !prevStop) {
            onStopPressed();
        }
        prevStop = stop;

        // OVERDUB button
        boolean overdub = overdubParam.getBooleanValue();
        if (overdub && !prevOverdub) {
            onOverdubPressed();
        }
        prevOverdub = overdub;

        // PAUSE button
        boolean pause = pauseParam.getBooleanValue();
        if (pause && !prevPause) {
            onPausePressed();
        }
        prevPause = pause;

        // DELETE button
        boolean delete = deleteParam.getBooleanValue();
        if (delete && !prevDelete) {
            onDeletePressed();
        }
        prevDelete = delete;

        // MUTE button
        boolean mute = muteParam.getBooleanValue();
        if (mute != prevMute) {
            muted = mute;
        }
        prevMute = mute;

        // SAVE button
        boolean save = saveParam.getBooleanValue();
        if (save && !prevSave) {
            onSavePressed();
        }
        prevSave = save;
    }

    private void onRecPressed() {
        if (state == LooperState.IDLE || state == LooperState.PLAYING || state == LooperState.PAUSED) {
            // Clear everything and start fresh recording
            layers.clear();
            recordPosition = 0;
            loopLength = 0;
            state = LooperState.RECORDING;
            System.out.println("[Looper] Recording started");
        }
    }

    private void onStopPressed() {
        if (state == LooperState.RECORDING) {
            // Finish recording, create first layer
            loopLength = recordPosition;
            if (loopLength > 0) {
                float[] layer = new float[loopLength];
                System.arraycopy(recordBuffer, 0, layer, 0, loopLength);
                layers.add(layer);
                playPosition = 0;
                state = LooperState.PLAYING;
                System.out.println("[Looper] Recording stopped. Loop length: " + loopLength + " samples (" +
                        String.format("%.2f", (float) loopLength / sampleRate) + "s)");
            } else {
                state = LooperState.IDLE;
            }
        } else if (state == LooperState.OVERDUBBING) {
            // Finish overdub, create new layer
            float[] layer = new float[loopLength];
            System.arraycopy(recordBuffer, 0, layer, 0, loopLength);
            layers.add(layer);
            state = LooperState.PLAYING;
            System.out.println("[Looper] Overdub finished. Total layers: " + layers.size());
        } else if (state == LooperState.PLAYING) {
            // Stop playback
            state = LooperState.PAUSED;
            System.out.println("[Looper] Stopped");
        }
    }

    private void onOverdubPressed() {
        if (state == LooperState.PLAYING) {
            // Start overdubbing
            java.util.Arrays.fill(recordBuffer, 0, loopLength, 0.0f);
            state = LooperState.OVERDUBBING;
            System.out.println("[Looper] Overdubbing...");
        } else if (state == LooperState.OVERDUBBING) {
            // Finish overdub (same as stop)
            onStopPressed();
        }
    }

    private void onPausePressed() {
        if (state == LooperState.PLAYING || state == LooperState.OVERDUBBING) {
            state = LooperState.PAUSED;
            System.out.println("[Looper] Paused");
        } else if (state == LooperState.PAUSED) {
            state = LooperState.PLAYING;
            System.out.println("[Looper] Resumed");
        }
    }

    private void onDeletePressed() {
        if (!layers.isEmpty()) {
            layers.remove(layers.size() - 1);
            System.out.println("[Looper] Deleted last layer. Remaining: " + layers.size());
            if (layers.isEmpty()) {
                state = LooperState.IDLE;
                loopLength = 0;
            }
        }
    }

    private void onSavePressed() {
        // This is now handled by saveToFile() called from UI
        System.out.println("[Looper] Use saveToFile(name) to save with a specific name");
    }

    /**
     * Save the loop to a WAV file with the specified name.
     * Files are saved to ~/JFx2_Loops/
     *
     * @param name The name for the file (without extension)
     * @return The path where the file was saved, or null if failed
     */
    public Path saveToFile(String name) {
        if (layers.isEmpty() || loopLength == 0) {
            System.out.println("[Looper] Nothing to save");
            return null;
        }

        if (name == null || name.trim().isEmpty()) {
            name = "loop_" + System.currentTimeMillis();
        }

        // Sanitize filename
        name = name.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");

        try {
            // Mix all layers
            float[] mixed = new float[loopLength];
            for (float[] layer : layers) {
                for (int i = 0; i < loopLength && i < layer.length; i++) {
                    mixed[i] += layer[i];
                }
            }

            // Normalize
            float max = 0;
            for (float sample : mixed) {
                max = Math.max(max, Math.abs(sample));
            }
            if (max > 1.0f) {
                for (int i = 0; i < mixed.length; i++) {
                    mixed[i] /= max;
                }
            }

            // Convert to 16-bit PCM
            byte[] pcmData = new byte[loopLength * 2];
            for (int i = 0; i < loopLength; i++) {
                short sample = (short) (mixed[i] * 32767);
                pcmData[i * 2] = (byte) (sample & 0xFF);
                pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }

            // Create audio format
            AudioFormat format = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    1,  // mono
                    2,  // frame size
                    sampleRate,
                    false  // little endian
            );

            // Save to file in user's home directory
            String filename = name + ".wav";
            Path loopsDir = Path.of(System.getProperty("user.home"), "JFx2_Loops");
            loopsDir.toFile().mkdirs();
            Path path = loopsDir.resolve(filename);

            // Check if file exists
            if (path.toFile().exists()) {
                // Add timestamp to make unique
                filename = name + "_" + System.currentTimeMillis() + ".wav";
                path = loopsDir.resolve(filename);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
            AudioInputStream ais = new AudioInputStream(bais, format, loopLength);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, path.toFile());

            System.out.println("[Looper] Saved to: " + path);
            return path;

        } catch (Exception e) {
            System.err.println("[Looper] Save failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the directory where loops are saved.
     */
    public static Path getLoopsDirectory() {
        return Path.of(System.getProperty("user.home"), "JFx2_Loops");
    }

    @Override
    protected void onReset() {
        if (layers != null) {
            layers.clear();
        }
        if (recordBuffer != null) {
            java.util.Arrays.fill(recordBuffer, 0);
        }
        loopLength = 0;
        playPosition = 0;
        recordPosition = 0;
        state = LooperState.IDLE;
        muted = false;

        // Re-apply file if was loaded
        if (fileLoaded && pendingFileData == null && !wavFilePath.isEmpty()) {
            loadWavFile();
            if (pendingFileData != null && sampleRate > 0) {
                applyLoadedFile();
            }
        }
    }

    // ==================== PUBLIC TRIGGER METHODS ====================
    // These can be called directly from UI to trigger actions

    public void triggerRec() {
        onRecPressed();
    }

    public void triggerStop() {
        onStopPressed();
    }

    public void triggerOverdub() {
        onOverdubPressed();
    }

    public void triggerPause() {
        onPausePressed();
    }

    public void triggerDelete() {
        onDeletePressed();
    }

    public void triggerSave() {
        onSavePressed();
    }

    public void setMuted(boolean mute) {
        this.muted = mute;
    }

    // Public getters for UI
    public LooperState getState() {
        return state;
    }

    public int getLoopLengthSamples() {
        return loopLength;
    }

    public float getLoopLengthSeconds() {
        return sampleRate > 0 ? (float) loopLength / sampleRate : 0;
    }

    public int getLayerCount() {
        return layers != null ? layers.size() : 0;
    }

    public float getPlaybackPosition() {
        return loopLength > 0 ? (float) playPosition / loopLength : 0;
    }

    public boolean isMuted() {
        return muted;
    }

    public boolean isFileLoaded() {
        return fileLoaded && !layers.isEmpty();
    }
}
