package asia.buildtheearh.asean.test;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;
import org.junit.platform.commons.io.Resource;
import org.junit.platform.engine.discovery.ClasspathResourceSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

public abstract class ResourceProvider implements ArgumentsProvider {

    private final Stream<? extends Arguments> arguments;

    public ResourceProvider(String... sources) {
        this(Stream.of(Arguments.of((Object) sources)));
    }

    public ResourceProvider(Arguments... sources) {
        this(Stream.of(sources));
    }

    @Contract(pure = true)
    private ResourceProvider(@NotNull Stream<? extends Arguments> arguments) {
        this.arguments = arguments.map(provided -> {
            Object[] argument = provided.get();
            Object[] resource = new Object[argument.length];

            selection:
            for (int i = 0; i < argument.length; i++) {
                String filename = Assertions.assertInstanceOf(String.class, argument[i],
                        "ResourceProvider requires argument of type String as the resource file."
                );
                ClasspathResourceSelector selector = DiscoverySelectors.selectClasspathResource(filename);

                for (Resource file : selector.getResources()) {
                    resource[i] = (TemporaryTestFile) directory -> {
                        File tempFile = directory.resolve(file.getName()).toFile();
                        try (FileOutputStream temp = new FileOutputStream(tempFile)) {
                            try (InputStream content = file.getInputStream()) {
                                temp.write(content.readAllBytes());
                            }
                        } catch (IOException ex) {
                            Assertions.fail("Failed to write test resource to temp directory", ex);
                        }

                        return tempFile;
                    };
                    break selection;
                }

                Assertions.fail("Test Resource not found for " + filename);
            }

            if (provided instanceof Arguments.ArgumentSet sets)
                return Arguments.argumentSet(sets.getName(), resource);

            return Arguments.of(resource);
        });
    }

    @Override
    @NotNull
    public Stream<? extends Arguments> provideArguments(@NotNull ParameterDeclarations parameters,
                                                        @NotNull ExtensionContext context) {
        return this.arguments;
    }
}
