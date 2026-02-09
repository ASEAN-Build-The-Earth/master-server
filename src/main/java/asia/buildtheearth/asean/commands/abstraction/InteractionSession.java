package asia.buildtheearth.asean.commands.abstraction;

import asia.buildtheearth.asean.core.AbstractModule;
import asia.buildtheearth.asean.core.providers.PluginForDiscordSRV;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionWithHookEvent;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.events.discord.interaction.component.DiscordSelectMenuInteractionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.UUID;

public abstract class InteractionSession<T extends Record> extends AbstractModule {

    private final com.github.benmanes.caffeine.cache.Cache<UUID, T> session;

    public InteractionSession(PluginForDiscordSRV plugin) {
        super(plugin);
        this.session = this.getPlugin()
            .caffeineBuilder()
            .expireAfterWrite(Duration.ofMinutes(15))
            .build();
    }

    @NotNull
    public UUID startNew(T interaction) {
        UUID sessionUUID = UUID.randomUUID();
        this.put(sessionUUID, interaction);
        return sessionUUID;
    }

    @Nullable
    public final T get(UUID sessionUUID) {
        return this.session.getIfPresent(sessionUUID);
    }

    public final void put(UUID sessionUUID, T interaction) {
        this.session.put(sessionUUID, interaction);
    }

    protected T fromModalInteractionEvent(@NotNull DiscordModalInteractionEvent event,
                                       @Nullable T session) {
        throw new UnsupportedOperationException(
            this.getClass().getName()
            + "#fromModalInteractionEvent is unimplemented for this interaction. "
            + '(' + event.getClass().getSimpleName() + ')'
        );
    }

    protected T fromSelectMenuInteractionEvent(@NotNull DiscordSelectMenuInteractionEvent event,
                                            @Nullable T session) {
        throw new UnsupportedOperationException(
            this.getClass().getName()
            + "#fromSelectMenuInteractionEvent is unimplemented for this interaction. "
            + '(' + event.getClass().getSimpleName() + ')'
        );
    }

    protected T fromButtonInteractionEvent(@NotNull DiscordButtonInteractionEvent event,
                                        @Nullable T session) {
        throw new UnsupportedOperationException(
            this.getClass().getName()
            + "#fromButtonInteractionEvent is unimplemented for this interaction. "
            + '(' + event.getClass().getSimpleName() + ')'
        );
    }

    @Subscribe
    @SuppressWarnings("unused")
    public final void onModalInteractionEvent(@NotNull DiscordModalInteractionEvent event) {
        handle(event, this::fromModalInteractionEvent);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public final void onSelectMenuInteractionEvent(@NotNull DiscordSelectMenuInteractionEvent event) {
        handle(event, this::fromSelectMenuInteractionEvent);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public final void onButtonInteractionEvent(@NotNull DiscordButtonInteractionEvent event) {

        handle(event, this::fromButtonInteractionEvent);
    }

    private <E extends AbstractInteractionWithHookEvent<?>> void handle(@NotNull E event,
                                                                        @NotNull BiFunction<E, T, T> handler) {
        Consumer<UUID> handleSessionUUID = sessionID -> {
            T session = this.get(sessionID);
            T interaction = handler.apply(event, session);
            this.put(sessionID, interaction);
        };

        ComponentIdentifier identifier = event.getIdentifier();

        ComponentForBTE.parseFromDiscord(identifier)
            .flatMap(ComponentForBTE::getSessionUUID)
            .ifPresent(handleSessionUUID);
    }
}
