package it.denzosoft.jfx2.effects.impl;

import it.denzosoft.jfx2.effects.*;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * WAV file recorder effect - records audio to a WAV file.
 *
 * <p>This effect passes audio through while optionally recording it to disk.
 * When recording starts, a new file is created with a timestamp suffix.</p>
 *
 * <p>Supports both mono and stereo recording:
 * - Mono input (onProcess) → mono WAV file
 * - Stereo input (onProcessStereo) → stereo WAV file</p>
 */
public class WavFileOutputEffect extends AbstractEffect {

    private static final EffectMetadata METADATA = EffectMetadata.of(
            "wavfileoutput",
            "WAV Recorder",
            "Records audio to WAV file with timestamp",
            EffectCategory.OUTPUT_SINK
    );

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String WAV_OUTPUT_DIR = "./wav-output/";

    // Parameters
    private final Parameter recordingParam;

    // Recording state
    private String outputDirectory = WAV_OUTPUT_DIR;
    private String rigName = "recording";
    private volatile boolean isRecording;
    private List<float[]> recordedBuffersL;  // Left channel (or mono)
    private List<float[]> recordedBuffersR;  // Right channel (null for mono)
    private int totalFramesRecorded;         // Frames (samples per channel)
    private int recordingSampleRate;
    private int recordingChannels;           // 1 = mono, 2 = stereo
    private String currentRecordingPath;

    public WavFileOutputEffect() {
        super(METADATA);

        // Recording state (toggle)
        recordingParam = addBooleanParameter("recording", "Recording",
                "Start or stop recording. Each recording creates a new file with timestamp.",
                false);
    }

    @Override
    protected void onPrepare(int sampleRate, int maxFrameCount) {
        this.recordingSampleRate = sampleRate;
    }

    @Override
    protected void onProcess(float[] input, float[] output, int frameCount) {
        // Pass-through: copy input to output
        System.arraycopy(input, 0, output, 0, Math.min(frameCount, Math.min(input.length, output.length)));

        // Check recording state
        boolean shouldRecord = recordingParam.getValue() > 0.5f;

        if (shouldRecord && !isRecording) {
            // Start recording in mono mode
            startRecording(1);
        } else if (!shouldRecord && isRecording) {
            // Stop recording
            stopRecording();
        }

        // Record if active (mono)
        if (isRecording && recordedBuffersL != null && recordingChannels == 1) {
            // Copy buffer for recording
            float[] bufferCopy = new float[frameCount];
            System.arraycopy(input, 0, bufferCopy, 0, Math.min(frameCount, input.length));
            recordedBuffersL.add(bufferCopy);
            totalFramesRecorded += frameCount;
        }
    }

    @Override
    protected void onProcessStereo(float[] inputL, float[] inputR, float[] outputL, float[] outputR, int frameCount) {
        // Pass-through: copy input to output
        int len = Math.min(frameCount, Math.min(inputL.length, Math.min(inputR.length, Math.min(outputL.length, outputR.length))));
        System.arraycopy(inputL, 0, outputL, 0, len);
        System.arraycopy(inputR, 0, outputR, 0, len);

        // Check recording state
        boolean shouldRecord = recordingParam.getValue() > 0.5f;

        if (shouldRecord && !isRecording) {
            // Start recording in stereo mode
            startRecording(2);
        } else if (!shouldRecord && isRecording) {
            // Stop recording
            stopRecording();
        }

        // Record if active (stereo)
        if (isRecording && recordedBuffersL != null && recordingChannels == 2) {
            // Copy L and R buffers separately for true stereo recording
            float[] bufferCopyL = new float[len];
            float[] bufferCopyR = new float[len];
            System.arraycopy(inputL, 0, bufferCopyL, 0, len);
            System.arraycopy(inputR, 0, bufferCopyR, 0, len);
            recordedBuffersL.add(bufferCopyL);
            recordedBuffersR.add(bufferCopyR);
            totalFramesRecorded += len;
        }
    }

    @Override
    protected void onReset() {
        if (isRecording) {
            stopRecording();
        }
    }

    @Override
    public void release() {
        if (isRecording) {
            stopRecording();
        }
        super.release();
    }

    /**
     * Set the output directory for recordings.
     */
    public void setOutputDirectory(String directory) {
        this.outputDirectory = directory;
    }

    /**
     * Get the output directory.
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    /**
     * Set the rig name (used in filename).
     */
    public void setRigName(String name) {
        this.rigName = name != null && !name.isEmpty() ? name : "recording";
    }

    /**
     * Get the rig name.
     */
    public String getRigName() {
        return rigName;
    }

    /**
     * Start recording to a new file.
     * @param channels 1 for mono, 2 for stereo
     */
    public void startRecording(int channels) {
        if (isRecording) {
            return;
        }

        // Create output directory if needed
        Path dirPath = Path.of(outputDirectory);
        try {
            Files.createDirectories(dirPath);
        } catch (IOException e) {
            System.err.println("Cannot create output directory: " + e.getMessage());
            return;
        }

        // Generate filename with rig name and timestamp: <rigName>-yyyyMMddHHmmss.wav
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String safeRigName = rigName.replaceAll("[^a-zA-Z0-9_-]", "_");
        String fileName = safeRigName + "-" + timestamp + ".wav";
        currentRecordingPath = dirPath.resolve(fileName).toString();

        // Initialize recording buffers
        recordingChannels = channels;
        recordedBuffersL = new ArrayList<>();
        recordedBuffersR = (channels == 2) ? new ArrayList<>() : null;
        totalFramesRecorded = 0;
        isRecording = true;
        recordingParam.setValue(1.0f);

        System.out.println("Started recording to: " + currentRecordingPath +
                " (" + (channels == 2 ? "stereo" : "mono") + ")");
    }

    /**
     * Start recording to a new file (default mono for backward compatibility).
     */
    public void startRecording() {
        startRecording(1);
    }

    /**
     * Stop recording and save the file.
     */
    public void stopRecording() {
        if (!isRecording) {
            return;
        }

        isRecording = false;
        recordingParam.setValue(0.0f);

        if (recordedBuffersL == null || recordedBuffersL.isEmpty()) {
            System.out.println("No audio recorded");
            recordedBuffersL = null;
            recordedBuffersR = null;
            return;
        }

        // Save to WAV file
        try {
            saveWavFile();
            System.out.println("Saved recording: " + currentRecordingPath +
                    " (" + String.format("%.2f", totalFramesRecorded / (float) recordingSampleRate) + "s, " +
                    (recordingChannels == 2 ? "stereo" : "mono") + ")");
        } catch (IOException e) {
            System.err.println("Error saving WAV file: " + e.getMessage());
        }

        recordedBuffersL = null;
        recordedBuffersR = null;
        totalFramesRecorded = 0;
    }

    /**
     * Save recorded audio to WAV file.
     */
    private void saveWavFile() throws IOException {
        if (currentRecordingPath == null || recordedBuffersL == null) {
            return;
        }

        // Combine all L buffers into one array
        float[] allSamplesL = new float[totalFramesRecorded];
        int offset = 0;
        for (float[] buffer : recordedBuffersL) {
            System.arraycopy(buffer, 0, allSamplesL, offset, buffer.length);
            offset += buffer.length;
        }

        // For stereo, also combine R buffers
        float[] allSamplesR = null;
        if (recordingChannels == 2 && recordedBuffersR != null) {
            allSamplesR = new float[totalFramesRecorded];
            offset = 0;
            for (float[] buffer : recordedBuffersR) {
                System.arraycopy(buffer, 0, allSamplesR, offset, buffer.length);
                offset += buffer.length;
            }
        }

        // Convert to 16-bit PCM (interleaved for stereo)
        int totalSamples = totalFramesRecorded * recordingChannels;
        byte[] pcmData = new byte[totalSamples * 2];
        ByteBuffer byteBuffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN);

        for (int i = 0; i < totalFramesRecorded; i++) {
            // Left channel (or mono)
            float sampleL = Math.max(-1.0f, Math.min(1.0f, allSamplesL[i]));
            short shortSampleL = (short) (sampleL * 32767);
            byteBuffer.putShort(shortSampleL);

            // Right channel (if stereo)
            if (recordingChannels == 2 && allSamplesR != null) {
                float sampleR = Math.max(-1.0f, Math.min(1.0f, allSamplesR[i]));
                short shortSampleR = (short) (sampleR * 32767);
                byteBuffer.putShort(shortSampleR);
            }
        }

        // Write WAV file
        writeWavFile(currentRecordingPath, pcmData, recordingSampleRate, recordingChannels, 16);
    }

    /**
     * Write a WAV file with the given PCM data.
     */
    private void writeWavFile(String filePath, byte[] pcmData, int sampleRate, int channels, int bitsPerSample)
            throws IOException {

        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int chunkSize = 36 + dataSize;

        try (FileOutputStream fos = new FileOutputStream(filePath);
             DataOutputStream dos = new DataOutputStream(fos)) {

            // RIFF header
            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(chunkSize));
            dos.writeBytes("WAVE");

            // fmt sub-chunk
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16)); // Sub-chunk size (16 for PCM)
            dos.writeShort(Short.reverseBytes((short) 1)); // Audio format (1 = PCM)
            dos.writeShort(Short.reverseBytes((short) channels));
            dos.writeInt(Integer.reverseBytes(sampleRate));
            dos.writeInt(Integer.reverseBytes(byteRate));
            dos.writeShort(Short.reverseBytes((short) blockAlign));
            dos.writeShort(Short.reverseBytes((short) bitsPerSample));

            // data sub-chunk
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(dataSize));
            dos.write(pcmData);
        }
    }

    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Get the path of the current or last recording.
     */
    public String getCurrentRecordingPath() {
        return currentRecordingPath;
    }

    /**
     * Get the duration of the current recording in seconds.
     */
    public float getRecordingDuration() {
        if (recordingSampleRate == 0) return 0;
        return (float) totalFramesRecorded / recordingSampleRate;
    }

    /**
     * Check if currently recording in stereo.
     */
    public boolean isStereoRecording() {
        return isRecording && recordingChannels == 2;
    }

    /**
     * Toggle recording state.
     */
    public void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
    }
}
