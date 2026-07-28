package asia.buildtheearth.asean.commands.discord.geotools;

import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.tasks.FileUploadTask;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.api.PluginProvider;
import asia.buildtheearth.asean.core.worldedit.WorldEditCommandActor;
import asia.buildtheearth.asean.discord.InteractionSession;
import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.discord.abstraction.*;
import asia.buildtheearth.asean.discord.api.ComponentDispatcher;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import asia.buildtheearth.asean.geotools.ConversionException;
import asia.buildtheearth.asean.geotools.SchematicExport;
import asia.buildtheearth.asean.geotools.projection.MinecraftProjection;
import asia.buildtheearth.asean.geotools.worldedit.BufferingRegionExtent;
import asia.buildtheearth.asean.geotools.worldedit.WorldEditGeometryWriter;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordSelectMenuInteractionEvent;
import com.discordsrv.api.task.Task;
import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.extent.PassthroughExtent;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extension.input.ParserContext;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.util.SideEffectSet;
import com.sk89q.worldedit.world.NullWorld;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenuInteraction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.NamedAttachmentProxy;
import net.kyori.adventure.text.Component;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static asia.buildtheearth.asean.commands.discord.geotools.ExportCommand.*;
import static asia.buildtheearth.asean.commands.discord.AbstractDiscordCommand.CommandWithModal;
import static asia.buildtheearth.asean.commands.discord.GeoToolsCommand.SCHEMATIC_LABEL;

public class ExportSchematic extends CommandWithModal<Interaction> implements SubCommand {
    public static final String ERROR_FILE_PREFIX = "ERROR_";
    public static final List<String> supports = List.of("kml", "geojson", "json", "gjson");

    protected final ExportCommand parent;
    private ExportSession session = null;

    public ExportSchematic(ExportCommand parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public MainGuildCommand.Config getCommandConfig() {
        return MainGuildCommand.Config.GEO_TOOLS_EXPORT_SCHEMATIC;
    }

    public String getCommandName() {
        return SCHEMATIC_LABEL;
    }

    @Override
    public boolean requireLinked() {
        return false;
    }

    public InteractionSession<?> getSession() {
        if(this.session == null)
            this.session = new ExportSession(this);

        return this.session;
    }
    // region Handler

    @Override @NotNull
    protected Task<Interaction> createNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                                 @Nullable UUID linkedUUID) {
        Interaction interaction = parent.createSession(getCommandName(), execution, linkedUUID);
        return Task.completed(interaction);
    }

    @Override @NotNull
    protected Task<?> acceptNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                       @NotNull InteractionInstance session) {
        Interaction interaction = session.getInstance();
        DiscordInteractionWithModal response = execution.getInteraction();
        return response.supply(() -> parent.createModal(getCommandName(), response, interaction, null));
    }

    @Override @NotNull
    protected Task<Interaction> acceptModal(@NotNull ReceivedEvent<DiscordModalInteractionEvent> session) {
        DiscordModalExecution.WithHook<Interaction> execution = new DiscordModalExecution.WithHook<>(plugin, session.getEvent());

        MasterServer.info("Got Modal Interaction");

        return parent.verify(getCommandName(), execution, session, session.getEvent().asJDA().getMessage());
    }

    @Override
    protected @NotNull Task<Interaction> acceptButton(@NotNull ReceivedEvent<DiscordButtonInteractionEvent> event) {
        ComponentIdentifier identifier = event.getEvent().getIdentifier();
        Optional<String> optionalID = ComponentForBTE.parseFromDiscord(identifier).map(ComponentForBTE::getIdentifier);
        Optional<Task<Interaction>> optional = optionalID.map(button -> switch (button) {
            case BUTTON_DISMISS -> {
                MessageComponentTree tree = event.getEvent().asJDA().getMessage().getComponentTree().asDisabled();
                CompletableFuture<?> task = event.getEvent().asJDA().editComponents(tree).useComponentsV2().submit();
                yield Task.of(task.thenApply(ignored -> null));
            }
            case BUTTON_RE_OPEN -> {
                Interaction newModal = Interaction.of(
                    event.getInstance().linkedUUID(),
                    null, // Attachment must be null because re-uploading is the only way to edit it
                    event.getInstance().pattern(),
                    event.getInstance().ephemeral(),
                    event.getInstance().height().isPresent()?
                    event.getInstance().height().getAsDouble() : null
                );
                parent.execute(getCommandName(),
                    new DiscordInteractionWithModal(getPlugin(), event.getEvent()),
                    newModal,
                    event.getSessionUUID()
                );
                yield Task.completed(newModal);
            }
            default -> null;
        });

        return optional.orElseGet(() -> Task.completed(null));
    }

    @Override
    protected @NotNull Task<Interaction> acceptSelectMenu(@NotNull ReceivedEvent<DiscordSelectMenuInteractionEvent> event) {

        Interaction instance = event.getInstance();
        SelectMenuInteraction<?, ?> menu = event.getEvent().asJDA().getInteraction();

        String select = (String) menu.getValues().getFirst();

        MasterServer.info("Selected " + select);

        // Acknowledge to the menu
        CompletableFuture<?> task = menu.deferEdit().submit();

        return Task.of(task.thenApply(ignored -> new Interaction(
            instance.ephemeral(),
            instance.linkedUUID(),
            instance.file(),
            instance.pattern(),
            select,
            instance.exportHint(),
            instance.height()
        )));
    }
    //endregion

    protected record Session(
        double height,
        @NotNull Optional<UUID> linkedUUID,
        @NotNull List<NamedAttachmentProxy> fileProxies,
        @NotNull Pattern pattern,
        @NotNull BuiltInClipboardFormat format
    ) {

    }

    protected class ExportSession extends InteractionSession<Session> {

        protected interface SessionError {
            @Contract("_ -> null")
            Session send(@NotNull String cause);
        }

        public ExportSession(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        protected @NotNull Task<Session> acceptButton(@NotNull ReceivedEvent<DiscordButtonInteractionEvent> event) {
            ComponentIdentifier identifier = event.getEvent().getIdentifier();
            Optional<String> optionalID = ComponentForBTE.parseFromDiscord(identifier).map(ComponentForBTE::getIdentifier);
            Optional<Task<Session>> optional = optionalID.map(button -> switch (button) {
                case BUTTON_CONFIRM -> start(event, new DiscordInteractionWithEdit<>(plugin, event.getEvent()));
                default -> null;
            });

            return optional.orElseGet(() -> Task.completed(null));
        }


        /**
         * Start exporting session
         */
        private Task<Session> start(ComponentDispatcher.Instance<Session> session,
                                    DiscordInteractionWithEdit<Session> interaction) {
            // Retrieve info from previous dispatcher
            final UUID sessionUUID = session.getSessionUUID();
            final Interaction info = ExportSchematic.this.getSession(sessionUUID);
            final DiscordInteractionMessageEdit<Session> edit = interaction.asEdit();
            final SessionError error = cause -> {
                Container container = Container.of(
                    TextDisplay.of("## :broken_heart: Something went wrong!\n" + cause)
                ).withAccentColor(Constants.RED);

                SendableDiscordMessage message =  SendableDiscordMessage.builder()
                    .forceComponentsV2()
                    .addComponent(() -> container)
                    .build();

                edit.send((Component) null, message);

                return null;
            };

            if(info == null)
               return Task.completed(error.send("Unexpected error occurred. Please try again or contact support."));

            // Prepare session information for the export task
            Task<@Nullable Session> task = edit.supply(() -> {
                MasterServer.info("Received session: " + sessionUUID);
                MasterServer.info("Downloading Schematic...");

                if(info.file() == null) return error.send("Retrieved file are null");

                final File cacheFolder = prepareCacheFolder(sessionUUID);

                boolean created = cacheFolder.mkdirs();

                if(!created) return error.send("Could not create cache folder");
                if(info.pattern() == null) return error.send("Cannot retrieve input pattern");

                ParserContext context = new ParserContext();
                context.setRestricted(false);

                Container container = Container
                    .of(TextDisplay.of("Converting... Please wait"))
                    .withAccentColor(Constants.ORANGE);
                SendableDiscordMessage response = SendableDiscordMessage.builder()
                    .forceComponentsV2()
                    .addComponent(() -> container)
                    .build();
                edit.send((Component) null, response);

                return new Session(
                    info.height().orElseThrow(),
                    Optional.ofNullable(info.linkedUUID()),
                    info.file()
                        .stream()
                        .map(Message.Attachment::getProxy)
                        .toList(),
                    Fawe.instance()
                        .getWorldEdit()
                        .getPatternFactory()
                        .parseFromInput(info.pattern(), context),
                    BuiltInClipboardFormat.FAST_V2
                );
            });
            final File cacheFolder = this.prepareCacheFolder(sessionUUID);

            return task.then(settings -> this.submit(cacheFolder, settings, edit::send));
        }

        public ExportTask newTask(@NotNull Session session, Path path) {
            return new ExportTask(ExportSchematic.this, session, path);
        }

        public Task<Session> submit(@NotNull File cacheFolder,
                                    @Nullable Session session,
                                    @NotNull BiConsumer<Component, SendableDiscordMessage> consumer) {
            if(session == null) return null;

            final ExportTask task = this.newTask(session, cacheFolder.toPath());
            final Map<String, String> resultMap = new HashMap<>();

            return task.submit(resultMap).thenApply(finished -> {
                List<ContainerChildComponent> exported = new ArrayList<>();
                Color accentColor;
                int uploaded = 0;
                // collect our files
                for(@Nullable String filename : finished) {
                    if(filename == null)
                        continue; // unknown error occurred, see server console stacktrace
                    else if(filename.startsWith(ERROR_FILE_PREFIX) && filename.endsWith(".txt"))
                        uploaded--; // Error occurred, attaching as log file for user

                    Path schematicFile = cacheFolder.toPath().resolve(filename);
                    FileUpload fileUpload = FileUpload.fromData(schematicFile, StandardOpenOption.READ);
                    exported.add(FileDisplay.fromFile(fileUpload));
                    uploaded++;
                }

                if(uploaded <= 0) {
                    // No file is exported somehow
                    accentColor = Constants.RED;
                    exported.add(Separator.createDivider(Separator.Spacing.SMALL));
                    exported.add(TextDisplay.of(
                            "## :warning: Sorry! we couldn't convert any file :(\n"
                                    + "Please check your upload contents and try again, or contact support for help."
                    ));
                }
                else if(uploaded < session.fileProxies().size()) {
                    // Some file(s) is missing in final export
                    List<String> errors = new ArrayList<>();

                    // lookup missing files
                    task.lookup(Map.copyOf(resultMap), (originalName, fileName) -> {
                        if(fileName == null) errors.add(originalName);
                    });

                    String errorDisplay = "## :warning: We couldn't convert some file(s)\n"
                            + "The following file(s) couldn't be converted:\n```"
                            + String.join(", ", errors) + "```\n"
                            + "Please check your upload contents and try again, or contact support for help.";

                    accentColor = Constants.ORANGE;
                    exported.add(Separator.createDivider(Separator.Spacing.SMALL));
                    exported.add(TextDisplay.of(errorDisplay));
                }
                else accentColor = Constants.GREEN;

                Container container = Container.of(exported).withAccentColor(accentColor);
                SendableDiscordMessage message = SendableDiscordMessage
                        .builder()
                        .forceComponentsV2()
                        .addComponent(() -> container)
                        .build();

                consumer.accept(null, message);

                return session;
            });
        }
    }

    /**
     * Supply task that result in a list of filenames which is exported by the session information.
     */
    protected static class ExportTask extends FileUploadTask {
        protected final Session session;

        protected ExportTask(@NotNull PluginProvider provider,
                             @NotNull Session session,
                             @NotNull Path path) {
            super(provider, session.fileProxies(), path);
            this.session = session;
        }

        /**
         * Submit this task to download, verify, and then export schematic file(s).
         *
         * @param resultMap Empty-modifiable map to output as proxy list by unique fileName keys.
         * @return Future to a list of exported file name(s).
         */
        public Task<? extends Collection<String>> submit(Map<String, String> resultMap) {
            return this.verify(resultMap).then(this::export);
        }

        public @NotNull Task<? extends Collection<@Nullable String>> export(Map<String, String> sourceMap) {
            // finally convert it
            List<Task<String>> tasks = new ArrayList<>();

            this.lookup(sourceMap, (ignored, filename) -> {
                if(filename != null)
                    tasks.add(this.convert(filename));
            });

            return Task.allOf(tasks);
        }

        public @Nullable String schematicExport(@NotNull SchematicExport export,
                                                @NotNull String filename) throws Throwable {

            BufferingRegionExtent buffer = new BufferingRegionExtent();

            export.setProjection(MinecraftProjection::getASEAN);
            export.setFormat(this.session::format);
            export.setPattern(this.session.pattern());

            try(EditSession edit = Fawe.instance()
                    .getWorldEdit()
                    .newEditSessionBuilder()
                    .world(NullWorld.getInstance())
                    .setSideEffectSet(SideEffectSet.none())
                    .actor(new WorldEditCommandActor(this.plugin))
                    .build()) {

                edit.setExtent(new PassthroughExtent(buffer));

                WorldEditGeometryWriter writer = new WorldEditGeometryWriter(
                        edit,
                        MinecraftProjection.getASEAN(),
                        this.session.pattern()
                );

                export.convert(writer::writeGeometry);
            }

            String output = String.join(".",
                    Text.stripExtension(filename),
                    this.session.format().getPrimaryFileExtension()
            );

            File finalFile = this.sessionFile.get(output);

            export.export(buffer, finalFile.toPath());

            return output;
        }

        public Task<@Nullable String> convert(@NotNull String filename) {
            return this.plugin.scheduler().supply(() -> {
                File source = this.sessionFile.get(filename);
                String extension = Text.getFileExtension(filename);
                SchematicExport export = switch (extension) {
                    case "geojson", "json", "gjson" -> SchematicExport.fromGeoJSON(source);
                    case "kml" -> SchematicExport.fromKML(source);
                    case null -> throw new IllegalStateException("File extension not found");
                    default -> throw new IllegalStateException("Unexpected file extension: " + extension);
                };

                return this.schematicExport(export, filename);
            }).mapException((throwable) -> {
                StringWriter stringWriter = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stringWriter));

                // Save log to a .txt file and send to user
                try {
                    File file = this.sessionFile.get(ERROR_FILE_PREFIX + Text.stripExtension(filename) + ".txt");
                    Files.writeString(file.toPath(), stringWriter.toString());
                    return file.getName();
                } catch (IOException exception) {
                    MasterServer.error(this.getClass().getSimpleName()
                        + ": Failed to save log file... dumping on console.", exception);
                    MasterServer.error(this.getClass().getSimpleName()
                        + ": Error occurred during conversion task.", throwable);
                    return null;
                }
            });
        }

    }
}
