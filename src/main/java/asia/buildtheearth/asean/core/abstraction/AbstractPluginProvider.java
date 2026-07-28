package asia.buildtheearth.asean.core.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.api.PluginProvider;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A base class that provides access to the main {@link MasterServer} plugin instance.
 */
public abstract class AbstractPluginProvider implements PluginProvider {

    /**
     * The reference to the main {@link MasterServer} plugin instance.
     */
    protected final @NotNull MasterServer plugin;

    /**
     * Constructs a new {@code PluginProvider} with the specified {@link MasterServer} plugin instance.
     * @param plugin the plugin instance to be provided; must not be {@code null}
     */
    public AbstractPluginProvider(@NotNull MasterServer plugin) {
        this.plugin = plugin;
    }

    /**
     * Constructs a new {@code PluginProvider} using existing provider.
     * @param provider the provider instance; must not be {@code null}
     */
    @Contract(pure = true)
    public AbstractPluginProvider(@NotNull PluginProvider provider) {
        this(provider.getPlugin());
    }

    public final @NotNull MasterServer getPlugin() {
        return this.plugin;
    }
}
