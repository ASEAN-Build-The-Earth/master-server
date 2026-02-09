package asia.buildtheearth.asean.core;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.providers.PluginForDiscordSRV;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.DiscordSRV;

public abstract class AbstractModule extends PluginForDiscordSRV implements Module {

    public AbstractModule(MasterServer plugin, DiscordSRV api) {
        super(plugin, api);
    }

    public AbstractModule(PluginForDiscordSRV plugin) {
        super(plugin);
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    public final void register() {
        this.api.registerModule(this);
    }
}