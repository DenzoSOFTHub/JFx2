package it.denzosoft.jfx2.audio.sf2;

/**
 * Represents a zone in an SF2 instrument.
 *
 * <p>A zone maps a key range and velocity range to a sample,
 * with optional tuning parameters.</p>
 */
public class SF2Zone {

    private final int keyLo;      // Low key (0-127)
    private final int keyHi;      // High key (0-127)
    private final int velLo;      // Low velocity (0-127)
    private final int velHi;      // High velocity (0-127)
    private final SF2Sample sample;
    private final int fineTune;   // Fine tuning in cents

    public SF2Zone(int keyLo, int keyHi, int velLo, int velHi, SF2Sample sample, int fineTune) {
        this.keyLo = keyLo;
        this.keyHi = keyHi;
        this.velLo = velLo;
        this.velHi = velHi;
        this.sample = sample;
        this.fineTune = fineTune;
    }

    public int getKeyLo() { return keyLo; }
    public int getKeyHi() { return keyHi; }
    public int getVelLo() { return velLo; }
    public int getVelHi() { return velHi; }
    public SF2Sample getSample() { return sample; }
    public int getFineTune() { return fineTune; }

    /**
     * Check if this zone matches the given note and velocity.
     */
    public boolean matches(int note, int velocity) {
        return note >= keyLo && note <= keyHi &&
               velocity >= velLo && velocity <= velHi;
    }

    /**
     * Check if this zone contains the given note (any velocity).
     */
    public boolean containsKey(int note) {
        return note >= keyLo && note <= keyHi;
    }

    @Override
    public String toString() {
        return "SF2Zone{" +
                "keys=" + keyLo + "-" + keyHi +
                ", vel=" + velLo + "-" + velHi +
                ", sample=" + (sample != null ? sample.getName() : "null") +
                '}';
    }
}
