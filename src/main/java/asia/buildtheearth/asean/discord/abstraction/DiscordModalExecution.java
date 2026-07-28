package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.discord.api.DiscordInteraction;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed class DiscordModalExecution<
    I extends AbstractDiscordInteraction<?>
> implements DiscordInteraction<I> {
    private final ModalInteractionEvent modal;
    private final I interaction;

    public DiscordModalExecution(I interaction,
                                 @NotNull DiscordModalInteractionEvent event) {
        // super(plugin);
        this.interaction = interaction;
        this.modal = event.asJDA();
    }

    public String getString(String label) {
        ModalMapping mapping = modal.getValue(label);
        return mapping != null ? mapping.getAsString() : null;
    }

    public List<String> getSelect(String label) {
        ModalMapping mapping = modal.getValue(label);
        return mapping != null ? mapping.getAsStringList() : null;
    }

    public List<Message.Attachment> getAttachment(String label) {
        ModalMapping mapping = modal.getValue(label);
        return mapping != null ? mapping.getAsAttachmentList() : null;
    }

    @Override
    public I getInteraction() {
        return this.interaction;
    }

    public static final class WithHook<T>
    extends DiscordModalExecution<DiscordInteractionWithEdit<T>> {

        public WithHook(MasterServer plugin,
                        DiscordModalInteractionEvent event) {
            super(new DiscordInteractionWithEdit<>(plugin, event), event);
        }
    }
}
