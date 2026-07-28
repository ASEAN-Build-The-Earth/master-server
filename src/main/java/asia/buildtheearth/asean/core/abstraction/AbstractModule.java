package asia.buildtheearth.asean.core.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.DiscordSRV;

public abstract class AbstractModule extends AbstractDiscordPlugin implements Module {

    public AbstractModule(MasterServer plugin, DiscordSRV api) {
        super(plugin, api);
    }

    public AbstractModule(DiscordPluginProvider plugin) {
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