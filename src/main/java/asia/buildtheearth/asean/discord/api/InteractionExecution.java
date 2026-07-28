package asia.buildtheearth.asean.discord.api;

import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.utils.ComponentUtil;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/**
 *
 * @param <T> Execution Type
 */
public interface InteractionExecution<T> {
    DiscordUser getUser();

    Locale locale();

    void setEphemeral(boolean ephemeral);

    default void send(Text... texts) {
        send(Arrays.asList(texts));
    }

    default void send(Collection<Text> texts) {
        send(texts, Collections.emptyList());
    }

    void send(Collection<Text> texts, Collection<Text> extra);

    default void send(@Nullable MinecraftComponent minecraftComponent, @Nullable SendableDiscordMessage discordMessage) {
        send(ComponentUtil.fromAPI(minecraftComponent), discordMessage);
    }

    void send(@Nullable Component minecraftComponent, @Nullable SendableDiscordMessage discordMessage);

    void execute(Runnable execution);

    Task<T> supply(CheckedSupplier<T> supplier);
}
