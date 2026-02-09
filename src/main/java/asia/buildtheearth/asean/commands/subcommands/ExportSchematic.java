package asia.buildtheearth.asean.commands.subcommands;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.abstraction.*;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import static asia.buildtheearth.asean.commands.subcommands.ExportFileCommand.*;

public class ExportSchematic
    extends AbstractDiscordCommandWithSession<ExportFileCommand.Interaction> {

    private final ExportFileCommand parent;

    public ExportSchematic(ExportFileCommand parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void execute(DiscordCommandExecution execution) {
        parent.execute(ExportFileCommand.SCHEMATIC_LABEL, execution);
    }

    @Override
    protected Interaction fromModalInteractionEvent(@NotNull DiscordModalInteractionEvent event,
                                                    @Nullable Interaction session) {
        MasterServer.info("Got Modal Interaction");

        return session;
    }
}
