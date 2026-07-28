package asia.buildtheearth.asean.core;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.abstraction.AbstractPluginProvider;
import asia.buildtheearth.asean.core.api.MainGuildCommand;
import asia.buildtheearth.asean.core.api.MainGuild;
import asia.buildtheearth.asean.core.api.MainGuildPermission;
import asia.buildtheearth.asean.core.api.MainGuildRole;
import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPermissionHolder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Represent ASEAN BTE Main discord guild.
 * Used to manage major {@linkplain IPermissionHolder permission holder}:
 * our official builder role, our builders for checks inside application.
 *
 * @see #isOfficialBuilder(long)
 */
public class MainGuildImpl extends AbstractPluginProvider implements MainGuild {
    private final Cache<Long, JDAEntity<? extends IPermissionHolder>> cache;
    private final LoadingCache<MainGuildRole, Set<Long>> roleSnowflakeCache;
    private final LoadingCache<MainGuildCommand.Config, MainGuildCommand> slashCommandConfig;

    private final AtomicBoolean hook = new AtomicBoolean(false);
    private final Function<Long, DiscordGuild> provider;

    /**
     * Construct a new Main Guild instance
     *
     * @param plugin   plugin instance
     * @param provider provider function to provide guild instance
     */
    MainGuildImpl(@NotNull MasterServer plugin,
                  @NotNull Function<Long, @Nullable DiscordGuild> provider) {
        super(plugin);
        this.provider = provider;
        this.cache = this.plugin.caffeineBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();
        this.roleSnowflakeCache = this.plugin.caffeineBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(this.getPlugin()::getMainGuildRole);
        this.slashCommandConfig = this.plugin.caffeineBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .build(this.getPlugin()::parseCommandConfig);
    }

    /**
     * Is the main guild available by the latest cached value.
     *
     * @return True if available
     */
    @Override
    public boolean isAvailable() {
        return this.hook.get();
    }

    @Override
    public void invalidateCaches() {
        this.cache.invalidateAll();
        this.roleSnowflakeCache.invalidateAll();
        this.slashCommandConfig.invalidateAll();
    }

    @Override
    public @Nullable DiscordGuild get() {
        Long guildID = this.plugin.getMainGuildID();
        DiscordGuild guild = Optional.ofNullable(guildID)
            .map(this.provider)
            .orElse(null);
        this.hook.set(guild != null);
        return guild;
    }

    protected void put(long snowflake,
                       JDAEntity<? extends IPermissionHolder> instance) {
        this.cache.put(snowflake, instance);
    }

    protected void invalidate(long snowflake) {
        this.cache.invalidate(snowflake);
    }

    protected JDAEntity<Role> retrieveRole(long snowflake) {
        DiscordGuild guild = this.get();
        return (guild != null) ? guild.getRoleById(snowflake) : null;
    }

    protected JDAEntity<Member> retrieveDiscordMember(long snowflake,
                                                      @NotNull Supplier<@Nullable DiscordGuild> scope) {
        DiscordGuild guild = scope.get();
        return (guild != null) ? guild.getMemberById(snowflake) : null;
    }

    protected JDAEntity<Member> retrieveDiscordMember(long snowflake) {
        return retrieveDiscordMember(snowflake, this::get);
    }

    @Override
    public @Nullable DiscordRole getOfficialBuilderRole() {
        Long roleID = this.plugin.getBuilderRoleID();
        if (roleID == null) return null;
        return (DiscordRole) this.cache.get(roleID, this::retrieveRole);
    }

    public @Nullable DiscordRole getRole(Long roleID) {
        if (roleID == null) return null;
        return (DiscordRole) this.cache.get(roleID, this::retrieveRole);
    }

    @Override
    public @Nullable DiscordGuildMember getGuildMember(long userID) {
        return (DiscordGuildMember) this.cache.get(userID, this::retrieveDiscordMember);
    }

    @Override
    public @Nullable DiscordGuildMember getGuildMember(long userID,
                                                       @NotNull Supplier<@Nullable DiscordGuild> scope) {
        return (DiscordGuildMember) this.cache.get(userID, snowflake -> this.retrieveDiscordMember(snowflake, scope));
    }

    @Override
    public MainGuildCommand getSlashCommand(MainGuildCommand.Config command) {
        return this.slashCommandConfig.get(command);
    }

    @Override
    public Stream<@Nullable Boolean> checkRoleVerbose(long userID, @NotNull Set<MainGuildRole> roles) {
        DiscordGuildMember member = getGuildMember(userID);
        if (member == null) return Stream.of(new Boolean[roles.size()]);

        Stream.Builder<@Nullable Boolean> permission = Stream.builder();

        role:
        for (MainGuildRole entry : roles) {
            // Retrieve role ID from config path and check it against member
            Set<Long> set = this.roleSnowflakeCache.get(entry);

            if(set == null || set.isEmpty()) {
                MainGuildPermission defaultPermission = entry.getDefaultPermission();
                permission.accept(this.checkPermissionVerbose(userID, defaultPermission));

                MasterServer.warning(
                    "Role ID of '" + entry.getName()
                    + "' not configured! checking against its default permission '"
                    + defaultPermission.name() + "' instead."
                );
                continue;
            }

            for(Long roleID : set) {
                DiscordRole role = this.getRole(roleID);
                if (role == null || !member.hasRole(role)) continue;

                MasterServer.info("Passed for role: " + Long.toUnsignedString(roleID));
                permission.accept(Boolean.TRUE);
                continue role;
            }
            permission.accept(Boolean.FALSE);
        }

        return permission.build();
    }

    public boolean checkPermission(long userID, @NotNull MainGuildPermission permission) {
        Boolean result = this.checkPermissionVerbose(userID, permission);

        return Boolean.TRUE.equals(result);
    }

    public @Nullable Boolean checkPermissionVerbose(long userID, @NotNull MainGuildPermission permission) {
        return switch (permission) {
            case OWNER -> Optional.ofNullable(this.getGuildMember(userID))
                .map(DiscordGuildMember::isOwner)
                .orElse(null);
            case ADMINISTRATOR -> Optional.ofNullable(this.getGuildMember(userID))
                .map(JDAEntity::asJDA)
                .<Function<Permission, Boolean>>map(member -> member::hasPermission)
                .orElse(unknown -> null)
                .apply(Permission.ADMINISTRATOR);
            case OFFICIAL_BUILDER -> Optional.ofNullable(this.getOfficialBuilderRole())
                .flatMap(role -> Optional.ofNullable(this.getGuildMember(userID, role::getGuild))
                .map(member -> member.hasRole(role)))
                .orElse(null);
            case DENY -> Boolean.FALSE;
            case NONE -> Boolean.TRUE;
        };
    }
}
