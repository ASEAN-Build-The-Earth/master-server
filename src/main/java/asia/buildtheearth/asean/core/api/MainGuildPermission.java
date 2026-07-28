package asia.buildtheearth.asean.core.api;

/**
 * Strict permission handled separately from roles.
 */
public enum MainGuildPermission {
    /**
     * Accept every check
     */
    NONE,
    /**
     * Reject every check
     */
    DENY,
    /**
     * member#isOwner() check
     */
    OWNER,
    /**
     * JDA {@link net.dv8tion.jda.api.Permission#ADMINISTRATOR} check
     */
    ADMINISTRATOR,
    /**
     *  Verified, Trusted builders
     */
    OFFICIAL_BUILDER,

    // LINKED_OFFICIAL_BUILDER; // TODO: Should I ???
}
