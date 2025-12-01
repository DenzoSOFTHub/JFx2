package it.denzosoft.jfx2.audio.sf2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a preset in an SF2 soundfont.
 *
 * <p>A preset is what the user selects (like "Piano", "Strings", "Drums").
 * Each preset contains one or more instruments.</p>
 */
public class SF2Preset {

    private final String name;
    private final int preset;    // Program number (0-127)
    private final int bank;      // Bank number (0-127, 128 for drums)
    private final int bagIndex;  // Index into preset bags

    private final List<SF2Instrument> instruments = new ArrayList<>();

    public SF2Preset(String name, int preset, int bank, int bagIndex) {
        this.name = name;
        this.preset = preset;
        this.bank = bank;
        this.bagIndex = bagIndex;
    }

    public String getName() { return name; }
    public int getPreset() { return preset; }
    public int getBank() { return bank; }
    public int getBagIndex() { return bagIndex; }

    public void addInstrument(SF2Instrument instrument) {
        instruments.add(instrument);
    }

    public List<SF2Instrument> getInstruments() {
        return Collections.unmodifiableList(instruments);
    }

    /**
     * Check if this is a drum preset.
     */
    public boolean isDrumPreset() {
        return bank == 128 || bank == 127;
    }

    /**
     * Find the sample for a given MIDI note and velocity.
     *
     * @param note MIDI note number (0-127)
     * @param velocity MIDI velocity (0-127)
     * @return The matching sample, or null if not found
     */
    public SF2Sample findSample(int note, int velocity) {
        for (SF2Instrument inst : instruments) {
            SF2Sample sample = inst.findSample(note, velocity);
            if (sample != null) {
                return sample;
            }
        }
        return null;
    }

    /**
     * Find the zone for a given MIDI note and velocity.
     */
    public SF2Zone findZone(int note, int velocity) {
        for (SF2Instrument inst : instruments) {
            SF2Zone zone = inst.findZone(note, velocity);
            if (zone != null) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SF2Preset{" +
                "name='" + name + '\'' +
                ", bank=" + bank +
                ", preset=" + preset +
                ", instruments=" + instruments.size() +
                '}';
    }
}
