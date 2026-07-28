package asia.buildtheearth.asean.discord.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public interface OwnableSession {

    /**
     * Allow the check for {@link #isOwner(UUID, long) #isOwner} to pass
     * even if no owner is assigned on this session.
     *
     * @return Default: true
     */
    default boolean allowOwnerless() {
        return true;
    }

    void assignOwner(UUID sessionUUID, @NotNull Long... ownerIDs);

    void assignOwner(UUID sessionUUID, long ownerID);

    /**
     * Get the owner of this interaction instance, if assigned
     *
     * @return Set of owner IDs as snowflake long, null if not assigned.
     */
    @NotNull Set<Long> getOwners(@NotNull UUID sessionUUID);

    default boolean isOwner(UUID sessionUUID, long ownerID) {
        return getOwners(sessionUUID).contains(ownerID) || allowOwnerless();
    }

    interface Instance extends OwnableSession {

        UUID getSessionUUID();

        default void assignOwner(@NotNull Long... ownerIDs) {
            assignOwner(getSessionUUID(), ownerIDs);
        }

        default void assignOwner(long ownerID) {
            assignOwner(getSessionUUID(), ownerID);
        }

        default @Nullable Set<Long> getOwners() {
            return getOwners(getSessionUUID());
        }

        default boolean isOwner(long ownerID) {
            return isOwner(getSessionUUID(), ownerID);
        }
    }
}
