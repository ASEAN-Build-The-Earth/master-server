package asia.buildtheearth.asean.commands.discord.schematic;

import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.exception.SessionCreationException;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.discord.Text;
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
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordTextInput;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class GetCommand extends AbstractSchematicCommand<GetCommand.Interaction> {
    private static final String LABEL = "get";
    private static final String NAME_LABEL = "name";
    private static final String PRIVATE_LABEL = "private";
    static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("slash-schematic-get-command");
    static final ComponentIdentifier MODAL_NAME = ComponentForBTE.of("modal-file-name");

    public GetCommand(MasterServer plugin, DiscordSRV api) {
        super(DiscordPluginProvider.fork(plugin, api));
    }

    @Override
    public MainGuildCommand.Config getCommandConfig() {
        return MainGuildCommand.Config.SCHEMATIC_GET;
    }

    private static DiscordCommand INSTANCE;
    public static DiscordCommand get(MasterServer plugin, DiscordSRV api) {
        if (INSTANCE == null) {
            // region Construction
            GetCommand command = new GetCommand(plugin, api);

            CommandOption fineName = CommandOption
                    .builder(CommandOption.Type.STRING, NAME_LABEL, plugin.getLang().get(Language.OPTION_NAME))
                    .addDescriptionTranslations(plugin.getLang(Language.OPTION_NAME, LanguageFile::get))
                    .build();

            CommandOption ephemeral = CommandOption
                    .builder(CommandOption.Type.BOOLEAN, PRIVATE_LABEL, plugin.getLang().get(Language.OPTION_PRIVATE))
                    .addDescriptionTranslations(plugin.getLang(Language.OPTION_PRIVATE, LanguageFile::get))
                    .build();

            INSTANCE = DiscordCommand.chatInput(IDENTIFIER, LABEL, plugin.getLang().get(Language.CMD_GET_DESC))
                    .addOption(fineName)
                    .addOption(ephemeral)
                    .addDescriptionTranslations(plugin.getLang(Language.CMD_GET_DESC, LanguageFile::get))
                    .addNameTranslations(plugin.getLang(Language.CMD_GET_NAME, LanguageFile::get))
                    .setEventHandler(command)
                    .build();
            // endregion
        }

        return INSTANCE;
    }

    @Override @NotNull
    protected Task<?> acceptNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                       @NotNull InteractionInstance session) {
        MasterServer.info("Accepting " + session + " For getting schematic");
        DiscordInteractionWithModal response = execution.getInteraction();

        if (session.getInstance().fileNames() != null)
            return Task.completed(null);

        // no prior name input, open modal for user
        return response.supply(() -> {
            // Supply modal to upload files
            LanguageFile lang = plugin.getLang(response.locale());

            DiscordTextInput fileNameInput = DiscordTextInput
                .builder(MODAL_NAME, "Ignored", DiscordTextInput.Style.SHORT)
                .setMinLength(1)
                .setMaxLength(100)
                .setPlaceholder("Filename.schem, another-file, directory/folder/filename.schem")
                .build();

            ModalComponent<?> inputLabel = () -> lang
                .getPair(Language.MODAL_NAME)
                .as(Language::asDiscordLabel)
                .apply(fileNameInput::asJDA)
                .asJDA();

            ComponentIdentifier modalID;
            modalID = session.newComponent(IDENTIFIER_MODAL);
            // endregion

            MasterServer.info("Sending modal of ID: " + modalID);

            return DiscordModal
                .builder(modalID, "Get Schematic File")
                .addComponent(inputLabel)
                .build();
        });
    }

    @Override @NotNull
    protected Task<Interaction> createNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                                 @Nullable UUID linkedUUID) {
        UUID playerUUID = Objects.requireNonNull(linkedUUID);
        File file = this.getSchematicFolder(playerUUID).toFile();
        DiscordInteractionWithHook<?> response = execution.getInteraction();
        if(!file.exists() || !file.isDirectory()) {
            response.setEphemeral(true);
            return Task.failed(new SessionCreationException(() -> "Nothing to find! your schematic folder is empty."));
        }

        @Nullable String input = execution.getString(NAME_LABEL);
        boolean ephemeral = !Boolean.FALSE.equals(execution.getBoolean(PRIVATE_LABEL));

        if(input == null) return Task.completed(new Interaction(playerUUID, ephemeral, null));

        // Instant response if input filename is set
        Interaction interaction = Interaction.of(playerUUID, ephemeral, input);
        List<File> fileList = execute(interaction);

        if (fileList.isEmpty()) {
            response.setEphemeral(true);
            return Task.failed(new SessionCreationException(() -> "File not found."));
        }

        response.setEphemeral(ephemeral);
        response.execute(() -> this.response(fileList, response));

        return Task.completed(Interaction.of(playerUUID, ephemeral, fileList));
    }

    @Override
    protected @NotNull Task<Interaction> acceptModal(@NonNull ReceivedEvent<DiscordModalInteractionEvent> session) {
        DiscordModalInteractionEvent event = session.getEvent();
        DiscordModalExecution.WithHook<Interaction> execution = new DiscordModalExecution.WithHook<>(plugin, event);
        DiscordInteractionWithHook<Interaction> response = execution.getInteraction();
        LanguageFile lang = plugin.getLang(event.getGuildLocale());
        Interaction info = session.getInstance();

        execution.getInteraction().setEphemeral(info.ephemeral());
        return execution.getInteraction().supply(() -> {
            String input = execution.getString(MODAL_NAME.getDiscordIdentifier());
            List<File> fileList = execute(Interaction.of(info.playerUUID(), info.ephemeral(), input));
            if (fileList.isEmpty()) {
                response.setEphemeral(true);
                response.send(new Text("File not found."));
                return null;
            }

            this.response(fileList, response);

            return Interaction.of(info.playerUUID(), info.ephemeral(), fileList);
        });
    }

    protected List<File> execute(Interaction info) {
        Path path = this.getSchematicFolder(info.playerUUID());
        File file = path.toFile();
        List<File> fileList = new ArrayList<>();

        for(String name : info.fileNames()) {
            String directory = checkFilePath(name);
            File searchDir = directory == null ? file : path.resolve(directory).toFile();
            String prefix = directory == null ? name : name.substring(directory.length());
            File[] files = searchDir.listFiles((dir, fileName) -> fileName.startsWith(prefix));

            if(files != null)
                fileList.addAll(List.of(files));
        }

        return fileList;
    }

    protected void response(List<File> fileList,
                            DiscordInteractionWithHook<?> response) {

        List<ContainerChildComponent> uploads = new ArrayList<>();
        uploads.add(TextDisplay.of("## Found " + fileList.size() + " files"));

        // collect our files
        for(File foundFile : fileList) {
            FileUpload fileUpload = FileUpload.fromData(foundFile);
            uploads.add(FileDisplay.fromFile(fileUpload));
        }

        Container container = Container.of(uploads).withAccentColor(Constants.BLUE);
        SendableDiscordMessage message = SendableDiscordMessage
                .builder()
                .forceComponentsV2()
                .addComponent(() -> container)
                .build();

        response.send((MinecraftComponent) null, message);
    }

    public @Nullable String checkFilePath(@NotNull String fileName) {
        int i = fileName.lastIndexOf('/');
        return i < 0 ? null : fileName.substring(0, i + 1);
    }

    protected record Interaction(UUID playerUUID,
                                 boolean ephemeral,
                                 String[] fileNames
    ) {
        @Contract("_, _, _ -> new")
        static @NotNull Interaction of(UUID playerUUID, boolean ephemeral, String input) {
            return new Interaction(playerUUID, ephemeral, input.split("\\s*,\\s*"));
        }

        @Contract("_, _, _ -> new")
        static @NotNull Interaction of(UUID playerUUID, boolean ephemeral, List<File> fileList) {
            String[] fileNames = fileList.stream().map(File::getPath).toArray(String[]::new);
            return new Interaction(playerUUID, ephemeral, fileNames);
        }
    }




}
