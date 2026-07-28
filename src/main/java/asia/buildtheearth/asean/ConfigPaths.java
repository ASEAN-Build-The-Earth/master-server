package asia.buildtheearth.asean;

/**
 * MasterServer plugin's config.yml specifications
 */
public final class ConfigPaths {

    private static final String MAIN_GUILD = "main-guild.";
    /**
     * The discord server's ID (discord snowflake)
     */
    public static final String MAIN_GUILD_ID = MAIN_GUILD + "guild-id";
    /**
     * Accent roles {@code role-id.[name]}
     */
    public static final String MAIN_GUILD_ROLE = MAIN_GUILD + "role-id.";
    /**
     * Main builder role: Official Builder
     */
    public static final String OFFICIAL_BUILDER_ROLE_ID = MAIN_GUILD + "official-builder-role-id";

    // Database
    private static final String DATABASE = "database.";
    public static final String DATABASE_URL = DATABASE + "url";
    public static final String DATABASE_NAME = DATABASE + "name";
    public static final String DATABASE_USERNAME = DATABASE + "username";
    public static final String DATABASE_PASSWORD = DATABASE + "password";
    public static final String DATABASE_TABLE = DATABASE + "table";

    // Discord Slash Commands
    public static final String SLASH_COMMAND = MAIN_GUILD + "slash-commands.";
    public static final String SLASH_SCHEMATIC = SLASH_COMMAND + "slash-schematic.sub-commands.";
    public static final String SLASH_GEO_TOOLS = SLASH_COMMAND + "slash-geotools.sub-commands.";

}
