package asia.buildtheearth.asean.core.api;

import asia.buildtheearth.asean.ConfigPaths;

/**
 * Optional accent roles for specifying as command's allowed permissions.
 * List of ids are also allowed
 */
public enum MainGuildRole implements PermissionConfig {
    /**
     * Staff role
     */
    STAFF("staff", MainGuildPermission.ADMINISTRATOR),
    /**
     * Normal member roles
     */
    MEMBER("member", MainGuildPermission.NONE),
    /**
     * Plot builders, Trial builders, Tutorial builders, Builder from other team(S)
     */
    BUILDER("builder", MainGuildPermission.OFFICIAL_BUILDER),
    /**
     * Developer role
     */
    DEVELOPER("developer", MainGuildPermission.OWNER);

    private final String configName;
    private final MainGuildPermission defaultPermission;

    /**
     * Main guild's role preset
     *
     * @param configName Name of this role as written in config.yml
     * @param defaultPermission Estimate permission, in-case actual value is not found.
     */
    MainGuildRole(String configName, MainGuildPermission defaultPermission) {
        this.configName = configName;
        this.defaultPermission = defaultPermission;
    }

    @Override
    public String getName() {
        return this.configName;
    }

    @Override
    public String getPath() {
        return ConfigPaths.MAIN_GUILD_ROLE;
    }

    @Override
    public MainGuildPermission getDefaultPermission() {
        return defaultPermission;
    }


}
