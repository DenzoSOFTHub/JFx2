package it.denzosoft.jfx2.effects.plugin;

import it.denzosoft.jfx2.effects.AudioEffect;

import java.util.List;
import java.util.function.Supplier;

/**
 * Service Provider Interface for effect plugins.
 *
 * <p>External JARs can provide additional effects by implementing this interface
 * and registering it via Java's ServiceLoader mechanism (META-INF/services).</p>
 *
 * <p>Example implementation:
 * <pre>
 * public class CommercialPackPlugin implements EffectPlugin {
 *     @Override
 *     public String getName() {
 *         return "Commercial Packs";
 *     }
 *
 *     @Override
 *     public List<EffectRegistration> getEffects() {
 *         return List.of(
 *             new EffectRegistration("bigpuff", BigPuffEffect::new),
 *             new EffectRegistration("tubsquealer", TubeSqualerEffect::new)
 *         );
 *     }
 * }
 * </pre>
 * </p>
 */
public interface EffectPlugin {

    /**
     * Get the plugin name for display purposes.
     *
     * @return Plugin name (e.g., "Commercial Packs")
     */
    String getName();

    /**
     * Get the plugin version.
     *
     * @return Version string (e.g., "1.0.0")
     */
    default String getVersion() {
        return "1.0.0";
    }

    /**
     * Get the plugin author/vendor.
     *
     * @return Author name
     */
    default String getAuthor() {
        return "Unknown";
    }

    /**
     * Get the list of effects provided by this plugin.
     *
     * @return List of effect registrations
     */
    List<EffectRegistration> getEffects();

    /**
     * Called when the plugin is loaded.
     * Override to perform initialization.
     */
    default void onLoad() {
        // Default: no-op
    }

    /**
     * Called when the plugin is unloaded.
     * Override to perform cleanup.
     */
    default void onUnload() {
        // Default: no-op
    }

    /**
     * Registration record for a single effect.
     */
    record EffectRegistration(
        String id,
        Supplier<AudioEffect> factory
    ) {}
}
