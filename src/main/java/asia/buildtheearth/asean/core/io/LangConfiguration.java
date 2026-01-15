package asia.buildtheearth.asean.core.io;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.providers.PluginProvider;
import org.bukkit.configuration.InvalidConfigurationException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class LangConfiguration extends PluginProvider {
    private static final Function<Locale, String> RESOURCE = locale -> "lang/" + locale.toLanguageTag() + ".yml";

    private LanguageFile english;

    private Map<String, LanguageFile> lang;

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
        if(this.lang == null) return null;
        return this.lang.get(locale.toLanguageTag());
    }

    /**
     * Load all available language from its YML file configuration.
     *
     * @throws IOException If embedded resource is not found (this should not ever happen)
     * @throws InvalidConfigurationException If embedded resource cannot be loaded.
     */
    public void initLanguageFiles() throws IOException, InvalidConfigurationException {
        // Load config from resource to the plugin
        this.english = new LanguageFile();

        // We'd load more language here if we have budget
        this.tryLoadLang(this.english, Locale.ENGLISH, Locale.UK, Locale.US);
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
    private void tryLoadLang(@NotNull LanguageFile lang,
                             @NotNull Locale locale,
                             Locale... mapping) throws IOException, InvalidConfigurationException {

        String path = RESOURCE.apply(locale);
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
            return;
        }

        // Register new language
        if(this.lang == null) this.lang = new HashMap<>();

        this.lang.put(locale.toLanguageTag(), lang);
        for(Locale map : mapping) this.lang.put(map.toLanguageTag(), lang);
    }
}
