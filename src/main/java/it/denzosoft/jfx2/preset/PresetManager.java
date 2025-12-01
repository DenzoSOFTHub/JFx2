package it.denzosoft.jfx2.preset;

import it.denzosoft.jfx2.effects.AudioEffect;
import it.denzosoft.jfx2.effects.Parameter;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Manages individual effect presets: saving, loading, and applying.
 *
 * <p>Presets are stored in the user's home directory under ~/.jfx2/presets/
 * organized by effect type. Each effect type has its own subdirectory.</p>
 *
 * <p>This is different from templates (TemplateManager) which store complete
 * rig configurations. This handles saving and restoring parameter configurations
 * for individual effect blocks.</p>
 */
public class PresetManager {

    private static final PresetManager INSTANCE = new PresetManager();

    private static final String PRESETS_DIR = ".jfx2/presets";
    private static final String PRESET_EXTENSION = ".jfxpreset";

    private final Path presetsRoot;
    private final Map<String, List<Preset>> presetCache;

    private PresetManager() {
        this.presetsRoot = Paths.get(System.getProperty("user.home"), PRESETS_DIR);
        this.presetCache = new HashMap<>();
        ensureDirectoryExists(presetsRoot);
    }

    /**
     * Get the singleton instance.
     */
    public static PresetManager getInstance() {
        return INSTANCE;
    }

    /**
     * Save the current state of an effect as a preset.
     *
     * @param effect The effect to save
     * @param presetName Name for the preset
     * @return The created preset
     * @throws IOException if save fails
     */
    public Preset savePreset(AudioEffect effect, String presetName) throws IOException {
        String effectId = effect.getMetadata().id();

        // Collect current parameter values
        Map<String, Float> values = new LinkedHashMap<>();
        for (Parameter param : effect.getParameters()) {
            values.put(param.getId(), param.getValue());
        }

        // Create preset
        Preset preset = new Preset(presetName, effectId, values);

        // Save to file
        Path effectDir = presetsRoot.resolve(effectId);
        ensureDirectoryExists(effectDir);

        Path presetFile = effectDir.resolve(sanitizeFileName(presetName) + PRESET_EXTENSION);
        writePresetFile(presetFile, preset);

        // Update cache
        invalidateCache(effectId);

        return preset;
    }

    /**
     * Save a preset with additional metadata.
     */
    public Preset savePreset(AudioEffect effect, String presetName,
                              String description, String author) throws IOException {
        Preset preset = savePreset(effect, presetName);
        preset.setDescription(description);
        preset.setAuthor(author);

        // Re-save with metadata
        String effectId = effect.getMetadata().id();
        Path effectDir = presetsRoot.resolve(effectId);
        Path presetFile = effectDir.resolve(sanitizeFileName(presetName) + PRESET_EXTENSION);
        writePresetFile(presetFile, preset);

        return preset;
    }

    /**
     * Apply a preset to an effect.
     *
     * @param preset The preset to apply
     * @param effect The effect to configure
     */
    public void applyPreset(Preset preset, AudioEffect effect) {
        // Verify effect type matches
        if (!preset.getEffectId().equals(effect.getMetadata().id())) {
            throw new IllegalArgumentException(
                    "Preset is for effect '" + preset.getEffectId() +
                            "' but trying to apply to '" + effect.getMetadata().id() + "'");
        }

        // Apply each parameter value
        Map<String, Float> values = preset.getParameterValues();
        for (Parameter param : effect.getParameters()) {
            Float value = values.get(param.getId());
            if (value != null) {
                param.setValue(value);
            }
        }
    }

    /**
     * Get all presets for a specific effect type.
     *
     * @param effectId Effect type identifier
     * @return List of presets, sorted by name
     */
    public List<Preset> getPresetsForEffect(String effectId) {
        // Check cache
        if (presetCache.containsKey(effectId)) {
            return new ArrayList<>(presetCache.get(effectId));
        }

        // Load from disk
        List<Preset> presets = loadPresetsFromDisk(effectId);

        // Sort by name
        presets.sort(Comparator.comparing(Preset::getName, String.CASE_INSENSITIVE_ORDER));

        // Cache
        presetCache.put(effectId, presets);

        return new ArrayList<>(presets);
    }

    /**
     * Get all presets for an effect instance.
     */
    public List<Preset> getPresetsForEffect(AudioEffect effect) {
        return getPresetsForEffect(effect.getMetadata().id());
    }

    /**
     * Delete a preset.
     *
     * @param preset The preset to delete
     * @return true if deleted successfully
     */
    public boolean deletePreset(Preset preset) {
        Path presetFile = getPresetFile(preset);
        try {
            boolean deleted = Files.deleteIfExists(presetFile);
            if (deleted) {
                invalidateCache(preset.getEffectId());
            }
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Rename a preset.
     *
     * @param preset The preset to rename
     * @param newName New name for the preset
     * @return The renamed preset
     * @throws IOException if rename fails
     */
    public Preset renamePreset(Preset preset, String newName) throws IOException {
        // Create new preset with new name
        Preset newPreset = new Preset(newName, preset.getEffectId(),
                preset.getParameterValues());
        newPreset.setDescription(preset.getDescription());
        newPreset.setAuthor(preset.getAuthor());
        newPreset.setCreatedTime(preset.getCreatedTime());

        // Save new file
        Path effectDir = presetsRoot.resolve(preset.getEffectId());
        Path newFile = effectDir.resolve(sanitizeFileName(newName) + PRESET_EXTENSION);
        writePresetFile(newFile, newPreset);

        // Delete old file
        deletePreset(preset);

        invalidateCache(preset.getEffectId());

        return newPreset;
    }

    /**
     * Check if a preset name already exists for an effect.
     */
    public boolean presetExists(String effectId, String presetName) {
        Path effectDir = presetsRoot.resolve(effectId);
        Path presetFile = effectDir.resolve(sanitizeFileName(presetName) + PRESET_EXTENSION);
        return Files.exists(presetFile);
    }

    /**
     * Get the presets directory path.
     */
    public Path getPresetsRoot() {
        return presetsRoot;
    }

    /**
     * Invalidate the cache for an effect type.
     */
    public void invalidateCache(String effectId) {
        presetCache.remove(effectId);
    }

    /**
     * Clear all cached presets.
     */
    public void clearCache() {
        presetCache.clear();
    }

    // --- Private methods ---

    private List<Preset> loadPresetsFromDisk(String effectId) {
        List<Preset> presets = new ArrayList<>();
        Path effectDir = presetsRoot.resolve(effectId);

        if (!Files.exists(effectDir)) {
            return presets;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(effectDir, "*" + PRESET_EXTENSION)) {
            for (Path file : stream) {
                try {
                    Preset preset = readPresetFile(file, effectId);
                    if (preset != null) {
                        presets.add(preset);
                    }
                } catch (IOException e) {
                    // Skip invalid preset files
                    System.err.println("Warning: Could not load preset: " + file + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Could not read presets directory: " + effectDir);
        }

        return presets;
    }

    private void writePresetFile(Path file, Preset preset) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            // Header
            writer.write("# JFx2 Effect Preset");
            writer.newLine();
            writer.write("# Effect: " + preset.getEffectId());
            writer.newLine();
            writer.newLine();

            // Metadata
            writer.write("name=" + escapeValue(preset.getName()));
            writer.newLine();
            writer.write("effectId=" + preset.getEffectId());
            writer.newLine();
            writer.write("created=" + preset.getCreatedTime());
            writer.newLine();
            writer.write("modified=" + preset.getModifiedTime());
            writer.newLine();

            if (preset.getDescription() != null && !preset.getDescription().isEmpty()) {
                writer.write("description=" + escapeValue(preset.getDescription()));
                writer.newLine();
            }
            if (preset.getAuthor() != null && !preset.getAuthor().isEmpty()) {
                writer.write("author=" + escapeValue(preset.getAuthor()));
                writer.newLine();
            }

            writer.newLine();
            writer.write("# Parameters");
            writer.newLine();

            // Parameter values
            for (Map.Entry<String, Float> entry : preset.getParameterValues().entrySet()) {
                writer.write("param." + entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
        }
    }

    private Preset readPresetFile(Path file, String expectedEffectId) throws IOException {
        String name = null;
        String effectId = null;
        String description = null;
        String author = null;
        long created = 0;
        long modified = 0;
        Map<String, Float> params = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int eq = line.indexOf('=');
                if (eq < 0) continue;

                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                switch (key) {
                    case "name":
                        name = unescapeValue(value);
                        break;
                    case "effectId":
                        effectId = value;
                        break;
                    case "description":
                        description = unescapeValue(value);
                        break;
                    case "author":
                        author = unescapeValue(value);
                        break;
                    case "created":
                        try {
                            created = Long.parseLong(value);
                        } catch (NumberFormatException ignored) {}
                        break;
                    case "modified":
                        try {
                            modified = Long.parseLong(value);
                        } catch (NumberFormatException ignored) {}
                        break;
                    default:
                        if (key.startsWith("param.")) {
                            String paramId = key.substring(6);
                            try {
                                float paramValue = Float.parseFloat(value);
                                params.put(paramId, paramValue);
                            } catch (NumberFormatException ignored) {}
                        }
                        break;
                }
            }
        }

        // Validate
        if (name == null || name.isEmpty()) {
            // Use filename as name
            String fileName = file.getFileName().toString();
            name = fileName.substring(0, fileName.length() - PRESET_EXTENSION.length());
        }

        if (effectId == null || effectId.isEmpty()) {
            effectId = expectedEffectId;
        }

        // Create preset
        Preset preset = new Preset(name, effectId, params);
        preset.setDescription(description);
        preset.setAuthor(author);
        if (created > 0) preset.setCreatedTime(created);
        if (modified > 0) preset.setModifiedTime(modified);

        return preset;
    }

    private Path getPresetFile(Preset preset) {
        Path effectDir = presetsRoot.resolve(preset.getEffectId());
        return effectDir.resolve(sanitizeFileName(preset.getName()) + PRESET_EXTENSION);
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            System.err.println("Warning: Could not create directory: " + dir);
        }
    }

    private String sanitizeFileName(String name) {
        // Remove or replace invalid filename characters
        return name.replaceAll("[\\\\/:*?\"<>|]", "_")
                   .replaceAll("\\s+", "_")
                   .trim();
    }

    private String escapeValue(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    private String unescapeValue(String value) {
        if (value == null) return "";
        return value.replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\\\", "\\");
    }
}
