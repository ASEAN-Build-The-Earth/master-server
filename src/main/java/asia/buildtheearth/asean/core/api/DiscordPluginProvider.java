package asia.buildtheearth.asean.core.api;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.DiscordSRVModule;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.DiscordAPI;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public interface DiscordPluginProvider extends PluginProvider {
    @NotNull DiscordSRV getAPI();

    default @NotNull DiscordAPI getDiscordAPI() {
        return getAPI().discordAPI();
    }

    default DiscordSRVModule getDiscordSRV() {
        return getPlugin().discordSRV();
    }

    default MainGuild getMainGuild() {
        return getDiscordSRV().mainGuild();
    }

    @Contract(value = "_, _ -> new", pure = true)
    static @NotNull DiscordPluginProvider fork(@NotNull MasterServer plugin, @NotNull DiscordSRV api) {
        return new DiscordPluginProvider() {
            @Override public @NotNull MasterServer getPlugin() { return plugin; }
            @Override public @NotNull DiscordSRV getAPI() { return api; }
        };
    }
}
