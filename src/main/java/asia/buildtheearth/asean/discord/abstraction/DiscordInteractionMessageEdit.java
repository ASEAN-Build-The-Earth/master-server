package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.utils.SendableDiscordMessageUtil;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.concurrent.CompletableFuture;

public class DiscordInteractionMessageEdit<T> extends DiscordInteractionWithHook<T> {
    protected final IMessageEditCallback editCallback;

    public DiscordInteractionMessageEdit(MasterServer plugin,
                                            AbstractInteractionWithHookEvent<? extends IMessageEditCallback> event) {
        super(plugin, event);
        this.editCallback = event.asJDA();
    }

    @Override
    protected void sendResponse(SendableDiscordMessage message) {
        if (message == null) return;
        MessageEditData data = SendableDiscordMessageUtil.toJDAEdit(message);
        InteractionHook interactionHook = hook.get();

        if (interactionHook != null)
            interactionHook.editOriginal(data).queue();
        else
            editCallback.editMessage(data).queue();
    }

    public Task<T> supply(CheckedSupplier<T> supplier) {
        CompletableFuture<InteractionHook> task = this.editCallback.deferEdit().submit();

        MasterServer.info("Editing Task...");
        return Task.of(task).then(interaction -> {
            this.hook.set(interaction);
            MasterServer.info("Got hook");
            return this.plugin.scheduler().supply(supplier);
        });
    }

}
