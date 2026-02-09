package asia.buildtheearth.asean.commands.subcommands;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.commands.abstraction.AbstractDiscordCommandWithSession;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import static asia.buildtheearth.asean.commands.subcommands.ExportFileCommand.*;

public class ExportGeoJSON extends AbstractDiscordCommandWithSession<Interaction> {

    private final ExportFileCommand parent;

    public ExportGeoJSON(ExportFileCommand parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void execute(DiscordCommandExecution execution) {
         parent.execute(ExportFileCommand.GEOJSON_LABEL, execution);
    }

    @Override
    protected ExportFileCommand.Interaction fromModalInteractionEvent(@NotNull DiscordModalInteractionEvent event,
                                                                      @Nullable Interaction session) {
        MasterServer.info("Got Modal Interaction");

        return session;
    }
}
