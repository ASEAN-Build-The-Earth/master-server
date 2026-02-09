package asia.buildtheearth.asean.core;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.TestDiscordCommand;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;

public class DiscordSRVListener extends AbstractModule {
    private final MasterServer plugin;

    public DiscordSRVListener(MasterServer plugin, DiscordSRV discordSRV) {
        super(plugin, discordSRV);
        this.plugin = plugin;
    }

    @Override
    public void enable() {
        MasterServer.info("JDA has started, subscribing...");
        subscribeAndValidateJDA();
    }


    public void subscribeAndValidateJDA() {

        DiscordCommand.RegistrationResult result = this.getDiscordAPI()
            .registerCommand(TestDiscordCommand.get(this.plugin, this.api));

        // REGISTERED / ALREADY_REGISTERED / NAME_ALREADY_IN_USE / TOO_MANY_COMMANDS
        MasterServer.info("Command Registration: " + result.name());
    }

}
