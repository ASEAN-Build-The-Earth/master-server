package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.utils.SendableDiscordMessageUtil;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordInteractionWithHook<T> extends AbstractDiscordInteraction<T> {
    protected final AtomicReference<InteractionHook> hook;

    /**
     * Construct a discord interaction instance.
     *
     * @param plugin Plugin instance to inherit
     * @param event JDA interaction event to wrap
     * @throws ClassCastException if the JDA event cannot be forked as {@link IReplyCallback},
     * we assume all discord interaction should be able to send reply callbacks/message
     */
    public DiscordInteractionWithHook(@NotNull MasterServer plugin,
                                      @NotNull AbstractInteractionWithHookEvent<? extends Interaction> event) {
        super(plugin, event);
        this.hook = new AtomicReference<>();
    }

    @Override
    protected void sendResponse(SendableDiscordMessage message) {
        InteractionHook interactionHook = hook.get();
        boolean ephemeral = isEphemeral.get();
        MessageCreateData data = SendableDiscordMessageUtil.toJDASend(message);
        if (interactionHook != null) {
            interactionHook.sendMessage(data).setEphemeral(ephemeral).queue();
        } else {
            replyCallback.reply(data).setEphemeral(ephemeral).queue();
        }
    }

    @Override
    public void execute(Runnable runnable) {
        this.replyCallback.deferReply(isEphemeral.get()).queue(interaction -> {
            this.hook.set(interaction);
            this.plugin.scheduler().run(runnable);
        });
    }

    @Override
    public Task<T> supply(CheckedSupplier<T> supplier) {
        CompletableFuture<InteractionHook> task = this.replyCallback.deferReply(isEphemeral.get()).submit();

        MasterServer.info("Supplying Task...");
        return Task.of(task).then(interaction -> {
            this.hook.set(interaction);
            MasterServer.info("Got hook");
            return this.plugin.scheduler().supply(supplier);
        });
    }
}
