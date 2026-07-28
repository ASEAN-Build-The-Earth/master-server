package asia.buildtheearth.asean.test;

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
import java.nio.file.Files;
import java.nio.file.Path;
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
        this.arguments = arguments.map(this::resolveResource);
    }

    private <T extends Arguments> Arguments resolveResource(@NotNull T provided) {
        Object[] argument = provided.get();
        Object[] resource = new Object[argument.length];

        selection:
        for (int i = 0; i < argument.length; i++) {
            String filename = Assertions.assertInstanceOf(String.class, argument[i],
                "ResourceProvider requires argument of type String as the resource file."
            );
            ClasspathResourceSelector selector = DiscoverySelectors.selectClasspathResource(filename);

            for (Resource file : selector.getResources()) {
                resource[i] = new TemporaryTestFileImpl(file);
                break selection; // break because we look for 1 resource only
            }

            Assertions.fail("Test Resource not found for " + filename);
        }

        if (provided instanceof Arguments.ArgumentSet sets)
            return Arguments.argumentSet(sets.getName(), resource);

        return Arguments.of(resource);
    }

    @Override
    @NotNull
    public Stream<? extends Arguments> provideArguments(@NotNull ParameterDeclarations parameters,
                                                        @NotNull ExtensionContext context) {
        return this.arguments;
    }

    public static class TemporaryTestFileImpl implements TemporaryTestFile {
        protected final Resource resource;

        public TemporaryTestFileImpl(@NotNull Resource resource) {
            this.resource = resource;
        }

        @Override @NotNull
        public File apply(@NotNull Path directory) {
            Path temp = directory.resolve(this.resource.getName());
            File file = this.prepare(temp);

            // Write test resource on to temporary test directory
            // Would go in for example: /tmp/junit-9871151558931527943/test.txt
            this.create(file);

            return file;
        }

        public @NotNull File prepare(@NotNull Path tempDir) {
            try {
                Path parent = tempDir.getParent();
                Files.createDirectories(parent);
            } catch (IOException ex) {
                Assertions.fail("Failed to prepare temp file directory", ex);
            }
            return tempDir.toFile();
        }

        public void create(@NotNull File tempFile) {
            try(
                FileOutputStream temp = new FileOutputStream(tempFile);
                InputStream content = this.resource.getInputStream()
            ) {
                temp.write(content.readAllBytes());
            } catch (IOException ex) {
                Assertions.fail("Failed to write test resource to temp directory", ex);
            }
        }
    }
}
