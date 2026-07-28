package asia.buildtheearth.asean.core.io;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.abstraction.AbstractPluginProvider;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class LangConfiguration extends AbstractPluginProvider {
    private LanguageFile english;

    private Map<String, LanguageFile> registry;

    @NotNull
    public static String getPath(@NotNull Locale locale) {
        return "lang/" + locale.toLanguageTag() + ".yml";
    }

    public LangConfiguration(MasterServer plugin) {
        super(plugin);
    }

    /**
     * Get the default locale ({@link Locale#ENGLISH})
     * @return Language file definition
     */
    public LanguageFile get() {
        return this.english;
    }

    /**
     * Get language file by locale, or {@code null}
     * @param locale Locale to get (This will use {@link Locale#toLanguageTag()})
     * @return  Language file definition or {@code null}
     */
    public LanguageFile get(@NotNull Locale locale) {
        if(this.registry == null) return null;
        return this.registry.get(locale.toLanguageTag());
    }

    /**
     * Collect all language in all registered locales.
     * @param key Config key to be resolved
     * @param resolver Resolving function to collect each locale to
     * @return  Language file definition or {@code null}
     * @param <K> Key type to get
     * @param <V> The resolving function of type {@link LanguageFile}
     */
    public <K, V> Map<Locale, V> get(@NotNull K key,
                                     @NotNull BiFunction<LanguageFile, K, V> resolver) {
        Map<Locale, V> map = new HashMap<>();

        if(this.registry != null) {
            this.registry.forEach((tag, file) -> {
                Locale locale = Locale.forLanguageTag(tag);
                V value = resolver.apply(file, key);

                if (value == null) return;

                if (value instanceof String seq && seq.isBlank()) return;

                map.put(locale, value);
            });
        }

        return Collections.unmodifiableMap(map);
    }

    public Stream<Locale> available() {
        return registry.keySet().stream().map(Locale::forLanguageTag);
    }

    /**
     * Load all available language from its YML file configuration.
     *
     * @throws IOException If embedded resource is not found (this should not ever happen)
     * @throws InvalidConfigurationException If embedded resource cannot be loaded.
     */
    public void initLanguageFiles() throws IOException, InvalidConfigurationException {
        // Load config from resource to the plugin
        Locale primary = Locale.ENGLISH;
        this.english = new LanguageFile(primary);

        // We'd load more language here if we have budget
        this.tryLoadLang(this.english, getPath(primary));
        this.putLanguageRegistry(this.english, primary, Locale.UK, Locale.US);
    }

    /**
     * Load new language file to the configuration and register it as new available mapping.
     *
     * @param lang The language file to load
     * @param locale The primary locale which will be loaded in at {@code lang/[locale].yml}
     * @param mapping additional mapping for this locale
     * @throws IOException If embedded resource is not found (this should not ever happen)
     * @throws InvalidConfigurationException If embedded resource cannot be loaded.
     */
    public void tryLoadLang(@NotNull LanguageFile lang,
                             @NotNull String path) throws IOException, InvalidConfigurationException {
        File file = new File(this.plugin.getDataFolder(), path);
        if (!file.exists())
            this.plugin.saveResource(path, false);

        try { lang.load(file); }
        catch (Exception ex) {
            InputStream resourceData = this.plugin.getResource(path);
            if(resourceData == null) throw new IOException(
                "Fallback method to load " + path + " from resource failed with null value"
            );
            MasterServer.error("System Language File failed to load from data folder, falling back to embedded resource data.");
            lang.load(new InputStreamReader(resourceData));
        }
    }

    private void putLanguageRegistry(@NotNull LanguageFile lang,
                                     @NotNull Locale locale,
                                     Locale... compatible) {
        // Register new language
        if(this.registry == null) this.registry = new HashMap<>();

        this.registry.put(locale.toLanguageTag(), lang);
        for(Locale map : compatible) this.registry.put(map.toLanguageTag(), lang);
    }
}
