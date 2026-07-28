package asia.buildtheearth.asean.core.api;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static asia.buildtheearth.asean.ConfigPaths.SLASH_COMMAND;
import static asia.buildtheearth.asean.ConfigPaths.SLASH_SCHEMATIC;
import static asia.buildtheearth.asean.ConfigPaths.SLASH_GEO_TOOLS;
import static asia.buildtheearth.asean.core.api.MainGuildPermission.NONE;
import static asia.buildtheearth.asean.core.api.MainGuildPermission.ADMINISTRATOR;
import static asia.buildtheearth.asean.core.api.MainGuildPermission.OFFICIAL_BUILDER;

public record MainGuildCommand(
    @NotNull MainGuildPermission permission,
    @NotNull Set<MainGuildRole> role
) {
    public enum Config implements PermissionConfig {
        RELOAD(SLASH_COMMAND, "slash-reload", ADMINISTRATOR),

        SCHEMATIC_GET(SLASH_SCHEMATIC, "get", OFFICIAL_BUILDER),
        SCHEMATIC_UPLOAD(SLASH_SCHEMATIC, "upload", OFFICIAL_BUILDER),

        GEO_TOOLS_DRAW(SLASH_GEO_TOOLS, "draw", OFFICIAL_BUILDER),
        GEO_TOOLS_EXPORT_KML(SLASH_GEO_TOOLS, "export-kml", NONE),
        GEO_TOOLS_EXPORT_GEOJSON(SLASH_GEO_TOOLS, "export-geojson", NONE),
        GEO_TOOLS_EXPORT_SCHEMATIC(SLASH_GEO_TOOLS, "export-schematic", NONE);

        private final String path;
        private final String configName;
        private final MainGuildPermission defaultPermission;

        Config(String path, String configName, MainGuildPermission defaultPermission) {
            this.path = path;
            this.configName = configName;
            this.defaultPermission = defaultPermission;
        }

        @Override
        public String getName() {
            return this.configName;
        }

        @Override
        public String getPath() {
            return this.path;
        }

        public MainGuildPermission getDefaultPermission() {
            return this.defaultPermission;
        }
    }

    @Contract("_ -> new")
    public static @NotNull MainGuildCommand of(@NotNull MainGuildPermission permission) {
        return new MainGuildCommand(permission, Set.of());
    }

    @Contract("_, _ -> new")
    public static @NotNull MainGuildCommand of(@NotNull MainGuildPermission permission,
                                               @NotNull EnumSet<MainGuildRole> role) {
        if(role.isEmpty()) return of(permission);

        return new MainGuildCommand(permission, Collections.unmodifiableSet(role));
    }
}
