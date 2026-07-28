package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordModal;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.requests.RestAction;

public class DiscordInteractionWithModal extends DiscordInteractionWithHook<DiscordModal> {
    protected final IModalCallback modalCallback;

    /**
     * Construct a discord interaction instance.
     *
     * @param plugin Plugin instance to inherit
     * @param event JDA interaction event to wrap
     * @apiNote as of JDA v6.4.1 , would not throw runtime ClassCastException
     * as descendants of IModalCallback all implements IReplyCallback
     */
    public DiscordInteractionWithModal(MasterServer plugin,
                                       AbstractInteractionWithHookEvent<? extends IModalCallback> event) {
        super(plugin, event);
        this.modalCallback = event.asJDA();
    }

    /**
     * Execute the supplying modal and returns the modal instance.
     *
     * @param supplier Modal supplier to be accepted.
     */
    @Override
    public Task<DiscordModal> supply(CheckedSupplier<DiscordModal> supplier) {
       Task<DiscordModal> task = plugin.scheduler().supply(supplier);

        task.thenApply(DiscordModal::asJDA)
            .thenApply(modalCallback::replyModal)
            .whenSuccessful(RestAction::queue)
            .whenFailed(MasterServer::error);

       return task;
    }
}
