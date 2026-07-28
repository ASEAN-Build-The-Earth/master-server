package asia.buildtheearth.asean.test;

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

public class TestCustomID {
    static final int TEST_REPEATING_TIME = 10;
    static final String ID_PREFIX = "BTE_";
    public interface CustomID extends Function<String, ComponentIdentifier> {
        @Override
        ComponentIdentifier apply(String s);

        default CustomID fork() { return this; }
    }

    public static class Generator implements ArgumentsProvider, CustomID {
        static Random random = new Random();

        @Override
        public ComponentIdentifier apply(String identifier) {
            @Subst(ID_PREFIX + "IDENTIFY-UUID-UUID-UUID-INTERACTIONS")
            String extensionID =  ID_PREFIX + UUID.randomUUID();

            @Subst("UUID_interaction-identifier")
            String customID = Integer.toHexString(random.nextInt()) + '_' + identifier;

            return ComponentIdentifier.of(extensionID, customID);
        }

        @Override @NotNull
        public Stream<? extends Arguments> provideArguments(@NotNull ParameterDeclarations parameters,
                                                            @NotNull ExtensionContext context) {
            Object[] generator = new Object[TEST_REPEATING_TIME];
            Arrays.fill(generator, this.fork());
            return Stream.of(generator).map(Arguments::of);
        }
    }


    @ParameterizedTest
    @ArgumentsSource(Generator.class)
    @DisplayName("Generate random customID")
    public void test(CustomID customID) {
        ComponentIdentifier id = customID.apply("test");

        System.out.println(id.getDiscordIdentifier());
        Assertions.assertTrue(id.getExtensionName().startsWith(ID_PREFIX));
        Assertions.assertTrue(id.getDiscordIdentifier().length() < 100);

    }
}
