package asia.buildtheearth.asean.core.api;

import asia.buildtheearth.asean.MasterServer;
import org.jetbrains.annotations.NotNull;

public interface PluginProvider {
    @NotNull MasterServer getPlugin();
}
