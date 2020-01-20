package au.com.addstar.pandora;

import java.util.logging.Level;

import org.bukkit.Bukkit;

public interface Module {
    void onEnable();

    void onDisable();

    void setPandoraInstance(MasterPlugin plugin);

    default boolean disableListener() {
        return false;
    }

    default void log(String message) {
        Bukkit.getLogger().log(Level.ALL, "[ PANDORA -" + this.getClass().getName() + "] " + message);
    }

}
