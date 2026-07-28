package asia.buildtheearth.asean.discord.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.abstraction.AbstractPluginProvider;
import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.discord.api.InteractionExecution;
import asia.buildtheearth.asean.utils.SendableDiscordMessageUtil;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represent any discord interactions that can be replied to.
 */
public abstract class AbstractDiscordInteraction<T> extends AbstractPluginProvider implements InteractionExecution<T> {
    private final AbstractInteractionEvent<?> event;

    protected final IReplyCallback replyCallback;

    protected final AtomicBoolean isEphemeral = new AtomicBoolean(true);

    /**
     * Construct a discord interaction instance.
     *
     * @param plugin Plugin instance to inherit
     * @param event JDA interaction event to wrap
     * @throws ClassCastException if the JDA event cannot be forked as {@link IReplyCallback},
     * we assume all discord interaction should be able to send reply callbacks/message
     */
    public AbstractDiscordInteraction(@NotNull MasterServer plugin,
                                      @NotNull AbstractInteractionEvent<? extends Interaction> event) {
        super(plugin);
        this.event = event;
        // Fork internal executable interfaces
        this.replyCallback = (IReplyCallback) event.asJDA();
    }

    @Override
    public DiscordUser getUser() {
        return event.getUser();
    }

    @Override
    public Locale locale() {
        return event.getUserLocale();
    }

    @Override
    public void setEphemeral(boolean ephemeral) {
        isEphemeral.set(ephemeral);
    }

    @Override
    public void send(Collection<Text> texts, Collection<Text> extra) {
        StringBuilder builder = new StringBuilder();
        EnumMap<Text.Formatting, Boolean> formats = new EnumMap<>(Text.Formatting.class);

        for (Text text : texts) {
            // This starts the text with its formatting token, ex. **xxx
            render(text, builder, formats);
        }
        // This end the text by its format, ex. **xxx** is bold
        verifyStyle(builder, formats, null);

        if (!extra.isEmpty()) {
            builder.append("\n\n");
            for (Text text : extra) {
                render(text, builder, formats);
            }
            verifyStyle(builder, formats, null);
        }

        sendResponse(SendableDiscordMessage.builder().setContent(builder.toString()).build());
    }

    @Override
    public void send(Component minecraftComponent, SendableDiscordMessage discord) {
        if (discord == null) {
            return;
        }
        sendResponse(discord);
    }

    @Override
    public abstract void execute(Runnable execution);

    @Override
    public abstract Task<T> supply(CheckedSupplier<T> supplier);

    protected void sendResponse(SendableDiscordMessage message) {
        boolean ephemeral = isEphemeral.get();
        MessageCreateData data = SendableDiscordMessageUtil.toJDASend(message);
        replyCallback.reply(data).setEphemeral(ephemeral).queue();
    }

    protected void render(Text text, StringBuilder builder, EnumMap<Text.Formatting, Boolean> formats) {
        if (StringUtils.isEmpty(text.content())) return;

        verifyStyle(builder, formats, text);
        builder.append(text.content());
    }

    /**
     * This appends format token to string builder ex. **, ~~
     *
     * @param builder The current building message
     * @param formats Formats lookup map
     * @param text Follow-up content if any
     */
    protected void verifyStyle(@NotNull StringBuilder builder,
                               @NotNull EnumMap<Text.Formatting, Boolean> formats,
                               @Nullable Text text) {
        Text.Formatting[] values = Text.Formatting.values();

        /*
         * Find requested true value, that would mean `thisIs` is true,
         * indicating there exist a format to append its ending token.
         */
        boolean hasFormat = formats.containsValue(true);

        for (int i = 0; i < values.length; i++) {
            /*
             * We need to reverse enum indexing if there exist a format to end,
             * so that it ended like xy__yx and NOT xy__xy
             */
            int index = hasFormat? (values.length - 1) - i : i;

            Text.Formatting format = values[index];
            boolean is = formats.computeIfAbsent(format, key -> false);
            boolean thisIs = text != null && text.discordFormatting().contains(format);

            if (is != thisIs) {
                // should end or start
                builder.append(format.discord());
                formats.put(format, thisIs);
            }
        }
    }

}
