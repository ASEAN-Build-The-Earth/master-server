package asia.buildtheearth.asean.discord;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.abstraction.AbstractModule;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.discord.api.SessionDispatcher;
import asia.buildtheearth.asean.discord.api.ComponentDispatcher;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import asia.buildtheearth.asean.discord.api.InteractionSessionEvents;
import asia.buildtheearth.asean.discord.api.OwnableSession;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordSelectMenuInteractionEvent;
import com.discordsrv.api.task.Task;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * An abstract class.
 *
 * <p>Subclasses may override #from... handler to receive interaction events.</p>
 * @param <S> Typed store that will be used in this session. (define it beforehand)
 *
 * @see #acceptButton(ReceivedEvent) handle button
 * @see #acceptModal(ReceivedEvent) handle modal
 * @see #acceptSelectMenu(ReceivedEvent)  handle select menu
 */
public abstract class InteractionSession<S>
        extends AbstractModule implements ComponentDispatcher<S>, InteractionSessionEvents, OwnableSession {

    private final SessionDispatcher<S> dispatcher;

    /**
     * The owner cache for this {@link OwnableSession}
     */
    private final Cache<UUID, Set<Long>> cache;

    public InteractionSession(DiscordPluginProvider plugin) {
        super(plugin);

        this.dispatcher = this
            .getDiscordSRV()
            .sessionModule()
            .register(this);
        this.cache = this.plugin.caffeineBuilder().build();
    }

    public @Nullable S getSession(@NotNull UUID sessionUUID) {
        return this.dispatcher.get(sessionUUID);
    }

    public void assignOwner(UUID sessionUUID, Long... ownerIDs) {
        this.cache.put(sessionUUID, Set.of(ownerIDs));
    }

    public void assignOwner(UUID sessionUUID, long ownerIDs) {
        this.cache.put(sessionUUID, Set.<Long>of(ownerIDs));
    }

    @Override
    public @NotNull Set<Long> getOwners(@NotNull UUID sessionUUID) {
        return Optional.of(sessionUUID).map(this.cache::getIfPresent).orElseGet(Set::of);
    }

    @NotNull
    protected Task<@Nullable S> acceptModal(@NotNull ReceivedEvent<DiscordModalInteractionEvent> event) {
        throw new UnsupportedOperationException(
            this.getClass().getName()
            + "#acceptModal is unimplemented for this interaction.\n"
            + "event: " + event.getClass().getSimpleName() + "\nsession: " + event.getInstance()
        );
    }

    @NotNull
    protected Task<@Nullable S> acceptSelectMenu(@NotNull ReceivedEvent<DiscordSelectMenuInteractionEvent> event) {
        throw new UnsupportedOperationException(
            this.getClass().getName()
            + "#acceptSelectMenu is unimplemented for this interaction.\n"
            + "event: " + event.getClass().getSimpleName() + "\nsession: " + event.getInstance()
        );
    }

    @NotNull
    protected Task<@Nullable S> acceptButton(@NotNull ReceivedEvent<DiscordButtonInteractionEvent> event) {
        throw new UnsupportedOperationException(
            this.getClass().getName()
            + "#acceptButton is unimplemented for this interaction.\n"
            + "event: " + event.getClass().getSimpleName() + "\nsession: " + event.getInstance()
        );
    }

    @Override
    public final void onModalInteractionEvent(@NotNull DiscordModalInteractionEvent event) {
        handle(event, this::acceptModal);
    }

    @Override
    public final void onSelectMenuInteractionEvent(@NotNull DiscordSelectMenuInteractionEvent event) {
        handle(event, this::acceptSelectMenu);
    }

    @Override
    public final void onButtonInteractionEvent(@NotNull DiscordButtonInteractionEvent event) {
        handle(event, this::acceptButton);
    }

    @Override
    public final void onRemoval(UUID sessionUUID, S session, @NotNull RemovalCause cause) {
        if(sessionUUID == null) return;

        // EXPLICIT, REPLACED, COLLECTED, EXPIRED, SIZE
        // Anything that was evicted is handled,
        // except if it is replaced since we treat this as updating the session.
        if(cause != RemovalCause.REPLACED) {
            MasterServer.info("Accepting " + sessionUUID + " removal for " + (session != null? session.toString() : "empty session"));
            this.acceptRemoval(sessionUUID, session);
            this.cache.invalidate(sessionUUID);
        }
    }

    /**
     * Message sent as reaction when the session is interacted, but it has already expired.
     *
     * @return The message to sent
     */
    public SendableDiscordMessage whenExpired() {
        return SendableDiscordMessage.builder().setContent("This interaction has expired.").build();
    }

    /**
     * Message sent as reaction when the session is interacted by a user that does not own it.
     *
     * @return The message to sent
     */
    public SendableDiscordMessage whenNotOwned() {
        return SendableDiscordMessage.builder().setContent("You do not own this").build();
    }

    @Override
    public <E extends AbstractInteractionWithHookEvent<?>> void acceptUnknownEvent(@NotNull E event,
                                                                                   @NotNull Supplier<SendableDiscordMessage> supplier) {
        InteractionSessionEvents.super.acceptUnknownEvent(event, supplier);
    }

    private <E extends AbstractInteractionWithHookEvent<?>>
    void handle(@NotNull E event, @NotNull Function<ReceivedEvent<E>, Task<S>> handler) {
        ComponentForBTE.WithSession session = getComponentChecked(event);

        UUID sessionUUID = session.sessionUUID();
        long userID = event.getUser().getId();

        // Guard permission
        if(!this.isOwner(sessionUUID, userID)) {
            Runnable deny = () -> this.replyEphemeral(event, this::whenNotOwned);
            plugin.scheduler().run(deny);
            return;
        }

        MasterServer.info("Handling session " + sessionUUID);
        MasterServer.info("session: " + session);
        MasterServer.info("event: " + event.asJDA().getClass().getName());
        MasterServer.info("from: " + this.getClass().getName());

        Function<Optional<S>, Runnable> accept = optional -> () -> {
            S instance = optional.orElse(null);
            ReceivedEvent<E> forward = new ReceivedEvent<>(event, sessionUUID, instance);
            Task<S> task = handler.apply(forward);

            task.whenComplete((update, error) -> {
                if (error != null) {
                    // ...handle it
                    MasterServer.error(error);
                    return;
                }

                // Refresh if the session is still present
                this.dispatcher.getPacked(sessionUUID).ifPresent(present -> {
                    // Extend the session if retrieved information isn't null
                    if(update != null || present.isEmpty())
                        this.putSession(sessionUUID, update);
                        // Otherwise we accept them as expired if the handler's return value are null
                    else this.endSession(sessionUUID); // transition from not-empty session into null value
                });
            });
        };

        Runnable handling = this.dispatcher.getPacked(sessionUUID)
            .map(accept)
            .orElse(() -> this.acceptUnknownEvent(event, this::whenExpired));

        plugin.scheduler().run(handling);
    }

    /**
     * Get session-created component from discord event, and throws if invalid.
     *
     * @param <E> Event of type
     * @param event The event to retrieve its component
     * @return Component Identifier
     * @throws IllegalStateException if the event does not have valid component identifier,
     *         functionally would never throw because parent session manager handle for us.
     */
    @NotNull private <E extends AbstractInteractionWithHookEvent<?>>
    ComponentForBTE.WithSession getComponentChecked(@NotNull E event) {

        Optional<ComponentForBTE.WithSession> optional = ComponentForBTE
            .parseFromDiscord(event.getIdentifier())
            .flatMap(ComponentForBTE::getSession);

        return optional.orElseThrow(() -> new IllegalStateException(
            "Illegal event received from session manager! "
            + "An interactive event must have BTE component identifier. "
            + "(Event: " + event.getClass().getSimpleName() + ')')
        );
    }

    /**
     * Return plot media folder by ID and create if not exist.
     *
     * @param sessionUUID session of this cache
     * @return The cache folder as {@link File} instance
     */
    public @NotNull File prepareCacheFolder(@NotNull UUID sessionUUID) {
        return this.plugin.getDataFolder().toPath().resolve("cache/session/" + sessionUUID.toString()).toFile();
    }

    protected void acceptRemoval(@NotNull UUID sessionUUID, @Nullable S session) {
        File cache = this.prepareCacheFolder(sessionUUID);
        if(!cache.exists() || !cache.isDirectory()) return;

        Optional<Long> count = Optional.ofNullable(cache.listFiles())
            .map(files -> Arrays.stream(files).filter(File::isFile)
            .map(File::delete).filter(deleted -> deleted).count());

        boolean cleared = cache.delete();
        count.ifPresent(deleted -> MasterServer.info("Cleared " + deleted + " files from session cache."));
        if(!cleared) MasterServer.error("Failed to delete cache directory for session " + sessionUUID);
    }

    @NotNull @Contract("_ -> new")
    public InteractionInstance startNewSession(S session) {
        UUID sessionUUID = this.dispatcher.startNew(session);
        return new InteractionInstance(sessionUUID, session);
    }

    @NotNull
    public UUID startNewSessionUUID(S session) {
        return this.dispatcher.startNew(session);
    }

    public void endSession(UUID sessionUUID) {
        this.dispatcher.invalidate(sessionUUID);
    }

    public void putSession(UUID sessionUUID, S session) {
        this.dispatcher.put(sessionUUID, session);
    }

    public class InteractionInstance implements ComponentDispatcher.Instance<S>, OwnableSession.Instance {
        private final S instance;
        private final UUID sessionUUID;

        public InteractionInstance(@NotNull UUID sessionUUID, S instance) {
            this.instance = instance;
            this.sessionUUID = sessionUUID;
        }

        /**
         * {@inheritDoc}
         */
        public UUID getSessionUUID() {
            return sessionUUID;
        }

        @Override
        public S getInstance() {
            return instance;
        }

        /**
         * {@inheritDoc}
         */
        public String getDispatcherID() {
            return InteractionSession.this.getDispatcherID();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onRemoval(UUID sessionUUID, S session, @NotNull RemovalCause cause) {
            InteractionSession.this.onRemoval(sessionUUID, session, cause);
        }

        @Override
        public void assignOwner(UUID sessionUUID, @NonNull @NotNull Long... ownerIDs) {
            InteractionSession.this.assignOwner(sessionUUID, ownerIDs);
        }

        @Override
        public void assignOwner(UUID sessionUUID, long ownerID) {
            InteractionSession.this.assignOwner(sessionUUID, ownerID);
        }

        @Override
        public @NotNull  Set<Long> getOwners(@NotNull UUID sessionUUID) {
            return InteractionSession.this.getOwners(sessionUUID);
        }
    }

    /**
     * Pair receivable events to its dispatching session UUID.
     *
     * @param <E> Event of type received.
     */
    protected final class ReceivedEvent<E extends AbstractInteractionWithHookEvent<?>> extends InteractionInstance {

        private final E event;

        private ReceivedEvent(@NotNull E event,
                              @NotNull UUID sessionUUID,
                              S instance) {
            super(sessionUUID, instance);
            this.event = event;
        }

        public E getEvent() {
            return this.event;
        }
    }
}
