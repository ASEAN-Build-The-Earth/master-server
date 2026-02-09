package asia.buildtheearth.asean.core.providers;

import asia.buildtheearth.asean.MasterServer;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.DiscordAPI;
import org.jetbrains.annotations.NotNull;

public abstract class PluginForDiscordSRV extends PluginProvider {

    protected final DiscordSRV api;

    public PluginForDiscordSRV(@NotNull MasterServer plugin, @NotNull DiscordSRV api) {
        super(plugin);
        this.api = api;
    }

    public PluginForDiscordSRV(@NotNull PluginForDiscordSRV provider) {
        this(provider.plugin, provider.api);
    }

    public final MasterServer getPlugin() {
        return this.plugin;
    }

    public final DiscordSRV getAPI() {
        return this.api;
    }

    public final DiscordAPI getDiscordAPI() {
        return this.api.discordAPI();
    }
}
