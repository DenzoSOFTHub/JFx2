package it.denzosoft.jfx2.effects;

/**
 * Metadata describing an effect type.
 *
 * @param id          Unique identifier for this effect type
 * @param name        Display name
 * @param description Brief description of the effect
 * @param category    Effect category for organization
 * @param author      Author/creator
 * @param version     Version string
 */
public record EffectMetadata(
        String id,
        String name,
        String description,
        EffectCategory category,
        String author,
        String version
) {
    /**
     * Create metadata with minimal info.
     */
    public static EffectMetadata of(String id, String name, EffectCategory category) {
        return new EffectMetadata(id, name, "", category, "JFx2", "1.0");
    }

    /**
     * Create metadata with description.
     */
    public static EffectMetadata of(String id, String name, String description, EffectCategory category) {
        return new EffectMetadata(id, name, description, category, "JFx2", "1.0");
    }

    /**
     * Create metadata with author and version.
     */
    public static EffectMetadata of(String id, String name, String description, EffectCategory category, String author, String version) {
        return new EffectMetadata(id, name, description, category, author, version);
    }
}
