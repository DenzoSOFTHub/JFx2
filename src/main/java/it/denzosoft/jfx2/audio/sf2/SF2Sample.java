package it.denzosoft.jfx2.audio.sf2;

/**
 * Represents a sample in an SF2 soundfont.
 */
public class SF2Sample {

    private final String name;
    private final int start;      // Start offset in sample data
    private final int end;        // End offset in sample data
    private final int loopStart;  // Loop start offset
    private final int loopEnd;    // Loop end offset
    private final int sampleRate; // Sample rate in Hz
    private int originalPitch;    // MIDI note number of original pitch (can be overridden)
    private final int pitchCorrection; // Fine tuning in cents
    private final int sampleType; // 1=mono, 2=right, 4=left, 8=linked

    public SF2Sample(String name, int start, int end, int loopStart, int loopEnd,
                     int sampleRate, int originalPitch, int pitchCorrection, int sampleType) {
        this.name = name;
        this.start = start;
        this.end = end;
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
        this.sampleRate = sampleRate;
        this.originalPitch = originalPitch;
        this.pitchCorrection = pitchCorrection;
        this.sampleType = sampleType;
    }

    public String getName() { return name; }
    public int getStart() { return start; }
    public int getEnd() { return end; }
    public int getLoopStart() { return loopStart; }
    public int getLoopEnd() { return loopEnd; }
    public int getSampleRate() { return sampleRate; }
    public int getOriginalPitch() { return originalPitch; }
    public int getPitchCorrection() { return pitchCorrection; }
    public int getSampleType() { return sampleType; }

    /**
     * Get the length of the sample in frames.
     */
    public int getLength() {
        return end - start;
    }

    /**
     * Check if this sample has loop points defined.
     */
    public boolean hasLoop() {
        return loopEnd > loopStart && loopStart >= start && loopEnd <= end;
    }

    /**
     * Set the root key (overrides originalPitch).
     */
    public void setRootKey(int rootKey) {
        this.originalPitch = rootKey;
    }

    @Override
    public String toString() {
        return "SF2Sample{" +
                "name='" + name + '\'' +
                ", length=" + getLength() +
                ", rate=" + sampleRate +
                ", pitch=" + originalPitch +
                '}';
    }
}
