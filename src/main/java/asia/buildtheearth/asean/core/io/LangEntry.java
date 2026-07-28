package asia.buildtheearth.asean.core.io;

/**
 * Represent all language configuration providers, available in
 * {@code lang/}{@linkplain java.util.Locale#toLanguageTag() [locale]}.yml
 *
 * <p>We uses test suite to verify and compare definitions over each classes between each language files (.yml).
 * Anonymous implementations will NOT be discoverable because of reflections restrictions.
 * </p>
 *
 * <p>Implementing classes must be statically invokable, conventionally an enum.
 * For classes, they must have a default no-params constructor.</p>
 *
 * @see #getKey()
 */
@FunctionalInterface
public interface LangEntry {

    /**
     * Get the YAML path of this language config
     *
     * @return The YAML path in string
     */
    @org.jetbrains.annotations.NotNull String getKey();
}
