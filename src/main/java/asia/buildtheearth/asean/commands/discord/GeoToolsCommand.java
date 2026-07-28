package asia.buildtheearth.asean.commands.discord;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.geotools.DrawCommand;
import asia.buildtheearth.asean.commands.discord.geotools.ExportCommand;
import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.DiscordSRV;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Server related utility commands. (prefixed /srv)
 *
 * <pre>
 * /geotools draw - draw compatible geo file on to server as outlines using WorldEdit
 * /geotools export {file} - export geo file from available file type
 * </pre>
 */
public class GeoToolsCommand {

    private static final String LABEL = "geotools";
    private static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("slash-geotools-command");

    public static final String EXPORT_LABEL = "export";
    public static final String EXPORT_CMD = "export-";
    public static final String SCHEMATIC_LABEL = "schematic";
    public static final String GEOJSON_LABEL = "geojson";
    public static final String KML_LABEL = "kml";

    public static final String DRAW_LABEL = "draw";

    public static final String NORMALIZE_Z_LABEL = "normalize-z";
    public static final String HEIGHT_LABEL = "height";
    public static final String OFFSET_Z_LABEL = "offset-z";
    public static final String PATTERN_LABEL = "pattern";
    public static final String PRIVATE_LABEL = "private";
    public static final String DROP_Z_LABEL = "drop-z";
    public static final String FILE_LABEL = "file";

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(MasterServer plugin, DiscordSRV api) {
        if (INSTANCE == null) {
            DiscordCommand.ChatInputBuilder builder = DiscordCommand
                .chatInput(IDENTIFIER, LABEL, plugin.getLang().get(Language.GROUP_DESC))
                .addDescriptionTranslations(plugin.getLang(Language.GROUP_DESC, LanguageFile::get));

            // Command instances
            ExportCommand export = new ExportCommand(plugin, api);
            DrawCommand draw = export.getDrawCommand();

            // Common option arguments
            CommandOption file = CommandOption
                .builder(CommandOption.Type.ATTACHMENT, FILE_LABEL, plugin.getLang().get(Language.OPTION_FILE))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_FILE, LanguageFile::get))
                .build();

            CommandOption height = CommandOption
                .builder(CommandOption.Type.DOUBLE, HEIGHT_LABEL, plugin.getLang().get(Language.OPTION_HEIGHT))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_HEIGHT, LanguageFile::get))
                .build();

            CommandOption pattern = CommandOption
                .builder(CommandOption.Type.STRING, PATTERN_LABEL, plugin.getLang().get(Language.OPTION_PATTERN))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_PATTERN, LanguageFile::get))
                .build();

            CommandOption ephemeral = CommandOption
                .builder(CommandOption.Type.BOOLEAN, PRIVATE_LABEL, plugin.getLang().get(Language.OPTION_PRIVATE))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_PRIVATE, LanguageFile::get))
                .build();

            INSTANCE = builder
                .addSubCommandGroup(ExportCommand.get(export, pattern, ephemeral, file, height))
                .addSubCommand(DrawCommand.get(draw, pattern, ephemeral, file, height))
                .setContexts(true, false)
                .setGuildId(plugin.getMainGuildID())
                .setDefaultPermission(DiscordCommand.DefaultAccess.EVERYONE)
                .build();
        }

        return INSTANCE;
    }

    enum Language implements LangEntry {
        // Parent Command Info
        GROUP_DESC("description"),
        GROUP_NAME("name"),

        // Command Options
        OPTION_PATTERN( "options", "pattern"),
        OPTION_PRIVATE( "options", "private"),
        OPTION_HEIGHT(  "options", "height"),
        OPTION_FILE(    "options", "file"),
        ;

        private static final String PARENT_PATH = "slash-commands.geo-tools.";
        private final String path;

        Language(String... path) {
            this.path = String.join(".", path);
        }

        @Override @Contract(pure = true)
        public @NotNull String getKey() {
            return PARENT_PATH + path;
        }

    }
}