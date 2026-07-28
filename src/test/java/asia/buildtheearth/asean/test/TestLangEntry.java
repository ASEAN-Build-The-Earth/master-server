package asia.buildtheearth.asean.test;

import asia.buildtheearth.asean.core.io.LangEntry;
import asia.buildtheearth.asean.core.io.LanguageFile;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.platform.commons.support.ReflectionSupport;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;

/**
 * Recursively collect ALL classes inside package {@value BASE_PACKAGE_ROOT},
 * filtered for {@linkplain #LangEntryFilter(Class) LangEntry}.
 * And test them against every entries inside {@linkplain LanguageFiles language files} (ex. lang/en.yml)
 *
 * <p>Note: we use {@link ReflectionSupport} from junit utility to do the class collection reflects.</p>
 *
 * @see #test(TemporaryTestFile, Path)
 */
@DisplayName("Language Files")
public class TestLangEntry {
    /**
     * The root package to recursively find all classes
     * <p>Note: edit the scope for debugging if necessary</p>
     */
    public static final String BASE_PACKAGE_ROOT = "asia.buildtheearth.asean";

    @Unmodifiable
    protected static List<Class<?>> implementations;

    @BeforeAll
    public static void findAllClassesInPackage() {
        List<Class<?>> filtered = ReflectionSupport.findAllClassesInPackage(
            BASE_PACKAGE_ROOT,
            TestLangEntry::LangEntryFilter,
            name -> true
        );

        if(filtered.isEmpty()) Assertions.fail(
            "Nothing to analyze inside package " + BASE_PACKAGE_ROOT
            + ", no class is assignable from " + LangEntry.class.getSimpleName()
        );

        TestLangEntry.implementations = Collections.unmodifiableList(filtered);
    }

    /**
     * Filter for any implementation class of {@link LangEntry},
     * which is our ultimate interface for all language keys.
     *
     * @param instance Class reflection
     * @return True if this class annotate non-interface implementation of {@link LangEntry}
     */
    public static boolean LangEntryFilter(Class<?> instance) {
        return LangEntry.class.isAssignableFrom(instance)
                && instance != LangEntry.class
                && !instance.isInterface();
    }

    public static final class LanguageFiles extends ResourceProvider {
        public LanguageFiles() {
            //... add more language file here
            super(Arguments.argumentSet("Default Language (EN)", "lang/en.yml"));
        }
    }

    @ParameterizedTest
    @ArgumentsSource(LanguageFiles.class)
    @DisplayName("Test Integrity")
    public void test(@NotNull TemporaryTestFile temp, @TempDir Path directory) {
        LanguageFile language = new LanguageFile(null);
        File file = temp.apply(directory);
        Assertions.assertDoesNotThrow(() -> language.load(file));

        // Retrieve the flat set of ALL leaf keys
        Set<String> keys = new HashSet<>();

        language.getValues(true).forEach((key, value) -> {
            // ConfigurationSection/MemorySection are section of n keys, which we don't want
            if( !(value instanceof ConfigurationSection) ) keys.add(key);
        });

        Map<String, Class<?>> missingKeys = new HashMap<>();
        Map<String, Enum<?>> missingEnums = new HashMap<>();
        StringBuilder problems = new StringBuilder();

        for(Class<?> clazz : TestLangEntry.implementations) {
            if (clazz.isEnum()) {
                Object[] constants = clazz.getEnumConstants();
                for(Object instance : constants) {
                    // Fork instances
                    Enum<?> ordinal = Assertions.assertInstanceOf(Enum.class, instance);
                    LangEntry entry = Assertions.assertInstanceOf(LangEntry.class, instance);
                    String key = entry.getKey();

                    if (!keys.contains(key)) missingEnums.put(key, ordinal);
                    else keys.remove(key);
                }
            }
            else if(LangEntry.class.isAssignableFrom(clazz)) {
                final LangEntry entry;
                try { // Try to find default no-params constructor to analyze
                    Constructor<?> constructor = clazz.getDeclaredConstructor();
                    Object instance = constructor.newInstance();
                    entry = Assertions.assertInstanceOf(LangEntry.class, instance);
                }
                catch (NoSuchMethodException ex) {
                    problems.append("Class ").append(clazz.getName())
                            .append(":\n\t").append("Is implementing ").append(LangEntry.class.getSimpleName())
                            .append(" but no default constructor is found (requires for analyzing its language key).");
                    continue;
                }
                catch (InvocationTargetException | InstantiationException | IllegalAccessException ex) {
                    problems.append("Class ").append(clazz.getName())
                            .append(":\n\t").append("Is implementing ").append(LangEntry.class.getSimpleName())
                            .append(" but its default constructor yield exception (cannot analyze its language key).\n\t")
                            .append(ex.getMessage());
                    continue;
                }

                String key = entry.getKey();

                if(!keys.contains(key)) missingKeys.put(key, clazz);
                else keys.remove(key);
            }
            else // check #LangEntryFilter is its filtering correctly if this ever happen
                Assertions.fail(clazz.getName() + " Not compatible with " + LangEntry.class.getName());
        }

        if(!missingEnums.isEmpty() || !missingKeys.isEmpty()) {
            problems.append("Missing language key defined in classpath:\n\t");

            missingEnums.forEach((key, ordinal) -> problems
                .append(key).append(": ")
                .append(ordinal.getDeclaringClass().getName()).append('#')
                .append(ordinal.name()).append("\n\t")
            );

            missingKeys.forEach((key, instance) -> problems
                .append(key).append(": ")
                .append(instance.getName()).append("\n\t")
            );
        }

        if(!keys.isEmpty()) {
            problems.append('\n').append("Found language key defined but never used:\n\t");
            keys.forEach(key -> problems.append(key).append("\n\t"));
        }

        if(!problems.isEmpty())
            Assertions.fail(problems::toString);
    }
}