package asia.buildtheearth.asean.commands.abstraction;

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Custom Implementation for {@link com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier ComponentIdentifier}
 */
public sealed interface ComponentForBTE permits ComponentIdentifierImpl {
    /**
     * Inherited from DiscordSRV
     */
    String COMPONENT_REGEX = "[\\w-_]{1,40}";

    /**
     * Restricted to 40 characters by DiscordSRV,
     * we preserve 4 characters to prefix BTE owned components.
     */
    String EXTENSION_REGEX = "[\\w-_]{1,36}";

    /**
     * Prefix for all BTE owned extensions,
     * <strong>MAX 4 CHARACTERS</strong>.
     * <p>Note: We preserve 36 characters to generate session UUID.</p>
     */
    @Pattern("[\\w-_]{1,4}")
    String BTE_PREFIX = "BTE_";

    /**
     * Default Extension Name.
     */
    @Pattern(COMPONENT_REGEX)
    String EXTENSION_NAME = BTE_PREFIX + "MasterServerASEAN";

    /**
     * Creates a new {@link ComponentIdentifierImpl}.
     *
     * @param sessionUUID UUID of a session which will own this component identifier, use internally for BTE.
     * @param identifier the identifier of this component (1-40 characters, a-z, A-Z, 0-9, -)
     * @return a new {@link ComponentIdentifierImpl}
     * @throws IllegalArgumentException if the identifier does not match the required constraints
     */
    @NotNull
    static ComponentForBTE of(@NotNull java.util.UUID sessionUUID,
                                      @NotNull @Pattern(COMPONENT_REGEX) String identifier
    ) {
        ComponentIdentifierImpl.IdentifierPattern.assertIdentifierMatches(identifier);

        return new ComponentIdentifierImpl(sessionUUID, identifier);
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
        ComponentIdentifierImpl.IdentifierPattern.assertIdentifierMatches(identifier);

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
        ComponentIdentifierImpl.IdentifierPattern.assertExtensionMatches(extensionName);
        ComponentIdentifierImpl.IdentifierPattern.assertIdentifierMatches(identifier);

        return new ComponentIdentifierImpl(extensionName, identifier).getParent();
    }

    @NotNull
    static java.util.Optional<ComponentForBTE> parseFromDiscord(@NotNull ComponentIdentifier identifier) {
        ComponentForBTE parent = ComponentIdentifierImpl.IdentifierPattern.parseFromDiscordSRV(identifier);

        return java.util.Optional.ofNullable(parent);
    }

    @NotNull
    static java.util.Optional<ComponentForBTE> parseFromDiscord(@NotNull String discordIdentifier) {
        ComponentIdentifier parent = ComponentIdentifier.parseFromDiscord(discordIdentifier);

        return java.util.Optional.ofNullable(parent).map(ComponentIdentifierImpl.IdentifierPattern::parseFromDiscordSRV);
    }

    java.util.Optional<java.util.UUID> getSessionUUID();

    String getExtensionName();

    String getIdentifier();

    String getDiscordIdentifier();

    ComponentIdentifier getParent();
}
