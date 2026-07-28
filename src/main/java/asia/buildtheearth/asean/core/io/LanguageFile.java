package asia.buildtheearth.asean.core.io;

import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class LanguageFile extends YamlConfiguration {
    private final Locale locale;

    public LanguageFile(Locale locale) {
        this.locale = locale;
    }

    public static final String NULL_LANG = "undefined";

    public Locale getLocale() {
        return this.locale;
    }

    @NotNull
    public String get(@NotNull String key) {
        return this.getString(key, getNull(key));
    }

    @NotNull
    public <T extends LangEntry> String get(@NotNull T entry) {
        return this.getString(entry.getKey(), getNull(entry));
    }

    @NotNull
    public LangPair<String> getPair(@NotNull String key,
                                    @Nullable String defaultValue) {
        java.util.List<String> lang = this.getStringList(key);

        String title = !lang.isEmpty() ? lang.getFirst() : defaultValue;
        String description = lang.size() > 1 ? lang.get(1) : defaultValue;

        return new LangPair<>(title, description);
    }

    @NotNull
    public <T extends LangEntry> LangPair<String> getPair(@NotNull T entry) {
        return this.getPair(entry.getKey(), getNull(entry));
    }

    @NotNull
    public <T extends LangEntry> LangPair<String> getPair(@NotNull T entry,
                                                          @Nullable String defaultValue) {
        return this.getPair(entry.getKey(), defaultValue);
    }

    /**
     * Returns {@value #NULL_LANG}-{@linkplain Integer#toHexString}.
     *
     * @param key Any object with functionable {@link #hashCode()} method
     * @return Default fallback lang concatenated by the key's hashcode.
     * @param <T> Type of the null value
     */
    @Contract("_ -> new")
    private <T> @NotNull String getNull(@NotNull T key) {
        return String.join("-", NULL_LANG, Integer.toHexString(key.hashCode()));
    }

    @Contract("_ -> new")
    private @NotNull String getNull(@NotNull LangEntry entry) {
        return getNull(entry.getKey());
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}
