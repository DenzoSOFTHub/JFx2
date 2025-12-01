package it.denzosoft.jfx2.preset;

/**
 * Metadata for a rig preset.
 *
 * @param name        Preset name
 * @param author      Author/creator
 * @param description Brief description
 * @param category    Category (Clean, Crunch, Lead, Ambient, etc.)
 * @param tags        Comma-separated tags for search
 * @param version     Preset format version
 * @param createdAt   Creation timestamp (ISO 8601)
 * @param modifiedAt  Last modification timestamp (ISO 8601)
 */
public record RigMetadata(
        String name,
        String author,
        String description,
        String category,
        String tags,
        String version,
        String createdAt,
        String modifiedAt
) {
    /**
     * Current preset format version.
     */
    public static final String CURRENT_VERSION = "1.0";

    /**
     * Create metadata with minimal info.
     */
    public static RigMetadata of(String name, String category) {
        String now = java.time.Instant.now().toString();
        return new RigMetadata(name, "User", "", category, "", CURRENT_VERSION, now, now);
    }

    /**
     * Create metadata with description.
     */
    public static RigMetadata of(String name, String category, String description) {
        String now = java.time.Instant.now().toString();
        return new RigMetadata(name, "User", description, category, "", CURRENT_VERSION, now, now);
    }

    /**
     * Create a copy with updated modification time.
     */
    public RigMetadata withModifiedNow() {
        return new RigMetadata(name, author, description, category, tags, version,
                createdAt, java.time.Instant.now().toString());
    }

    /**
     * Create a copy with a new name.
     */
    public RigMetadata withName(String newName) {
        return new RigMetadata(newName, author, description, category, tags, version, createdAt, modifiedAt);
    }
}
