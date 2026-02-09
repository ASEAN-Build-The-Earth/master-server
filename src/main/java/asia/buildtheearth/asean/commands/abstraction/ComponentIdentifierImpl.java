package asia.buildtheearth.asean.commands.abstraction;

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Implementation for {@link ComponentForBTE}
 */
final class ComponentIdentifierImpl implements ComponentForBTE {
    private final @Nullable java.util.UUID sessionUUID;
    private final @NotNull ComponentIdentifier parent;
    private final @NotNull String extensionName;

    ComponentIdentifierImpl(@Subst("IDENTIFY-UUID-UUID-UUID-INTERACTIONS")
                            @NotNull java.util.UUID sessionUUID,
                            @Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String identifier) {
        this(sessionUUID, ComponentIdentifier.of(BTE_PREFIX + sessionUUID, identifier));
    }

    ComponentIdentifierImpl(@Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String identifier) {
        this(null, ComponentIdentifier.of(EXTENSION_NAME, identifier));
    }

    ComponentIdentifierImpl(@Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String extensionName,
                            @Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String identifier) {
        this(null, ComponentIdentifier.of(BTE_PREFIX + extensionName, identifier));
    }

    private ComponentIdentifierImpl(@Nullable java.util.UUID sessionUUID,
                                    @NotNull ComponentIdentifier parent) {
        this.sessionUUID = sessionUUID;
        this.parent = parent;
        this.extensionName = parent.getExtensionName().substring(BTE_PREFIX.length());
    }


    @NotNull @Contract(pure = true)
    public java.util.Optional<java.util.UUID> getSessionUUID() {
        return java.util.Optional.ofNullable(this.sessionUUID);
    }

    @NotNull
    public String getExtensionName() {
        return this.extensionName;
    }

    @NotNull
    public String getIdentifier() {
        return this.parent.getIdentifier();
    }

    @NotNull
    public String getDiscordIdentifier() {
        return this.parent.getDiscordIdentifier();
    }

    @NotNull
    public ComponentIdentifier getParent() {
        return this.parent;
    }

    protected static final class IdentifierPattern {
        static final Pattern PATTERN = Pattern.compile(COMPONENT_REGEX);
        static final Pattern EXTENSION_PATTERN = Pattern.compile(EXTENSION_REGEX);

        static void assertIdentifierMatches(String identifier) {
            if (!PATTERN.matcher(identifier).matches()) throw new IllegalArgumentException(
                "Identifier does not match the required pattern " + COMPONENT_REGEX
            );
        }

        static void assertExtensionMatches(String extensionName) {
            if (!EXTENSION_PATTERN.matcher(extensionName).matches()) throw new IllegalArgumentException(
                "Extension name does not match the required pattern " + EXTENSION_REGEX
            );
        }

        /**
         * Identical to {@link ComponentIdentifier#parseFromDiscord}
         *
         * @param identifier The identifier retrieved by DiscordSRV.
         * @return Optional with value if the string are valid BTE component.
         */
        @Nullable
        static ComponentIdentifierImpl parseFromDiscordSRV(@NotNull ComponentIdentifier identifier) {
            if (!identifier.getExtensionName().startsWith(BTE_PREFIX)) {
                return null;
            }

            if(EXTENSION_NAME.equals(identifier.getExtensionName())) {
                return new ComponentIdentifierImpl(null, identifier);
            }

            try {
                String discordSessionID = identifier.getExtensionName().substring(BTE_PREFIX.length());
                java.util.UUID sessionID = java.util.UUID.fromString(discordSessionID);
                return new ComponentIdentifierImpl(sessionID, identifier);
            }
            catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                return new ComponentIdentifierImpl(null, identifier);
            }
        }
    }
}
