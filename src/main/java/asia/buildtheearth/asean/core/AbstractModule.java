package asia.buildtheearth.asean.core;

import com.discordsrv.api.discord.DiscordAPI;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.DiscordSRV;

public abstract class AbstractModule<API extends DiscordSRV> implements Module {

    protected final API api;

    public AbstractModule(API api) {
        this.api = api;
    }

    @Override
    public String toString() {
        return getClass().getName();
    }

    public final DiscordAPI discordAPI() {
        return this.api.discordAPI();
    }
}