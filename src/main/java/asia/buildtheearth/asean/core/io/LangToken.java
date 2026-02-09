package asia.buildtheearth.asean.core.io;

/**
 * Formatter tokens used in language file messages,
 * replaced at runtime with relevant values.
 *
 * <p>The available constants define how each formatting token is used in its context.</p>
 */
public interface LangToken {
    /** Replaced with the formatted name of the plot's owner. */
    String OWNER = "{owner}";

    /** Replaced with the Discord thread's snowflake ID. */
    String THREAD_ID = "{threadID}";

    /** Replaced with the Discord message's snowflake ID. */
    String MESSAGE_ID = "{messageID}";

    /** Replaced with the Discord member's user ID. */
    String USER_ID = "{userID}";

    /** Replaced with a Discord snowflake timestamp. */
    String TIMESTAMP = "{timestamp}";

    /** Replaced with the filename of a referenced {@link java.io.File}. */
    String FILE_NAME = "{filename}";

    /** Replaced with the type of file of a referenced {@link java.io.File}. */
    String FILE_TYPE = "{file}";

    /** Replaced with the absolute path of a referenced file. */
    String PATH = "{path}";

    /** Replaced with a custom-defined label message. */
    String LABEL = "{label}";

    /** Replaced with the simple class name of an event (usually {@link Class#getSimpleName()}). */
    String EVENT = "{event}";

    /** Replaced with an incrementing counter or index. */
    String COUNT = "{count}";

    /** Replaced with country name. */
    String COUNTRY = "{country}";

    /** Replaced with city project name. */
    String CITY = "{city}";
}
