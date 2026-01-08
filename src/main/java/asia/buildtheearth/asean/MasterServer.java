package asia.buildtheearth.asean;

import asia.buildtheearth.asean.core.DiscordSRVListener;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.module.Module;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.function.Consumer;

public final class MasterServer extends JavaPlugin {

    public static final String DISCORD_SRV_SYMBOL = "DiscordSRV-Ascension";

    private static MasterServer plugin;

    private DiscordSRVListener discordSrvHook = null;

    public static MasterServer getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Initialize plugin reference
        plugin = this;

        // Initialize plugin
        Thread initThread = createInitThread();
        initThread.start();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void init() {
        // TODO: Initialize database connection

        org.bukkit.plugin.Plugin discordSRV = getServer().getPluginManager().getPlugin(DISCORD_SRV_SYMBOL);

        if (discordSRV != null) {
            info("DiscordSRV is loaded");
            subscribeToDiscordSRV(discordSRV);
        }
        else { // Fatal error if DiscordSRV does not exist
            error("DiscordSRV symbol does not exist in master server");
        }
    }

    /**
     * Subscribe to DiscordSRV instance.
     *
     * @param plugin The DiscordSRV plugin instance
     * @see com.discordsrv.api.DiscordSRV#registerModule(Module)
     */
    public void subscribeToDiscordSRV(@NotNull org.bukkit.plugin.Plugin plugin) {
        info("subscribing to DiscordSRV: " + plugin);

        if (!DISCORD_SRV_SYMBOL.equals(plugin.getName())) {
            error("Expected plugin name for integration does not match for: "
                + plugin + " (Expecting: " + DISCORD_SRV_SYMBOL + ")");
            return;
        }
        if (isDiscordSrvHookEnabled()) {
            error("Already subscribed to DiscordSRV. Did the server reload?");
            return;
        }

        DiscordSRV.optional().ifPresentOrElse(api -> {
            // If DiscordSRV JDA is ready before this plugin finish initializing
            api.registerModule(discordSrvHook = new DiscordSRVListener(this, api));

            info("Subscribed to DiscordSRV.");
        }, () -> error("Instance for DiscordSRV does not exist. Did it started correctly?"));
    }

    private @NotNull Thread createInitThread() {
        Thread initThread = new Thread(this::init, "MasterServer - Initialization");
        initThread.setUncaughtExceptionHandler((t, e) -> {
            error("[MasterServer - Initialization] ERROR: Uncaught exception");
            error("[MasterServer - Initialization] ERROR: " + e, e);
            for(StackTraceElement ex : e.getStackTrace()) {
                error(ex.toString());
            }

            // disablePlugin("DiscordPlotSystem failed to load properly: " + e);
        });
        return initThread;
    }

    public boolean isDiscordSrvHookEnabled() {
        return discordSrvHook != null && discordSrvHook.isEnabled();
    }

    public static void logThrowable(@NotNull Throwable throwable, Consumer<String> logger) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        for (String line : stringWriter.toString().split("\n")) logger.accept(line);
    }

    public static void info(String message) {
        plugin.getLogger().info(message);
    }

    public static void warning(String message) {
        plugin.getLogger().warning(message);
    }

    public static void error(String message) {
        plugin.getLogger().severe(message);
    }

    public static void error(Throwable throwable) {
        logThrowable(throwable, MasterServer::error);
    }

    public static void error(String message, Throwable throwable) {
        error(message);
        error(throwable);
    }
}
