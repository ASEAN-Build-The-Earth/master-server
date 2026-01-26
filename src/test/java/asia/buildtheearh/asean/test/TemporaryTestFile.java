package asia.buildtheearh.asean.test;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Function;

public interface TemporaryTestFile extends Function<Path, File> {
    File apply(Path temp);
}
