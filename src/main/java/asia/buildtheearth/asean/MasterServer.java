package asia.buildtheearth.asean;

import asia.buildtheearth.asean.core.DiscordSRVModule;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.api.MainGuildPermission;
import asia.buildtheearth.asean.core.api.MainGuildRole;
import asia.buildtheearth.asean.core.api.PermissionConfig;
import asia.buildtheearth.asean.core.io.LangConfiguration;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.core.scheduler.BukkitScheduler;
import asia.buildtheearth.asean.utils.function.CheckedConsumer;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.module.Module;
import net.dv8tion.jda.internal.utils.Checks;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MasterServer extends JavaPlugin {

    public static final String DISCORD_SRV_SYMBOL = "DiscordSRV-Ascension";

    private static MasterServer plugin;

    private final BukkitScheduler scheduler = new BukkitScheduler(this);

    private final DiscordSRVModule discordSRVModule = new DiscordSRVModule(this);

    private YamlConfiguration config;

    private LangConfiguration langConfig;

    private Long mainGuildID;

    private Long builderRole;

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

    public Stream<Locale> getAvailableLocale() {
        return this.langConfig.available();
    }

    /**
     * Server's Caching strategy
     *
     * @return A new caffeine builder
     */
    public Caffeine<Object, Object> caffeineBuilder() {
        ExecutorService executor = scheduler().executorService();

        return Caffeine.newBuilder().executor(executor);
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
        if (discordSRVModule.isSubscribed()) {
            error("Already subscribed to DiscordSRV. Did the server reload?");
            return;
        }

        DiscordSRV.optional().ifPresentOrElse(api -> {
            // If DiscordSRV JDA is ready before this plugin finish initializing
            discordSRVModule.initialize(api);
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

    public void reloadLang(Locale @NotNull ... locale) throws IOException, InvalidConfigurationException {
        Set<LanguageFile> files = new HashSet<>();
        for(Locale lang : locale)
            files.add(this.langConfig.get(lang));

        for(LanguageFile file : files)
            this.langConfig.tryLoadLang(file, LangConfiguration.getPath(file.getLocale()));
    }

    public void reloadConfig(File file) throws IOException, InvalidConfigurationException {
        this.config.load(file);

        this.mainGuildID = this.getMainGuildConfig("Main Guild ID", ConfigPaths.MAIN_GUILD_ID);
        this.builderRole = this.getMainGuildConfig("Official Builder Role ID", ConfigPaths.OFFICIAL_BUILDER_ROLE_ID);
    }

    public @NotNull Set<Long> getMainGuildRole(@NotNull MainGuildRole entry) {
        String name = entry.getName();
        String path = entry.getConfigPath();
        Set<Long> set = new HashSet<>();
        Consumer<String> consumer = snowflake -> {
            Long roleID = parseSnowflakeConfig(snowflake, ex -> MasterServer.error(
                "Role ID of " + name + " is incorrectly set! Please see config.yml '" + path + "'", ex));
            if(roleID != null) set.add(roleID);
        };

        if (this.config.isList(path))
            this.config.getStringList(path).forEach(consumer);

        if (this.config.isString(path)) {
            String value = this.config.getString(path, null);
            if(value != null && !value.isBlank())
                for(String snowflake : value.split(", "))
                    consumer.accept(snowflake);
        }

        return Collections.unmodifiableSet(set);
    }

    public @Nullable Long getMainGuildConfig(String label, String path) {
        String snowflake = this.config.getString(path, null);
        if(snowflake != null && !snowflake.isBlank()) {
            return parseSnowflakeConfig(snowflake, ex -> MasterServer.error(
            "Server's " + label + " is incorrectly set! Please see config.yml '" + path + "'", ex));
        }

        MasterServer.error("Server's " + label + " is not set! Please see config.yml '" + path + "'");
        return null;
    }

    @NotNull
    public MainGuildCommand parseCommandConfig(@NotNull PermissionConfig command) {
        String configPath = command.getConfigPath();
        MainGuildPermission defaultPermission = command.getDefaultPermission();
        Object value = this.config.get(configPath);

        // shortcut command: PERMISSION
        if (value instanceof String permissionValue) {
            try {
                MainGuildPermission permission = MainGuildPermission.valueOf(permissionValue.toUpperCase());
                return MainGuildCommand.of(permission);
            }
            catch (Throwable ex) {
                MasterServer.error("Permission config for slash command '"
                    + configPath + "' is invalid (" + ex.getMessage() + ")");
            }
        }

        if (value instanceof ConfigurationSection child)
            if (child.contains("permission") || child.contains("role")) {
                try {
                    String permissionValue = child.getString("permission", defaultPermission.name());
                    MainGuildPermission permission = MainGuildPermission.valueOf(permissionValue.toUpperCase());
                    EnumSet<MainGuildRole> roles = parseRoles(child);

                    return MainGuildCommand.of(permission, roles);
                }
                catch (Throwable ex) {
                    MasterServer.error("Permission config for slash command '"
                        + configPath + "' is invalid (" + ex.getMessage() + ")");
                }
            }

        return MainGuildCommand.of(defaultPermission);
    }

    @NotNull
    private EnumSet<MainGuildRole> parseRoles(@NotNull ConfigurationSection config) throws Throwable {
        EnumSet<MainGuildRole> roles = EnumSet.noneOf(MainGuildRole.class);

        CheckedConsumer<String> consumer = name -> {
            roles.add(MainGuildRole.valueOf(name.toUpperCase()));
        };

        if (config.isList("role"))
            for(String name : config.getStringList("role"))
                consumer.accept(name);

        if (config.isString("role")) {
            String value = config.getString("role", null);
            if(value != null && !value.isBlank())
                for(String name : value.split(", "))
                    consumer.accept(name);
        }

        return roles;
    }

    private @Nullable Long parseSnowflakeConfig(@NotNull String value,
                                                @NotNull Consumer<Throwable> error) {
        try {
            Checks.isSnowflake(value, value);
            return Long.parseUnsignedLong(value);
        }
        catch (Throwable ex) {
            error.accept(ex);
            return null;
        }
    }

    @NotNull
    public DiscordSRVModule discordSRV() {
        return this.discordSRVModule;
    }

    /**
     *
     * @return snowflake, null if not configured
     */
    public @Nullable Long getBuilderRoleID() {
        return this.builderRole;
    }

    /**
     *
     * @return snowflake, null if not configured
     */
    public @Nullable Long getMainGuildID() {
        return this.mainGuildID;
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
