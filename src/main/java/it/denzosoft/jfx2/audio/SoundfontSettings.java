package it.denzosoft.jfx2.audio;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * Settings for the MIDI soundfont configuration.
 *
 * <p>Supports either using the default Java wavetable synthesizer
 * or loading an external SF2 soundfont file.</p>
 */
public class SoundfontSettings {

    private static final String PREF_USE_EXTERNAL = "soundfont.useExternal";
    private static final String PREF_SF2_PATH = "soundfont.sf2Path";

    private static final Preferences prefs = Preferences.userNodeForPackage(SoundfontSettings.class);

    // Singleton instance
    private static SoundfontSettings instance;

    private boolean useExternalSoundfont;
    private String sf2FilePath;

    private SoundfontSettings() {
        load();
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized SoundfontSettings getInstance() {
        if (instance == null) {
            instance = new SoundfontSettings();
        }
        return instance;
    }

    /**
     * Load settings from preferences.
     */
    public void load() {
        useExternalSoundfont = prefs.getBoolean(PREF_USE_EXTERNAL, false);
        sf2FilePath = prefs.get(PREF_SF2_PATH, "");
    }

    /**
     * Save settings to preferences.
     */
    public void save() {
        prefs.putBoolean(PREF_USE_EXTERNAL, useExternalSoundfont);
        prefs.put(PREF_SF2_PATH, sf2FilePath != null ? sf2FilePath : "");

        try {
            prefs.flush();
        } catch (Exception e) {
            System.err.println("[SoundfontSettings] Failed to save preferences: " + e.getMessage());
        }
    }

    /**
     * Check if an external soundfont should be used.
     */
    public boolean isUseExternalSoundfont() {
        return useExternalSoundfont;
    }

    /**
     * Set whether to use an external soundfont.
     */
    public void setUseExternalSoundfont(boolean useExternal) {
        this.useExternalSoundfont = useExternal;
    }

    /**
     * Get the path to the SF2 file.
     */
    public String getSf2FilePath() {
        return sf2FilePath;
    }

    /**
     * Set the path to the SF2 file.
     */
    public void setSf2FilePath(String path) {
        this.sf2FilePath = path;
    }

    /**
     * Check if the configured SF2 file exists and is valid.
     */
    public boolean isValidSf2File() {
        if (sf2FilePath == null || sf2FilePath.isEmpty()) {
            return false;
        }
        File file = new File(sf2FilePath);
        return file.exists() && file.isFile() && file.canRead() &&
               (sf2FilePath.toLowerCase().endsWith(".sf2") ||
                sf2FilePath.toLowerCase().endsWith(".dls"));
    }

    /**
     * Get the SF2 file, or null if not configured or invalid.
     */
    public File getSf2File() {
        if (isValidSf2File()) {
            return new File(sf2FilePath);
        }
        return null;
    }

    /**
     * Get a description of the current configuration.
     */
    public String getDescription() {
        if (useExternalSoundfont && isValidSf2File()) {
            File file = new File(sf2FilePath);
            return "External: " + file.getName();
        } else {
            return "Default Java Wavetable";
        }
    }

    @Override
    public String toString() {
        return "SoundfontSettings{" +
                "useExternal=" + useExternalSoundfont +
                ", sf2Path='" + sf2FilePath + '\'' +
                ", valid=" + isValidSf2File() +
                '}';
    }
}
