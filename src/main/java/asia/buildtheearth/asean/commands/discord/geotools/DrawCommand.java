package asia.buildtheearth.asean.commands.discord.geotools;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.exception.CommandRuntimeException;
import asia.buildtheearth.asean.commands.discord.exception.SessionCreationException;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.api.PluginProvider;
import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.io.LanguageFile;
import asia.buildtheearth.asean.discord.InteractionSession;
import asia.buildtheearth.asean.discord.abstraction.DiscordCommandExecution;
import asia.buildtheearth.asean.discord.api.ComponentDispatcher;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import asia.buildtheearth.asean.geotools.SchematicExport;
import asia.buildtheearth.asean.geotools.projection.MinecraftProjection;
import asia.buildtheearth.asean.geotools.worldedit.WorldEditGeometryWriter;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.fastasyncworldedit.core.Fawe;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;

import static asia.buildtheearth.asean.commands.discord.geotools.ExportCommand.*;
import static asia.buildtheearth.asean.commands.discord.GeoToolsCommand.DRAW_LABEL;

public class DrawCommand extends ExportSchematic {
    private static final ComponentIdentifier IDENTIFIER = ComponentForBTE.of("slash-geotools-draw-command");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DrawCommand command,
                                     CommandOption... options) {
        if (INSTANCE == null) {
            MasterServer plugin = command.getPlugin();
            DiscordCommand.ChatInputBuilder builder = DiscordCommand
                .chatInput(IDENTIFIER, DRAW_LABEL, plugin.getLang().get(Language.COMMAND_DESC))
                .addDescriptionTranslations(plugin.getLang(Language.COMMAND_DESC, LanguageFile::get));

            for(CommandOption option : options) builder.addOption(option);

            INSTANCE = builder
                    .setContexts(true, false)
                    .setGuildId(plugin.getMainGuildID())
                    .setEventHandler(command)
                    .setDefaultPermission(DiscordCommand.DefaultAccess.EVERYONE)
                    .build();
        }

        return INSTANCE;
    }

    private DrawSession session = null;

    public DrawCommand(ExportCommand parent) {
        super(parent);
        this.getAPI().registerModule(this);
    }

    @Override
    public MainGuildCommand.Config getCommandConfig() {
        return MainGuildCommand.Config.GEO_TOOLS_DRAW;
    }

    @Override
    public boolean requireLinked() {
        return true;
    }

    @Override
    public String getCommandName() {
        return DRAW_LABEL;
    }

    public InteractionSession<?> getSession() {
        if(this.session == null)
            this.session = new DrawSession(this);

        return this.session;
    }

    @Override
    protected @NotNull Task<Interaction> createNewSession(@NotNull DiscordCommandExecution.WithModal execution,
                                                          @Nullable UUID linkedUUID) {
        UUID playerUUID = Objects.requireNonNull(linkedUUID);

        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);

//        if(player == null)
//            return Task.failed(new SessionCreationException(Language.MSG_PLAYER_NOT_FOUND));

        if(!player.isOnline())
            return Task.failed(new SessionCreationException(Language.MSG_PLAYER_NOT_ONLINE));

        return super.createNewSession(execution, linkedUUID);
    }

    protected class DrawSession extends ExportSession {

        public DrawSession(DiscordPluginProvider plugin) {
            super(plugin);
        }

        @Override
        public ExportTask newTask(@NotNull Session session, Path path) {
            return new DrawTask(DrawSession.this, session, path);
        }

        @Override
        public Task<Session> submit(@NotNull File cacheFolder,
                                    @Nullable Session session,
                                    @NotNull BiConsumer<Component, SendableDiscordMessage> consumer) {
            // TODO: might need some specific implementations
            return super.submit(cacheFolder, session, consumer);
        }
    }

    protected static class DrawTask extends ExportTask {

        protected DrawTask(@NotNull PluginProvider provider,
                           @NotNull Session session,
                           @NotNull Path path) {
            super(provider, session, path);
        }

        @Override
        public Task<@Nullable String> convert(@NotNull String filename) {

            return super.convert(filename);
        }

        @Override
        public @Nullable String schematicExport(@NotNull SchematicExport export,
                                                @NotNull String filename) throws Throwable {

            UUID playerUUID = this.session.linkedUUID().orElseThrow();

            Player player = Bukkit.getPlayer(playerUUID);

            if(player == null || !player.isOnline())
                throw new CommandRuntimeException(Language.MSG_PLAYER_NOT_ONLINE);

            export.setProjection(MinecraftProjection::getASEAN);
            export.setFormat(this.session::format);
            export.setPattern(this.session.pattern());
            export.normalizeZ(this.session.height());

            BukkitPlayer bukkitPlayer = WorldEditPlugin.getInstance().wrapPlayer(player);

            try(EditSession edit = Fawe.instance()
                    .getWorldEdit()
                    .newEditSessionBuilder()
                    .world(bukkitPlayer.getWorld())
                    .actor(bukkitPlayer)
                    // .setSideEffectSet(SideEffectSet.none())
                    // .actor(new WorldEditCommandActor(this.plugin))
                    .build()) {

                // edit.setExtent(new PassthroughExtent(buffer));

                WorldEditGeometryWriter writer = new WorldEditGeometryWriter(
                    edit,
                    MinecraftProjection.getASEAN(),
                    this.session.pattern()
                );

                export.convert(writer::writeGeometry);
            }

            return null;
        }
    }


    enum Language implements LangEntry {
        // Parent Command Info
        COMMAND_DESC("description"),
        COMMAND_NAME("name"),

        MSG_PLAYER_NOT_ONLINE("messages", "player-not-online"),
        MSG_PLAYER_NOT_FOUND("messages", "player-not-found"),
        ;

        private static final String PARENT_PATH = "slash-commands.geo-tools-" + DRAW_LABEL + '.';
        private final String path;

        Language(String... path) {
            this.path = String.join(".", path);
        }

        @Override @Contract(pure = true)
        public @NotNull String getKey() {
            return PARENT_PATH + path;
        }

    }

}
