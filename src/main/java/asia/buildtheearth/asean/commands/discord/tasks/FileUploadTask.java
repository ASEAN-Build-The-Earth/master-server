package asia.buildtheearth.asean.commands.discord.tasks;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.abstraction.AbstractPluginProvider;
import asia.buildtheearth.asean.core.api.PluginProvider;
import asia.buildtheearth.asean.discord.Text;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.utils.NamedAttachmentProxy;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class FileUploadTask extends AbstractPluginProvider  {
    public interface SessionFile extends Function<String, Path> {
        default File get(String fileName) {
            return apply(fileName).toFile();
        }

        Path apply(String fileName);
    }

    protected final Path sessionPath;
    protected final SessionFile sessionFile;
    protected final List<NamedAttachmentProxy> proxies;

    public FileUploadTask(@NotNull PluginProvider provider,
                          @NotNull List<NamedAttachmentProxy> proxies,
                          @NotNull Path path,
                          @NotNull Function<String, Path> sessionFile) {
        super(provider);
        this.sessionFile = sessionFile::apply;
        this.sessionPath = path;
        this.proxies = proxies;
    }

    public FileUploadTask(@NotNull PluginProvider provider,
                          @NotNull List<NamedAttachmentProxy> proxies,
                          @NotNull Path path) {
        this(provider, proxies, path, path::resolve);
    }

    /**
     * Submit this task to download, verify, and then export schematic file(s).
     *
     * @param resultMap Empty-modifiable map to output as proxy list by unique fileName keys.
     * @return Future to a list of exported file name(s).
     */
    public Task<? extends Collection<String>> submit(Map<String, String> resultMap) {
        return this.verify(resultMap).thenApply(Map::values);
    }

    /**
     * Submit download tasks to all file proxies.
     *
     * @param proxyMap Empty map to output as proxy list by unique fileName keys.
     * @return Future to all the downloading tasks.
     */
    public @NotNull CompletableFuture<CompletableFuture<?>[]> download(
            @NotNull Map<String, String> proxyMap) {

        int size = this.proxies.size();
        String[] folderFiles = sessionPath.toFile().list();
        Set<String> existing = folderFiles != null? Set.of(folderFiles) : null;
        CompletableFuture<?>[] task = new CompletableFuture[size];

        // Normalize filenames on to proxy map
        for (int i = 0; i < size; i++) {
            NamedAttachmentProxy file = this.proxies.get(i);
            String proxyURL = file.getUrl(),
                    fileName = file.getFileName(),
                    base = Text.stripExtension(fileName),
                    name = Text.toLowerHyphen(base),
                    ext = '.' + Text.getFileExtension(fileName),
                    finalName;

            // Ensure name is unique
            boolean alreadyExist = existing != null && existing.contains(name + ext);
            if(proxyMap.containsKey(name + ext) || alreadyExist) {
                int j = 1;
                String candidate;

                do { candidate = name + '-' + j; j++; }
                while(proxyMap.containsKey(candidate + ext));

                proxyMap.put(finalName = candidate + ext, proxyURL);
            }
            else proxyMap.put(finalName = name + ext, proxyURL);

            File location = this.sessionFile.get(finalName);
            MasterServer.info("Downloading proxy: " + proxyURL + " to: " + finalName);
            task[i] = file.downloadToFile(location);
        }

        return CompletableFuture.allOf(task).thenApply(finished -> task);
    }

    public void lookup(Map<String, String> lookupMap,
                       BiConsumer<@NotNull String, @Nullable String> consumer) {
        this.proxies.forEach(proxy -> {
            // Compare expected proxy file to downloaded source map,
            String proxyURL = proxy.getUrl();
            String originalName = proxy.getFileName();

            consumer.accept(originalName, lookupMap.get(proxyURL));
        });
    }

    public @NotNull Task<Map<String, String>> verify(
            @NotNull Map<String, String> resultMap) {
        Map<String, String> proxyMap = new HashMap<>();

        final CompletableFuture<CompletableFuture<?>[]> task = this.download(proxyMap);
        final Map<String, String> downloadMap = Map.copyOf(proxyMap);

        return Task.of(task).thenApply((download) -> {
            // Take note of downloaded file names (it is checked to be unique on step before)
            Set<String> resultSet = new HashSet<>();

            // Check downloaded integrity
            for (CompletableFuture<?> future : download) {
                try {
                    File file = (File) future.get();
                    resultSet.add(file.getName());
                } catch(Throwable throwable) {
                    MasterServer.error(this.getClass().getSimpleName()
                        + ": Error saving Discord FileUpload.", throwable);
                }
            }
            /*
             * Result will be checked against original proxy lists.
             * It might have duplicate file name so we use full proxy URL as the key.
             */
            downloadMap.forEach((filename, proxyURL) -> {
                // Collect only file that are successfully downloaded
                if(resultSet.contains(filename))
                    resultMap.put(proxyURL, filename);
            });

            return Collections.unmodifiableMap(resultMap);
        });
    }


}
