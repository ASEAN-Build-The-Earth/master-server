package asia.buildtheearth.asean.discord.api;

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public interface ComponentDispatcher<S> extends RemovalListener<UUID, S> {
    /**
     * The dispatcher ID which will be embedded to each dispatching components.
     *
     * <p>NOTE: Override this method to make this dispatcher persist across jvm restarts.</p>
     * {@snippet : Integer.toHexString(System.identityHashCode(this))}
     * @return The dispatcherID, default as 32bits integer hashcode encoded as HEX string.
     */
    @Subst("b70c05cd")
    default String getDispatcherID() {
        return Integer.toHexString(System.identityHashCode(this));
    }

    /**
     * Inherited from {@link com.github.benmanes.caffeine.cache.RemovalListener}
     * invoked when the session is evicted.
     *
     * @param sessionUUID The UUID of evicting session.
     * @param session The session instance if existed.
     * @param cause The eviction cause by caffeine cache.
     */
    void onRemoval(UUID sessionUUID, S session, @NotNull RemovalCause cause);

    /**
     * Create a new component within this {@linkplain  ComponentDispatcher dispatcher}, for a session (UUID).
     *
     * @param sessionUUID UUID of this session.
     * @param identifier The identifier of this new component.
     * @return DiscordSRV compatible component identifier.
     */
    default ComponentIdentifier newComponent(@NotNull java.util.UUID sessionUUID,
                                             @Subst("MAX31-dash-annotated-identifier")
                                             @NotNull String identifier) {
        return newComponentForBTE(sessionUUID, identifier).getParent();
    }

    /**
     * Create a new component within this {@linkplain  ComponentDispatcher dispatcher}, for a session (UUID).
     *
     * @param sessionUUID UUID of this session.
     * @param identifier The identifier of this new component.
     * @return BTE component instance, use {@link ComponentForBTE#getParent()}
     *         or {@link #newComponent(java.util.UUID, String)} for DiscordSRV component.
     */
    default ComponentForBTE newComponentForBTE(@NotNull java.util.UUID sessionUUID,
                                             @Subst("MAX31-dash-annotated-identifier")
                                             @NotNull String identifier) {
        ComponentForBTE.WithSession session = ComponentForBTE.session(getDispatcherID(), sessionUUID);

        return session.create(identifier);
    }

    /**
     * Instance based dispatcher where it is dependent on the session UUID.
     *
     * @param <S> The session scope
     * @see #getSessionUUID()
     */
    interface Instance<S> extends ComponentDispatcher<S> {

        /**
         * Get the session UUID of this event interaction.
         *
         * @return UUID as {@linkplain java.util.UUID java.util.UUID}
         */
        UUID getSessionUUID();

        S getInstance();

        /**
         * Create a new component within this {@linkplain  ComponentDispatcher dispatcher}, for a session (UUID).
         *
         * @param identifier The identifier of this new component.
         * @return DiscordSRV compatible component identifier.
         */
        default ComponentIdentifier newComponent(@Subst("MAX31-dash-annotated-identifier")
                                                 @NotNull String identifier) {
            return newComponent(getSessionUUID(), identifier);
        }

        /**
         * Create a new component within this {@linkplain  ComponentDispatcher dispatcher}, for a session (UUID).
         *
         * @param identifier The identifier of this new component.
         * @return BTE component instance, use {@link ComponentForBTE#getParent()}
         *         or {@link #newComponent(java.util.UUID, String)} for DiscordSRV component.
         */
        default ComponentForBTE newComponentForBTE(@Subst("MAX31-dash-annotated-identifier")
                                                   @NotNull String identifier) {
            return newComponentForBTE(getSessionUUID(), identifier);
        }
    }
}
