package asia.buildtheearth.asean.discord.api;

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Implementation for {@link ComponentForBTE}
 *
 * <p>Purely a functional guard for {@link ComponentIdentifier} "parent" instance</p>
 */
final class ComponentIdentifierImpl implements ComponentForBTE {
    private final @Nullable WithSession session;
    private final @NotNull ComponentIdentifier parent;
    private final @NotNull String extensionName;
    private final @NotNull String identifier;

    ComponentIdentifierImpl(@NotNull WithSession session,
                            @Subst("MAX31-dash-annotated-identifier")
                            @NotNull String identifier) {
        this(session, identifier, ComponentIdentifier.of(
            BTE_PREFIX + PART_SEPARATOR + session.sessionUUID(),
            session.dispatchingID() + PART_SEPARATOR + identifier
        ));
    }

    ComponentIdentifierImpl(@Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String identifier) {
        this(null, identifier, ComponentIdentifier.of(DEFAULT_EXTENSION_NAME, identifier));
    }

    ComponentIdentifierImpl(@Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String extensionName,
                            @Subst("ANY-dash-or_underscore_identifier")
                            @NotNull String identifier) {
        this(null, identifier, ComponentIdentifier.of(
            BTE_PREFIX + PART_SEPARATOR + extensionName,
            identifier
        ));
    }

    private ComponentIdentifierImpl(@Nullable WithSession session,
                                    @NotNull String identifier,
                                    @NotNull ComponentIdentifier parent) {
        this.session = session;
        this.parent = parent;
        this.identifier = identifier;
        this.extensionName = IdentifierPattern.getExtensionNameOrUUID(parent);
    }

    @NotNull @Contract(pure = true)
    public java.util.Optional<WithSession> getSession() {
        return java.util.Optional.ofNullable(this.session);
    }

    @NotNull
    public String getExtensionName() {
        return this.extensionName;
    }

    @NotNull
    public String getIdentifier() {
        return this.identifier;
    }

    @NotNull
    public String getDiscordIdentifier() {
        return this.parent.getDiscordIdentifier();
    }

    @NotNull
    public ComponentIdentifier getParent() {
        return this.parent;
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return switch (obj) {
            case ComponentForBTE fromBTE -> parent.equals(fromBTE.getParent());
            case ComponentIdentifier fromDiscordSRV -> parent.equals(fromDiscordSRV);
            case null, default -> false;
        };
    }

    protected static final class IdentifierPattern {
        static final String SEPARATOR_PATTERN = Pattern.quote(String.valueOf(PART_SEPARATOR));

        /**
         * Inherited from DiscordSRV
         */
        public static final String COMPONENT_REGEX = "[\\w-_]{1,40}";
        static final Pattern PATTERN = Pattern.compile(COMPONENT_REGEX);

        /**
         * Restricted to 8 characters, default to a hashcode as HEX string.
         *
         * <p>Will be embedded in to max 40 characters identifier from DiscordSRV for *9 characters,<br/>
         * *9 = 8(32bits HEX) + 1({@value PART_SEPARATOR}).</p>
         */
        public static final String DISPATCHER_REGEX = "[\\w-]{1,8}";
        static final Pattern DISPATCHER_PATTERN = Pattern.compile(DISPATCHER_REGEX);


        /**
         * Restricted to 31 characters.
         *
         * <p>Inherited max 40 characters from DiscordSRV,
         * we preserve *9 characters to prefix BTE owned components.</p>
         *
         * <p>* 9 = 8(32bits HEX) + 1({@value PART_SEPARATOR})
         */
        public static final String WITH_SESSION_REGEX = "[\\w-]{1,31}";
        static final Pattern SESSION_PATTERN = Pattern.compile(WITH_SESSION_REGEX);

        /**
         * Restricted to 36 characters.
         *
         * <p>Inherited max 40 characters from DiscordSRV,
         * we preserve 4 characters to prefix BTE owned components.</p>
         */
        public static final String EXTENSION_REGEX = "[\\w-_]{1,36}";
        static final Pattern EXTENSION_PATTERN = Pattern.compile(EXTENSION_REGEX);

        //region Assertions
        private static void assertPattern(@NotNull Pattern pattern,
                                          @NotNull String assertion,
                                          @NotNull String displayName,
                                          @NotNull String regex) throws IllegalArgumentException {
            if (pattern.matcher(assertion).matches()) return;

            throw new IllegalArgumentException(
                displayName + '(' + assertion + ')'
                            + " does not match the required pattern: "
                            + regex
            );
        }

        static void assertIdentifierMatches(String identifier) {
            assertPattern(PATTERN, identifier, "Identifier", COMPONENT_REGEX);
        }

        static void assertSessionIdentifierMatches(String dispatcherID, String identifier) {
            assertPattern(DISPATCHER_PATTERN, dispatcherID, "Dispatcher ID", DISPATCHER_REGEX);
            assertPattern(SESSION_PATTERN, identifier, "Identifier", WITH_SESSION_REGEX);
        }

        static void assertExtensionMatches(String extensionName) {
            assertPattern(EXTENSION_PATTERN, extensionName, "Extension Name", EXTENSION_REGEX);
        }

        static @NotNull String getExtensionNameOrUUID(@NotNull ComponentIdentifier identifier) {
            return identifier.getExtensionName().substring(BTE_PREFIX.length() + 1);
        }
        //endregion

        /**
         * Identical to {@link ComponentIdentifier#parseFromDiscord},
         * but aims to parse our custom BTE identifier structure.
         *
         * @param identifier The identifier retrieved by DiscordSRV.
         * @return Nullable if the identifier are valid BTE component.
         */
        @Nullable
        static ComponentIdentifierImpl parseFromDiscordSRV(@NotNull ComponentIdentifier identifier) {
            if (!identifier.getExtensionName().startsWith(BTE_PREFIX)) {
                return null;
            }

            if(DEFAULT_EXTENSION_NAME.equals(identifier.getExtensionName())) {
                return new ComponentIdentifierImpl(null, identifier.getIdentifier(), identifier);
            }

            // Try parse embedded session identifier: BTE_UUID:Dispatcher_ID
            try {
                String discordSessionID = getExtensionNameOrUUID(identifier);
                java.util.UUID sessionID = java.util.UUID.fromString(discordSessionID);

                @Subst("b70c05cd_example-component-id")
                String[] parts = identifier.getIdentifier().split(SEPARATOR_PATTERN, 2);

                if(parts.length != 2)
                    throw new IllegalArgumentException("Unknown identifier parts for session");

                ComponentForBTE.WithSession session = ComponentForBTE.session(parts[0], sessionID);

                return new ComponentIdentifierImpl(session, parts[1], identifier);
            }
            catch (IllegalArgumentException | IndexOutOfBoundsException ignored) {
                return new ComponentIdentifierImpl(null, identifier.getIdentifier(), identifier);
            }
        }
    }
}
