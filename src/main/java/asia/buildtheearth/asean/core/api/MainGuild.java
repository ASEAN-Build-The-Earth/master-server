package asia.buildtheearth.asean.core.api;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface MainGuild {

    boolean isAvailable();

    /**
     * Get the main guild's instance.
     *
     * @return Discord Guild instance.
     */
    DiscordGuild get();

    DiscordRole getOfficialBuilderRole();

    DiscordGuildMember getGuildMember(long userID);

    DiscordGuildMember getGuildMember(long userID, Supplier<@Nullable DiscordGuild> scope);

    MainGuildCommand getSlashCommand(MainGuildCommand.Config command);

    void invalidateCaches();

    /**
     * Check that the userID meet this permission
     */
    boolean checkPermission(long userID, @NotNull MainGuildPermission permission);

    /**
     * Do a verbose check for this permission
     *
     * @param userID The discord user ID (snowflake) to check
     * @param permission The permission
     * @return True if this user passed the permission, Null if either user or configs
     *         required to check the permission is not correctly set.
     */
    @Nullable Boolean checkPermissionVerbose(long userID, @NotNull MainGuildPermission permission);

    /**
     * Do a verbose role check for the provided roles set
     *
     * @param userID The discord user ID (snowflake) to check
     * @param roles Set of role(s) to check
     * @return Stream of same ordered result, with Null value if either user
     *         or configs required to check the role is not correctly set.
     */
    Stream<@Nullable Boolean> checkRoleVerbose(long userID, @NotNull Set<MainGuildRole> roles);

    /**
     * Check if the userID has this role
     */
    default boolean checkRole(long userID, @NotNull MainGuildRole roles) {
        return checkAllRole(userID, Set.of(roles));
    }

    /**
     * Check that the userID contains all role
     */
    default boolean checkAllRole(long userID, @NotNull Set<MainGuildRole> roles) {
        return checkRoleVerbose(userID, roles).allMatch(Predicate.isEqual(Boolean.TRUE));
    }

    /**
     * Check that the userID has any 1 of roles provided
     */
    default boolean checkAnyRole(long userID, @NotNull Set<MainGuildRole> roles) {
        return checkRoleVerbose(userID, roles).anyMatch(Predicate.isEqual(Boolean.TRUE));
    }

    /**
     * Shortcut to check for {@link MainGuildPermission#OFFICIAL_BUILDER} permission check.
     */
    default boolean isOfficialBuilder(long userID) {
        return checkPermission(userID, MainGuildPermission.OFFICIAL_BUILDER);
    }
}
