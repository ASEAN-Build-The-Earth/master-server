package asia.buildtheearth.asean.discord;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.abstraction.AbstractModule;
import asia.buildtheearth.asean.core.api.DiscordPluginProvider;
import asia.buildtheearth.asean.discord.api.SessionDispatcher;
import asia.buildtheearth.asean.discord.api.InteractionSessionEvents;
import asia.buildtheearth.asean.discord.api.ComponentDispatcher;
import asia.buildtheearth.asean.discord.api.ComponentForBTE;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordSelectMenuInteractionEvent;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Global manager for discord interactive sessions ex. slash commands
 *
 * <p>This module manage the global cache for any session that create dispatcher from this module.</p>
 *
 * @see #register(ComponentDispatcher)
 */
public class DiscordSessionModule extends AbstractModule implements InteractionSessionEvents {
    private final Map<String, Dispatcher<?>> registry;

    public DiscordSessionModule(DiscordPluginProvider provider) {
        super(provider);

        this.registry = new ConcurrentHashMap<>();
    }

    @Override
    public void enable() {
        this.invalidateCaches();
    }

    @Override
    public void disable() {
        this.forAllCaches(Cache::cleanUp);
        this.registry.clear();
    }

    public void invalidateCaches() {
        this.forAllCaches(Cache::invalidateAll);
    }

    private void forAllCaches(Consumer<Cache<UUID, ? extends Optional<?>>> consumer) {
        this.registry.values()
            .stream()
            .map(Dispatcher::getCache)
            .forEach(consumer);
    }

    @Nullable
    public SessionDispatcher<?> getDispatcher(String dispatcherID) {
        return this.registry.get(dispatcherID);
    }

    /**
     * Fork and register a dispatcher for a session instance to global session cache.
     *
     * @param instance Session manager instance for this dispatcher.
     * @return A new Dispatcher instance
     * @param <S> Session's interaction type
     */
    @NotNull
    public <S,
            V extends ComponentDispatcher<S> & InteractionSessionEvents>
    SessionDispatcher<S> register(@NotNull V instance) {
        final String dispatcherID = instance.getDispatcherID();

        if(this.registry.containsKey(dispatcherID))
            throw new IllegalStateException("Cannot fork new dispatcher of duplicate identifier.");

        MasterServer.info("Registering Session dispatcher for "
            + instance.getClass().getSimpleName()
            + " as " + instance.getDispatcherID());

        final Dispatcher<S> dispatcher = new Dispatcher<>(instance);

        this.registry.put(dispatcherID, dispatcher);

        return dispatcher;
    }

    @Subscribe
    public final void onModalInteractionEvent(@NotNull DiscordModalInteractionEvent event) {
        accept(event, InteractionSessionEvents::onModalInteractionEvent);
    }

    @Subscribe
    public final void onSelectMenuInteractionEvent(@NotNull DiscordSelectMenuInteractionEvent event) {
        accept(event, InteractionSessionEvents::onSelectMenuInteractionEvent);
    }

    @Subscribe
    public final void onButtonInteractionEvent(@NotNull DiscordButtonInteractionEvent event) {
        accept(event, InteractionSessionEvents::onButtonInteractionEvent);
    }

    /**
     * The global handler for DiscordSessionModule.
     * Accept event (if valid) to its registered dispatcher.
     *
     * @param <E> Event of type (Abstracted from DiscordSRV)
     * @param event The event to accept
     * @param dispatching The dispatching function that will be forwarded to
     */
    private <E extends AbstractInteractionWithHookEvent<?>> void accept(@NotNull E event,
                                                                        @NotNull BiConsumer<InteractionSessionEvents, E> dispatching) {
        Optional<ComponentForBTE.WithSession> session = ComponentForBTE
            .parseFromDiscord(event.getIdentifier()) // Parse identifier
            .flatMap(ComponentForBTE::getSession); // Find its session

        Optional<InteractionSessionEvents> dispatcher = session
            .map(ComponentForBTE.WithSession::dispatchingID) // Lookup dispatching ID
            .filter(this.registry::containsKey) // Lookup who owns this dispatching ID
            .map(this.registry::get)
            .map(Dispatcher::getEventHandler);

        dispatcher.ifPresentOrElse(
            /*
             * Forward the dispatcher to its dispatching event if an instance is found.
             */
            handler -> dispatching.accept(handler, event),
            /*
             * Edge case if a session was found, but no dispatcher is provided for it.
             */
            () -> session.map(ignored -> event).ifPresent(this::onUnknownEvent)
        );
    }

    /**
     * Default fallback for unknown component.
     */
    private  <E extends AbstractInteractionWithHookEvent<?>> void onUnknownEvent(@NotNull E event) {
        this.acceptUnknownEvent(event, () -> SendableDiscordMessage.builder()
            .setContent("Unknown Interaction, maybe it has expired?")
            .build()
        );
    }

    @Override
    public <E extends AbstractInteractionWithHookEvent<?>> void acceptUnknownEvent(@NotNull E event,
                                                                                   @NotNull Supplier<SendableDiscordMessage> supplier) {
        this.plugin.scheduler().run(() -> InteractionSessionEvents.super.acceptUnknownEvent(event, supplier));
    }

    @NotNull @Contract("-> new")
    public UUID generateSessionKey() {
        return UUID.randomUUID();
    }

    /**
     * Expose handle to sessions' global cache, use this to get/put/startNew interaction caches.
     *
     * @param <S> The binding type this dispatcher is dispatching.
     */
    public class Dispatcher<S> implements SessionDispatcher<S> {
        private final Cache<UUID, Optional<S>> cache;
        private final ComponentDispatcher<S> instance;
        private final InteractionSessionEvents handler;

        private <V extends ComponentDispatcher<S> & InteractionSessionEvents> Dispatcher(V instance) {
            this.instance = instance;
            this.handler = instance;
            /*
             * NOTE: we use expireAfterAccess because a session doesn't represent 1 interaction hook
             *       ex. a session here can execute a modal, which starts new discord interaction hook
             *       (creating new snowflake token, extending this session expiry)
             */
            this.cache = DiscordSessionModule.this.getPlugin()
                .caffeineBuilder()
                .expireAfterAccess(Duration.ofMinutes(15))
                .removalListener(new CacheRemoval())
                .build();
        }

        @NotNull
        public ComponentDispatcher<S> getInstance() {
            return this.instance;
        }

        @NotNull
        public InteractionSessionEvents getEventHandler() {
            return this.handler;
        }

        @NotNull
        public UUID startNew(S session) {
            UUID sessionUUID = DiscordSessionModule.this.generateSessionKey();
            MasterServer.info("Starting session " + sessionUUID);
            this.put(sessionUUID, session);
            return sessionUUID;
        }

        @NotNull
        public final Optional<Optional<S>> getPacked(@NotNull UUID sessionUUID) {
            MasterServer.info("Getting session " + sessionUUID);
            return Optional.of(sessionUUID).map(this.cache::getIfPresent);
        }

        public final void put(@NotNull UUID sessionUUID, S session) {
            MasterServer.info("Putting session " + sessionUUID);
            this.cache.put(sessionUUID, Optional.ofNullable(session));
        }

        public final void invalidate(@NotNull UUID sessionUUID) {
            MasterServer.info("Ending session " + sessionUUID);
            this.cache.invalidate(sessionUUID);
        }

        @NotNull
        private Cache<UUID, Optional<S>> getCache() {
            return this.cache;
        }

        private class CacheRemoval implements RemovalListener<UUID, Optional<S>> {
            @Override
            public void onRemoval(@Nullable UUID sessionUUID,
                                  @Nullable Optional<S> optional,
                                  @NonNull RemovalCause removalCause) {
                S session = Optional.ofNullable(optional)
                        .flatMap(Function.identity())
                        .orElse(null);
                getInstance().onRemoval(sessionUUID, session, removalCause);
            }
        }
    }
}
