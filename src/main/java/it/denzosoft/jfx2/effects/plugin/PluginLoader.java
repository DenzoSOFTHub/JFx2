package it.denzosoft.jfx2.effects.plugin;

import it.denzosoft.jfx2.effects.EffectFactory;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Loads effect plugins from external JAR files.
 *
 * <p>At startup, scans the "plugins/" directory for JAR files and loads
 * any EffectPlugin implementations found via ServiceLoader.</p>
 *
 * <p>Usage:
 * <pre>
 * PluginLoader loader = new PluginLoader();
 * loader.loadPlugins();
 * </pre>
 * </p>
 */
public class PluginLoader {

    private static final String PLUGINS_DIR = "plugins";

    private final List<LoadedPlugin> loadedPlugins = new ArrayList<>();
    private URLClassLoader pluginClassLoader;

    /**
     * Information about a loaded plugin.
     */
    public record LoadedPlugin(
        String name,
        String version,
        String author,
        int effectCount,
        String jarFile
    ) {}

    /**
     * Load all plugins from the plugins directory.
     *
     * @return Number of effects loaded
     */
    public int loadPlugins() {
        File pluginsDir = new File(PLUGINS_DIR);

        if (!pluginsDir.exists()) {
            System.out.println("[PluginLoader] No plugins directory found. Creating: " + pluginsDir.getAbsolutePath());
            pluginsDir.mkdirs();
            return 0;
        }

        if (!pluginsDir.isDirectory()) {
            System.err.println("[PluginLoader] 'plugins' is not a directory");
            return 0;
        }

        // Find all JAR files
        File[] jarFiles = pluginsDir.listFiles((dir, name) ->
            name.toLowerCase().endsWith(".jar"));

        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("[PluginLoader] No plugin JARs found in: " + pluginsDir.getAbsolutePath());
            return 0;
        }

        System.out.println("[PluginLoader] Found " + jarFiles.length + " plugin JAR(s)");

        // Create URL array for class loader
        List<URL> urls = new ArrayList<>();
        for (File jar : jarFiles) {
            try {
                urls.add(jar.toURI().toURL());
                System.out.println("[PluginLoader] Adding JAR: " + jar.getName());
            } catch (Exception e) {
                System.err.println("[PluginLoader] Failed to load JAR: " + jar.getName() + " - " + e.getMessage());
            }
        }

        if (urls.isEmpty()) {
            return 0;
        }

        // Create class loader with plugin JARs
        pluginClassLoader = new URLClassLoader(
            urls.toArray(new URL[0]),
            getClass().getClassLoader()
        );

        // Load plugins using ServiceLoader
        int totalEffects = 0;
        ServiceLoader<EffectPlugin> serviceLoader = ServiceLoader.load(EffectPlugin.class, pluginClassLoader);

        for (EffectPlugin plugin : serviceLoader) {
            try {
                totalEffects += loadPlugin(plugin);
            } catch (Exception e) {
                System.err.println("[PluginLoader] Failed to load plugin: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("[PluginLoader] Loaded " + loadedPlugins.size() + " plugin(s) with " + totalEffects + " effect(s)");
        return totalEffects;
    }

    /**
     * Load a single plugin.
     */
    private int loadPlugin(EffectPlugin plugin) {
        System.out.println("[PluginLoader] Loading plugin: " + plugin.getName() + " v" + plugin.getVersion());

        // Call plugin initialization
        plugin.onLoad();

        // Get effects from plugin
        List<EffectPlugin.EffectRegistration> effects = plugin.getEffects();
        if (effects == null || effects.isEmpty()) {
            System.out.println("[PluginLoader] Plugin '" + plugin.getName() + "' has no effects");
            return 0;
        }

        // Register effects with factory
        EffectFactory factory = EffectFactory.getInstance();
        int registered = 0;

        for (EffectPlugin.EffectRegistration registration : effects) {
            try {
                if (factory.isRegistered(registration.id())) {
                    System.out.println("[PluginLoader] Skipping duplicate effect ID: " + registration.id());
                    continue;
                }

                factory.register(registration.id(), registration.factory());
                registered++;
                System.out.println("[PluginLoader]   Registered effect: " + registration.id());
            } catch (Exception e) {
                System.err.println("[PluginLoader]   Failed to register effect '" + registration.id() + "': " + e.getMessage());
            }
        }

        // Track loaded plugin
        loadedPlugins.add(new LoadedPlugin(
            plugin.getName(),
            plugin.getVersion(),
            plugin.getAuthor(),
            registered,
            "plugin"
        ));

        System.out.println("[PluginLoader] Plugin '" + plugin.getName() + "' loaded with " + registered + " effect(s)");
        return registered;
    }

    /**
     * Unload all plugins.
     */
    public void unloadPlugins() {
        // Note: We can't actually unregister effects from the factory,
        // but we can clean up the class loader
        if (pluginClassLoader != null) {
            try {
                pluginClassLoader.close();
            } catch (Exception e) {
                System.err.println("[PluginLoader] Error closing class loader: " + e.getMessage());
            }
            pluginClassLoader = null;
        }
        loadedPlugins.clear();
        System.out.println("[PluginLoader] Plugins unloaded");
    }

    /**
     * Get list of loaded plugins.
     */
    public List<LoadedPlugin> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    /**
     * Check if any plugins are loaded.
     */
    public boolean hasPlugins() {
        return !loadedPlugins.isEmpty();
    }

    /**
     * Get total number of effects from plugins.
     */
    public int getPluginEffectCount() {
        return loadedPlugins.stream()
            .mapToInt(LoadedPlugin::effectCount)
            .sum();
    }
}
