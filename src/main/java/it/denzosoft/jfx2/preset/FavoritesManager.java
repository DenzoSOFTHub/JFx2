package it.denzosoft.jfx2.preset;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Manages preset favorites and ratings.
 * Stores data separately from preset files to allow rating factory presets.
 */
public class FavoritesManager {

    private static final String FAVORITES_FILE = "favorites.json";

    private final Path dataFile;
    private final Map<String, PresetData> presetDataMap = new HashMap<>();

    /**
     * Data for a single preset.
     */
    public record PresetData(
            boolean favorite,
            int rating,  // 0-5 stars
            int useCount,
            String lastUsed  // ISO timestamp
    ) {
        public static PresetData DEFAULT = new PresetData(false, 0, 0, null);

        public PresetData withFavorite(boolean favorite) {
            return new PresetData(favorite, rating, useCount, lastUsed);
        }

        public PresetData withRating(int rating) {
            return new PresetData(favorite, rating, useCount, lastUsed);
        }

        public PresetData withUsed() {
            return new PresetData(favorite, rating, useCount + 1,
                    java.time.Instant.now().toString());
        }
    }

    public FavoritesManager() {
        this(Path.of("presets"));
    }

    public FavoritesManager(Path presetsDir) {
        this.dataFile = presetsDir.resolve(FAVORITES_FILE);
        load();
    }

    // ==================== FAVORITES ====================

    /**
     * Check if a preset is a favorite.
     */
    public boolean isFavorite(String presetId) {
        PresetData data = presetDataMap.get(presetId);
        return data != null && data.favorite();
    }

    /**
     * Toggle favorite status.
     */
    public boolean toggleFavorite(String presetId) {
        PresetData data = presetDataMap.getOrDefault(presetId, PresetData.DEFAULT);
        data = data.withFavorite(!data.favorite());
        presetDataMap.put(presetId, data);
        save();
        return data.favorite();
    }

    /**
     * Set favorite status.
     */
    public void setFavorite(String presetId, boolean favorite) {
        PresetData data = presetDataMap.getOrDefault(presetId, PresetData.DEFAULT);
        data = data.withFavorite(favorite);
        presetDataMap.put(presetId, data);
        save();
    }

    /**
     * Add a preset to favorites.
     */
    public void addFavorite(String presetId) {
        setFavorite(presetId, true);
    }

    /**
     * Remove a preset from favorites.
     */
    public void removeFavorite(String presetId) {
        setFavorite(presetId, false);
    }

    /**
     * Get all favorite preset IDs.
     */
    public List<String> getFavorites() {
        List<String> favorites = new ArrayList<>();
        for (Map.Entry<String, PresetData> entry : presetDataMap.entrySet()) {
            if (entry.getValue().favorite()) {
                favorites.add(entry.getKey());
            }
        }
        return favorites;
    }

    // ==================== RATINGS ====================

    /**
     * Get the rating for a preset.
     */
    public int getRating(String presetId) {
        PresetData data = presetDataMap.get(presetId);
        return data != null ? data.rating() : 0;
    }

    /**
     * Set the rating for a preset.
     */
    public void setRating(String presetId, int rating) {
        rating = Math.max(0, Math.min(5, rating));
        PresetData data = presetDataMap.getOrDefault(presetId, PresetData.DEFAULT);
        data = data.withRating(rating);
        presetDataMap.put(presetId, data);
        save();
    }

    // ==================== USAGE TRACKING ====================

    /**
     * Mark a preset as used.
     */
    public void markUsed(String presetId) {
        PresetData data = presetDataMap.getOrDefault(presetId, PresetData.DEFAULT);
        data = data.withUsed();
        presetDataMap.put(presetId, data);
        save();
    }

    /**
     * Mark a preset as recently used (alias for markUsed).
     */
    public void markRecentlyUsed(String presetId) {
        markUsed(presetId);
    }

    /**
     * Check if a preset was recently used (within last 20 entries).
     */
    public boolean isRecentlyUsed(String presetId) {
        return getRecentlyUsed(20).contains(presetId);
    }

    /**
     * Get usage count for a preset.
     */
    public int getUseCount(String presetId) {
        PresetData data = presetDataMap.get(presetId);
        return data != null ? data.useCount() : 0;
    }

    /**
     * Get most used presets.
     */
    public List<String> getMostUsed(int limit) {
        return presetDataMap.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().useCount(), a.getValue().useCount()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /**
     * Get recently used presets.
     */
    public List<String> getRecentlyUsed(int limit) {
        return presetDataMap.entrySet().stream()
                .filter(e -> e.getValue().lastUsed() != null)
                .sorted((a, b) -> b.getValue().lastUsed().compareTo(a.getValue().lastUsed()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    // ==================== PERSISTENCE ====================

    /**
     * Load favorites from file.
     */
    private void load() {
        if (!Files.exists(dataFile)) {
            return;
        }

        try {
            String json = Files.readString(dataFile, StandardCharsets.UTF_8);
            parseJson(json);
        } catch (IOException e) {
            System.err.println("Could not load favorites: " + e.getMessage());
        }
    }

    /**
     * Save favorites to file.
     */
    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            String json = toJson();
            Files.writeString(dataFile, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Could not save favorites: " + e.getMessage());
        }
    }

    /**
     * Parse JSON data.
     */
    private void parseJson(String json) {
        // Simple JSON parsing (no external libraries)
        presetDataMap.clear();

        // Basic parsing for structure: {"presets": {"id": {"favorite": true, "rating": 5, ...}}}
        int presetsStart = json.indexOf("\"presets\"");
        if (presetsStart < 0) return;

        int objectStart = json.indexOf("{", presetsStart + 10);
        if (objectStart < 0) return;

        // Find matching closing brace
        int depth = 1;
        int i = objectStart + 1;
        while (i < json.length() && depth > 0) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            i++;
        }

        if (depth != 0) return;

        String presetsContent = json.substring(objectStart + 1, i - 1);

        // Parse each preset entry
        int pos = 0;
        while (pos < presetsContent.length()) {
            // Find key
            int keyStart = presetsContent.indexOf("\"", pos);
            if (keyStart < 0) break;
            int keyEnd = presetsContent.indexOf("\"", keyStart + 1);
            if (keyEnd < 0) break;

            String presetId = presetsContent.substring(keyStart + 1, keyEnd);

            // Find value object
            int valueStart = presetsContent.indexOf("{", keyEnd);
            if (valueStart < 0) break;

            // Find matching closing brace
            depth = 1;
            int valueEnd = valueStart + 1;
            while (valueEnd < presetsContent.length() && depth > 0) {
                char c = presetsContent.charAt(valueEnd);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                valueEnd++;
            }

            String valueJson = presetsContent.substring(valueStart, valueEnd);

            // Parse preset data
            boolean favorite = valueJson.contains("\"favorite\":true") || valueJson.contains("\"favorite\": true");
            int rating = parseIntValue(valueJson, "rating");
            int useCount = parseIntValue(valueJson, "useCount");
            String lastUsed = parseStringValue(valueJson, "lastUsed");

            presetDataMap.put(presetId, new PresetData(favorite, rating, useCount, lastUsed));

            pos = valueEnd;
        }
    }

    private int parseIntValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return 0;

        idx += pattern.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;

        StringBuilder sb = new StringBuilder();
        while (idx < json.length() && Character.isDigit(json.charAt(idx))) {
            sb.append(json.charAt(idx));
            idx++;
        }

        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String parseStringValue(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx < 0) return null;

        idx += pattern.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;

        if (idx >= json.length() || json.charAt(idx) != '"') return null;

        int start = idx + 1;
        int end = json.indexOf("\"", start);
        if (end < 0) return null;

        return json.substring(start, end);
    }

    /**
     * Convert to JSON.
     */
    private String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"presets\": {\n");

        boolean first = true;
        for (Map.Entry<String, PresetData> entry : presetDataMap.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;

            PresetData data = entry.getValue();
            sb.append("    \"").append(escapeJson(entry.getKey())).append("\": {");
            sb.append("\"favorite\":").append(data.favorite());
            sb.append(",\"rating\":").append(data.rating());
            sb.append(",\"useCount\":").append(data.useCount());
            if (data.lastUsed() != null) {
                sb.append(",\"lastUsed\":\"").append(escapeJson(data.lastUsed())).append("\"");
            }
            sb.append("}");
        }

        sb.append("\n  }\n}");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Get preset data.
     */
    public PresetData getData(String presetId) {
        return presetDataMap.getOrDefault(presetId, PresetData.DEFAULT);
    }
}
