package asia.buildtheearth.asean.commands.discord;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.exception.CommandRuntimeException;
import asia.buildtheearth.asean.commands.discord.exception.LackingPermissionException;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.api.MainGuildPermission;
import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.io.LangToken;
import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.discord.abstraction.*;
import asia.buildtheearth.asean.discord.api.InteractionExecution;
import com.discordsrv.api.discord.entity.Snowflake;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.profile.Profile;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static asia.buildtheearth.asean.core.api.MainGuildPermission.*;

/**
 * Abstract for all discord's guild command.
 */
public abstract class AbstractDiscordCommand<S, T extends DiscordCommandExecution<?>>
    extends AbstractDiscordExecution<
            S, // Storing session interaction
            T, // Executing discord command
            DiscordChatInputInteractionEvent  // From Discord slash command
    > {

    public AbstractDiscordCommand(DiscordPluginProvider plugin) {
        super(plugin);
    }

    public abstract MainGuildCommand.Config getCommandConfig();

    public abstract boolean requireLinked();

    @Override
    public void execute(@NotNull T execution) {
        InteractionExecution<?> response = execution.getInteraction();
        long userID = response.getUser().getId();

        Task<Optional<LackingPermissionException>> permissionTask = this.getPlugin()
            .scheduler()
            .supply(() -> Optional.of(userID).map(this::checkLackingPermission));

        Task<S> createTask = permissionTask.then(lacking -> {
            if(lacking.isPresent()) // lacking required permission
                return Task.failed(lacking.get());

            if(requireLinked()) { // special task if account linking is required
                Function<UUID, Task<S>> task = linkedUUID -> this.createNewSession(execution, linkedUUID);
                return this.forwardLinkedUserUUID(userID, task);
            }

            // else, accept the execution
            return this.createNewSession(execution, null);
        });

        createTask.whenFailed(exception -> {
            if(exception instanceof LackingPermissionException lacking)
                this.rejectCommand(response, lacking);
            else if(exception instanceof CommandRuntimeException known)
                this.sendException(response, known);
            else this.sendException(response, locale ->
                "Unknown error occurred\n```" + exception.getMessage() + "```"
            );
        });

        Task<?> executionTask = createTask.then(session -> this.acceptNewSession(
                execution,
                this.startNewSession(session)
        ));

        // TODO: log notification?
        // executionTask.whenComplete((success, failure) -> {});
    }

    /**
     * Check if userID is lacking required permission or not.
     *
     * @param userID discord user snowflake ID
     * @return Non-null as a language entry if the user DOES lack permission to continue.
     */
    @Nullable
    public LackingPermissionException checkLackingPermission(long userID) {
        MainGuildCommand command = getMainGuild().getSlashCommand(this.getCommandConfig());

        // MUST be official builder of linking is required
        MainGuildPermission permission = this.requireLinked()?
                (command.permission() != DENY? OFFICIAL_BUILDER : DENY)
                : command.permission();
        boolean hasPermission = getMainGuild().checkPermission(userID, permission);

        if(!hasPermission) {
            // no permission...
            LackingPermission reason = switch (permission) {
                case DENY -> LackingPermission.COMMAND_IS_DISABLED;
                case OFFICIAL_BUILDER -> LackingPermission.NOT_OFFICIAL_BUILDER;
                case NONE, OWNER, ADMINISTRATOR -> LackingPermission.NO_PERMISSION;
            };

            return new LackingPermissionException(reason, permission.name());
        }

        if(!command.role().isEmpty()) { // role required for this command
            boolean hasRole = getMainGuild().checkAnyRole(userID, command.role());

            if(!hasRole) {
                LackingPermission reason = LackingPermission.REQUIRED_ROLE;
                Iterable<String> roles = command.role().stream().map(Enum::name)::iterator;
                String cause = String.join(", ", roles);
                return new LackingPermissionException(reason, cause);
            }
        }

        return null;
    }

    public void sendException(@NotNull InteractionExecution<?> response,
                              @NotNull Function<Locale, String> applier) {
        String message = applier.apply(response.locale());
        response.setEphemeral(true);
        response.send(new Text(message));
    }

    public void sendException(@NotNull InteractionExecution<?> response,
                              @NotNull CommandRuntimeException exception) {
        this.sendException(response, locale -> getPlugin()
            .getLang(locale)
            .get(exception.getLang())
        );
    }

    public void rejectCommand(@NotNull InteractionExecution<?> response,
                              @NotNull LackingPermissionException exception) {
        this.sendException(response, locale -> {
            LangEntry lang = exception.getLang();
            String message = getPlugin().getLang(locale).get(lang);
            final String cause;

            // Special cause for official builder, replaced with role tag
            if(OFFICIAL_BUILDER.name().equals(exception.getProblem())) {
                cause = Optional.ofNullable(getMainGuild().getOfficialBuilderRole())
                        .map(Snowflake::getId)
                        .map(Long::toUnsignedString)
                        .map(snowflake -> "<@&" + snowflake + '>')
                        .orElse(exception.getProblem());
            }
            else cause = exception.getProblem();

            return message.replace(LangToken.VALUE, cause);
        });
    }

    /**
     * Forward a userID as linked Minecraft profile.
     *
     * @param userID The userID to check for linked Minecraft account
     * @param task The forwarding task to apply as new session,
     *             applying with the linked account UUID
     * @return The result of the forwarding task
     */
    public Task<S> forwardLinkedUserUUID(long userID, @NotNull Function<UUID, Task<S>> task) {
        Task<? extends Profile> profileTask = this.getAPI().profileManager().getProfile(userID);

        return profileTask.then(profile -> {
            // profile#isLinked() check for: playerUUID() != null && userId() != null
            if(true) // TODO: TESTING ONLY
                return task.apply(UUID.fromString("68d4a076-4b97-48a6-9ffb-24672d1e325d"));
            else if(!profile.isLinked()) {
                LackingPermission reason = LackingPermission.NOT_LINKED;
                String cause = OFFICIAL_BUILDER.name();
                return Task.failed(new LackingPermissionException(reason, cause));
            }
            else return task.apply(Objects.requireNonNull(profile.playerUUID()));
        });
    }

    /**
     * Accept this command execution.
     *
     * @param execution The execution
     * @param linkedUUID Minecraft linked account UUID,
     *                   Non-null ONLY IF {@linkplain #requireLinked()} returns True
     * @return Task to create new {@link S} session instance.
     */
    @NotNull
    protected abstract Task<S> createNewSession(@NotNull T execution,
                                                @Nullable UUID linkedUUID);

    /**
     * Accept this execution's instance.
     * Invoked when session is first started {@link #startNewSession(S)}
     *
     * @param execution The execution
     * @param session Session instance that is created to be continued by this command.
     * @return Task of any completion.
     */
    @NotNull
    protected abstract Task<?> acceptNewSession(@NotNull T execution,
                                                @NotNull InteractionInstance session);

    public static abstract class CommandWithHook<S, T> extends AbstractDiscordCommand<
            S, // Storing session interaction
            DiscordCommandExecution.WithHook<T> // Executing with hook
        > {
        public CommandWithHook(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        protected DiscordCommandExecution.WithHook<T> forward(DiscordChatInputInteractionEvent event) {
            return DiscordCommandExecution.withHook(getPlugin(), event);
        }
    }

    public static abstract class CommandWithModal<S> extends AbstractDiscordCommand<
            S, // Storing session interaction
            DiscordCommandExecution.WithModal // Executing with modal
        > {
        public CommandWithModal(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        protected DiscordCommandExecution.WithModal forward(DiscordChatInputInteractionEvent event) {
            return DiscordCommandExecution.withModal(getPlugin(), event);
        }
    }

    protected enum LackingPermission implements LangEntry {
        NO_PERMISSION("no-permission"),
        REQUIRED_ROLE("required-role"),
        COMMAND_IS_DISABLED("command-is-disabled"),
        NOT_OFFICIAL_BUILDER("not-official-builder"),
        NOT_LINKED("not-linked")
        ;

        private static final String PARENT_PATH = "slash-commands.interactions.authorisation.denied-";
        private final String key;

        LackingPermission(String reason) {
            this.key = PARENT_PATH + reason;
        }

        @Override @Contract(pure = true)
        public @NotNull String getKey() {
            return this.key;
        }
    }
}
