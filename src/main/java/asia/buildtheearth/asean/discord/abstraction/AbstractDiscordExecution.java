package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.discord.InteractionSession;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;

import java.util.function.Consumer;

/**
 * Execute a discord event of type {@code E}, into an interaction of type {@code T}.
 * With the session information of type {@code S}.
 *
 * @param <S> Session Type
 * @param <T> Type of the interaction execution
 * @param <E> Event type which the interaction originate from
 *
 * @see #forward(E)
 * @see #execute(T)
 */
public abstract class AbstractDiscordExecution<S, T, E>
    extends InteractionSession<S>
    implements Consumer<E> {

    public AbstractDiscordExecution(DiscordPluginProvider plugin) {
        super(plugin);
    }

    @Override
    public final void accept(E event) {
        T execution = forward(event);
        execute(execution);
    }

    protected abstract T forward(E event);

    public abstract void execute(T execution);

    public static abstract class CommandWithHook<S, T> extends AbstractDiscordExecution<
                S, // Storing session interaction
                DiscordCommandExecution.WithHook<T>, // Executing with modal
                DiscordChatInputInteractionEvent  // From Discord slash command
                > {
        public CommandWithHook(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        protected DiscordCommandExecution.WithHook<T> forward(DiscordChatInputInteractionEvent event) {
            return DiscordCommandExecution.withHook(getPlugin(), event);
        }
    }

    public static abstract class CommandWithModal<S> extends AbstractDiscordExecution<
            S, // Storing session interaction
            DiscordCommandExecution.WithModal, // Executing with modal
            DiscordChatInputInteractionEvent  // From Discord slash command
    > {
        public CommandWithModal(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        protected DiscordCommandExecution.WithModal forward(DiscordChatInputInteractionEvent event) {
            return DiscordCommandExecution.withModal(getPlugin(), event);
        }
    }

    public static abstract class ButtonWithModal<S> extends AbstractDiscordExecution<
            S, // Storing session interaction
            DiscordInteractionWithModal, // Executing with modal
            DiscordButtonInteractionEvent  // From Discord button
    > {
        public ButtonWithModal(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        protected DiscordInteractionWithModal forward(DiscordButtonInteractionEvent event) {
            return new DiscordInteractionWithModal(getPlugin(), event);
        }
    }
}
