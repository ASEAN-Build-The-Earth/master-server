package asia.buildtheearth.asean.commands.discord.schematic;

import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import com.discordsrv.api.discord.entity.interaction.component.component.LabelComponent;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordLabel;
import com.fastasyncworldedit.core.Fawe;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static asia.buildtheearth.asean.commands.discord.AbstractDiscordCommand.CommandWithModal;

/**
 * Discord parent command, alias {@code /schematic} get/upload
 *
 * @param <T> Child command execution
 * @param <S> Child command session information
 * @see GetCommand
 * @see UploadCommand
 */
public abstract class AbstractSchematicCommand<S> extends CommandWithModal<S> {
    static final String IDENTIFIER_MODAL = "modal";

    public AbstractSchematicCommand(DiscordPluginProvider plugin) {
        super(plugin);
    }

    public @NotNull Path getSchematicFolder(@NotNull UUID playerUUID) {
        return Fawe.platform().getDirectory().toPath().resolve("schematics/" + playerUUID.toString());
    }

    @Override
    public boolean requireLinked() {
        return true;
    }

    public enum Language implements LangEntry {
        // Parent Command Info
        GROUP_DESC("description"),
        GROUP_NAME("name"),

        CMD_GET_DESC("schematic-get", "description"),
        CMD_GET_NAME("schematic-get", "name"),
        CMD_UPLOAD_DESC("schematic-upload", "description"),
        CMD_UPLOAD_NAME("schematic-upload", "name"),
        // Command Options
        OPTION_NAME( "options", "name"),
        OPTION_FILE( "options", "file"),
        OPTION_PRIVATE( "options", "private"),
        // Modal
        MODAL_FILE("modal", "file-upload"),
        MODAL_NAME("modal", "file-name")
        ;


        private static final String PARENT_PATH = "slash-commands.schematic.";
        private final String path;

        Language(String... path) {
            this.path = String.join(".", path);
        }

        @Override @Contract(pure = true)
        public @NotNull String getKey() {
            return PARENT_PATH + path;
        }

        protected interface DiscordLabelConsumer extends Function<LabelComponent<?>, DiscordLabel> {
            @Override
            DiscordLabel apply(LabelComponent<?> labelComponent);
        }

        protected interface EntryConsumer<T> extends BiFunction<String, String, Consumer<T>> {
            @Override
            Consumer<T> apply(String label, String description);
        }

        protected interface EntryBiConsumer<T, U> extends BiFunction<String, String, BiConsumer<T, U>> {
            @Override
            BiConsumer<T, U> apply(String label, String description);
        }

        @Contract(pure = true)
        protected static @NotNull Language.DiscordLabelConsumer asDiscordLabel(String label, String desc) {
            return value -> DiscordLabel.of(label, desc, value);
        }

    }
}
