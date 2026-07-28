package asia.buildtheearth.asean.commands.discord;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.discord.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.discord.abstraction.DiscordInteractionWithHook;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.UUID;

import static asia.buildtheearth.asean.commands.discord.AbstractDiscordCommand.CommandWithHook;

public class ReloadCommand extends CommandWithHook<Void, Void> {

    private static final String LABEL = "reload";
    private static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("slash-reload-command");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(MasterServer plugin, DiscordSRV api) {
        if (INSTANCE == null) {
            ReloadCommand command = new ReloadCommand(DiscordPluginProvider.fork(plugin, api));
            DiscordCommand.ChatInputBuilder builder = DiscordCommand
                    .chatInput(IDENTIFIER, LABEL, "Reload config and caches of Master Server application.");
            api.registerModule(command);

            INSTANCE = builder
                    .setContexts(true, false)
                    .setGuildId(plugin.getMainGuildID())
                    .setEventHandler(command)
                    .setDefaultPermission(DiscordCommand.DefaultAccess.EVERYONE)
                    .build();
        }

        return INSTANCE;
    }

    public ReloadCommand(DiscordPluginProvider plugin) {
        super(plugin);
    }

    @Override
    public MainGuildCommand.Config getCommandConfig() {
        return MainGuildCommand.Config.RELOAD;
    }

    @Override
    public boolean requireLinked() {
        return false;
    }

    @Override
    protected @NotNull Task<Void> createNewSession(@NotNull DiscordCommandExecution.WithHook<Void> execution,
                                                   @Nullable UUID linkedUUID) {
        return Task.completed(null).thenApply(Void.class::cast);
    }

    @Override
    protected @NotNull Task<?> acceptNewSession(@NotNull DiscordCommandExecution.WithHook<Void> execution,
                                                @NotNull InteractionInstance session) {
        DiscordInteractionWithHook<Void> response = execution.getInteraction();
        response.setEphemeral(true);
        return response.supply(() -> {

            File file = new File(this.plugin.getDataFolder(), "config.yml");
            this.getPlugin().reloadConfig(file);

            response.send(new Text("Config file reloaded"));

            Locale[] locales = this.getPlugin().getAvailableLocale().toArray(Locale[]::new);

            this.getPlugin().reloadLang(locales);

            response.send(new Text("Language files reloaded"));

            this.getMainGuild().invalidateCaches();
            this.getDiscordSRV().sessionModule().invalidateCaches();

            response.send(new Text("Invalidated all caches"));
            return null;
        }).mapException((error) -> {
            response.send(new Text("Error occurred!\n```" + error.getMessage() + "```"));
            return null;
        });
    }
}
