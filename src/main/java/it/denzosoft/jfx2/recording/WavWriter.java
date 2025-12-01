package it.denzosoft.jfx2.recording;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * WAV file writer with support for markers/cue points.
 *
 * <p>Supports:
 * - PCM 16-bit and 24-bit audio
 * - Mono and stereo
 * - Cue point markers
 * - Streaming write (for real-time recording)</p>
 */
public class WavWriter implements AutoCloseable {

    /**
     * Bit depth options for WAV files.
     */
    public enum BitDepth {
        BIT_16(16, 2),
        BIT_24(24, 3);

        public final int bits;
        public final int bytesPerSample;

        BitDepth(int bits, int bytesPerSample) {
            this.bits = bits;
            this.bytesPerSample = bytesPerSample;
        }
    }

    /**
     * Marker/cue point in the audio file.
     */
    public static class Marker {
        public final int id;
        public final long samplePosition;
        public final String label;

        public Marker(int id, long samplePosition, String label) {
            this.id = id;
            this.samplePosition = samplePosition;
            this.label = label;
        }
    }

    private FileChannel channel;
    private RandomAccessFile raf;
    private final Path filePath;
    private final int sampleRate;
    private final int channels;
    private final BitDepth bitDepth;

    private long samplesWritten;
    private long dataChunkSizePosition;
    private long riffSizePosition;

    private final List<Marker> markers;
    private int nextMarkerId;

    private boolean closed;
    private ByteBuffer writeBuffer;

    /**
     * Create a new WAV writer.
     *
     * @param filePath   Output file path
     * @param sampleRate Sample rate in Hz
     * @param channels   Number of channels (1 = mono, 2 = stereo)
     * @param bitDepth   Bit depth (16 or 24)
     */
    public WavWriter(Path filePath, int sampleRate, int channels, BitDepth bitDepth) throws IOException {
        this.filePath = filePath;
        this.sampleRate = sampleRate;
        this.channels = channels;
        this.bitDepth = bitDepth;
        this.markers = new ArrayList<>();
        this.nextMarkerId = 1;
        this.samplesWritten = 0;
        this.closed = false;

        // Pre-allocate write buffer (64KB)
        this.writeBuffer = ByteBuffer.allocate(65536);
        this.writeBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Open file and write header
        openFile();
        writeHeader();
    }

    /**
     * Open the output file.
     */
    private void openFile() throws IOException {
        this.raf = new RandomAccessFile(filePath.toFile(), "rw");
        this.channel = raf.getChannel();
    }

    /**
     * Write the WAV header (placeholder sizes to be updated on close).
     */
    private void writeHeader() throws IOException {
        ByteBuffer header = ByteBuffer.allocate(44);
        header.order(ByteOrder.LITTLE_ENDIAN);

        int byteRate = sampleRate * channels * bitDepth.bytesPerSample;
        int blockAlign = channels * bitDepth.bytesPerSample;

        // RIFF header
        header.put("RIFF".getBytes());
        riffSizePosition = header.position();
        header.putInt(0);  // Placeholder for file size - 8
        header.put("WAVE".getBytes());

        // fmt chunk
        header.put("fmt ".getBytes());
        header.putInt(16);  // Chunk size
        header.putShort((short) 1);  // PCM format
        header.putShort((short) channels);
        header.putInt(sampleRate);
        header.putInt(byteRate);
        header.putShort((short) blockAlign);
        header.putShort((short) bitDepth.bits);

        // data chunk header
        header.put("data".getBytes());
        dataChunkSizePosition = header.position();
        header.putInt(0);  // Placeholder for data size

        header.flip();
        channel.write(header);
    }

    /**
     * Write mono audio samples.
     *
     * @param samples    Audio samples (-1.0 to 1.0)
     * @param frameCount Number of samples
     */
    public void write(float[] samples, int frameCount) throws IOException {
        if (closed || channel == null || !channel.isOpen()) {
            throw new IOException("Writer is closed");
        }
        if (channels != 1) {
            throw new IOException("Use writeStereo() for stereo files");
        }

        writeBuffer.clear();

        for (int i = 0; i < frameCount; i++) {
            float sample = Math.max(-1.0f, Math.min(samples[i], 1.0f));

            if (bitDepth == BitDepth.BIT_16) {
                short s = (short) (sample * 32767.0f);
                writeBuffer.putShort(s);
            } else {
                int s = (int) (sample * 8388607.0f);
                writeBuffer.put((byte) (s & 0xFF));
                writeBuffer.put((byte) ((s >> 8) & 0xFF));
                writeBuffer.put((byte) ((s >> 16) & 0xFF));
            }

            // Flush buffer if nearly full
            if (writeBuffer.remaining() < bitDepth.bytesPerSample * 2) {
                writeBuffer.flip();
                channel.write(writeBuffer);
                writeBuffer.clear();
            }
        }

        // Flush remaining
        if (writeBuffer.position() > 0) {
            writeBuffer.flip();
            channel.write(writeBuffer);
        }

        samplesWritten += frameCount;
    }

    /**
     * Write stereo audio samples (interleaved).
     *
     * @param left       Left channel samples
     * @param right      Right channel samples
     * @param frameCount Number of frames (samples per channel)
     */
    public void writeStereo(float[] left, float[] right, int frameCount) throws IOException {
        if (closed || channel == null || !channel.isOpen()) {
            throw new IOException("Writer is closed");
        }
        if (channels != 2) {
            throw new IOException("Use write() for mono files");
        }

        writeBuffer.clear();

        for (int i = 0; i < frameCount; i++) {
            float sampleL = Math.max(-1.0f, Math.min(left[i], 1.0f));
            float sampleR = Math.max(-1.0f, Math.min(right[i], 1.0f));

            if (bitDepth == BitDepth.BIT_16) {
                writeBuffer.putShort((short) (sampleL * 32767.0f));
                writeBuffer.putShort((short) (sampleR * 32767.0f));
            } else {
                int sl = (int) (sampleL * 8388607.0f);
                int sr = (int) (sampleR * 8388607.0f);
                writeBuffer.put((byte) (sl & 0xFF));
                writeBuffer.put((byte) ((sl >> 8) & 0xFF));
                writeBuffer.put((byte) ((sl >> 16) & 0xFF));
                writeBuffer.put((byte) (sr & 0xFF));
                writeBuffer.put((byte) ((sr >> 8) & 0xFF));
                writeBuffer.put((byte) ((sr >> 16) & 0xFF));
            }

            // Flush buffer if nearly full
            if (writeBuffer.remaining() < bitDepth.bytesPerSample * 4) {
                writeBuffer.flip();
                channel.write(writeBuffer);
                writeBuffer.clear();
            }
        }

        // Flush remaining
        if (writeBuffer.position() > 0) {
            writeBuffer.flip();
            channel.write(writeBuffer);
        }

        samplesWritten += frameCount;
    }

    /**
     * Add a marker at the current position.
     *
     * @param label Marker label
     * @return The marker ID
     */
    public int addMarker(String label) {
        int id = nextMarkerId++;
        markers.add(new Marker(id, samplesWritten, label));
        return id;
    }

    /**
     * Add a marker at a specific sample position.
     *
     * @param samplePosition Sample position
     * @param label          Marker label
     * @return The marker ID
     */
    public int addMarker(long samplePosition, String label) {
        int id = nextMarkerId++;
        markers.add(new Marker(id, samplePosition, label));
        return id;
    }

    /**
     * Get all markers.
     */
    public List<Marker> getMarkers() {
        return new ArrayList<>(markers);
    }

    /**
     * Get the number of samples written.
     */
    public long getSamplesWritten() {
        return samplesWritten;
    }

    /**
     * Get the duration in seconds.
     */
    public double getDurationSeconds() {
        return (double) samplesWritten / sampleRate;
    }

    /**
     * Get the file size in bytes (current).
     */
    public long getFileSizeBytes() throws IOException {
        return channel.size();
    }

    /**
     * Close the writer and finalize the WAV file.
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        try {
            // Check channel is still open
            if (channel != null && channel.isOpen()) {
                // Write markers if any
                if (!markers.isEmpty()) {
                    writeMarkersChunk();
                }

                // Update header with final sizes
                updateHeader();
            }
        } finally {
            try {
                if (channel != null && channel.isOpen()) {
                    channel.close();
                }
            } catch (IOException ignored) {}
            try {
                if (raf != null) {
                    raf.close();
                }
            } catch (IOException ignored) {}
        }
    }

    /**
     * Write the cue and labl chunks for markers.
     */
    private void writeMarkersChunk() throws IOException {
        // cue chunk
        ByteBuffer cueChunk = ByteBuffer.allocate(12 + markers.size() * 24);
        cueChunk.order(ByteOrder.LITTLE_ENDIAN);

        cueChunk.put("cue ".getBytes());
        cueChunk.putInt(4 + markers.size() * 24);  // Chunk size
        cueChunk.putInt(markers.size());  // Number of cue points

        for (Marker marker : markers) {
            cueChunk.putInt(marker.id);           // Cue ID
            cueChunk.putInt((int) marker.samplePosition);  // Position
            cueChunk.put("data".getBytes());      // Chunk ID
            cueChunk.putInt(0);                   // Chunk start
            cueChunk.putInt(0);                   // Block start
            cueChunk.putInt((int) marker.samplePosition);  // Sample offset
        }

        cueChunk.flip();
        channel.write(cueChunk);

        // LIST/adtl chunk with labl sub-chunks for marker labels
        int listSize = 4;  // "adtl"
        for (Marker marker : markers) {
            int labelLen = marker.label.length() + 1;  // Include null terminator
            if (labelLen % 2 != 0) labelLen++;  // Pad to even
            listSize += 12 + labelLen;  // labl chunk header + ID + label
        }

        ByteBuffer listChunk = ByteBuffer.allocate(8 + listSize);
        listChunk.order(ByteOrder.LITTLE_ENDIAN);

        listChunk.put("LIST".getBytes());
        listChunk.putInt(listSize);
        listChunk.put("adtl".getBytes());

        for (Marker marker : markers) {
            byte[] labelBytes = marker.label.getBytes();
            int labelLen = labelBytes.length + 1;
            if (labelLen % 2 != 0) labelLen++;

            listChunk.put("labl".getBytes());
            listChunk.putInt(4 + labelLen);  // Chunk size
            listChunk.putInt(marker.id);     // Cue ID

            listChunk.put(labelBytes);
            // Pad with zeros
            for (int i = labelBytes.length; i < labelLen; i++) {
                listChunk.put((byte) 0);
            }
        }

        listChunk.flip();
        channel.write(listChunk);
    }

    /**
     * Update the header with final sizes.
     */
    private void updateHeader() throws IOException {
        long dataSize = samplesWritten * channels * bitDepth.bytesPerSample;
        long fileSize = channel.size();

        // Update RIFF size (file size - 8)
        channel.position(riffSizePosition);
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt((int) (fileSize - 8));
        buf.flip();
        channel.write(buf);

        // Update data chunk size
        channel.position(dataChunkSizePosition);
        buf.clear();
        buf.putInt((int) dataSize);
        buf.flip();
        channel.write(buf);
    }

    /**
     * Get the output file path.
     */
    public Path getFilePath() {
        return filePath;
    }

    /**
     * Get the sample rate.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Get the number of channels.
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Get the bit depth.
     */
    public BitDepth getBitDepth() {
        return bitDepth;
    }
}
