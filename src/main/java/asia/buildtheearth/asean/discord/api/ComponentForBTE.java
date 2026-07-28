package asia.buildtheearth.asean.discord.api;

import org.intellij.lang.annotations.Pattern;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import static asia.buildtheearth.asean.discord.api.ComponentIdentifierImpl.IdentifierPattern.*;

/**
 * Custom Implementation for {@link com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier ComponentIdentifier}
 *
 * <pre>DiscordSRV/BTE_MasterServerASEAN:identifier</pre>
 * <pre>DiscordSRV/BTE_{@linkplain java.util.UUID {Session UUID}}:{@linkplain ComponentDispatcher#getDispatcherID() {dispatcher ID}}_identifier</pre>
 * <pre>DiscordSRV/BTE_0c21ffb7-da01-3e1e-b9d9-ea6cea50e196:b70c05cd_MAX31-dash-annotated-identifier</pre>
 */
public sealed interface ComponentForBTE
    permits ComponentIdentifierImpl {

    /**
     * Inherited from DiscordSRV, which only allow underscore or dash.
     *
     * <p>We use underscore to separate parts and use dash for the identifier naming.</p>
     */
    char PART_SEPARATOR = '_';

    /**
     * Prefix for all BTE owned extensions,
     * <strong>MAX 3 CHARACTERS</strong>.
     * <p>Note: We preserve 36 characters to generate session UUID.</p>
     */
    @Pattern("[\\w]{3}")
    String BTE_PREFIX = "BTE"; // possible prefix choice: CMD, TXT, BTN, EXT, UID

    /**
     * Default Extension Name.
     */
    @Pattern(COMPONENT_REGEX)
    String DEFAULT_EXTENSION_NAME = BTE_PREFIX + PART_SEPARATOR + "MasterServerASEAN";

    /**
     * Stage a new session based component.
     *
     * @param dispatchingID the dispatcher identifier that owns this session (1-8 characters, a-z, A-Z, 0-9, -)
     * @param sessionUUID UUID of a session which will own this component identifier, use internally for BTE.
     * @return a new {@link WithSession}
     */
    @Contract("_, _ -> new") @NotNull
    static WithSession session(@Subst("id-max-8")
                               @Pattern(DISPATCHER_REGEX)
                               @NotNull String dispatchingID,
                               @NotNull java.util.UUID sessionUUID) {
        return new WithSession(dispatchingID, sessionUUID);
    }

    /**
     * Creates a new {@link ComponentIdentifier}.
     *
     * @param identifier the identifier of this component (1-40 characters, a-z, A-Z, 0-9, -, _)
     * @return a new {@link ComponentIdentifier}
     * @throws IllegalArgumentException if the extension name or identifier does not match the required constraints
     */
    @NotNull
    static ComponentIdentifier of(@NotNull @Pattern(COMPONENT_REGEX) String identifier) {
        assertIdentifierMatches(identifier);

        return new ComponentIdentifierImpl(identifier).getParent();
    }

    /**
     * Creates a new {@link ComponentIdentifier}.
     *
     * @param extensionName the name of the plugin or mod that owns this identifier (1-36 characters, a-z, A-Z, 0-9, -, _)
     * @param identifier the identifier of this component (1-40 characters, a-z, A-Z, 0-9, -, _)
     * @return a new {@link ComponentIdentifier}
     * @throws IllegalArgumentException if the extension name or identifier does not match the required constraints
     */
    @NotNull
    static ComponentIdentifier of(@NotNull @Pattern(EXTENSION_REGEX) String extensionName,
                                  @NotNull @Pattern(COMPONENT_REGEX) String identifier) {
        assertExtensionMatches(extensionName);
        assertIdentifierMatches(identifier);

        return new ComponentIdentifierImpl(extensionName, identifier).getParent();
    }

    @NotNull
    static java.util.Optional<ComponentForBTE> parseFromDiscord(ComponentIdentifier identifier) {
        return java.util.Optional.ofNullable(identifier)
            .map(ComponentIdentifierImpl.IdentifierPattern::parseFromDiscordSRV);
    }

    @NotNull
    static java.util.Optional<ComponentForBTE> parseFromDiscord(String discordIdentifier) {
        return java.util.Optional.ofNullable(discordIdentifier)
            .map(ComponentIdentifier::parseFromDiscord)
            .map(ComponentIdentifierImpl.IdentifierPattern::parseFromDiscordSRV);
    }

    /**
     * Create a component with session information.
     *
     * @param dispatchingID Dispatching ID in which will manage this session.
     * @param sessionUUID UUID of a session which will own this component identifier, use internally for BTE.
     */
    record WithSession(
        @Pattern(DISPATCHER_REGEX) @Subst("id-max-8")
        @NotNull String dispatchingID,
        @Subst("IDENTIFY-UUID-UUID-UUID-INTERACTIONS")
        @NotNull java.util.UUID sessionUUID
    ) {
        /**
         * Creates a new {@link ComponentIdentifierImpl}.
         *
         * @param identifier the identifier of this component (1-40 characters, a-z, A-Z, 0-9, -)
         * @return a new {@link ComponentForBTE}
         * @throws IllegalArgumentException if the identifier does not match the required constraints
         */
        @NotNull
        public ComponentForBTE create(@NotNull @Pattern(WITH_SESSION_REGEX) String identifier) {
            assertSessionIdentifierMatches(this.dispatchingID, identifier);

            return new ComponentIdentifierImpl(this, identifier);
        }
    }

    java.util.Optional<WithSession> getSession();

    String getExtensionName();

    String getIdentifier();

    String getDiscordIdentifier();

    ComponentIdentifier getParent();
}
