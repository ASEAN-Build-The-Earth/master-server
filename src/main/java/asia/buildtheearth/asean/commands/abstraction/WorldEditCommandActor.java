package asia.buildtheearth.asean.commands.abstraction;

import asia.buildtheearth.asean.MasterServer;
import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.text.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Constructable non-player actor for WorldEdit's edit session's actor.
 * This class send all outputs to the {@link CommandExecution} process.
 *
 * @see <a href="https://github.com/IntellectualSites/FastAsyncWorldEdit/blob/373dfbc858563701a5ec0f827387b930e2b248f1/worldedit-bukkit/src/main/java/com/sk89q/worldedit/bukkit/BukkitCommandSender.java">
 *     BukkitCommandSender.java
 * </a>
 */
public class WorldEditCommandActor extends AbstractNonPlayerActor {

    /**
     * One time generated ID. {@snippet : UUID.nameUUIDFromBytes("MASTER_SERVER".getBytes())}
     */
    private static final UUID DEFAULT_ID = UUID.fromString("0c21ffb7-da01-3e1e-b9d9-ea6cea50e196");

    private final CommandExecution sender;
    private final MasterServer plugin;

    /**
     * Create a command execution executing to WorldEdit process.
     *
     * @param plugin Plugin instance initializing this actor.
     * @param sender Command execution which initialized this actor.
     */
    public WorldEditCommandActor(MasterServer plugin, CommandExecution sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    @Override
    public String getName() {
        return plugin.getName();
    }

    @Override
    @Deprecated
    public void printRaw(String msg) {
        sender.send(new Text(msg));
//        for (String part : msg.split("\n")) {
//            sender.sendMessage(part);
//        }
    }

    @Override
    @Deprecated
    public void print(String msg) {
        sender.send(new Text(msg));
//        for (String part : msg.split("\n")) {
//            sender.send(new Text("§d" + part));
//        }
    }

    @Override
    @Deprecated
    public void printDebug(String msg) {
        sender.send(new Text(msg));
//        for (String part : msg.split("\n")) {
//            sender.send(new Text("§7" + part));
//        }
    }

    @Override
    @Deprecated
    public void printError(String msg) {
        sender.send(new Text(msg));
//        for (String part : msg.split("\n")) {
//            sender.send(new Text("§c" + part));
//        }
    }

    @Override
    public void print(Component component) {
        MasterServer.error("Trying to print WorldEdit component " + component.toString());
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {
            @Nullable
            @Override
            public String getName() {
                return sender.getID();
            }

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean isPersistent() {
                return false;
            }

            @Override
            public UUID getUniqueId() {
                return DEFAULT_ID;
            }
        };
    }

    @Override
    public UUID getUniqueId() {
        return DEFAULT_ID;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void checkPermission(String permission) throws AuthorizationException {

    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public void setPermission(String permission, boolean value) {

    }
}
