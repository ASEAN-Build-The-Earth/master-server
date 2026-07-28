package asia.buildtheearth.asean.core.io;

import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represent common pair of type as used in language file.
 *
 * @param primary The first pair entry
 * @param secondary The second pair entry
 * @param <S> Character sequence, normally String
 */
public record LangPair<S extends CharSequence>(S primary, S secondary) {
    /**
     * Consume The information "as" something else.
     *
     * @param consumer pair consumer function
     * @param <T>      Result of the consumption
     * @return The consumer {@linkplain BiFunction#apply(Object, Object) applied}
     */
    public <T> T as(@NotNull BiFunction<S, S, T> consumer) {
        return consumer.apply(this.primary(), this.secondary());
    }

    /**
     * Modify this pair using modifier function and return a new.
     *
     * @param primary modifier function to apply for the primary entry
     * @param secondary modifier function to apply for the secondary entry
     * @return The return value of both modifier function as a new Language Pair
     */
    @Contract("_, _ -> new")
    public @NotNull LangPair<S> with(@NotNull Function<S, S> primary,
                                     @NotNull Function<S, S> secondary) {
        return new LangPair<>(primary.apply(this.primary), secondary.apply(this.secondary));
    }

    @NotNull
    public EmbedBuilder asEmbedBuilder(@NotNull Function<S, S> title,
                                       @NotNull Function<S, S> description) {
        return this.as((primary, secondary) -> {
            EmbedBuilder embed = new EmbedBuilder();

            if (primary != null) embed.setTitle((String) title.apply(primary));
            if (secondary != null) embed.setDescription(description.apply(secondary));

            return embed;
        });
    }

    @NotNull
    public EmbedBuilder asEmbedBuilder() {
        return this.asEmbedBuilder(Function.identity(), Function.identity());
    }

    @NotNull
    public EmbedBuilder asEmbedBuilder(@NotNull Function<S, S> description) {
        return this.asEmbedBuilder(Function.identity(), description);
    }
}
