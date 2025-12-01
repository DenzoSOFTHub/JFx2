package it.denzosoft.jfx2.effects.impl.drums;

import it.denzosoft.jfx2.audio.SoundfontSettings;
import it.denzosoft.jfx2.audio.sf2.*;
import it.denzosoft.jfx2.effects.impl.drums.DrumSounds.DrumSound;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Drum sounds loaded from an SF2 soundfont file.
 *
 * <p>Uses our custom SF2 parser - no dependency on internal Java APIs.</p>
 */
public class SF2DrumSounds implements DrumSoundProvider {

    private final int sampleRate;
    private final Map<DrumSound, float[]> sounds = new HashMap<>();
    private String soundfontName = "SF2";

    // General MIDI drum note numbers
    private static final Map<DrumSound, Integer> NOTE_MAP = Map.ofEntries(
            Map.entry(DrumSound.KICK, 36),           // Bass Drum 1
            Map.entry(DrumSound.SNARE, 38),          // Acoustic Snare
            Map.entry(DrumSound.HIHAT_CLOSED, 42),   // Closed Hi-Hat
            Map.entry(DrumSound.HIHAT_OPEN, 46),     // Open Hi-Hat
            Map.entry(DrumSound.CRASH, 49),          // Crash Cymbal 1
            Map.entry(DrumSound.RIDE, 51),           // Ride Cymbal 1
            Map.entry(DrumSound.RIDE_BELL, 53),      // Ride Bell
            Map.entry(DrumSound.TOM_HIGH, 50),       // High Tom
            Map.entry(DrumSound.TOM_MID, 47),        // Low-Mid Tom
            Map.entry(DrumSound.TOM_LOW, 45),        // Low Tom
            Map.entry(DrumSound.RIMSHOT, 37),        // Side Stick
            Map.entry(DrumSound.CLAP, 39),           // Hand Clap
            Map.entry(DrumSound.COWBELL, 56),        // Cowbell
            Map.entry(DrumSound.STICKS, 31)          // Sticks (metronome click)
    );

    public SF2DrumSounds(int sampleRate) {
        this.sampleRate = sampleRate;
        loadSounds();
    }

    private void loadSounds() {
        // Initialize with empty sounds
        for (DrumSound sound : DrumSound.values()) {
            sounds.put(sound, new float[0]);
        }

        // Try to load SF2 file
        SF2File sf2 = loadSF2File();
        if (sf2 == null) {
            return;
        }

        soundfontName = sf2.getName();

        // Find drum preset
        SF2Preset drumPreset = sf2.findDrumPreset();
        if (drumPreset == null) {
            return;
        }

        // Extract samples for each drum sound
        for (Map.Entry<DrumSound, Integer> entry : NOTE_MAP.entrySet()) {
            DrumSound drumSound = entry.getKey();
            int midiNote = entry.getValue();

            SF2Zone zone = drumPreset.findZone(midiNote, 100);
            if (zone != null && zone.getSample() != null) {
                float[] samples = sf2.extractSampleData(zone.getSample(), sampleRate);
                if (samples.length > 0) {
                    sounds.put(drumSound, normalize(samples));
                }
            }
        }
    }

    private SF2File loadSF2File() {
        // First try user-configured SF2
        SoundfontSettings settings = SoundfontSettings.getInstance();
        if (settings.isUseExternalSoundfont()) {
            File sf2File = settings.getSf2File();
            if (sf2File != null && sf2File.exists()) {
                try {
                    return SF2File.load(sf2File);
                } catch (Exception e) {
                    // Fall through to defaults
                }
            }
        }

        // Try to find system soundfont
        SF2File sf2 = tryLoadSystemSoundfont();
        if (sf2 != null) {
            return sf2;
        }

        return null;
    }

    private SF2File tryLoadSystemSoundfont() {
        // Common soundfont locations
        String[] paths = {
                // Linux
                "/usr/share/sounds/sf2/FluidR3_GM.sf2",
                "/usr/share/sounds/sf2/default.sf2",
                "/usr/share/soundfonts/FluidR3_GM.sf2",
                "/usr/share/soundfonts/default-GM.sf2",
                // macOS
                "/Library/Audio/Sounds/Banks/FluidR3_GM.sf2",
                // Windows - relative to Java home
                System.getProperty("java.home") + "/lib/audio/soundbank.gm",
                // User home
                System.getProperty("user.home") + "/.local/share/sounds/sf2/default.sf2",
        };

        for (String path : paths) {
            try {
                Path p = Path.of(path);
                if (Files.exists(p) && Files.isReadable(p)) {
                    return SF2File.load(p);
                }
            } catch (Exception ignored) {
                // Try next path
            }
        }

        // Try to load embedded resource (if we bundle one)
        try {
            InputStream is = getClass().getResourceAsStream("/soundfonts/drums.sf2");
            if (is != null) {
                SF2File sf2 = SF2File.load(is);
                is.close();
                return sf2;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private float[] normalize(float[] samples) {
        float max = 0;
        for (float s : samples) {
            max = Math.max(max, Math.abs(s));
        }
        if (max > 0.001f) {
            float gain = 0.92f / max;
            for (int i = 0; i < samples.length; i++) {
                samples[i] *= gain;
            }
        }
        return samples;
    }

    // ==================== INTERFACE METHODS ====================

    @Override
    public float[] getSound(DrumSound sound) {
        return sounds.getOrDefault(sound, new float[0]);
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public String getName() {
        return "SF2: " + soundfontName;
    }

    /**
     * Check if SF2 loading is available.
     * Always returns true since we use our own parser.
     */
    public static boolean isAvailable() {
        // Check if we can find any SF2 file
        SoundfontSettings settings = SoundfontSettings.getInstance();
        if (settings.isUseExternalSoundfont() && settings.isValidSf2File()) {
            return true;
        }

        // Check system paths
        String[] paths = {
                "/usr/share/sounds/sf2/FluidR3_GM.sf2",
                "/usr/share/sounds/sf2/default.sf2",
                "/usr/share/soundfonts/FluidR3_GM.sf2",
                System.getProperty("java.home") + "/lib/audio/soundbank.gm",
        };

        for (String path : paths) {
            try {
                if (Files.exists(Path.of(path))) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }

        return false;
    }
}
