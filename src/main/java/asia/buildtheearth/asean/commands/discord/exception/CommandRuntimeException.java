package asia.buildtheearth.asean.commands.discord.exception;

import asia.buildtheearth.asean.core.io.LangEntry;

public class CommandRuntimeException extends RuntimeException {
    private final LangEntry lang;

    public CommandRuntimeException(LangEntry lang, String cause) {
        super(cause);

        this.lang = lang;
    }

    public CommandRuntimeException(LangEntry lang) {
        super("Runtime exception trying to execute command.");

        this.lang = lang;
    }

    public LangEntry getLang() {
        return this.lang;
    }

    /**
     * Get the expected permission
     *
     * @return list formatted String
     */
    public String getProblem() {
        return this.getMessage();
    }
}
