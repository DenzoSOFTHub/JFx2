package it.denzosoft.jfx2.nam;

import it.denzosoft.jfx2.nam.json.JsonParser;
import it.denzosoft.jfx2.nam.json.JsonValue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Loader for NAM (Neural Amp Modeler) .nam files.
 *
 * <p>Supports WaveNet and LSTM architectures.</p>
 */
public class NAMLoader {

    private static final int DEFAULT_SAMPLE_RATE = 48000;

    /**
     * Load a NAM model from file.
     *
     * @param path Path to .nam file
     * @return Loaded model
     * @throws IOException if loading fails
     */
    public static NAMModel load(Path path) throws IOException {
        JsonValue root = JsonParser.parseFile(path);
        return load(root);
    }

    /**
     * Load a NAM model from file path string.
     */
    public static NAMModel load(String path) throws IOException {
        return load(Path.of(path));
    }

    /**
     * Load a NAM model from parsed JSON.
     */
    public static NAMModel load(JsonValue root) throws IOException {
        // Get architecture
        String architecture = root.getString("architecture");
        if (architecture == null) {
            throw new IOException("Missing 'architecture' in NAM file");
        }

        // Get sample rate
        int sampleRate = root.getInt("sample_rate", DEFAULT_SAMPLE_RATE);

        // Get config
        JsonValue config = root.getObject("config");
        if (config == null) {
            throw new IOException("Missing 'config' in NAM file");
        }

        // Get weights
        List<JsonValue> weightsArray = root.getArray("weights");
        if (weightsArray == null) {
            throw new IOException("Missing 'weights' in NAM file");
        }

        float[] weights = new float[weightsArray.size()];
        for (int i = 0; i < weightsArray.size(); i++) {
            weights[i] = weightsArray.get(i).asFloat();
        }

        // Create model based on architecture
        return switch (architecture.toLowerCase()) {
            case "wavenet" -> new WaveNet(config, weights, sampleRate);
            case "lstm" -> new LSTM(config, weights, sampleRate);
            default -> throw new IOException("Unsupported architecture: " + architecture);
        };
    }

    /**
     * Get metadata from a NAM file without loading the full model.
     */
    public static NAMMetadata getMetadata(Path path) throws IOException {
        JsonValue root = JsonParser.parseFile(path);
        return getMetadata(root);
    }

    /**
     * Get metadata from parsed JSON.
     */
    public static NAMMetadata getMetadata(JsonValue root) {
        String version = root.getString("version", "unknown");
        String architecture = root.getString("architecture", "unknown");
        int sampleRate = root.getInt("sample_rate", DEFAULT_SAMPLE_RATE);

        // Get optional metadata
        JsonValue metaObj = root.getObject("metadata");
        String name = null;
        String author = null;
        String description = null;

        if (metaObj != null) {
            name = metaObj.getString("name", null);
            author = metaObj.getString("author", null);
            description = metaObj.getString("description", null);
        }

        // Get weight count
        List<JsonValue> weightsArray = root.getArray("weights");
        int paramCount = weightsArray != null ? weightsArray.size() : 0;

        return new NAMMetadata(version, architecture, sampleRate, name, author, description, paramCount);
    }

    /**
     * Metadata record for NAM files.
     */
    public record NAMMetadata(
            String version,
            String architecture,
            int sampleRate,
            String name,
            String author,
            String description,
            int parameterCount
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("NAM Model:\n");
            if (name != null) sb.append("  Name: ").append(name).append("\n");
            if (author != null) sb.append("  Author: ").append(author).append("\n");
            sb.append("  Architecture: ").append(architecture).append("\n");
            sb.append("  Sample rate: ").append(sampleRate).append(" Hz\n");
            sb.append("  Parameters: ").append(parameterCount).append("\n");
            sb.append("  Version: ").append(version).append("\n");
            if (description != null) sb.append("  Description: ").append(description).append("\n");
            return sb.toString();
        }
    }
}
