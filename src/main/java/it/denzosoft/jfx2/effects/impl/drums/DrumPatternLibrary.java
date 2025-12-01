package it.denzosoft.jfx2.effects.impl.drums;

import it.denzosoft.jfx2.effects.impl.drums.DrumPattern.DrumHit;
import it.denzosoft.jfx2.effects.impl.drums.DrumSounds.DrumSound;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages drum pattern libraries in JSON format.
 *
 * <p>Supports loading and saving drum patterns from/to JSON files,
 * enabling users to create custom pattern libraries.</p>
 */
public class DrumPatternLibrary {

    private String name;
    private String version;
    private String author;
    private String description;
    private final List<DrumPattern> patterns;
    private final Map<String, List<DrumPattern>> patternsByCategory;

    public DrumPatternLibrary() {
        this.name = "Default Library";
        this.version = "1.0";
        this.author = "JFx2";
        this.description = "Built-in drum patterns";
        this.patterns = new ArrayList<>();
        this.patternsByCategory = new LinkedHashMap<>();
    }

    public DrumPatternLibrary(String name, String version, String author, String description) {
        this();
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
    }

    /**
     * Add a pattern to the library.
     */
    public void addPattern(DrumPattern pattern) {
        patterns.add(pattern);
        patternsByCategory.computeIfAbsent(pattern.getStyle(), k -> new ArrayList<>()).add(pattern);
    }

    /**
     * Get all patterns in the library.
     */
    public List<DrumPattern> getPatterns() {
        return Collections.unmodifiableList(patterns);
    }

    /**
     * Get patterns by category/style.
     */
    public List<DrumPattern> getPatternsByCategory(String category) {
        return patternsByCategory.getOrDefault(category, Collections.emptyList());
    }

    /**
     * Get all category names.
     */
    public Set<String> getCategories() {
        return Collections.unmodifiableSet(patternsByCategory.keySet());
    }

    /**
     * Get pattern by index.
     */
    public DrumPattern getPattern(int index) {
        if (index >= 0 && index < patterns.size()) {
            return patterns.get(index);
        }
        return null;
    }

    /**
     * Get pattern by name.
     */
    public DrumPattern getPatternByName(String name) {
        for (DrumPattern p : patterns) {
            if (p.getName().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Get number of patterns.
     */
    public int getPatternCount() {
        return patterns.size();
    }

    // Getters
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getAuthor() { return author; }
    public String getDescription() { return description; }

    // ==================== JSON SERIALIZATION ====================

    /**
     * Save library to JSON file.
     */
    public void saveToFile(Path path) throws IOException {
        String json = toJson();
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    /**
     * Load library from JSON file.
     */
    public static DrumPatternLibrary loadFromFile(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        return fromJson(json);
    }

    /**
     * Load library from input stream.
     */
    public static DrumPatternLibrary loadFromStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return fromJson(sb.toString());
        }
    }

    /**
     * Convert library to JSON string.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"name\": \"").append(escapeJson(name)).append("\",\n");
        sb.append("  \"version\": \"").append(escapeJson(version)).append("\",\n");
        sb.append("  \"author\": \"").append(escapeJson(author)).append("\",\n");
        sb.append("  \"description\": \"").append(escapeJson(description)).append("\",\n");
        sb.append("  \"patterns\": [\n");

        for (int i = 0; i < patterns.size(); i++) {
            DrumPattern p = patterns.get(i);
            sb.append(patternToJson(p, "    "));
            if (i < patterns.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    private String patternToJson(DrumPattern pattern, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append("{\n");
        sb.append(indent).append("  \"name\": \"").append(escapeJson(pattern.getName())).append("\",\n");
        sb.append(indent).append("  \"category\": \"").append(escapeJson(pattern.getStyle())).append("\",\n");
        sb.append(indent).append("  \"timeSignature\": \"").append(pattern.getBeatsPerBar()).append("/").append(pattern.getBeatUnit()).append("\",\n");
        sb.append(indent).append("  \"bars\": ").append(pattern.getBars()).append(",\n");
        sb.append(indent).append("  \"stepsPerBeat\": ").append(pattern.getStepsPerBeat()).append(",\n");
        sb.append(indent).append("  \"tracks\": {\n");

        // Group hits by drum sound
        Map<DrumSound, List<HitInfo>> hitsBySound = new LinkedHashMap<>();
        int totalSteps = pattern.getTotalSteps();

        for (int step = 0; step < totalSteps; step++) {
            List<DrumHit> hits = pattern.getHitsAt(step);
            for (DrumHit hit : hits) {
                hitsBySound.computeIfAbsent(hit.sound(), k -> new ArrayList<>())
                    .add(new HitInfo(step, hit.velocity(), hit.pan()));
            }
        }

        // Write each track
        List<DrumSound> sounds = new ArrayList<>(hitsBySound.keySet());
        for (int i = 0; i < sounds.size(); i++) {
            DrumSound sound = sounds.get(i);
            List<HitInfo> hits = hitsBySound.get(sound);

            sb.append(indent).append("    \"").append(sound.name().toLowerCase()).append("\": [\n");

            for (int j = 0; j < hits.size(); j++) {
                HitInfo hit = hits.get(j);
                sb.append(indent).append("      { \"step\": ").append(hit.step);
                if (hit.velocity != 1.0f) {
                    sb.append(", \"velocity\": ").append(String.format("%.2f", hit.velocity));
                }
                if (hit.pan != 0.0f) {
                    sb.append(", \"pan\": ").append(String.format("%.2f", hit.pan));
                }
                sb.append(" }");
                if (j < hits.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }

            sb.append(indent).append("    ]");
            if (i < sounds.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append(indent).append("  }\n");
        sb.append(indent).append("}");
        return sb.toString();
    }

    private record HitInfo(int step, float velocity, float pan) {}

    /**
     * Parse library from JSON string.
     */
    public static DrumPatternLibrary fromJson(String json) {
        DrumPatternLibrary library = new DrumPatternLibrary();

        // Parse library metadata
        library.name = extractString(json, "name", "Unnamed Library");
        library.version = extractString(json, "version", "1.0");
        library.author = extractString(json, "author", "Unknown");
        library.description = extractString(json, "description", "");

        // Parse patterns array
        int patternsStart = json.indexOf("\"patterns\"");
        if (patternsStart == -1) return library;

        int arrayStart = json.indexOf('[', patternsStart);
        int arrayEnd = findMatchingBracket(json, arrayStart, '[', ']');
        if (arrayStart == -1 || arrayEnd == -1) return library;

        String patternsJson = json.substring(arrayStart + 1, arrayEnd);

        // Parse each pattern object
        int pos = 0;
        while (pos < patternsJson.length()) {
            int objStart = patternsJson.indexOf('{', pos);
            if (objStart == -1) break;

            int objEnd = findMatchingBracket(patternsJson, objStart, '{', '}');
            if (objEnd == -1) break;

            String patternJson = patternsJson.substring(objStart, objEnd + 1);
            DrumPattern pattern = parsePattern(patternJson);
            if (pattern != null) {
                library.addPattern(pattern);
            }

            pos = objEnd + 1;
        }

        return library;
    }

    private static DrumPattern parsePattern(String json) {
        String name = extractString(json, "name", "Unnamed");
        String category = extractString(json, "category", "Other");
        String timeSig = extractString(json, "timeSignature", "4/4");
        int bars = extractInt(json, "bars", 1);
        int stepsPerBeat = extractInt(json, "stepsPerBeat", 4);

        // Parse time signature
        String[] parts = timeSig.split("/");
        int beatsPerBar = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 4;
        int beatUnit = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 4;

        DrumPattern pattern = new DrumPattern(name, category, beatsPerBar, beatUnit, bars);

        // Parse tracks
        int tracksStart = json.indexOf("\"tracks\"");
        if (tracksStart == -1) return pattern;

        int tracksObjStart = json.indexOf('{', tracksStart);
        int tracksObjEnd = findMatchingBracket(json, tracksObjStart, '{', '}');
        if (tracksObjStart == -1 || tracksObjEnd == -1) return pattern;

        String tracksJson = json.substring(tracksObjStart + 1, tracksObjEnd);

        // Parse each track
        for (DrumSound sound : DrumSound.values()) {
            String trackName = sound.name().toLowerCase();
            int trackStart = tracksJson.indexOf("\"" + trackName + "\"");
            if (trackStart == -1) continue;

            int arrayStart = tracksJson.indexOf('[', trackStart);
            int arrayEnd = findMatchingBracket(tracksJson, arrayStart, '[', ']');
            if (arrayStart == -1 || arrayEnd == -1) continue;

            String hitsJson = tracksJson.substring(arrayStart + 1, arrayEnd);

            // Parse hits
            int hitPos = 0;
            while (hitPos < hitsJson.length()) {
                int hitStart = hitsJson.indexOf('{', hitPos);
                if (hitStart == -1) break;

                int hitEnd = hitsJson.indexOf('}', hitStart);
                if (hitEnd == -1) break;

                String hitJson = hitsJson.substring(hitStart, hitEnd + 1);

                int step = extractInt(hitJson, "step", 0);
                float velocity = extractFloat(hitJson, "velocity", 1.0f);
                float pan = extractFloat(hitJson, "pan", 0.0f);

                pattern.addHit(step, sound, velocity, pan);

                hitPos = hitEnd + 1;
            }
        }

        return pattern;
    }

    // ==================== JSON PARSING HELPERS ====================

    private static String extractString(String json, String key, String defaultValue) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*?)\"");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return unescapeJson(m.group(1));
        }
        return defaultValue;
    }

    private static int extractInt(String json, String key, int defaultValue) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return defaultValue;
    }

    private static float extractFloat(String json, String key, float defaultValue) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*([\\d.]+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Float.parseFloat(m.group(1));
        }
        return defaultValue;
    }

    private static int findMatchingBracket(String str, int start, char open, char close) {
        if (start < 0 || start >= str.length() || str.charAt(start) != open) {
            return -1;
        }

        int depth = 1;
        boolean inString = false;
        boolean escaped = false;

        for (int i = start + 1; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == open) depth++;
                else if (c == close) {
                    depth--;
                    if (depth == 0) return i;
                }
            }
        }

        return -1;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private static String unescapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\\\", "\\");
    }

    // ==================== DEFAULT LIBRARY ====================

    /**
     * Create the default built-in library with 100 patterns.
     */
    public static DrumPatternLibrary createDefaultLibrary() {
        DrumPatternLibrary library = new DrumPatternLibrary(
            "JFx2 Default Drum Library",
            "1.0",
            "JFx2",
            "Built-in drum patterns covering various styles and tempos"
        );

        // Add all default patterns
        addMetronomePatterns(library);
        addRockPatterns(library);
        addPopPatterns(library);
        addMetalPatterns(library);
        addBluesPatterns(library);
        addJazzPatterns(library);
        addFunkPatterns(library);
        addSoulPatterns(library);
        addReggaePatterns(library);
        addCountryPatterns(library);
        addDiscoPatterns(library);
        addLatinPatterns(library);
        addWorldPatterns(library);
        addElectronicPatterns(library);

        return library;
    }

    // ==================== PATTERN GENERATORS ====================

    private static void addMetronomePatterns(DrumPatternLibrary library) {
        // Time Beat - Simple metronome (cowbell for downbeat, rimshot for others)
        DrumPattern p = new DrumPattern("Time Beat 4/4", "Metronome", 4, 4, 1);
        for (int beat = 0; beat < 4; beat++) {
            if (beat == 0) {
                p.addHit(0, beat, 0, DrumSound.COWBELL, 1.0f, 0);
            } else {
                p.addHit(0, beat, 0, DrumSound.RIMSHOT, 0.8f, 0);
            }
        }
        library.addPattern(p);

        // Time Beat 3/4
        p = new DrumPattern("Time Beat 3/4", "Metronome", 3, 4, 1);
        for (int beat = 0; beat < 3; beat++) {
            if (beat == 0) {
                p.addHit(0, beat, 0, DrumSound.COWBELL, 1.0f, 0);
            } else {
                p.addHit(0, beat, 0, DrumSound.RIMSHOT, 0.8f, 0);
            }
        }
        library.addPattern(p);

        // Time Beat 6/8 (accents on 1 and 4)
        p = new DrumPattern("Time Beat 6/8", "Metronome", 6, 8, 1);
        for (int beat = 0; beat < 6; beat++) {
            if (beat == 0) {
                p.addHit(0, beat, 0, DrumSound.COWBELL, 1.0f, 0);
            } else if (beat == 3) {
                p.addHit(0, beat, 0, DrumSound.COWBELL, 0.8f, 0);
            } else {
                p.addHit(0, beat, 0, DrumSound.RIMSHOT, 0.6f, 0);
            }
        }
        library.addPattern(p);

        // Click Track (cowbell for downbeat, sticks for others)
        p = new DrumPattern("Click Track", "Metronome", 4, 4, 1);
        for (int beat = 0; beat < 4; beat++) {
            if (beat == 0) {
                p.addHit(0, beat, 0, DrumSound.COWBELL, 1.0f, 0);
            } else {
                p.addHit(0, beat, 0, DrumSound.STICKS, 0.8f, 0);
            }
        }
        library.addPattern(p);
    }

    private static void addRockPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Basic Rock
        p = new DrumPattern("Basic Rock", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        p.addHit(0, 0, 0, DrumSound.CRASH, 0.8f, -0.3f);
        library.addPattern(p);

        // Hard Rock
        p = new DrumPattern("Hard Rock", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 2, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.95f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.8f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Driving Rock
        p = new DrumPattern("Driving Rock", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.9f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Rock Shuffle
        p = new DrumPattern("Rock Shuffle", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Half-Time Rock
        p = new DrumPattern("Half-Time Rock", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Rock Ballad
        p = new DrumPattern("Rock Ballad", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.7f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_OPEN, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Punk Rock
        p = new DrumPattern("Punk Rock", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
                p.addHit(bar, beat, 2, DrumSound.KICK, 0.9f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Garage Rock
        p = new DrumPattern("Garage Rock", "Rock", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.8f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);
    }

    private static void addPopPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Basic Pop
        p = new DrumPattern("Basic Pop", "Pop", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
            }
        }
        library.addPattern(p);

        // Modern Pop
        p = new DrumPattern("Modern Pop", "Pop", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.CLAP, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.CLAP, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Pop Ballad
        p = new DrumPattern("Pop Ballad", "Pop", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.6f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
            }
        }
        library.addPattern(p);

        // Four on the Floor
        p = new DrumPattern("Four on the Floor", "Pop", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.9f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.8f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Uptempo Pop
        p = new DrumPattern("Uptempo Pop", "Pop", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 3, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0) ? 0.6f : 0.4f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        library.addPattern(p);
    }

    private static void addMetalPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Basic Metal
        p = new DrumPattern("Basic Metal", "Metal", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
                p.addHit(bar, beat, 2, DrumSound.KICK, 0.9f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0) ? 0.8f : 0.5f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        p.addHit(0, 0, 0, DrumSound.CRASH, 0.9f, -0.4f);
        library.addPattern(p);

        // Double Bass
        p = new DrumPattern("Double Bass", "Metal", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.KICK, 0.9f, 0);
                }
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Thrash Metal
        p = new DrumPattern("Thrash Metal", "Metal", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
                if (beat % 2 == 0) {
                    p.addHit(bar, beat, 2, DrumSound.KICK, 0.8f, 0);
                }
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Blast Beat
        p = new DrumPattern("Blast Beat", "Metal", 4, 4, 1);
        for (int beat = 0; beat < 4; beat++) {
            for (int sub = 0; sub < 4; sub++) {
                p.addHit(0, beat, sub, DrumSound.KICK, 0.9f, 0);
                p.addHit(0, beat, sub, DrumSound.SNARE, 0.85f, 0);
                p.addHit(0, beat, sub, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
            }
        }
        library.addPattern(p);

        // Half-Time Metal
        p = new DrumPattern("Half-Time Metal", "Metal", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 2, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.8f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Groove Metal
        p = new DrumPattern("Groove Metal", "Metal", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.8f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);
    }

    private static void addBluesPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Blues Shuffle
        p = new DrumPattern("Blues Shuffle", "Blues", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Slow Blues
        p = new DrumPattern("Slow Blues", "Blues", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.7f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.7f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.5f, 0.2f);
                p.addHit(bar, beat, 3, DrumSound.RIDE, 0.4f, 0.2f);
            }
        }
        library.addPattern(p);

        // 12/8 Blues
        p = new DrumPattern("12/8 Blues", "Blues", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.8f, 0);
            // Triplet feel on hi-hat
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 1, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Texas Blues
        p = new DrumPattern("Texas Blues", "Blues", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Chicago Blues
        p = new DrumPattern("Chicago Blues", "Blues", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.85f, 0);
            p.addHit(bar, 0, 2, DrumSound.SNARE, 0.3f, 0); // Ghost
            p.addHit(bar, 2, 2, DrumSound.SNARE, 0.3f, 0); // Ghost
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.6f, 0.2f);
                p.addHit(bar, beat, 3, DrumSound.RIDE, 0.4f, 0.2f);
            }
        }
        library.addPattern(p);
    }

    private static void addJazzPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Jazz Swing
        p = new DrumPattern("Jazz Swing", "Jazz", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.4f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.3f, 0);
            p.addHit(bar, 1, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.4f);
            p.addHit(bar, 3, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.4f);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.6f, 0.2f);
                p.addHit(bar, beat, 3, DrumSound.RIDE, 0.4f, 0.2f);
            }
        }
        p.addHit(1, 1, 2, DrumSound.SNARE, 0.25f, 0);
        p.addHit(1, 3, 2, DrumSound.SNARE, 0.25f, 0);
        library.addPattern(p);

        // Bebop
        p = new DrumPattern("Bebop", "Jazz", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            // Light kick feathering
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.3f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.4f);
            p.addHit(bar, 3, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.4f);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.6f, 0.2f);
                p.addHit(bar, beat, 3, DrumSound.RIDE, 0.45f, 0.2f);
            }
        }
        library.addPattern(p);

        // Jazz Waltz
        p = new DrumPattern("Jazz Waltz", "Jazz", 3, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.5f, 0);
            for (int beat = 0; beat < 3; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.6f, 0.2f);
                if (beat > 0) {
                    p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.4f);
                }
            }
        }
        library.addPattern(p);

        // Bossa Nova
        p = new DrumPattern("Bossa Nova", "Jazz", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.RIMSHOT, 0.7f, 0);
            p.addHit(bar, 0, 3, DrumSound.RIMSHOT, 0.5f, 0);
            p.addHit(bar, 2, 2, DrumSound.RIMSHOT, 0.6f, 0);
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.55f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.35f, 0.3f);
            }
        }
        library.addPattern(p);

        // Cool Jazz
        p = new DrumPattern("Cool Jazz", "Jazz", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.35f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.25f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.5f, 0.2f);
            }
            p.addHit(bar, 1, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.4f);
            p.addHit(bar, 3, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.4f);
        }
        library.addPattern(p);

        // Afro-Cuban Jazz
        p = new DrumPattern("Afro-Cuban Jazz", "Jazz", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            // Clave-inspired kick
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.6f, 0);
            // Ride pattern
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIDE, 0.6f, 0.2f);
                p.addHit(bar, beat, 2, DrumSound.RIDE, 0.4f, 0.2f);
            }
            // Cross-stick
            p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.5f, 0);
            p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.5f, 0);
        }
        library.addPattern(p);
    }

    private static void addFunkPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Basic Funk
        p = new DrumPattern("Basic Funk", "Funk", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            // Ghost notes
            p.addHit(bar, 0, 2, DrumSound.SNARE, 0.25f, 0);
            p.addHit(bar, 1, 2, DrumSound.SNARE, 0.2f, 0);
            p.addHit(bar, 2, 2, DrumSound.SNARE, 0.25f, 0);
            p.addHit(bar, 3, 2, DrumSound.SNARE, 0.2f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0 || sub == 2) ? 0.7f : 0.4f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        p.addHit(0, 3, 2, DrumSound.HIHAT_OPEN, 0.6f, 0.3f);
        library.addPattern(p);

        // Funky Drummer
        p = new DrumPattern("Funky Drummer", "Funk", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            // Heavy ghost notes
            p.addHit(bar, 0, 2, DrumSound.SNARE, 0.3f, 0);
            p.addHit(bar, 1, 2, DrumSound.SNARE, 0.25f, 0);
            p.addHit(bar, 2, 2, DrumSound.SNARE, 0.3f, 0);
            p.addHit(bar, 3, 2, DrumSound.SNARE, 0.25f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Slap Funk
        p = new DrumPattern("Slap Funk", "Funk", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // P-Funk
        p = new DrumPattern("P-Funk", "Funk", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 3, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub % 2 == 0) ? 0.6f : 0.4f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Tower of Power
        p = new DrumPattern("Tower of Power", "Funk", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.4f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            // Ghost notes
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 2, DrumSound.SNARE, 0.2f, 0);
            }
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                }
            }
        }
        library.addPattern(p);
    }

    private static void addSoulPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Classic Soul
        p = new DrumPattern("Classic Soul", "Soul", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
            }
        }
        library.addPattern(p);

        // Motown
        p = new DrumPattern("Motown", "Soul", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.8f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
            }
            // Tambourine feel
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.RIMSHOT, 0.3f, 0.5f);
                p.addHit(bar, beat, 2, DrumSound.RIMSHOT, 0.25f, 0.5f);
            }
        }
        library.addPattern(p);

        // Neo Soul
        p = new DrumPattern("Neo Soul", "Soul", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 3, DrumSound.KICK, 0.5f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.8f, 0);
            // Ghost notes
            p.addHit(bar, 0, 2, DrumSound.SNARE, 0.2f, 0);
            p.addHit(bar, 2, 2, DrumSound.SNARE, 0.2f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0) ? 0.5f : 0.3f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Philadelphia Soul
        p = new DrumPattern("Philadelphia Soul", "Soul", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.85f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.4f, 0.3f);
            }
        }
        library.addPattern(p);
    }

    private static void addReggaePatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // One Drop
        p = new DrumPattern("One Drop", "Reggae", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.6f, 0);
            p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.6f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Rockers
        p = new DrumPattern("Rockers", "Reggae", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.5f, 0);
            p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.5f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Steppers
        p = new DrumPattern("Steppers", "Reggae", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.85f, 0);
            }
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.5f, 0);
            p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.5f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Ska
        p = new DrumPattern("Ska", "Reggae", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Dub
        p = new DrumPattern("Dub", "Reggae", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 2, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);
    }

    private static void addCountryPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Train Beat
        p = new DrumPattern("Train Beat", "Country", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 1, DrumSound.HIHAT_OPEN, 0.4f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_OPEN, 0.4f, 0.3f);
            }
        }
        library.addPattern(p);

        // Country Rock
        p = new DrumPattern("Country Rock", "Country", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.7f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Country Waltz
        p = new DrumPattern("Country Waltz", "Country", 3, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.6f, 0);
            for (int beat = 0; beat < 3; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Two-Step
        p = new DrumPattern("Two-Step", "Country", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.85f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Honky Tonk
        p = new DrumPattern("Honky Tonk", "Country", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.85f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 3, DrumSound.HIHAT_CLOSED, 0.45f, 0.3f);
            }
        }
        library.addPattern(p);
    }

    private static void addDiscoPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Classic Disco
        p = new DrumPattern("Classic Disco", "Disco", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.7f, 0.3f);
            }
        }
        library.addPattern(p);

        // Hi-NRG
        p = new DrumPattern("Hi-NRG", "Disco", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.CLAP, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.CLAP, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 2) ? 0.7f : 0.5f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Eurodisco
        p = new DrumPattern("Eurodisco", "Disco", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.95f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.65f, 0.3f);
            }
        }
        library.addPattern(p);

        // Nu-Disco
        p = new DrumPattern("Nu-Disco", "Disco", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 0.9f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.CLAP, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.CLAP, 0.85f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);
    }

    private static void addLatinPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Samba
        p = new DrumPattern("Samba", "Latin", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.65f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.8f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Salsa
        p = new DrumPattern("Salsa", "Latin", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            // Tumbao kick pattern
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.7f, 0);
            // Clave on rimshot
            p.addHit(bar, 0, 0, DrumSound.RIMSHOT, 0.7f, 0);
            p.addHit(bar, 0, 3, DrumSound.RIMSHOT, 0.6f, 0);
            p.addHit(bar, 2, 2, DrumSound.RIMSHOT, 0.65f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.45f, 0.3f);
            }
        }
        library.addPattern(p);

        // Cha-Cha
        p = new DrumPattern("Cha-Cha", "Latin", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.75f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.65f, 0);
            // Guiro/shaker feel
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                }
            }
            p.addHit(bar, 1, 0, DrumSound.COWBELL, 0.5f, 0.4f);
            p.addHit(bar, 3, 0, DrumSound.COWBELL, 0.5f, 0.4f);
        }
        library.addPattern(p);

        // Mambo
        p = new DrumPattern("Mambo", "Latin", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.7f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.7f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.COWBELL, 0.6f, 0.4f);
                p.addHit(bar, beat, 2, DrumSound.COWBELL, 0.5f, 0.4f);
            }
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.35f, 0.3f);
            }
        }
        library.addPattern(p);

        // Rumba
        p = new DrumPattern("Rumba", "Latin", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            // Clave pattern
            if (bar == 0) {
                p.addHit(bar, 0, 0, DrumSound.RIMSHOT, 0.7f, 0);
                p.addHit(bar, 0, 3, DrumSound.RIMSHOT, 0.6f, 0);
                p.addHit(bar, 2, 2, DrumSound.RIMSHOT, 0.65f, 0);
            } else {
                p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.65f, 0);
                p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.7f, 0);
            }
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.65f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.35f, 0.3f);
            }
        }
        library.addPattern(p);

        // Tango
        p = new DrumPattern("Tango", "Latin", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 0, 0, DrumSound.SNARE, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.55f, 0);
        }
        library.addPattern(p);
    }

    private static void addWorldPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // Afrobeat
        p = new DrumPattern("Afrobeat", "World", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 2, 3, DrumSound.KICK, 0.55f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.85f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 0 || sub == 2) ? 0.6f : 0.4f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Highlife
        p = new DrumPattern("Highlife", "World", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.85f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 0.8f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.8f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.55f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.45f, 0.3f);
            }
            // Bell pattern
            p.addHit(bar, 0, 0, DrumSound.COWBELL, 0.5f, 0.5f);
            p.addHit(bar, 1, 2, DrumSound.COWBELL, 0.4f, 0.5f);
            p.addHit(bar, 2, 0, DrumSound.COWBELL, 0.45f, 0.5f);
            p.addHit(bar, 3, 2, DrumSound.COWBELL, 0.4f, 0.5f);
        }
        library.addPattern(p);

        // Middle Eastern
        p = new DrumPattern("Middle Eastern", "World", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            // Doum (low)
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.7f, 0);
            // Tek (high)
            p.addHit(bar, 1, 0, DrumSound.RIMSHOT, 0.7f, 0);
            p.addHit(bar, 1, 2, DrumSound.RIMSHOT, 0.5f, 0);
            p.addHit(bar, 3, 0, DrumSound.RIMSHOT, 0.7f, 0);
            p.addHit(bar, 3, 2, DrumSound.RIMSHOT, 0.5f, 0);
        }
        library.addPattern(p);

        // 6/8 African
        p = new DrumPattern("6/8 African", "World", 6, 8, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 0.7f, 0);
            for (int beat = 0; beat < 6; beat++) {
                float vel = (beat == 0 || beat == 3) ? 0.7f : 0.5f;
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, vel, 0.3f);
            }
            // Bell
            p.addHit(bar, 0, 0, DrumSound.COWBELL, 0.6f, 0.5f);
            p.addHit(bar, 2, 0, DrumSound.COWBELL, 0.5f, 0.5f);
            p.addHit(bar, 3, 0, DrumSound.COWBELL, 0.55f, 0.5f);
            p.addHit(bar, 5, 0, DrumSound.COWBELL, 0.5f, 0.5f);
        }
        library.addPattern(p);
    }

    private static void addElectronicPatterns(DrumPatternLibrary library) {
        DrumPattern p;

        // House
        p = new DrumPattern("House", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.CLAP, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.CLAP, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_OPEN, 0.6f, 0.3f);
            }
        }
        library.addPattern(p);

        // Techno
        p = new DrumPattern("Techno", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.KICK, 1.0f, 0);
            }
            p.addHit(bar, 1, 0, DrumSound.CLAP, 0.85f, 0);
            p.addHit(bar, 3, 0, DrumSound.CLAP, 0.85f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    float vel = (sub == 2) ? 0.65f : 0.45f;
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, vel, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Hip-Hop
        p = new DrumPattern("Hip-Hop", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Trap
        p = new DrumPattern("Trap", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            // Rolling hi-hats
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Drum and Bass
        p = new DrumPattern("Drum and Bass", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 2, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Dubstep
        p = new DrumPattern("Dubstep", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
            }
        }
        library.addPattern(p);

        // Breakbeat
        p = new DrumPattern("Breakbeat", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 0, 3, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 2, DrumSound.KICK, 0.8f, 0);
            p.addHit(bar, 1, 0, DrumSound.SNARE, 1.0f, 0);
            p.addHit(bar, 2, 0, DrumSound.SNARE, 0.7f, 0);
            p.addHit(bar, 3, 0, DrumSound.SNARE, 1.0f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.6f, 0.3f);
                p.addHit(bar, beat, 2, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
            }
        }
        library.addPattern(p);

        // Electro
        p = new DrumPattern("Electro", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 1.0f, 0);
            p.addHit(bar, 1, 0, DrumSound.KICK, 0.7f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.9f, 0);
            p.addHit(bar, 3, 2, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 1, 0, DrumSound.CLAP, 0.9f, 0);
            p.addHit(bar, 3, 0, DrumSound.CLAP, 0.9f, 0);
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.5f, 0.3f);
                }
            }
        }
        library.addPattern(p);

        // Ambient
        p = new DrumPattern("Ambient", "Electronic", 4, 4, 2);
        for (int bar = 0; bar < 2; bar++) {
            p.addHit(bar, 0, 0, DrumSound.KICK, 0.6f, 0);
            p.addHit(bar, 2, 0, DrumSound.KICK, 0.5f, 0);
            for (int beat = 0; beat < 4; beat++) {
                p.addHit(bar, beat, 0, DrumSound.HIHAT_CLOSED, 0.3f, 0.3f);
            }
        }
        library.addPattern(p);

        // IDM
        p = new DrumPattern("IDM", "Electronic", 4, 4, 2);
        // Irregular pattern
        p.addHit(0, 0, 0, DrumSound.KICK, 0.9f, 0);
        p.addHit(0, 1, 1, DrumSound.KICK, 0.7f, 0);
        p.addHit(0, 2, 3, DrumSound.KICK, 0.8f, 0);
        p.addHit(1, 0, 2, DrumSound.KICK, 0.75f, 0);
        p.addHit(1, 2, 0, DrumSound.KICK, 0.85f, 0);
        p.addHit(0, 1, 0, DrumSound.SNARE, 0.9f, 0);
        p.addHit(0, 3, 2, DrumSound.SNARE, 0.7f, 0);
        p.addHit(1, 1, 1, DrumSound.SNARE, 0.8f, 0);
        p.addHit(1, 3, 0, DrumSound.SNARE, 0.9f, 0);
        for (int bar = 0; bar < 2; bar++) {
            for (int beat = 0; beat < 4; beat++) {
                for (int sub = 0; sub < 4; sub++) {
                    p.addHit(bar, beat, sub, DrumSound.HIHAT_CLOSED, 0.4f, 0.3f);
                }
            }
        }
        library.addPattern(p);
    }
}
