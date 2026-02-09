package asia.buildtheearth.asean.commands.abstraction;

import asia.buildtheearth.asean.core.providers.PluginForDiscordSRV;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;

import java.util.function.Consumer;

public abstract class AbstractDiscordCommandWithSession<T extends Record>
    extends InteractionSession<T>
    implements Consumer<DiscordChatInputInteractionEvent> {

    public AbstractDiscordCommandWithSession(PluginForDiscordSRV plugin) {
        super(plugin);
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        execute(new DiscordCommandExecution(this.plugin, event));
    }

    public abstract void execute(DiscordCommandExecution execution);
}
