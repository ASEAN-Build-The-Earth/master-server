package asia.buildtheearth.asean.discord.api;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.utils.SendableDiscordMessageUtil;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordSelectMenuInteractionEvent;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Receivable events during an interaction session.
 *
 * @param <S> Session information type of this interaction.
 */
public interface InteractionSessionEvents {
    void onModalInteractionEvent(@NotNull DiscordModalInteractionEvent event);

    void onSelectMenuInteractionEvent(@NotNull DiscordSelectMenuInteractionEvent event);

    void onButtonInteractionEvent(@NotNull DiscordButtonInteractionEvent event);

    @MustBeInvokedByOverriders
    default <
        E extends AbstractInteractionWithHookEvent<?>
    > void acceptUnknownEvent(@NotNull E event, @NotNull Supplier<SendableDiscordMessage> supplier) {
        replyEphemeral(event, supplier).thenAccept(hook -> {
            // If possible, disable the component so no-one can trigger it again
            if(event.asJDA() instanceof GenericComponentInteractionCreateEvent interaction) {
                if(interaction.getMessage().isUsingComponentsV2()) {
                    MessageComponentTree tree = interaction.getMessage().getComponentTree().asDisabled();
                    interaction.getMessage().editMessageComponents(tree).useComponentsV2().queue();
                }
            }
        });
    }

    /**
     * Default handler to reply to an event with ephemeral message.
     *
     * @param event The receiving event.
     * @param supplier Supplier to sendable discord message to reply.
     * @param <E> Type of the receiving event.
     */
    default <E extends AbstractInteractionWithHookEvent<?>>
    CompletableFuture<InteractionHook> replyEphemeral(@NotNull E event,
                                                      @NotNull Supplier<SendableDiscordMessage> supplier) {
        SendableDiscordMessage message = supplier.get();
        MessageCreateData data = SendableDiscordMessageUtil.toJDASend(message);

        if (event.asJDA() instanceof IReplyCallback reply) {
            Function<MessageCreateData, RestAction<?>> sendable = reply.isAcknowledged()? reply.getHook()::sendMessage : reply::reply;
            return ((ReplyCallbackAction) sendable.apply(data)).setEphemeral(true).submit();
        }
        else MasterServer.error("Illegal request to reply a message for the event: " + event.getClass().getSimpleName());
        return CompletableFuture.completedFuture(null);
    }
}
