package it.denzosoft.jfx2.audio.sf2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an instrument in an SF2 soundfont.
 *
 * <p>An instrument contains multiple zones, each mapping a key/velocity
 * range to a sample.</p>
 */
public class SF2Instrument {

    private final String name;
    private final int bagIndex;  // Index into instrument bags

    private final List<SF2Zone> zones = new ArrayList<>();

    public SF2Instrument(String name, int bagIndex) {
        this.name = name;
        this.bagIndex = bagIndex;
    }

    public String getName() { return name; }
    public int getBagIndex() { return bagIndex; }

    public void addZone(SF2Zone zone) {
        zones.add(zone);
    }

    public List<SF2Zone> getZones() {
        return Collections.unmodifiableList(zones);
    }

    /**
     * Find the sample for a given MIDI note and velocity.
     */
    public SF2Sample findSample(int note, int velocity) {
        SF2Zone zone = findZone(note, velocity);
        return zone != null ? zone.getSample() : null;
    }

    /**
     * Find the zone for a given MIDI note and velocity.
     */
    public SF2Zone findZone(int note, int velocity) {
        for (SF2Zone zone : zones) {
            if (zone.matches(note, velocity)) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "SF2Instrument{" +
                "name='" + name + '\'' +
                ", zones=" + zones.size() +
                '}';
    }
}
