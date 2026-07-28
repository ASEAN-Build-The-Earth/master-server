package asia.buildtheearth.asean.commands.discord.geotools;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.discord.InteractionSession;
import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.discord.abstraction.DiscordInteractionWithModal;
import asia.buildtheearth.asean.discord.api.ComponentDispatcher;
import asia.buildtheearth.asean.discord.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.discord.abstraction.DiscordModalExecution;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static asia.buildtheearth.asean.commands.discord.GeoToolsCommand.GEOJSON_LABEL;
import static asia.buildtheearth.asean.commands.discord.geotools.ExportCommand.*;
import static asia.buildtheearth.asean.commands.discord.AbstractDiscordCommand.CommandWithModal;

public class ExportGeoJSON extends CommandWithModal<Interaction> implements SubCommand {

    private final ExportCommand parent;

    public ExportGeoJSON(ExportCommand parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public MainGuildCommand.Config getCommandConfig() {
        return MainGuildCommand.Config.GEO_TOOLS_EXPORT_GEOJSON;
    }

    @Override
    public boolean requireLinked() {
        return false;
    }

    @Override @NotNull
    protected Task<?> acceptNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                       @NotNull InteractionInstance session) {
        DiscordInteractionWithModal response = execution.getInteraction();
        response.execute(() -> response.send(new Text("WIP")));
        return Task.completed(null);
    }

    @Override @NotNull
    protected Task<Interaction> createNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                                 @Nullable UUID linkedUUID) {
        return Task.completed(null);
    }

    @Override @NotNull
    protected Task<Interaction> acceptModal(@NotNull ReceivedEvent<DiscordModalInteractionEvent> session) {
        DiscordModalExecution.WithHook execution = new DiscordModalExecution.WithHook(plugin, session.getEvent());

        MasterServer.info("Got Modal Interaction");

        return parent.verify(GEOJSON_LABEL, execution, session, session.getEvent().asJDA().getMessage());
    }

    public InteractionSession<?> getSession() {
        return this;
    }

    public java.util.UUID newSession() {
        return java.util.UUID.randomUUID();
    }
}
