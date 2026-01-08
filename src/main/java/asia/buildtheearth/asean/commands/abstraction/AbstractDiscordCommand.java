package asia.buildtheearth.asean.commands.abstraction;

import asia.buildtheearth.asean.MasterServer;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;

import java.util.function.Consumer;

public abstract class AbstractDiscordCommand implements Consumer<DiscordChatInputInteractionEvent> {
    protected final MasterServer plugin;

    public AbstractDiscordCommand(MasterServer discordSRV) {
        this.plugin = discordSRV;
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        execute(new DiscordCommandExecution(plugin, event));
    }

    public abstract void execute(CommandExecution execution);
}
