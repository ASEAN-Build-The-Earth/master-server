package asia.buildtheearth.asean.core;

import asia.buildtheearth.asean.Constants;
import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.commands.discord.GeoToolsCommand;
import asia.buildtheearth.asean.commands.discord.ReloadCommand;
import asia.buildtheearth.asean.commands.discord.SchematicCommand;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.discord.DiscordSessionModule;
import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.module.Module;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DiscordSRVModule implements Module {
    private final MasterServer plugin;

    /**
     * API is initialized lazily because we want the instance of this class to never be null,
     * this is actually effectively final.
     */
    private DiscordSRV api;

    private DiscordSessionModule sessionModule;

    private final MainGuildImpl guild;

    private boolean subscribed = false;

    public DiscordSRVModule(MasterServer plugin) {
        this.plugin = plugin;
        this.guild = new MainGuildImpl(plugin, this::provideGuild);
    }

    private @Nullable DiscordGuild provideGuild(Long snowflake) {
        if(this.api == null || snowflake == null) return null;
        else return this.api.discordAPI().getGuildById(snowflake);
    }

    public void initialize(DiscordSRV instance) {
        this.api = instance;
        this.api.registerModule(this);
    }

    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public void enable() {
        MasterServer.info("JDA has started, subscribing...");
        this.subscribeAndValidateJDA();
        subscribed = true;
    }

    public void subscribeAndValidateJDA() {
        DiscordPluginProvider provider = DiscordPluginProvider.fork(this.plugin, this.api);

        this.sessionModule = new DiscordSessionModule(provider);
        this.api.registerModule(sessionModule);


        // REGISTERED / ALREADY_REGISTERED / NAME_ALREADY_IN_USE / TOO_MANY_COMMANDS

        DiscordCommand.RegistrationResult reload = this.api.discordAPI()
                .registerCommand(ReloadCommand.get(this.plugin, this.api));

        MasterServer.info("/reload Command Registration: " + reload.name());

        DiscordCommand.RegistrationResult geotools = this.api.discordAPI()
            .registerCommand(GeoToolsCommand.get(this.plugin, this.api));

        MasterServer.info("/geotools Command Registration: " + geotools.name());

        DiscordCommand.RegistrationResult schematic = this.api.discordAPI()
                .registerCommand(SchematicCommand.get(this.plugin, this.api));

        MasterServer.info("/schematic Command Registration: " + schematic.name());
    }

    public DiscordSessionModule sessionModule() {
        return this.sessionModule;
    }

    public MainGuildImpl mainGuild() {
        return this.guild;
    }

    @Subscribe @SuppressWarnings("unused")
    public void onAccountLinked(@NotNull AccountLinkedEvent event) {
        long snowflake = event.getUserId();
        JDAEntity<Member> member = this.guild.retrieveDiscordMember(snowflake);
        if(member != null)
            this.guild.put(snowflake, member);
        else this.guild.invalidate(snowflake);
        MasterServer.info("Linked account on discord");
    }

    @Subscribe @SuppressWarnings("unused")
    public void onAccountUnlinked(@NotNull AccountUnlinkedEvent event) {
        this.guild.invalidate(event.getUserId());
        MasterServer.info("Unlinked account on discord");
    }

}
