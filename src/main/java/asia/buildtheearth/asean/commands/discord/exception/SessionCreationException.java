package asia.buildtheearth.asean.commands.discord.exception;

import asia.buildtheearth.asean.core.io.LangEntry;

public class SessionCreationException extends CommandRuntimeException {
    public SessionCreationException(LangEntry lang) {
        super(lang);
    }
}
