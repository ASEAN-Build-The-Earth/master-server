package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.discord.api.DiscordInteraction;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public sealed abstract class DiscordCommandExecution<
    I extends AbstractDiscordInteraction<?>
> implements DiscordInteraction<I> {

    private final CommandInteractionPayload interactionPayload;
    private final I interaction;

    public DiscordCommandExecution(I interaction,
                                   @NotNull DiscordChatInputInteractionEvent event) {
        this.interaction = interaction;
        this.interactionPayload = event.asJDA();
    }

    @Nullable
    public String getString(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsString() : null;
    }

    @Nullable
    public Boolean getBoolean(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsBoolean() : null;
    }

    @Nullable
    public Message.Attachment getAttachment(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsAttachment() : null;
    }

    @Nullable
    public Long getLong(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsLong() : null;
    }

    @Nullable
    public Double getDouble(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsDouble() : null;
    }

    public String getID() {
        return interactionPayload.getId();
    }

    public I getInteraction() {
        return this.interaction;
    }

    @Contract("_, _ -> new")
    public static @NotNull WithModal withModal(MasterServer plugin, DiscordChatInputInteractionEvent event) {
        return new WithModal(plugin, event);
    }

    @Contract("_, _ -> new")
    public static <T> @NotNull WithHook<T> withHook(MasterServer plugin, DiscordChatInputInteractionEvent event) {
        return new WithHook<>(plugin, event);
    }

    public static final class WithHook<T>
    extends DiscordCommandExecution<DiscordInteractionWithHook<T>> {

        public WithHook(MasterServer plugin,
                        DiscordChatInputInteractionEvent event) {
            super(new DiscordInteractionWithHook<>(plugin, event), event);
        }
    }

    public static final class WithModal
    extends DiscordCommandExecution<DiscordInteractionWithModal> {

        public WithModal(MasterServer plugin,
                         DiscordChatInputInteractionEvent event) {
            super(new DiscordInteractionWithModal(plugin, event), event);
        }
    }
}