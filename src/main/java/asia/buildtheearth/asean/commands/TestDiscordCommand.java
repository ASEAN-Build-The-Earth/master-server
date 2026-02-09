package asia.buildtheearth.asean.commands;


import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.subcommands.ExportFileCommand;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.DiscordSRV;

public class TestDiscordCommand {

    private static final String LABEL = "testing";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("MasterServer", "testing");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(MasterServer plugin, DiscordSRV api) {
        if (INSTANCE == null) {
            DiscordCommand.ChatInputBuilder builder = DiscordCommand
                .chatInput(IDENTIFIER, LABEL, "Test Commands");

            INSTANCE = builder
                .addSubCommandGroup(ExportFileCommand.get(plugin, api))
                .setContexts(true, false)
                .setGuildId(plugin.getMainGuildID())
                .setDefaultPermission(DiscordCommand.DefaultAccess.ADMINISTRATOR)
                .build();
        }

        return INSTANCE;
    }
}