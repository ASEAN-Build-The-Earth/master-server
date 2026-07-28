package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IMessageEditCallback;

/**
 * {@link DiscordInteractionWithHook} with a message edit hook
 *
 * @see #asEdit()
 */
public class DiscordInteractionWithEdit<T> extends DiscordInteractionWithHook<T> {
    protected final DiscordInteractionMessageEdit<T> edit;

    /**
     * Construct a discord interaction instance.
     *
     * @param plugin Plugin instance to inherit
     * @param event JDA interaction event to wrap
     * @apiNote as of JDA v6.4.1 , would not throw runtime ClassCastException
     * as descendants of IMessageEditCallback all implements IReplyCallback
     */
    public DiscordInteractionWithEdit(MasterServer plugin,
                                      AbstractInteractionWithHookEvent<? extends IMessageEditCallback> event) {
        super(plugin, event);
        this.edit = new DiscordInteractionMessageEdit<>(plugin, event);
    }

    public DiscordInteractionMessageEdit<T> asEdit() {
        return this.edit;
    }
}
