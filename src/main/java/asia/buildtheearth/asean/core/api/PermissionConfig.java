package asia.buildtheearth.asean.core.api;

public interface PermissionConfig {
    default String getConfigPath() {
        return this.getPath() + this.getName();
    }

    String getName();

    String getPath();

    MainGuildPermission getDefaultPermission();
}
