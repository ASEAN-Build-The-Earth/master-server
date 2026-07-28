package asia.buildtheearth.asean.core.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.DiscordAPI;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractDiscordPlugin extends AbstractPluginProvider implements DiscordPluginProvider {

    protected final @NotNull DiscordSRV api;

    public AbstractDiscordPlugin(@NotNull MasterServer plugin,
                                 @NotNull DiscordSRV api) {
        super(plugin);
        this.api = api;
    }

    public AbstractDiscordPlugin(@NotNull DiscordPluginProvider provider) {
        this(provider.getPlugin(), provider.getAPI());
    }

    public final @NotNull DiscordSRV getAPI() {
        return this.api;
    }

    public final @NotNull DiscordAPI getDiscordAPI() {
        return this.api.discordAPI();
    }


}
