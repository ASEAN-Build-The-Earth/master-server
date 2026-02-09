package asia.buildtheearth.asean.commands.subcommands;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.abstraction.ComponentForBTE;
import asia.buildtheearth.asean.commands.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.commands.abstraction.InteractionSession;
import asia.buildtheearth.asean.core.io.LangToken;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.providers.PluginForDiscordSRV;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.command.SubCommandGroup;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;

import com.discordsrv.api.discord.entity.interaction.component.component.LabelComponent;
import com.discordsrv.api.discord.entity.interaction.component.component.ModalComponent;
import com.discordsrv.api.discord.entity.interaction.component.impl.*;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Optional;
import java.util.function.Supplier;

public class ExportFileCommand extends PluginForDiscordSRV {
    static final String LABEL = "export";
    static final String PREFIX = "export-";
    static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("export");

    static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;
    static final String SCHEMATIC_LABEL = "schematic";
    static final String GEOJSON_LABEL = "geojson";
    static final String KML_LABEL = "kml";

    static final String IDENTIFIER_MODAL = "modal";

    static final ComponentIdentifier IDENTIFIER_SCHEMATIC = ComponentForBTE.of(PREFIX + SCHEMATIC_LABEL);
    static final ComponentIdentifier IDENTIFIER_GEOJSON = ComponentForBTE.of(PREFIX + GEOJSON_LABEL);
    static final ComponentIdentifier IDENTIFIER_KML = ComponentForBTE.of(PREFIX + KML_LABEL);

    static final ComponentIdentifier MODAL_PATTERN = ComponentForBTE.of("modal-pattern");
    static final ComponentIdentifier MODAL_HEIGHT = ComponentForBTE.of("modal-height");
    static final ComponentIdentifier MODAL_HEIGHT_OPTION = ComponentForBTE.of("modal-z-placement");
    static final ComponentIdentifier MODAL_FILE_HINTS = ComponentForBTE.of("modal-file-hints");
    static final ComponentIdentifier MODAL_FILE = ComponentForBTE.of("modal-file-upload");

    static final String NORMALIZE_Z_LABEL = "normalize-z";
    static final String HEIGHT_LABEL = "height";
    static final String OFFSET_Z_LABEL = "offset-z";
    static final String PATTERN_LABEL = "pattern";
    static final String DROP_Z_LABEL = "drop-z";
    static final String FILE_LABEL = "file";

    private static SubCommandGroup INSTANCE;
    private final ExportSchematic schematic;
    private final ExportGeoJSON geojson;
    private final ExportKML kml;

    public static SubCommandGroup get(MasterServer plugin,
                                      DiscordSRV api) {
        if (INSTANCE == null) {
            // region Construction
            ExportFileCommand command = new ExportFileCommand(plugin, api);

            CommandOption file = CommandOption
                .builder(CommandOption.Type.ATTACHMENT, FILE_LABEL, plugin.getLang().get(Language.OPTION_FILE))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_FILE, LanguageFile::get))
                .build();

            CommandOption height = CommandOption
                .builder(CommandOption.Type.LONG, HEIGHT_LABEL, plugin.getLang().get(Language.OPTION_HEIGHT))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_HEIGHT, LanguageFile::get))
                .build();

            CommandOption pattern = CommandOption
                .builder(CommandOption.Type.STRING, PATTERN_LABEL, plugin.getLang().get(Language.OPTION_PATTERN))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_PATTERN, LanguageFile::get))
                .build();

            INSTANCE = SubCommandGroup.builder(LABEL, plugin.getLang().get(Language.GROUP_DESC))
                .addNameTranslations(plugin.getLang(Language.GROUP_NAME, LanguageFile::get))
                .addDescriptionTranslations(plugin.getLang(Language.GROUP_DESC, LanguageFile::get))
                .addCommand(DiscordCommand.chatInput(IDENTIFIER_GEOJSON, GEOJSON_LABEL, plugin.getLang().get(Language.CMD_1_DESC))
                    .addDescriptionTranslations(plugin.getLang(Language.CMD_1_DESC, LanguageFile::get))
                    .addOption(file)
                    .addOption(height)
                    .setEventHandler(command.geojson)
                    .build())
                .addCommand(DiscordCommand.chatInput(IDENTIFIER_SCHEMATIC, SCHEMATIC_LABEL, plugin.getLang().get(Language.CMD_2_DESC))
                    .addDescriptionTranslations(plugin.getLang(Language.CMD_2_DESC, LanguageFile::get))
                    .addOption(file)
                    .addOption(pattern)
                    .addOption(height)
                    .setEventHandler(command.schematic)
                    .build())
                .addCommand(DiscordCommand.chatInput(IDENTIFIER_KML, KML_LABEL, plugin.getLang().get(Language.CMD_3_DESC))
                    .addDescriptionTranslations(plugin.getLang(Language.CMD_3_DESC, LanguageFile::get))
                    .addOption(file)
                    .addOption(height)
                    .setEventHandler(command.kml)
                    .build())
                .build();
            // endregion
        }

        return INSTANCE;
    }

    public ExportFileCommand(MasterServer plugin,
                             DiscordSRV api) {
        super(plugin, api);
        this.schematic = new ExportSchematic(this);
        this.geojson = new ExportGeoJSON(this);
        this.kml = new ExportKML(this);
        api.registerModule(this.schematic);
        api.registerModule(this.geojson);
        api.registerModule(this.kml);
    }

    public void execute(@NotNull String label,
                        @NotNull DiscordCommandExecution execution) {
        execution.executeModal(() -> handle(label, execution));
    }

    private DiscordModal handle(@NotNull String command,
                                @NotNull DiscordCommandExecution execution) {
        LanguageFile lang = plugin.getLang(execution.locale());
        Interaction interaction = new Interaction(
            execution.getAttachment(FILE_LABEL),
            execution.getLong(HEIGHT_LABEL),
            execution.getString(PATTERN_LABEL)
        );

        // region Session
        // SubCommand session specific options:
        // - Display different title based on what file we're exporting
        // - Exporting schematic file require Minecraft block pattern
        // - Additional hints for height placement and file input format
        String title;
        ModalComponent<?> patternInputLabel = null;
        StringSelectMenu.Builder zInput = StringSelectMenu.create(MODAL_HEIGHT_OPTION.getDiscordIdentifier());
        Language.EntryConsumer<String> zOption = (label, desc) -> value -> zInput.addOption(label, value, desc);

        lang.getEmbed(Language.Modal.MODAL_Z_NORMALIZE).as(zOption).accept(NORMALIZE_Z_LABEL);
        lang.getEmbed(Language.Modal.MODAL_Z_OFFSET).as(zOption).accept(OFFSET_Z_LABEL);
        zInput.setDefaultValues(NORMALIZE_Z_LABEL);

        StringSelectMenu.Builder hintInput = StringSelectMenu.create(MODAL_FILE_HINTS.getDiscordIdentifier()).setRequired(false);
        Language.EntryConsumer<String> hints = (label, desc) -> value -> hintInput.addOption(label, value, desc);

        InteractionSession<Interaction> session = switch (command) {
            case SCHEMATIC_LABEL -> {
                LanguageFile.EmbedLang exportingFile = lang.getEmbed(Language.Modal.MODAL_FILE_SCHEM);
                title = lang.get(Language.Modal.MODAL_TITLE).replace(LangToken.FILE_TYPE, exportingFile.title());
                lang.getEmbed(Language.Modal.MODAL_FILE_GEOJSON).as(hints).accept("geojson");
                lang.getEmbed(Language.Modal.MODAL_FILE_KML).as(hints).accept("kml");

                // Schematic file need Minecraft block pattern to export.
                DiscordTextInput patternInput = DiscordTextInput
                    .builder(MODAL_PATTERN, "Ignored", DiscordTextInput.Style.SHORT)
                    .setMinLength(1)
                    .setMaxLength(100)
                    .setDefaultValue("diamond_block")
                    .build();

                DiscordLabel patternLabel = lang
                    .getEmbed(Language.Modal.MODAL_PATTERN)
                    .as(Language::asDiscordLabel)
                    .apply(patternInput::asJDA);

                patternInputLabel = patternLabel::asJDA;

                yield this.schematic;
            }
            case GEOJSON_LABEL -> {
                LanguageFile.EmbedLang exportingFile = lang.getEmbed(Language.Modal.MODAL_FILE_GEOJSON);
                title = lang.get(Language.Modal.MODAL_TITLE).replace(LangToken.FILE_TYPE, exportingFile.title());
                lang.getEmbed(Language.Modal.MODAL_FILE_KML).as(hints).accept("kml");
                lang.getEmbed(Language.Modal.MODAL_Z_DROP).as(zOption).accept(DROP_Z_LABEL);
                yield this.geojson;
            }
            case KML_LABEL -> {
                LanguageFile.EmbedLang exportingFile = lang.getEmbed(Language.Modal.MODAL_FILE_KML);
                title = lang.get(Language.Modal.MODAL_TITLE).replace(LangToken.FILE_TYPE, exportingFile.title());
                lang.getEmbed(Language.Modal.MODAL_FILE_GEOJSON).as(hints).accept("geojson");
                lang.getEmbed(Language.Modal.MODAL_Z_DROP).as(zOption).accept(DROP_Z_LABEL);
                yield this.kml;
            }
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
        // endregion

        // region Modal
        // 1. Block Pattern OR modal description
        // NOTE: Discord restricted 5 components per modal,
        //       so if we have space available in-case we don't use block pattern input,
        //       we put a simple instruction message as TextDisplay description.
        Supplier<ModalComponent<?>> modalDescription = () -> {
            String description = lang.get(Language.Modal.MODAL_DESCRIPTION);
            return () -> TextDisplay.of(description);
        };

        ModalComponent<?> patternOrDescription = Optional
            .<ModalComponent<?>>ofNullable(patternInputLabel)
            .orElseGet(modalDescription);

        // 2. Height (Y-Level)
        String initialInputHeight = interaction.height() != null? interaction.height().toString() : null;
        DiscordTextInput heightInput = DiscordTextInput
            .builder(MODAL_HEIGHT, "Ignored", DiscordTextInput.Style.SHORT)
            .setPlaceholder("7")
            .setMinLength(1)
            .setMaxLength(5)
            .setDefaultValue(initialInputHeight)
            .setRequired(true)
            .build();

        DiscordLabel heightInputLabel = lang
            .getEmbed(Language.Modal.MODAL_HEIGHT)
            .as(Language::asDiscordLabel)
            .apply(heightInput::asJDA);

        // 3. Height Placement Hints
        DiscordLabel zPlacementLabel  = lang
            .getEmbed(Language.Modal.MODAL_Z_LABEL)
            .as(Language::asDiscordLabel)
            .apply(zInput::build);

        // 4. Source File Hints
        DiscordLabel fileHintsLabel = lang
            .getEmbed(Language.Modal.MODAL_FILE_LABEL)
            .as(Language::asDiscordLabel)
            .apply(hintInput::build);

        // 5. File Uploads to export
        LanguageFile.EmbedLang fileIfUploaded = lang.getEmbed(Language.Modal.MODAL_FILE_UPLOADED);

        ModalComponent<?> fileDisplay = createModalFileDisplay(interaction, fileIfUploaded).orElseGet(() -> {
            AttachmentUpload.Builder fileUpload = AttachmentUpload
                .create(MODAL_FILE.getDiscordIdentifier())
                .setRequired(true);

            DiscordLabel fileUploadLabel  = lang
                .getEmbed(Language.Modal.MODAL_FILE)
                .as(Language::asDiscordLabel)
                .apply(fileUpload::build);

            return fileUploadLabel::asJDA;
        });

        // Finally, we create new session for the user to fill this modal before executing.
        java.util.UUID sessionUUID = session.startNew(interaction);
        ComponentForBTE modalID = ComponentForBTE.of(sessionUUID, IDENTIFIER_MODAL);
        // endregion

        return DiscordModal
            .builder(modalID.getParent(), title)
            .addComponent(patternOrDescription)
            .addComponent(heightInputLabel::asJDA)
            .addComponent(zPlacementLabel::asJDA)
            .addComponent(fileHintsLabel::asJDA)
            .addComponent(fileDisplay)
            .build();
    }

    @NotNull
    private Optional<ModalComponent<?>> createModalFileDisplay(Interaction interaction,
                                                               LanguageFile.EmbedLang lang) {
        ModalComponent<?> fileDisplay = null;
        try(Message.Attachment file = interaction.file()) {
            if(file != null) fileDisplay = () -> TextDisplay.ofFormat(
                "## %s\n%s",
                lang.title(),
                lang.description()
                    .replace(LangToken.FILE_NAME, '`' + file.getFileName() + '`')
            );
        }

        return Optional.ofNullable(fileDisplay);
    }


    /**
     * Session Based interaction value.
     *
     * @param file Attachment source file to export from
     * @param height The Y-Level of each coordinates placing in the geometries
     * @param pattern The Minecraft block pattern if required
     */
    protected record Interaction(
        @Nullable Message.Attachment file,
        @Nullable Long height,
        @Nullable String pattern) {
    }

    enum Language implements LangEntry {
        // Parent Command Info
        GROUP_DESC("description"),
        GROUP_NAME("name"),

        // Command Descriptions
        CMD_1_DESC(PREFIX + GEOJSON_LABEL),
        CMD_2_DESC(PREFIX + SCHEMATIC_LABEL),
        CMD_3_DESC(PREFIX + KML_LABEL),

        // Command Options
        OPTION_PATTERN( "options", "pattern"),
        OPTION_HEIGHT(  "options", "height"),
        OPTION_FILE(    "options", "file"),
        ;

        enum Modal implements LangEntry {
            // Form Modal
            MODAL_TITLE(        "title"),
            MODAL_DESCRIPTION(  "description"),
            // Required Options
            MODAL_HEIGHT(       "options", "height"),
            MODAL_PATTERN(      "options", "pattern"),
            MODAL_FILE(         "options", "file"),
            MODAL_FILE_UPLOADED("options", "file-if-uploaded"),
            // height Placement Hints
            MODAL_Z_LABEL(      "options", "y-placement", "label"),
            MODAL_Z_NORMALIZE(  "options", "y-placement", "normalize"),
            MODAL_Z_OFFSET(     "options", "y-placement", "offset"),
            MODAL_Z_DROP(       "options", "y-placement", "drop"),
            // File Format Hints
            MODAL_FILE_LABEL(   "options", "file-hint", "label"),
            MODAL_FILE_GEOJSON( "options", "file-hint", "geojson"),
            MODAL_FILE_KML(     "options", "file-hint", "kml"),
            MODAL_FILE_SCHEM(   "options", "file-hint", "schematic"),
            MODAL_FILE_FAST(    "options", "file-hint", "fast-schematic"),
            MODAL_FILE_MC_EDIT( "options", "file-hint", "mc-edit-schematic"),
            MODAL_FILE_SPONGE(  "options", "file-hint", "sponge-schematic"),
            ;
            private final String path;

            Modal(String... path) {
                this.path = String.join(".", path);
            }

            @Override @Contract(pure = true)
            public @NotNull String getKey() {
                return PARENT_PATH + ".modal." + path;
            }
        }

        private static final String PARENT_PATH = "slash-commands." + LABEL;
        private final String path;

        Language(String... path) {
            this.path = String.join(".", path);
        }

        @Override @Contract(pure = true)
        public @NotNull String getKey() {
            return PARENT_PATH + path;
        }

        private interface DiscordLabelConsumer extends Function<LabelComponent<?>, DiscordLabel> {
            @Override
            DiscordLabel apply(LabelComponent<?> labelComponent);
        }

        private interface EntryConsumer<T> extends BiFunction<String, String, Consumer<T>> {
            @Override
            Consumer<T> apply(String label, String description);
        }

        @Contract(pure = true)
        private static @NotNull DiscordLabelConsumer asDiscordLabel(String label, String desc) {
            return value -> DiscordLabel.of(label, desc, value);
        }
    }
}
