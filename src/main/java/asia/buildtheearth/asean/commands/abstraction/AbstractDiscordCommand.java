package asia.buildtheearth.asean.commands.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.providers.PluginProvider;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;

import java.util.function.Consumer;

public abstract class AbstractDiscordCommand extends PluginProvider implements Consumer<DiscordChatInputInteractionEvent> {
    public AbstractDiscordCommand(MasterServer plugin) {
        super(plugin);
    }

    public AbstractDiscordCommand(PluginProvider plugin) {
        super(plugin);
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        execute(new DiscordCommandExecution(this.plugin, event));
    }

    public abstract void execute(DiscordCommandExecution execution);
}
