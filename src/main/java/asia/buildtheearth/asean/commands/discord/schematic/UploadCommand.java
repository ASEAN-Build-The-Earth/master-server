package asia.buildtheearth.asean.commands.discord.schematic;

import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.tasks.FileUploadTask;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.discord.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.discord.abstraction.DiscordInteractionWithHook;
import asia.buildtheearth.asean.discord.abstraction.DiscordInteractionWithModal;
import asia.buildtheearth.asean.discord.abstraction.DiscordModalExecution;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.component.ModalComponent;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordModal;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.components.attachmentupload.AttachmentUpload;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.NamedAttachmentProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;

public class UploadCommand extends AbstractSchematicCommand<
    UploadCommand.Interaction // Storing session interaction
> {
    private static final String LABEL = "upload";
    private static final String FILE_LABEL = "file";
    static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("slash-schematic-upload-command");
    static final ComponentIdentifier MODAL_FILE = ComponentForBTE.of("modal-file-upload");

    public UploadCommand(MasterServer plugin, DiscordSRV api) {
        super(DiscordPluginProvider.fork(plugin, api));
    }

    @Override
    public MainGuildCommand.Config getCommandConfig() {
        return MainGuildCommand.Config.SCHEMATIC_UPLOAD;
    }

    private static DiscordCommand INSTANCE;
    public static DiscordCommand get(MasterServer plugin, DiscordSRV api) {
        if (INSTANCE == null) {
            // region Construction
            UploadCommand command = new UploadCommand(plugin, api);

            CommandOption file = CommandOption
                .builder(CommandOption.Type.ATTACHMENT, FILE_LABEL, plugin.getLang().get(Language.OPTION_FILE))
                .addDescriptionTranslations(plugin.getLang(Language.OPTION_FILE, LanguageFile::get))
                .build();

            INSTANCE = DiscordCommand.chatInput(IDENTIFIER, LABEL, plugin.getLang().get(Language.CMD_UPLOAD_DESC))
                .addOption(file)
                .addDescriptionTranslations(plugin.getLang(Language.CMD_UPLOAD_DESC, LanguageFile::get))
                .addNameTranslations(plugin.getLang(Language.CMD_UPLOAD_NAME, LanguageFile::get))
                .setEventHandler(command)
                .build();
            // endregion
        }

        return INSTANCE;
    }

    @Override @NotNull
    protected Task<?> acceptNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                       @NotNull InteractionInstance session) {
        MasterServer.info("Accepting " + session + " For uploading schematic");

        if(session.getInstance() == null) return Task.completed(null);

        DiscordInteractionWithModal response = execution.getInteraction();
        Interaction interaction = session.getInstance();
        if (!interaction.uploads().isEmpty()) {
            // attachment added beforehand, no need to open modal
            response.execute(() -> this.upload(interaction, response::send));

            return Task.completed(null);
        }

        return response.supply(() -> {
            // Supply modal to upload files
            LanguageFile lang = plugin.getLang(response.locale());
            AttachmentUpload.Builder fileUpload = AttachmentUpload
                .create(MODAL_FILE.getDiscordIdentifier())
                .setMaxValues(10) // Can upload up to 10 files by discord API limit
                .setMinValues(1)
                .setRequired(true);

            ModalComponent<?> fileDisplay = () -> lang
                .getPair(Language.MODAL_FILE)
                .as(Language::asDiscordLabel)
                .apply(fileUpload::build)
                .asJDA();

            ComponentIdentifier modalID;
            modalID = session.newComponent(IDENTIFIER_MODAL);
            // endregion

            MasterServer.info("Sending modal of ID: " + modalID);

            return DiscordModal
                .builder(modalID, "Upload Schematic File to Master Server")
                .addComponent(fileDisplay)
                .build();
        });
    }

    @Override @NotNull
    protected Task<Interaction> createNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                                 @Nullable UUID linkedUUID) {
        UUID playerUUID = Objects.requireNonNull(linkedUUID);
        Message.Attachment attachment = execution.getAttachment(FILE_LABEL);
        final Interaction interaction = new Interaction(playerUUID,
            attachment != null?  List.of(attachment.getProxy()) : List.of()
        );

        return Task.completed(interaction);
    }

    @Override
    protected @NotNull Task<Interaction> acceptModal(@NotNull ReceivedEvent<DiscordModalInteractionEvent> session) {
        DiscordModalInteractionEvent event = session.getEvent();
        DiscordModalExecution.WithHook<Interaction> execution = new DiscordModalExecution.WithHook<>(plugin, event);
        DiscordInteractionWithHook<Interaction> response = execution.getInteraction();
        LanguageFile lang = plugin.getLang(event.getGuildLocale());

        return response.supply(() -> {
            Interaction info = session.getInstance();

            return new Interaction(info.playerUUID(), execution
                .getAttachment(MODAL_FILE.getDiscordIdentifier())
                .stream()
                .map(Message.Attachment::getProxy)
                .toList());
        }).then(info -> this.upload(info, response::send));
    }

    protected Task<Interaction> upload(Interaction info,
                                       BiConsumer<MinecraftComponent, SendableDiscordMessage> consumer) {
        Path folder = this.getSchematicFolder(info.playerUUID());
        FileUploadTask task = new FileUploadTask(this, info.uploads(), folder);
        Map<String, String> resultMap = new HashMap<>(info.uploads().size());

        if(folder.toFile().mkdirs())
            MasterServer.info("Created schematic folder for player UUID: " + info.playerUUID());

        return task.submit(resultMap).thenApply(finished -> {
            List<ContainerChildComponent> exported = new ArrayList<>();
            Color accentColor;

            Runnable header = () -> {
                exported.add(TextDisplay.of("## Schematic File uploaded\n"));
                // collect our files
                for(@Nullable String filename : finished) {
                    exported.add(TextDisplay.of("```//schem load " + filename + "```"));
                }
            };

            if(finished.size() == 0) {
                // No file is exported somehow
                accentColor = Constants.RED;
                exported.add(Separator.createDivider(Separator.Spacing.SMALL));
                exported.add(TextDisplay.of(
                        "## :warning: Sorry! we couldn't upload any file :(\n"
                                + "Please check your upload contents and try again, or contact support for help."
                ));
            }
            else if(finished.size() < info.uploads().size()) {
                header.run();

                // Some file(s) is missing in final export
                List<String> errors = new ArrayList<>();

                // lookup missing files
                task.lookup(Map.copyOf(resultMap), (originalName, fileName) -> {
                    if(fileName == null) errors.add(originalName);
                });

                String errorDisplay = "## :warning: We couldn't upload some file(s)\n"
                        + "The following file(s) couldn't be uploaded:\n```"
                        + String.join(", ", errors) + "```\n"
                        + "Please check your upload contents and try again, or contact support for help.";

                accentColor = Constants.ORANGE;
                exported.add(Separator.createDivider(Separator.Spacing.SMALL));
                exported.add(TextDisplay.of(errorDisplay));
            }
            else {
                header.run();
                accentColor = Constants.GREEN;
            }

            Container container = Container.of(exported).withAccentColor(accentColor);
            SendableDiscordMessage message = SendableDiscordMessage
                    .builder()
                    .forceComponentsV2()
                    .addComponent(() -> container)
                    .build();
            consumer.accept(null, message);

            return new Interaction(info.playerUUID(), info.uploads());
        });
    }

    protected record Interaction(UUID playerUUID, List<NamedAttachmentProxy> uploads) {}
}
