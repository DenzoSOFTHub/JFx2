package it.denzosoft.jfx2.audio.sf2;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser for SoundFont 2 (SF2) files.
 *
 * <p>SF2 is a RIFF-based format containing:</p>
 * <ul>
 *   <li>INFO: metadata (name, author, etc.)</li>
 *   <li>sdta: sample data (raw PCM)</li>
 *   <li>pdta: preset data (instruments, zones, generators)</li>
 * </ul>
 *
 * <p>This implementation focuses on extracting drum samples for the DrumMachine.</p>
 */
public class SF2File {

    private String name = "Unknown";
    private String author = "";
    private String copyright = "";
    private String comment = "";

    private final List<SF2Sample> samples = new ArrayList<>();
    private final List<SF2Preset> presets = new ArrayList<>();
    private final List<SF2Instrument> instruments = new ArrayList<>();

    // Raw sample data (16-bit signed PCM)
    private short[] sampleData;

    // Preset bags, generators, etc.
    private final List<int[]> presetBags = new ArrayList<>();      // [genIndex, modIndex]
    private final List<int[]> presetGens = new ArrayList<>();      // [genOper, genAmount]
    private final List<int[]> instrumentBags = new ArrayList<>();  // [genIndex, modIndex]
    private final List<int[]> instrumentGens = new ArrayList<>();  // [genOper, genAmount]

    /**
     * Load an SF2 file from disk.
     */
    public static SF2File load(Path path) throws IOException {
        return load(path.toFile());
    }

    /**
     * Load an SF2 file from disk.
     */
    public static SF2File load(File file) throws IOException {
        SF2File sf2 = new SF2File();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {

            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            sf2.parse(buffer);
        }
        return sf2;
    }

    /**
     * Load an SF2 file from an input stream.
     */
    public static SF2File load(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }

        SF2File sf2 = new SF2File();
        ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        sf2.parse(buffer);
        return sf2;
    }

    private void parse(ByteBuffer buffer) throws IOException {
        // RIFF header
        String riff = readFourCC(buffer);
        if (!"RIFF".equals(riff)) {
            throw new IOException("Not a RIFF file");
        }

        int fileSize = buffer.getInt();
        String sfbk = readFourCC(buffer);
        if (!"sfbk".equals(sfbk)) {
            throw new IOException("Not a SoundFont file (expected 'sfbk', got '" + sfbk + "')");
        }

        // Parse chunks
        while (buffer.hasRemaining()) {
            String chunkId = readFourCC(buffer);
            int chunkSize = buffer.getInt();
            int chunkEnd = buffer.position() + chunkSize;

            if ("LIST".equals(chunkId)) {
                String listType = readFourCC(buffer);
                switch (listType) {
                    case "INFO" -> parseInfoChunk(buffer, chunkEnd - 4);
                    case "sdta" -> parseSdtaChunk(buffer, chunkEnd - 4);
                    case "pdta" -> parsePdtaChunk(buffer, chunkEnd - 4);
                }
            }

            buffer.position(chunkEnd);
        }

        // Build connections between presets, instruments, and samples
        buildConnections();
    }

    private void parseInfoChunk(ByteBuffer buffer, int endPos) {
        while (buffer.position() < endPos) {
            String subId = readFourCC(buffer);
            int subSize = buffer.getInt();
            int subEnd = buffer.position() + subSize;

            switch (subId) {
                case "INAM" -> name = readString(buffer, subSize);
                case "IENG" -> author = readString(buffer, subSize);
                case "ICOP" -> copyright = readString(buffer, subSize);
                case "ICMT" -> comment = readString(buffer, subSize);
            }

            buffer.position(subEnd);
        }
    }

    private void parseSdtaChunk(ByteBuffer buffer, int endPos) {
        while (buffer.position() < endPos) {
            String subId = readFourCC(buffer);
            int subSize = buffer.getInt();
            int subEnd = buffer.position() + subSize;

            if ("smpl".equals(subId)) {
                // 16-bit signed PCM samples
                int numSamples = subSize / 2;
                sampleData = new short[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    sampleData[i] = buffer.getShort();
                }
            }

            buffer.position(subEnd);
        }
    }

    private void parsePdtaChunk(ByteBuffer buffer, int endPos) {
        while (buffer.position() < endPos) {
            String subId = readFourCC(buffer);
            int subSize = buffer.getInt();
            int subEnd = buffer.position() + subSize;

            switch (subId) {
                case "phdr" -> parsePresetHeaders(buffer, subSize);
                case "pbag" -> parsePresetBags(buffer, subSize);
                case "pgen" -> parsePresetGenerators(buffer, subSize);
                case "inst" -> parseInstrumentHeaders(buffer, subSize);
                case "ibag" -> parseInstrumentBags(buffer, subSize);
                case "igen" -> parseInstrumentGenerators(buffer, subSize);
                case "shdr" -> parseSampleHeaders(buffer, subSize);
            }

            buffer.position(subEnd);
        }
    }

    private void parsePresetHeaders(ByteBuffer buffer, int size) {
        // Each phdr record is 38 bytes
        int count = size / 38;
        for (int i = 0; i < count; i++) {
            String presetName = readString(buffer, 20);
            int preset = buffer.getShort() & 0xFFFF;
            int bank = buffer.getShort() & 0xFFFF;
            int bagIndex = buffer.getShort() & 0xFFFF;
            buffer.getInt(); // library
            buffer.getInt(); // genre
            buffer.getInt(); // morphology

            if (i < count - 1) { // Skip terminal record
                presets.add(new SF2Preset(presetName.trim(), preset, bank, bagIndex));
            }
        }
    }

    private void parsePresetBags(ByteBuffer buffer, int size) {
        int count = size / 4;
        for (int i = 0; i < count; i++) {
            int genIndex = buffer.getShort() & 0xFFFF;
            int modIndex = buffer.getShort() & 0xFFFF;
            presetBags.add(new int[]{genIndex, modIndex});
        }
    }

    private void parsePresetGenerators(ByteBuffer buffer, int size) {
        int count = size / 4;
        for (int i = 0; i < count; i++) {
            int genOper = buffer.getShort() & 0xFFFF;
            int genAmount = buffer.getShort(); // signed
            presetGens.add(new int[]{genOper, genAmount});
        }
    }

    private void parseInstrumentHeaders(ByteBuffer buffer, int size) {
        // Each inst record is 22 bytes
        int count = size / 22;
        for (int i = 0; i < count; i++) {
            String instName = readString(buffer, 20);
            int bagIndex = buffer.getShort() & 0xFFFF;

            if (i < count - 1) { // Skip terminal record
                instruments.add(new SF2Instrument(instName.trim(), bagIndex));
            }
        }
    }

    private void parseInstrumentBags(ByteBuffer buffer, int size) {
        int count = size / 4;
        for (int i = 0; i < count; i++) {
            int genIndex = buffer.getShort() & 0xFFFF;
            int modIndex = buffer.getShort() & 0xFFFF;
            instrumentBags.add(new int[]{genIndex, modIndex});
        }
    }

    private void parseInstrumentGenerators(ByteBuffer buffer, int size) {
        int count = size / 4;
        for (int i = 0; i < count; i++) {
            int genOper = buffer.getShort() & 0xFFFF;
            int genAmount = buffer.getShort(); // signed
            instrumentGens.add(new int[]{genOper, genAmount});
        }
    }

    private void parseSampleHeaders(ByteBuffer buffer, int size) {
        // Each shdr record is 46 bytes
        int count = size / 46;
        for (int i = 0; i < count; i++) {
            String sampleName = readString(buffer, 20);
            int start = buffer.getInt();
            int end = buffer.getInt();
            int loopStart = buffer.getInt();
            int loopEnd = buffer.getInt();
            int sampleRate = buffer.getInt();
            int originalPitch = buffer.get() & 0xFF;
            int pitchCorrection = buffer.get(); // signed
            int sampleLink = buffer.getShort() & 0xFFFF;
            int sampleType = buffer.getShort() & 0xFFFF;

            if (i < count - 1) { // Skip terminal record
                samples.add(new SF2Sample(
                        sampleName.trim(), start, end, loopStart, loopEnd,
                        sampleRate, originalPitch, pitchCorrection, sampleType
                ));
            }
        }
    }

    private void buildConnections() {
        // Link preset zones to instruments
        for (int p = 0; p < presets.size(); p++) {
            SF2Preset preset = presets.get(p);
            int startBag = preset.getBagIndex();
            int endBag = (p + 1 < presets.size()) ? presets.get(p + 1).getBagIndex() : presetBags.size() - 1;

            for (int b = startBag; b < endBag; b++) {
                int startGen = presetBags.get(b)[0];
                int endGen = (b + 1 < presetBags.size()) ? presetBags.get(b + 1)[0] : presetGens.size();

                for (int g = startGen; g < endGen; g++) {
                    int[] gen = presetGens.get(g);
                    if (gen[0] == 41) { // instrument generator
                        int instIndex = gen[1] & 0xFFFF;
                        if (instIndex < instruments.size()) {
                            preset.addInstrument(instruments.get(instIndex));
                        }
                    }
                }
            }
        }

        // Link instrument zones to samples with key ranges
        for (int i = 0; i < instruments.size(); i++) {
            SF2Instrument inst = instruments.get(i);
            int startBag = inst.getBagIndex();
            int endBag = (i + 1 < instruments.size()) ? instruments.get(i + 1).getBagIndex() : instrumentBags.size() - 1;

            for (int b = startBag; b < endBag; b++) {
                int startGen = instrumentBags.get(b)[0];
                int endGen = (b + 1 < instrumentBags.size()) ? instrumentBags.get(b + 1)[0] : instrumentGens.size();

                int keyLo = 0, keyHi = 127;
                int velLo = 0, velHi = 127;
                int sampleIndex = -1;
                int rootKey = -1;
                int fineTune = 0;

                for (int g = startGen; g < endGen; g++) {
                    int[] gen = instrumentGens.get(g);
                    switch (gen[0]) {
                        case 43 -> { // keyRange
                            keyLo = gen[1] & 0xFF;
                            keyHi = (gen[1] >> 8) & 0xFF;
                        }
                        case 44 -> { // velRange
                            velLo = gen[1] & 0xFF;
                            velHi = (gen[1] >> 8) & 0xFF;
                        }
                        case 53 -> sampleIndex = gen[1] & 0xFFFF; // sampleID
                        case 58 -> rootKey = gen[1] & 0xFF; // overridingRootKey
                        case 52 -> fineTune = gen[1]; // fineTune (cents)
                    }
                }

                if (sampleIndex >= 0 && sampleIndex < samples.size()) {
                    SF2Sample sample = samples.get(sampleIndex);
                    if (rootKey >= 0) {
                        sample.setRootKey(rootKey);
                    }
                    inst.addZone(new SF2Zone(keyLo, keyHi, velLo, velHi, sample, fineTune));
                }
            }
        }
    }

    // ==================== UTILITY METHODS ====================

    private String readFourCC(ByteBuffer buffer) {
        byte[] bytes = new byte[4];
        buffer.get(bytes);
        return new String(bytes);
    }

    private String readString(ByteBuffer buffer, int maxLen) {
        byte[] bytes = new byte[maxLen];
        buffer.get(bytes);
        int end = 0;
        while (end < bytes.length && bytes[end] != 0) end++;
        return new String(bytes, 0, end);
    }

    // ==================== GETTERS ====================

    public String getName() { return name; }
    public String getAuthor() { return author; }
    public String getCopyright() { return copyright; }
    public String getComment() { return comment; }
    public List<SF2Sample> getSamples() { return Collections.unmodifiableList(samples); }
    public List<SF2Preset> getPresets() { return Collections.unmodifiableList(presets); }
    public List<SF2Instrument> getInstruments() { return Collections.unmodifiableList(instruments); }
    public short[] getSampleData() { return sampleData; }

    /**
     * Find a preset by bank and program number.
     */
    public SF2Preset findPreset(int bank, int program) {
        for (SF2Preset p : presets) {
            if (p.getBank() == bank && p.getPreset() == program) {
                return p;
            }
        }
        return null;
    }

    /**
     * Find the drum preset (bank 128, or first preset with "drum" in name).
     */
    public SF2Preset findDrumPreset() {
        // Standard: bank 128, program 0
        SF2Preset drums = findPreset(128, 0);
        if (drums != null) return drums;

        // Try bank 127 (some soundfonts use this)
        drums = findPreset(127, 0);
        if (drums != null) return drums;

        // Search by name
        for (SF2Preset p : presets) {
            String name = p.getName().toLowerCase();
            if (name.contains("drum") || name.contains("kit") || name.contains("perc")) {
                return p;
            }
        }

        return null;
    }

    /**
     * Extract sample data as float array.
     *
     * @param sample The sample to extract
     * @return Normalized float samples (-1 to +1)
     */
    public float[] extractSampleData(SF2Sample sample) {
        int start = sample.getStart();
        int end = sample.getEnd();
        int length = end - start;

        if (length <= 0 || start < 0 || end > sampleData.length) {
            return new float[0];
        }

        float[] result = new float[length];
        for (int i = 0; i < length; i++) {
            result[i] = sampleData[start + i] / 32768.0f;
        }
        return result;
    }

    /**
     * Extract and resample sample data to target sample rate.
     */
    public float[] extractSampleData(SF2Sample sample, int targetSampleRate) {
        float[] original = extractSampleData(sample);
        if (original.length == 0) return original;

        int sourceSampleRate = sample.getSampleRate();
        if (sourceSampleRate == targetSampleRate) {
            return original;
        }

        // Simple linear interpolation resampling
        double ratio = (double) sourceSampleRate / targetSampleRate;
        int newLength = (int) (original.length / ratio);
        float[] resampled = new float[newLength];

        for (int i = 0; i < newLength; i++) {
            double srcPos = i * ratio;
            int srcIndex = (int) srcPos;
            double frac = srcPos - srcIndex;

            if (srcIndex + 1 < original.length) {
                resampled[i] = (float) (original[srcIndex] * (1 - frac) + original[srcIndex + 1] * frac);
            } else if (srcIndex < original.length) {
                resampled[i] = original[srcIndex];
            }
        }

        return resampled;
    }

    @Override
    public String toString() {
        return "SF2File{" +
                "name='" + name + '\'' +
                ", presets=" + presets.size() +
                ", instruments=" + instruments.size() +
                ", samples=" + samples.size() +
                '}';
    }
}
