package asia.buildtheearth.asean.core.io;

import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class LanguageFile extends YamlConfiguration {

    public static final String NULL_LANG = "undefined";

    public record EmbedLang(String title, String description) {
        /**
         * Consume The information "as" something else.
         *
         * @param consumer consumer function
         * @param <T> Result of the consumption
         * @return The consumer {@linkplain BiFunction#apply(Object, Object) applied}
         */
        public <T> T as(@NotNull BiFunction<String, String, T> consumer) {
            return consumer.apply(title(), description());
        }
    };

    @NotNull
    public String get(@NotNull String key) {
        return this.getString(key, NULL_LANG);
    }

    @NotNull
    public <T extends LangEntry> String get(@NotNull T config) {
        return this.getString(config.getKey(), NULL_LANG);
    }

    @NotNull
    public EmbedLang getEmbed(@NotNull String key,
                              @Nullable String defaultValue) {
        List<String> lang = this.getStringList(key);

        String title = !lang.isEmpty() ? lang.getFirst() : defaultValue;
        String description = lang.size() > 1 ? lang.get(1) : defaultValue;

        return new EmbedLang(title, description);
    }

    @NotNull
    public <T extends LangEntry> EmbedLang getEmbed(@NotNull T config) {
        return this.getEmbed(config.getKey(), NULL_LANG);
    }

    @NotNull
    public <T extends LangEntry> EmbedLang getEmbed(@NotNull T config,
                                                    @Nullable String defaultValue) {
        return this.getEmbed(config.getKey(), defaultValue);
    }

    @NotNull
    public <T extends LangEntry> EmbedBuilder getEmbedBuilder(@NotNull T config,
                                                              @NotNull Function<String, String> title,
                                                              @NotNull Function<String, String> description) {
        List<String> lang = this.getStringList(config.getKey());
        EmbedBuilder embed = new EmbedBuilder();

        if(!lang.isEmpty()) embed.setTitle(title.apply(lang.getFirst()));
        if(lang.size() > 1) embed.setDescription(description.apply(lang.get(1)));

        return embed;
    }

    @NotNull
    public <T extends LangEntry> EmbedBuilder getEmbedBuilder(@NotNull T config) {
        return this.getEmbedBuilder(config, Function.identity(), Function.identity());
    }

    @NotNull
    public <T extends LangEntry> EmbedBuilder getEmbedBuilder(@NotNull T config,
                                                              @NotNull Function<String, String> description) {
        return this.getEmbedBuilder(config, Function.identity(), description);

    }
}
