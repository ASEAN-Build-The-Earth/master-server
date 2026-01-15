package asia.buildtheearth.asean.core.providers;


import asia.buildtheearth.asean.MasterServer;

/**
 * A base class that provides access to the main {@link MasterServer} plugin instance.
 */
public abstract class PluginProvider {

    /**
     * The reference to the main {@link MasterServer} plugin instance.
     */
    protected final MasterServer plugin;

    /**
     * Constructs a new {@code PluginProvider} with the specified {@link MasterServer} plugin instance.
     * @param plugin the plugin instance to be provided; must not be {@code null}
     */
    public PluginProvider(MasterServer plugin) {
        this.plugin = plugin;
    }
}
