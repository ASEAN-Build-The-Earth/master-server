package asia.buildtheearth.asean.commands;


import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.DiscordSRV;

public class TestDiscordCommand {

    private static final String LABEL = "testing";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("MasterServer", "testing");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(MasterServer plugin) {
        if (INSTANCE == null) {
            DiscordCommand.ChatInputBuilder builder = DiscordCommand
                .chatInput(IDENTIFIER, LABEL, "Test Commands");

            INSTANCE = builder
                    .setContexts(true, false)
                    .setGuildId(plugin.getMainGuildID())
                    .setDefaultPermission(DiscordCommand.DefaultAccess.ADMINISTRATOR)
                    .build();
        }

        return INSTANCE;
    }
}