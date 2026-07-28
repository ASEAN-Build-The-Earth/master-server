package asia.buildtheearth.asean.discord.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public interface SessionDispatcher<S> {
    @NotNull
    ComponentDispatcher<S> getInstance();

    @NotNull
    InteractionSessionEvents getEventHandler();

    @NotNull
    UUID startNew(S session);

    /**
     * Get the session instance, packed as optional.
     *
     * <p>The outer optional packed whether the given session UUID is alive or not.</p>
     * The inner optional packed the session instance where:<ol>
     *     <li>Present: The instance existed with a session value {@link S}</li>
     *     <li>Empty: The instance existed as an empty value, e.g. {@link Void}</li>
     * </ol>
     *
     * @see #get(UUID) #get(UUID) to unpack them as Nullable instead
     * @param sessionUUID The session UUID
     * @return Optional of the session instance by UUID
     */
    @NotNull
    Optional<Optional<S>> getPacked(@NotNull UUID sessionUUID);

    @Nullable
    default S get(@NotNull UUID sessionUUID) {
        return getPacked(sessionUUID).flatMap(Function.identity()).orElse(null);
    }

    void put(@NotNull UUID sessionUUID, S session);

    void invalidate(@NotNull UUID sessionUUID);
}
