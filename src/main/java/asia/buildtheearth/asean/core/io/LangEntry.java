package asia.buildtheearth.asean.core.io;

/**
 * Represent all language configuration providers
 *
 * @see #getKey()
 */
public interface LangEntry {

    /**
     * Get the YAML path of this language config
     *
     * @return The YAML path in string
     */
    @org.jetbrains.annotations.NotNull String getKey();
}
