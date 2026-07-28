package asia.buildtheearth.asean.commands.discord;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.schematic.GetCommand;
import asia.buildtheearth.asean.commands.discord.schematic.UploadCommand;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;

import static asia.buildtheearth.asean.commands.discord.schematic.AbstractSchematicCommand.Language;

/**
 *  <pre>
 *  /schematic upload - upload schematic file to master server
 *  /schematic get - get owned schematic created by user
 *  </pre>
 */
public class SchematicCommand {
    private static final String LABEL = "schematic";
    private static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("slash-schematic-command");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(MasterServer plugin, DiscordSRV api) {
        if (INSTANCE == null) {
            DiscordCommand.ChatInputBuilder builder = DiscordCommand
                .chatInput(IDENTIFIER, LABEL, plugin.getLang().get(Language.GROUP_DESC));

            INSTANCE = builder
                .addSubCommand(GetCommand.get(plugin, api))
                .addSubCommand(UploadCommand.get(plugin, api))
                .addDescriptionTranslations(plugin.getLang(Language.GROUP_DESC, LanguageFile::get))
                .addNameTranslations(plugin.getLang(Language.GROUP_NAME, LanguageFile::get))
                .setContexts(true, false)
                .setGuildId(plugin.getMainGuildID())
                .setDefaultPermission(DiscordCommand.DefaultAccess.EVERYONE)
                .build();
        }

        return INSTANCE;
    }
}
