package it.denzosoft.jfx2.recording;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Audio recorder for capturing audio to WAV files.
 *
 * <p>Features:
 * - Start/stop/pause recording
 * - Automatic file naming with timestamp
 * - Marker support for marking points during recording
 * - Non-blocking audio capture with background writing
 * - Configurable bit depth (16 or 24 bit)</p>
 *
 * <p>Usage:
 * <pre>
 * AudioRecorder recorder = new AudioRecorder();
 * recorder.prepare(44100, outputDir);
 * recorder.startRecording();
 * // In audio callback:
 * recorder.process(inputBuffer, frameCount);
 * // When done:
 * recorder.stopRecording();
 * </pre></p>
 */
public class AudioRecorder {

    /**
     * Recording state.
     */
    public enum State {
        IDLE,
        RECORDING,
        PAUSED
    }

    // Configuration
    private int sampleRate;
    private Path outputDirectory;
    private WavWriter.BitDepth bitDepth;
    private boolean stereo;

    // State
    private final AtomicReference<State> state;
    private WavWriter writer;
    private Path currentFile;

    // Buffer for background writing
    private final BlockingQueue<float[]> writeQueue;
    private Thread writerThread;
    private volatile boolean writerRunning;

    // Statistics
    private long totalSamplesRecorded;
    private long recordingStartTime;
    private final List<WavWriter.Marker> sessionMarkers;

    // File naming
    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private String filePrefix;

    public AudioRecorder() {
        this.state = new AtomicReference<>(State.IDLE);
        this.writeQueue = new LinkedBlockingQueue<>(1000);  // Buffer up to 1000 blocks
        this.sessionMarkers = new ArrayList<>();
        this.bitDepth = WavWriter.BitDepth.BIT_16;
        this.stereo = false;
        this.filePrefix = "recording";
    }

    /**
     * Prepare the recorder.
     *
     * @param sampleRate      Sample rate in Hz
     * @param outputDirectory Directory for output files
     */
    public void prepare(int sampleRate, Path outputDirectory) throws IOException {
        this.sampleRate = sampleRate;
        this.outputDirectory = outputDirectory;

        // Create output directory if needed
        if (!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
        }
    }

    /**
     * Start recording to a new file.
     *
     * @return The path of the new recording file
     */
    public Path startRecording() throws IOException {
        return startRecording(generateFilename());
    }

    /**
     * Start recording to a specific file.
     *
     * @param filename The filename (without path)
     * @return The full path of the recording file
     */
    public Path startRecording(String filename) throws IOException {
        if (state.get() != State.IDLE) {
            throw new IllegalStateException("Recording already in progress");
        }

        // Create output file
        currentFile = outputDirectory.resolve(filename);
        int channels = stereo ? 2 : 1;
        writer = new WavWriter(currentFile, sampleRate, channels, bitDepth);

        // Start background writer thread
        writerRunning = true;
        writerThread = new Thread(this::writerLoop, "AudioRecorder-Writer");
        writerThread.setPriority(Thread.MAX_PRIORITY - 1);
        writerThread.start();

        // Update state
        totalSamplesRecorded = 0;
        recordingStartTime = System.currentTimeMillis();
        sessionMarkers.clear();

        state.set(State.RECORDING);

        return currentFile;
    }

    /**
     * Stop recording and close the file.
     *
     * @return Information about the completed recording
     */
    public RecordingInfo stopRecording() throws IOException {
        if (state.get() == State.IDLE) {
            return null;
        }

        state.set(State.IDLE);

        // Stop writer thread
        writerRunning = false;
        if (writerThread != null) {
            writerThread.interrupt();
            try {
                writerThread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Flush remaining data
        flushQueue();

        // Close writer
        RecordingInfo info = null;
        if (writer != null) {
            info = new RecordingInfo(
                    currentFile,
                    writer.getSamplesWritten(),
                    writer.getDurationSeconds(),
                    sampleRate,
                    stereo ? 2 : 1,
                    bitDepth,
                    new ArrayList<>(sessionMarkers)
            );
            writer.close();
            writer = null;
        }

        writeQueue.clear();
        currentFile = null;

        return info;
    }

    /**
     * Pause recording.
     */
    public void pauseRecording() {
        if (state.get() == State.RECORDING) {
            state.set(State.PAUSED);
        }
    }

    /**
     * Resume recording after pause.
     */
    public void resumeRecording() {
        if (state.get() == State.PAUSED) {
            state.set(State.RECORDING);
        }
    }

    /**
     * Process audio samples (call from audio callback).
     * This method is non-blocking.
     *
     * @param input      Input audio buffer
     * @param frameCount Number of frames
     */
    public void process(float[] input, int frameCount) {
        if (state.get() != State.RECORDING) {
            return;
        }

        // Copy buffer and queue for background writing
        float[] copy = new float[frameCount];
        System.arraycopy(input, 0, copy, 0, frameCount);

        if (!writeQueue.offer(copy)) {
            // Queue full - drop samples (shouldn't happen with proper sizing)
            System.err.println("AudioRecorder: Write queue overflow, dropping samples");
        }

        totalSamplesRecorded += frameCount;
    }

    /**
     * Process stereo audio samples.
     *
     * @param left       Left channel buffer
     * @param right      Right channel buffer
     * @param frameCount Number of frames
     */
    public void processStereo(float[] left, float[] right, int frameCount) {
        if (state.get() != State.RECORDING || !stereo) {
            return;
        }

        // Interleave and queue
        float[] interleaved = new float[frameCount * 2];
        for (int i = 0; i < frameCount; i++) {
            interleaved[i * 2] = left[i];
            interleaved[i * 2 + 1] = right[i];
        }

        if (!writeQueue.offer(interleaved)) {
            System.err.println("AudioRecorder: Write queue overflow, dropping samples");
        }

        totalSamplesRecorded += frameCount;
    }

    /**
     * Add a marker at the current position.
     *
     * @param label Marker label
     * @return Marker ID
     */
    public int addMarker(String label) {
        if (state.get() == State.IDLE || writer == null) {
            return -1;
        }

        int id = writer.addMarker(label);
        sessionMarkers.add(new WavWriter.Marker(id, totalSamplesRecorded, label));
        return id;
    }

    /**
     * Background writer loop.
     */
    private void writerLoop() {
        while (writerRunning) {
            try {
                float[] buffer = writeQueue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (buffer != null && writer != null && writerRunning) {
                    if (stereo) {
                        // De-interleave
                        int frames = buffer.length / 2;
                        float[] left = new float[frames];
                        float[] right = new float[frames];
                        for (int i = 0; i < frames; i++) {
                            left[i] = buffer[i * 2];
                            right[i] = buffer[i * 2 + 1];
                        }
                        writer.writeStereo(left, right, frames);
                    } else {
                        writer.write(buffer, buffer.length);
                    }
                }
            } catch (InterruptedException e) {
                // Exit on interrupt
                break;
            } catch (IOException e) {
                // Only log if we're still supposed to be running
                if (writerRunning) {
                    System.err.println("AudioRecorder: Write error - " + e.getMessage());
                }
            }
        }
    }

    /**
     * Flush remaining data in the queue.
     */
    private void flushQueue() {
        while (!writeQueue.isEmpty() && writer != null) {
            float[] buffer = writeQueue.poll();
            if (buffer != null) {
                try {
                    if (stereo) {
                        int frames = buffer.length / 2;
                        float[] left = new float[frames];
                        float[] right = new float[frames];
                        for (int i = 0; i < frames; i++) {
                            left[i] = buffer[i * 2];
                            right[i] = buffer[i * 2 + 1];
                        }
                        writer.writeStereo(left, right, frames);
                    } else {
                        writer.write(buffer, buffer.length);
                    }
                } catch (IOException e) {
                    // Writer might be closed, stop flushing
                    break;
                }
            }
        }
    }

    /**
     * Generate a filename with timestamp.
     */
    private String generateFilename() {
        String timestamp = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return filePrefix + "_" + timestamp + ".wav";
    }

    // Getters and setters

    /**
     * Get the current state.
     */
    public State getState() {
        return state.get();
    }

    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return state.get() == State.RECORDING;
    }

    /**
     * Check if paused.
     */
    public boolean isPaused() {
        return state.get() == State.PAUSED;
    }

    /**
     * Get the current recording file path.
     */
    public Path getCurrentFile() {
        return currentFile;
    }

    /**
     * Get total samples recorded in current session.
     */
    public long getTotalSamplesRecorded() {
        return totalSamplesRecorded;
    }

    /**
     * Get recording duration in seconds.
     */
    public double getRecordingDurationSeconds() {
        return (double) totalSamplesRecorded / sampleRate;
    }

    /**
     * Get recording duration as formatted string (MM:SS).
     */
    public String getRecordingDurationString() {
        long seconds = (long) getRecordingDurationSeconds();
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    /**
     * Get elapsed time since recording started (includes pauses).
     */
    public long getElapsedTimeMs() {
        if (recordingStartTime == 0) return 0;
        return System.currentTimeMillis() - recordingStartTime;
    }

    /**
     * Get the bit depth setting.
     */
    public WavWriter.BitDepth getBitDepth() {
        return bitDepth;
    }

    /**
     * Set the bit depth for new recordings.
     */
    public void setBitDepth(WavWriter.BitDepth bitDepth) {
        if (state.get() != State.IDLE) {
            throw new IllegalStateException("Cannot change bit depth while recording");
        }
        this.bitDepth = bitDepth;
    }

    /**
     * Check if stereo recording is enabled.
     */
    public boolean isStereo() {
        return stereo;
    }

    /**
     * Set stereo recording mode.
     */
    public void setStereo(boolean stereo) {
        if (state.get() != State.IDLE) {
            throw new IllegalStateException("Cannot change channels while recording");
        }
        this.stereo = stereo;
    }

    /**
     * Get the file prefix for auto-generated filenames.
     */
    public String getFilePrefix() {
        return filePrefix;
    }

    /**
     * Set the file prefix for auto-generated filenames.
     */
    public void setFilePrefix(String prefix) {
        this.filePrefix = prefix;
    }

    /**
     * Get the output directory.
     */
    public Path getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Information about a completed recording.
     */
    public record RecordingInfo(
            Path filePath,
            long samples,
            double durationSeconds,
            int sampleRate,
            int channels,
            WavWriter.BitDepth bitDepth,
            List<WavWriter.Marker> markers
    ) {
        public String getDurationString() {
            long seconds = (long) durationSeconds;
            return String.format("%02d:%02d", seconds / 60, seconds % 60);
        }

        public long getFileSizeBytes() {
            try {
                return Files.size(filePath);
            } catch (IOException e) {
                return 0;
            }
        }

        public String getFileSizeString() {
            long bytes = getFileSizeBytes();
            if (bytes < 1024) return bytes + " B";
            if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }
}
