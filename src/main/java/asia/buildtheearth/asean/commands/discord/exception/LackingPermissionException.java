package asia.buildtheearth.asean.commands.discord.exception;

import asia.buildtheearth.asean.core.io.LangEntry;

public class LackingPermissionException extends CommandRuntimeException {
    private final String cause;

    public LackingPermissionException(LangEntry lang, String cause) {
        super(lang, "Lacking required permission '" + cause + "'");

        this.cause = cause;
    }

    /**
     * Get the expected permission
     *
     * @return list formatted String
     */
    public String getProblem() {
        return cause;
    }
}
