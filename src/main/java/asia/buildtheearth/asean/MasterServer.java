package asia.buildtheearth.asean;

import asia.buildtheearth.asean.core.DiscordSRVListener;
import asia.buildtheearth.asean.core.io.LangConfiguration;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.core.scheduler.BukkitScheduler;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.module.Module;
import net.dv8tion.jda.internal.utils.Checks;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class MasterServer extends JavaPlugin {

    public static final String DISCORD_SRV_SYMBOL = "DiscordSRV-Ascension";

    private static MasterServer plugin;

    private final BukkitScheduler scheduler = new BukkitScheduler(this);

    private DiscordSRVListener discordSrvHook = null;

    private YamlConfiguration config;

    private LangConfiguration langConfig;

    private long mainGuildID;

    public static MasterServer getPlugin() {
        return plugin;
    }

    public BukkitScheduler scheduler() {
        return scheduler;
    }

    public LanguageFile getLang() {
        return langConfig.get();
    }

    public LanguageFile getLang(java.util.Locale locale) {
        return langConfig.get(locale);
    }

    public <T, V> Map<Locale, T> getLang(@NotNull V v,
                                         @NotNull BiFunction<LanguageFile, V, T> resolver) {
        return this.langConfig.get(v, resolver);
    }

    /**
     * Main Guild ID set in this plugin's config file<br/>
     * If the file is edited, use {@link #reloadConfig(File)}.
     *
     * @return The main guild as unsigned-long
     */
    public long getMainGuildID() {
        return mainGuildID;
    }

    /**
     * Server's Caching strategy
     * @param <K> Key type
     * @param <V> Value type
     * @return A new caffeine builder
     */
    @SuppressWarnings("unchecked")
    public <K, V> Caffeine<K, V> caffeineBuilder() {
        ExecutorService executor = scheduler().executorService();
        return (Caffeine<K, V>) Caffeine.newBuilder().executor(executor);
    }

    @Override
    public void onEnable() {
        // Initialize plugin reference
        plugin = this;

        // Create configs
        plugin.createConfig();

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

    private void createConfig() {
        // Initial data directory
        File createConfig = new File(getDataFolder(), "config.yml");
        if (!createConfig.exists()) {
            if(createConfig.getParentFile().mkdirs())
                info("Created MasterServer data directory");

            saveResource("config.yml", false);
        }

        // Cache directory
        File cacheDir = new File(getDataFolder(), "cache");
        if (!cacheDir.exists() || (cacheDir.exists() && !cacheDir.isDirectory())) {
            if(cacheDir.mkdirs()) info("Created MasterServer cache directory");
        }

        // Load config from resource to the plugin
        this.config = new YamlConfiguration();
        this.langConfig = new LangConfiguration(this);
        try {
            this.langConfig.initLanguageFiles();
            this.reloadConfig(createConfig);
        } catch (Exception ex) {
            MasterServer.error("Internal Error occurred when loading config file", ex);
        }
    }

    public void reloadConfig(File file) throws IOException, InvalidConfigurationException {
        this.config.load(file);

        String guildConfig = this.config.getString(ConfigPaths.MAIN_GUILD_ID);
        if(guildConfig == null) {
            MasterServer.error(
                "Server's main guild is not set! " +
                "Please see '" + config.getCurrentPath()
                + "' at '" + ConfigPaths.MAIN_GUILD_ID + "'"
            );
            return;
        }

        Checks.isSnowflake(guildConfig);
        this.mainGuildID = Long.parseUnsignedLong(guildConfig);
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
