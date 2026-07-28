package asia.buildtheearth.asean.commands.discord.geotools;

import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.io.LangPair;
import asia.buildtheearth.asean.core.io.LangToken;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.abstraction.AbstractDiscordPlugin;
import asia.buildtheearth.asean.discord.InteractionSession;
import asia.buildtheearth.asean.discord.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.discord.abstraction.DiscordInteractionWithModal;
import asia.buildtheearth.asean.discord.abstraction.DiscordModalExecution;
import asia.buildtheearth.asean.discord.api.ComponentDispatcher;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import asia.buildtheearth.asean.discord.api.InteractionExecution;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.command.SubCommandGroup;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;

import com.discordsrv.api.discord.entity.interaction.component.component.LabelComponent;
import com.discordsrv.api.discord.entity.interaction.component.component.ModalComponent;
import com.discordsrv.api.discord.entity.interaction.component.impl.*;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.extension.input.InputParseException;
import com.sk89q.worldedit.extension.input.ParserContext;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.*;

import static asia.buildtheearth.asean.commands.discord.GeoToolsCommand.*;

public class ExportCommand extends AbstractDiscordPlugin {
    static final int MESSAGE_MAX_LENGTH = Message.MAX_CONTENT_LENGTH;

    static final String SEA_PROJ_LABEL = "sea-proj";
    static final String BTE_PROJ_LABEL = "bte-proj";

    static final String FAST_V2_LABEL = ".fast2";
    static final String FAST_V3_LABEL = ".fast3";
    static final String SPONGE_V3_LABEL = ".sponge3";

    static final String IDENTIFIER_MODAL = "modal";

    static final String BUTTON_CONFIRM = "button-confirm-modal";
    static final String BUTTON_DISMISS = "button-dismiss-modal";
    static final String BUTTON_RE_OPEN = "button-re-open-modal";

    static final String EXPORT_HINTS = "modal-export-hints";

    static final ComponentIdentifier IDENTIFIER_SCHEMATIC = ComponentForBTE.of(EXPORT_CMD + SCHEMATIC_LABEL);
    static final ComponentIdentifier IDENTIFIER_GEOJSON = ComponentForBTE.of(EXPORT_CMD + GEOJSON_LABEL);
    static final ComponentIdentifier IDENTIFIER_KML = ComponentForBTE.of(EXPORT_CMD + KML_LABEL);

    static final ComponentIdentifier MODAL_PATTERN = ComponentForBTE.of("modal-pattern");
    static final ComponentIdentifier MODAL_HEIGHT = ComponentForBTE.of("modal-height");
    static final ComponentIdentifier MODAL_HEIGHT_OPTION = ComponentForBTE.of("modal-z-placement");
    static final ComponentIdentifier MODAL_PROJECTION = ComponentForBTE.of("modal-projection");
    static final ComponentIdentifier MODAL_FILE = ComponentForBTE.of("modal-file-upload");

    protected interface SubCommand {
        InteractionSession<?> getSession();
        default void forward(UUID sessionUUID) {
            this.getSession().putSession(sessionUUID, null);
        }
    }

    private static SubCommandGroup INSTANCE;
    private final ExportSchematic schematic;
    private final ExportGeoJSON geojson;
    private final ExportKML kml;
    private final DrawCommand draw;

    public static SubCommandGroup get(ExportCommand command,
                                      CommandOption pattern,
                                      CommandOption... options) {
        if (INSTANCE == null) {
            // region Construction
            MasterServer plugin = command.getPlugin();

            DiscordCommand.ChatInputBuilder geojson = DiscordCommand
                .chatInput(IDENTIFIER_GEOJSON, GEOJSON_LABEL, plugin.getLang().get(Language.CMD_1_DESC))
                .addDescriptionTranslations(plugin.getLang(Language.CMD_1_DESC, LanguageFile::get));

            DiscordCommand.ChatInputBuilder kml = DiscordCommand
                .chatInput(IDENTIFIER_KML, KML_LABEL, plugin.getLang().get(Language.CMD_3_DESC))
                .addDescriptionTranslations(plugin.getLang(Language.CMD_3_DESC, LanguageFile::get));

            DiscordCommand.ChatInputBuilder schematic = DiscordCommand
                .chatInput(IDENTIFIER_SCHEMATIC, SCHEMATIC_LABEL, plugin.getLang().get(Language.CMD_2_DESC))
                .addDescriptionTranslations(plugin.getLang(Language.CMD_2_DESC, LanguageFile::get))
                .addOption(pattern); // game-compatible export can have pattern option

            for(CommandOption option : options) {
                schematic.addOption(option);
                geojson.addOption(option);
                kml.addOption(option);
            }

            INSTANCE = SubCommandGroup.builder(EXPORT_LABEL, plugin.getLang().get(Language.GROUP_DESC))
                .addNameTranslations(plugin.getLang(Language.GROUP_NAME, LanguageFile::get))
                .addDescriptionTranslations(plugin.getLang(Language.GROUP_DESC, LanguageFile::get))
                .addCommand(geojson.setEventHandler(command.geojson).build())
                .addCommand(schematic.setEventHandler(command.schematic).build())
                .addCommand(kml.setEventHandler(command.kml).build())
                .build();
            // endregion
        }

        return INSTANCE;
    }

    public ExportCommand(MasterServer plugin,
                         DiscordSRV api) {
        super(plugin, api);
        this.schematic = new ExportSchematic(this);
        this.geojson = new ExportGeoJSON(this);
        this.kml = new ExportKML(this);
        this.draw = new DrawCommand(this);

        api.registerModule(this.schematic);
        api.registerModule(this.geojson);
        api.registerModule(this.kml);
        api.registerModule(this.draw);
    }

    /**
     * Get draw command instance, since its independent of export subcommand group
     *
     * @return Created instance
     */
    public DrawCommand getDrawCommand() {
        return this.draw;
    }

    protected void execute(@NotNull String subcommand,
                           @NotNull DiscordInteractionWithModal execution,
                           @NotNull Interaction interaction,
                           @NotNull java.util.UUID sessionUUID) {
        execution.supply(() -> createModal(subcommand, execution, interaction, sessionUUID));
    }

    protected Task<Interaction> verify(@NotNull String subcommand,
                                       @NotNull DiscordModalExecution.WithHook<Interaction> execution,
                                       @NotNull ComponentDispatcher.Instance<Interaction> session,
                                       @Nullable Message message) {
        MasterServer.info("Verifying interaction...");
        execution.getInteraction().setEphemeral(session.getInstance().ephemeral());
        CheckedSupplier<Interaction> action = () -> verifyModal(subcommand, execution, session, message);
        return (message != null)? execution.getInteraction().asEdit().supply(action) : execution.getInteraction().supply(action);
    }

    @NonNull @Contract("_, _, _, _ -> new")
    private Interaction verifyModal(@NotNull String subcommand,
                                    @NotNull DiscordModalExecution.WithHook<Interaction> execution,
                                    @NotNull ComponentDispatcher.Instance<Interaction> session,
                                    @Nullable Message message) {
        MasterServer.info("Verifying interaction (ASYNC)...");
        LanguageFile lang = plugin.getLang(execution.getInteraction().locale());
        String heightInput = execution.getString(MODAL_HEIGHT.getDiscordIdentifier());

        List<Message.Attachment> fileInput = (session.getInstance().file() != null)?
            session.getInstance().file() : execution.getAttachment(MODAL_FILE.getDiscordIdentifier());

        List<String> proj = execution.getSelect(MODAL_PROJECTION.getDiscordIdentifier());
        String patternInput = execution.getString(MODAL_PATTERN.getDiscordIdentifier());
        String heightOption = execution.getSelect(MODAL_HEIGHT_OPTION.getDiscordIdentifier()).getFirst();

        Optional<StringSelectMenu> optionalHint = Optional.empty();

        List<LangPair<?>>
            warnings = new ArrayList<>(),
            failures = new ArrayList<>(),
            verified = new ArrayList<>();

        Consumer<List<String>> verifyFile = supports -> fileInput.forEach(file -> {
            if(file.getFileExtension() == null) {
                failures
                    .add(lang.getPair(Language.Verify.FAILURE_FORMAT_NOT_FOUND)
                    .with(Function.identity(), description -> description
                    .replace(LangToken.FILE_NAME, file.getFileName())
                    .replace(LangToken.VALUE, supports.toString()))
                );
                return;
            }
            if(!supports.contains(file.getFileExtension().toLowerCase(Locale.ENGLISH))) failures
                .add(lang.getPair(Language.Verify.FAILURE_FORMAT_NOT_SUPPORTS)
                .with(Function.identity(), description -> description
                .replace(LangToken.FILE_NAME, file.getFileName())
                .replace(LangToken.VALUE, supports.toString()))
            );
        });

        // region Session
        SubCommand command = switch (subcommand) {
            case SCHEMATIC_LABEL, DRAW_LABEL -> {

                // Exporting schematic file
                Function<Language.Verify, LangPair<?>> pattern = signature -> lang
                    .getPair(signature)
                    .with(Function.identity(), description -> description
                    .replace(LangToken.VALUE, patternInput));

                try {
                    ParserContext context = new ParserContext();
                    context.setRestricted(false);
                    Fawe.instance()
                        .getWorldEdit()
                        .getPatternFactory()
                        .parseFromInput(patternInput, context);
                    verified.add(pattern.apply(Language.Verify.VERIFIED_BLOCK_PATTERN));
                }
                catch (InputParseException ignored) {
                    warnings.add(pattern.apply(Language.Verify.WARNING_BLOCK_PATTERN));
                }

                // Supported Formats
                verifyFile.accept(ExportSchematic.supports);

                ComponentIdentifier menu = session.newComponent(EXPORT_HINTS);
                StringSelectMenu.Builder exportHint = StringSelectMenu.create(menu.getDiscordIdentifier());
                Language.EntryBiConsumer<String, Integer> hintOption = (label, desc) -> (value, version) -> {
                    String title = label.replace(LangToken.COUNT, version.toString());
                    exportHint.addOption(title, value, desc);
                };

                lang.getPair(Language.Modal.MODAL_FILE_FAST).as(hintOption).accept(FAST_V2_LABEL, 2);
                lang.getPair(Language.Modal.MODAL_FILE_FAST).as(hintOption).accept(FAST_V3_LABEL, 3);
                lang.getPair(Language.Modal.MODAL_FILE_SPONGE).as(hintOption).accept(SPONGE_V3_LABEL, 3);
                exportHint.setDefaultValues(FAST_V2_LABEL);

                optionalHint = Optional.of(exportHint.build());

                yield DRAW_LABEL.equals(subcommand)? this.draw : this.schematic;
            }
            case GEOJSON_LABEL -> {
                // Supported Formats
                List<String> supports = List.of("kml");
                verifyFile.accept(supports);

                yield this.geojson;
            }
            case KML_LABEL -> {
                // Supported Formats
                List<String> supports = List.of("geojson");
                verifyFile.accept(supports);

                yield this.kml;
            }
            default -> throw new IllegalStateException("Unexpected value: " + subcommand);
        };
        // endregion

        // region Verification

        OptionalDouble height = OptionalDouble.empty();

        switch (heightOption) {
            case DROP_Z_LABEL:
                if(SCHEMATIC_LABEL.equals(subcommand))
                    warnings.add(lang.getPair(Language.Verify.WARNING_CANNOT_DROP_HEIGHT));
                else verified.add(lang.getPair(Language.Verify.VERIFIED_HEIGHT_DROPPED));
                break;
            case OFFSET_Z_LABEL:
                verified.add(lang.getPair(Language.Verify.VERIFIED_HEIGHT_OFFSET)
                    .with(Function.identity(), description -> description
                    .replace(LangToken.VALUE, heightInput))
                );
            case NORMALIZE_Z_LABEL:
                verified.add(lang.getPair(Language.Verify.VERIFIED_HEIGHT_NORMALIZED)
                    .with(Function.identity(), description -> description
                    .replace(LangToken.VALUE, heightInput))
                );
            case null, default:
                height = parseDoubleInput(heightInput);
                if(height.isEmpty()) {
                    failures.add(lang
                        .getPair(Language.Verify.FAILURE_HEIGHT_VALUE)
                        .with(Function.identity(), description -> description
                        .replace(LangToken.VALUE, heightInput))
                    );
                }
        }

        // endregion


        DiscordButton cancel = Language.Button.CANCEL
                .builder(lang, session::newComponent)
                .apply(BUTTON_DISMISS, DiscordButton.Style.DANGER)
                .build();

        DiscordButton edit = Language.Button.EDIT
                .builder(lang, session::newComponent)
                .apply(BUTTON_RE_OPEN, DiscordButton.Style.PRIMARY)
                .build();

        // Confirm button will forward to a different dispatcher
        command.forward(session.getSessionUUID());
        DiscordButton confirm = Language.Button.CONFIRM
                .builder(lang, identifier -> command.getSession().newComponent(session.getSessionUUID(), identifier))
                .apply(BUTTON_CONFIRM, DiscordButton.Style.SUCCESS)
                .build();
        MasterServer.info("Forwarding session to: " + session.getSessionUUID().toString());


        List<ContainerChildComponent> information = new ArrayList<>();

        information.add(TextDisplay.of("# Submit Information"));

        BiConsumer<String, List<LangPair<?>>> callback = (prefix, verification) -> {
            verification.forEach(msg -> {
                information.add(
                    TextDisplay.of(prefix + msg.primary() + "\n" + msg.secondary()));
                information.add(Separator.createDivider(Separator.Spacing.SMALL));
            });
        };
        java.awt.Color color = null;

        if(!failures.isEmpty()) {
            callback.accept("### :bangbang: ", failures);
            color = Constants.RED;
        }

        if(!warnings.isEmpty()) {
            callback.accept("### :warning: ", warnings);

            if(color == null) color = Constants.ORANGE;
        }

        if(!verified.isEmpty()) {
            callback.accept("### :green_circle: ", verified);

            if(color == null) color = Constants.GREEN;
        }

        // Can proceed (add confirm button) if no failure occurred
        if(failures.isEmpty()) {
            // TODO: Move this to "Advanced" Modal
            // optionalHint.ifPresent(hint -> {
            //     LanguageFile.EntryPair<?> msg = lang.getPair(Language.Modal.MODAL_FILE_EXPORT);
            //     TextDisplay label =TextDisplay.of("### :gear: " + msg.primary() + "\n" + msg.secondary());
            //     information.add(label);
            //     information.add(ActionRow.of(hint));
            // });
        }

        // TODO: "Advanced" button that open up a modal
        information.add(ActionRow.of(Button.secondary("TODO: Advanced Options", "Advanced [WIP]")).asDisabled());

        information.add(Separator.createDivider(Separator.Spacing.SMALL));
        information.add(ActionRow.of(
            cancel.asJDA(),
            edit.asJDA(),
            failures.isEmpty()? confirm.asJDA() : confirm.asJDA().asDisabled()
        ));

        Container container = Container.of(information).withAccentColor(color);

        MasterServer.info("Sending interaction...");

        SendableDiscordMessage component = SendableDiscordMessage
            .builder()
            .addComponent(() -> container)
            .forceComponentsV2()
            .build();

        boolean ephemeral = session.getInstance().ephemeral();

        if(message != null)
            execution.getInteraction().asEdit().send((MinecraftComponent) null, component);
        else {
            execution.getInteraction().setEphemeral(ephemeral);
            execution.getInteraction().send((MinecraftComponent) null, component);
        }

        return new Interaction(
                ephemeral,
                session.getInstance().linkedUUID(),
                fileInput,
                patternInput,
                proj.getFirst(),
                FAST_V2_LABEL,
                height
        );
    }

    protected Interaction createSession(@NotNull String command,
                                        @NotNull DiscordCommandExecution.WithModal execution,
                                        @Nullable UUID linkedUUID) {
        return Interaction.of(
                linkedUUID,
                Optional.ofNullable(execution.getAttachment(FILE_LABEL))
                        .map(Collections::singletonList)
                        .orElse(null),
                execution.getString(PATTERN_LABEL),
                !Boolean.FALSE.equals(execution.getBoolean(PRIVATE_LABEL)),
                execution.getDouble(HEIGHT_LABEL)
        );
    }

    protected DiscordModal createModal(@NotNull String command,
                                     @NotNull InteractionExecution<?> execution,
                                     @NotNull Interaction interaction,
                                     @Nullable java.util.UUID sessionUUID) {
        LanguageFile lang = plugin.getLang(execution.locale());

        // region Session
        // SubCommand session specific options:
        // - Display different title based on what file we're exporting
        // - Exporting schematic file require Minecraft block pattern
        // - Additional hints for height placement and file input format
        String title;
        ModalComponent<?> patternInputLabel = null;
        StringSelectMenu.Builder zInput = StringSelectMenu.create(MODAL_HEIGHT_OPTION.getDiscordIdentifier());
        Language.EntryConsumer<String> zOption = (label, desc) -> value -> zInput.addOption(label, value, desc);

        lang.getPair(Language.Modal.MODAL_Z_NORMALIZE).as(zOption).accept(NORMALIZE_Z_LABEL);
        lang.getPair(Language.Modal.MODAL_Z_OFFSET).as(zOption).accept(OFFSET_Z_LABEL);
        zInput.setDefaultValues(NORMALIZE_Z_LABEL);

        // Projections
        StringSelectMenu.Builder projInputs = StringSelectMenu.create(MODAL_PROJECTION.getDiscordIdentifier()).setRequired(false);
        Language.EntryConsumer<String> proj = (label, desc) -> value -> projInputs.addOption(label, value, desc);

        InteractionSession<Interaction> session = switch (command) {
            case SCHEMATIC_LABEL, DRAW_LABEL -> {
                LangPair<?> exportingFile = lang.getPair(Language.Modal.MODAL_FILE_SCHEM);
                title = lang.get(Language.Modal.MODAL_TITLE).replace(LangToken.FILE_TYPE, exportingFile.primary());

                // Supported projections Formats
                lang.getPair(Language.Modal.MODAL_PROJ_SEA).as(proj).accept(SEA_PROJ_LABEL);
                lang.getPair(Language.Modal.MODAL_PROJ_BTE).as(proj).accept(BTE_PROJ_LABEL);

                // Schematic file need Minecraft block pattern to export.
                DiscordTextInput patternInput = DiscordTextInput
                    .builder(MODAL_PATTERN, "Ignored", DiscordTextInput.Style.SHORT)
                    .setMinLength(1)
                    .setMaxLength(100)
                    .setDefaultValue(interaction.pattern() != null? interaction.pattern() : "diamond_block")
                    .build();

                DiscordLabel patternLabel = lang
                    .getPair(Language.Modal.MODAL_PATTERN)
                    .as(Language::asDiscordLabel)
                    .apply(patternInput::asJDA);

                patternInputLabel = patternLabel::asJDA;

                yield DRAW_LABEL.equals(command)? this.draw : this.schematic;
            }
            case GEOJSON_LABEL -> {
                LangPair<?> exportingFile = lang.getPair(Language.Modal.MODAL_FILE_GEOJSON);
                title = lang.get(Language.Modal.MODAL_TITLE).replace(LangToken.FILE_TYPE, exportingFile.primary());
                lang.getPair(Language.Modal.MODAL_Z_DROP).as(zOption).accept(DROP_Z_LABEL);

                // Supported Formats
                // lang.getPair(Language.Modal.MODAL_FILE_KML).as(proj).accept(KML_LABEL);

                yield this.geojson;
            }
            case KML_LABEL -> {
                LangPair<?> exportingFile = lang.getPair(Language.Modal.MODAL_FILE_KML);
                title = lang.get(Language.Modal.MODAL_TITLE).replace(LangToken.FILE_TYPE, exportingFile.primary());
                lang.getPair(Language.Modal.MODAL_Z_DROP).as(zOption).accept(DROP_Z_LABEL);

                // Supported Formats
                // lang.getPair(Language.Modal.MODAL_FILE_GEOJSON).as(proj).accept(GEOJSON_LABEL);

                yield this.kml;
            }
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
        // endregion

        // region Modal
        // 1. File Uploads to export
        // NOTE: Normally display a text that it is uploaded,
        //       else create a modal file upload input
        LangPair<String> fileIfUploaded = lang.getPair(Language.Modal.MODAL_FILE_UPLOADED);

        ModalComponent<?> fileDisplay = createModalFileDisplay(interaction, fileIfUploaded).orElseGet(() -> {
            AttachmentUpload.Builder fileUpload = AttachmentUpload
                    .create(MODAL_FILE.getDiscordIdentifier())
                    .setMaxValues(10) // Can upload up to 10 files by discord API limit
                    .setMinValues(1)
                    .setRequired(true);

            DiscordLabel fileUploadLabel  = lang
                    .getPair(Language.Modal.MODAL_FILE)
                    .as(Language::asDiscordLabel)
                    .apply(fileUpload::build);

            return fileUploadLabel::asJDA;
        });

        // 2. Block Pattern OR modal description
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

        // 3. Height (Y-Level)
        String initialInputHeight = interaction.height().isPresent()?
            Double.toString(interaction.height().getAsDouble()) : null;
        DiscordTextInput heightInput = DiscordTextInput
            .builder(MODAL_HEIGHT, "Ignored", DiscordTextInput.Style.SHORT)
            .setPlaceholder("7")
            .setMinLength(1)
            .setMaxLength(5)
            .setDefaultValue(initialInputHeight)
            .setRequired(true)
            .build();

        DiscordLabel heightInputLabel = lang
            .getPair(Language.Modal.MODAL_HEIGHT)
            .as(Language::asDiscordLabel)
            .apply(heightInput::asJDA);

        // 4. Height Placement Hints
        DiscordLabel zPlacementLabel  = lang
            .getPair(Language.Modal.MODAL_Z_LABEL)
            .as(Language::asDiscordLabel)
            .apply(zInput::build);

        // 5. Projection Option
        projInputs.setDefaultValues(SEA_PROJ_LABEL);
        projInputs.setMaxValues(1);
        projInputs.setMinValues(1);

        DiscordLabel projectionLabel = lang
            .getPair(Language.Modal.MODAL_PROJ_LABEL)
            .as(Language::asDiscordLabel)
            .apply(projInputs::build);

        // Finally, we start new or inherit session for the user to fill this modal before executing.
        ComponentIdentifier modalID;
        if(sessionUUID == null) {
            UUID newSessionUUID = session.startNewSessionUUID(interaction);
            session.assignOwner(newSessionUUID, execution.getUser().getId());
            modalID = session.newComponent(newSessionUUID, IDENTIFIER_MODAL);
        }
        else modalID = session.newComponent(sessionUUID, IDENTIFIER_MODAL);
        // endregion

        return DiscordModal
            .builder(modalID, title)
            .addComponent(fileDisplay)
            .addComponent(patternOrDescription)
            .addComponent(heightInputLabel::asJDA)
            .addComponent(zPlacementLabel::asJDA)
            .addComponent(projectionLabel::asJDA)
            .build();
    }

    @NotNull
    private Optional<ModalComponent<?>> createModalFileDisplay(@NotNull Interaction interaction,
                                                               @NotNull LangPair<? extends String> lang) {
        ModalComponent<?> fileDisplay = null;
        List<Message.Attachment> file = interaction.file();

        if(file != null && !file.isEmpty()) fileDisplay = () -> TextDisplay.ofFormat(
            "## %s\n%s",
            lang.primary(),
            lang.secondary()
                .replace(LangToken.FILE_NAME, '`' + file.getFirst().getFileName() + '`')
        );

        return Optional.ofNullable(fileDisplay);
    }

    @NotNull
    private OptionalDouble parseDoubleInput(@Nullable String doubleInput) {
        if(doubleInput == null) return OptionalDouble.empty();
        try {
            // Try parse and catch exception
            double value = Double.parseDouble(doubleInput);
            return OptionalDouble.of(value);
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }


    /**
     * Session Based interaction value.
     *
     * @param file Attachment source file to export from
     * @param height The Y-Level of each coordinates placing in the geometries
     * @param pattern The Minecraft block pattern if required
     */
    protected record Interaction(
        boolean ephemeral,
        @Nullable UUID linkedUUID,
        @Nullable List<Message.Attachment> file,
        @Nullable String pattern,
        @Nullable String projection,
        @Nullable String exportHint,
        @NotNull OptionalDouble height) {

        /**
         * Shorthand for {@link Interaction::new}
         */
        @Contract("_, _, _, _, _ -> new")
        protected static @NotNull Interaction of(@Nullable UUID linkedUUID,
                                                 @Nullable List<Message.Attachment> file,
                                                 @Nullable String pattern,
                                                 boolean ephemeral,
                                                 @Nullable Double height) {
            if(height == null) return of(linkedUUID, file, pattern, ephemeral);
            else return of(linkedUUID, file, pattern, ephemeral, height.doubleValue());
        }
        /**
         * Shorthand for {@link Interaction::new}
         */
        @Contract("_, _, _, _, _ -> new")
        protected static @NotNull Interaction of(@Nullable UUID linkedUUID,
                                                 @Nullable List<Message.Attachment> file,
                                                 @Nullable String pattern,
                                                 boolean ephemeral,
                                                 double height) {
            return new Interaction(ephemeral, linkedUUID, file, pattern, null, null, OptionalDouble.of(height));
        }

        /**
         * Shorthand for {@link Interaction::new}
         */
        @Contract("_, _, _, _ -> new")
        protected static @NotNull Interaction of(@Nullable UUID linkedUUID,
                                                 @Nullable List<Message.Attachment> file,
                                                 @Nullable String pattern,
                                                 boolean ephemeral) {
            return new Interaction(ephemeral, linkedUUID, file, pattern, null, null, OptionalDouble.empty());
        }
    }

    enum Language implements LangEntry {
        // Parent Command Info
        GROUP_DESC("description"),
        GROUP_NAME("name"),

        // Command Descriptions
        CMD_1_DESC(EXPORT_CMD + GEOJSON_LABEL),
        CMD_2_DESC(EXPORT_CMD + SCHEMATIC_LABEL),
        CMD_3_DESC(EXPORT_CMD + KML_LABEL);

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
            MODAL_PROJ_SEA( "options", "projection", "sea"),
            MODAL_PROJ_BTE( "options", "projection", "bte"),
            MODAL_PROJ_LABEL( "options", "projection", "label"),
            // File Format Hints
            MODAL_FILE_LABEL(   "options", "file-hint", "label"),
            MODAL_FILE_EXPORT(   "options", "file-hint", "export"),
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
                return PARENT_PATH + "modal." + path;
            }
        }

        enum Verify implements LangEntry {
            WARNING_BLOCK_PATTERN("warning-block-pattern"),
            WARNING_CANNOT_DROP_HEIGHT("warning-cannot-drop-height"),
            VERIFIED_BLOCK_PATTERN("verified-block-pattern"),
            VERIFIED_HEIGHT_NORMALIZED("verified-height-normalized"),
            VERIFIED_HEIGHT_DROPPED("verified-height-dropped"),
            FAILURE_HEIGHT_VALUE("failure-height-value"),
            FAILURE_FORMAT_NOT_SUPPORTS("failure-format-unsupported"),
            FAILURE_FORMAT_NOT_FOUND("failure-format-not-found"),
            VERIFIED_HEIGHT_OFFSET("verified-height-offset");

            private final String path;

            Verify(String... path) {
                this.path = String.join(".", path);
            }

            @Override @Contract(pure = true)
            public @NotNull String getKey() {
                return PARENT_PATH + "verification." + path;
            }
        }

        enum Button implements LangEntry {
            CANCEL( "cancel"),
            CONFIRM("confirm"),
            DISMISS("dismiss"),
            EDIT("edit");

            private final String path;

            Button(String path) {
                this.path = path;
            }

            @Override @Contract(pure = true)
            public @NotNull String getKey() {
                return "slash-commands.interactions.button." + path;
            }

            private interface Builder extends  BiFunction<String, DiscordButton.Style, DiscordButton.Builder> {
                @Override
                DiscordButton.Builder apply(String identifier, DiscordButton.Style style);
            }

            private Builder builder(LanguageFile lang,
                                    Function<String, ComponentIdentifier> provider) {
                String label = lang.get(this);

                return (identifier, style) -> DiscordButton.builder(provider.apply(identifier), style).setLabel(label);
            }
        }

        private static final String PARENT_PATH = "slash-commands.geo-tools-" + EXPORT_LABEL + '.';
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

        private interface EntryBiConsumer<T, U> extends BiFunction<String, String, BiConsumer<T, U>> {
            @Override
            BiConsumer<T, U> apply(String label, String description);
        }

        @Contract(pure = true)
        private static @NotNull DiscordLabelConsumer asDiscordLabel(String label, String desc) {
            return value -> DiscordLabel.of(label, desc, value);
        }

    }
}
