package asia.buildtheearth.asean.test;

import asia.buildtheearth.asean.discord.abstraction.AbstractDiscordInteraction;
import asia.buildtheearth.asean.discord.Text;
import asia.buildtheearth.asean.utils.function.CheckedSupplier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionEvent;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.Interaction;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class FormattingTest {
    static @NotNull Stream<Arguments> arguments() {
        return Stream.of(
            parameter("Test Bold",
                // Test that the expected formatting
                "**%s**", // Result: **test discord bold**
                // Can be created by the following arguments
                Actual.discord("test discord bold", Text.Formatting.BOLD)
            ),
            parameter("Test Bold WITH Italics",
                "***%s***",
                Actual.discord("test discord bold WITH italics", Text.Formatting.BOLD, Text.Formatting.ITALICS)
            ),
            parameter("Test Bold AND Italics",
                "**%s**%s*%s*",
                Actual.discord("test discord bold", Text.Formatting.BOLD),
                Actual.discord(" AND "),
                Actual.discord("test discord italics", Text.Formatting.ITALICS)
            ),
            parameter("Test Bold w/ Strikethrough AND Italics w/ Underlined",
                "**~~%s~~**%s*__%s__*",
                Actual.discord("test discord bold & strikethrough", Text.Formatting.BOLD, Text.Formatting.STRIKETHROUGH),
                Actual.discord(" AND "),
                Actual.discord("test discord italics & underlined", Text.Formatting.ITALICS, Text.Formatting.UNDERLINED)
            )
        );
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void test(@NotNull Test test) {
        TestFormatting sender = new TestFormatting();

        sender.send(test.actual());

        Assertions.assertEquals(test.expected(), sender.getResult());
    }

    //region Test Logic
    @NotNull
    static Arguments.ArgumentSet parameter(@NotNull String name,
                                           @NotNull String expected,
                                           @NotNull FormattingTest.Actual... actual) {
        String testPreview = expected.replace("%s", "{}");
        String displayName = String.format("%s – \"%s\"", name, testPreview);

        return Arguments.argumentSet(displayName, Test.of(expected, actual));
    }

    static class MockEvent<T extends GenericInteractionCreateEvent> extends AbstractInteractionEvent<T> {
        public MockEvent() {
            super(null, null, null, null, null);
        }

        public T asJDA() { return null; }
    }


    static class TestFormatting extends AbstractDiscordInteraction<Interaction> {
        private final AtomicReference<String> result = new AtomicReference<>();

        public TestFormatting() {
            super(null, new MockEvent<>());
        }

        @Override
        public void execute(Runnable execution) {
        }

        @Override
        public Task<Interaction> supply(CheckedSupplier<Interaction> supplier) {
            return null;
        }

        @Override
        protected void sendResponse(@NotNull SendableDiscordMessage message) {
            result.compareAndSet(null, message.getContent());
        }

        public String getResult() {
            return result.get();
        }

    }

    record Actual(String text,
                  BiFunction<Text, Text.Formatting[], Text> format,
                  Text.Formatting... formatting) {
        @Contract("_, _, _ -> new")
        static @NotNull Actual of(String text,
                                  BiFunction<Text, Text.Formatting[], Text> format,
                                  Text.Formatting... formatting) {
            return new Actual(text, format, formatting);
        }

        @Contract("_, _ -> new")
        static @NotNull Actual discord(String text,
                                  Text.Formatting... formatting) {
            return of(text, Text::withDiscordFormatting, formatting);
        }

        @Contract("_, _ -> new")
        static @NotNull Actual game(String text,
                                    Text.Formatting... formatting) {
            return of(text, Text::withGameFormatting, formatting);
        }

        @Unmodifiable
        @Contract(" -> new")
        @NotNull Collection<Text> asSingleton() {
            return Collections.singletonList(asText());
        }

        @Contract(" -> new")
        @NotNull Text asText() {
            Text instance = new Text(text);
            return (formatting.length > 0)? format.apply(instance, formatting) : instance;
        }
    }

    record Test(String expected, Collection<Text> actual) {
        @Contract("_, _ -> new")
        static @NotNull Test of(@NotNull String expected,
                                @NotNull Actual actual) {
            String test = String.format(expected, actual.text());
            return new Test(test, actual.asSingleton());
        }

        @Contract("_, _ -> new")
        static @NotNull Test of(@NotNull String expected,
                                @NotNull Actual... actual) {
            Object[] formats = Stream.of(actual).map(Actual::text).toArray();
            Collection<Text> collection = Stream.of(actual).map(Actual::asText).toList();
            String test = String.format(expected, formats);

            return new Test(test, collection);
        }
    }
    //endregion
}
